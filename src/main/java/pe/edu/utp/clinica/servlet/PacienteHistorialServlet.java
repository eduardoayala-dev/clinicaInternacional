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
            response.sendRedirect(request.getContextPath() + "/login/paciente");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        int usuarioId = usuario != null ? usuario.getIdUsuario() : -1;

        CitaDAO citaDAO = new CitaDAO();
        int idPaciente = citaDAO.obtenerIdPacientePorUsuarioId(usuarioId);
        List<Map<String, Object>> historialCitas = idPaciente > 0 ? citaDAO.obtenerHistorialPaciente(idPaciente) : new ArrayList<>();

        request.setAttribute("nombrePaciente", usuario != null ? usuario.getNombres() + " " + usuario.getApellidos() : "Paciente");
        request.setAttribute("historialCitas", historialCitas);

        request.getRequestDispatcher("/WEB-INF/views/paciente/historial.jsp").forward(request, response);
    }
}
