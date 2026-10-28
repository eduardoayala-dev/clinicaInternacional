package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.CatalogoDatosException;
import pe.edu.utp.clinica.model.Usuario;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class PacientePortalServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(PacientePortalServlet.class.getName());
    protected abstract String ruta();
    protected abstract void mostrar(HttpServletRequest request, HttpServletResponse response, Usuario usuario) throws IOException, ServletException;
    protected abstract void ejecutar(HttpServletRequest request, HttpServletResponse response, Usuario usuario) throws IOException;

    @Override protected final void doGet(HttpServletRequest request, HttpServletResponse response) {
        atender(request, response, false);
    }
    @Override protected final void doPost(HttpServletRequest request, HttpServletResponse response) {
        atender(request, response, true);
    }

    private void atender(HttpServletRequest request, HttpServletResponse response, boolean escritura) {
        try {
            Usuario usuario = paciente(request);
            if (escritura) ejecutar(request, response, usuario); else mostrar(request, response, usuario);
        } catch (AgendaException e) {
            error(request, response, e.status(), e.code(), e.getMessage());
        } catch (CatalogoDatosException e) {
            error(request, response, 503, "DATOS_NO_DISPONIBLES", "No se pudieron consultar los datos. Inténtalo más tarde.");
        } catch (IOException e) {
            transporte(response, e);
        } catch (ServletException | RuntimeException e) {
            LOGGER.warning(() -> "Error al atender el portal: " + e.getClass().getSimpleName());
            error(request, response, 500, "ERROR_INTERNO", "No se pudo completar la operación.");
        }
    }

    private void error(HttpServletRequest request, HttpServletResponse response, int status, String code, String mensaje) {
        if (response.isCommitted()) return;
        try {
            if (status == 405) response.setHeader("Allow", "POST");
            if (json(request) || status == 403 || status == 405) {
                ApiJson.write(response, status, new ApiResponse<>(code, mensaje, null));
            } else if (!"POST".equals(request.getMethod())) {
                response.setStatus(status);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write(mensaje);
            } else {
                request.getSession().setAttribute("toastMsg", mensaje);
                request.getSession().setAttribute("toastTipo", "error");
                response.sendRedirect(request.getContextPath() + ruta());
            }
        } catch (IOException e) { transporte(response, e); }
    }

    private static Usuario paciente(HttpServletRequest request) {
        var session = request.getSession(false);
        Usuario usuario = session == null ? null : (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || !"PACIENTE".equals(usuario.getRol())) throw new AgendaException(403, "ACCESO_DENEGADO", "Acceso exclusivo para pacientes.");
        return usuario;
    }

    protected static boolean json(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    protected static int positivo(String value) {
        try {
            if (value == null || !value.trim().matches("\\d+")) throw new NumberFormatException();
            int result = Integer.parseInt(value.trim());
            if (result <= 0) throw new NumberFormatException();
            return result;
        } catch (NumberFormatException e) { throw new AgendaException(400, "DATOS_INVALIDOS", "El identificador debe ser un entero positivo."); }
    }

    protected static void resultado(HttpServletRequest request, HttpServletResponse response, int status, ApiResponse<?> data, String destino) throws IOException {
        if (json(request)) ApiJson.write(response, status, data);
        else {
            request.getSession().setAttribute("toastMsg", data.message());
            request.getSession().setAttribute("toastTipo", "success");
            response.sendRedirect(request.getContextPath() + destino);
        }
    }

    protected static void flash(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null) return;
        request.setAttribute("toastMsg", session.getAttribute("toastMsg"));
        request.setAttribute("toastTipo", session.getAttribute("toastTipo"));
        session.removeAttribute("toastMsg");
        session.removeAttribute("toastTipo");
    }

    private static void transporte(HttpServletResponse response, IOException e) {
        LOGGER.log(Level.WARNING, "No se pudo entregar la respuesta del portal.", e);
        if (!response.isCommitted()) response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
