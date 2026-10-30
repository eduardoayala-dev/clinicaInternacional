package pe.edu.utp.clinica.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.util.CsrfToken;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import java.io.IOException;
import java.util.Set;

/** Validación antes de ejecutar cualquier operación que pueda modificar datos. */
public class CsrfFilter implements Filter {
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final String CACHE_CONTROL = "Cache-Control";
    private static final String NO_STORE = "no-store";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        req.setCharacterEncoding("UTF-8");
        if (!SAFE_METHODS.contains(req.getMethod())) {
            // No crear una sesión/token para legitimar un POST que llegó sin sesión.
            if (!CsrfToken.isValid(req)) {
                rechazar(req,res);
                return;
            }
        } else if (!req.getServletPath().startsWith("/assets/")
                && !"/health/ready".equals(req.getServletPath())) {
            CsrfToken.getOrCreate(req.getSession(true));
            res.setHeader(CACHE_CONTROL, NO_STORE);
        }
        chain.doFilter(request, response);
    }

    private static void rechazar(HttpServletRequest req,HttpServletResponse res) throws IOException,ServletException {
        if ("POST".equals(req.getMethod()) && "/logout".equals(req.getServletPath()) && htmlRequest(req)) {
            var session = req.getSession(false);
            boolean autenticado = session != null && session.getAttribute("usuarioLogueado") != null;
            res.setStatus(HttpServletResponse.SC_SEE_OTHER);
            res.setHeader(CACHE_CONTROL, NO_STORE);
            res.setHeader("Location", req.getContextPath() + (autenticado
                    ? "/login?error=sesion_formulario" : "/login?msg=sesion_cerrada"));
            return;
        }
        if (loginHtml(req)) {
            res.setHeader(CACHE_CONTROL, NO_STORE);
            res.setStatus(HttpServletResponse.SC_SEE_OTHER);
            res.setHeader("Location", req.getContextPath() + req.getServletPath() + "?error=sesion_formulario");
            return;
        }
        if (htmlRequest(req)) {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            res.setHeader(CACHE_CONTROL, NO_STORE);
            res.setHeader("X-Content-Type-Options", "nosniff");
            req.setAttribute("jakarta.servlet.error.status_code", HttpServletResponse.SC_FORBIDDEN);
            req.setAttribute("csrfRejected", true);
            req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, res);
            return;
        }
        ApiJson.write(res, HttpServletResponse.SC_FORBIDDEN, new ApiResponse<>("CSRF_INVALIDO",
                "La sesión o el token de seguridad no es válido. Recarga la página.", null));
    }

    private static boolean loginHtml(HttpServletRequest request) {
        String path = request.getServletPath();
        return "POST".equals(request.getMethod())
                && Set.of("/login", "/login/paciente", "/login/personal").contains(path)
                && htmlRequest(request);
    }

    private static boolean htmlRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return !request.getServletPath().startsWith("/api/") && accept != null
                && accept.contains("text/html") && !accept.contains("application/json");
    }
}
