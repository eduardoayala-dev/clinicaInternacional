package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.dao.AdmisionCitaDAO;
import pe.edu.utp.clinica.dao.AgendaCitaDAO;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.MedicoDAO;
import pe.edu.utp.clinica.dao.UsuarioDAO;
import pe.edu.utp.clinica.model.Usuario;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.*;

@WebServlet(name = "AdminDashboardServlet", urlPatterns = {
    "/admin/dashboard",
    "/admin/inicio",
    "/admin/medicos",
    "/admin/pacientes",
    "/admin/dashboard-operativo"
})
public class AdminDashboardServlet extends HttpServlet {
    private static final String ESTADO="estado";
    private static final String DNI="dni";
    private static final String SIN_REGISTRAR="Sin registrar";
    private static final String RUTA_DASHBOARD="/admin/dashboard";
    private static final String NOMBRES="nombres";
    private static final String APELLIDOS="apellidos";
    private static final String TELEFONO="telefono";
    private static final String HORA="hora";
    private static final String RUTA_MEDICOS="/admin/medicos";
    private static final String RUTA_PACIENTES="/admin/pacientes";
    private static final String ID_CITA="idCita";
    private static final String DIRECCION="direccion";
    private static final String FECHA="fecha";
    private static final String SEDES="sedes";
    private static final String ESPECIALIDAD="especialidad";
    private static final String SEDE="sede";
    private static final String COBERTURA="cobertura";
    private static final String FORMATO_PORCENTAJE="%.1f%%";
    private static final String EMAIL="email";
    private static final String ZONA_CLINICA="America/Lima";
    private static final String SEDE_NOMBRE="sede_nombre";

    private static final String ACTIVO="ACTIVO";
    private static final String FLASH_ERROR="flashError";
    private static final String ID_PACIENTE="idPaciente";
    private static final String MEDICO="medico";
    private static final String ID_DISPONIBILIDAD="idDisponibilidad";

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
        try {procesarGet(request,response);}
        catch(AgendaException e){responderError(response,e.status(),e.getMessage());}
        catch(IOException|ServletException e){pe.edu.utp.clinica.util.TransporteServlet.fallo(response);}
    }
    private void procesarGet(HttpServletRequest request,HttpServletResponse response) throws IOException,ServletException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(USUARIO_LOGUEADO) == null) {
            response.sendRedirect(request.getContextPath() + "/login/personal?error=acceso_denegado");
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
                request.getRequestDispatcher("/WEB-INF/views/admin/medicos.jsp").forward(request, response);
                break;
            case RUTA_PACIENTES:
                cargarDatosPacientes(request);
                request.getRequestDispatcher("/WEB-INF/views/admin/pacientes.jsp").forward(request, response);
                break;
            case "/admin/dashboard-operativo":
                cargarDatosDashboardOperativo(request);
                request.getRequestDispatcher("/WEB-INF/views/admin/dashboard_operativo.jsp").forward(request, response);
                break;
            case RUTA_DASHBOARD:
            case "/admin/inicio":
            default:
                cargarDatosDashboardInicio(request);
                request.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(request, response);
                break;
        }
        } catch (AgendaException e) {
            responderError(response,e.status(),e.getMessage());
        }
    }

    private void cargarDatosDashboardInicio(HttpServletRequest request) {
        Usuario usuario = (Usuario) request.getSession(false).getAttribute(USUARIO_LOGUEADO);
        List<Map<String, Object>> citas = obtenerCitasSalaEsperaBD(usuario.getIdUsuario());
        request.setAttribute("listaCitas", citas);

        int totalCitasBD = 0;
        int cancelacionesBD = 0;
        int cuposLibresBD = 0;
        int reasignacionesBD = 0;

        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM cita");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totalCitasBD = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM cita WHERE estado = 'CANCELADA'");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) cancelacionesBD = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM disponibilidad WHERE estado = 'PUBLICADO'");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) cuposLibresBD = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM lista_espera WHERE estado = 'ACTIVA'");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) reasignacionesBD = rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Métricas de tablero institucional directamente desde la base de datos
        request.setAttribute("totalCitas", totalCitasBD);
        request.setAttribute("cancelaciones", cancelacionesBD);
        request.setAttribute("cuposLibres", cuposLibresBD);
        request.setAttribute("reasignacionesPendientes", reasignacionesBD);

        // Sedes para el filtro
        MedicoDAO medicoDAO = new MedicoDAO();
        request.setAttribute(SEDES, medicoDAO.obtenerSedes());

        // Feed de últimas operaciones en vivo desde la base de datos
        request.setAttribute("ultimasOperaciones", obtenerUltimasOperaciones());
    }

    private void cargarDatosMedicos(HttpServletRequest request) {
        String query = request.getParameter("query");
        String especialidad = request.getParameter(ESPECIALIDAD);
        String sede = request.getParameter(SEDE);
        String estado = request.getParameter(ESTADO);
        if (estado == null || estado.isBlank()) estado = ACTIVO;

        List<Map<String, Object>> medicos = obtenerMedicosPadronBD(query, especialidad, sede, estado,ambitoSede(request));
        int totalMedicosBD = medicos.size();
        int totalActivosBD = 0;
        int totalLicenciaBD = 0;
        int totalEspecialidadesBD = 0;

        totalMedicosBD=medicos.size();
        totalActivosBD=(int)medicos.stream().filter(m->ACTIVO.equals(m.get(ESTADO))).count();
        totalLicenciaBD=(int)medicos.stream().filter(m->"LICENCIA".equals(m.get(ESTADO))).count();
        totalEspecialidadesBD=(int)medicos.stream().map(m->m.get(ESPECIALIDAD)).distinct().count();

        // Obtener sedes y especialidades para los filtros y modal
        MedicoDAO medicoDAO = new MedicoDAO();
        List<Map<String, Object>> sedes = medicoDAO.obtenerSedes();
        List<Map<String, Object>> especialidades = medicoDAO.obtenerEspecialidades();

        request.setAttribute("listaMedicos", medicos);
        request.setAttribute(SEDES, sedes);
        request.setAttribute("especialidades", especialidades);
        request.setAttribute("listaDisponibilidades", obtenerTodasDisponibilidades(ambitoSede(request)));
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

        List<Map<String, Object>> pacientes = obtenerPacientesPadronBD(query, cobertura, sede,ambitoSede(request));
        int totalPacientesBD = pacientes.size();
        // Sedes para el selector
        MedicoDAO medicoDAO = new MedicoDAO();
        request.setAttribute(SEDES, medicoDAO.obtenerSedes());

        request.setAttribute("listaPacientes", pacientes);
        request.setAttribute("sedeAdmision",new pe.edu.utp.clinica.dao.AmbitoAdmisionDAO().nombreSede(((Usuario)request.getSession(false).getAttribute(USUARIO_LOGUEADO)).getIdUsuario()));
        request.setAttribute("totalPacientes", totalPacientesBD);
        request.setAttribute("totalRegistradosFormat", String.format(Locale.US, "%,d", totalPacientesBD).replace(",", " "));
        request.setAttribute("totalVerificadosFormat", "Sin datos");

        request.setAttribute("filtroQuery", query != null ? query : "");
        request.setAttribute("filtroCobertura", cobertura != null ? cobertura : "");
        request.setAttribute("filtroSede", sede != null ? sede : "");
    }


    private void cargarDatosDashboardOperativo(HttpServletRequest request) {
        Usuario usuario=(Usuario)request.getSession(false).getAttribute(USUARIO_LOGUEADO);
        var filtro=pe.edu.utp.clinica.reporte.FiltroReporte.leer(request.getParameter("desde"),request.getParameter("hasta"),request.getParameter(ESTADO),"citas",java.time.Clock.system(java.time.ZoneId.of(ZONA_CLINICA)));
        int pagina=1;
        try {if(request.getParameter("pagina")!=null)pagina=Integer.parseInt(request.getParameter("pagina"));}
        catch(NumberFormatException e){throw new AgendaException(400,"PAGINA_INVALIDA","Selecciona una página válida.");}
        request.setAttribute("reporte",new pe.edu.utp.clinica.dao.ReporteAdmisionDAO().consultar(usuario.getIdUsuario(),filtro,pagina,false));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {procesarPost(request,response);}
        catch(IOException|ServletException e){pe.edu.utp.clinica.util.TransporteServlet.fallo(response);}
    }
    private void procesarPost(HttpServletRequest request,HttpServletResponse response) throws IOException,ServletException {
        String action = request.getParameter("action");

        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(USUARIO_LOGUEADO) instanceof Usuario usuario)
                || !"PERSONAL_ADMINISTRATIVO".equals(usuario.getRol())) {
            responderError(response,403,"Ingresa con una cuenta de admisión.");
            return;
        }
        try { comprobarAmbito(request,usuario,action); }
        catch(IllegalArgumentException e){responderError(response,400,"Revisa los identificadores.");return;}
        catch(AgendaException e){responderError(response,e.status(),e.getMessage());return;}
        switch(action==null?"":action) {
            case "admitir_cita" -> admitirCita(request,response);
            case "cancelar_cita" -> cancelarCita(request,response,session,usuario);
            case "registrar_paciente" -> registrarPaciente(request,response,usuario);
            case "editar_paciente" -> editarPaciente(request,response,session,usuario);
            case "reasignar" -> reasignarCita(request,response);
            case "cambiar_estado" -> rechazarCambioGenerico(response);
            case "registrar_cita_ventanilla" -> reservarEnVentanilla(request,response);
            case "publicar_horario_medico" -> publicarHorario(request,response);
            case "cambiar_estado_horario" -> cambiarEstadoHorario(request,response);
            case "nuevo_medico","registrarMedico" -> rechazarAltaMedico(response);
            default -> response.sendRedirect(request.getContextPath()+RUTA_DASHBOARD);
        }
}

    private void ofertarCancelacion(HttpServletRequest request,int cita) {
        try {citaDAO.ejecutarReasignacionInteligente(cita);}
        catch(AgendaException e){request.getSession().setAttribute(FLASH_ERROR,"La cita se canceló; el mantenimiento revisará la oferta del cupo.");}
    }

    private List<Map<String, Object>> obtenerMedicosPadronBD(String query, String especialidad, String sede, String estado,int ambito) {
        List<Map<String, Object>> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT m.id_medico, m.cmp, u.nombres, u.apellidos, COALESCE(p_dni.dni, 'Sin registrar') AS dni, " +
            "COALESCE(e.nombre, 'Medicina general') AS especialidad_nombre, " +
            "COALESCE(s.nombre, 'Sede San Borja') AS sede_nombre, " +
            "CASE WHEN u.activo=FALSE THEN 'INACTIVO' ELSE COALESCE(m.estado, 'ACTIVO') END AS estado " +
            "FROM medico m " +
            "JOIN usuario u ON m.id_usuario = u.id_usuario " +
            "LEFT JOIN paciente p_dni ON u.id_usuario = p_dni.id_usuario " +
            "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
            "LEFT JOIN sede s ON m.id_sede = s.id_sede " +
            "WHERE m.id_sede=? "
        );

        List<Object> params = new ArrayList<>();
        params.add(ambito);
        filtrarMedicos(sql,params,query,especialidad,sede,estado);

        sql.append("ORDER BY m.id_medico ASC");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapearMedicoPadron(rs));
                }
            }
        } catch (java.sql.SQLException e) {
            throw new AgendaException(503,"PADRON_NO_DISPONIBLE","No se pudo consultar el padrón de la sede.");
        }
        return list;
    }

    private List<Map<String, Object>> obtenerPacientesPadronBD(String query, String cobertura, String sede,int ambito) {
        List<Map<String, Object>> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT p.id_paciente, p.dni, p.fecha_nacimiento, p.telefono, p.direccion, " +
            "COALESCE(p.canal_aviso, 'WhatsApp') AS canal_aviso, " +
            "COALESCE(p.sede_preferida, 'Sede San Borja') AS sede_preferida, " +
            "COALESCE(p.tipo_cobertura, 'EPS') AS tipo_cobertura, " +
            "u.nombres, u.apellidos, u.email, " +
            "COUNT(c.id_cita) AS citas_activas " +
            "FROM paciente p " +
            "JOIN usuario u ON p.id_usuario = u.id_usuario " +
            "LEFT JOIN cita c ON p.id_paciente = c.id_paciente AND c.estado IN ('PROGRAMADA', 'CONFIRMADA', 'REPROGRAMADA', 'EN_SALA') " +
            "WHERE (EXISTS(SELECT 1 FROM sede sc WHERE sc.id_sede=? AND sc.nombre=p.sede_preferida) OR EXISTS(SELECT 1 FROM cita ca JOIN medico ma ON ma.id_medico=ca.id_medico WHERE ca.id_paciente=p.id_paciente AND ma.id_sede=?)) "
        );

        List<Object> params = new ArrayList<>();
        params.add(ambito);params.add(ambito);
        filtrarPacientes(sql,params,query,cobertura,sede);

        sql.append("ORDER BY p.id_paciente DESC");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapearPacientePadron(rs));
                }
            }
        } catch (java.sql.SQLException e) {
            throw new AgendaException(503,"PADRON_NO_DISPONIBLE","No se pudo consultar el padrón de la sede.");
        }
        return list;
    }

    private List<Map<String, Object>> obtenerCitasSalaEsperaBD(int usuario) {
        int toleranciaActual=admisionDAO.toleranciaMinutos();
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT c.id_cita,c.fecha_hora,c.estado,up.nombres AS pac_nom,up.apellidos AS pac_ape,p.dni,p.telefono,um.nombres AS med_nom,um.apellidos AS med_ape,s.nombre AS sede_nombre,a.fecha_llegada,a.llego_tarde "
                + "FROM cita c JOIN paciente p ON c.id_paciente=p.id_paciente JOIN usuario up ON p.id_usuario=up.id_usuario JOIN medico m ON c.id_medico=m.id_medico JOIN usuario um ON m.id_usuario=um.id_usuario JOIN sede s ON m.id_sede=s.id_sede "
                + "JOIN personal_administrativo pa ON pa.id_sede=m.id_sede JOIN usuario ua ON pa.id_usuario=ua.id_usuario LEFT JOIN admision_cita a ON a.id_cita=c.id_cita "
                + "WHERE pa.id_usuario=? AND ua.activo=TRUE AND ua.rol='PERSONAL_ADMINISTRATIVO' AND s.estado='ACTIVO' AND c.fecha_hora>=? AND c.fecha_hora<? ORDER BY c.fecha_hora,c.id_cita";
        java.time.LocalDate hoy = java.time.LocalDate.now(java.time.ZoneId.of(ZONA_CLINICA));
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuario); ps.setObject(2, hoy.atStartOfDay()); ps.setObject(3, hoy.plusDays(1).atStartOfDay());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put(ID_CITA, rs.getInt("id_cita"));
                    map.put(HORA, rs.getObject(FECHA_HORA, java.time.LocalDateTime.class).toLocalTime().toString());
                    map.put("pacienteNombre", rs.getString("pac_nom") + " " + rs.getString("pac_ape"));
                    map.put("pacienteDni", rs.getString(DNI));
                    map.put("pacienteTelefono", Objects.toString(rs.getString(TELEFONO), SIN_REGISTRAR));
                    map.put("sedeNombre", rs.getString(SEDE_NOMBRE));
                    map.put("consultorio", "");
                    map.put("medicoNombre", rs.getString("med_nom") + " " + rs.getString("med_ape"));
                    String estado = rs.getString(ESTADO);
                    java.time.LocalDateTime llegada = rs.getObject("fecha_llegada", java.time.LocalDateTime.class);
                    String etiqueta = switch (estado) {
                        case "EN_SALA" -> rs.getBoolean("llego_tarde") ? "Llegó tarde" : "En sala";
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
                    java.time.LocalDateTime ahora = java.time.LocalDateTime.now(java.time.ZoneId.of(ZONA_CLINICA));
                    map.put("puedeMarcarAusencia", ESTADO_CONFIRMADA.equals(estado) && llegada == null && ahora.isAfter(rs.getObject(FECHA_HORA, java.time.LocalDateTime.class).plusMinutes(toleranciaActual)));
                    map.put("puedeAnular", Set.of("SOLICITADA", ESTADO_CONFIRMADA).contains(estado));
                    String accionTexto=etiqueta;
                    if(llegada!=null) accionTexto="Llegada " + llegada.toLocalTime();
                    else if("EN_SALA".equals(estado)) accionTexto="Hora de llegada sin registrar";
                    map.put("accionTexto",accionTexto);
                    list.add(map);
                }
            }
        } catch (java.sql.SQLException e) {
            throw new AgendaException(503, "ADMISION_NO_DISPONIBLE", "No se pudo cargar la sala de espera.");
        }
        return list;
    }

    private List<Map<String, String>> obtenerUltimasOperaciones() {
        List<Map<String, String>> list = new ArrayList<>();
        String sql = "SELECT mensaje, fecha_envio FROM notificacion " +
                     "ORDER BY id_notificacion DESC LIMIT 6";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, String> o = new HashMap<>();
                Timestamp ts = rs.getTimestamp("fecha_envio");
                String hora = "09:00";
                if (ts != null) {
                    hora = String.format("%02d:%02d", ts.toLocalDateTime().getHour(), ts.toLocalDateTime().getMinute());
                }
                o.put(HORA, hora);
                String msg = rs.getString("mensaje");
                o.put("texto", msg != null ? msg : "Operación registrada en sistema");
                list.add(o);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (list.isEmpty()) {
            Map<String, String> o1 = new HashMap<>();
            o1.put(HORA, "09:00");
            o1.put("texto", "Sistema de control operativo y sala de espera iniciado");
            list.add(o1);
        }

        return list;
    }






    private List<Map<String, Object>> obtenerTodasDisponibilidades(int ambito) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT d.id_disponibilidad, d.id_medico, d.id_sede, COALESCE(s.nombre, 'Sede Central') AS sede_nombre, " +
                     "d.fecha, d.hora_inicio, d.hora_fin, COALESCE(d.duracion_minutos, 30) AS duracion_minutos, " +
                     "COALESCE(d.estado, 'PUBLICADO') AS estado, " +
                     "(SELECT COUNT(*) FROM cita c WHERE c.id_disponibilidad = d.id_disponibilidad AND c.estado NOT IN ('CANCELADA', 'REPROGRAMADA')) AS citas_agendadas " +
                     "FROM disponibilidad d " +
                     "LEFT JOIN sede s ON d.id_sede = s.id_sede " +
                     "WHERE d.id_sede=? ORDER BY d.fecha DESC, d.hora_inicio ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = consultaDisponibilidades(ps,ambito)) {
            java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            while (rs.next()) {
                Map<String, Object> d = new HashMap<>();
                d.put(ID_DISPONIBILIDAD, rs.getInt("id_disponibilidad"));
                d.put(ID_MEDICO, rs.getInt("id_medico"));
                d.put(ID_SEDE, rs.getInt("id_sede"));
                d.put("sedeNombre", rs.getString(SEDE_NOMBRE));

                java.sql.Date sqlFecha = rs.getDate(FECHA);
                d.put(FECHA, sqlFecha != null ? sqlFecha.toString() : "");

                java.sql.Time hi = rs.getTime("hora_inicio");
                java.sql.Time hf = rs.getTime("hora_fin");
                d.put("horaInicio", hi != null ? hi.toLocalTime().format(timeFmt) : "");
                d.put("horaFin", hf != null ? hf.toLocalTime().format(timeFmt) : "");
                d.put("duracionMinutos", rs.getInt("duracion_minutos"));
                d.put(ESTADO, rs.getString(ESTADO));
                d.put("citasAgendadas", rs.getInt("citas_agendadas"));
                list.add(d);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
    private static ResultSet consultaDisponibilidades(PreparedStatement ps,int sede)throws java.sql.SQLException {ps.setInt(1,sede);return ps.executeQuery();}
    private static int ambitoSede(HttpServletRequest request) {
        Usuario usuario=(Usuario)request.getSession(false).getAttribute(USUARIO_LOGUEADO);
        return new pe.edu.utp.clinica.dao.AmbitoAdmisionDAO().sede(usuario.getIdUsuario());
    }
    private static void comprobarAmbito(HttpServletRequest request,Usuario usuario,String accion) {
        var ambito=new pe.edu.utp.clinica.dao.AmbitoAdmisionDAO();
        var recurso=pe.edu.utp.clinica.dao.AmbitoAdmisionDAO.Recurso.CITA;
        String campo=null;
        if(java.util.Set.of("cancelar_cita","reasignar","cambiar_estado").contains(accion==null?"":accion))campo=ID_CITA;
        else if("editar_paciente".equals(accion)){campo=ID_PACIENTE;recurso=pe.edu.utp.clinica.dao.AmbitoAdmisionDAO.Recurso.PACIENTE;}
        else if("registrar_cita_ventanilla".equals(accion)){campo=MEDICO;recurso=pe.edu.utp.clinica.dao.AmbitoAdmisionDAO.Recurso.MEDICO;}
        else if("publicar_horario_medico".equals(accion)){campo=ID_MEDICO;recurso=pe.edu.utp.clinica.dao.AmbitoAdmisionDAO.Recurso.MEDICO;}
        else if("cambiar_estado_horario".equals(accion)){campo=ID_DISPONIBILIDAD;recurso=pe.edu.utp.clinica.dao.AmbitoAdmisionDAO.Recurso.HORARIO;}
        if(campo!=null)ambito.exigir(usuario.getIdUsuario(),recurso,Integer.parseInt(request.getParameter(campo)));
    }

    private static void responderError(HttpServletResponse response,int estado,String mensaje){
        if(response.isCommitted()) return;
        try { response.sendError(estado,mensaje); }
        catch(IOException e) { response.setStatus(500); }
    }


    private Map<String,Object> mapearMedicoPadron(ResultSet rs) throws java.sql.SQLException {
        Map<String,Object> datos=new HashMap<>();
        datos.put(ID_MEDICO,rs.getInt("id_medico"));
        datos.put("cmp",valor(rs,"cmp",SIN_REGISTRAR));
        datos.put(DNI,valor(rs,DNI,SIN_REGISTRAR));
        datos.put("nombre","Dr. "+(valor(rs,NOMBRES,"")+" "+valor(rs,APELLIDOS,"")).trim());
        String especialidad=valor(rs,"especialidad_nombre",SIN_REGISTRAR);
        datos.put(ESPECIALIDAD,especialidad);
        datos.put("especialidades",List.of(especialidad));
        datos.put(SEDE,valor(rs,SEDE_NOMBRE,SIN_REGISTRAR));
        String estado=valor(rs,ESTADO,"INACTIVO");
        datos.put(ESTADO,estado);
        datos.put("horas","Sin datos");
        return datos;
    }

    private Map<String,Object> mapearPacientePadron(ResultSet rs) throws java.sql.SQLException {
        Map<String,Object> datos=new HashMap<>();
        datos.put(ID_PACIENTE,rs.getInt("id_paciente"));
        datos.put(DNI,rs.getString(DNI));
        String nombres=valor(rs,NOMBRES,"");
        String apellidos=valor(rs,APELLIDOS,"");
        datos.put(NOMBRES,nombres);
        datos.put(APELLIDOS,apellidos);
        datos.put("nombreCompleto",(nombres+" "+apellidos).trim());
        mapearNacimiento(rs,datos);
        String direccion=valor(rs,DIRECCION,"");
        datos.put(DIRECCION,direccion);
        datos.put("zona",direccion);
        String telefono=valor(rs,TELEFONO,"");
        datos.put(TELEFONO,telefono);
        datos.put("telefonoRaw",telefono);
        datos.put(EMAIL,rs.getString(EMAIL));
        datos.put("canalAviso",valor(rs,"canal_aviso","Correo"));
        datos.put("confirmaCanal","Consultar seguimiento de asistencia");
        datos.put("sedePreferida",rs.getString("sede_preferida"));
        datos.put("tipoCobertura",rs.getString("tipo_cobertura"));
        datos.put("citasActivas",rs.getInt("citas_activas"));
        return datos;
    }

    private void filtrarMedicos(StringBuilder sql,List<Object> params,String query,String especialidad,String sede,String estado) {
        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (LOWER(CONCAT(u.nombres,' ',u.apellidos)) LIKE ? ESCAPE '!' OR LOWER(m.cmp) LIKE ? ESCAPE '!') ");
            String qLike = "%" + query.trim().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_") + "%";
            params.add(qLike);
            params.add(qLike);
        }
        if (especialidad != null && !especialidad.trim().isEmpty() && !"Todas".equalsIgnoreCase(especialidad)) {
            sql.append("AND e.nombre = ? ");
            params.add(especialidad.trim());
        }
        if (sede != null && !sede.trim().isEmpty() && !"Todas las sedes".equalsIgnoreCase(sede)) {
            sql.append("AND s.nombre = ? ");
            params.add(sede.trim());
        }
        if (estado != null && !estado.trim().isEmpty() && !"Todos".equalsIgnoreCase(estado)) {
            sql.append("AND (CASE WHEN u.activo=FALSE THEN 'INACTIVO' ELSE m.estado END) = ? ");
            params.add(estado.trim());
        }

    }

    private void filtrarPacientes(StringBuilder sql,List<Object> params,String query,String cobertura,String sede) {
        if (query != null && !query.trim().isEmpty()) {
            String qLike = "%" + query.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(p.dni) LIKE ? OR LOWER(u.nombres) LIKE ? OR LOWER(u.apellidos) LIKE ? OR LOWER(p.telefono) LIKE ?) ");
            params.add(qLike);
            params.add(qLike);
            params.add(qLike);
            params.add(qLike);
        }
        if (cobertura != null && !cobertura.trim().isEmpty() && !cobertura.toLowerCase().contains("todas")) {
            sql.append("AND LOWER(p.tipo_cobertura) LIKE ? ");
            params.add("%" + cobertura.trim().toLowerCase() + "%");
        }
        if (sede != null && !sede.trim().isEmpty() && !sede.toLowerCase().contains("todas")) {
            sql.append("AND LOWER(p.sede_preferida) LIKE ? ");
            params.add("%" + sede.trim().toLowerCase() + "%");
        }

        sql.append("GROUP BY p.id_paciente, p.dni, p.fecha_nacimiento, p.telefono, p.direccion, p.canal_aviso, p.sede_preferida, p.tipo_cobertura, u.nombres, u.apellidos, u.email ");
    }

    private void mapearNacimiento(ResultSet rs,Map<String,Object> datos) throws java.sql.SQLException {
        java.sql.Date fecha=rs.getDate("fecha_nacimiento");
        if(fecha==null){
            datos.put("edad",SIN_REGISTRAR);
            datos.put("fechaNac",SIN_REGISTRAR);
            datos.put("fechaNacInput","");
            return;
        }
        var nacimiento=fecha.toLocalDate();
        var hoy=java.time.LocalDate.now(java.time.ZoneId.of(ZONA_CLINICA));
        datos.put("edad",java.time.Period.between(nacimiento,hoy).getYears()+" años");
        datos.put("fechaNac",nacimiento.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        datos.put("fechaNacInput",nacimiento.toString());
    }
    private static String valor(ResultSet rs,String columna,String defecto) throws java.sql.SQLException {
        String valor=rs.getString(columna);
        return valor==null||valor.isBlank()?defecto:valor;
    }

    private void admitirCita(HttpServletRequest request,HttpServletResponse response) throws ServletException,IOException {
            try {
                request.getRequestDispatcher("/admin/admision").forward(request,response);
            } catch (ServletException | IOException e) {
                responderError(response,500,"No se pudo completar la operación de admisión.");
            }

    }

    private void cancelarCita(HttpServletRequest request,HttpServletResponse response,HttpSession session,Usuario usuario) throws IOException {
            try {
                int idCita=Integer.parseInt(request.getParameter(ID_CITA));
                admisionDAO.cancelar(usuario.getIdUsuario(),idCita,request.getParameter("motivo"));
                ofertarCancelacion(request,idCita);
                session.setAttribute(FLASH_MESSAGE,"Cita cancelada correctamente.");
            } catch(NumberFormatException e){session.setAttribute(FLASH_ERROR,"Selecciona una cita válida.");}
            catch(AgendaException e){session.setAttribute(FLASH_ERROR,e.getMessage());}
            response.sendRedirect(request.getContextPath()+RUTA_DASHBOARD);
    }

    private void registrarPaciente(HttpServletRequest request,HttpServletResponse response,Usuario usuario) throws IOException {
            String redirect = request.getParameter("redirect");
            if (redirect == null || redirect.trim().isEmpty()) {
                redirect = RUTA_PACIENTES;
            }
            Usuario u=usuarioPaciente(request);
            var p=perfilPaciente(request,usuario);
            String nombres=u.getNombres();
            String apellidos=u.getApellidos();
            String dni=p.getDni();
            boolean ok = usuarioDAO.crearCuentaPaciente(u, p);
            if (ok) {
                String opMsg = "Registro: nuevo paciente " + nombres + " " + apellidos + " (DNI " + dni + ") registrado en padrón";
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement("INSERT INTO notificacion (id_usuario, tipo, canal, mensaje, fecha_envio, estado_envio) VALUES (1, 'Registro', 'PLATAFORMA', ?, CURRENT_TIMESTAMP, 'ENVIADA')")) {
                    ps.setString(1, opMsg);
                    ps.executeUpdate();
                } catch (Exception ignored) {}

                request.getSession().setAttribute(FLASH_MESSAGE, "Paciente registrado. Para establecer su contraseña debe utilizar Recuperar contraseña con su DNI y correo.");
            } else {
                request.getSession().setAttribute(FLASH_ERROR, "No se pudo registrar: verifique que el DNI o correo no estén duplicados.");
            }
            response.sendRedirect(request.getContextPath() + redirect);

    }

    private void editarPaciente(HttpServletRequest request,HttpServletResponse response,HttpSession session,Usuario usuario) throws IOException {
            try {
                new pe.edu.utp.clinica.dao.AmbitoAdmisionDAO().editarPaciente(usuario.getIdUsuario(),Integer.parseInt(request.getParameter(ID_PACIENTE)),
                    new pe.edu.utp.clinica.dao.AmbitoAdmisionDAO.EdicionPaciente(request.getParameter(NOMBRES),request.getParameter(APELLIDOS),request.getParameter(TELEFONO),request.getParameter(DIRECCION),request.getParameter(COBERTURA)));
                session.setAttribute(FLASH_MESSAGE,"Datos del paciente actualizados.");
            } catch(NumberFormatException e){session.setAttribute(FLASH_ERROR,"Selecciona un paciente válido.");}
            catch(AgendaException e){session.setAttribute(FLASH_ERROR,e.getMessage());}
            response.sendRedirect(request.getContextPath()+RUTA_PACIENTES);
    }

    private void reasignarCita(HttpServletRequest request,HttpServletResponse response) throws IOException {
            try {
                int idCita = Integer.parseInt(request.getParameter(ID_CITA));
                String mensaje = citaDAO.ejecutarReasignacionInteligente(idCita);
                request.getSession().setAttribute(FLASH_MESSAGE, mensaje);
            } catch (Exception e) {
                request.getSession().setAttribute(FLASH_MESSAGE, "Error al procesar reasignación: " + e.getMessage());
            }
            response.sendRedirect(request.getContextPath() + RUTA_DASHBOARD);

    }

    private void rechazarCambioGenerico(HttpServletResponse response) {
            responderError(response,403,"Utiliza los flujos de admisión, confirmación y atención para cambiar el estado de una cita.");

    }

    private void reservarEnVentanilla(HttpServletRequest request,HttpServletResponse response) throws IOException {
            String dni = request.getParameter(DNI);
            String medicoStr = request.getParameter(MEDICO);
            String fecha = request.getParameter(FECHA);
            String hora = request.getParameter(HORA);
            int idMed = -1;
            try { idMed = Integer.parseInt(medicoStr); } catch (Exception ignored) {}

            int idPac = -1;
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement psPac = conn.prepareStatement("SELECT id_paciente FROM paciente WHERE dni = ?")) {
                psPac.setString(1, dni != null ? dni.trim() : "");
                try (ResultSet rs = psPac.executeQuery()) {
                    if (rs.next()) idPac = rs.getInt(1);
                }
            } catch (Exception ignored) {}

            int idCitaNueva = citaDAO.reservarCitaConId(idPac, idMed, fecha, hora, "MEDIA");
            if (idCitaNueva > 0) {
                request.getSession().setAttribute(FLASH_MESSAGE, "¡Cita agendada exitosamente (ID #" + idCitaNueva + ") para el " + fecha + " a las " + hora + "!");
            } else {
                request.getSession().setAttribute(FLASH_ERROR, "No se pudo agendar la cita: el horario " + (fecha != null ? fecha : "") + " " + (hora != null ? hora : "") + " ya se encuentra ocupado por otro paciente.");
            }
            response.sendRedirect(request.getContextPath() + RUTA_DASHBOARD);

    }

    private void publicarHorario(HttpServletRequest request,HttpServletResponse response) throws IOException {
            try {
                int medico = Integer.parseInt(request.getParameter(ID_MEDICO));
                int sede = Integer.parseInt(request.getParameter(ID_SEDE));
                String duracion = request.getParameter("duracion");
                int minutos = duracion == null || duracion.isBlank() ? 30 : Integer.parseInt(duracion);
                new AgendaCitaDAO().publicar(medico, sede, request.getParameter(FECHA),
                        request.getParameter("horaInicio"), request.getParameter("horaFin"), minutos);
                request.getSession().setAttribute(FLASH_MESSAGE, "Turno de atención publicado correctamente.");
            } catch (NumberFormatException e) {
                request.getSession().setAttribute(FLASH_ERROR, "Los identificadores y la duración deben ser números válidos.");
            } catch (AgendaException e) {
                request.getSession().setAttribute(FLASH_ERROR, e.getMessage());
            }
            response.sendRedirect(request.getContextPath() + RUTA_MEDICOS);

    }

    private void cambiarEstadoHorario(HttpServletRequest request,HttpServletResponse response) throws IOException {
            try {
                new AgendaCitaDAO().cambiarEstado(Integer.parseInt(request.getParameter(ID_DISPONIBILIDAD)),
                        request.getParameter("nuevoEstado"));
                request.getSession().setAttribute(FLASH_MESSAGE, "Estado del turno actualizado correctamente.");
            } catch (NumberFormatException e) {
                request.getSession().setAttribute(FLASH_ERROR, "El identificador del turno no es válido.");
            } catch (AgendaException e) {
                request.getSession().setAttribute(FLASH_ERROR, e.getMessage());
            }
            response.sendRedirect(request.getContextPath() + RUTA_MEDICOS);

    }

    private void rechazarAltaMedico(HttpServletResponse response) {
            responderError(response,403,"Las cuentas de personal se crean mediante invitación del administrador del sistema.");

    }

    private Usuario usuarioPaciente(HttpServletRequest request) {
            String nombres = request.getParameter(NOMBRES);
            String apellidos = request.getParameter(APELLIDOS);
            String dni = request.getParameter(DNI);
            String email = request.getParameter(EMAIL);
            Usuario u = new Usuario();
            u.setNombres(nombres != null ? nombres.trim() : "Paciente");
            u.setApellidos(apellidos != null ? apellidos.trim() : "Registrado");
            u.setEmail(email != null && !email.trim().isEmpty() ? email.trim() : ("paciente_" + dni + "@clinica.pe"));
            u.setRol("PACIENTE");
            u.setActivo(true);
            u.setPasswordHash(pe.edu.utp.clinica.util.SecurityUtil.hashPassword(pe.edu.utp.clinica.auth.SecretosSesion.nuevo()));

        return u;
    }
    private pe.edu.utp.clinica.model.Paciente perfilPaciente(HttpServletRequest request,Usuario usuario) {
            String dni = request.getParameter(DNI);
            String telefono = request.getParameter(TELEFONO);
            String direccion = request.getParameter(DIRECCION);
            String sedePreferida = new pe.edu.utp.clinica.dao.AmbitoAdmisionDAO().nombreSede(usuario.getIdUsuario());
            String cobertura = request.getParameter(COBERTURA);
            String fechaNacStr = request.getParameter("fecha_nacimiento");
            pe.edu.utp.clinica.model.Paciente p = new pe.edu.utp.clinica.model.Paciente();
            p.setDni(dni != null ? dni.trim() : "00000000");
            p.setTelefono(telefono != null ? telefono.trim() : "999999999");
            p.setDireccion(direccion != null && !direccion.trim().isEmpty() ? direccion.trim() : "San Borja, Lima");

            p.setFechaNacimiento(fechaPaciente(fechaNacStr));
            p.setCanalAviso("Correo");
            p.setSedePreferida(sedePreferida != null && !sedePreferida.trim().isEmpty() ? sedePreferida.trim() : "Sede San Borja");
            p.setTipoCobertura(cobertura != null && !cobertura.trim().isEmpty() ? cobertura.trim() : "EPS");

        return p;
    }
    private java.time.LocalDate fechaPaciente(String fechaNacStr) {
            java.time.LocalDate fnac = java.time.LocalDate.of(1990, 1, 1);
            if (fechaNacStr != null && !fechaNacStr.trim().isEmpty()) {
                try {
                    fnac = java.time.LocalDate.parse(fechaNacStr.trim());
                } catch (Exception ignored) {}
            }
        return fnac;
    }
}
