package pe.edu.utp.clinica.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import pe.edu.utp.clinica.config.CorreosCitaContextListener;
import java.io.IOException;

@WebFilter(asyncSupported = true, urlPatterns="/*")
public final class CorreosCitaFilter implements Filter {
    @Override public void doFilter(ServletRequest request,ServletResponse response,FilterChain chain)throws IOException,ServletException {
        try { chain.doFilter(request,response); }
        finally {
            if(request instanceof HttpServletRequest http && "POST".equals(http.getMethod())
                    && http.getServletContext().getAttribute(CorreosCitaContextListener.DESPERTAR) instanceof Runnable despertar) {
                despertar.run();
            }
        }
    }
}
