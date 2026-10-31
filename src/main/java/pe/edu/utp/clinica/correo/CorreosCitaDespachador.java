package pe.edu.utp.clinica.correo;

import java.sql.SQLException;
import java.time.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public final class CorreosCitaDespachador implements AutoCloseable {
    private static final Logger LOGGER=Logger.getLogger(CorreosCitaDespachador.class.getName());
    private final CorreosCitaServicio servicio;
    private final CorreosCitaJdbc almacen;
    private final Clock reloj;
    private final ScheduledThreadPoolExecutor tareas;
    private ScheduledFuture<?> programada;

    public CorreosCitaDespachador(CorreosCitaJdbc almacen,EmisorCorreo emisor,Clock reloj) {
        this.almacen=almacen;this.reloj=reloj;this.servicio=new CorreosCitaServicio(almacen,emisor,reloj);
        tareas=new ScheduledThreadPoolExecutor(1,runnable->{
            var hilo=new Thread(runnable,"confirmaciones-correo");hilo.setDaemon(true);return hilo;
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
            LOGGER.warning("No se pudo completar el procesamiento de correos de citas. Se retomará al recibir una operación o reiniciar el servicio.");
        }
    }

    @Override public synchronized void close(){tareas.shutdownNow();}
}
