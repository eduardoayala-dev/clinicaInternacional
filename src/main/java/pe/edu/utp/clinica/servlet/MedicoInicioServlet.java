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

@WebServlet(name = "MedicoInicioServlet", urlPatterns = {"/medico/inicio", "/medico/dashboard"})
public class MedicoInicioServlet extends HttpServlet {

    private CitaDAO citaDAO;

    @Override
    public void init() throws ServletException {
        this.citaDAO = new CitaDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login/personal");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (!"MEDICO".equals(usuario.getRol())) {
            response.sendRedirect(request.getContextPath() + "/login/personal?error=rol_invalido");
            return;
        }

        int idMedico = citaDAO.obtenerIdMedicoPorUsuarioId(usuario.getIdUsuario());
        List<Map<String, Object>> agenda = citaDAO.obtenerAgendaMedico(idMedico);

        request.setAttribute("usuario", usuario);
        request.setAttribute("nombreMedico", usuario.getNombres() + " " + usuario.getApellidos());
        request.setAttribute("agenda", agenda);

        request.getRequestDispatcher("/WEB-INF/views/medico/inicio.jsp").forward(request, response);
    }
}
