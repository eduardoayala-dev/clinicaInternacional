package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.clinica.dao.UsuarioDAO;
import pe.edu.utp.clinica.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pe.edu.utp.clinica.config.DatabaseConfig;

@WebServlet(name = "UsuariosServlet", urlPatterns = {"/admin/usuarios"})
public class UsuariosServlet extends HttpServlet {

    private UsuarioDAO usuarioDAO;

    @Override
    public void init() throws ServletException {
        this.usuarioDAO = new UsuarioDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String rol = (String) session.getAttribute("rol");
        if (!"ADMINISTRADOR".equals(rol)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "No tienes permisos para acceder a esta vista.");
            return;
        }

        List<Usuario> listaUsuarios = usuarioDAO.obtenerTodos();
        request.setAttribute("listaUsuarios", listaUsuarios);

        // Fetch Sedes and Especialidades for the Modal
        List<Map<String, Object>> sedes = new ArrayList<>();
        List<Map<String, Object>> especialidades = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection()) {
            try(PreparedStatement ps = conn.prepareStatement("SELECT id_sede, nombre FROM sede WHERE estado='ACTIVO'"); ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", rs.getInt("id_sede"));
                    m.put("nombre", rs.getString("nombre"));
                    sedes.add(m);
                }
            }
            try(PreparedStatement ps = conn.prepareStatement("SELECT id_especialidad, nombre FROM especialidad WHERE estado='ACTIVO'"); ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", rs.getInt("id_especialidad"));
                    m.put("nombre", rs.getString("nombre"));
                    especialidades.add(m);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        request.setAttribute("listaSedes", sedes);
        request.setAttribute("listaEspecialidades", especialidades);

        // Calcular Métricas
        int countMedico = 0, countPersonal = 0, countPaciente = 0, countAdmin = 0;
        for (Usuario u : listaUsuarios) {
            if ("MEDICO".equals(u.getRol())) countMedico++;
            else if ("PERSONAL_ADMINISTRATIVO".equals(u.getRol())) countPersonal++;
            else if ("PACIENTE".equals(u.getRol())) countPaciente++;
            else if ("ADMINISTRADOR".equals(u.getRol())) countAdmin++;
        }
        request.setAttribute("countMedico", countMedico);
        request.setAttribute("countPersonal", countPersonal);
        request.setAttribute("countPaciente", countPaciente);
        request.setAttribute("countAdmin", countAdmin);
        request.setAttribute("countTotal", listaUsuarios.size());

        request.getRequestDispatcher("/WEB-INF/views/admin/usuarios.jsp").forward(request, response);
    }
}
