package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.auth.SesionesJdbc;
import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.*;
import java.time.*;
import java.util.*;

public final class SeguimientoOfertasDAO {
    public static final int POR_PAGINA = 15;
    public static final List<String> ESTADOS =
            List.of("PENDIENTE", "ACEPTADA", "RECHAZADA", "EXPIRADA", "CANCELADA");

    public record Filtro(String estado, LocalDate fecha, int especialidad, int pagina) {
        public Filtro {
            if (estado == null
                    || (!estado.isEmpty() && !ESTADOS.contains(estado))
                    || especialidad < 0
                    || pagina < 1
                    || pagina > 10000)
                throw new AgendaException(
                        400, "FILTRO_INVALIDO", "Selecciona filtros y una página válidos.");
        }
    }

    public record Especialidad(int id, String nombre) {}

    public record Oferta(
            int id,
            int citaOriginal,
            int lista,
            String paciente,
            String medico,
            String especialidad,
            LocalDateTime cita,
            LocalDateTime ofrecida,
            LocalDateTime limite,
            String estado,
            Integer citaAsignada,
            boolean plazoVencido) {}

    public record Panel(
            String sede,
            Filtro filtro,
            int pagina,
            int paginas,
            long total,
            Map<String, Long> estados,
            List<Oferta> ofertas,
            List<Especialidad> especialidades,
            LocalDateTime consultadoEn) {
        public Panel {
            estados = Map.copyOf(estados);
            ofertas = List.copyOf(ofertas);
            especialidades = List.copyOf(especialidades);
        }
    }

    private final SesionesJdbc.Conexion conexiones;
    private final Clock reloj;

    public SeguimientoOfertasDAO() {
        this(DatabaseConfig::getConnection, Clock.system(ZoneId.of("America/Lima")));
    }

    public SeguimientoOfertasDAO(SesionesJdbc.Conexion conexiones, Clock reloj) {
        this.conexiones = conexiones;
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    public Panel consultar(int actor, Filtro filtro) {
        try (var c = conexiones.abrir()) {
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            return pe.edu.utp.clinica.persistence.PersistenciaJpa.transaccion(
                    c,
                    () -> {
                        var sede =
                                new pe.edu.utp.clinica.repository.AmbitoAdmisionRepository(c)
                                        .sedeConNombre(actor);
                        if (sede == null)
                            throw new AgendaException(
                                    403,
                                    "ACCESO_DENEGADO",
                                    "La cuenta de admisión no está habilitada en una sede activa.");
                        var repo =
                                new pe.edu.utp.clinica.repository.SeguimientoOfertasRepository(c);
                        Map<String, Long> conteos = repo.contar(sede.id(), filtro);
                        long total = conteos.values().stream().mapToLong(Long::longValue).sum();
                        int paginas = (int) Math.max(1, (total + POR_PAGINA - 1) / POR_PAGINA);
                        int pagina = Math.min(filtro.pagina(), paginas);
                        var ahora = LocalDateTime.now(reloj);
                        return new Panel(
                                sede.nombre(),
                                filtro,
                                pagina,
                                paginas,
                                total,
                                conteos,
                                repo.listar(sede.id(), filtro, pagina, ahora),
                                repo.especialidades(sede.id()),
                                ahora);
                    });
        } catch (SQLException e) {
            throw new AgendaException(
                    503,
                    "OFERTAS_NO_DISPONIBLES",
                    "No se pudo consultar el seguimiento de ofertas.");
        }
    }
}
