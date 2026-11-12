package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.util.EnlaceCita;

import java.sql.*;
import java.time.*;

public class EnlaceCitaDAO {
    public record Resumen(
            int id,
            LocalDateTime fecha,
            String medico,
            String especialidad,
            String sede,
            String estado,
            boolean asistenciaConfirmada,
            boolean plazoVencido,
            boolean futura) {}

    private final AgendaCitaDAO.ConnectionSource conexiones;
    private final Clock reloj;

    public EnlaceCitaDAO() {
        this(DatabaseConfig::getConnection, Clock.system(ZoneId.of("America/Lima")));
    }

    EnlaceCitaDAO(AgendaCitaDAO.ConnectionSource conexiones, Clock reloj) {
        this.conexiones = conexiones;
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    public Resumen consultar(int usuario, int cita) {
        try (var c = conexiones.open()) {
            var datos =
                    new pe.edu.utp.clinica.repository.EnlaceCitaRepository(c)
                            .consultar(usuario, cita);
            if (datos == null)
                throw new AgendaException(
                        404,
                        "CITA_NO_ENCONTRADA",
                        "No se encontró una cita propia para este enlace.");
            var ahora = LocalDateTime.now(reloj);
            return new Resumen(
                    datos.id(),
                    datos.fecha(),
                    datos.medico(),
                    datos.especialidad(),
                    datos.sede(),
                    datos.estado(),
                    datos.confirmada() != null,
                    datos.vence() == null || !ahora.isBefore(datos.vence()),
                    datos.fecha().isAfter(ahora));
        } catch (SQLException e) {
            throw new AgendaException(
                    503,
                    "CITA_NO_DISPONIBLE",
                    "No se pudo consultar tu cita. Inténtalo nuevamente.");
        }
    }

    public void ejecutar(int usuario, EnlaceCita enlace) {
        ejecutar(usuario, enlace, null);
    }

    public void ejecutar(int usuario, EnlaceCita enlace, String motivo) {
        Resumen cita = consultar(usuario, enlace.cita());
        if (!cita.futura()
                || !("CONFIRMADA".equals(cita.estado()) || "SOLICITADA".equals(cita.estado())))
            throw new AgendaException(
                    409, "CITA_NO_DISPONIBLE", "La cita ya no está disponible para esta acción.");
        if (enlace.accion() == EnlaceCita.Accion.CONFIRMAR) {
            new ConfirmacionAsistenciaDAO(conexiones, reloj)
                    .confirmarPaciente(usuario, enlace.cita());
            return;
        }
        var citas =
                new CitaDAO() {
                    @Override
                    protected Connection abrirConexion() throws SQLException {
                        return conexiones.open();
                    }

                    @Override
                    protected Clock relojClinico() {
                        return reloj;
                    }
                };
        int paciente = citas.obtenerIdPacientePorUsuarioId(usuario);
        if (!citas.actualizarEstadoCitaPaciente(enlace.cita(), paciente, "CANCELADA", motivo))
            throw new AgendaException(
                    409, "CITA_NO_DISPONIBLE", "La cita cambió de estado o ya pasó su horario.");
    }
}
