package pe.edu.utp.clinica.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.logging.Logger;

@WebListener
public class AppContextListener implements ServletContextListener {

    private MantenimientoOfertas ofertas;
    private DisponibilidadSistema disponibilidad;

    private static final Logger LOGGER = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        jakarta.servlet.SessionCookieConfig cookie = sce.getServletContext().getSessionCookieConfig();
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
        boolean produccion = System.getenv("RAILWAY_ENVIRONMENT") != null
                || "production".equalsIgnoreCase(System.getenv("APP_ENV"));
        cookie.setSecure(produccion);
        disponibilidad = new DisponibilidadSistema();
        sce.getServletContext().setAttribute(DisponibilidadSistema.CONTEXT_ATTRIBUTE, disponibilidad);
        disponibilidad.iniciar();
        ofertas = new MantenimientoOfertas();
        ofertas.iniciar();
        LOGGER.info("Aplicación Clínica Internacional iniciada correctamente.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("Deteniendo recursos de la aplicación...");
        if (ofertas != null) ofertas.close();
        if (disponibilidad != null) disponibilidad.close();
        DatabaseConfig.shutdown();
        LOGGER.info("Recursos de base de datos liberados exitosamente.");
    }
}
