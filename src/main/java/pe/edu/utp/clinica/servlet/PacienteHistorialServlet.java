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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet(name = "PacienteHistorialServlet", urlPatterns = {"/paciente/historial"})
public class PacienteHistorialServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login?error=sesion_requerida");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        int usuarioId = usuario != null ? usuario.getIdUsuario() : -1;

        CitaDAO citaDAO = new CitaDAO();
        int idPaciente = citaDAO.obtenerIdPacientePorUsuarioId(usuarioId);

        List<Map<String, Object>> citasActivas = idPaciente > 0 ? citaDAO.obtenerProximasCitasPaciente(idPaciente) : new ArrayList<>();
        List<Map<String, Object>> historialPasadas = idPaciente > 0 ? citaDAO.obtenerHistorialPaciente(idPaciente) : new ArrayList<>();
        List<Map<String, Object>> citasCanceladas = idPaciente > 0 ? citaDAO.obtenerCitasCanceladasPaciente(idPaciente) : new ArrayList<>();
        int numNotifSinLeer = usuario != null ? citaDAO.contarNotificacionesSinLeer(usuario.getIdUsuario()) : 0;

        String nombrePaciente = usuario != null ? usuario.getNombres() + " " + usuario.getApellidos() : "Paciente";

        request.setAttribute("nombrePaciente", nombrePaciente);
        request.setAttribute("citasActivas", citasActivas);
        request.setAttribute("historialPasadas", historialPasadas);
        request.setAttribute("citasCanceladas", citasCanceladas);
        request.setAttribute("numNotifSinLeer", numNotifSinLeer);

        request.getRequestDispatcher("/WEB-INF/views/paciente/historial.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login?error=sesion_requerida");
            return;
        }

        String action = request.getParameter("action");
        String idCitaStr = request.getParameter("idCita");
        if (idCitaStr != null && !idCitaStr.isEmpty()) {
            try {
                int idCita = Integer.parseInt(idCitaStr);
                CitaDAO citaDAO = new CitaDAO();
                if ("confirmar".equals(action)) {
                    citaDAO.actualizarEstadoCita(idCita, "CONFIRMADA");
                } else if ("cancelar".equals(action)) {
                    citaDAO.actualizarEstadoCita(idCita, "CANCELADA");
                }
            } catch (NumberFormatException e) {
                // Ignore parse error
            }
        }
        response.sendRedirect(request.getContextPath() + "/paciente/historial");
    }
}
