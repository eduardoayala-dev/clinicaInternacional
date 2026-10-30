package pe.edu.utp.clinica.util;

import jakarta.servlet.http.HttpServletRequest;
import pe.edu.utp.clinica.auth.GestorAutenticacion;
import pe.edu.utp.clinica.auth.SesionAutenticada;
import pe.edu.utp.clinica.model.Usuario;

public final class PortalAcceso {
    private static final java.util.Set<String> PERSONAL=java.util.Set.of("MEDICO","PERSONAL_ADMINISTRATIVO","ADMINISTRADOR");
    private PortalAcceso() {}

    public static String login(HttpServletRequest request) {
        String rol = rol(request);
        if ("PACIENTE".equals(rol)) return "/login/paciente";
        if (esPersonal(rol)) return "/login/personal";
        return "personal".equals(request.getParameter("portal")) || origenPersonal(request) ? "/login/personal" : "/login";
    }

    public static boolean esPersonal(String rol) { return rol!=null && PERSONAL.contains(rol); }

    private static boolean origenPersonal(HttpServletRequest request) {
        String origen=request.getHeader("Referer");
        if(origen==null)return false;
        try {
            String path=java.net.URI.create(origen).getPath();
            String contexto=request.getContextPath();
            return path!=null && (path.startsWith(contexto+"/medico/") || path.startsWith(contexto+"/admin/") || path.startsWith(contexto+"/sysadmin/"));
        } catch (IllegalArgumentException e) { return false; }
    }

    private static String rol(HttpServletRequest request) {
        if (request.getAttribute(GestorAutenticacion.SESION) instanceof SesionAutenticada actual) return actual.usuario().getRol();
        var session = request.getSession(false);
        if (session != null && session.getAttribute("usuarioLogueado") instanceof Usuario usuario) return usuario.getRol();
        return null;
    }
}
