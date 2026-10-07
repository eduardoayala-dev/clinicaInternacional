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

    private UsuarioDAO usuarioDAO;

    @Override
    public void init() throws ServletException {
        this.usuarioDAO = new UsuarioDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
                List<Usuario> listaUsuarios = usuarioDAO.obtenerTodos();
                request.setAttribute("listaUsuarios", listaUsuarios);
                cargarSedesYEspecialidades(request);
                request.getRequestDispatcher("/WEB-INF/views/sysadmin/usuarios.jsp").forward(request, response);
                break;

            case "/sysadmin/sedes":
                request.getRequestDispatcher("/WEB-INF/views/sysadmin/sedes.jsp").forward(request, response);
                break;

            case "/sysadmin/priorizacion":
                request.getRequestDispatcher("/WEB-INF/views/sysadmin/priorizacion.jsp").forward(request, response);
                break;

            case "/sysadmin/analitica":
                request.getRequestDispatcher("/WEB-INF/views/sysadmin/analitica.jsp").forward(request, response);
                break;

            case "/sysadmin/dashboard":
            case "/sysadmin/inicio":
            default:
                request.getRequestDispatcher("/WEB-INF/views/sysadmin/dashboard.jsp").forward(request, response);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login/personal");
            return;
        }

        String action = request.getParameter("action");
        if ("guardarPriorizacion".equals(action)) {
            session.setAttribute("flashMessage", "Parámetros de priorización y tiempo límite de respuesta actualizados correctamente.");
            response.sendRedirect(request.getContextPath() + "/sysadmin/priorizacion");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/sysadmin/dashboard");
    }

    private void cargarSedesYEspecialidades(HttpServletRequest request) {
        List<Map<String, Object>> sedes = new ArrayList<>();
        List<Map<String, Object>> especialidades = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT id_sede, nombre FROM sede WHERE estado='ACTIVO'");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", rs.getInt("id_sede"));
                    m.put("nombre", rs.getString("nombre"));
                    sedes.add(m);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT id_especialidad, nombre FROM especialidad");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", rs.getInt("id_especialidad"));
                    m.put("nombre", rs.getString("nombre"));
                    especialidades.add(m);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        request.setAttribute("sedes", sedes);
        request.setAttribute("especialidades", especialidades);
    }
}
