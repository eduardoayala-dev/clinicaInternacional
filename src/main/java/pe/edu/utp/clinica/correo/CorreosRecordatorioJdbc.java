package pe.edu.utp.clinica.correo;

import pe.edu.utp.clinica.auth.SesionesJdbc;
import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.util.Html;

import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class CorreosRecordatorioJdbc extends CorreosPendientesJdbc {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final String portal;
    private final String base;

    public CorreosRecordatorioJdbc(String base) {
        this(DatabaseConfig::getConnection, base);
    }

    public CorreosRecordatorioJdbc(SesionesJdbc.Conexion conexiones, String base) {
        super(conexiones, RECORDATORIOS);
        this.base = base;
        this.portal = base + "/paciente/historial";
    }

    public int programar(Instant ahora, int horas) throws SQLException {
        if (horas < 1 || horas > 168)
            throw new IllegalArgumentException("La anticipación debe ser de 1 a 168 horas.");
        try (var c = conexiones.abrir()) {
            c.setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED);
            return pe.edu.utp.clinica.persistence.PersistenciaJpa.transaccion(
                    c,
                    () ->
                            new pe.edu.utp.clinica.repository.ProgramacionCorreosRepository(c)
                                    .programar(
                                            LocalDateTime.ofInstant(ahora, LIMA),
                                            utc(ahora),
                                            horas));
        }
    }

    @Override
    public Optional<Correo> mensaje(int id, Instant ahora) throws SQLException {
        try (var c = conexiones.abrir()) {
            if (!new pe.edu.utp.clinica.repository.ProgramacionCorreosRepository(c)
                    .recordatorioPendiente(id, LocalDateTime.ofInstant(ahora, LIMA)))
                return Optional.empty();
        }
        return new CorreosCitaJdbc(conexiones).datos(id, ahora).map(this::crearCorreo);
    }

    private Correo crearCorreo(ConfirmacionCita cita) {
        String fecha = cita.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String texto =
                "Hola, "
                        + cita.paciente()
                        + ". Recuerda tu cita CIT-"
                        + cita.id()
                        + " el "
                        + fecha
                        + " (hora de Perú)."
                        + "\nEspecialidad: "
                        + cita.especialidad()
                        + "\nMédico: "
                        + cita.medico()
                        + "\nSede: "
                        + cita.sede()
                        + "\n"
                        + "Tu asistencia está pendiente de confirmar. Ingresa al portal para"
                        + " confirmar tu asistencia o gestionar tu cita: "
                        + portal
                        + "\n"
                        + "Si ya venció el plazo de confirmación, contacta a admisión. Este mensaje"
                        + " no cancela tu cita."
                        + EnlacesCorreoCita.texto(base, cita.id());
        String html =
                """
<h1 style="color:#0d2b4e;font-family:Georgia,serif">Recuerda tu próxima cita</h1>
<p>Hola, %s.</p><p><strong>CIT-%d · %s (hora de Perú)</strong></p>
<p>%s · %s<br>%s</p>
<p>Tu asistencia está pendiente de confirmar. Ingresa al portal para confirmar o gestionar tu cita.</p>
<p><a href="%s" style="display:inline-block;padding:12px 20px;background:#1668b8;color:#fff;border-radius:8px;text-decoration:none">Ver mis citas</a></p>
<p>Si ya venció el plazo de confirmación, contacta a admisión. Este mensaje no cancela tu cita.</p>
"""
                        .formatted(
                                Html.texto(cita.paciente()),
                                cita.id(),
                                fecha,
                                Html.texto(cita.especialidad()),
                                Html.texto(cita.medico()),
                                Html.texto(cita.sede()),
                                Html.texto(portal));
        return new Correo(
                cita.destinatario(),
                "Recordatorio de cita CIT-" + cita.id(),
                texto,
                PlantillasCorreo.marco(
                        html + EnlacesCorreoCita.html(base, cita.id()), "Recordatorio de cita"));
    }
}
