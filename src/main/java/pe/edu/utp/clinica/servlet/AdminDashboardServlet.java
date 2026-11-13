package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.dao.AdmisionCitaDAO;
import pe.edu.utp.clinica.dao.AgendaCitaDAO;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.dao.MedicoDAO;
import pe.edu.utp.clinica.dao.UsuarioDAO;
import pe.edu.utp.clinica.model.Usuario;

import java.io.IOException;
import java.sql.Connection;
import java.util.*;

@WebServlet(
        name = "AdminDashboardServlet",
        urlPatterns = {
            "/admin/dashboard",
            "/admin/inicio",
            "/admin/medicos",
            "/admin/pacientes",
            "/admin/dashboard-operativo"
        })
public class AdminDashboardServlet extends HttpServlet {
    private static final String ESTADO = "estado";
    private static final String DNI = "dni";
    private static final String SIN_REGISTRAR = "Sin registrar";
    private static final String RUTA_DASHBOARD = "/admin/dashboard";
    private static final String NOMBRES = "nombres";
    private static final String APELLIDOS = "apellidos";
    private static final String TELEFONO = "telefono";
    private static final String HORA = "hora";
    private static final String RUTA_MEDICOS = "/admin/medicos";
    private static final String RUTA_PACIENTES = "/admin/pacientes";
    private static final String ID_CITA = "idCita";
    private static final String DIRECCION = "direccion";
    private static final String FECHA = "fecha";
    private static final String SEDES = "sedes";
    private static final String ESPECIALIDAD = "especialidad";
    private static final String SEDE = "sede";
    private static final String COBERTURA = "cobertura";
    private static final String FORMATO_PORCENTAJE = "%.1f%%";
    private static final String EMAIL = "email";
    private static final String ZONA_CLINICA = "America/Lima";
    private static final String SEDE_NOMBRE = "sede_nombre";

    private static final String ACTIVO = "ACTIVO";
    private static final String FLASH_ERROR = "flashError";
    private static final String ID_PACIENTE = "idPaciente";
    private static final String MEDICO = "medico";
    private static final String ID_DISPONIBILIDAD = "idDisponibilidad";

    private static final String USUARIO_LOGUEADO = "usuarioLogueado";
    private static final String FLASH_MESSAGE = "flashMessage";
    private static final String ESTADO_CONFIRMADA = "CONFIRMADA";
    private static final String ID_MEDICO = "idMedico";
    private static final String ID_SEDE = "idSede";
    private static final String FECHA_HORA = "fecha_hora";

    private CitaDAO citaDAO;
    private UsuarioDAO usuarioDAO;
    private transient AdmisionCitaDAO admisionDAO;

    @Override
    public void init() throws ServletException {
        this.citaDAO = new CitaDAO();
        this.usuarioDAO = new UsuarioDAO();
        this.admisionDAO = new AdmisionCitaDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            procesarGet(request, response);
        } catch (AgendaException e) {
            responderError(response, e.status(), e.getMessage());
        } catch (IOException | ServletException e) {
            pe.edu.utp.clinica.util.TransporteServlet.fallo(response);
        }
    }

    private void procesarGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(USUARIO_LOGUEADO) == null) {
            response.sendRedirect(
                    request.getContextPath() + "/login/personal?error=acceso_denegado");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute(USUARIO_LOGUEADO);
        if ("ADMINISTRADOR".equals(usuario.getRol())) {
            response.sendRedirect(request.getContextPath() + "/sysadmin/dashboard");
            return;
        }
        if (!"PERSONAL_ADMINISTRATIVO".equals(usuario.getRol())) {
            response.sendRedirect(request.getContextPath() + "/login/personal?error=rol_invalido");
            return;
        }

        String path = request.getServletPath();
        String nombrePersonal = usuario.getNombres() + " " + usuario.getApellidos();
        request.setAttribute("nombrePersonal", nombrePersonal);

        try {
            switch (path) {
                case RUTA_MEDICOS:
                    cargarDatosMedicos(request);
                    request.getRequestDispatcher("/WEB-INF/views/admin/medicos.jsp")
                            .forward(request, response);
                    break;
                case RUTA_PACIENTES:
                    cargarDatosPacientes(request);
                    request.getRequestDispatcher("/WEB-INF/views/admin/pacientes.jsp")
                            .forward(request, response);
                    break;
                case "/admin/dashboard-operativo":
                    cargarDatosDashboardOperativo(request);
                    request.getRequestDispatcher("/WEB-INF/views/admin/dashboard_operativo.jsp")
                            .forward(request, response);
                    break;
                case RUTA_DASHBOARD:
                case "/admin/inicio":
                default:
                    cargarDatosDashboardInicio(request);
                    request.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp")
                            .forward(request, response);
                    break;
            }
        } catch (AgendaException e) {
            responderError(response, e.status(), e.getMessage());
        }
    }

    private void cargarDatosDashboardInicio(HttpServletRequest request) {
        Usuario usuario = (Usuario) request.getSession(false).getAttribute(USUARIO_LOGUEADO);
        List<Map<String, Object>> citas = obtenerCitasSalaEsperaBD(usuario.getIdUsuario());
        request.setAttribute("listaCitas", citas);

        var resumen =
                new pe.edu.utp.clinica.dao.InicioOperativoDAO().consultar(usuario.getIdUsuario());
        request.setAttribute("totalCitas", resumen.citas());
        request.setAttribute("cancelaciones", resumen.canceladas());
        request.setAttribute("cuposLibres", resumen.cupos());
        request.setAttribute("reasignacionesPendientes", resumen.ofertas());
        request.setAttribute(
                "actualizadoOperativo",
                pe.edu.utp.clinica.util.FormatoClinico.fechaHora(resumen.actualizado()));
        // Sedes para el filtro
        var ambito = new pe.edu.utp.clinica.dao.AmbitoAdmisionDAO();
        int sedeAsignada = ambito.sede(usuario.getIdUsuario());
        String nombreSede = ambito.nombreSede(usuario.getIdUsuario());
        request.setAttribute(SEDES, List.of(Map.of("id", sedeAsignada, "nombre", nombreSede)));
        request.setAttribute("nombreSedeActual", nombreSede);

        // Feed de últimas operaciones en vivo desde la base de datos
        request.setAttribute(
                "ultimasOperaciones",
                new pe.edu.utp.clinica.dao.InicioOperativoDAO()
                        .operaciones(usuario.getIdUsuario()));
    }

    private void cargarDatosMedicos(HttpServletRequest request) {
        String query = request.getParameter("query");
        String especialidad = request.getParameter(ESPECIALIDAD);
        String sede = request.getParameter(SEDE);
        String estado = request.getParameter(ESTADO);
        if (estado == null || estado.isBlank()) estado = ACTIVO;

        List<Map<String, Object>> medicos =
                obtenerMedicosPadronBD(query, especialidad, sede, estado, ambitoSede(request));
        int totalMedicosBD = medicos.size();
        int totalActivosBD = 0;
        int totalLicenciaBD = 0;
        int totalEspecialidadesBD = 0;

        totalMedicosBD = medicos.size();
        totalActivosBD = (int) medicos.stream().filter(m -> ACTIVO.equals(m.get(ESTADO))).count();
        totalLicenciaBD =
                (int) medicos.stream().filter(m -> "LICENCIA".equals(m.get(ESTADO))).count();
        totalEspecialidadesBD =
                (int) medicos.stream().map(m -> m.get(ESPECIALIDAD)).distinct().count();

        // Obtener sedes y especialidades para los filtros y modal
        MedicoDAO medicoDAO = new MedicoDAO();
        List<Map<String, Object>> sedes = medicoDAO.obtenerSedes();
        List<Map<String, Object>> especialidades = medicoDAO.obtenerEspecialidades();

        request.setAttribute("listaMedicos", medicos);
        request.setAttribute(SEDES, sedes);
        request.setAttribute("especialidades", especialidades);
        request.setAttribute(
                "listaDisponibilidades", obtenerTodasDisponibilidades(ambitoSede(request)));
        request.setAttribute("totalMedicos", totalMedicosBD);
        request.setAttribute("totalActivos", totalActivosBD);
        request.setAttribute("totalLicencia", totalLicenciaBD);
        request.setAttribute("totalEspecialidades", totalEspecialidadesBD);
        request.setAttribute("filtroQuery", query != null ? query : "");
        request.setAttribute("filtroEspecialidad", especialidad != null ? especialidad : "");
        request.setAttribute("filtroSede", sede != null ? sede : "");
        request.setAttribute("filtroEstado", estado != null ? estado : ACTIVO);
    }

    private void cargarDatosPacientes(HttpServletRequest request) {
        String query = request.getParameter("query");
        String cobertura = request.getParameter(COBERTURA);
        String sede = request.getParameter(SEDE);

        List<Map<String, Object>> pacientes =
                obtenerPacientesPadronBD(query, cobertura, sede, ambitoSede(request));
        int totalPacientesBD = pacientes.size();
        // Sedes para el selector
        MedicoDAO medicoDAO = new MedicoDAO();
        request.setAttribute(SEDES, medicoDAO.obtenerSedes());

        request.setAttribute("listaPacientes", pacientes);
        request.setAttribute(
                "sedeAdmision",
                new pe.edu.utp.clinica.dao.AmbitoAdmisionDAO()
                        .nombreSede(
                                ((Usuario) request.getSession(false).getAttribute(USUARIO_LOGUEADO))
                                        .getIdUsuario()));
        request.setAttribute("totalPacientes", totalPacientesBD);
        request.setAttribute(
                "totalRegistradosFormat",
                String.format(Locale.US, "%,d", totalPacientesBD).replace(",", " "));
        request.setAttribute("totalVerificadosFormat", "Sin datos");

        request.setAttribute("filtroQuery", query != null ? query : "");
        request.setAttribute("filtroCobertura", cobertura != null ? cobertura : "");
        request.setAttribute("filtroSede", sede != null ? sede : "");
    }

    private void cargarDatosDashboardOperativo(HttpServletRequest request) {
        Usuario usuario = (Usuario) request.getSession(false).getAttribute(USUARIO_LOGUEADO);
        var filtro =
                pe.edu.utp.clinica.reporte.FiltroReporte.leer(
                        request.getParameter("desde"),
                        request.getParameter("hasta"),
                        request.getParameter(ESTADO),
                        "citas",
                        java.time.Clock.system(java.time.ZoneId.of(ZONA_CLINICA)));
        int pagina = 1;
        try {
            if (request.getParameter("pagina") != null)
                pagina = Integer.parseInt(request.getParameter("pagina"));
        } catch (NumberFormatException e) {
            throw new AgendaException(400, "PAGINA_INVALIDA", "Selecciona una página válida.");
        }
        request.setAttribute(
                "reporte",
                new pe.edu.utp.clinica.dao.ReporteAdmisionDAO()
                        .consultar(usuario.getIdUsuario(), filtro, pagina, false));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            procesarPost(request, response);
        } catch (IOException | ServletException e) {
            pe.edu.utp.clinica.util.TransporteServlet.fallo(response);
        }
    }

    private void procesarPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String action = request.getParameter("action");

        HttpSession session = request.getSession(false);
        if (session == null
                || !(session.getAttribute(USUARIO_LOGUEADO) instanceof Usuario usuario)
                || !"PERSONAL_ADMINISTRATIVO".equals(usuario.getRol())) {
            responderError(response, 403, "Ingresa con una cuenta de admisión.");
            return;
        }
        try {
            comprobarAmbito(request, usuario, action);
        } catch (IllegalArgumentException e) {
            responderError(response, 400, "Revisa los identificadores.");
            return;
        } catch (AgendaException e) {
            responderError(response, e.status(), e.getMessage());
            return;
        }
        switch (action == null ? "" : action) {
            case "admitir_cita" -> admitirCita(request, response);
            case "cancelar_cita" -> cancelarCita(request, response, session, usuario);
            case "registrar_paciente" -> registrarPaciente(request, response, usuario);
            case "editar_paciente" -> editarPaciente(request, response, session, usuario);
            case "reasignar" -> reasignarCita(request, response);
            case "cambiar_estado" -> rechazarCambioGenerico(response);
            case "registrar_cita_ventanilla" -> reservarEnVentanilla(request, response);
            case "publicar_horario_medico" -> publicarHorario(request, response);
            case "cambiar_estado_horario" -> cambiarEstadoHorario(request, response);
            case "nuevo_medico", "registrarMedico" -> rechazarAltaMedico(response);
            default -> response.sendRedirect(request.getContextPath() + RUTA_DASHBOARD);
        }
    }

    private void ofertarCancelacion(HttpServletRequest request, int cita) {
        try {
            citaDAO.ejecutarReasignacionInteligente(cita);
        } catch (AgendaException e) {
            request.getSession()
                    .setAttribute(
                            FLASH_ERROR,
                            "La cita se canceló; el mantenimiento revisará la oferta del cupo.");
        }
    }

    private List<Map<String, Object>> obtenerMedicosPadronBD(
            String query, String especialidad, String sede, String estado, int ambito) {
        try (var c = DatabaseConfig.getConnection()) {
            var filtro =
                    new pe.edu.utp.clinica.dao.PadronMedicosDAO.Filtro(
                            query,
                            "Todas".equalsIgnoreCase(especialidad) ? "" : especialidad,
                            "Todas las sedes".equalsIgnoreCase(sede) ? "" : sede,
                            estado);
            var lista = new ArrayList<Map<String, Object>>();
            for (var m :
                    new pe.edu.utp.clinica.repository.PadronMedicosRepository(c)
                            .listar(ambito, filtro, null)) {
                Map<String, Object> d = new HashMap<>();
                d.put(ID_MEDICO, m.id());
                d.put("cmp", m.cmp());
                d.put("nombre", "Dr. " + m.nombre());
                d.put(ESPECIALIDAD, m.especialidad());
                d.put("especialidades", List.of(m.especialidad()));
                d.put(SEDE, m.sede());
                d.put(ESTADO, m.estado());
                d.put("horas", "Sin datos");
                lista.add(d);
            }
            return lista;
        } catch (java.sql.SQLException e) {
            throw new AgendaException(
                    503, "PADRON_NO_DISPONIBLE", "No se pudo consultar el padrón de la sede.");
        }
    }

    private List<Map<String, Object>> obtenerPacientesPadronBD(
            String query, String cobertura, String sede, int ambito) {
        try (var c = DatabaseConfig.getConnection()) {
            var lista = new ArrayList<Map<String, Object>>();
            for (var fila :
                    new pe.edu.utp.clinica.repository.PadronAdmisionRepository(c)
                            .pacientes(query, cobertura, sede, ambito))
                lista.add(mapearPacientePadron(fila));
            return lista;
        } catch (java.sql.SQLException e) {
            throw new AgendaException(
                    503, "PADRON_NO_DISPONIBLE", "No se pudo consultar el padrón de la sede.");
        }
    }

    private List<Map<String, Object>> obtenerCitasSalaEsperaBD(int usuario) {
        int toleranciaActual = admisionDAO.toleranciaMinutos();
        List<Map<String, Object>> list = new ArrayList<>();
        java.time.LocalDate hoy = java.time.LocalDate.now(java.time.ZoneId.of(ZONA_CLINICA));
        try (Connection conn = DatabaseConfig.getConnection()) {
            var ambito =
                    new pe.edu.utp.clinica.repository.AmbitoAdmisionRepository(conn).sede(usuario);
            if (ambito == null)
                throw new AgendaException(
                        403, "SEDE_NO_ASIGNADA", "No tienes una sede activa asignada.");
            int sede = ambito.id();
            for (var rs :
                    new pe.edu.utp.clinica.repository.PadronAdmisionRepository(conn)
                            .sala(sede, hoy)) {
                Map<String, Object> map = new HashMap<>();
                map.put(ID_CITA, rs.entero("id_cita"));
                map.put(
                        HORA,
                        rs.valor(FECHA_HORA, java.time.LocalDateTime.class)
                                .toLocalTime()
                                .toString());
                map.put("pacienteNombre", rs.texto("pac_nom") + " " + rs.texto("pac_ape"));
                map.put("pacienteDni", rs.texto(DNI));
                map.put("pacienteTelefono", Objects.toString(rs.texto(TELEFONO), SIN_REGISTRAR));
                map.put("sedeNombre", rs.texto(SEDE_NOMBRE));
                map.put("consultorio", "");
                map.put("medicoNombre", rs.texto("med_nom") + " " + rs.texto("med_ape"));
                String estado = rs.texto(ESTADO);
                java.time.LocalDateTime llegada =
                        rs.valor("fecha_llegada", java.time.LocalDateTime.class);
                String etiqueta =
                        switch (estado) {
                            case "EN_SALA" ->
                                    rs.booleano("llego_tarde") ? "Llegó tarde" : "En sala";
                            case "ATENDIDA" -> "Atendida";
                            case "EN_ATENCION" -> "En atención";
                            case "NO_ATENDIDA" -> "No atendida";
                            case "NO_ASISTIO" -> "No asistió";
                            case "CANCELADA" -> "Cancelada";
                            case "REPROGRAMADA" -> "Reprogramada";
                            case "SOLICITADA" -> "Sin confirmar";
                            default -> "Confirmada";
                        };
                map.put(ESTADO, etiqueta);
                map.put("puedeAdmitir", ESTADO_CONFIRMADA.equals(estado));
                java.time.LocalDateTime ahora =
                        java.time.LocalDateTime.now(java.time.ZoneId.of(ZONA_CLINICA));
                map.put(
                        "puedeMarcarAusencia",
                        ESTADO_CONFIRMADA.equals(estado)
                                && llegada == null
                                && ahora.isAfter(
                                        rs.valor(FECHA_HORA, java.time.LocalDateTime.class)
                                                .plusMinutes(toleranciaActual)));
                map.put("puedeAnular", Set.of("SOLICITADA", ESTADO_CONFIRMADA).contains(estado));
                String accionTexto = etiqueta;
                if (llegada != null) accionTexto = "Llegada " + llegada.toLocalTime();
                else if ("EN_SALA".equals(estado)) accionTexto = "Hora de llegada sin registrar";
                map.put("accionTexto", accionTexto);
                list.add(map);
            }
        } catch (java.sql.SQLException e) {
            throw new AgendaException(
                    503, "ADMISION_NO_DISPONIBLE", "No se pudo cargar la sala de espera.");
        }
        return list;
    }

    private List<Map<String, Object>> obtenerTodasDisponibilidades(int ambito) {
        try (var conn = DatabaseConfig.getConnection()) {
            var lista = new ArrayList<Map<String, Object>>();
            var formato = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            for (var r :
                    new pe.edu.utp.clinica.repository.PadronAdmisionRepository(conn)
                            .disponibilidades(ambito)) {
                Map<String, Object> d = new HashMap<>();
                d.put(ID_DISPONIBILIDAD, r.entero("id_disponibilidad"));
                d.put(ID_MEDICO, r.entero("id_medico"));
                d.put(ID_SEDE, r.entero("id_sede"));
                d.put("sedeNombre", r.texto("sede_nombre"));
                d.put(FECHA, Objects.toString(r.valor("fecha"), ""));
                var inicio = r.valor("hora_inicio", java.time.LocalTime.class);
                var fin = r.valor("hora_fin", java.time.LocalTime.class);
                d.put("horaInicio", inicio == null ? "" : inicio.format(formato));
                d.put("horaFin", fin == null ? "" : fin.format(formato));
                d.put("duracionMinutos", r.entero("duracion_minutos"));
                d.put(ESTADO, r.texto("estado"));
                d.put("citasAgendadas", r.entero("citas_agendadas"));
                lista.add(d);
            }
            return lista;
        } catch (java.sql.SQLException e) {
            throw new AgendaException(
                    503,
                    "HORARIOS_NO_DISPONIBLES",
                    "No se pudieron consultar los horarios de la sede.");
        }
    }

    private static int ambitoSede(HttpServletRequest request) {
        Usuario usuario = (Usuario) request.getSession(false).getAttribute(USUARIO_LOGUEADO);
        return new pe.edu.utp.clinica.dao.AmbitoAdmisionDAO().sede(usuario.getIdUsuario());
    }

    private static void comprobarAmbito(
            HttpServletRequest request, Usuario usuario, String accion) {
        var ambito = new pe.edu.utp.clinica.dao.AmbitoAdmisionDAO();
        var recurso = pe.edu.utp.clinica.dao.AmbitoAdmisionDAO.Recurso.CITA;
        String campo = null;
        if (java.util.Set.of("cancelar_cita", "reasignar", "cambiar_estado")
                .contains(accion == null ? "" : accion)) campo = ID_CITA;
        else if ("editar_paciente".equals(accion)) {
            campo = ID_PACIENTE;
            recurso = pe.edu.utp.clinica.dao.AmbitoAdmisionDAO.Recurso.PACIENTE;
        } else if ("registrar_cita_ventanilla".equals(accion)) {
            campo = MEDICO;
            recurso = pe.edu.utp.clinica.dao.AmbitoAdmisionDAO.Recurso.MEDICO;
        } else if ("publicar_horario_medico".equals(accion)) {
            campo = ID_MEDICO;
            recurso = pe.edu.utp.clinica.dao.AmbitoAdmisionDAO.Recurso.MEDICO;
        } else if ("cambiar_estado_horario".equals(accion)) {
            campo = ID_DISPONIBILIDAD;
            recurso = pe.edu.utp.clinica.dao.AmbitoAdmisionDAO.Recurso.HORARIO;
        }
        if (campo != null)
            ambito.exigir(
                    usuario.getIdUsuario(), recurso, Integer.parseInt(request.getParameter(campo)));
    }

    private static void responderError(HttpServletResponse response, int estado, String mensaje) {
        if (response.isCommitted()) return;
        try {
            response.sendError(estado, mensaje);
        } catch (IOException e) {
            response.setStatus(500);
        }
    }

    private Map<String, Object> mapearPacientePadron(pe.edu.utp.clinica.persistence.FilaConsulta rs)
            throws java.sql.SQLException {
        Map<String, Object> datos = new HashMap<>();
        datos.put(ID_PACIENTE, rs.entero("id_paciente"));
        datos.put(DNI, rs.texto(DNI));
        String nombres = valor(rs, NOMBRES, "");
        String apellidos = valor(rs, APELLIDOS, "");
        datos.put(NOMBRES, nombres);
        datos.put(APELLIDOS, apellidos);
        datos.put("nombreCompleto", (nombres + " " + apellidos).trim());
        mapearNacimiento(rs, datos);
        String direccion = valor(rs, DIRECCION, "");
        datos.put(DIRECCION, direccion);
        datos.put("zona", direccion);
        String telefono = valor(rs, TELEFONO, "");
        datos.put(TELEFONO, telefono);
        datos.put("telefonoRaw", telefono);
        datos.put(EMAIL, rs.texto(EMAIL));
        datos.put("canalAviso", valor(rs, "canal_aviso", "Correo"));
        datos.put("confirmaCanal", "Consultar seguimiento de asistencia");
        datos.put("sedePreferida", rs.texto("sede_preferida"));
        datos.put("tipoCobertura", rs.texto("tipo_cobertura"));
        datos.put("citasActivas", rs.entero("citas_activas"));
        return datos;
    }

    private void mapearNacimiento(
            pe.edu.utp.clinica.persistence.FilaConsulta rs, Map<String, Object> datos)
            throws java.sql.SQLException {
        java.time.LocalDate fecha = rs.valor("fecha_nacimiento", java.time.LocalDate.class);
        if (fecha == null) {
            datos.put("edad", SIN_REGISTRAR);
            datos.put("fechaNac", SIN_REGISTRAR);
            datos.put("fechaNacInput", "");
            return;
        }
        var nacimiento = fecha;
        var hoy = java.time.LocalDate.now(java.time.ZoneId.of(ZONA_CLINICA));
        datos.put("edad", java.time.Period.between(nacimiento, hoy).getYears() + " años");
        datos.put(
                "fechaNac",
                nacimiento.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        datos.put("fechaNacInput", nacimiento.toString());
    }

    private static String valor(
            pe.edu.utp.clinica.persistence.FilaConsulta rs, String columna, String defecto)
            throws java.sql.SQLException {
        String valor = rs.texto(columna);
        return valor == null || valor.isBlank() ? defecto : valor;
    }

    private void admitirCita(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.getRequestDispatcher("/admin/admision").forward(request, response);
        } catch (ServletException | IOException e) {
            responderError(response, 500, "No se pudo completar la operación de admisión.");
        }
    }

    private void cancelarCita(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            Usuario usuario)
            throws IOException {
        try {
            int idCita = Integer.parseInt(request.getParameter(ID_CITA));
            admisionDAO.cancelar(usuario.getIdUsuario(), idCita, request.getParameter("motivo"));
            ofertarCancelacion(request, idCita);
            session.setAttribute(FLASH_MESSAGE, "Cita cancelada correctamente.");
        } catch (NumberFormatException e) {
            session.setAttribute(FLASH_ERROR, "Selecciona una cita válida.");
        } catch (AgendaException e) {
            session.setAttribute(FLASH_ERROR, e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + RUTA_DASHBOARD);
    }

    private void registrarPaciente(
            HttpServletRequest request, HttpServletResponse response, Usuario usuario)
            throws IOException {
        String redirect = request.getParameter("redirect");
        if (redirect == null || redirect.trim().isEmpty()) {
            redirect = RUTA_PACIENTES;
        }
        Usuario u = usuarioPaciente(request);
        var p = perfilPaciente(request, usuario);
        String nombres = u.getNombres();
        String apellidos = u.getApellidos();
        String dni = p.getDni();
        boolean ok = usuarioDAO.crearCuentaPaciente(u, p);
        if (ok) {
            String opMsg =
                    "Registro: nuevo paciente "
                            + nombres
                            + " "
                            + apellidos
                            + " (DNI "
                            + dni
                            + ") registrado en padrón";
            usuarioDAO.registrarAuditoria(
                    usuario.getIdUsuario(),
                    request.getRemoteAddr(),
                    "PACIENTE_REGISTRADO",
                    opMsg,
                    false);

            request.getSession()
                    .setAttribute(
                            FLASH_MESSAGE,
                            "Paciente registrado. Para establecer su contraseña debe utilizar"
                                + " Recuperar contraseña con su DNI y correo.");
        } else {
            request.getSession()
                    .setAttribute(
                            FLASH_ERROR,
                            "No se pudo registrar: verifique que el DNI o correo no estén"
                                + " duplicados.");
        }
        response.sendRedirect(request.getContextPath() + redirect);
    }

    private void editarPaciente(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            Usuario usuario)
            throws IOException {
        try {
            new pe.edu.utp.clinica.dao.AmbitoAdmisionDAO()
                    .editarPaciente(
                            usuario.getIdUsuario(),
                            Integer.parseInt(request.getParameter(ID_PACIENTE)),
                            new pe.edu.utp.clinica.dao.AmbitoAdmisionDAO.EdicionPaciente(
                                    request.getParameter(NOMBRES),
                                    request.getParameter(APELLIDOS),
                                    request.getParameter(TELEFONO),
                                    request.getParameter(DIRECCION),
                                    request.getParameter(COBERTURA)));
            session.setAttribute(FLASH_MESSAGE, "Datos del paciente actualizados.");
        } catch (NumberFormatException e) {
            session.setAttribute(FLASH_ERROR, "Selecciona un paciente válido.");
        } catch (AgendaException e) {
            session.setAttribute(FLASH_ERROR, e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + RUTA_PACIENTES);
    }

    private void reasignarCita(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            int idCita = Integer.parseInt(request.getParameter(ID_CITA));
            String mensaje = citaDAO.ejecutarReasignacionInteligente(idCita);
            request.getSession().setAttribute(FLASH_MESSAGE, mensaje);
        } catch (Exception e) {
            request.getSession()
                    .setAttribute(
                            FLASH_MESSAGE, "Error al procesar reasignación: " + e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + RUTA_DASHBOARD);
    }

    private void rechazarCambioGenerico(HttpServletResponse response) {
        responderError(
                response,
                403,
                "Utiliza los flujos de admisión, confirmación y atención para cambiar el estado de"
                    + " una cita.");
    }

    private void reservarEnVentanilla(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String dni = request.getParameter(DNI);
        String medicoStr = request.getParameter(MEDICO);
        String fecha = request.getParameter(FECHA);
        String hora = request.getParameter(HORA);
        int idMed = -1;
        try {
            idMed = Integer.parseInt(medicoStr);
        } catch (Exception ignored) {
        }

        int idPac = -1;
        try (var conn = DatabaseConfig.getConnection()) {
            idPac =
                    new pe.edu.utp.clinica.repository.PadronAdmisionRepository(conn)
                            .paciente(dni == null ? "" : dni.trim());
        } catch (java.sql.SQLException e) {
            throw new AgendaException(
                    503, "PACIENTE_NO_DISPONIBLE", "No se pudo consultar el paciente.");
        }

        int idCitaNueva = citaDAO.reservarCitaConId(idPac, idMed, fecha, hora, "MEDIA");
        if (idCitaNueva > 0) {
            request.getSession()
                    .setAttribute(
                            FLASH_MESSAGE,
                            "¡Cita agendada exitosamente (ID #"
                                    + idCitaNueva
                                    + ") para el "
                                    + fecha
                                    + " a las "
                                    + hora
                                    + "!");
        } else {
            request.getSession()
                    .setAttribute(
                            FLASH_ERROR,
                            "No se pudo agendar la cita: el horario "
                                    + (fecha != null ? fecha : "")
                                    + " "
                                    + (hora != null ? hora : "")
                                    + " ya se encuentra ocupado por otro paciente.");
        }
        response.sendRedirect(request.getContextPath() + RUTA_DASHBOARD);
    }

    private void publicarHorario(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            int medico = Integer.parseInt(request.getParameter(ID_MEDICO));
            int sede = Integer.parseInt(request.getParameter(ID_SEDE));
            String duracion = request.getParameter("duracion");
            int minutos = duracion == null || duracion.isBlank() ? 30 : Integer.parseInt(duracion);
            new AgendaCitaDAO()
                    .publicar(
                            medico,
                            sede,
                            request.getParameter(FECHA),
                            request.getParameter("horaInicio"),
                            request.getParameter("horaFin"),
                            minutos);
            request.getSession()
                    .setAttribute(FLASH_MESSAGE, "Turno de atención publicado correctamente.");
        } catch (NumberFormatException e) {
            request.getSession()
                    .setAttribute(
                            FLASH_ERROR,
                            "Los identificadores y la duración deben ser números válidos.");
        } catch (AgendaException e) {
            request.getSession().setAttribute(FLASH_ERROR, e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + RUTA_MEDICOS);
    }

    private void cambiarEstadoHorario(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            new AgendaCitaDAO()
                    .cambiarEstado(
                            Integer.parseInt(request.getParameter(ID_DISPONIBILIDAD)),
                            request.getParameter("nuevoEstado"));
            request.getSession()
                    .setAttribute(FLASH_MESSAGE, "Estado del turno actualizado correctamente.");
        } catch (NumberFormatException e) {
            request.getSession()
                    .setAttribute(FLASH_ERROR, "El identificador del turno no es válido.");
        } catch (AgendaException e) {
            request.getSession().setAttribute(FLASH_ERROR, e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + RUTA_MEDICOS);
    }

    private void rechazarAltaMedico(HttpServletResponse response) {
        responderError(
                response,
                403,
                "Las cuentas de personal se crean mediante invitación del administrador del"
                    + " sistema.");
    }

    private Usuario usuarioPaciente(HttpServletRequest request) {
        String nombres = request.getParameter(NOMBRES);
        String apellidos = request.getParameter(APELLIDOS);
        String dni = request.getParameter(DNI);
        String email = request.getParameter(EMAIL);
        Usuario u = new Usuario();
        u.setNombres(nombres != null ? nombres.trim() : "Paciente");
        u.setApellidos(apellidos != null ? apellidos.trim() : "Registrado");
        u.setEmail(
                email != null && !email.trim().isEmpty()
                        ? email.trim()
                        : ("paciente_" + dni + "@clinica.pe"));
        u.setRol("PACIENTE");
        u.setActivo(true);
        u.setPasswordHash(
                pe.edu.utp.clinica.util.SecurityUtil.hashPassword(
                        pe.edu.utp.clinica.auth.SecretosSesion.nuevo()));

        return u;
    }

    private pe.edu.utp.clinica.model.Paciente perfilPaciente(
            HttpServletRequest request, Usuario usuario) {
        String dni = request.getParameter(DNI);
        String telefono = request.getParameter(TELEFONO);
        String direccion = request.getParameter(DIRECCION);
        String sedePreferida =
                new pe.edu.utp.clinica.dao.AmbitoAdmisionDAO().nombreSede(usuario.getIdUsuario());
        String cobertura = request.getParameter(COBERTURA);
        String fechaNacStr = request.getParameter("fecha_nacimiento");
        pe.edu.utp.clinica.model.Paciente p = new pe.edu.utp.clinica.model.Paciente();
        p.setDni(dni != null ? dni.trim() : "00000000");
        p.setTelefono(telefono != null ? telefono.trim() : "999999999");
        p.setDireccion(
                direccion != null && !direccion.trim().isEmpty()
                        ? direccion.trim()
                        : "San Borja, Lima");

        p.setFechaNacimiento(fechaPaciente(fechaNacStr));
        p.setCanalAviso("Correo");
        p.setSedePreferida(
                sedePreferida != null && !sedePreferida.trim().isEmpty()
                        ? sedePreferida.trim()
                        : "Sede San Borja");
        p.setTipoCobertura(
                cobertura != null && !cobertura.trim().isEmpty() ? cobertura.trim() : "EPS");

        return p;
    }

    private java.time.LocalDate fechaPaciente(String fechaNacStr) {
        java.time.LocalDate fnac = java.time.LocalDate.of(1990, 1, 1);
        if (fechaNacStr != null && !fechaNacStr.trim().isEmpty()) {
            try {
                fnac = java.time.LocalDate.parse(fechaNacStr.trim());
            } catch (Exception ignored) {
            }
        }
        return fnac;
    }
}
