package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class PanelSistemaDAO {
    public record Filtro(LocalDate desde, LocalDate hasta, int sede, int especialidad) {
        public Filtro {
            if (desde == null
                    || hasta == null
                    || desde.getYear() < 1000
                    || hasta.getYear() > 9998
                    || hasta.isBefore(desde)
                    || ChronoUnit.DAYS.between(desde, hasta) > 365
                    || sede < 0
                    || especialidad < 0)
                throw new AgendaException(
                        400,
                        "FILTROS_INVALIDOS",
                        "Selecciona fechas válidas, hasta un año, y filtros válidos.");
        }
    }

    public record Opcion(int id, String nombre) {}

    public record Indicadores(
            long citas,
            long atendidas,
            long canceladas,
            long inasistencias,
            long reasignadas,
            long ofertas,
            long aceptadas,
            long pendientes,
            long bajo,
            long medio,
            long alto,
            long sinRiesgo) {
        public static Indicadores cero() {
            return new Indicadores(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    public record Sede(int id, String nombre, String direccion, String estado, Indicadores datos) {}

    public record Semana(LocalDate desde, LocalDate hasta, long citas, long atendidas) {}

    public record Auditoria(LocalDateTime fecha, String actor, String evento, String descripcion) {}

    public record Totales(
            long usuarios,
            long pacientes,
            long medicos,
            long personal,
            long administradores,
            long sedes) {}

    public record Panel(
            Filtro filtro,
            Indicadores datos,
            List<Sede> sedes,
            List<Opcion> catalogoSedes,
            List<Opcion> especialidades,
            List<Semana> semanas,
            List<Auditoria> auditoria,
            Totales totales,
            LocalDateTime actualizado) {}

    private final AgendaCitaDAO.ConnectionSource conexiones;
    private final Clock reloj;

    public PanelSistemaDAO() {
        this(DatabaseConfig::getConnection, Clock.system(ZoneId.of("America/Lima")));
    }

    PanelSistemaDAO(AgendaCitaDAO.ConnectionSource conexiones, Clock reloj) {
        this.conexiones = conexiones;
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    public Panel consultar(int actor, Filtro filtro) {
        Objects.requireNonNull(filtro);
        return TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    InvitacionPersonalDAO.administrador(c, actor);
                    var repo = new pe.edu.utp.clinica.repository.PanelSistemaRepository(c);
                    var sedes = repo.sedes();
                    var especialidades = repo.especialidades();
                    if (filtro.sede() > 0 && sedes.stream().noneMatch(v -> v.id() == filtro.sede())
                            || filtro.especialidad() > 0
                                    && especialidades.stream()
                                            .noneMatch(v -> v.id() == filtro.especialidad()))
                        throw new AgendaException(
                                400,
                                "FILTROS_INVALIDOS",
                                "La sede o especialidad seleccionada no existe.");
                    var porSede = new LinkedHashMap<Integer, Sede>();
                    for (var v : repo.detalleSedes(filtro.sede())) porSede.put(v.id(), v);
                    for (var v : repo.citas(filtro)) {
                        var a = porSede.get(v.sede());
                        if (a == null) continue;
                        porSede.put(
                                a.id(),
                                new Sede(
                                        a.id(),
                                        a.nombre(),
                                        a.direccion(),
                                        a.estado(),
                                        new Indicadores(
                                                v.citas(),
                                                v.atendidas(),
                                                v.canceladas(),
                                                v.ausencias(),
                                                v.reasignadas(),
                                                0,
                                                0,
                                                0,
                                                v.bajo(),
                                                v.medio(),
                                                v.alto(),
                                                v.sinRiesgo())));
                    }
                    for (var v : repo.ofertas(filtro, LocalDateTime.now(reloj))) {
                        var a = porSede.get(v.sede());
                        if (a == null) continue;
                        var d = a.datos();
                        porSede.put(
                                a.id(),
                                new Sede(
                                        a.id(),
                                        a.nombre(),
                                        a.direccion(),
                                        a.estado(),
                                        new Indicadores(
                                                d.citas(),
                                                d.atendidas(),
                                                d.canceladas(),
                                                d.inasistencias(),
                                                d.reasignadas(),
                                                v.total(),
                                                v.aceptadas(),
                                                v.pendientes(),
                                                d.bajo(),
                                                d.medio(),
                                                d.alto(),
                                                d.sinRiesgo())));
                    }
                    long[] t = new long[12];
                    for (var a : porSede.values()) {
                        var v = a.datos();
                        long[] valores = {
                            v.citas(),
                            v.atendidas(),
                            v.canceladas(),
                            v.inasistencias(),
                            v.reasignadas(),
                            v.ofertas(),
                            v.aceptadas(),
                            v.pendientes(),
                            v.bajo(),
                            v.medio(),
                            v.alto(),
                            v.sinRiesgo()
                        };
                        for (int n = 0; n < t.length; n++) t[n] += valores[n];
                    }
                    var datos =
                            new Indicadores(
                                    t[0], t[1], t[2], t[3], t[4], t[5], t[6], t[7], t[8], t[9],
                                    t[10], t[11]);
                    int cantidad =
                            (int) (ChronoUnit.DAYS.between(filtro.desde(), filtro.hasta()) / 7) + 1;
                    long[][] valores = new long[cantidad][2];
                    for (var d : repo.dias(filtro)) {
                        int n = (int) (ChronoUnit.DAYS.between(filtro.desde(), d.fecha()) / 7);
                        valores[n][0] += d.citas();
                        valores[n][1] += d.atendidas();
                    }
                    var semanas = new ArrayList<Semana>();
                    for (int n = 0; n < cantidad; n++) {
                        var desde = filtro.desde().plusDays(n * 7L);
                        var hasta = desde.plusDays(6);
                        semanas.add(
                                new Semana(
                                        desde,
                                        hasta.isAfter(filtro.hasta()) ? filtro.hasta() : hasta,
                                        valores[n][0],
                                        valores[n][1]));
                    }
                    return new Panel(
                            filtro,
                            datos,
                            List.copyOf(porSede.values()),
                            sedes,
                            especialidades,
                            List.copyOf(semanas),
                            repo.auditoria(),
                            repo.totales(),
                            LocalDateTime.now(reloj));
                });
    }
}
