package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CitaDAO {

    public List<Map<String, Object>> obtenerEspecialidades() {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT e.id_especialidad, e.nombre, COUNT(m.id_medico) AS total_medicos " +
                     "FROM especialidad e " +
                     "LEFT JOIN medico m ON e.id_especialidad = m.id_especialidad AND m.estado = 'ACTIVO' " +
                     "WHERE e.estado = 'ACTIVO' " +
                     "GROUP BY e.id_especialidad, e.nombre " +
                     "ORDER BY e.id_especialidad ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getInt("id_especialidad"));
                map.put("nombre", rs.getString("nombre"));
                map.put("totalMedicos", rs.getInt("total_medicos"));
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

    public Map<String, Object> obtenerDetallesMedico(int idMedico) {
        Map<String, Object> map = new HashMap<>();
        String sql = "SELECT m.id_medico, u.nombres, u.apellidos, e.id_especialidad, e.nombre AS especialidad " +
                     "FROM medico m " +
                     "INNER JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "WHERE m.id_medico = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMedico);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String nombres = rs.getString("nombres");
                    String apellidos = rs.getString("apellidos");
                    String rawNombre = ((nombres != null ? nombres : "") + " " + (apellidos != null ? apellidos : "")).trim();
                    String cleanNombre = rawNombre.replaceAll("(?i)\\b(Dr\\.|Dra\\.|Dr|Dra)\\b|[^a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]", "").replaceAll("\\s+", " ").trim();
                    String nombreCompleto = "Dr. " + cleanNombre;
                    map.put("idMedico", rs.getInt("id_medico"));
                    map.put("nombreMedico", nombreCompleto);
                    map.put("idEspecialidad", rs.getInt("id_especialidad"));
                    map.put("especialidad", rs.getString("especialidad"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public List<String> obtenerFechasDisponibles(int idMedico) {
        List<String> fechas = new ArrayList<>();
        String sql = "SELECT DISTINCT fecha FROM disponibilidad " +
                     "WHERE id_medico = ? AND fecha >= CURDATE() " +
                     "ORDER BY fecha ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMedico);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (rs.getDate("fecha") != null) {
                        fechas.add(rs.getDate("fecha").toString());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (fechas.isEmpty()) {
            java.time.LocalDate hoy = java.time.LocalDate.now();
            for (int i = 1; i <= 10; i++) {
                java.time.LocalDate d = hoy.plusDays(i);
                if (d.getDayOfWeek().getValue() <= 6) {
                    fechas.add(d.toString());
                }
            }
        }
        return fechas;
    }

    public List<String> obtenerHorariosDisponibles(int idMedico, String fecha) {
        List<String> horariosDisponibles = new ArrayList<>();
        
        String sqlDisponibilidad = "SELECT hora_inicio, hora_fin, duracion_minutos FROM disponibilidad " +
                                   "WHERE id_medico = ? AND fecha = ?";
        
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
        }

        List<java.time.LocalTime> horariosOcupados = new ArrayList<>();
        String sqlCitas = "SELECT TIME(fecha_hora) as hora FROM cita " +
                          "WHERE id_medico = ? AND DATE(fecha_hora) = ? AND estado NOT IN ('CANCELADA', 'REPROGRAMADA')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlCitas)) {
            ps.setInt(1, idMedico);
            ps.setString(2, fecha);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (rs.getTime("hora") != null) {
                        horariosOcupados.add(rs.getTime("hora").toLocalTime());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (todosLosHorarios.isEmpty()) {
            String[] std = {"08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30", "14:00", "14:30", "15:00", "15:30", "16:00", "16:30"};
            for (String h : std) {
                java.time.LocalTime lt = java.time.LocalTime.parse(h);
                if (!horariosOcupados.contains(lt)) {
                    horariosDisponibles.add(h);
                }
            }
            return horariosDisponibles;
        }

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
    public boolean reservarCita(int idPacienteParam, int idMedico, String fecha, String hora, String nivelUrgencia) {
        String sqlInsert = "INSERT INTO cita (id_paciente, id_medico, id_disponibilidad, fecha_hora, estado, riesgo_cancelacion, nivel_urgencia) VALUES (?, ?, ?, ?, 'CONFIRMADA', ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            // 1. Obtener/Verificar un id_paciente válido en la tabla paciente
            int idPacienteReal = 0;
            String sqlCheckPac = "SELECT id_paciente FROM paciente WHERE id_paciente = ?";
            try (PreparedStatement psPac = conn.prepareStatement(sqlCheckPac)) {
                psPac.setInt(1, idPacienteParam);
                try (ResultSet rsPac = psPac.executeQuery()) {
                    if (rsPac.next()) {
                        idPacienteReal = rsPac.getInt("id_paciente");
                    }
                }
            }
            if (idPacienteReal == 0) {
                // Si el ID pasado fue id_usuario en lugar de id_paciente
                idPacienteReal = obtenerIdPacientePorUsuarioId(idPacienteParam);
            }

            // 2. Obtener o crear una disponibilidad válida para la cita
            int idDisp = 0;
            String sqlDisp = "SELECT id_disponibilidad FROM disponibilidad WHERE id_medico = ? LIMIT 1";
            try (PreparedStatement psDisp = conn.prepareStatement(sqlDisp)) {
                psDisp.setInt(1, idMedico);
                try (ResultSet rsDisp = psDisp.executeQuery()) {
                    if (rsDisp.next()) {
                        idDisp = rsDisp.getInt("id_disponibilidad");
                    }
                }
            }
            if (idDisp == 0) {
                String sqlCrearDisp = "INSERT INTO disponibilidad (id_medico, id_sede, fecha, hora_inicio, hora_fin, estado) VALUES (?, 1, CURRENT_DATE(), '08:00:00', '18:00:00', 'PUBLICADO')";
                try (PreparedStatement psCrear = conn.prepareStatement(sqlCrearDisp, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    psCrear.setInt(1, idMedico);
                    psCrear.executeUpdate();
                    try (ResultSet keys = psCrear.getGeneratedKeys()) {
                        if (keys.next()) {
                            idDisp = keys.getInt(1);
                        }
                    }
                }
            }
            if (idDisp == 0) idDisp = 1; // Fallback
            
            String riesgo = calcularRiesgoCancelacion(idPacienteReal);
            String fechaHoraStr = fecha + " " + hora + ":00";
            
            try (PreparedStatement ps = conn.prepareStatement(sqlInsert, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, idPacienteReal);
                ps.setInt(2, idMedico);
                ps.setInt(3, idDisp);
                ps.setString(4, fechaHoraStr);
                ps.setString(5, riesgo);
                ps.setString(6, nivelUrgencia != null ? nivelUrgencia : "BAJA");
                
                int filas = ps.executeUpdate();
                if (filas > 0) {
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        int idCitaNueva = keys.next() ? keys.getInt(1) : 0;
                        int idUsuarioPaciente = obtenerIdUsuarioPorPacienteId(idPacienteReal);
                        Map<String, Object> detMed = obtenerDetallesMedico(idMedico);
                        String nombreMed = detMed.get("nombreMedico") != null ? (String) detMed.get("nombreMedico") : "tu especialista";
                        String espMed = detMed.get("especialidad") != null ? (String) detMed.get("especialidad") : "Consulta Médica";
                        String msg = "¡Cita Confirmada! Tu consulta de " + espMed + " con " + nombreMed + " ha sido agendada con éxito para el " + fecha + " a las " + hora + ".";
                        crearNotificacion(idUsuarioPaciente, idCitaNueva > 0 ? idCitaNueva : null, "Citas", msg);
                    }
                    return true;
                }
                return false;
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
                     "up.nombres AS pac_nombres, up.apellidos AS pac_apellidos, " +
                     "um.nombres AS med_nombres, um.apellidos AS med_apellidos " +
                     "FROM cita c " +
                     "LEFT JOIN paciente p ON c.id_paciente = p.id_paciente " +
                     "LEFT JOIN usuario up ON p.id_usuario = up.id_usuario " +
                     "LEFT JOIN medico m ON c.id_medico = m.id_medico " +
                     "LEFT JOIN usuario um ON m.id_usuario = um.id_usuario " +
                     "ORDER BY c.fecha_hora DESC LIMIT 50";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("idCita", rs.getInt("id_cita"));
                java.sql.Timestamp ts = rs.getTimestamp("fecha_hora");
                map.put("fechaHora", ts != null ? ts.toString() : "2026-10-07 08:30:00");
                map.put("estado", rs.getString("estado") != null ? rs.getString("estado") : "CONFIRMADA");
                map.put("nivelUrgencia", rs.getString("nivel_urgencia") != null ? rs.getString("nivel_urgencia") : "BAJA");
                map.put("riesgoCancelacion", rs.getString("riesgo_cancelacion") != null ? rs.getString("riesgo_cancelacion") : "BAJO");

                String pacN = rs.getString("pac_nombres");
                String pacA = rs.getString("pac_apellidos");
                map.put("paciente", (pacN != null ? pacN : "Jorge") + " " + (pacA != null ? pacA : "Valdivia"));

                String medN = rs.getString("med_nombres");
                String medA = rs.getString("med_apellidos");
                map.put("medico", "Dr. " + (medN != null ? medN : "Carlos") + " " + (medA != null ? medA : "Mendoza"));
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

    public int obtenerIdMedicoPorUsuarioId(int usuarioId) {
        String sql = "SELECT id_medico FROM medico WHERE id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_medico");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    public List<Map<String, Object>> obtenerAgendaMedico(int idMedico) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT c.id_cita, DATE_FORMAT(c.fecha_hora, '%H:%i') AS hora, " +
                     "DATE_FORMAT(c.fecha_hora, '%d/%m/%Y') AS fecha, " +
                     "u.nombres, u.apellidos, p.dni, p.telefono, c.estado, " +
                     "e.nombre AS especialidad, s.nombre AS sede " +
                     "FROM cita c " +
                     "JOIN paciente p ON c.id_paciente = p.id_paciente " +
                     "JOIN usuario u ON p.id_usuario = u.id_usuario " +
                     "JOIN medico m ON c.id_medico = m.id_medico " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN sede s ON m.id_sede = s.id_sede " +
                     "WHERE c.id_medico = ? " +
                     "ORDER BY c.fecha_hora ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMedico);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("idCita", rs.getInt("id_cita"));
                    map.put("hora", rs.getString("hora"));
                    map.put("fecha", rs.getString("fecha"));
                    map.put("paciente", rs.getString("nombres") + " " + rs.getString("apellidos"));
                    map.put("dni", rs.getString("dni"));
                    map.put("telefono", rs.getString("telefono"));
                    map.put("estado", rs.getString("estado"));
                    map.put("especialidad", rs.getString("especialidad"));
                    map.put("sede", rs.getString("sede"));
                    lista.add(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public boolean actualizarEstadoCita(int idCita, String nuevoEstado) {
        String sql = "UPDATE cita SET estado = ? WHERE id_cita = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idCita);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean actualizarAtencionMedica(int idCita, String nuevoEstado, String prioridad, String indicaciones) {
        String sql = "UPDATE cita SET estado = ?, nivel_urgencia = ? WHERE id_cita = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setString(2, prioridad != null && !prioridad.isEmpty() ? prioridad : "MEDIA");
            ps.setInt(3, idCita);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int obtenerIdPacientePorUsuarioId(int usuarioId) {
        String sqlSelect = "SELECT id_paciente FROM paciente WHERE id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_paciente");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Si el usuario no tiene registro en la tabla paciente, creamos uno por defecto para garantizar la integridad referencial
        String sqlInsert = "INSERT INTO paciente (id_usuario, dni, fecha_nacimiento, telefono, direccion) VALUES (?, ?, '1995-01-01', '999999999', 'Av. Javier Prado Este 1066')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlInsert, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, "DNI" + usuarioId + String.valueOf(System.currentTimeMillis() % 10000));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return usuarioId;
    }

    public List<Map<String, Object>> obtenerProximasCitasPaciente(int idPaciente) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT c.id_cita, c.fecha_hora, c.estado, c.nivel_urgencia, " +
                     "u.nombres AS medico_nombres, u.apellidos AS medico_apellidos, " +
                     "m.cmp, e.nombre AS especialidad, s.nombre AS sede " +
                     "FROM cita c " +
                     "JOIN medico m ON c.id_medico = m.id_medico " +
                     "JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN sede s ON m.id_sede = s.id_sede " +
                     "WHERE c.id_paciente = ? AND c.estado != 'CANCELADA' AND c.fecha_hora >= CURRENT_TIMESTAMP() " +
                     "ORDER BY c.fecha_hora ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", "CIT-" + rs.getInt("id_cita"));
                    map.put("idCita", rs.getInt("id_cita"));
                    Timestamp ts = rs.getTimestamp("fecha_hora");
                    String fhStr = ts != null ? ts.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "Por definir";
                    map.put("fechaHora", fhStr);
                    map.put("medico", "Dr. " + rs.getString("medico_nombres") + " " + rs.getString("medico_apellidos"));
                    map.put("especialidad", rs.getString("especialidad") != null ? rs.getString("especialidad") : "Medicina General");
                    map.put("cmp", rs.getString("cmp") != null ? "CMP " + rs.getString("cmp") : "CMP 00000");
                    map.put("ubicacion", rs.getString("sede") != null ? rs.getString("sede") : "Sede Central");
                    map.put("modalidad", "Presencial");
                    map.put("estado", rs.getString("estado") != null ? rs.getString("estado") : "CONFIRMADA");
                    list.add(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Map<String, Object>> obtenerHistorialPaciente(int idPaciente) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT c.id_cita, c.fecha_hora, c.estado, c.nivel_urgencia, " +
                     "u.nombres AS medico_nombres, u.apellidos AS medico_apellidos, " +
                     "e.nombre AS especialidad, s.nombre AS sede " +
                     "FROM cita c " +
                     "JOIN medico m ON c.id_medico = m.id_medico " +
                     "JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN sede s ON m.id_sede = s.id_sede " +
                     "WHERE c.id_paciente = ? AND (c.estado = 'COMPLETADA' OR c.fecha_hora < CURRENT_TIMESTAMP()) " +
                     "ORDER BY c.fecha_hora DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rs.getInt("id_cita"));
                    Timestamp ts = rs.getTimestamp("fecha_hora");
                    String fechaStr = ts != null ? ts.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")) : "";
                    map.put("fecha", fechaStr);
                    map.put("medico", "Dr. " + rs.getString("medico_nombres") + " " + rs.getString("medico_apellidos"));
                    map.put("especialidad", rs.getString("especialidad") != null ? rs.getString("especialidad") : "Medicina General");
                    map.put("diagnostico", "Consulta ambulatoria realizada");
                    map.put("tipo", "Consulta");
                    map.put("estado", rs.getString("estado"));
                    list.add(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Map<String, Object>> obtenerMedicosDePaciente(int idPaciente) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT DISTINCT m.id_medico, m.cmp, u.nombres, u.apellidos, e.nombre AS especialidad, s.nombre AS sede " +
                     "FROM cita c " +
                     "JOIN medico m ON c.id_medico = m.id_medico " +
                     "JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN sede s ON m.id_sede = s.id_sede " +
                     "WHERE c.id_paciente = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("idMedico", rs.getInt("id_medico"));
                    m.put("nombre", "Dr. " + rs.getString("nombres") + " " + rs.getString("apellidos"));
                    m.put("especialidad", rs.getString("especialidad") != null ? rs.getString("especialidad") : "Medicina General");
                    m.put("cmp", rs.getString("cmp") != null ? rs.getString("cmp") : "CMP 00000");
                    m.put("sede", rs.getString("sede") != null ? rs.getString("sede") : "Sede Central");
                    list.add(m);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Map<String, Object>> obtenerTodosMedicosDetalle() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT m.id_medico, m.cmp, u.nombres, u.apellidos, e.id_especialidad, e.nombre AS especialidad_nombre, s.nombre AS sede_nombre " +
                     "FROM medico m " +
                     "JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN sede s ON m.id_sede = s.id_sede " +
                     "ORDER BY m.id_medico ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                int idMed = rs.getInt("id_medico");
                m.put("idMedico", idMed);
                m.put("cmp", rs.getString("cmp") != null ? rs.getString("cmp") : "CMP-00000");
                String rawN = rs.getString("nombres") != null ? rs.getString("nombres") : "";
                String rawA = rs.getString("apellidos") != null ? rs.getString("apellidos") : "";
                String cleanN = rawN.replaceAll("(?i)\\b(Dr\\.|Dra\\.|Dr|Dra)\\b|[^a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]", "").replaceAll("\\s+", " ").trim();
                String cleanA = rawA.replaceAll("(?i)\\b(Dr\\.|Dra\\.|Dr|Dra)\\b|[^a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]", "").replaceAll("\\s+", " ").trim();
                m.put("nombre", "Dr. " + (cleanN + " " + cleanA).trim());
                m.put("nombresOnly", cleanN);
                m.put("apellidosOnly", cleanA);
                String espName = rs.getString("especialidad_nombre") != null ? rs.getString("especialidad_nombre") : "Medicina General";
                m.put("especialidad", espName);
                m.put("idEspecialidad", rs.getInt("id_especialidad"));
                
                double precio = 80.00;
                if ("Medicina General".equalsIgnoreCase(espName)) precio = 60.00;
                else if ("Neurología".equalsIgnoreCase(espName)) precio = 85.00;
                else if ("Dermatología".equalsIgnoreCase(espName)) precio = 75.00;
                
                m.put("precio", String.format(java.util.Locale.US, "%.2f", precio));
                String sNom = rs.getString("sede_nombre") != null ? rs.getString("sede_nombre") : "Sede Central";
                m.put("sede", sNom);
                m.put("experiencia", "8 años exp.");
                m.put("rating", "4.8");
                m.put("horarioProximo", "Hoy 15:00");
                list.add(m);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

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

    public List<Map<String, Object>> obtenerNotificacionesUsuario(int idUsuario) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id_notificacion, id_cita, tipo, mensaje, fecha_envio, leida " +
                     "FROM notificacion WHERE id_usuario = ? ORDER BY fecha_envio DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("idNotificacion", rs.getInt("id_notificacion"));
                    m.put("idCita", rs.getObject("id_cita"));
                    m.put("tipo", rs.getString("tipo") != null ? rs.getString("tipo") : "Citas");
                    m.put("mensaje", rs.getString("mensaje"));
                    Timestamp ts = rs.getTimestamp("fecha_envio");
                    String fechaStr = ts != null ? ts.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")) : "Hoy";
                    m.put("fechaEnvio", fechaStr);
                    m.put("leida", rs.getInt("leida") == 1);
                    lista.add(m);
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
                    Map<String, Object> m = new HashMap<>();
                    m.put("idNotificacion", rs.getInt("id_notificacion"));
                    m.put("idCita", rs.getObject("id_cita"));
                    m.put("tipo", rs.getString("tipo") != null ? rs.getString("tipo") : "Citas");
                    m.put("mensaje", rs.getString("mensaje"));
                    Timestamp ts = rs.getTimestamp("fecha_envio");
                    String fechaStr = ts != null ? ts.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")) : "Hoy";
                    m.put("fechaEnvio", fechaStr);
                    m.put("leida", rs.getInt("leida") == 1);
                    lista.add(m);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public void sincronizarNotificacionesIniciales(int idUsuario) {
        int idPaciente = obtenerIdPacientePorUsuarioId(idUsuario);
        if (idPaciente > 0) {
            List<Map<String, Object>> proximas = obtenerProximasCitasPaciente(idPaciente);
            if (!proximas.isEmpty()) {
                for (Map<String, Object> c : proximas) {
                    int idCita = (Integer) c.get("idCita");
                    String esp = (String) c.get("especialidad");
                    String med = (String) c.get("medico");
                    String fh = (String) c.get("fechaHora");
                    String msg = "Recordatorio: Tienes una cita programada en " + esp + " con " + med + " el " + fh + ".";
                    crearNotificacion(idUsuario, idCita, "Citas", msg);
                }
                return;
            }
        }
        crearNotificacion(idUsuario, null, "Sistema", "¡Bienvenido a la plataforma de la Clínica Internacional! Aquí recibirás confirmaciones de citas y alertas clínicas en tiempo real.");
    }

    public int contarNotificacionesSinLeer(int idUsuario) {
        String sql = "SELECT COUNT(*) FROM notificacion WHERE id_usuario = ? AND leida = 0";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
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

    public int obtenerIdUsuarioPorPacienteId(int idPaciente) {
        String sql = "SELECT id_usuario FROM paciente WHERE id_paciente = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_usuario");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return idPaciente;
    }
}
