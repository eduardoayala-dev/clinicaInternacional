package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.clinica.dao.UsuarioDAO;
import pe.edu.utp.clinica.model.Paciente;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.util.SecurityUtil;

import java.io.IOException;

@WebServlet(name = "PacientePerfilServlet", urlPatterns = {"/paciente/perfil"})
public class PacientePerfilServlet extends HttpServlet {

    private UsuarioDAO usuarioDAO;

    @Override
    public void init() throws ServletException {
        this.usuarioDAO = new UsuarioDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login/paciente");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        Paciente paciente = usuarioDAO.obtenerPacientePorUsuarioId(usuario.getIdUsuario());

        pe.edu.utp.clinica.dao.CitaDAO citaDAO = new pe.edu.utp.clinica.dao.CitaDAO();
        int numNotifSinLeer = citaDAO.contarNotificacionesSinLeer(usuario.getIdUsuario());

        request.setAttribute("usuario", usuario);
        request.setAttribute("paciente", paciente);
        request.setAttribute("nombrePaciente", usuario.getNombres() + " " + usuario.getApellidos());
        request.setAttribute("numNotifSinLeer", numNotifSinLeer);

        request.getRequestDispatcher("/WEB-INF/views/paciente/perfil.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login/paciente");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        String email = SecurityUtil.sanitizeInput(request.getParameter("email"));
        String telefono = SecurityUtil.sanitizeInput(request.getParameter("telefono"));
        String direccion = SecurityUtil.sanitizeInput(request.getParameter("direccion"));

        boolean exito = usuarioDAO.actualizarContactoPaciente(usuario.getIdUsuario(), email, telefono, direccion);
        if (exito) {
            usuario.setEmail(email);
            session.setAttribute("usuarioLogueado", usuario);
            request.setAttribute("mensajeExito", "¡Datos de contacto actualizados correctamente!");
        } else {
            request.setAttribute("mensajeError", "No se pudieron actualizar los datos de contacto.");
        }

        doGet(request, response);
    }
}
