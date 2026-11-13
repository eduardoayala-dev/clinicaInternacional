package pe.edu.utp.clinica.util;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public final class TransporteHttp {
    private static final Logger LOGGER=Logger.getLogger(TransporteHttp.class.getName());
    private TransporteHttp() {}

    public static void error(HttpServletResponse response,int estado) {
        try {
            response.sendError(estado);
        } catch (IOException e) {
            LOGGER.warning("No se pudo entregar la respuesta de error HTTP.");
        }
    }

    public static void redirigir(HttpServletResponse response,String destino) {
        try {
            response.sendRedirect(destino);
        } catch (IOException e) {
            LOGGER.warning("No se pudo entregar la redirección HTTP.");
        }
    }
}
