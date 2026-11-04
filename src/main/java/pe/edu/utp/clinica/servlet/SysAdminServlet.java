package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import pe.edu.utp.clinica.dao.UsuarioDAO;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.config.DatabaseConfig;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "SysAdminServlet", urlPatterns = {
    "/sysadmin/dashboard",
    "/sysadmin/inicio",
    "/sysadmin/usuarios",
    "/sysadmin/sedes",
    "/sysadmin/priorizacion",
    "/sysadmin/analitica"
})
public class SysAdminServlet extends HttpServlet {
    private static final String NOMBRE="nombre";
    private static final String FLASH_MESSAGE="flashMessage";
    private static final String FLASH_ERROR="flashError";
    private static final String ESTADO="estado";


    private UsuarioDAO usuarioDAO;

    private pe.edu.utp.clinica.dao.SedeDAO sedeDAO;

    @Override
    public void init() throws ServletException {
        this.usuarioDAO = new UsuarioDAO();

        this.sedeDAO = new pe.edu.utp.clinica.dao.SedeDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {procesarGet(request,response);}
        catch(pe.edu.utp.clinica.dao.AgendaException e){try{response.sendError(e.status(),e.getMessage());}catch(IOException ex){pe.edu.utp.clinica.util.TransporteServlet.fallo(response);}}
        catch(IOException|ServletException e){pe.edu.utp.clinica.util.TransporteServlet.fallo(response);}
    }
    private void procesarGet(HttpServletRequest request,HttpServletResponse response) throws IOException,ServletException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login/personal?error=sesion_requerida");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (!"ADMINISTRADOR".equals(usuario.getRol())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso no autorizado al módulo de Administrador del Sistema.");
            return;
        }

        String path = request.getServletPath();
        request.setAttribute("nombreAdmin", usuario.getNombreCompleto());

        switch (path) {
            case "/sysadmin/usuarios":
                List<Usuario> todos = usuarioDAO.obtenerTodos();
                List<Usuario> personal = new ArrayList<>();
                for (Usuario u : todos) {
                    if (!"PACIENTE".equals(u.getRol())) {
                        personal.add(u);
                    }
                }
                request.setAttribute("listaUsuarios", personal);
                try {request.setAttribute("estadosInvitacion",new pe.edu.utp.clinica.dao.InvitacionPersonalDAO().estados(usuario.getIdUsuario()));}
                catch(pe.edu.utp.clinica.dao.AgendaException e){response.sendError(e.status(),e.getMessage());return;}
                Map<String, Object> metricasUsuarios = usuarioDAO.obtenerMetricasPersonal();
                request.setAttribute("metricasUsuarios", metricasUsuarios);
                cargarSedesYEspecialidades(request);
                request.getRequestDispatcher("/WEB-INF/views/sysadmin/usuarios.jsp").forward(request, response);
                break;

            case "/sysadmin/sedes":
                response.sendRedirect(request.getContextPath()+"/sysadmin/catalogo-sedes");
                break;

            case "/sysadmin/priorizacion":
                request.getRequestDispatcher("/sysadmin/configuracion-ofertas").forward(request, response);
                break;

            case "/sysadmin/analitica":
                cargarPanelSistema(request);
                request.getRequestDispatcher("/WEB-INF/views/sysadmin/analitica.jsp").forward(request, response);
                break;

            case "/sysadmin/dashboard":
            case "/sysadmin/inicio":
            default:
                cargarPanelSistema(request);
                request.getRequestDispatcher("/WEB-INF/views/sysadmin/dashboard.jsp").forward(request, response);
                break;
        }
    }

    protected pe.edu.utp.clinica.dao.PanelSistemaDAO panelSistema(){return new pe.edu.utp.clinica.dao.PanelSistemaDAO();}
    private void cargarPanelSistema(HttpServletRequest request) {
        var hoy=java.time.LocalDate.now(java.time.ZoneId.of("America/Lima"));
        try {
            String desde=request.getParameter("desde"),hasta=request.getParameter("hasta");
            var filtro=new pe.edu.utp.clinica.dao.PanelSistemaDAO.Filtro(
                desde==null||desde.isBlank()?hoy.minusDays(29):java.time.LocalDate.parse(desde),
                hasta==null||hasta.isBlank()?hoy:java.time.LocalDate.parse(hasta),
                filtroId(request.getParameter("sede")),filtroId(request.getParameter("especialidad")));
            var actor=(Usuario)request.getSession(false).getAttribute("usuarioLogueado");
            request.setAttribute("panelSistema",panelSistema().consultar(actor.getIdUsuario(),filtro));
        }catch(java.time.DateTimeException|NumberFormatException e){throw new pe.edu.utp.clinica.dao.AgendaException(400,"FILTROS_INVALIDOS","Revisa las fechas y los filtros seleccionados.");}
    }
    private static int filtroId(String valor){return valor==null||valor.isBlank()?0:Integer.parseInt(valor);}
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {procesarPost(request,response);}
        catch(pe.edu.utp.clinica.dao.AgendaException e){try{response.sendError(e.status(),e.getMessage());}catch(IOException ex){pe.edu.utp.clinica.util.TransporteServlet.fallo(response);}}
        catch(IOException|ServletException e){pe.edu.utp.clinica.util.TransporteServlet.fallo(response);}
    }
    private void procesarPost(HttpServletRequest request,HttpServletResponse response) throws IOException,ServletException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login/personal");
            return;
        }

        String action = request.getParameter("action");

        Usuario actor=(Usuario)session.getAttribute("usuarioLogueado");
        if(!"ADMINISTRADOR".equals(actor.getRol())){response.sendError(403);return;}
        if(java.util.Set.of("registrar","editar","cambiarEstado","reinvitar").contains(action==null?"":action)) {
            request.getRequestDispatcher("/sysadmin/personal").forward(request,response);
            return;
        }

        switch(action==null?"":action) {
            case "guardarPriorizacion" -> guardarPriorizacion(request,response);
            case "crearSede" -> response.sendError(410,"Utiliza el catálogo de sedes actualizado.");
            case "editarSede" -> response.sendError(410,"Utiliza el catálogo de sedes actualizado.");
            case "cambiarEstadoSede" -> response.sendError(410,"Utiliza el catálogo de sedes actualizado.");
            default -> response.sendRedirect(request.getContextPath()+"/sysadmin/dashboard");
        }
}

    private void cargarSedesYEspecialidades(HttpServletRequest request) {
        List<Map<String, Object>> sedes = new ArrayList<>();
        List<Map<String, Object>> especialidades = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT id_sede, nombre,estado FROM sede ORDER BY nombre,id_sede");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", rs.getInt("id_sede"));
                    m.put("estado",rs.getString("estado"));
                    m.put(NOMBRE, rs.getString(NOMBRE));
                    sedes.add(m);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT id_especialidad, nombre FROM especialidad");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", rs.getInt("id_especialidad"));
                    m.put(NOMBRE, rs.getString(NOMBRE));
                    especialidades.add(m);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        request.setAttribute("sedes", sedes);
        request.setAttribute("especialidades", especialidades);
    }

    private void guardarPriorizacion(HttpServletRequest request,HttpServletResponse response) throws IOException,ServletException {
            request.getRequestDispatcher("/sysadmin/configuracion-ofertas").forward(request,response);

    }

    private void crearSede(HttpServletRequest request,HttpServletResponse response,HttpSession session) throws IOException {
            String codigo = request.getParameter("codigo");
            String nombre = request.getParameter(NOMBRE);
            String direccion = request.getParameter("direccion");
            String telefono = request.getParameter("telefono");
            String estado = request.getParameter(ESTADO);

            pe.edu.utp.clinica.model.Sede s = new pe.edu.utp.clinica.model.Sede(
                0,
                codigo != null ? codigo.trim() : "",
                nombre != null ? nombre.trim() : "",
                direccion != null ? direccion.trim() : "",
                telefono != null ? telefono.trim() : "",
                null,
                estado != null ? estado.trim() : "ACTIVO"
            );

            boolean ok = sedeDAO.crearSede(s);
            if (ok) {
                session.setAttribute(FLASH_MESSAGE, "¡Sede " + s.getNombre() + " (" + s.getCodigo() + ") guardada exitosamente en la Base de Datos!");
            } else {
                session.setAttribute(FLASH_ERROR, "Error al crear la sede. Verifica que el código no esté duplicado.");
            }
            response.sendRedirect(request.getContextPath() + "/sysadmin/sedes");

    }

    private void editarSede(HttpServletRequest request,HttpServletResponse response,HttpSession session) throws IOException {
            try {
                int idSede = Integer.parseInt(request.getParameter("idSede"));
                String codigo = request.getParameter("codigo");
                String nombre = request.getParameter(NOMBRE);
                String direccion = request.getParameter("direccion");
                String telefono = request.getParameter("telefono");
                String estado = request.getParameter(ESTADO);

                pe.edu.utp.clinica.model.Sede s = new pe.edu.utp.clinica.model.Sede(
                    idSede,
                    codigo != null ? codigo.trim() : "",
                    nombre != null ? nombre.trim() : "",
                    direccion != null ? direccion.trim() : "",
                    telefono != null ? telefono.trim() : "",
                    null,
                    estado != null ? estado.trim() : "ACTIVO"
                );

                boolean ok = sedeDAO.actualizarSede(s);
                if (ok) {
                    session.setAttribute(FLASH_MESSAGE, "Sede " + s.getNombre() + " actualizada correctamente.");
                } else {
                    session.setAttribute(FLASH_ERROR, "No se pudo actualizar la sede.");
                }
            } catch (Exception e) {
                session.setAttribute(FLASH_ERROR, "Datos inválidos para actualizar la sede.");
            }
            response.sendRedirect(request.getContextPath() + "/sysadmin/sedes");

    }

    private void cambiarEstadoSede(HttpServletRequest request,HttpServletResponse response,HttpSession session) throws IOException {
            try {
                int idSede = Integer.parseInt(request.getParameter("idSede"));
                String nuevoEstado = request.getParameter(ESTADO);
                boolean ok = sedeDAO.cambiarEstado(idSede, nuevoEstado);
                if (ok) {
                    session.setAttribute(FLASH_MESSAGE, "Estado de la sede #" + idSede + " cambiado a " + nuevoEstado + ".");
                } else {
                    session.setAttribute(FLASH_ERROR, "No se pudo cambiar el estado de la sede.");
                }
            } catch (Exception e) {
                session.setAttribute(FLASH_ERROR, "Error al procesar cambio de estado de sede.");
            }
            response.sendRedirect(request.getContextPath() + "/sysadmin/sedes");
            return;

    }
}
