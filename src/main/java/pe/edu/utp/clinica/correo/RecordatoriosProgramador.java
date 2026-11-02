package pe.edu.utp.clinica.correo;

import java.sql.SQLException;
import java.time.Clock;
import java.util.concurrent.*;
import java.util.logging.Logger;

public final class RecordatoriosProgramador implements AutoCloseable {
    private static final Logger LOGGER=Logger.getLogger(RecordatoriosProgramador.class.getName());
    private final CorreosRecordatorioJdbc almacen;
    private final CorreosDespachador despachador;
    private final Clock reloj;
    private final java.util.function.IntSupplier horas;
    private final AvisosVencimientoJdbc vencimientos;
    private final Runnable avisarCambios;
    private final ScheduledExecutorService tareas=Executors.newSingleThreadScheduledExecutor(r->{
        var hilo=new Thread(r,"recordatorios-citas");hilo.setDaemon(true);return hilo;
    });
    public RecordatoriosProgramador(CorreosRecordatorioJdbc almacen,AvisosVencimientoJdbc vencimientos,CorreosDespachador despachador,Clock reloj,java.util.function.IntSupplier horas,Runnable avisarCambios){
        this.almacen=almacen;this.despachador=despachador;this.reloj=reloj;this.horas=horas;
        this.vencimientos=vencimientos;this.avisarCambios=avisarCambios;
    }
    public void iniciar(){tareas.scheduleWithFixedDelay(this::procesar,0,60,TimeUnit.SECONDS);}
    private void procesar(){
        try{
            var ahora=reloj.instant();
            almacen.programar(ahora,horas.getAsInt());
            vencimientos.programar(ahora);
        }
        catch(SQLException|RuntimeException e){LOGGER.warning("No se pudo programar recordatorios. Se reintentará en el próximo ciclo.");}
        finally{despachador.despertar();avisarCambios.run();}
    }
    @Override public void close(){tareas.shutdownNow();despachador.close();}
}
