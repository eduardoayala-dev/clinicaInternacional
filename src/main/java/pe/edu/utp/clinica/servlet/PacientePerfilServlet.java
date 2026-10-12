package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.dao.UsuarioDAO;
import pe.edu.utp.clinica.model.Paciente;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.util.SecurityUtil;

import java.io.IOException;

@WebServlet(name = "PacientePerfilServlet", urlPatterns = {"/paciente/perfil"})
public class PacientePerfilServlet extends HttpServlet {

    private UsuarioDAO usuarioDAO;
    private CitaDAO citaDAO;

    @Override
    public void init() throws ServletException {
        this.usuarioDAO = new UsuarioDAO();
        this.citaDAO = new CitaDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login/paciente");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario.getIdUsuario() <= 0 && usuario.getEmail() != null) {
            Usuario usuarioRecuperado = usuarioDAO.buscarPorEmailODni(usuario.getEmail());
            if (usuarioRecuperado != null) {
                usuario = usuarioRecuperado;
                session.setAttribute("usuarioLogueado", usuario);
            }
        }

        Paciente paciente = usuarioDAO.obtenerPacientePorUsuarioId(usuario.getIdUsuario());
        if (paciente == null) {
            paciente = new Paciente();
            paciente.setUsuarioId(usuario.getIdUsuario());
            paciente.setDni("");
            paciente.setTelefono("");
            paciente.setDireccion("");
            paciente.setCanalAviso("WhatsApp");
            paciente.setSedePreferida("Sede San Borja");
            paciente.setAlergias("");
            paciente.setTipoCobertura("EPS");
        }

        int numNotifSinLeer = citaDAO.contarNotificacionesSinLeer(usuario.getIdUsuario());

        String mensajeExito = (String) session.getAttribute("mensajeExito");
        if (mensajeExito != null) {
            request.setAttribute("mensajeExito", mensajeExito);
            session.removeAttribute("mensajeExito");
        }

        String mensajeError = (String) session.getAttribute("mensajeError");
        if (mensajeError != null) {
            request.setAttribute("mensajeError", mensajeError);
            session.removeAttribute("mensajeError");
        }

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
        if (usuario.getIdUsuario() <= 0 && usuario.getEmail() != null) {
            Usuario usuarioRecuperado = usuarioDAO.buscarPorEmailODni(usuario.getEmail());
            if (usuarioRecuperado != null) {
                usuario = usuarioRecuperado;
                session.setAttribute("usuarioLogueado", usuario);
            }
        }

        String nombres = SecurityUtil.sanitizeInput(request.getParameter("nombres"));
        String apellidos = SecurityUtil.sanitizeInput(request.getParameter("apellidos"));
        String email = SecurityUtil.sanitizeInput(request.getParameter("email"));
        String fechaNacimiento = SecurityUtil.sanitizeInput(request.getParameter("fechaNacimiento"));
        String telefono = SecurityUtil.sanitizeInput(request.getParameter("telefono"));
        String direccion = SecurityUtil.sanitizeInput(request.getParameter("direccion"));
        String canalAviso = SecurityUtil.sanitizeInput(request.getParameter("canalAviso"));
        String sedePreferida = SecurityUtil.sanitizeInput(request.getParameter("sedePreferida"));
        String alergias = SecurityUtil.sanitizeInput(request.getParameter("alergias"));
        String tipoCobertura = SecurityUtil.sanitizeInput(request.getParameter("tipoCobertura"));

        if (nombres == null || nombres.trim().isEmpty()) nombres = usuario.getNombres();
        if (apellidos == null || apellidos.trim().isEmpty()) apellidos = usuario.getApellidos();
        if (email == null || email.trim().isEmpty()) email = usuario.getEmail();

        boolean exito = usuarioDAO.actualizarPerfilCompletoPaciente(
            usuario.getIdUsuario(),
            nombres,
            apellidos,
            email,
            fechaNacimiento,
            telefono,
            direccion,
            canalAviso,
            sedePreferida,
            alergias,
            tipoCobertura
        );

        if (exito) {
            usuario.setNombres(nombres);
            usuario.setApellidos(apellidos);
            usuario.setEmail(email);
            session.setAttribute("usuarioLogueado", usuario);
            session.setAttribute("mensajeExito", "Los cambios en tu perfil fueron guardados correctamente.");
        } else {
            session.setAttribute("mensajeError", "No se pudieron actualizar los datos del perfil.");
        }

        response.sendRedirect(request.getContextPath() + "/paciente/perfil");
    }
}

