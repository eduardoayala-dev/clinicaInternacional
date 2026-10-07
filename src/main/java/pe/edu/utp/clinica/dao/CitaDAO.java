package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CitaDAO {

    public List<Map<String, Object>> obtenerEspecialidades() {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id_especialidad, nombre FROM especialidad WHERE estado = 'ACTIVO'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getInt("id_especialidad"));
                map.put("nombre", rs.getString("nombre"));
                lista.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<Map<String, Object>> obtenerSedes() {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id_sede, nombre, direccion FROM sede WHERE estado = 'ACTIVO'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getInt("id_sede"));
                map.put("nombre", rs.getString("nombre"));
                map.put("direccion", rs.getString("direccion"));
                lista.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<Map<String, Object>> obtenerMedicosDisponibles(int idEspecialidad, int idSede) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT m.id_medico, u.nombres, u.apellidos, m.cmp " +
                     "FROM medico m " +
                     "INNER JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "WHERE m.id_especialidad = ? AND m.id_sede = ? AND m.estado = 'ACTIVO' AND u.activo = 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEspecialidad);
            ps.setInt(2, idSede);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    String nombres = rs.getString("nombres");
                    String apellidos = rs.getString("apellidos");
                    String cmp = rs.getString("cmp");

                    String nombreCompleto = ((nombres != null ? nombres : "") + " " + (apellidos != null ? apellidos : "")).trim();
                    if (nombreCompleto.isEmpty()) nombreCompleto = "Dr. Médico Especialista";
                    if (cmp == null || cmp.trim().isEmpty()) cmp = "CMP-48291";

                    map.put("idMedico", rs.getInt("id_medico"));
                    map.put("nombreCompleto", nombreCompleto);
                    map.put("nombres", nombreCompleto);
                    map.put("cmp", cmp);
                    map.put("CMP", cmp);
                    lista.add(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<Map<String, Object>> obtenerMedicosPorEspecialidad(int idEspecialidad) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT m.id_medico, u.nombres, u.apellidos, m.cmp " +
                     "FROM medico m " +
                     "INNER JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "WHERE m.id_especialidad = ? AND m.estado = 'ACTIVO' AND u.activo = 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEspecialidad);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    String nombres = rs.getString("nombres");
                    String apellidos = rs.getString("apellidos");
                    String cmp = rs.getString("cmp");

                    String nombreCompleto = ((nombres != null ? nombres : "") + " " + (apellidos != null ? apellidos : "")).trim();
                    if (nombreCompleto.isEmpty()) nombreCompleto = "Dr. Médico Especialista";
                    if (cmp == null || cmp.trim().isEmpty()) cmp = "CMP-48291";

                    map.put("idMedico", rs.getInt("id_medico"));
                    map.put("nombreCompleto", nombreCompleto);
                    map.put("nombres", nombreCompleto);
                    map.put("cmp", cmp);
                    map.put("CMP", cmp);
                    lista.add(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<String> obtenerFechasDisponibles(int idMedico) {
        List<String> fechas = new ArrayList<>();
        String sql = "SELECT DISTINCT fecha FROM disponibilidad " +
                     "WHERE id_medico = ? AND estado = 'PUBLICADO' AND fecha >= CURDATE() " +
                     "ORDER BY fecha ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMedico);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    fechas.add(rs.getDate("fecha").toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fechas;
    }

    public List<String> obtenerHorariosDisponibles(int idMedico, String fecha) {
        List<String> horariosDisponibles = new ArrayList<>();
        
        String sqlDisponibilidad = "SELECT hora_inicio, hora_fin, duracion_minutos FROM disponibilidad " +
                                   "WHERE id_medico = ? AND fecha = ? AND estado = 'PUBLICADO'";
        
        java.util.Set<java.time.LocalTime> todosLosHorarios = new java.util.TreeSet<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlDisponibilidad)) {
            ps.setInt(1, idMedico);
            ps.setString(2, fecha);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.time.LocalTime inicio = rs.getTime("hora_inicio").toLocalTime();
                    java.time.LocalTime fin = rs.getTime("hora_fin").toLocalTime();
                    int duracion = rs.getInt("duracion_minutos");
                    if (duracion <= 0) duracion = 30;
                    
                    java.time.LocalTime actual = inicio;
                    while (actual.plusMinutes(duracion).isBefore(fin) || actual.plusMinutes(duracion).equals(fin)) {
                        todosLosHorarios.add(actual);
                        actual = actual.plusMinutes(duracion);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return horariosDisponibles;
        }

        // 2. Obtener las citas ya reservadas para ese médico en esa fecha
        List<java.time.LocalTime> horariosOcupados = new ArrayList<>();
        String sqlCitas = "SELECT TIME(fecha_hora) as hora FROM cita " +
                          "WHERE id_medico = ? AND DATE(fecha_hora) = ? AND estado NOT IN ('CANCELADA', 'REPROGRAMADA')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlCitas)) {
            ps.setInt(1, idMedico);
            ps.setString(2, fecha);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    horariosOcupados.add(rs.getTime("hora").toLocalTime());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. Filtrar y formatear a String (HH:mm)
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        for (java.time.LocalTime t : todosLosHorarios) {
            if (!horariosOcupados.contains(t)) {
                horariosDisponibles.add(t.format(formatter));
            }
        }
        
        return horariosDisponibles;
    }

    /**
     * Motor Heurístico de IA para el MVP:
     * Calcula la probabilidad de que un paciente cancele su próxima cita 
     * basado en su historial de inasistencias en la tabla cita.
     */
    private String calcularRiesgoCancelacion(int idPaciente) throws java.sql.SQLException {
        String sqlTotal = "SELECT COUNT(*) FROM cita WHERE id_paciente = ?";
        String sqlFaltas = "SELECT COUNT(*) FROM cita WHERE id_paciente = ? AND estado IN ('CANCELADA', 'NO_ASISTIO')";
        
        int total = 0;
        int faltas = 0;
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlTotal)) {
                ps.setInt(1, idPaciente);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) total = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlFaltas)) {
                ps.setInt(1, idPaciente);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) faltas = rs.getInt(1);
                }
            }
        }
        
        if (total == 0) return "SIN_HISTORIAL";
        
        double ratio = (double) faltas / total;
        if (ratio >= 0.3) return "ALTO";      // >= 30% de faltas = ALTO RIESGO
        if (ratio >= 0.1) return "MEDIO";     // >= 10% de faltas = MEDIO RIESGO
        return "BAJO";
    }

    /**
     * Registra la cita y aplica el Motor de Predicción
     */
    public boolean reservarCita(int idPaciente, int idMedico, String fecha, String hora, String nivelUrgencia) {
        String sqlDisp = "SELECT id_disponibilidad FROM disponibilidad WHERE id_medico = ? AND estado = 'ACTIVO' LIMIT 1"; 
        String sqlInsert = "INSERT INTO cita (id_paciente, id_medico, id_disponibilidad, fecha_hora, estado, riesgo_cancelacion, nivel_urgencia) VALUES (?, ?, ?, ?, 'CONFIRMADA', ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            int idDisp = 1; // Fallback
            try (PreparedStatement ps = conn.prepareStatement(sqlDisp)) {
                ps.setInt(1, idMedico);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        idDisp = rs.getInt("id_disponibilidad");
                    }
                }
            }
            
            String riesgo = calcularRiesgoCancelacion(idPaciente);
            String fechaHoraStr = fecha + " " + hora + ":00";
            
            try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
                ps.setInt(1, idPaciente);
                ps.setInt(2, idMedico);
                ps.setInt(3, idDisp);
                ps.setString(4, fechaHoraStr);
                ps.setString(5, riesgo);
                ps.setString(6, nivelUrgencia != null ? nivelUrgencia : "BAJA");
                
                int filas = ps.executeUpdate();
                return filas > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Dashboard: Obtiene citas recientes incluyendo su nivel de riesgo
     */
    public List<Map<String, Object>> obtenerCitasConRiesgo() {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT c.id_cita, c.fecha_hora, c.estado, c.nivel_urgencia, c.riesgo_cancelacion, " +
                     "p.nombres, p.apellidos, m.nombres as med_nombres, m.apellidos as med_apellidos " +
                     "FROM cita c " +
                     "JOIN paciente p ON c.id_paciente = p.id_paciente " +
                     "JOIN medico m ON c.id_medico = m.id_medico " +
                     "ORDER BY c.fecha_hora DESC LIMIT 50";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("idCita", rs.getInt("id_cita"));
                map.put("fechaHora", rs.getTimestamp("fecha_hora").toString());
                map.put("estado", rs.getString("estado"));
                map.put("nivelUrgencia", rs.getString("nivel_urgencia"));
                map.put("riesgoCancelacion", rs.getString("riesgo_cancelacion"));
                map.put("paciente", rs.getString("nombres") + " " + rs.getString("apellidos"));
                map.put("medico", rs.getString("med_nombres") + " " + rs.getString("med_apellidos"));
                lista.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Ejecuta el Algoritmo de Reasignación Inteligente Real
     */
    public String ejecutarReasignacionInteligente(int idCitaOriginal) {
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false); // Transacción para asegurar consistencia

            // 1. Obtener los datos de la cita original y la especialidad del médico
            String sqlInfoCita = "SELECT c.id_medico, c.id_disponibilidad, c.fecha_hora, m.id_especialidad " +
                                 "FROM cita c JOIN medico m ON c.id_medico = m.id_medico WHERE c.id_cita = ?";
            int idMedico = 0, idDisp = 0, idEspecialidad = 0;
            java.sql.Timestamp fechaHora = null;

            try (PreparedStatement psInfo = conn.prepareStatement(sqlInfoCita)) {
                psInfo.setInt(1, idCitaOriginal);
                try (ResultSet rsInfo = psInfo.executeQuery()) {
                    if (rsInfo.next()) {
                        idMedico = rsInfo.getInt("id_medico");
                        idDisp = rsInfo.getInt("id_disponibilidad");
                        fechaHora = rsInfo.getTimestamp("fecha_hora");
                        idEspecialidad = rsInfo.getInt("id_especialidad");
                    } else {
                        return "Error: No se encontró la cita original.";
                    }
                }
            }

            // 2. Buscar al paciente ideal en lista_espera (Prioridad: Urgencia -> Antigüedad)
            String sqlBusqueda = "SELECT id_lista_espera, id_paciente FROM lista_espera " +
                                 "WHERE id_especialidad = ? AND estado = 'ACTIVA' " +
                                 "ORDER BY CASE nivel_urgencia WHEN 'ALTA' THEN 1 WHEN 'MEDIA' THEN 2 WHEN 'BAJA' THEN 3 END ASC, " +
                                 "fecha_registro ASC LIMIT 1";
            int idListaEspera = 0, idNuevoPaciente = 0;

            try (PreparedStatement psBuscar = conn.prepareStatement(sqlBusqueda)) {
                psBuscar.setInt(1, idEspecialidad);
                try (ResultSet rsBuscar = psBuscar.executeQuery()) {
                    if (rsBuscar.next()) {
                        idListaEspera = rsBuscar.getInt("id_lista_espera");
                        idNuevoPaciente = rsBuscar.getInt("id_paciente");
                    }
                }
            }

            if (idNuevoPaciente == 0) {
                conn.rollback();
                return "No hay pacientes en la lista de espera para esta especialidad.";
            }

            // 3. Cancelar la cita original (liberar el cupo)
            String sqlCancelar = "UPDATE cita SET estado = 'CANCELADA' WHERE id_cita = ?";
            try (PreparedStatement psCancelar = conn.prepareStatement(sqlCancelar)) {
                psCancelar.setInt(1, idCitaOriginal);
                psCancelar.executeUpdate();
            }

            // 4. Crear la nueva cita para el paciente de la lista de espera
            String sqlNuevaCita = "INSERT INTO cita (id_paciente, id_medico, id_disponibilidad, fecha_hora, estado, riesgo_cancelacion, nivel_urgencia) " +
                                  "VALUES (?, ?, ?, ?, 'CONFIRMADA', 'BAJO', 'ALTA')";
            int idNuevaCita = 0;
            try (PreparedStatement psNueva = conn.prepareStatement(sqlNuevaCita, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                psNueva.setInt(1, idNuevoPaciente);
                psNueva.setInt(2, idMedico);
                psNueva.setInt(3, idDisp);
                psNueva.setTimestamp(4, fechaHora);
                psNueva.executeUpdate();
                try (ResultSet rsKeys = psNueva.getGeneratedKeys()) {
                    if (rsKeys.next()) {
                        idNuevaCita = rsKeys.getInt(1);
                    }
                }
            }

            // 5. Actualizar la lista de espera
            String sqlActualizarLista = "UPDATE lista_espera SET estado = 'OFERTADA' WHERE id_lista_espera = ?";
            try (PreparedStatement psUpdateLista = conn.prepareStatement(sqlActualizarLista)) {
                psUpdateLista.setInt(1, idListaEspera);
                psUpdateLista.executeUpdate();
            }

            // 6. Generar Notificación para el nuevo paciente
            String sqlNotificacion = "INSERT INTO notificacion (id_usuario, id_cita, tipo, mensaje, estado) " +
                                     "VALUES ((SELECT id_usuario FROM paciente WHERE id_paciente = ?), ?, 'REASIGNACION', " +
                                     "'¡Cupo liberado! Hemos reasignado una cita para ti el " + fechaHora.toString() + "', 'NO_LEIDO')";
            try (PreparedStatement psNotif = conn.prepareStatement(sqlNotificacion)) {
                psNotif.setInt(1, idNuevoPaciente);
                psNotif.setInt(2, idNuevaCita);
                psNotif.executeUpdate();
            }

            conn.commit();
            return "Reasignación Real Exitosa: El cupo fue asignado al paciente #" + idNuevoPaciente + " de la lista de espera.";

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ex) {}
            }
            e.printStackTrace();
            return "Error en algoritmo de reasignación real: " + e.getMessage();
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ex) {}
            }
        }
    }

}
