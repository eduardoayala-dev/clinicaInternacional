package pe.edu.utp.clinica.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

final class AtencionCitaDAO {
    private static final String EN_ATENCION = "EN_ATENCION";
    private final AgendaCitaDAO.ConnectionSource connections;
    private final Clock clock;
    private record Cita(int paciente, String estado, LocalDateTime programada, LocalDateTime llegada) {}

    AtencionCitaDAO(AgendaCitaDAO.ConnectionSource connections, Clock clock) {
        this.connections = connections;
        this.clock = clock.withZone(ZoneId.of("America/Lima"));
    }

    boolean iniciar(int medico, int cita) {
        return TransaccionJdbc.ejecutar(connections, conn -> {
            Cita actual = bloquear(conn, medico, cita);
            LocalDateTime ahora = ahora();
            validarSala(actual, ahora);
            try (PreparedStatement ps = conn.prepareStatement("SELECT id_cita FROM atencion_cita WHERE medico_activo=? OR paciente_activo=?")) {
                ps.setInt(1, medico); ps.setInt(2, actual.paciente());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) throw conflicto("El médico o el paciente ya tiene una consulta en curso. Finalízala antes de iniciar otra.");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO atencion_cita(id_cita,id_medico,id_paciente,fecha_inicio) VALUES(?,?,?,?)")) {
                ps.setInt(1, cita); ps.setInt(2, medico); ps.setInt(3, actual.paciente()); ps.setObject(4, ahora);
                ps.executeUpdate();
            }
            estado(conn, cita, medico, EN_ATENCION);
            return true;
        });
    }

    boolean finalizar(int medico, int cita, String prioridad, String fecha, String notas) {
        return TransaccionJdbc.ejecutar(connections, conn -> {
            Cita actual = bloquear(conn, medico, cita);
            LocalDateTime ahora = ahora();
            if (!EN_ATENCION.equals(actual.estado())) throw conflicto("Debes iniciar la atención antes de finalizarla.");
            LocalDate seguimiento = validarSeguimiento(prioridad, fecha, notas);
            cerrarRegistro(conn, medico, cita, ahora, null);
            try (PreparedStatement ps = conn.prepareStatement("UPDATE cita SET estado='ATENDIDA',nivel_urgencia=?,fecha_recomendada_proxima=? WHERE id_cita=? AND id_medico=?")) {
                ps.setString(1, prioridad); ps.setObject(2, seguimiento); ps.setInt(3, cita); ps.setInt(4, medico);
                if (ps.executeUpdate() != 1) throw inexistente();
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO historial_asistencia(id_paciente,id_cita,asistio,observacion,fecha_registro) VALUES(?,?,TRUE,?,?)")) {
                ps.setInt(1, actual.paciente()); ps.setInt(2, cita); ps.setString(3, notas.trim()); ps.setObject(4, ahora);
                ps.executeUpdate();
            }
            return true;
        });
    }

    boolean noAtender(int medico, int cita, String motivo) {
        String razon = motivo(motivo);
        return TransaccionJdbc.ejecutar(connections, conn -> {
            Cita actual = bloquear(conn, medico, cita);
            LocalDateTime ahora = ahora();
            if (EN_ATENCION.equals(actual.estado())) cerrarRegistro(conn, medico, cita, ahora, razon);
            else {
                validarSala(actual, ahora);
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO atencion_cita(id_cita,id_medico,id_paciente,fecha_fin,motivo_no_atencion) VALUES(?,?,?,?,?)")) {
                    ps.setInt(1, cita); ps.setInt(2, medico); ps.setInt(3, actual.paciente());
                    ps.setObject(4, ahora); ps.setString(5, razon); ps.executeUpdate();
                }
            }
            estado(conn, cita, medico, "NO_ATENDIDA");
            return true;
        });
    }

    private Cita bloquear(Connection conn, int medico, int cita) throws SQLException {
        if (medico <= 0 || cita <= 0) throw inexistente();
        int paciente;
        try (PreparedStatement ps = conn.prepareStatement("SELECT id_paciente FROM cita WHERE id_cita=? AND id_medico=?")) {
            ps.setInt(1, cita); ps.setInt(2, medico);
            try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw inexistente(); paciente = rs.getInt(1); }
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT id_paciente FROM paciente WHERE id_paciente=? FOR UPDATE")) {
            ps.setInt(1, paciente); try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw inexistente(); }
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT u.id_usuario FROM usuario u JOIN medico m ON u.id_usuario=m.id_usuario WHERE m.id_medico=? AND u.activo=TRUE AND u.rol='MEDICO' FOR UPDATE")) {
            ps.setInt(1, medico); try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new AgendaException(403, "MEDICO_NO_HABILITADO", "El perfil médico no está habilitado.");
            }
        }
        AgendaCitaDAO.medico(conn, medico);
        try (PreparedStatement ps = conn.prepareStatement("SELECT c.estado,c.fecha_hora,a.fecha_llegada FROM cita c LEFT JOIN admision_cita a ON a.id_cita=c.id_cita WHERE c.id_cita=? AND c.id_medico=? AND c.id_paciente=? FOR UPDATE")) {
            ps.setInt(1, cita); ps.setInt(2, medico); ps.setInt(3, paciente);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw inexistente();
                return new Cita(paciente, rs.getString(1), rs.getObject(2, LocalDateTime.class), rs.getObject(3, LocalDateTime.class));
            }
        }
    }

    private static void validarSala(Cita cita, LocalDateTime ahora) {
        if (!"EN_SALA".equals(cita.estado()) || cita.programada() == null || !cita.programada().toLocalDate().equals(ahora.toLocalDate())
                || cita.llegada() == null || cita.llegada().isAfter(ahora) || !cita.llegada().toLocalDate().equals(ahora.toLocalDate())) {
            throw conflicto("La cita debe ser de hoy y tener una llegada registrada por admisión.");
        }
    }

    private static void cerrarRegistro(Connection conn, int medico, int cita, LocalDateTime ahora, String motivo) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE atencion_cita SET fecha_fin=?,motivo_no_atencion=? WHERE id_cita=? AND id_medico=? AND fecha_inicio IS NOT NULL AND fecha_fin IS NULL AND fecha_inicio<=?")) {
            ps.setObject(1, ahora); ps.setString(2, motivo); ps.setInt(3, cita); ps.setInt(4, medico); ps.setObject(5, ahora);
            if (ps.executeUpdate() != 1) throw conflicto("No se encontró una consulta activa para cerrar.");
        }
    }

    private static void estado(Connection conn, int cita, int medico, String estado) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE cita SET estado=? WHERE id_cita=? AND id_medico=?")) {
            ps.setString(1, estado); ps.setInt(2, cita); ps.setInt(3, medico);
            if (ps.executeUpdate() != 1) throw inexistente();
        }
    }

    private LocalDate validarSeguimiento(String prioridad, String fecha, String notas) {
        if (!Set.of("ALTA", "MEDIA", "BAJA").contains(prioridad == null ? "" : prioridad) || notas == null || notas.isBlank()
                || notas.length() > 200 || controles(notas)) {
            throw invalida("Selecciona la prioridad y registra una indicación de atención de hasta 200 caracteres.");
        }
        if (fecha == null || fecha.isBlank()) return null;
        try {
            if (!fecha.matches("\\d{4}-\\d{2}-\\d{2}")) throw invalida("Selecciona una fecha futura válida para el seguimiento.");
            LocalDate proxima = LocalDate.parse(fecha);
            if (!proxima.isAfter(LocalDate.now(clock))) throw invalida("El seguimiento debe ser posterior a hoy.");
            return proxima;
        } catch (java.time.DateTimeException e) { throw invalida("Selecciona una fecha futura válida para el seguimiento."); }
    }

    static String motivo(String valor) {
        if (valor == null || valor.trim().length() < 3 || valor.length() > 200 || controles(valor)) {
            throw invalida("Registra un motivo de entre 3 y 200 caracteres.");
        }
        return valor.trim();
    }
    private static boolean controles(String valor) { return valor.chars().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t'); }
    private LocalDateTime ahora() { return LocalDateTime.now(clock).withNano(0); }
    private static AgendaException conflicto(String mensaje) { return new AgendaException(409, "ATENCION_NO_DISPONIBLE", mensaje); }
    private static AgendaException invalida(String mensaje) { return new AgendaException(400, "DATOS_INVALIDOS", mensaje); }
    private static AgendaException inexistente() { return new AgendaException(404, "CITA_NO_ENCONTRADA", "No se encontró una cita asignada a este médico."); }
}
