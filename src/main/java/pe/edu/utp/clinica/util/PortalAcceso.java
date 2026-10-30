package pe.edu.utp.clinica.util;

import jakarta.servlet.http.HttpServletRequest;
import pe.edu.utp.clinica.auth.GestorAutenticacion;
import pe.edu.utp.clinica.auth.SesionAutenticada;
import pe.edu.utp.clinica.model.Usuario;

public final class PortalAcceso {
    private PortalAcceso() {}

    public static String login(HttpServletRequest request) {
        String rol = rol(request);
        if ("PACIENTE".equals(rol)) return "/login/paciente";
        if ("MEDICO".equals(rol) || "PERSONAL_ADMINISTRATIVO".equals(rol) || "ADMINISTRADOR".equals(rol)) return "/login/personal";
        return "personal".equals(request.getParameter("portal")) ? "/login/personal" : "/login";
    }

    private static String rol(HttpServletRequest request) {
        if (request.getAttribute(GestorAutenticacion.SESION) instanceof SesionAutenticada actual) return actual.usuario().getRol();
        var session = request.getSession(false);
        if (session != null && session.getAttribute("usuarioLogueado") instanceof Usuario usuario) return usuario.getRol();
        return null;
    }
}
