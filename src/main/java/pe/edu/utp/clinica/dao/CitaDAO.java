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
    private static final String ZONA_CLINICA = "America/Lima";
    private static final String ESTADO_CANCELADA = "CANCELADA";
    private static final String ESTADO_SOLICITADA = "SOLICITADA";
    private static final String ESTADO_CONFIRMADA = "CONFIRMADA";
    private static final String ESPECIALIDAD = "especialidad";

    private static final Logger LOGGER = Logger.getLogger(CitaDAO.class.getName());

    protected Connection abrirConexion() throws SQLException {
        return DatabaseConfig.getConnection();
    }


    private final MedicoDAO medicoDAO = new MedicoDAO();
    private final NotificacionDAO notificacionDAO = new NotificacionDAO(this::abrirConexion, java.time.Clock.system(java.time.ZoneId.of(ZONA_CLINICA)));
    private final ListaEsperaDAO listaEsperaDAO = new ListaEsperaDAO(this::abrirConexion, java.time.Clock.system(java.time.ZoneId.of(ZONA_CLINICA)));

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

    public List<Map<String, Object>> obtenerMedicosParaEspera() {
        return medicoDAO.obtenerMedicosParaEspera();
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
                     "WHERE c.id_paciente = ? AND c.estado IN ('SOLICITADA','CONFIRMADA','EN_SALA','EN_ATENCION') " +
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
                     "WHERE c.id_paciente = ? AND c.estado IN ('ATENDIDA','NO_ASISTIO','NO_ATENDIDA') " +
                     "ORDER BY c.fecha_hora DESC";
        List<Map<String, Object>> list = ejecutarConsultaCitas(sql, idPaciente);
        for (Map<String, Object> c : list) {
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
        map.put(ESPECIALIDAD, rs.getString(ESPECIALIDAD) != null ? rs.getString(ESPECIALIDAD) : "Medicina General");
        map.put("cmp", normalizarCmp(rs.getString("cmp")));
        map.put("sede", rs.getString("sede") != null ? rs.getString("sede") : "Sede Central");
        map.put("consultorio", "Sin asignar");
        map.put("ubicacion", rs.getString("sede") != null ? rs.getString("sede") : "Sede Central");
        map.put("modalidad", "Presencial");
        String est = rs.getString("estado");
        map.put("estado", est != null ? est : ESTADO_CONFIRMADA);
        map.put("estadoTexto", switch(est == null ? "" : est) {
            case "EN_ATENCION" -> "En atención";
            case "NO_ATENDIDA" -> "No atendida";
            case "NO_ASISTIO" -> "No asistió";
            case "ATENDIDA" -> "Atendida";
            case "EN_SALA" -> "En sala";
            case ESTADO_SOLICITADA -> "Solicitada";
            case ESTADO_CONFIRMADA -> "Confirmada";
            default -> est == null ? "Sin registrar" : est;
        });
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
        String anterior = switch (nuevoEstado) {
            case ESTADO_CONFIRMADA, ESTADO_CANCELADA -> ESTADO_SOLICITADA;
            default -> null;
        };
        if (anterior == null) return false;
        String alternativo = ESTADO_CANCELADA.equals(nuevoEstado) ? ESTADO_CONFIRMADA : anterior;
        String sql = "UPDATE cita SET estado=? WHERE id_cita=? AND (estado=? OR estado=?)";
        try (Connection conn = abrirConexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idCita);
            ps.setString(3, anterior);
            ps.setString(4, alternativo);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            LOGGER.warning("No se pudo actualizar el estado de la cita.");
            return false;
        }
    }

    /** Actualización condicionada al propietario y al estado actual de la cita. */
    public boolean actualizarEstadoCitaPaciente(int idCita, int idPaciente, String nuevoEstado) {
        if (idCita <= 0 || idPaciente <= 0) return false;
        if (!ESTADO_CONFIRMADA.equals(nuevoEstado) && !ESTADO_CANCELADA.equals(nuevoEstado)) return false;
        String alternativo = ESTADO_CANCELADA.equals(nuevoEstado) ? ESTADO_CONFIRMADA : ESTADO_SOLICITADA;
        String sql = "UPDATE cita SET estado=? WHERE id_cita=? AND id_paciente=? AND (estado=? OR estado=?)";
        try (Connection conn = abrirConexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idCita);
            ps.setInt(3, idPaciente);
            ps.setString(4, ESTADO_SOLICITADA);
            ps.setString(5, alternativo);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            LOGGER.log(java.util.logging.Level.WARNING, "No se pudo actualizar la cita del paciente", e);
            return false;
        }
    }

    protected java.time.Clock relojClinico() {
        return java.time.Clock.system(java.time.ZoneId.of(ZONA_CLINICA));
    }

    private MedicoPortalDAO portalMedico() { return new MedicoPortalDAO(this::abrirConexion,relojClinico()); }

    public Map<String,Object> obtenerPerfilPortalMedico(int usuario) { return portalMedico().perfil(usuario); }

    public boolean actualizarAtencionMedica(int cita,int medico,String estado,String prioridad,String fecha,String indicaciones) {
        try { return actualizarAtencionMedicaValidada(cita,medico,estado,prioridad,fecha,indicaciones); }
        catch(AgendaException e) { return false; }
    }

    public boolean iniciarAtencionMedica(int cita,int medico) { return portalMedico().iniciar(medico,cita); }
    public boolean registrarNoAtencionMedica(int cita,int medico,String motivo) { return portalMedico().noAtender(medico,cita,motivo); }
    public int obtenerAtencionActivaMedico(int medico) { return portalMedico().activa(medico); }

    public boolean actualizarAtencionMedicaValidada(int cita,int medico,String estado,String prioridad,String fecha,String indicaciones) {
        return portalMedico().guardar(medico,cita,estado,prioridad,fecha,indicaciones);
    }

    public boolean finalizarConsultaClinica(int cita,int medico,String prioridad,String fecha,String indicaciones,pe.edu.utp.clinica.model.medico.ConsultaClinica consulta) {
        if (consulta == null) throw new AgendaException(400,"CONSULTA_CLINICA_INVALIDA","Completa el registro clínico.");
        return new AtencionCitaDAO(this::abrirConexion,relojClinico()).finalizar(medico,cita,prioridad,fecha,indicaciones,consulta);
    }

    public List<Map<String,Object>> obtenerAgendaMedico(int medico) {
        return obtenerAgendaMedico(medico,LocalDate.now(relojClinico()));
    }

    public List<Map<String,Object>> obtenerAgendaMedico(int medico,LocalDate desde,LocalDate hasta) {
        return portalMedico().agenda(medico,desde,hasta);
    }

    public List<Map<String,Object>> obtenerAgendaMedico(int medico,LocalDate fecha) {
        return portalMedico().agenda(medico,fecha);
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
                map.put(ESPECIALIDAD, rs.getString(ESPECIALIDAD) != null ? rs.getString(ESPECIALIDAD) : "Cardiología");
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

    public String ejecutarReasignacionInteligente(int original) {
        int oferta = new OfertasDAO(this::abrirConexion, java.time.Clock.system(java.time.ZoneId.of(ZONA_CLINICA))).generar(original);
        return oferta > 0 ? "Se creó una oferta de cupo para un paciente compatible en lista de espera."
                : "No se generó una nueva oferta para este cupo.";
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
        return listaEsperaDAO.aceptarOfertaCupo(idPaciente, idListaEspera);
    }

    public boolean rechazarOfertaCupo(int idPaciente, int idListaEspera) {
        return listaEsperaDAO.rechazarOfertaCupo(idPaciente, idListaEspera);
    }

    public Map<String,Object> obtenerDetalleCitaMedico(int cita,int medico) {
        return portalMedico().detalle(medico,cita);
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
