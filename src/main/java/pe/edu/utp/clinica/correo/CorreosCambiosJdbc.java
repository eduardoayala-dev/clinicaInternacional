package pe.edu.utp.clinica.correo;

import pe.edu.utp.clinica.auth.SesionesJdbc;
import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.util.Html;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class CorreosCambiosJdbc extends CorreosPendientesJdbc {
    public enum Tipo {
        CANCELADA,
        REPROGRAMADA,
        CONFIRMACION_VENCIDA
    }

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public CorreosCambiosJdbc() {
        this(DatabaseConfig::getConnection);
    }

    public CorreosCambiosJdbc(SesionesJdbc.Conexion conexiones) {
        super(conexiones, CAMBIOS);
    }

    public static void encolar(Connection c, int cita, Integer anterior, Tipo tipo, Instant ahora)
            throws SQLException {
        new pe.edu.utp.clinica.repository.ReservaCitaRepository(c)
                .correoCambio(cita, anterior, tipo.name(), ahora);
    }

    @Override
    public Optional<Correo> mensaje(int id, Instant ahora) throws SQLException {
        try (var c = conexiones.abrir()) {
            var resultado = new pe.edu.utp.clinica.repository.CorreoDatosRepository(c).cambio(id);
            if (resultado.isEmpty()) return Optional.empty();
            var r = resultado.get();
            Tipo tipo = Tipo.valueOf(r.tipo());
            var fecha = r.fecha();
            if (tipo == Tipo.CANCELADA && !"CANCELADA".equals(r.estado())) return Optional.empty();
            if (tipo == Tipo.REPROGRAMADA
                    && (!"CONFIRMADA".equals(r.estado())
                            || !fecha.isAfter(LocalDateTime.ofInstant(ahora, LIMA))
                            || r.origen() == null
                            || !"REPROGRAMADA".equals(r.estadoAnterior()))) return Optional.empty();
            if (tipo == Tipo.CONFIRMACION_VENCIDA) {
                var limite = r.vence();
                var local = LocalDateTime.ofInstant(ahora, LIMA);
                if (!("CONFIRMADA".equals(r.estado()) || "SOLICITADA".equals(r.estado()))
                        || !fecha.isAfter(local)
                        || r.confirmada() != null
                        || limite == null
                        || limite.isAfter(local)) return Optional.empty();
            }
            String titulo =
                    switch (tipo) {
                        case CANCELADA -> "Tu cita fue cancelada";
                        case REPROGRAMADA -> "Tu cita fue reprogramada";
                        case CONFIRMACION_VENCIDA -> "Coordina tu asistencia con admisión";
                    };
            String detalle =
                    switch (tipo) {
                        case CANCELADA ->
                                "Esta cita ya no está reservada. Puedes buscar un nuevo horario en"
                                    + " tu portal.";
                        case REPROGRAMADA ->
                                "La cita CIT-"
                                        + r.origen()
                                        + " del "
                                        + r.fechaAnterior().format(FECHA)
                                        + " fue reemplazada por esta nueva reserva. La asistencia"
                                        + " de la nueva cita está pendiente de confirmar.";
                        case CONFIRMACION_VENCIDA ->
                                "El plazo para confirmar tu asistencia terminó. Tu cita sigue"
                                    + " vigente: contacta con admisión para coordinarla. Este aviso"
                                    + " no cancela tu reserva.";
                    };
            String datos = "CIT-" + r.cita() + " · " + fecha.format(FECHA) + " (hora de Perú)";
            String medico = r.medico() + " " + r.apellidos();
            String texto =
                    titulo
                            + "\nHola, "
                            + r.paciente()
                            + ".\n"
                            + datos
                            + "\n"
                            + r.especialidad()
                            + " · "
                            + medico
                            + "\n"
                            + r.sede()
                            + "\n\n"
                            + detalle
                            + "\n"
                            + "Consulta el estado actualizado en Mis citas, iniciando sesión en tu"
                            + " portal.";
            String contenido =
                    "<h1 style=\"color:#0d2b4e;font-family:Georgia,serif\">"
                            + titulo
                            + "</h1><p>Hola, "
                            + Html.texto(r.paciente())
                            + ".</p><p><strong>"
                            + Html.texto(datos)
                            + "</strong></p><p>"
                            + Html.texto(r.especialidad())
                            + " · "
                            + Html.texto(medico)
                            + "<br>"
                            + Html.texto(r.sede())
                            + "</p><p>"
                            + Html.texto(detalle)
                            + "</p><p>Consulta el estado actualizado en <strong>Mis citas</strong>,"
                            + " iniciando sesión en tu portal.</p>";
            return Optional.of(
                    new Correo(
                            r.correo(),
                            titulo + " · CIT-" + r.cita(),
                            texto,
                            PlantillasCorreo.marco(contenido, titulo)));
        }
    }
}
