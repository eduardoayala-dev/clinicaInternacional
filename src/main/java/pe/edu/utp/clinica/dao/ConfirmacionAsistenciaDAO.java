package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ConfirmacionAsistenciaDAO {
    private static final Set<String> ESTADOS_ACTIVOS = Set.of("SOLICITADA", "CONFIRMADA");
    private final AgendaCitaDAO.ConnectionSource connections;
    private final Clock clock;

    public record Seguimiento(int idCita, String paciente, String documento, String correo, String telefono,
                              String fechaHora, String estado, int intentos, String ultimoContacto,
                              String ultimoResultado, boolean vencida) {}
    public record ResultadoContacto(boolean asistenciaConfirmada) {}
    public record EstadoAsistencia(boolean confirmada, boolean vencida, String venceEn) {}

    public ConfirmacionAsistenciaDAO() {
        this(DatabaseConfig::getConnection, Clock.system(ZoneId.of("America/Lima")));
    }

    ConfirmacionAsistenciaDAO(AgendaCitaDAO.ConnectionSource connections, Clock clock) {
        this.connections = connections;
        this.clock = clock.withZone(ZoneId.of("America/Lima"));
    }

    public Map<Integer, EstadoAsistencia> estadoDelPaciente(int idUsuario, List<Integer> citas) {
        if (idUsuario <= 0 || citas == null || citas.isEmpty()) return Map.of();
        Set<Integer> solicitadas = Set.copyOf(citas);
        String sql = "SELECT ca.id_cita,ca.confirmada_en,ca.vence_en FROM confirmacion_asistencia ca JOIN cita c ON c.id_cita=ca.id_cita "
                + "JOIN paciente p ON p.id_paciente=c.id_paciente WHERE p.id_usuario=?";
        try (Connection conn = connections.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);

            try (ResultSet rs = ps.executeQuery()) {
                Map<Integer, EstadoAsistencia> estados = new java.util.HashMap<>();
                LocalDateTime ahora = LocalDateTime.now(clock);
                while (rs.next()) {
                    if (!solicitadas.contains(rs.getInt(1))) continue;
                    LocalDateTime confirmada = rs.getObject(2, LocalDateTime.class);
                    LocalDateTime vence = rs.getObject(3, LocalDateTime.class);
                    estados.put(rs.getInt(1), new EstadoAsistencia(confirmada != null, confirmada == null && !ahora.isBefore(vence),
                            pe.edu.utp.clinica.util.FormatoClinico.fechaHora(vence)));
                }
                return Map.copyOf(estados);
            }
        } catch (SQLException e) {
            throw new AgendaException(503, "CONFIRMACION_NO_DISPONIBLE", "No se pudo consultar el estado de asistencia.");
        }
    }

    public void confirmarPaciente(int idUsuario, int idCita) {
        if (idUsuario <= 0 || idCita <= 0) throw invalida();
        TransaccionJdbc.ejecutar(connections, conn -> {
            int paciente = pacientePropio(conn, idUsuario);
            bloquearPaciente(conn, paciente);
            Cita cita = citaPaciente(conn, idCita, paciente);
            LocalDateTime ahora = LocalDateTime.now(clock).withNano(0);
            if (!ESTADOS_ACTIVOS.contains(cita.estado()) || !cita.fechaHora().isAfter(ahora)) throw noConfirmable();
            if (cita.confirmada()) return null;
            if (!ahora.isBefore(cita.venceEn())) throw new AgendaException(409, "PLAZO_CONFIRMACION_VENCIDO", "El plazo de confirmación terminó. Contacta con admisión para coordinar tu cita.");
            try (PreparedStatement ps = conn.prepareStatement("UPDATE confirmacion_asistencia SET confirmada_en=?,canal='WEB',id_usuario_confirmo=? WHERE id_cita=? AND confirmada_en IS NULL")) {
                ps.setObject(1, ahora); ps.setInt(2, idUsuario); ps.setInt(3, idCita);
                if (ps.executeUpdate() != 1) throw noConfirmable();
            }
            return null;
        });
    }

    public List<Seguimiento> listarPendientes(int idUsuario) {
        try (Connection conn = connections.open()) {
            int sede = sedePersonal(conn, idUsuario);
            LocalDateTime ahora = LocalDateTime.now(clock).withNano(0);
            String sql = "SELECT c.id_cita,CONCAT(u.nombres,' ',u.apellidos),p.dni,u.email,p.telefono,c.fecha_hora,c.estado,"
                    + "COUNT(co.id_contacto),MAX(co.fecha_contacto),"
                    + "(SELECT co2.resultado FROM contacto_admision_cita co2 WHERE co2.id_cita=c.id_cita ORDER BY co2.fecha_contacto DESC,co2.id_contacto DESC LIMIT 1),"
                    + "ca.vence_en "
                    + "FROM cita c JOIN paciente p ON p.id_paciente=c.id_paciente JOIN usuario u ON u.id_usuario=p.id_usuario "
                    + "JOIN medico m ON m.id_medico=c.id_medico LEFT JOIN confirmacion_asistencia ca ON ca.id_cita=c.id_cita "
                    + "LEFT JOIN contacto_admision_cita co ON co.id_cita=c.id_cita "
                    + "WHERE m.id_sede=? AND c.estado IN ('SOLICITADA','CONFIRMADA') AND c.fecha_hora>? "
                    + "AND (ca.confirmada_en IS NULL) GROUP BY c.id_cita,u.nombres,u.apellidos,p.dni,u.email,p.telefono,c.fecha_hora,c.estado,ca.vence_en "
                    + "ORDER BY ca.vence_en,c.fecha_hora";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, sede); ps.setObject(2, ahora);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Seguimiento> pendientes = new ArrayList<>();
                    while (rs.next()) {
                        LocalDateTime vence = rs.getObject(11, LocalDateTime.class);
                        LocalDateTime fecha = rs.getObject(6, LocalDateTime.class);
                        pendientes.add(new Seguimiento(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                                rs.getString(5), fecha.toString(), rs.getString(7), rs.getInt(8),
                                rs.getObject(9, LocalDateTime.class) == null ? null : rs.getObject(9, LocalDateTime.class).toString(),
                                rs.getString(10), vence != null && !ahora.isBefore(vence)));
                    }
                    return List.copyOf(pendientes);
                }
            }
        } catch (SQLException e) {
            throw new AgendaException(503, "SEGUIMIENTO_NO_DISPONIBLE", "No se pudo consultar el seguimiento de confirmaciones.");
        }
    }

    public ResultadoContacto registrarContacto(int idUsuario, int idCita, String canal, String resultado) {
        if (idUsuario <= 0 || idCita <= 0) throw invalida();
        if (canal == null || resultado == null || !Set.of("LLAMADA", "CORREO").contains(canal)
                || !Set.of("SIN_RESPUESTA", "CONFIRMA", "NO_CONFIRMA").contains(resultado)) throw invalida();
        return TransaccionJdbc.ejecutar(connections, conn -> {
            int sede = sedePersonal(conn, idUsuario);
            Cita cita = bloquearCitaAdmision(conn, idCita, sede);
            LocalDateTime ahora = LocalDateTime.now(clock).withNano(0);
            if (!ESTADOS_ACTIVOS.contains(cita.estado()) || !cita.fechaHora().isAfter(ahora)) throw noConfirmable();
            if (cita.confirmada()) throw new AgendaException(409, "ASISTENCIA_YA_CONFIRMADA", "La asistencia ya está confirmada.");
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO contacto_admision_cita(id_cita,id_usuario_personal,fecha_contacto,canal,resultado) VALUES(?,?,?,?,?)")) {
                ps.setInt(1, idCita); ps.setInt(2, idUsuario); ps.setObject(3, ahora); ps.setString(4, canal); ps.setString(5, resultado);
                ps.executeUpdate();
            }
            boolean confirmada = "CONFIRMA".equals(resultado);
            if (confirmada) {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE confirmacion_asistencia SET confirmada_en=?,canal='ADMISION',id_usuario_confirmo=? WHERE id_cita=? AND confirmada_en IS NULL")) {
                    ps.setObject(1, ahora); ps.setInt(2, idUsuario); ps.setInt(3, idCita);
                    if (ps.executeUpdate() != 1) throw noConfirmable();
                }
            }
            return new ResultadoContacto(confirmada);
        });
    }

    private record Cita(String estado, LocalDateTime fechaHora, LocalDateTime venceEn, boolean confirmada) {}

    private int pacientePropio(Connection conn, int usuario) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT p.id_paciente FROM paciente p JOIN usuario u ON u.id_usuario=p.id_usuario WHERE u.id_usuario=? AND u.activo=TRUE AND u.rol='PACIENTE'")) {
            ps.setInt(1, usuario);
            try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) { throw new AgendaException(403, "PACIENTE_NO_HABILITADO", "El perfil de paciente no está habilitado."); }
                return rs.getInt(1); }
        }
    }

    private static void bloquearPaciente(Connection conn, int paciente) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id_paciente FROM paciente WHERE id_paciente=? FOR UPDATE")) {
            ps.setInt(1, paciente); try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw noConfirmable(); }
        }
    }

    private static Cita citaPaciente(Connection conn, int cita, int paciente) throws SQLException {
        String sql = "SELECT c.estado,c.fecha_hora,ca.vence_en,ca.confirmada_en FROM cita c JOIN confirmacion_asistencia ca ON ca.id_cita=c.id_cita WHERE c.id_cita=? AND c.id_paciente=? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cita); ps.setInt(2, paciente);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw noConfirmable();
                return new Cita(rs.getString(1), rs.getObject(2, LocalDateTime.class), rs.getObject(3, LocalDateTime.class), rs.getObject(4, LocalDateTime.class) != null);
            }
        }
    }

    private static int sedePersonal(Connection conn, int usuario) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT p.id_sede FROM personal_administrativo p JOIN usuario u ON u.id_usuario=p.id_usuario JOIN sede s ON s.id_sede=p.id_sede WHERE u.id_usuario=? AND u.activo=TRUE AND u.rol='PERSONAL_ADMINISTRATIVO' AND s.estado='ACTIVO'")) {
            ps.setInt(1, usuario); try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) { throw new AgendaException(403, "ADMISION_NO_HABILITADA", "Tu perfil de admisión no está habilitado."); }
                return rs.getInt(1); }
        }
    }

    private static Cita bloquearCitaAdmision(Connection conn, int idCita, int sede) throws SQLException {
        String sql = "SELECT c.estado,c.fecha_hora,ca.vence_en,ca.confirmada_en FROM cita c JOIN medico m ON m.id_medico=c.id_medico JOIN confirmacion_asistencia ca ON ca.id_cita=c.id_cita WHERE c.id_cita=? AND m.id_sede=? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCita); ps.setInt(2, sede);
            try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) { throw noConfirmable(); }
                return new Cita(rs.getString(1), rs.getObject(2, LocalDateTime.class), rs.getObject(3, LocalDateTime.class), rs.getObject(4, LocalDateTime.class) != null); }
        }
    }

    private static AgendaException invalida() { return new AgendaException(400, "DATOS_INVALIDOS", "Revisa los datos de la solicitud."); }
    private static AgendaException noConfirmable() { return new AgendaException(409, "CITA_NO_CONFIRMABLE", "La cita no está disponible para confirmar asistencia."); }
}
