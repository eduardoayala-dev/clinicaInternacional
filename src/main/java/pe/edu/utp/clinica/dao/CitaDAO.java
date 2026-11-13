package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.repository.ReservaCitaRepository;
import pe.edu.utp.clinica.repository.ConsultaCitaRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private static final String ESTADO = "estado";
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
    public int reservarCitaRetenida(int paciente,int medico,String fecha,String hora,Integer motivo,String identificador) {
        return new AgendaCitaDAO(this::abrirConexion).reservarRetenida(paciente,medico,fecha,hora,motivo,identificador);
    }
    public int reservarCitaValidada(int paciente,int medico,String fecha,String hora,String urgencia,Integer motivo) {
        return new AgendaCitaDAO(this::abrirConexion).reservar(paciente,medico,fecha,hora,urgencia,motivo);
    }


    public List<Map<String, Object>> obtenerProximasCitasPaciente(int idPaciente) {
        return consultarCitas(idPaciente, ConsultaCitaRepository.Listado.PROXIMAS);
    }

    public List<Map<String, Object>> obtenerHistorialPaciente(int idPaciente) {
        var citas = consultarCitas(idPaciente, ConsultaCitaRepository.Listado.HISTORIAL);
        for (var cita : citas) {
            boolean pendiente = Set.of(ESTADO_SOLICITADA, ESTADO_CONFIRMADA).contains(cita.get(ESTADO));
            cita.put("pendienteCierre", pendiente);
            cita.put("tipo", pendiente ? "Cita pasada" : "Consulta");
            if (pendiente) cita.put("estadoTexto", "Pasada · asistencia por registrar");
        }
        return citas;
    }

    public List<Map<String, Object>> obtenerCitasCanceladasPaciente(int idPaciente) {
        return consultarCitas(idPaciente, ConsultaCitaRepository.Listado.CANCELADAS);
    }

    private LocalDateTime inicioDiaClinico() {
        return LocalDate.now(relojClinico().withZone(java.time.ZoneId.of(ZONA_CLINICA))).atStartOfDay();
    }

    private List<Map<String, Object>> consultarCitas(int paciente, ConsultaCitaRepository.Listado listado) {
        try (var conexion = abrirConexion()) {
            return new ConsultaCitaRepository(conexion).delPaciente(paciente, listado, inicioDiaClinico())
                    .stream().map(this::mapearCita).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        } catch (SQLException error) { throw new CatalogoDatosException(error); }
    }

    private Map<String, Object> mapearCita(ConsultaCitaRepository.Cita cita) {
        Map<String, Object> map = new HashMap<>();
        int idCita = cita.id();
        map.put("id", "CIT-" + idCita);
        map.put("idCita", idCita);
        map.put("idMedico", cita.medico());
        LocalDateTime ldt = cita.inicio();
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
        map.put("medico", "Dr. " + cita.nombres() + " " + cita.apellidos());
        map.put(ESPECIALIDAD, cita.especialidadNombre() != null ? cita.especialidadNombre() : "Medicina General");
        map.put("cmp", normalizarCmp(cita.cmp()));
        map.put("sede", cita.sedeNombre() != null ? cita.sedeNombre() : "Sede Central");
        map.put("consultorio", "Sin asignar");
        map.put("ubicacion", cita.sedeNombre() != null ? cita.sedeNombre() : "Sede Central");
        map.put("modalidad", "Presencial");
        String est = cita.estado();
        map.put(ESTADO, est != null ? est : ESTADO_CONFIRMADA);
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
        try (var conexion = abrirConexion()) {
            var cita = new ConsultaCitaRepository(conexion).detalle(idCita);
            if (cita == null) return null;
            var datos = mapearCita(cita);
            datos.put("idPaciente", cita.paciente() == null ? 0 : cita.paciente());
            datos.put("idSede", cita.sede() == null ? 0 : cita.sede());
            datos.put("idEspecialidad", cita.especialidad() == null ? 0 : cita.especialidad());
            datos.put("idMotivo", cita.motivo());
            datos.put("nivelUrgencia", cita.prioridad());
            return datos;
        } catch (SQLException error) { throw new CatalogoDatosException(error); }
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

    public int reprogramarCitaConId(int anterior, int paciente, int medico, String fecha, String hora, int motivo) {
        return new AgendaCitaDAO(this::abrirConexion).reprogramarConId(anterior, paciente, medico, fecha, hora, motivo, "MEDIA");
    }

    // =========================================================================
    // 5. ATENCIÓN MÉDICA Y AGENDA DE MÉDICOS
    // =========================================================================

    public boolean actualizarEstadoCita(int idCita, String nuevoEstado) {
        if (idCita <= 0 || nuevoEstado == null) return false;
        if (ESTADO_CANCELADA.equals(nuevoEstado)) return false;
        String anterior = switch (nuevoEstado) {
            case ESTADO_CONFIRMADA -> ESTADO_SOLICITADA;
            default -> null;
        };
        if (anterior == null) return false;
        String alternativo = ESTADO_CANCELADA.equals(nuevoEstado) ? ESTADO_CONFIRMADA : anterior;
        try {
            return TransaccionJdbc.ejecutar(this::abrirConexion, conn -> {
                boolean actualizado = new ReservaCitaRepository(conn).estado(idCita, null, nuevoEstado,
                        List.of(anterior, alternativo), null) == 1;
                if (actualizado) registrarCambio(conn, idCita, nuevoEstado);
                return actualizado;
            });
        } catch (AgendaException e) {
            LOGGER.warning("No se pudo actualizar el estado de la cita.");
            return false;
        }
    }

    /** Actualización condicionada al propietario y al estado actual de la cita. */
    public boolean actualizarEstadoCitaPaciente(int idCita, int idPaciente, String nuevoEstado) {
        if (ESTADO_CANCELADA.equals(nuevoEstado)) return false;
        return actualizarEstadoCitaPaciente(idCita,idPaciente,nuevoEstado,null);
    }

    public boolean actualizarEstadoCitaPaciente(int idCita, int idPaciente, String nuevoEstado, String motivo) {
        String motivoValidado=ESTADO_CANCELADA.equals(nuevoEstado)?pe.edu.utp.clinica.util.MotivoCancelacion.validar(motivo):null;
        if (idCita <= 0 || idPaciente <= 0) return false;
        if (!ESTADO_CONFIRMADA.equals(nuevoEstado) && !ESTADO_CANCELADA.equals(nuevoEstado)) return false;
        String alternativo = ESTADO_CANCELADA.equals(nuevoEstado) ? ESTADO_CONFIRMADA : ESTADO_SOLICITADA;
        try {
            return TransaccionJdbc.ejecutar(this::abrirConexion, conn -> {
                boolean actualizado = new ReservaCitaRepository(conn).estado(idCita, idPaciente, nuevoEstado,
                        List.of(ESTADO_SOLICITADA, alternativo), LocalDateTime.now(relojClinico())) == 1;
                if (actualizado) {
                    if (motivoValidado != null) auditarCancelacionPaciente(conn, idCita, idPaciente, motivoValidado);
                    registrarCambio(conn, idCita, nuevoEstado);
                }
                return actualizado;
            });
        } catch (AgendaException e) {
            LOGGER.log(java.util.logging.Level.WARNING, "No se pudo actualizar la cita del paciente", e);
            return false;
        }
    }

    private void registrarCambio(Connection conn,int cita,String estado)throws SQLException {
        if(ESTADO_CANCELADA.equals(estado)&&correosActivos()){
            new ReservaCitaRepository(conn).correoCambio(cita,null,"CANCELADA",relojClinico().instant());
        }
        avisarCambioCita(conn,cita);
    }

    public boolean estaCanceladaPorPaciente(int cita, int paciente) {
        if (cita <= 0 || paciente <= 0) return false;
        try (var conexion = abrirConexion()) {
            return new ConsultaCitaRepository(conexion).canceladaPropia(cita, paciente);
        } catch (SQLException error) { throw new CatalogoDatosException(error); }
    }
    private static void auditarCancelacionPaciente(Connection conn,int cita,int paciente,String motivo)throws SQLException {
        new ReservaCitaRepository(conn).auditarCancelacion(cita, paciente, motivo);
    }
    protected boolean correosActivos(){return Boolean.parseBoolean(System.getenv().getOrDefault("APPOINTMENT_EMAIL_ENABLED","false"));}
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
        try (var conexion = abrirConexion()) {
            return new ConsultaCitaRepository(conexion).riesgos(LocalDateTime.now(relojClinico())).stream().map(cita -> {
                Map<String, Object> datos = new HashMap<>();
                datos.put("idCita", cita.id());
                datos.put("fechaHora", cita.inicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                datos.put("paciente", cita.pacienteNombres() + " " + cita.pacienteApellidos());
                datos.put("medico", "Dr. " + cita.medicoNombres() + " " + cita.medicoApellidos());
                datos.put(ESPECIALIDAD, java.util.Objects.requireNonNullElse(cita.especialidad(), "Cardiología"));
                datos.put("sede", java.util.Objects.requireNonNullElse(cita.sede(), "Sede Central"));
                datos.put("nivelUrgencia", java.util.Objects.requireNonNullElse(cita.prioridad(), "BAJA"));
                datos.put("riesgoCancelacion", java.util.Objects.requireNonNullElse(cita.riesgo(), "MEDIO"));
                return datos;
            }).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        } catch (SQLException error) { throw new CatalogoDatosException(error); }
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

    public boolean confirmarAsistenciaDesdeNotificacion(int idNotificacion, int idUsuario) {
        return notificacionDAO.confirmarAsistenciaDesdeNotificacion(idNotificacion, idUsuario);
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
        try (var conexion = abrirConexion()) { return new ConsultaCitaRepository(conexion).medicoPorUsuario(usuarioId); }
        catch (SQLException error) { LOGGER.warning("No se pudo resolver el identificador del médico."); return -1; }
    }

    public int obtenerIdPacientePorUsuarioId(int usuarioId) {
        if (usuarioId <= 0) return -1;
        try (var conexion = abrirConexion()) { return new ConsultaCitaRepository(conexion).pacientePorUsuario(usuarioId); }
        catch (SQLException error) { LOGGER.warning("No se pudo resolver el identificador del paciente."); return -1; }
    }

    public int obtenerIdUsuarioPorPacienteId(int idPaciente) {
        try (var conexion = abrirConexion()) { return new ConsultaCitaRepository(conexion).usuarioPorPaciente(idPaciente); }
        catch (SQLException error) { LOGGER.warning("No se pudo resolver el identificador del usuario."); return -1; }
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

    private static void avisarCambioCita(Connection conn, int cita) {
        try {
            Integer sede = new ReservaCitaRepository(conn).sedeCita(cita);
            if (sede != null) TransaccionJdbc.avisarSede(sede);
        } catch (SQLException e) { LOGGER.warning("La cita se guardó; no se pudo avisar de su actualización."); }
    }
}
