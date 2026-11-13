package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.PropuestaHorarioDAO;
import pe.edu.utp.clinica.model.Usuario;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

@WebServlet(name="AdmisionHorariosServlet", urlPatterns="/admin/horarios")
public final class AdmisionHorariosServlet extends HttpServlet {
    private static final String APROBAR = "aprobar";
    private transient PropuestaHorarioDAO propuestas;
    @Override public void init() { propuestas = new PropuestaHorarioDAO(); }
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) { responder(request, response, false); }
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) { responder(request, response, true); }

    private void responder(HttpServletRequest request, HttpServletResponse response, boolean escritura) {
        try {
            atender(request, response, escritura);
        } catch (IOException | ServletException e) {
            java.util.logging.Logger.getLogger(getClass().getName()).warning("No se pudo entregar la respuesta de horarios de admisión.");
            if (!response.isCommitted()) response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void atender(HttpServletRequest request, HttpServletResponse response, boolean escritura) throws IOException, ServletException {
        try {
            var session = request.getSession(false);
            Usuario usuario = session == null ? null : (Usuario) session.getAttribute("usuarioLogueado");
            if (usuario == null || !"PERSONAL_ADMINISTRATIVO".equals(usuario.getRol())) throw new AgendaException(403, "ACCESO_DENEGADO", "Acceso exclusivo para admisión.");
            if (escritura) revisar(request, response, usuario);
            else {
                var lista = HorariosVista.paginar(request, propuestas.paraAdmision(usuario.getIdUsuario()));
                if (MedicoPortalServlet.json(request)) ApiJson.write(response, 200, new ApiResponse<>("PROPUESTAS_OBTENIDAS", "Consulta completada.", lista));
                else {
                    request.setAttribute("hoy", LocalDate.now(ZoneId.of("America/Lima")));
                    request.getRequestDispatcher("/WEB-INF/views/admin/horarios.jsp").forward(request, response);
                }
            }
        } catch (AgendaException e) {
            if (MedicoPortalServlet.json(request)) ApiJson.write(response, e.status(), new ApiResponse<>(e.code(), e.getMessage(), null));
            else if (escritura && e.status() != 403) {
                request.getSession().setAttribute("horariosError", e.getMessage());
                response.sendRedirect(request.getContextPath()+"/admin/horarios");
            } else response.sendError(e.status(), e.getMessage());
        }
    }

    private void revisar(HttpServletRequest request, HttpServletResponse response, Usuario usuario) throws IOException {
        String accion = request.getParameter("accion");
        if (!APROBAR.equals(accion) && !"rechazar".equals(accion)) throw new AgendaException(400, "ACCION_INVALIDA", "Selecciona aprobar o rechazar.");
        int id = MedicoPortalServlet.positivo(request.getParameter("idPropuesta"));
        int bloques = propuestas.revisar(usuario.getIdUsuario(), id, APROBAR.equals(accion), request.getParameter("motivo"));
        String mensaje = APROBAR.equals(accion) ? "Propuesta aprobada: "+bloques+" turnos publicados." : "Propuesta rechazada. El médico podrá consultar el motivo.";
        if (MedicoPortalServlet.json(request)) ApiJson.write(response, 200, new ApiResponse<>("PROPUESTA_REVISADA", mensaje, Map.of("idPropuesta", id, "turnosPublicados", bloques)));
        else {
            request.getSession().setAttribute("horariosMensaje", mensaje);
            response.sendRedirect(request.getContextPath()+"/admin/horarios");
        }
    }
}
