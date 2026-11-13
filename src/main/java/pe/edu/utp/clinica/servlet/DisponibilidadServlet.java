package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.config.DisponibilidadSistema;

@WebServlet(name = "DisponibilidadServlet", urlPatterns = "/health/ready")
public class DisponibilidadServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(DisponibilidadServlet.class.getName());
    protected boolean disponible() {
        Object servicio = getServletContext().getAttribute(DisponibilidadSistema.CONTEXT_ATTRIBUTE);
        return servicio instanceof DisponibilidadSistema sistema && sistema.disponible(1500);
    }

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        boolean listo = disponible();
        if (!listo) response.setHeader("Retry-After", "3");
        try {
            ApiJson.write(response, listo ? 200 : 503, new ApiResponse<>(
                    listo ? "SISTEMA_DISPONIBLE" : "SISTEMA_NO_DISPONIBLE",
                    listo ? "El sistema está disponible." : "El sistema no está disponible todavía. Inténtalo en unos segundos.",
                    null));
        } catch (IOException e) {
            if (!response.isCommitted()) response.setStatus(500);
            LOGGER.warning("No se pudo enviar la respuesta de disponibilidad: " + e.getClass().getSimpleName());
        }
    }
}
