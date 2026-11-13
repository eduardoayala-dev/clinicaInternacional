package pe.edu.utp.clinica.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;

import java.io.IOException;

@WebFilter(asyncSupported = true, filterName = "AuthFilter", urlPatterns = {"/paciente/*", "/medico/*", "/admin/*", "/sysadmin/*", "/cuenta/*"})
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);

        String uri = req.getServletPath();
        Usuario usuario = (session != null) ? (Usuario) session.getAttribute("usuarioLogueado") : null;

        if (usuario == null) {
            if (json(req)) ApiJson.write(res,403,new ApiResponse<>("SESION_REQUERIDA","Inicia sesión para continuar.",null));
            else res.sendRedirect(req.getContextPath() + "/login?error=sesion_requerida");
            return;
        }

        String rol = usuario.getRol();

        if (uri.startsWith("/paciente/") && !"PACIENTE".equals(rol)) {
            redirigirSegunRol(res, req, rol);
            return;
        }

        if (uri.startsWith("/medico/") && !"MEDICO".equals(rol)) {
            redirigirSegunRol(res, req, rol);
            return;
        }

        if (uri.startsWith("/sysadmin/") && !"ADMINISTRADOR".equals(rol)) {
            redirigirSegunRol(res, req, rol);
            return;
        }

        if (uri.startsWith("/admin/") && !"PERSONAL_ADMINISTRATIVO".equals(rol)) {
            redirigirSegunRol(res, req, rol);
            return;
        }

        res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        res.setHeader("Pragma", "no-cache");
        res.setHeader("Expires", "0");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("X-Content-Type-Options", "nosniff");

        chain.doFilter(request, response);
    }

    private static boolean json(HttpServletRequest request) {
        String accept=request.getHeader("Accept");
        return accept!=null && accept.contains("application/json");
    }

    private void redirigirSegunRol(HttpServletResponse res, HttpServletRequest request, String rol) throws IOException {
        if(json(request)) {
            ApiJson.write(res,403,new ApiResponse<>("ACCESO_DENEGADO","Tu perfil no tiene acceso a este recurso.",null));
            return;
        }
        String contextPath=request.getContextPath();
        if ("PACIENTE".equals(rol)) {
            res.sendRedirect(contextPath + "/paciente/dashboard");
        } else if ("MEDICO".equals(rol)) {
            res.sendRedirect(contextPath + "/medico/agenda");
        } else if ("PERSONAL_ADMINISTRATIVO".equals(rol)) {
            res.sendRedirect(contextPath + "/admin/dashboard");
        } else if ("ADMINISTRADOR".equals(rol)) {
            res.sendRedirect(contextPath + "/sysadmin/dashboard");
        } else {
            res.sendRedirect(contextPath + "/login?error=rol_invalido");
        }
    }

    @Override
    public void destroy() {
    }
}
