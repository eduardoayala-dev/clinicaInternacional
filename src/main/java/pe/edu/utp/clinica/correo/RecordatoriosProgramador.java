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
    private final ScheduledExecutorService tareas=Executors.newSingleThreadScheduledExecutor(r->{
        var hilo=new Thread(r,"recordatorios-citas");hilo.setDaemon(true);return hilo;
    });
    public RecordatoriosProgramador(CorreosRecordatorioJdbc almacen,CorreosDespachador despachador,Clock reloj,int horas){
        this(almacen,despachador,reloj,()->horas);
        if(horas<1||horas>168)throw new IllegalArgumentException("Anticipación de recordatorio inválida.");
    }
    public RecordatoriosProgramador(CorreosRecordatorioJdbc almacen,CorreosDespachador despachador,Clock reloj,java.util.function.IntSupplier horas){
        this.almacen=almacen;this.despachador=despachador;this.reloj=reloj;this.horas=horas;
    }
    public void iniciar(){tareas.scheduleWithFixedDelay(this::procesar,0,60,TimeUnit.SECONDS);}
    private void procesar(){
        try{almacen.programar(reloj.instant(),horas.getAsInt());despachador.despertar();}
        catch(SQLException|RuntimeException e){LOGGER.warning("No se pudo programar recordatorios. Se reintentará en el próximo ciclo.");}
    }
    @Override public void close(){tareas.shutdownNow();despachador.close();}
}
