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
        boolean produccion = EntornoAplicacion.produccion(System.getenv());
        cookie.setSecure(produccion);
        String modo=System.getenv().getOrDefault("AUTH_MODE","session");
        if ("jwt".equals(modo)) {
            var reloj=java.time.Clock.systemUTC();
            var jwt=new pe.edu.utp.clinica.auth.JwtAcceso(System.getenv("AUTH_JWT_KEY"),
                    System.getenv().getOrDefault("AUTH_ISSUER","clinica-internacional"),
                    System.getenv().getOrDefault("AUTH_AUDIENCE","clinica-web"),reloj);
            var gestor=new pe.edu.utp.clinica.auth.GestorAutenticacion(jwt,new pe.edu.utp.clinica.auth.SesionesJdbc(),reloj,produccion);
            sce.getServletContext().setAttribute(pe.edu.utp.clinica.auth.GestorAutenticacion.CONTEXTO,gestor);
        } else if (!"session".equals(modo)) {
            throw new IllegalArgumentException("AUTH_MODE debe ser jwt o session.");
        }

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
