package pe.edu.utp.clinica.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import pe.edu.utp.clinica.correo.ConfiguracionCorreo;
import pe.edu.utp.clinica.recuperacion.*;
import java.time.Clock;
import java.util.concurrent.*;

@WebListener
public final class RecuperacionContextListener implements ServletContextListener {
    private ThreadPoolExecutor tareas;

    @Override public void contextInitialized(ServletContextEvent evento) {
        if (!Boolean.parseBoolean(System.getenv().getOrDefault("RECOVERY_ENABLED","false"))) return;
        if (!"jwt".equals(System.getenv("AUTH_MODE")) || !"gmail".equals(System.getenv("MAIL_MODE"))) {
            throw new IllegalArgumentException("La recuperación requiere AUTH_MODE=jwt y MAIL_MODE=gmail.");
        }
        tareas=new ThreadPoolExecutor(2,2,0L,TimeUnit.SECONDS,new ArrayBlockingQueue<>(32),runnable->{
            Thread hilo=new Thread(runnable,"recuperacion-correo"); hilo.setDaemon(true); return hilo;
        });
        var codigos=new CodigosVerificacion(System.getenv("ACCOUNT_VERIFICATION_KEY"));
        var servicio=new RecuperacionServicio(new VerificacionJdbc(codigos),ConfiguracionCorreo.crear(System.getenv()),
                Clock.systemUTC(),tareas);
        evento.getServletContext().setAttribute(RecuperacionServicio.CONTEXTO,servicio);
    }

    @Override public void contextDestroyed(ServletContextEvent evento) {
        if (tareas!=null) tareas.shutdownNow();
    }
}
