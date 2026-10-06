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

import java.io.IOException;

@WebFilter(filterName = "AuthFilter", urlPatterns = {"/paciente/*", "/medico/*", "/admin/*"})
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

        String uri = req.getRequestURI();
        Usuario usuario = (session != null) ? (Usuario) session.getAttribute("usuarioLogueado") : null;

        if (usuario == null) {
            res.sendRedirect(req.getContextPath() + "/login?error=sesion_requerida");
            return;
        }

        String rol = usuario.getRol();

        if (uri.contains("/paciente/") && !"PACIENTE".equals(rol)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso no autorizado al módulo de Pacientes.");
            return;
        }

        if (uri.contains("/medico/") && !"MEDICO".equals(rol)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso no autorizado al módulo de Médicos.");
            return;
        }

        if (uri.contains("/admin/") && !"ADMINISTRADOR".equals(rol) && !"PERSONAL_ADMINISTRATIVO".equals(rol)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso no autorizado al módulo Administrativo.");
            return;
        }

        res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        res.setHeader("Pragma", "no-cache");
        res.setHeader("Expires", "0");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("X-Content-Type-Options", "nosniff");

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
