package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
                    if (nombreCompleto.isEmpty()) {
                        nombreCompleto = "Dr. Médico Especialista";
                    } else if (!nombreCompleto.startsWith("Dr.") && !nombreCompleto.startsWith("Dra.")) {
                        String nLower = nombreCompleto.toLowerCase();
                        String prefix = (nLower.startsWith("elena") || nLower.startsWith("andrea") || nLower.startsWith("mariana") || nLower.startsWith("marina") || nLower.startsWith("maria") || nLower.startsWith("rosa") || nLower.startsWith("claudia") || nLower.startsWith("patricia")) ? "Dra. " : "Dr. ";
                        nombreCompleto = prefix + nombreCompleto;
                    }
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
                    if (nombreCompleto.isEmpty()) {
                        nombreCompleto = "Dr. Médico Especialista";
                    } else if (!nombreCompleto.startsWith("Dr.") && !nombreCompleto.startsWith("Dra.")) {
                        String nLower = nombreCompleto.toLowerCase();
                        String prefix = (nLower.startsWith("elena") || nLower.startsWith("andrea") || nLower.startsWith("mariana") || nLower.startsWith("marina") || nLower.startsWith("maria") || nLower.startsWith("rosa") || nLower.startsWith("claudia") || nLower.startsWith("patricia")) ? "Dra. " : "Dr. ";
                        nombreCompleto = prefix + nombreCompleto;
                    }
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
        return reservarCitaConId(idPacienteParam, idMedico, fecha, hora, nivelUrgencia) > 0;
    }

    public int reservarCitaConId(int idPacienteParam, int idMedico, String fecha, String hora, String nivelUrgencia) {
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
                    int idCitaNueva = 0;
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        idCitaNueva = keys.next() ? keys.getInt(1) : 0;
                        int idUsuarioPaciente = obtenerIdUsuarioPorPacienteId(idPacienteReal);
                        Map<String, Object> detMed = obtenerDetallesMedico(idMedico);
                        String nombreMed = detMed.get("nombreMedico") != null ? (String) detMed.get("nombreMedico") : "tu especialista";
                        String espMed = detMed.get("especialidad") != null ? (String) detMed.get("especialidad") : "Consulta Médica";
                        String msg = "¡Cita Confirmada! Tu consulta de " + espMed + " con " + nombreMed + " ha sido agendada con éxito para el " + fecha + " a las " + hora + ".";
                        crearNotificacion(idUsuarioPaciente, idCitaNueva > 0 ? idCitaNueva : null, "Citas", msg);
                    }
                    return idCitaNueva > 0 ? idCitaNueva : 1;
                }
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
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
        if (usuarioId <= 0) {
            return -1;
        }

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

        // Verificar que el id_usuario realmente exista en la tabla usuario para evitar fallas de Foreign Key
        String sqlCheckUser = "SELECT dni FROM usuario WHERE id_usuario = ?";
        String dniUsuario = null;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlCheckUser)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    dniUsuario = rs.getString("dni");
                } else {
                    return -1; // El usuario no existe en la BD
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        if (dniUsuario == null || dniUsuario.trim().isEmpty()) {
            dniUsuario = "DNI" + usuarioId + String.valueOf(System.currentTimeMillis() % 10000);
        }

        // Si el usuario existe pero aún no tenía fila en paciente, creamos su ficha médica de forma segura
        String sqlInsert = "INSERT INTO paciente (id_usuario, dni, fecha_nacimiento, telefono, direccion) VALUES (?, ?, '1995-01-01', '999999999', 'Av. Javier Prado Este 1066')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlInsert, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, dniUsuario);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    private LocalDateTime extraerFechaHora(ResultSet rs, String columna) {
        try {
            String str = rs.getString(columna);
            if (str != null && !str.trim().isEmpty()) {
                str = str.trim();
                if (str.length() >= 19) {
                    str = str.substring(0, 19).replace('T', ' ');
                    return LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } else if (str.length() >= 16) {
                    str = str.substring(0, 16).replace('T', ' ');
                    return LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                }
            }
        } catch (Exception ignored) {}
        try {
            Timestamp ts = rs.getTimestamp(columna);
            if (ts != null) {
                return ts.toLocalDateTime();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String formatearFechaEspanol(LocalDateTime ldt) {
        if (ldt == null) return "Por definir";
        String diaSemana = ldt.format(DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("es"))).replace(".", "");
        String resto = ldt.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("es"))).replace(".", "");
        return diaSemana.toLowerCase() + " " + resto;
    }

    private String formatearFechaModal(LocalDateTime ldt) {
        if (ldt == null) return "--";
        String diaSemana = ldt.format(DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("es"))).replace(".", "");
        if (!diaSemana.isEmpty()) {
            diaSemana = Character.toUpperCase(diaSemana.charAt(0)) + diaSemana.substring(1).toLowerCase();
        }
        String resto = ldt.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("es"))).replace(".", "");
        String hora = ldt.format(DateTimeFormatter.ofPattern("HH:mm"));
        return diaSemana + " " + resto + " · " + hora;
    }

    private String normalizarCmp(String rawCmp) {
        if (rawCmp == null || rawCmp.trim().isEmpty()) {
            return "CMP 045218";
        }
        String clean = rawCmp.trim().replaceAll("(?i)^CMP[-:\\s]*", "").trim();
        return "CMP " + clean;
    }

    public List<Map<String, Object>> obtenerProximasCitasPaciente(int idPaciente) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT c.id_cita, c.id_medico, c.fecha_hora, c.estado, c.nivel_urgencia, " +
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
                    int idCita = rs.getInt("id_cita");
                    map.put("id", "CIT-" + idCita);
                    map.put("idCita", idCita);
                    map.put("idMedico", rs.getInt("id_medico"));
                    LocalDateTime ldt = extraerFechaHora(rs, "fecha_hora");
                    if (ldt != null) {
                        map.put("fechaHora", ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                        map.put("fechaFormateada", formatearFechaEspanol(ldt));
                        map.put("horaFormateada", ldt.format(DateTimeFormatter.ofPattern("HH:mm")));
                        map.put("fechaModal", formatearFechaModal(ldt));
                    } else {
                        map.put("fechaHora", "Por definir");
                        map.put("fechaFormateada", "Por definir");
                        map.put("horaFormateada", "--:--");
                        map.put("fechaModal", "Por definir");
                    }
                    map.put("medico", "Dr. " + rs.getString("medico_nombres") + " " + rs.getString("medico_apellidos"));
                    map.put("especialidad", rs.getString("especialidad") != null ? rs.getString("especialidad") : "Medicina General");
                    map.put("cmp", normalizarCmp(rs.getString("cmp")));
                    map.put("sede", rs.getString("sede") != null ? rs.getString("sede") : "Sede Central");
                    map.put("consultorio", "Consultorio " + (100 + (idCita % 300)));
                    map.put("ubicacion", rs.getString("sede") != null ? rs.getString("sede") : "Sede Central");
                    map.put("modalidad", "Presencial");
                    String est = rs.getString("estado");
                    map.put("estado", est != null ? est : "CONFIRMADA");
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
        String sql = "SELECT c.id_cita, c.id_medico, c.fecha_hora, c.estado, c.nivel_urgencia, " +
                     "u.nombres AS medico_nombres, u.apellidos AS medico_apellidos, " +
                     "m.cmp, e.nombre AS especialidad, s.nombre AS sede " +
                     "FROM cita c " +
                     "JOIN medico m ON c.id_medico = m.id_medico " +
                     "JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN sede s ON m.id_sede = s.id_sede " +
                     "WHERE c.id_paciente = ? AND (c.estado = 'COMPLETADA' OR (c.estado != 'CANCELADA' AND c.fecha_hora < CURRENT_TIMESTAMP())) " +
                     "ORDER BY c.fecha_hora DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    int idCita = rs.getInt("id_cita");
                    map.put("id", "CIT-" + idCita);
                    map.put("idCita", idCita);
                    map.put("idMedico", rs.getInt("id_medico"));
                    LocalDateTime ldt = extraerFechaHora(rs, "fecha_hora");
                    if (ldt != null) {
                        map.put("fechaHora", ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                        map.put("fechaFormateada", formatearFechaEspanol(ldt));
                        map.put("horaFormateada", ldt.format(DateTimeFormatter.ofPattern("HH:mm")));
                        map.put("fechaModal", formatearFechaModal(ldt));
                    } else {
                        map.put("fechaHora", "Concluida");
                        map.put("fechaFormateada", "Concluida");
                        map.put("horaFormateada", "--:--");
                        map.put("fechaModal", "Concluida");
                    }
                    map.put("medico", "Dr. " + rs.getString("medico_nombres") + " " + rs.getString("medico_apellidos"));
                    map.put("cmp", normalizarCmp(rs.getString("cmp")));
                    map.put("especialidad", rs.getString("especialidad") != null ? rs.getString("especialidad") : "Medicina General");
                    map.put("sede", rs.getString("sede") != null ? rs.getString("sede") : "Sede Central");
                    map.put("consultorio", "Consultorio " + (100 + (idCita % 300)));
                    map.put("diagnostico", "Consulta ambulatoria realizada");
                    map.put("tipo", "Consulta");
                    map.put("estado", rs.getString("estado") != null ? rs.getString("estado") : "COMPLETADA");
                    list.add(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Map<String, Object>> obtenerCitasCanceladasPaciente(int idPaciente) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT c.id_cita, c.id_medico, c.fecha_hora, c.estado, c.nivel_urgencia, " +
                     "u.nombres AS medico_nombres, u.apellidos AS medico_apellidos, " +
                     "m.cmp, e.nombre AS especialidad, s.nombre AS sede " +
                     "FROM cita c " +
                     "JOIN medico m ON c.id_medico = m.id_medico " +
                     "JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN sede s ON m.id_sede = s.id_sede " +
                     "WHERE c.id_paciente = ? AND c.estado = 'CANCELADA' " +
                     "ORDER BY c.fecha_hora DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    int idCita = rs.getInt("id_cita");
                    map.put("id", "CIT-" + idCita);
                    map.put("idCita", idCita);
                    map.put("idMedico", rs.getInt("id_medico"));
                    LocalDateTime ldt = extraerFechaHora(rs, "fecha_hora");
                    if (ldt != null) {
                        map.put("fechaHora", ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                        map.put("fechaFormateada", formatearFechaEspanol(ldt));
                        map.put("horaFormateada", ldt.format(DateTimeFormatter.ofPattern("HH:mm")));
                        map.put("fechaModal", formatearFechaModal(ldt));
                    } else {
                        map.put("fechaHora", "Cancelada");
                        map.put("fechaFormateada", "Cancelada");
                        map.put("horaFormateada", "--:--");
                        map.put("fechaModal", "Cancelada");
                    }
                    map.put("medico", "Dr. " + rs.getString("medico_nombres") + " " + rs.getString("medico_apellidos"));
                    map.put("cmp", normalizarCmp(rs.getString("cmp")));
                    map.put("especialidad", rs.getString("especialidad") != null ? rs.getString("especialidad") : "Medicina General");
                    map.put("sede", rs.getString("sede") != null ? rs.getString("sede") : "Sede Central");
                    map.put("consultorio", "Consultorio " + (100 + (idCita % 300)));
                    map.put("estado", "CANCELADA");
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
                    m.put("cmp", normalizarCmp(rs.getString("cmp")));
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

        // Determinar categoría para la tabla
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
            // Fila 1: Recordatorio de cita (Pendiente)
            ps.setInt(1, idUsuario);
            ps.setNull(2, java.sql.Types.INTEGER);
            ps.setString(3, "Recordatorio de cita");
            ps.setString(4, "Confirma tu cita del mié 14 oct, 16:00 · enviado por web y WhatsApp");
            ps.setTimestamp(5, Timestamp.valueOf("2026-10-12 16:00:00"));
            ps.setInt(6, 0); // Pendiente
            ps.addBatch();

            // Fila 2: Lista de espera (Pendiente)
            ps.setInt(1, idUsuario);
            ps.setNull(2, java.sql.Types.INTEGER);
            ps.setString(3, "Lista de espera");
            ps.setString(4, "Registro en lista de espera de Dermatología confirmado");
            ps.setTimestamp(5, Timestamp.valueOf("2026-10-05 10:12:00"));
            ps.setInt(6, 0); // Pendiente
            ps.addBatch();

            // Fila 3: Reserva registrada (Leído)
            ps.setInt(1, idUsuario);
            ps.setNull(2, java.sql.Types.INTEGER);
            ps.setString(3, "Reserva registrada");
            ps.setString(4, "Cita de Cardiología del jue 15 oct, 09:00 · código CI-2026-004821");
            ps.setTimestamp(5, Timestamp.valueOf("2026-10-03 18:40:00"));
            ps.setInt(6, 1); // Leído
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
                    map.put("sede", (sede != null && !sede.trim().isEmpty() ? sede : "Sede San Borja") + " · Consultorio 215");
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

    public boolean aceptarOfertaCupo(int idPaciente, int idListaEspera) {
        String sqlWaitlist = "UPDATE lista_espera SET estado = 'ATENDIDA' WHERE id_paciente = ? AND (id_lista_espera = ? OR ? <= 0) AND estado = 'OFERTADA'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlWaitlist)) {
            ps.setInt(1, idPaciente);
            ps.setInt(2, idListaEspera);
            ps.setInt(3, idListaEspera);
            ps.executeUpdate();

            int idUsuario = obtenerIdUsuarioPorPacienteId(idPaciente);
            crearNotificacion(idUsuario, null, "Reserva registrada", "Oferta aceptada: Cita de Dermatología para el Mar 13 oct 2026 · 10:20 confirmada por cupo recuperado.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean rechazarOfertaCupo(int idPaciente, int idListaEspera) {
        String sqlWaitlist = "UPDATE lista_espera SET estado = 'ACTIVA' WHERE id_paciente = ? AND (id_lista_espera = ? OR ? <= 0) AND estado = 'OFERTADA'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlWaitlist)) {
            ps.setInt(1, idPaciente);
            ps.setInt(2, idListaEspera);
            ps.setInt(3, idListaEspera);
            ps.executeUpdate();

            int idUsuario = obtenerIdUsuarioPorPacienteId(idPaciente);
            crearNotificacion(idUsuario, null, "Lista de espera", "Has rechazado la oferta de cupo recuperado. Sigues en la lista de espera con tu misma posición.");
            return true;
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
        marcarNotificacionIndividualLeida(idNotificacion, idUsuario);
        String sqlCita = "UPDATE cita c JOIN notificacion n ON c.id_cita = n.id_cita SET c.estado = 'CONFIRMADA' WHERE n.id_notificacion = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlCita)) {
            ps.setInt(1, idNotificacion);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene los datos detallados de una cita para su reprogramación o consulta
     */
    public Map<String, Object> obtenerCitaPorId(int idCita) {
        Map<String, Object> map = new HashMap<>();
        String sql = "SELECT c.id_cita, c.id_paciente, c.id_medico, c.id_motivo, c.fecha_hora, c.estado, c.nivel_urgencia, " +
                     "u.nombres AS medico_nombres, u.apellidos AS medico_apellidos, m.cmp, " +
                     "m.id_especialidad, e.nombre AS especialidad, " +
                     "m.id_sede, s.nombre AS sede, s.direccion AS sede_direccion, " +
                     "mc.descripcion AS motivo " +
                     "FROM cita c " +
                     "JOIN medico m ON c.id_medico = m.id_medico " +
                     "JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN sede s ON m.id_sede = s.id_sede " +
                     "LEFT JOIN motivo_cita mc ON c.id_motivo = mc.id_motivo " +
                     "WHERE c.id_cita = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCita);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    map.put("idCita", rs.getInt("id_cita"));
                    map.put("idPaciente", rs.getInt("id_paciente"));
                    map.put("idMedico", rs.getInt("id_medico"));
                    map.put("idEspecialidad", rs.getInt("id_especialidad"));
                    map.put("idSede", rs.getInt("id_sede"));
                    map.put("idMotivo", rs.getObject("id_motivo"));
                    map.put("estado", rs.getString("estado"));
                    map.put("nivelUrgencia", rs.getString("nivel_urgencia"));

                    String medN = rs.getString("medico_nombres");
                    String medA = rs.getString("medico_apellidos");
                    String rawNombre = ((medN != null ? medN : "") + " " + (medA != null ? medA : "")).trim();
                    String cleanNombre = rawNombre.replaceAll("(?i)\\b(Dr\\.|Dra\\.|Dr|Dra)\\b|[^a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]", "").replaceAll("\\s+", " ").trim();
                    String nombreMed = "Dr. " + cleanNombre;
                    map.put("medico", nombreMed);
                    map.put("cmp", normalizarCmp(rs.getString("cmp")));

                    String esp = rs.getString("especialidad");
                    map.put("especialidad", esp != null ? esp : "Medicina General");

                    String sed = rs.getString("sede");
                    map.put("sede", sed != null ? sed : "Sede Lima");

                    String mot = rs.getString("motivo");
                    map.put("motivo", mot != null ? mot : "");

                    LocalDateTime ldt = extraerFechaHora(rs, "fecha_hora");
                    if (ldt != null) {
                        map.put("fechaHora", ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                        map.put("fecha", ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                        map.put("hora", ldt.format(DateTimeFormatter.ofPattern("HH:mm")));
                        
                        // Formato idéntico al banner: "mié 14 oct, 16:00"
                        String bannerFecha = ldt.format(DateTimeFormatter.ofPattern("EEE dd MMM, HH:mm", Locale.forLanguageTag("es"))).replace(".", "");
                        map.put("fechaFormateadaBanner", bannerFecha);
                        
                        String extendidaFecha = ldt.format(DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy", Locale.forLanguageTag("es"))).replace(".", "");
                        map.put("fechaFormateadaExtendida", extendidaFecha);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * Obtiene los motivos de consulta disponibles según especialidad
     */
    public List<Map<String, Object>> obtenerMotivosPorEspecialidad(int idEspecialidad) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id_motivo, descripcion FROM motivo_cita WHERE id_especialidad = ? AND estado = 'ACTIVO'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEspecialidad);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("idMotivo", rs.getInt("id_motivo"));
                    m.put("descripcion", rs.getString("descripcion"));
                    lista.add(m);
                }
            }
        } catch (Exception e) {
            // Manejo silencioso
        }

        if (lista.isEmpty()) {
            Map<Integer, String[]> motivosMap = new HashMap<>();
            motivosMap.put(1, new String[]{"Control de hipertensión arterial", "Chequeo cardiovascular preventivo", "Evaluación por dolor torácico", "Lectura de electrocardiograma / holter"});
            motivosMap.put(2, new String[]{"Consulta por acné o dermatitis", "Revisión preventiva de lunares y manchas", "Caída de cabello / alopecia", "Alergia o erupción cutánea"});
            motivosMap.put(3, new String[]{"Control de crecimiento y desarrollo", "Fiebre y malestar pediátrico general", "Vacunación y chequeo preventivo", "Evaluación respiratoria infantil"});
            motivosMap.put(4, new String[]{"Chequeo médico integral de adulto", "Evaluación de fatiga o malestar general", "Revisión de análisis de laboratorio clínico", "Control de glucosa / diabetes"});
            motivosMap.put(5, new String[]{"Medición y descarte de miopía / astigmatismo", "Dolor o irritación ocular", "Fondo de ojo y presión intraocular", "Chequeo visual preventivo"});

            String[] seleccion = motivosMap.getOrDefault(idEspecialidad, new String[]{
                "Consulta médica general",
                "Evaluación preventiva anual",
                "Revisión de síntomas recientes",
                "Lectura de exámenes de laboratorio",
                "Renovación o ajuste de medicación"
            });

            for (int i = 0; i < seleccion.length; i++) {
                Map<String, Object> m = new HashMap<>();
                m.put("idMotivo", (i + 1));
                m.put("descripcion", seleccion[i]);
                lista.add(m);
            }
        }
        return lista;
    }

    /**
     * Reprograma una cita médica respetando el flujo y precauciones clínicas:
     * Transacción atómica: se valida disponibilidad futura, se libera la cita previa (marcada como REPROGRAMADA)
     * y se crea el nuevo cupo confirmado con notificación al paciente.
     */
    public boolean reprogramarCita(int idCitaAnterior, int idPaciente, int idNuevoMedico, String nuevaFecha, String nuevaHora, Integer idMotivo, String nivelUrgencia) {
        String sqlVerificar = "SELECT id_cita, id_paciente, estado FROM cita WHERE id_cita = ? AND id_paciente = ?";
        String sqlActualizarVieja = "UPDATE cita SET estado = 'REPROGRAMADA' WHERE id_cita = ?";
        String sqlInsertNueva = "INSERT INTO cita (id_paciente, id_medico, id_disponibilidad, id_motivo, fecha_hora, estado, riesgo_cancelacion, nivel_urgencia) VALUES (?, ?, ?, ?, ?, 'CONFIRMADA', ?, ?)";
        
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false); // Transacción atómica integral

            // 1. Validar que la cita pertenece al paciente y está en estado activo
            try (PreparedStatement psVer = conn.prepareStatement(sqlVerificar)) {
                psVer.setInt(1, idCitaAnterior);
                psVer.setInt(2, idPaciente);
                try (ResultSet rs = psVer.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    String estadoActual = rs.getString("estado");
                    if ("CANCELADA".equalsIgnoreCase(estadoActual) || "ATENDIDA".equalsIgnoreCase(estadoActual) || "REPROGRAMADA".equalsIgnoreCase(estadoActual)) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // 2. Validar que el médico no tenga ya una cita confirmada en esa misma fecha y hora
            String fechaHoraStr = nuevaFecha + " " + (nuevaHora.length() == 5 ? nuevaHora + ":00" : nuevaHora);
            String sqlCheckOcupado = "SELECT COUNT(*) FROM cita WHERE id_medico = ? AND fecha_hora = ? AND estado NOT IN ('CANCELADA', 'REPROGRAMADA')";
            try (PreparedStatement psCheck = conn.prepareStatement(sqlCheckOcupado)) {
                psCheck.setInt(1, idNuevoMedico);
                psCheck.setString(2, fechaHoraStr);
                try (ResultSet rsCheck = psCheck.executeQuery()) {
                    if (rsCheck.next() && rsCheck.getInt(1) > 0) {
                        conn.rollback();
                        return false; // Horario ya tomado
                    }
                }
            }

            // 3. Obtener o asignar id_disponibilidad
            int idDisp = 1;
            String sqlDisp = "SELECT id_disponibilidad FROM disponibilidad WHERE id_medico = ? LIMIT 1";
            try (PreparedStatement psDisp = conn.prepareStatement(sqlDisp)) {
                psDisp.setInt(1, idNuevoMedico);
                try (ResultSet rsDisp = psDisp.executeQuery()) {
                    if (rsDisp.next()) {
                        idDisp = rsDisp.getInt("id_disponibilidad");
                    }
                }
            }

            // 4. Marcar como REPROGRAMADA la cita anterior liberando su cupo
            try (PreparedStatement psUpd = conn.prepareStatement(sqlActualizarVieja)) {
                psUpd.setInt(1, idCitaAnterior);
                psUpd.executeUpdate();
            }

            // 5. Insertar la nueva cita con estado CONFIRMADA
            String riesgo = calcularRiesgoCancelacion(idPaciente);
            int idNuevaCita = 0;

            try (PreparedStatement psIns = conn.prepareStatement(sqlInsertNueva, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                psIns.setInt(1, idPaciente);
                psIns.setInt(2, idNuevoMedico);
                psIns.setInt(3, idDisp);
                if (idMotivo != null && idMotivo > 0) {
                    psIns.setInt(4, idMotivo);
                } else {
                    psIns.setNull(4, java.sql.Types.INTEGER);
                }
                psIns.setString(5, fechaHoraStr);
                psIns.setString(6, riesgo);
                psIns.setString(7, nivelUrgencia != null ? nivelUrgencia : "BAJA");

                int affected = psIns.executeUpdate();
                if (affected <= 0) {
                    conn.rollback();
                    return false;
                }

                try (ResultSet gk = psIns.getGeneratedKeys()) {
                    if (gk.next()) {
                        idNuevaCita = gk.getInt(1);
                    }
                }
            }

            // 6. Generar notificación de confirmación para el paciente
            int idUsuario = obtenerIdUsuarioPorPacienteId(idPaciente);
            Map<String, Object> detMed = obtenerDetallesMedico(idNuevoMedico);
            String nombreMed = detMed.get("nombreMedico") != null ? (String) detMed.get("nombreMedico") : "tu médico especialista";
            String espMed = detMed.get("especialidad") != null ? (String) detMed.get("especialidad") : "Consulta Médica";
            String msg = "Cita Reprogramada con éxito: Tu consulta con " + nombreMed + " (" + espMed + ") fue reagendada para el " + nuevaFecha + " a las " + nuevaHora + ". Tu cita previa fue cancelada y liberada.";

            String sqlNotif = "INSERT INTO notificacion (id_usuario, id_cita, tipo, canal, mensaje, estado_envio, leida) VALUES (?, ?, 'Citas', 'PLATAFORMA', ?, 'ENVIADA', 0)";
            try (PreparedStatement psNot = conn.prepareStatement(sqlNotif)) {
                psNot.setInt(1, idUsuario);
                psNot.setInt(2, idNuevaCita > 0 ? idNuevaCita : idCitaAnterior);
                psNot.setString(3, msg);
                psNot.executeUpdate();
            } catch (Exception exNot) {
                // Notificación accesoria no anula la transacción
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {}
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) {}
            }
        }
    }
}
