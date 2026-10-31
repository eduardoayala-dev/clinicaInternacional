package pe.edu.utp.clinica.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import pe.edu.utp.clinica.correo.*;
import java.time.Clock;

@WebListener
public final class CorreosCitaContextListener implements ServletContextListener {
    public static final String DESPERTAR=CorreosCitaContextListener.class.getName()+".despertar";
    private CorreosDespachador despachador;
    private CorreosDespachador ofertas;

    @Override public void contextInitialized(ServletContextEvent evento) {
        if(!Boolean.parseBoolean(System.getenv().getOrDefault("APPOINTMENT_EMAIL_ENABLED","false")))return;
        if(!"gmail".equals(System.getenv("MAIL_MODE")))throw new IllegalArgumentException("Los avisos por correo requieren MAIL_MODE=gmail.");
        var emisor=ConfiguracionCorreo.crear(System.getenv());
        var reloj=Clock.systemUTC();
        despachador=new CorreosDespachador(new CorreosCitaJdbc(),emisor,reloj);
        ofertas=new CorreosDespachador(new CorreosOfertaJdbc(),emisor,reloj);
        evento.getServletContext().setAttribute(DESPERTAR,(Runnable)this::despertar);
        despertar();
    }

    private void despertar() { despachador.despertar(); ofertas.despertar(); }

    @Override public void contextDestroyed(ServletContextEvent evento) {
        evento.getServletContext().removeAttribute(DESPERTAR);
        if(despachador!=null)despachador.close();
        if(ofertas!=null)ofertas.close();
    }
}
