package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.model.Usuario;
import java.io.IOException;
import java.time.Year;
import java.time.ZoneId;

@WebServlet(name = "ReservarCitaServlet", urlPatterns = {"/paciente/reservar"})
public class ReservarCitaServlet extends HttpServlet {
    private transient CitaDAO citaDAO;
    private record ReservaCreada(int idCita, String codigoCita) {}

    protected CitaDAO crearCitaDAO() { return new CitaDAO(); }
    @Override public void init() throws ServletException { citaDAO = crearCitaDAO(); }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var session = request.getSession(false);
        Usuario usuario = session == null ? null : (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || !"PACIENTE".equals(usuario.getRol())) {
            fallo(request, response, 403, "ACCESO_DENEGADO", "Acceso exclusivo para pacientes.");
            return;
        }
        try {
            int medico = Integer.parseInt(request.getParameter("idMedico"));
            int paciente = citaDAO.obtenerIdPacientePorUsuarioId(usuario.getIdUsuario());
            int id = citaDAO.reservarCitaValidada(paciente, medico, request.getParameter("fecha"),
                    request.getParameter("hora"), request.getParameter("urgencia"));
            String codigo = "CI-" + Year.now(ZoneId.of("America/Lima")) + "-" + String.format("%06d", id);
            if (json(request)) {
                ApiJson.write(response, 201, new ApiResponse<>("CITA_RESERVADA", "Cita reservada correctamente.", new ReservaCreada(id, codigo)));
            } else {
                response.sendRedirect(request.getContextPath() + "/paciente/historial?reserva=ok");
            }
        } catch (NumberFormatException e) {
            fallo(request, response, 400, "DATOS_INVALIDOS", "El médico no es válido.");
        } catch (AgendaException e) {
            fallo(request, response, e.status(), e.code(), e.getMessage());
        } catch (RuntimeException e) {
            fallo(request, response, 500, "ERROR_INTERNO", "No se pudo completar la reserva.");
        }
    }

    private static boolean json(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
                || (accept != null && accept.contains("application/json"))
                || "true".equals(request.getParameter("ajax"));
    }

    private static void fallo(HttpServletRequest request, HttpServletResponse response, int status, String code, String message) throws IOException {
        if (json(request) || status == 403) {
            ApiJson.write(response, status, new ApiResponse<>(code, message, null));
        } else {
            request.getSession().setAttribute("agendaError", message);
            response.sendRedirect(request.getContextPath() + "/paciente/citas?error=" + code);
        }
    }
}
