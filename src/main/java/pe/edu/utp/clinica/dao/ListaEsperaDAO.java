package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * DAO para gestión de la lista de espera ambulatoria y ofertas de cupos recuperados.
 */
public class ListaEsperaDAO {

    private final NotificacionDAO notificacionDAO = new NotificacionDAO();

    public boolean inscribirListaEspera(int idPaciente, int idEspecialidad, Integer idSede, String rangoHorario, String urgenciaDisponibilidad, String canalesNotificacion) {
        String sqlCancelar = "UPDATE lista_espera SET estado = 'CANCELADA' WHERE id_paciente = ? AND estado IN ('ACTIVA', 'OFERTADA')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement psCanc = conn.prepareStatement(sqlCancelar)) {
            psCanc.setInt(1, idPaciente);
            psCanc.executeUpdate();
        } catch (Exception ignored) {}

        int sedeIdFinal = (idSede != null && idSede > 0) ? idSede : 1;
        String sqlInsert = "INSERT INTO lista_espera (id_paciente, id_especialidad, id_sede, nivel_urgencia, rango_horario, canal_notificacion, estado, posicion) " +
                           "VALUES (?, ?, ?, ?, ?, ?, 'ACTIVA', 1)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
            ps.setInt(1, idPaciente);
            ps.setInt(2, idEspecialidad);
            ps.setInt(3, sedeIdFinal);
            ps.setString(4, urgenciaDisponibilidad != null && !urgenciaDisponibilidad.trim().isEmpty() ? urgenciaDisponibilidad : "MEDIA");
            ps.setString(5, rangoHorario != null && !rangoHorario.trim().isEmpty() ? rangoHorario : "MANANA");
            ps.setString(6, canalesNotificacion != null && !canalesNotificacion.trim().isEmpty() ? canalesNotificacion : "WHATSAPP");
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean retirarListaEspera(int idListaEspera, int idPaciente) {
        String sql = "UPDATE lista_espera SET estado = 'CANCELADA' WHERE id_lista_espera = ? AND id_paciente = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idListaEspera);
            ps.setInt(2, idPaciente);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Map<String, Object> obtenerEstadoListaEspera(int idPaciente) {
        String sql = "SELECT le.id_lista_espera, le.estado, le.rango_horario, le.canal_notificacion, " +
                     "e.nombre AS especialidad, s.nombre AS sede " +
                     "FROM lista_espera le " +
                     "JOIN especialidad e ON le.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN sede s ON le.id_sede = s.id_sede " +
                     "WHERE le.id_paciente = ? AND le.estado IN ('ACTIVA', 'OFERTADA') " +
                     "ORDER BY le.fecha_registro DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("idListaEspera", rs.getInt("id_lista_espera"));
                    String estado = rs.getString("estado");
                    map.put("estadoCodigo", estado != null ? estado.toUpperCase() : "ACTIVA");
                    map.put("estado", "OFERTADA".equalsIgnoreCase(estado) ? "Oferta pendiente de respuesta" : "En fila activa");
                    map.put("especialidad", rs.getString("especialidad"));
                    String sede = rs.getString("sede");
                    map.put("sede", (sede != null && !sede.trim().isEmpty()) ? sede : "Sede San Borja");
                    String rango = rs.getString("rango_horario");
                    String canal = rs.getString("canal_notificacion");

                    String rangoStr = "Mañana";
                    if (rango != null) {
                        String rLow = rango.toLowerCase();
                        if (rLow.contains("tarde") && (rLow.contains("manana") || rLow.contains("mañana"))) {
                            rangoStr = "Mañana / Tarde";
                        } else if (rLow.contains("tarde")) {
                            rangoStr = "Tarde";
                        } else if (rLow.contains("manana") || rLow.contains("mañana")) {
                            rangoStr = "Mañana";
                        }
                    }

                    String canalStr = "SMS / WhatsApp";
                    if (canal != null) {
                        String cUp = canal.toUpperCase();
                        if (cUp.contains("CORREO") && (cUp.contains("SMS") || cUp.contains("WHATSAPP"))) {
                            canalStr = "SMS / WhatsApp · Correo";
                        } else if (cUp.contains("CORREO")) {
                            canalStr = "Correo electrónico";
                        } else {
                            canalStr = "SMS / WhatsApp";
                        }
                    }

                    map.put("preferencias", rangoStr + " · " + canalStr);
                    return map;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<String, Object> obtenerOfertaCupoAnticipado(int idPaciente) {
        String sql = "SELECT le.id_lista_espera, e.nombre AS especialidad, s.nombre AS sede, " +
                     "u.nombres AS medico_nombres, u.apellidos AS medico_apellidos " +
                     "FROM lista_espera le " +
                     "JOIN especialidad e ON le.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN sede s ON le.id_sede = s.id_sede " +
                     "LEFT JOIN medico m ON le.id_especialidad = m.id_especialidad " +
                     "LEFT JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "WHERE le.id_paciente = ? AND le.estado = 'OFERTADA' " +
                     "ORDER BY le.fecha_registro DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("idListaEspera", rs.getInt("id_lista_espera"));
                    String esp = rs.getString("especialidad");
                    map.put("especialidad", esp != null ? esp : "Dermatología");
                    String medNom = rs.getString("medico_nombres");
                    String medApe = rs.getString("medico_apellidos");
                    map.put("medico", (medNom != null ? "Dr. " + medNom + " " + (medApe != null ? medApe : "") : "Dr. Jorge Ríos"));
                    String sede = rs.getString("sede");
                    String sedeStr = (sede != null && !sede.trim().isEmpty()) ? sede : "Sede San Borja";
                    map.put("sede", sedeStr + " · Consultorio 215");
                    map.put("fechaOfrecida", "Mar 13 oct 2026 · 10:20");
                    map.put("tiempoRestante", "14:32 min");
                    map.put("tiempoRestanteSegundos", 872);
                    map.put("origen", "Origen: cupo liberado por cancelación de otro paciente.");
                    return map;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean aceptarOfertaCupo(int idPaciente, int idListaEspera, int idUsuario) {
        String sqlWaitlist = "UPDATE lista_espera SET estado = 'ATENDIDA' WHERE id_paciente = ? AND (id_lista_espera = ? OR ? <= 0) AND estado = 'OFERTADA'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlWaitlist)) {
            ps.setInt(1, idPaciente);
            ps.setInt(2, idListaEspera);
            ps.setInt(3, idListaEspera);
            ps.executeUpdate();

            notificacionDAO.crearNotificacion(idUsuario, null, "Reserva registrada", "Oferta aceptada: Cita de Dermatología para el Mar 13 oct 2026 · 10:20 confirmada por cupo recuperado.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean rechazarOfertaCupo(int idPaciente, int idListaEspera, int idUsuario) {
        String sqlWaitlist = "UPDATE lista_espera SET estado = 'ACTIVA' WHERE id_paciente = ? AND (id_lista_espera = ? OR ? <= 0) AND estado = 'OFERTADA'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlWaitlist)) {
            ps.setInt(1, idPaciente);
            ps.setInt(2, idListaEspera);
            ps.setInt(3, idListaEspera);
            ps.executeUpdate();

            notificacionDAO.crearNotificacion(idUsuario, null, "Lista de espera", "Has rechazado la oferta de cupo recuperado. Sigues en la lista de espera con tu misma posición.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
