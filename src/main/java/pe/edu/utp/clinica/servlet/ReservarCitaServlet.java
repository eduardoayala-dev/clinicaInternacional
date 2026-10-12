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

@WebServlet(name = "ReservarCitaServlet", urlPatterns = {"/paciente/reservar"})
public class ReservarCitaServlet extends HttpServlet {

    private CitaDAO citaDAO;

    @Override
    public void init() throws ServletException {
        this.citaDAO = new CitaDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Usuario usuario = (session != null) ? (Usuario) session.getAttribute("usuarioLogueado") : null;
        if (usuario == null || !"PACIENTE".equals(usuario.getRol())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso denegado");
            return;
        }

        try {
            int idMedico = Integer.parseInt(request.getParameter("idMedico"));
            String fecha = request.getParameter("fecha");
            String hora = request.getParameter("hora");
            String nivelUrgencia = request.getParameter("urgencia"); // ALTA, MEDIA, BAJA

            // Obtener el id_paciente correspondiente al id_usuario de la sesión
            int idPaciente = citaDAO.obtenerIdPacientePorUsuarioId(usuario.getIdUsuario());

            boolean exito = citaDAO.reservarCita(idPaciente, idMedico, fecha, hora, nivelUrgencia);

            if (exito) {
                // Redirigir al dashboard con alerta de éxito
                response.sendRedirect(request.getContextPath() + "/paciente/dashboard?reserva=ok");
            } else {
                response.sendRedirect(request.getContextPath() + "/paciente/reservar_cita?error=true");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/paciente/reservar_cita?error=true");
        }
    }
}
