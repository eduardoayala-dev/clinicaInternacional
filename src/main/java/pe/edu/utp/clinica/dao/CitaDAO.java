package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.util.logging.Logger;

/**
 * DAO para gestión clínica integral de Citas Médicas, Reservas, Reprogramaciones y Reasignación Inteligente.
 * Delega en MedicoDAO, NotificacionDAO y ListaEsperaDAO para mantener alta cohesión y bajo acoplamiento.
 */
public class CitaDAO {

    private static final Logger LOGGER = Logger.getLogger(CitaDAO.class.getName());

    protected Connection abrirConexion() throws SQLException {
        return DatabaseConfig.getConnection();
    }


    private final MedicoDAO medicoDAO = new MedicoDAO();
    private final NotificacionDAO notificacionDAO = new NotificacionDAO();
    private final ListaEsperaDAO listaEsperaDAO = new ListaEsperaDAO();

    // =========================================================================
    // 1. DELEGACIONES A MedicoDAO (Catálogos, sedes, especialidades y disponibilidad)
    // =========================================================================

    public List<Map<String, Object>> obtenerEspecialidades() {
        return medicoDAO.obtenerEspecialidades();
    }

    public List<Map<String, Object>> obtenerSedes() {
        return medicoDAO.obtenerSedes();
    }

    public List<Map<String, Object>> obtenerMedicosDisponibles(int idEspecialidad, int idSede) {
        return medicoDAO.obtenerMedicosDisponibles(idEspecialidad, idSede);
    }

    public List<Map<String, Object>> obtenerMedicosPorEspecialidad(int idEspecialidad) {
        return medicoDAO.obtenerMedicosPorEspecialidad(idEspecialidad);
    }

    public Map<String, Object> obtenerDetallesMedico(int idMedico) {
        return medicoDAO.obtenerDetallesMedico(idMedico);
    }

    public List<String> obtenerFechasDisponibles(int idMedico) {
        return medicoDAO.obtenerFechasDisponibles(idMedico);
    }

    public List<String> obtenerHorariosDisponibles(int idMedico, String fecha) {
        return medicoDAO.obtenerHorariosDisponibles(idMedico, fecha);
    }

    public List<Map<String, Object>> obtenerTodosMedicosDetalle() {
        return medicoDAO.obtenerTodosMedicosDetalle();
    }

    public List<Map<String, Object>> obtenerMedicosDePaciente(int idPaciente) {
        return medicoDAO.obtenerMedicosDePaciente(idPaciente);
    }

    public List<Map<String, Object>> obtenerMotivosPorEspecialidad(int idEspecialidad) {
        return medicoDAO.obtenerMotivosPorEspecialidad(idEspecialidad);
    }

    // =========================================================================
    // 2. RESERVA DE CITAS MÉDICAS
    // =========================================================================

    public boolean reservarCita(int idPacienteParam, int idMedico, String fecha, String hora, String nivelUrgencia) {
        return reservarCitaConId(idPacienteParam, idMedico, fecha, hora, nivelUrgencia) > 0;
    }

    public int reservarCitaConId(int idPaciente, int idMedico, String fecha, String hora, String urgencia) {
        try {
            return reservarCitaValidada(idPaciente, idMedico, fecha, hora, urgencia);
        } catch (AgendaException e) {
            return -1;
        }
    }

    public int reservarCitaValidada(int idPaciente, int idMedico, String fecha, String hora, String urgencia) {
        return new AgendaCitaDAO(this::abrirConexion).reservar(idPaciente, idMedico, fecha, hora, urgencia);
    }


    public List<Map<String, Object>> obtenerProximasCitasPaciente(int idPaciente) {
        String sql = "SELECT c.id_cita, c.id_medico, c.fecha_hora, c.estado, c.nivel_urgencia, " +
                     "u.nombres AS medico_nombres, u.apellidos AS medico_apellidos, " +
                     "m.cmp, e.nombre AS especialidad, s.nombre AS sede " +
                     "FROM cita c " +
                     "JOIN medico m ON c.id_medico = m.id_medico " +
                     "JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN sede s ON m.id_sede = s.id_sede " +
                     "WHERE c.id_paciente = ? AND c.estado NOT IN ('CANCELADA', 'REPROGRAMADA') AND c.fecha_hora >= CURRENT_TIMESTAMP() " +
                     "ORDER BY c.fecha_hora ASC";
        return ejecutarConsultaCitas(sql, idPaciente);
    }

    public List<Map<String, Object>> obtenerHistorialPaciente(int idPaciente) {
        String sql = "SELECT c.id_cita, c.id_medico, c.fecha_hora, c.estado, c.nivel_urgencia, " +
                     "u.nombres AS medico_nombres, u.apellidos AS medico_apellidos, " +
                     "m.cmp, e.nombre AS especialidad, s.nombre AS sede " +
                     "FROM cita c " +
                     "JOIN medico m ON c.id_medico = m.id_medico " +
                     "JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN sede s ON m.id_sede = s.id_sede " +
                     "WHERE c.id_paciente = ? AND (c.estado = 'COMPLETADA' OR (c.estado NOT IN ('CANCELADA', 'REPROGRAMADA') AND c.fecha_hora < CURRENT_TIMESTAMP())) " +
                     "ORDER BY c.fecha_hora DESC";
        List<Map<String, Object>> list = ejecutarConsultaCitas(sql, idPaciente);
        for (Map<String, Object> c : list) {
            c.put("diagnostico", "Consulta ambulatoria realizada");
            c.put("tipo", "Consulta");
        }
        return list;
    }

    public List<Map<String, Object>> obtenerCitasCanceladasPaciente(int idPaciente) {
        String sql = "SELECT c.id_cita, c.id_medico, c.fecha_hora, c.estado, c.nivel_urgencia, " +
                     "u.nombres AS medico_nombres, u.apellidos AS medico_apellidos, " +
                     "m.cmp, e.nombre AS especialidad, s.nombre AS sede " +
                     "FROM cita c " +
                     "JOIN medico m ON c.id_medico = m.id_medico " +
                     "JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN sede s ON m.id_sede = s.id_sede " +
                     "WHERE c.id_paciente = ? AND c.estado IN ('CANCELADA', 'REPROGRAMADA') " +
                     "ORDER BY c.fecha_hora DESC";
        return ejecutarConsultaCitas(sql, idPaciente);
    }

    private List<Map<String, Object>> ejecutarConsultaCitas(String sql, int idPaciente) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = abrirConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapearFilaCita(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private Map<String, Object> mapearFilaCita(ResultSet rs) throws Exception {
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
            map.put("fecha", ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            map.put("fechaSolo", ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            map.put("hora", ldt.format(DateTimeFormatter.ofPattern("HH:mm")));
            map.put("horaSolo", ldt.format(DateTimeFormatter.ofPattern("HH:mm")));
            map.put("turno", ldt.getHour() < 13 ? "manana" : "tarde");
            map.put("fechaFormateadaBanner", formatearFechaEspanol(ldt) + " · " + ldt.format(DateTimeFormatter.ofPattern("HH:mm")));
        } else {
            map.put("fechaHora", "Por definir");
            map.put("fechaFormateada", "Por definir");
            map.put("horaFormateada", "--:--");
            map.put("fechaModal", "Por definir");
            map.put("fecha", "");
            map.put("fechaSolo", "");
            map.put("hora", "");
            map.put("horaSolo", "");
            map.put("turno", "manana");
            map.put("fechaFormateadaBanner", "Por definir");
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
        return map;
    }

    public Map<String, Object> obtenerCitaPorId(int idCita) {
        String sql = "SELECT c.id_cita, c.id_paciente, c.id_medico, c.id_motivo, c.fecha_hora, c.estado, c.nivel_urgencia, " +
                     "u.nombres AS medico_nombres, u.apellidos AS medico_apellidos, " +
                     "m.cmp, m.id_sede, m.id_especialidad, e.nombre AS especialidad, s.nombre AS sede " +
                     "FROM cita c " +
                     "JOIN medico m ON c.id_medico = m.id_medico " +
                     "JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN sede s ON m.id_sede = s.id_sede " +
                     "WHERE c.id_cita = ?";
        try (Connection conn = abrirConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCita);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = mapearFilaCita(rs);
                    map.put("idPaciente", rs.getInt("id_paciente"));
                    map.put("idSede", rs.getInt("id_sede"));
                    map.put("idEspecialidad", rs.getInt("id_especialidad"));
                    map.put("idMotivo", rs.getObject("id_motivo"));
                    map.put("nivelUrgencia", rs.getString("nivel_urgencia"));
                    
                    LocalDateTime ldt = extraerFechaHora(rs, "fecha_hora");
                    if (ldt != null) {
                        map.put("fecha", ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                        map.put("fechaSolo", ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                        map.put("hora", ldt.format(DateTimeFormatter.ofPattern("HH:mm")));
                        map.put("horaSolo", ldt.format(DateTimeFormatter.ofPattern("HH:mm")));
                        map.put("turno", ldt.getHour() < 13 ? "manana" : "tarde");
                        map.put("fechaFormateadaBanner", formatearFechaEspanol(ldt) + " · " + ldt.format(DateTimeFormatter.ofPattern("HH:mm")));
                    }
                    return map;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // =========================================================================
    // 4. REPROGRAMACIÓN DE CITAS MÉDICAS (Transacción Atómica Clínica)
    // =========================================================================

    public boolean reprogramarCita(int anterior, int paciente, int medico, String fecha, String hora, Integer motivo, String urgencia) {
        try {
            return reprogramarCitaValidada(anterior, paciente, medico, fecha, hora, motivo, urgencia);
        } catch (AgendaException e) {
            return false;
        }
    }

    public boolean reprogramarCitaValidada(int anterior, int paciente, int medico, String fecha, String hora, Integer motivo, String urgencia) {
        return new AgendaCitaDAO(this::abrirConexion).reprogramar(anterior, paciente, medico, fecha, hora, motivo, urgencia);
    }

    // =========================================================================
    // 5. ATENCIÓN MÉDICA Y AGENDA DE MÉDICOS
    // =========================================================================

    public boolean actualizarEstadoCita(int idCita, String nuevoEstado) {
        if (idCita <= 0 || nuevoEstado == null) return false;
        String anteriores = switch (nuevoEstado) {
            case "CONFIRMADA" -> "('SOLICITADA')";
            case "EN_SALA" -> "('CONFIRMADA')";
            case "CANCELADA" -> "('SOLICITADA','CONFIRMADA')";
            default -> null;
        };
        if (anteriores == null) return false;
        String sql = "UPDATE cita SET estado=? WHERE id_cita=? AND estado IN " + anteriores;
        try (Connection conn = abrirConexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idCita);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            LOGGER.warning("No se pudo actualizar el estado de la cita.");
            return false;
        }
    }

    /** Actualización condicionada al propietario y al estado actual de la cita. */
    public boolean actualizarEstadoCitaPaciente(int idCita, int idPaciente, String nuevoEstado) {
        if (idCita <= 0 || idPaciente <= 0) return false;
        String estadosPermitidos;
        if ("CONFIRMADA".equals(nuevoEstado)) estadosPermitidos = "('SOLICITADA')";
        else if ("CANCELADA".equals(nuevoEstado)) estadosPermitidos = "('SOLICITADA','CONFIRMADA')";
        else return false;
        String sql = "UPDATE cita SET estado = ? WHERE id_cita = ? AND id_paciente = ? AND estado IN "
                + estadosPermitidos;
        try (Connection conn = abrirConexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idCita);
            ps.setInt(3, idPaciente);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            LOGGER.log(java.util.logging.Level.WARNING, "No se pudo actualizar la cita del paciente", e);
            return false;
        }
    }

    /** Cita e historial se modifican juntos y solo por el médico asignado. */
    public boolean actualizarAtencionMedica(int idCita, int idMedico, String nuevoEstado,
            String prioridad, String fechaRecomendada, String indicaciones) {
        if (idCita <= 0 || idMedico <= 0
                || !("ATENDIDA".equals(nuevoEstado) || "NO_ASISTIO".equals(nuevoEstado))
                || !("ALTA".equals(prioridad) || "MEDIA".equals(prioridad) || "BAJA".equals(prioridad))) {
            return false;
        }
        java.sql.Date proxima = null;
        try {
            if (fechaRecomendada != null && !fechaRecomendada.isBlank()) {
                proxima = java.sql.Date.valueOf(fechaRecomendada);
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        String sql = "UPDATE cita SET estado = ?, nivel_urgencia = ?"
                + (proxima != null ? ", fecha_recomendada_proxima = ?" : "")
                + " WHERE id_cita = ? AND id_medico = ? AND estado IN ('CONFIRMADA','EN_SALA')";
        try (Connection conn = abrirConexion()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, nuevoEstado);
                    ps.setString(2, prioridad);
                    int param = 3;
                    if (proxima != null) ps.setDate(param++, proxima);
                    ps.setInt(param++, idCita);
                    ps.setInt(param, idMedico);
                    if (ps.executeUpdate() != 1) {
                        conn.rollback();
                        return false;
                    }
                }
                String historial = "INSERT INTO historial_asistencia (id_paciente, id_cita, asistio, observacion) "
                        + "SELECT id_paciente, id_cita, ?, ? FROM cita WHERE id_cita = ? AND id_medico = ?";
                try (PreparedStatement ps = conn.prepareStatement(historial)) {
                    ps.setBoolean(1, !"NO_ASISTIO".equals(nuevoEstado));
                    ps.setString(2, indicaciones);
                    ps.setInt(3, idCita);
                    ps.setInt(4, idMedico);
                    if (ps.executeUpdate() != 1) throw new SQLException("No se registró el historial");
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(java.util.logging.Level.WARNING, "No se pudo guardar la atención médica", e);
                return false;
            }
        } catch (SQLException e) {
            LOGGER.log(java.util.logging.Level.WARNING, "No se pudo abrir la transacción de atención", e);
            return false;
        }
    }

    public List<Map<String, Object>> obtenerAgendaMedico(int idMedico) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT c.id_cita, c.fecha_hora, c.estado, c.nivel_urgencia, " +
                     "u.nombres AS paciente_nombres, u.apellidos AS paciente_apellidos, p.dni, p.telefono, " +
                     "e.nombre AS especialidad, COALESCE(mc.descripcion, 'Consulta y control general') AS motivo_consulta " +
                     "FROM cita c " +
                     "JOIN paciente p ON c.id_paciente = p.id_paciente " +
                     "JOIN usuario u ON p.id_usuario = u.id_usuario " +
                     "JOIN medico m ON c.id_medico = m.id_medico " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN motivo_cita mc ON c.id_motivo = mc.id_motivo " +
                     "WHERE c.id_medico = ? AND DATE(c.fecha_hora) = CURDATE() AND c.estado NOT IN ('CANCELADA', 'REPROGRAMADA') " +
                     "ORDER BY c.fecha_hora ASC";
        try (Connection conn = abrirConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMedico);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearCitaAgenda(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Si hoy no hay citas agendadas para el médico, traer las citas más recientes del médico
        if (lista.isEmpty()) {
            String sqlFallback = "SELECT c.id_cita, c.fecha_hora, c.estado, c.nivel_urgencia, " +
                                 "u.nombres AS paciente_nombres, u.apellidos AS paciente_apellidos, p.dni, p.telefono, " +
                                 "e.nombre AS especialidad, COALESCE(mc.descripcion, 'Control y evaluación médica') AS motivo_consulta " +
                                 "FROM cita c " +
                                 "JOIN paciente p ON c.id_paciente = p.id_paciente " +
                                 "JOIN usuario u ON p.id_usuario = u.id_usuario " +
                                 "JOIN medico m ON c.id_medico = m.id_medico " +
                                 "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                                 "LEFT JOIN motivo_cita mc ON c.id_motivo = mc.id_motivo " +
                                 "WHERE c.id_medico = ? AND c.estado NOT IN ('CANCELADA', 'REPROGRAMADA') " +
                                 "ORDER BY c.fecha_hora DESC LIMIT 10";
            try (Connection conn = abrirConexion();
                 PreparedStatement ps = conn.prepareStatement(sqlFallback)) {
                ps.setInt(1, idMedico);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(mapearCitaAgenda(rs));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return lista;
    }

    private Map<String, Object> mapearCitaAgenda(ResultSet rs) throws Exception {
        Map<String, Object> map = new HashMap<>();
        int idCita = rs.getInt("id_cita");
        map.put("idCita", idCita);
        LocalDateTime ldt = extraerFechaHora(rs, "fecha_hora");
        map.put("hora", ldt != null ? ldt.format(DateTimeFormatter.ofPattern("HH:mm")) : "08:00");
        map.put("fecha", ldt != null ? ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "");
        map.put("fechaFormateada", ldt != null ? formatearFechaEspanol(ldt) : "");
        map.put("paciente", rs.getString("paciente_nombres") + " " + rs.getString("paciente_apellidos"));
        map.put("dni", rs.getString("dni") != null ? rs.getString("dni") : "10492837");
        map.put("telefono", rs.getString("telefono") != null ? rs.getString("telefono") : "+51 987 654 321");
        map.put("especialidad", rs.getString("especialidad") != null ? rs.getString("especialidad") : "Consulta General");
        map.put("motivo", rs.getString("motivo_consulta") != null ? rs.getString("motivo_consulta") : "Control general");
        map.put("estado", rs.getString("estado"));
        map.put("urgencia", rs.getString("nivel_urgencia"));
        return map;
    }

    // =========================================================================
    // 6. REASIGNACIÓN INTELIGENTE Y RIESGO DE CANCELACIÓN (HU008 / HU012)
    // =========================================================================

    public List<Map<String, Object>> obtenerCitasConRiesgo() {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT c.id_cita, c.fecha_hora, c.nivel_urgencia, c.riesgo_cancelacion, " +
                     "u.nombres AS paciente_nombres, u.apellidos AS paciente_apellidos, " +
                     "um.nombres AS medico_nombres, um.apellidos AS medico_apellidos, " +
                     "e.nombre AS especialidad, s.nombre AS sede " +
                     "FROM cita c " +
                     "JOIN paciente p ON c.id_paciente = p.id_paciente " +
                     "JOIN usuario u ON p.id_usuario = u.id_usuario " +
                     "JOIN medico m ON c.id_medico = m.id_medico " +
                     "JOIN usuario um ON m.id_usuario = um.id_usuario " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN sede s ON m.id_sede = s.id_sede " +
                     "WHERE c.estado = 'CONFIRMADA' AND c.fecha_hora >= CURRENT_TIMESTAMP() " +
                     "ORDER BY CASE c.riesgo_cancelacion WHEN 'ALTO' THEN 1 WHEN 'MEDIO' THEN 2 ELSE 3 END, c.fecha_hora ASC " +
                     "LIMIT 10";
        try (Connection conn = abrirConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("idCita", rs.getInt("id_cita"));
                LocalDateTime ldt = extraerFechaHora(rs, "fecha_hora");
                map.put("fechaHora", ldt != null ? ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "12/10/2026 15:00");
                map.put("paciente", rs.getString("paciente_nombres") + " " + rs.getString("paciente_apellidos"));
                map.put("medico", "Dr. " + rs.getString("medico_nombres") + " " + rs.getString("medico_apellidos"));
                map.put("especialidad", rs.getString("especialidad") != null ? rs.getString("especialidad") : "Cardiología");
                map.put("sede", rs.getString("sede") != null ? rs.getString("sede") : "Sede Central");
                map.put("nivelUrgencia", rs.getString("nivel_urgencia") != null ? rs.getString("nivel_urgencia") : "BAJA");
                map.put("riesgoCancelacion", rs.getString("riesgo_cancelacion") != null ? rs.getString("riesgo_cancelacion") : "MEDIO");
                lista.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public String ejecutarReasignacionInteligente(int idCitaOriginal) {
        String sqlCita = "SELECT c.id_cita, c.id_medico, c.id_disponibilidad, m.id_especialidad, m.id_sede " +
                         "FROM cita c " +
                         "JOIN medico m ON c.id_medico = m.id_medico " +
                         "WHERE c.id_cita = ?";
        int idEspecialidad = 1;
        int idDisp = 1;
        try (Connection conn = abrirConexion();
             PreparedStatement ps = conn.prepareStatement(sqlCita)) {
            ps.setInt(1, idCitaOriginal);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    idEspecialidad = rs.getInt("id_especialidad");
                    idDisp = rs.getInt("id_disponibilidad");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String sqlLista = "SELECT le.id_lista_espera, le.id_paciente, u.nombres, u.apellidos, u.id_usuario " +
                          "FROM lista_espera le " +
                          "JOIN paciente p ON le.id_paciente = p.id_paciente " +
                          "JOIN usuario u ON p.id_usuario = u.id_usuario " +
                          "WHERE le.id_especialidad = ? AND le.estado = 'ACTIVA' " +
                          "ORDER BY CASE le.nivel_urgencia WHEN 'ALTA' THEN 1 WHEN 'MEDIA' THEN 2 ELSE 3 END, le.posicion ASC " +
                          "LIMIT 1";
        try (Connection conn = abrirConexion();
             PreparedStatement ps = conn.prepareStatement(sqlLista)) {
            ps.setInt(1, idEspecialidad);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int idLista = rs.getInt("id_lista_espera");
                    String pacienteCandidato = rs.getString("nombres") + " " + rs.getString("apellidos");
                    int idUsuarioCandidato = rs.getInt("id_usuario");

                    String sqlUpdLista = "UPDATE lista_espera SET estado = 'OFERTADA' WHERE id_lista_espera = ?";
                    try (PreparedStatement psUpd = conn.prepareStatement(sqlUpdLista)) {
                        psUpd.setInt(1, idLista);
                        psUpd.executeUpdate();
                    }

                    String sqlInsReasig = "INSERT INTO reasignacion_inteligente (id_cita_original, id_lista_espera, id_disponibilidad, fecha_limite_respuesta, estado) " +
                                          "VALUES (?, ?, ?, DATE_ADD(CURRENT_TIMESTAMP(), INTERVAL 15 MINUTE), 'PENDIENTE')";
                    try (PreparedStatement psReasig = conn.prepareStatement(sqlInsReasig)) {
                        psReasig.setInt(1, idCitaOriginal);
                        psReasig.setInt(2, idLista);
                        psReasig.setInt(3, idDisp);
                        psReasig.executeUpdate();
                    }

                    crearNotificacion(idUsuarioCandidato, idCitaOriginal, "Lista de espera",
                            "¡Cupo recuperado anticipado! Se te ha asignado una vacante prioritaria para Dermatología. Tienes 15 minutos para confirmar.");

                    return "Cupo asignado prioritariamente al paciente en lista de espera: " + pacienteCandidato + " (Plazo de confirmación: 15 min).";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "No hay pacientes compatibles en lista de espera para esta especialidad.";
    }

    // =========================================================================
    // 7. DELEGACIONES A NotificacionDAO Y ListaEsperaDAO (Compatibilidad total)
    // =========================================================================

    public boolean crearNotificacion(int idUsuario, Integer idCita, String tipo, String mensaje) {
        return notificacionDAO.crearNotificacion(idUsuario, idCita, tipo, mensaje);
    }

    public List<Map<String, Object>> obtenerNotificacionesUsuario(int idUsuario) {
        return notificacionDAO.obtenerNotificacionesUsuario(idUsuario);
    }

    public void sincronizarNotificacionesIniciales(int idUsuario) {
        notificacionDAO.sincronizarNotificacionesIniciales(idUsuario);
    }

    public int contarNotificacionesSinLeer(int idUsuario) {
        return notificacionDAO.contarNotificacionesSinLeer(idUsuario);
    }

    public boolean marcarNotificacionesComoLeidas(int idUsuario) {
        return notificacionDAO.marcarNotificacionesComoLeidas(idUsuario);
    }

    public boolean marcarNotificacionIndividualLeida(int idNotificacion, int idUsuario) {
        return notificacionDAO.marcarNotificacionIndividualLeida(idNotificacion, idUsuario);
    }

    public boolean confirmarRecordatorioCita(int idNotificacion, int idUsuario) {
        return notificacionDAO.confirmarRecordatorioCita(idNotificacion, idUsuario);
    }

    public boolean inscribirListaEspera(int idPaciente, int idEspecialidad, Integer idSede, String rangoHorario, String urgenciaDisponibilidad, String canalesNotificacion) {
        return listaEsperaDAO.inscribirListaEspera(idPaciente, idEspecialidad, idSede, rangoHorario, urgenciaDisponibilidad, canalesNotificacion);
    }

    public boolean retirarListaEspera(int idListaEspera, int idPaciente) {
        return listaEsperaDAO.retirarListaEspera(idListaEspera, idPaciente);
    }

    public Map<String, Object> obtenerEstadoListaEspera(int idPaciente) {
        return listaEsperaDAO.obtenerEstadoListaEspera(idPaciente);
    }

    public Map<String, Object> obtenerOfertaCupoAnticipado(int idPaciente) {
        return listaEsperaDAO.obtenerOfertaCupoAnticipado(idPaciente);
    }

    public boolean aceptarOfertaCupo(int idPaciente, int idListaEspera) {
        int idUsuario = obtenerIdUsuarioPorPacienteId(idPaciente);
        return listaEsperaDAO.aceptarOfertaCupo(idPaciente, idListaEspera, idUsuario);
    }

    public boolean rechazarOfertaCupo(int idPaciente, int idListaEspera) {
        int idUsuario = obtenerIdUsuarioPorPacienteId(idPaciente);
        return listaEsperaDAO.rechazarOfertaCupo(idPaciente, idListaEspera, idUsuario);
    }

    public Map<String, Object> obtenerDetalleCitaMedico(int idCita, int idMedico) {
        Map<String, Object> map = new HashMap<>();
        if (idCita <= 0 || idMedico <= 0) return map;
        String sql = "SELECT c.id_cita, c.fecha_hora, c.estado, c.nivel_urgencia, c.riesgo_cancelacion, c.fecha_recomendada_proxima, c.fecha_creacion, " +
                     "u.nombres AS pac_nombres, u.apellidos AS pac_apellidos, u.email AS pac_email, " +
                     "p.id_paciente, p.dni, p.telefono, p.fecha_nacimiento, p.direccion, p.alergias, p.tipo_cobertura, p.canal_aviso, " +
                     "m.id_medico, m.cmp, um.nombres AS med_nombres, um.apellidos AS med_apellidos, " +
                     "e.nombre AS especialidad, s.nombre AS sede_nombre, " +
                     "COALESCE(mc.descripcion, 'Chequeo preventivo / Consulta especializada') AS motivo_consulta " +
                     "FROM cita c " +
                     "JOIN paciente p ON c.id_paciente = p.id_paciente " +
                     "JOIN usuario u ON p.id_usuario = u.id_usuario " +
                     "JOIN medico m ON c.id_medico = m.id_medico " +
                     "JOIN usuario um ON m.id_usuario = um.id_usuario " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN sede s ON m.id_sede = s.id_sede " +
                     "LEFT JOIN motivo_cita mc ON c.id_motivo = mc.id_motivo " +
                     "WHERE c.id_cita = ? AND c.id_medico = ?";
        try (Connection conn = abrirConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCita);
            ps.setInt(2, idMedico);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    map.put("idCita", rs.getInt("id_cita"));
                    LocalDateTime ldt = extraerFechaHora(rs, "fecha_hora");
                    map.put("fechaHora", ldt);
                    map.put("fechaFormateada", ldt != null ? formatearFechaModal(ldt) : "Por definir");
                    map.put("hora", ldt != null ? ldt.format(DateTimeFormatter.ofPattern("HH:mm")) : "08:00");
                    map.put("fecha", ldt != null ? ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "");
                    map.put("estado", rs.getString("estado"));
                    map.put("urgencia", rs.getString("nivel_urgencia") != null ? rs.getString("nivel_urgencia") : "MEDIA");
                    map.put("riesgo", rs.getString("riesgo_cancelacion"));
                    
                    // Fecha recomendada proxima
                    java.sql.Date fProx = rs.getDate("fecha_recomendada_proxima");
                    map.put("fechaRecomendadaProxima", fProx != null ? fProx.toString() : "");

                    // Fecha de creación de la reserva
                    LocalDateTime ldtCreacion = extraerFechaHora(rs, "fecha_creacion");
                    if (ldtCreacion != null) {
                        map.put("fechaRegistro", ldtCreacion.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    } else if (ldt != null) {
                        map.put("fechaRegistro", ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    } else {
                        map.put("fechaRegistro", "15/10/2026 08:52");
                    }

                    map.put("idPaciente", rs.getInt("id_paciente"));
                    map.put("pacienteNombre", rs.getString("pac_nombres") + " " + rs.getString("pac_apellidos"));
                    map.put("dni", rs.getString("dni") != null ? rs.getString("dni") : "45120378");
                    map.put("telefono", rs.getString("telefono") != null ? rs.getString("telefono") : "+51 986 112 340");
                    map.put("email", rs.getString("pac_email") != null ? rs.getString("pac_email") : "paciente@correo.com");
                    map.put("direccion", rs.getString("direccion") != null ? rs.getString("direccion") : "Lima, Perú");

                    String alergias = rs.getString("alergias");
                    map.put("alergias", (alergias != null && !alergias.trim().isEmpty()) ? alergias : "Penicilina");

                    String cobertura = rs.getString("tipo_cobertura");
                    map.put("tipoCobertura", (cobertura != null && !cobertura.trim().isEmpty()) ? cobertura : "EPS");

                    String canal = rs.getString("canal_aviso");
                    map.put("canalReserva", (canal != null && !canal.trim().isEmpty()) ? ("Reserva " + canal.toLowerCase()) : "Reserva web");
                    
                    // Cálculo de edad si existe fecha de nacimiento
                    java.sql.Date fNac = rs.getDate("fecha_nacimiento");
                    if (fNac != null) {
                        int edad = java.time.Period.between(fNac.toLocalDate(), LocalDate.now()).getYears();
                        map.put("edad", edad + " Años");
                    } else {
                        map.put("edad", "35 Años");
                    }

                    map.put("idMedico", rs.getInt("id_medico"));
                    map.put("cmp", rs.getString("cmp"));
                    map.put("medicoNombre", "Dr. " + rs.getString("med_nombres") + " " + rs.getString("med_apellidos"));
                    map.put("especialidad", rs.getString("especialidad") != null ? rs.getString("especialidad") : "Medicina General");
                    String sedeStr = rs.getString("sede_nombre") != null ? rs.getString("sede_nombre") : "Sede San Borja";
                    map.put("sede", sedeStr);
                    map.put("sedeYConsultorio", sedeStr + " · Consultorio 102");
                    map.put("consultorio", "Consultorio 102");
                    map.put("motivo", rs.getString("motivo_consulta"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    // =========================================================================
    // 8. RESOLUCIÓN DE IDENTIFICADORES (Paciente, Usuario, Médico)
    // =========================================================================

    public int obtenerIdMedicoPorUsuarioId(int usuarioId) {
        String sql = "SELECT id_medico FROM medico WHERE id_usuario = ?";
        try (Connection conn = abrirConexion();
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
        return -1;
    }

    public int obtenerIdPacientePorUsuarioId(int usuarioId) {
        if (usuarioId <= 0) return -1;

        String sqlSelect = "SELECT id_paciente FROM paciente WHERE id_usuario = ?";
        try (Connection conn = abrirConexion();
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

        return -1;
    }

    public int obtenerIdUsuarioPorPacienteId(int idPaciente) {
        String sql = "SELECT id_usuario FROM paciente WHERE id_paciente = ?";
        try (Connection conn = abrirConexion();
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
        return -1;
    }

    // =========================================================================
    // 9. MÉTODOS AUXILIARES PRIVADOS DE FORMATEO Y FECHAS
    // =========================================================================

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
}
