package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.clinica.model.Usuario;

import java.io.IOException;

@WebServlet(name = "MedicoPerfilServlet", urlPatterns = {"/medico/perfil"})
public class MedicoPerfilServlet extends HttpServlet {

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

        pe.edu.utp.clinica.dao.CitaDAO citaDAO = new pe.edu.utp.clinica.dao.CitaDAO();
        int idMedico = citaDAO.obtenerIdMedicoPorUsuarioId(usuario.getIdUsuario());
        java.util.Map<String, Object> detalles = citaDAO.obtenerDetallesMedico(idMedico);

        request.setAttribute("usuario", usuario);
        request.setAttribute("nombreMedico", detalles.get("nombreMedico") != null ? detalles.get("nombreMedico") : ("Dr. " + usuario.getNombres() + " " + usuario.getApellidos()));
        request.setAttribute("detallesMedico", detalles);

        request.getRequestDispatcher("/WEB-INF/views/medico/perfil.jsp").forward(request, response);
    }
}
