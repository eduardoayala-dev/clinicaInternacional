package pe.edu.utp.clinica.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import pe.edu.utp.clinica.auth.GestorAutenticacion;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

public final class AutenticacionJwtFilter implements Filter {
    private static final Logger LOGGER=Logger.getLogger(AutenticacionJwtFilter.class.getName());
    @Override public void doFilter(ServletRequest request,ServletResponse response,FilterChain chain) throws IOException,ServletException {
        var req=(HttpServletRequest)request;
        var res=(HttpServletResponse)response;
        var gestor=GestorAutenticacion.obtener(req);
        String path=req.getServletPath();
        if (gestor==null || path.startsWith("/assets/") || "/health/ready".equals(path)) {
            chain.doFilter(request,response); return;
        }
        try {
            gestor.resolver(req,res,actividadHumana(req,path));
        } catch (SQLException e) {
            LOGGER.warning("No se pudo verificar el acceso: "+pe.edu.utp.clinica.config.DatabaseConfig.diagnosticoSeguro(e));
            res.setHeader("Retry-After","3");
            res.setHeader("Cache-Control","no-store");
            String accept=req.getHeader("Accept");
            if (accept!=null && accept.contains("application/json")) {
                ApiJson.write(res,503,new ApiResponse<>("AUTENTICACION_NO_DISPONIBLE","No se pudo comprobar tu sesión. Inténtalo nuevamente.",null));
            } else res.sendError(503);
            return;
        }
        chain.doFilter(request,response);
    }
    private static boolean actividadHumana(HttpServletRequest request, String path) {
        if ("/auth/estado".equals(path) || "/admin/eventos-confirmaciones".equals(path)) return false;
        return !("/admin/seguimiento-confirmaciones".equals(path)
                && "XMLHttpRequest".equals(request.getHeader("X-Requested-With")));
    }
}
