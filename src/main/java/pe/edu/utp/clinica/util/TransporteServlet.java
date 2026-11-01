package pe.edu.utp.clinica.util;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public final class TransporteServlet {
    private static final Logger LOGGER=Logger.getLogger(TransporteServlet.class.getName());
    private TransporteServlet() {}
    @FunctionalInterface
    public interface Operacion {
        void ejecutar() throws IOException, ServletException;
    }
    public static void entregar(HttpServletResponse respuesta, Operacion operacion) {
        try {
            operacion.ejecutar();
        } catch(IOException | ServletException e) {
            LOGGER.warning("No se pudo entregar la respuesta HTTP de la operación.");
            if(!respuesta.isCommitted())respuesta.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
