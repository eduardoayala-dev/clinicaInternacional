package pe.edu.utp.clinica.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.util.CsrfToken;
import java.io.IOException;
import java.util.Set;

/** Validación antes de ejecutar cualquier operación que pueda modificar datos. */
public class CsrfFilter implements Filter {
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        req.setCharacterEncoding("UTF-8");
        if (!SAFE_METHODS.contains(req.getMethod())) {
            // No crear una sesión/token para legitimar un POST que llegó sin sesión.
            if (!CsrfToken.isValid(req)) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.setContentType("application/json;charset=UTF-8");
                res.setHeader("Cache-Control", "no-store");
                res.getWriter().write("{\"code\":\"CSRF_INVALIDO\",\"message\":\"La sesión o el token de seguridad no es válido. Recarga la página.\"}");
                return;
            }
        } else if (!req.getServletPath().startsWith("/assets/")) {
            CsrfToken.getOrCreate(req.getSession(true));
            res.setHeader("Cache-Control", "no-store");
        }
        chain.doFilter(request, response);
    }
}
