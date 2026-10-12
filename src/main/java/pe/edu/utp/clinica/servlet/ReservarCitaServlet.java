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

            int idCita = citaDAO.reservarCitaConId(idPaciente, idMedico, fecha, hora, nivelUrgencia);
            boolean exito = idCita > 0;

            String accept = request.getHeader("Accept");
            String requestedWith = request.getHeader("X-Requested-With");
            boolean esAjax = "XMLHttpRequest".equals(requestedWith) 
                          || (accept != null && accept.contains("application/json"))
                          || "true".equals(request.getParameter("ajax"));

            if (esAjax) {
                response.setContentType("application/json;charset=UTF-8");
                if (exito) {
                    String anio = java.time.Year.now().toString();
                    String codigoCita = "CI-" + anio + "-" + String.format("%06d", idCita);
                    response.getWriter().write("{\"success\":true,\"idCita\":" + idCita + ",\"codigoCita\":\"" + codigoCita + "\"}");
                } else {
                    response.getWriter().write("{\"success\":false,\"error\":\"No se pudo completar la reserva.\"}");
                }
                return;
            }

            if (exito) {
                String anio = java.time.Year.now().toString();
                String codigoCita = "CI-" + anio + "-" + String.format("%06d", idCita);
                response.sendRedirect(request.getContextPath() + "/paciente/citas?reserva=ok&codigo=" + codigoCita);
            } else {
                response.sendRedirect(request.getContextPath() + "/paciente/citas?error=true");
            }

        } catch (Exception e) {
            e.printStackTrace();
            String accept = request.getHeader("Accept");
            String requestedWith = request.getHeader("X-Requested-With");
            if ("XMLHttpRequest".equals(requestedWith) || (accept != null && accept.contains("application/json"))) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"error\":\"Error interno al procesar reserva.\"}");
            } else {
                response.sendRedirect(request.getContextPath() + "/paciente/citas?error=true");
            }
        }
    }
}
