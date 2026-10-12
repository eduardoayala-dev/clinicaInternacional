package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.model.Usuario;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(name = "PacienteNotificacionesServlet", urlPatterns = {"/paciente/notificaciones"})
public class PacienteNotificacionesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login/paciente");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        int usuarioId = usuario != null ? usuario.getIdUsuario() : -1;

        CitaDAO citaDAO = new CitaDAO();

        String action = request.getParameter("action");
        if ("marcarLeidas".equals(action)) {
            citaDAO.marcarNotificacionesComoLeidas(usuarioId);
            response.sendRedirect(request.getContextPath() + "/paciente/notificaciones");
            return;
        }
        
        List<Map<String, Object>> listaNotificaciones = citaDAO.obtenerNotificacionesUsuario(usuarioId);
        int unreadCount = citaDAO.contarNotificacionesSinLeer(usuarioId);

        request.setAttribute("nombrePaciente", usuario != null ? usuario.getNombres() + " " + usuario.getApellidos() : "Paciente");
        request.setAttribute("listaNotificaciones", listaNotificaciones);
        request.setAttribute("unreadCount", unreadCount);
        request.setAttribute("tieneNotificacionesSinLeer", unreadCount > 0);

        request.getRequestDispatcher("/WEB-INF/views/paciente/notificaciones.jsp").forward(request, response);
    }
}
