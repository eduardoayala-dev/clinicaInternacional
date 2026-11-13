package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.*;
import java.time.*;
import java.util.*;

public final class InicioOperativoDAO {
    public record Resumen(
            int citas, int canceladas, int cupos, int ofertas, LocalDateTime actualizado) {}

    private final AgendaCitaDAO.ConnectionSource conexiones;
    private final Clock reloj;

    public InicioOperativoDAO() {
        this(DatabaseConfig::getConnection, Clock.system(ZoneId.of("America/Lima")));
    }

    InicioOperativoDAO(AgendaCitaDAO.ConnectionSource conexiones, Clock reloj) {
        this.conexiones = conexiones;
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    public Resumen consultar(int actor) {
        return TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    int sede = AmbitoAdmisionDAO.sede(c, actor);
                    var ahora = LocalDateTime.now(reloj);
                    var fecha = ahora.toLocalDate();
                    var repo = new pe.edu.utp.clinica.repository.InicioOperativoRepository(c);
                    var conteo = repo.contar(sede, fecha);
                    int ofertas = repo.ofertas(sede, ahora);
                    int cupos =
                            new MedicoDAO(conexiones::open, reloj)
                                    .contarCuposDisponibles(c, sede, fecha, fecha.plusDays(7));
                    return new Resumen(conteo.citas(), conteo.canceladas(), cupos, ofertas, ahora);
                });
    }

    public List<Map<String, String>> operaciones(int actor) {
        return TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    int sede = AmbitoAdmisionDAO.sede(c, actor);
                    var lista = new ArrayList<Map<String, String>>();
                    for (var operacion :
                            new pe.edu.utp.clinica.repository.InicioOperativoRepository(c)
                                    .operaciones(sede)) {
                        String texto =
                                resumenNotificacion(operacion.tipo(), operacion.mensaje())
                                        + " · Cita #"
                                        + operacion.cita()
                                        + " · "
                                        + Objects.requireNonNullElse(
                                                operacion.paciente(), "Paciente")
                                        + " · "
                                        + pe.edu.utp.clinica.util.FormatoClinico.fechaHora(
                                                operacion.programada());
                        lista.add(
                                Map.of(
                                        "hora",
                                        pe.edu.utp.clinica.util.FormatoClinico.fechaHora(
                                                operacion.fecha()),
                                        "texto",
                                        texto));
                    }
                    return List.copyOf(lista);
                });
    }

    private static String resumenNotificacion(String tipo, String mensaje) {
        String texto =
                (Objects.requireNonNullElse(tipo, "")
                                + " "
                                + Objects.requireNonNullElse(mensaje, ""))
                        .toLowerCase(Locale.ROOT);
        if (texto.contains("cancelad")) return "Aviso de cancelación registrado";
        if (texto.contains("reprogramad")) return "Aviso de reprogramación registrado";
        if (texto.contains("recordatorio")) return "Recordatorio de cita registrado";
        if (texto.contains("confirmad")) return "Aviso de confirmación de cita registrado";
        return "Notificación de cita registrada";
    }
}
