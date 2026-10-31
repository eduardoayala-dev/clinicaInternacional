package pe.edu.utp.clinica.correo;

import java.sql.SQLException;
import java.time.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public final class CorreosDespachador implements AutoCloseable {
    private static final Logger LOGGER=Logger.getLogger(CorreosDespachador.class.getName());
    private final CorreosServicio servicio;
    private final CorreosPendientesJdbc almacen;
    private final Clock reloj;
    private final ScheduledThreadPoolExecutor tareas;
    private ScheduledFuture<?> programada;

    public CorreosDespachador(CorreosPendientesJdbc almacen,EmisorCorreo emisor,Clock reloj) {
        this.almacen=almacen;this.reloj=reloj;this.servicio=new CorreosServicio(almacen,emisor,reloj);
        tareas=new ScheduledThreadPoolExecutor(1,runnable->{
            var hilo=new Thread(runnable,"avisos-correo");hilo.setDaemon(true);return hilo;
        });
        tareas.setRemoveOnCancelPolicy(true);
    }

    public void despertar(){programar(0);}

    private synchronized void programar(long segundos) {
        if(tareas.isShutdown())return;
        if(programada!=null && !programada.isDone()) {
            if(programada.getDelay(TimeUnit.SECONDS)<=segundos)return;
            programada.cancel(false);
        }
        programada=tareas.schedule(this::procesar,segundos,TimeUnit.SECONDS);
    }

    private void procesar() {
        synchronized(this){programada=null;}
        try {
            for(int cantidad=0;cantidad<5;cantidad++)if(!servicio.procesarUno())break;
            var proxima=almacen.proxima();
            if(proxima.isPresent())programar(Math.max(1,Duration.between(reloj.instant(),proxima.get()).getSeconds()));
        } catch(SQLException|RuntimeException e) {
            programar(60);
            LOGGER.warning("No se pudo completar el procesamiento de avisos por correo. Se reintentará en 60 segundos.");
        }
    }

    @Override public synchronized void close(){tareas.shutdownNow();}
}
