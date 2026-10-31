package pe.edu.utp.clinica.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import pe.edu.utp.clinica.correo.*;
import java.time.Clock;

@WebListener
public final class CorreosCitaContextListener implements ServletContextListener {
    public static final String DESPERTAR=CorreosCitaContextListener.class.getName()+".despertar";
    private CorreosCitaDespachador despachador;

    @Override public void contextInitialized(ServletContextEvent evento) {
        if(!Boolean.parseBoolean(System.getenv().getOrDefault("APPOINTMENT_EMAIL_ENABLED","false")))return;
        if(!"gmail".equals(System.getenv("MAIL_MODE")))throw new IllegalArgumentException("Las confirmaciones requieren MAIL_MODE=gmail.");
        despachador=new CorreosCitaDespachador(new CorreosCitaJdbc(),ConfiguracionCorreo.crear(System.getenv()),Clock.systemUTC());
        evento.getServletContext().setAttribute(DESPERTAR,(Runnable)despachador::despertar);
        despachador.despertar();
    }

    @Override public void contextDestroyed(ServletContextEvent evento) {
        evento.getServletContext().removeAttribute(DESPERTAR);
        if(despachador!=null)despachador.close();
    }
}
