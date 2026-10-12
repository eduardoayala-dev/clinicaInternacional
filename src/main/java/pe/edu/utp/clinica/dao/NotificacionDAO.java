package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO para gestión del centro de notificaciones, alertas de confirmación y recordatorios del paciente.
 */
public class NotificacionDAO {

    public boolean crearNotificacion(int idUsuario, Integer idCita, String tipo, String mensaje) {
        String sql = "INSERT INTO notificacion (id_usuario, id_cita, tipo, canal, mensaje, estado_envio, leida) VALUES (?, ?, ?, 'PLATAFORMA', ?, 'ENVIADA', 0)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            if (idCita != null && idCita > 0) {
                ps.setInt(2, idCita);
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setString(3, tipo != null ? tipo : "Citas");
            ps.setString(4, mensaje);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Map<String, Object> mapearNotificacionFila(ResultSet rs) throws Exception {
        Map<String, Object> m = new HashMap<>();
        int idNotif = rs.getInt("id_notificacion");
        m.put("idNotificacion", idNotif);
        m.put("idCita", rs.getObject("id_cita"));
        String rawTipo = rs.getString("tipo");
        String mensaje = rs.getString("mensaje");
        m.put("mensaje", mensaje);
        m.put("detalle", mensaje);

        Timestamp ts = rs.getTimestamp("fecha_envio");
        String fechaDisplay = "12 oct 2026";
        String horaDisplay = "16:00";
        if (ts != null) {
            LocalDateTime ldt = ts.toLocalDateTime();
            int day = ldt.getDayOfMonth();
            String mes = switch (ldt.getMonthValue()) {
                case 1 -> "ene"; case 2 -> "feb"; case 3 -> "mar"; case 4 -> "abr";
                case 5 -> "may"; case 6 -> "jun"; case 7 -> "jul"; case 8 -> "ago";
                case 9 -> "set"; case 10 -> "oct"; case 11 -> "nov"; default -> "dic";
            };
            fechaDisplay = day + " " + mes + " " + ldt.getYear();
            horaDisplay = String.format("%02d:%02d", ldt.getHour(), ldt.getMinute());
        }
        m.put("fecha", fechaDisplay);
        m.put("hora", horaDisplay);
        m.put("fechaEnvio", fechaDisplay + " · " + horaDisplay);

        boolean leida = rs.getInt("leida") == 1;
        m.put("leida", leida);
        m.put("estado", leida ? "Leído" : "Pendiente");

        String cat = "Aviso del Sistema";
        String msgUpper = mensaje != null ? mensaje.toUpperCase() : "";
        String tipoUpper = rawTipo != null ? rawTipo.toUpperCase() : "";

        if (msgUpper.contains("ESPERA") || tipoUpper.contains("ESPERA")) {
            cat = "Lista de espera";
        } else if (msgUpper.contains("CONFIRMA TU CITA") || msgUpper.contains("RECORDATORIO") || tipoUpper.contains("RECORDATORIO")) {
            cat = "Recordatorio de cita";
        } else if (msgUpper.contains("AGENDADA") || msgUpper.contains("RESERVA") || msgUpper.contains("CÓDIGO CI-") || msgUpper.contains("CODIGO CI-") || tipoUpper.contains("RESERVA")) {
            cat = "Reserva registrada";
        } else if (tipoUpper.contains("CITA")) {
            cat = "Recordatorio de cita";
        } else if (rawTipo != null && !rawTipo.trim().isEmpty()) {
            cat = rawTipo;
        }
        m.put("tipo", rawTipo != null ? rawTipo : "Citas");
        m.put("categoria", cat);

        if (!leida && "Recordatorio de cita".equalsIgnoreCase(cat)) {
            m.put("accion", "Confirmar");
        } else {
            m.put("accion", "Ver");
        }

        return m;
    }

    public List<Map<String, Object>> obtenerNotificacionesUsuario(int idUsuario) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id_notificacion, id_cita, tipo, mensaje, fecha_envio, leida " +
                     "FROM notificacion WHERE id_usuario = ? ORDER BY fecha_envio DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearNotificacionFila(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (lista.isEmpty()) {
            sincronizarNotificacionesIniciales(idUsuario);
            return obtenerNotificacionesUsuarioDirecto(idUsuario);
        }
        return lista;
    }

    private List<Map<String, Object>> obtenerNotificacionesUsuarioDirecto(int idUsuario) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id_notificacion, id_cita, tipo, mensaje, fecha_envio, leida " +
                     "FROM notificacion WHERE id_usuario = ? ORDER BY fecha_envio DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearNotificacionFila(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public void sincronizarNotificacionesIniciales(int idUsuario) {
        String sqlIns = "INSERT INTO notificacion (id_usuario, id_cita, tipo, canal, mensaje, fecha_envio, estado_envio, leida) VALUES (?, ?, ?, 'PLATAFORMA', ?, ?, 'ENVIADA', ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlIns)) {
            ps.setInt(1, idUsuario);
            ps.setNull(2, java.sql.Types.INTEGER);
            ps.setString(3, "Recordatorio de cita");
            ps.setString(4, "Confirma tu cita del mié 14 oct, 16:00 · enviado por web y WhatsApp");
            ps.setTimestamp(5, Timestamp.valueOf("2026-10-12 16:00:00"));
            ps.setInt(6, 0);
            ps.addBatch();

            ps.setInt(1, idUsuario);
            ps.setNull(2, java.sql.Types.INTEGER);
            ps.setString(3, "Lista de espera");
            ps.setString(4, "Registro en lista de espera de Dermatología confirmado");
            ps.setTimestamp(5, Timestamp.valueOf("2026-10-05 10:12:00"));
            ps.setInt(6, 0);
            ps.addBatch();

            ps.setInt(1, idUsuario);
            ps.setNull(2, java.sql.Types.INTEGER);
            ps.setString(3, "Reserva registrada");
            ps.setString(4, "Cita de Cardiología del jue 15 oct, 09:00 · código CI-2026-004821");
            ps.setTimestamp(5, Timestamp.valueOf("2026-10-03 18:40:00"));
            ps.setInt(6, 1);
            ps.addBatch();

            ps.executeBatch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int contarNotificacionesSinLeer(int idUsuario) {
        if (idUsuario <= 0) return 0;
        String sql = "SELECT COUNT(*) FROM notificacion WHERE id_usuario = ? AND leida = 0";
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sqlCheck = "SELECT COUNT(*) FROM notificacion WHERE id_usuario = ?";
            try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
                psCheck.setInt(1, idUsuario);
                try (ResultSet rsCheck = psCheck.executeQuery()) {
                    if (rsCheck.next() && rsCheck.getInt(1) == 0) {
                        sincronizarNotificacionesIniciales(idUsuario);
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idUsuario);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean marcarNotificacionesComoLeidas(int idUsuario) {
        String sql = "UPDATE notificacion SET leida = 1, estado_envio = 'LEIDA' WHERE id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean marcarNotificacionIndividualLeida(int idNotificacion, int idUsuario) {
        String sql = "UPDATE notificacion SET leida = 1, estado_envio = 'LEIDA' WHERE id_notificacion = ? AND id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idNotificacion);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean confirmarRecordatorioCita(int idNotificacion, int idUsuario) {
        String sql = "UPDATE notificacion SET leida = 1, estado_envio = 'LEIDA' WHERE id_notificacion = ? AND id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idNotificacion);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
