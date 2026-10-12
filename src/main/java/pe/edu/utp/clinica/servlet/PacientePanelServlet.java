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

@WebServlet(name = "PacientePanelServlet", urlPatterns = {"/paciente/panel"})
public class PacientePanelServlet extends HttpServlet {

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
        List<Map<String, Object>> listaActividades = idPaciente > 0 ? citaDAO.obtenerProximasCitasPaciente(idPaciente) : new ArrayList<>();

        request.setAttribute("nombrePaciente", usuario != null ? usuario.getNombres() + " " + usuario.getApellidos() : "Paciente");
        request.setAttribute("listaActividades", listaActividades);

        request.getRequestDispatcher("/WEB-INF/views/paciente/panel.jsp").forward(request, response);
    }
}
