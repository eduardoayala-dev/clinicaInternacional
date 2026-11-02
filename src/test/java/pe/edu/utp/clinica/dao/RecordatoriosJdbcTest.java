package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.correo.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class RecordatoriosJdbcTest extends CorreoCitaContrato {
    private CorreosRecordatorioJdbc recordatorios;
    @Override protected void configurar(){url="jdbc:h2:mem:"+java.util.UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";}
    @BeforeEach void prepararRecordatorios()throws Exception {
        sql(Files.readString(Path.of("sql/migrations/V017__correo_recordatorio.sql")).replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",""));
        recordatorios=new CorreosRecordatorioJdbc(this::abrir,"https://clinica.example.test");
    }
    private int cita(){
        dao.publicar(1,1,"2026-10-28","09:00","12:00",20);
        return dao.reservar(1,1,"2026-10-28","09:00","MEDIA");
    }
    private ArrayList<Correo> enviar()throws Exception {
        var enviados=new ArrayList<Correo>();var servicio=new CorreosServicio(recordatorios,c->{enviados.add(c);return "prueba";},clock);
        while(servicio.procesarUno()){}
        return enviados;
    }
    @Test void unSoloRecordatorioPorCitaAunqueSeProgramenVariosCiclos()throws Exception {
        cita();recordatorios.programar(clock.instant(),48);recordatorios.programar(clock.instant(),48);
        var mensajes=enviar();assertEquals(1,mensajes.size());assertTrue(mensajes.get(0).html().contains("https://clinica.example.test/paciente/citas"));
        assertTrue(mensajes.get(0).texto().contains("asistencia está pendiente"));
        recordatorios.programar(clock.instant(),48);assertTrue(enviar().isEmpty());
    }
    @Test void soloSeProgramaDentroDelMargenYAntesDeLaCita()throws Exception {
        cita();recordatorios.programar(clock.instant(),1);assertTrue(enviar().isEmpty());
        recordatorios.programar(clock.instant().plus(Duration.ofDays(2)),48);assertTrue(enviar().isEmpty());
        assertEquals(0,count("SELECT COUNT(*) FROM correo_recordatorio"));
    }
    @Test void confirmarDespuesDeProgramarOmiteElEnvio()throws Exception {
        int id=cita();recordatorios.programar(clock.instant(),48);
        sql("UPDATE confirmacion_asistencia SET confirmada_en='2026-10-27 11:00:00' WHERE id_cita="+id);
        assertTrue(enviar().isEmpty());assertEquals(1,count("SELECT COUNT(*) FROM correo_recordatorio WHERE estado='OMITIDO'"));
    }
    @Test void cancelarDespuesDeProgramarOmiteElEnvio()throws Exception {
        int id=cita();recordatorios.programar(clock.instant(),48);sql("UPDATE cita SET estado='CANCELADA' WHERE id_cita="+id);
        assertTrue(enviar().isEmpty());
    }
    @Test void noProgramaCuentasInactivasNiAsistenciasConfirmadas()throws Exception {
        int id=cita();sql("UPDATE usuario SET activo=FALSE WHERE id_usuario=1");recordatorios.programar(clock.instant(),48);
        assertEquals(0,count("SELECT COUNT(*) FROM correo_recordatorio"));
        sql("UPDATE usuario SET activo=TRUE WHERE id_usuario=1");sql("UPDATE confirmacion_asistencia SET confirmada_en='2026-10-27 11:00:00' WHERE id_cita="+id);
        recordatorios.programar(clock.instant(),48);assertTrue(enviar().isEmpty());
    }
    @Test void alReprogramarSeOmiteLaAnteriorYSeRecuerdaLaNueva()throws Exception {
        int id=cita();recordatorios.programar(clock.instant(),48);assertTrue(dao.reprogramar(id,1,1,"2026-10-28","09:20",null,"MEDIA"));
        recordatorios.programar(clock.instant(),48);var mensajes=enviar();assertEquals(1,mensajes.size());assertTrue(mensajes.get(0).texto().contains("09:20"));
    }
    @Test void dosProgramadoresConcurrentesNoDuplicanLaCola()throws Exception {
        cita();var tareas=Executors.newFixedThreadPool(2);
        try {
            var a=tareas.submit(()->recordatorios.programar(clock.instant(),48));
            var b=tareas.submit(()->recordatorios.programar(clock.instant(),48));
            a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);
            assertEquals(1,count("SELECT COUNT(*) FROM correo_recordatorio"));assertEquals(1,enviar().size());
        } finally {tareas.shutdownNow();}
    }
    @Test void unaEntregaInciertaDeRecordatorioNoSeRepite()throws Exception {
        cita();recordatorios.programar(clock.instant(),48);
        var servicio=new CorreosServicio(recordatorios,c->{throw new EnvioCorreoException("Entrega incierta",false);},clock);
        assertTrue(servicio.procesarUno());assertFalse(servicio.procesarUno());
        recordatorios.programar(clock.instant(),48);assertFalse(servicio.procesarUno());
        assertEquals(1,count("SELECT COUNT(*) FROM correo_recordatorio WHERE estado='NO_CONFIRMADO'"));
    }
    @Test void unRecordatorioPendienteQueYaPasoNoSeEnvia()throws Exception {
        cita();recordatorios.programar(clock.instant(),48);
        var despues=java.time.Clock.offset(clock,Duration.ofDays(2));
        var servicio=new CorreosServicio(recordatorios,c->{fail("No debe recordar una cita pasada");return "";},despues);
        assertTrue(servicio.procesarUno());assertFalse(servicio.procesarUno());
        assertEquals(1,count("SELECT COUNT(*) FROM correo_recordatorio WHERE estado='OMITIDO'"));
    }
}
