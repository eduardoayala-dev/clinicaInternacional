package pe.edu.utp.clinica.util;

import jakarta.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

public final class TransporteServlet {
    private static final Logger LOGGER=Logger.getLogger(TransporteServlet.class.getName());
    private TransporteServlet() {}
    public static void fallo(HttpServletResponse respuesta) {
        LOGGER.warning("No se pudo entregar la respuesta HTTP de la operación.");
        if(!respuesta.isCommitted())respuesta.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
