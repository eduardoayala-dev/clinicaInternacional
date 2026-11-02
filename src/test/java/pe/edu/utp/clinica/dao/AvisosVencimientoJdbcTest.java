package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.correo.*;
import java.nio.file.*;
import java.time.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class AvisosVencimientoJdbcTest extends CorreoCitaContrato {
    private AvisosVencimientoJdbc vencimientos;
    @Override protected void configurar(){url="jdbc:h2:mem:"+java.util.UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";}
    @BeforeEach void prepararVencimientos()throws Exception {
        sql(Files.readString(Path.of("sql/migrations/V017__correo_recordatorio.sql")).replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",""));
        vencimientos=new AvisosVencimientoJdbc(this::abrir);
    }
    private int cita()throws Exception {
        dao.publicar(1,1,"2026-10-28","09:00","12:00",20);
        int id=dao.reservar(1,1,"2026-10-28","09:00","MEDIA");
        sql("UPDATE confirmacion_asistencia SET vence_en='2026-10-27 10:10:00' WHERE id_cita="+id);
        return id;
    }
    private ArrayList<Correo> enviar(Clock reloj)throws Exception {
        var mensajes=new ArrayList<Correo>();
        var servicio=new CorreosServicio(new CorreosCambiosJdbc(this::abrir),c->{mensajes.add(c);return "prueba";},reloj);
        for(int i=0;i<10;i++){if(!servicio.procesarUno())return mensajes;}
        throw new AssertionError("La cola de vencimientos no terminó de procesarse.");
    }
    @Test void enElLimiteSeEnviaUnaVezSinCancelarNiCambiarPrioridad()throws Exception {
        int id=cita();sql("UPDATE cita SET nivel_urgencia='ALTA' WHERE id_cita="+id);
        vencimientos.programar(clock.instant());vencimientos.programar(clock.instant());
        var mensajes=enviar(clock);assertEquals(1,mensajes.size());assertTrue(mensajes.get(0).texto().contains("Tu cita sigue vigente"));
        assertTrue(mensajes.get(0).texto().contains("Este aviso no cancela tu reserva"));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA' AND nivel_urgencia='ALTA'"));
        vencimientos.programar(clock.instant());assertTrue(enviar(clock).isEmpty());
    }
    @Test void noSeProgramaAntesDelVencimiento()throws Exception {
        cita();vencimientos.programar(clock.instant().minusSeconds(1));assertTrue(enviar(clock).isEmpty());
        assertEquals(0,count("SELECT COUNT(*) FROM correo_cambio_cita"));
    }
    @Test void noSeEnviaAdemasElRecordatorioNormalCuandoYaVencio()throws Exception {
        int id=cita();var normal=new CorreosRecordatorioJdbc(this::abrir,"http://localhost:8080");
        normal.programar(clock.instant().minusSeconds(1),48);
        assertEquals(1,count("SELECT COUNT(*) FROM correo_recordatorio"));
        vencimientos.programar(clock.instant());
        var servicio=new CorreosServicio(normal,c->{fail("El recordatorio anterior ya venció");return "";},clock);
        assertTrue(servicio.procesarUno());assertEquals(1,count("SELECT COUNT(*) FROM correo_recordatorio WHERE estado='OMITIDO'"));
        assertEquals(1,enviar(clock).size());
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE id_cita="+id+" AND estado='CONFIRMADA'"));
    }
    @Test void confirmarPorAdmisionDespuesDeEncolarOmiteElAviso()throws Exception {
        int id=cita();vencimientos.programar(clock.instant());
        sql("UPDATE confirmacion_asistencia SET confirmada_en='2026-10-27 10:10:01' WHERE id_cita="+id);
        assertTrue(enviar(clock).isEmpty());assertEquals(1,count("SELECT COUNT(*) FROM correo_cambio_cita WHERE estado='OMITIDO'"));
    }
    @Test void cancelarDespuesDeEncolarOmiteElAviso()throws Exception {omitirPorEstado("CANCELADA");}
    @Test void reprogramarDespuesDeEncolarOmiteElAviso()throws Exception {omitirPorEstado("REPROGRAMADA");}
    private void omitirPorEstado(String estado)throws Exception {
        int id=cita();vencimientos.programar(clock.instant());sql("UPDATE cita SET estado='"+estado+"' WHERE id_cita="+id);
        assertTrue(enviar(clock).isEmpty());
    }
    @Test void noSeRecuerdaUnaCitaPasadaNiUnaCuentaInactiva()throws Exception {
        cita();vencimientos.programar(clock.instant().plus(Duration.ofDays(2)));assertTrue(enviar(clock).isEmpty());
        sql("UPDATE usuario SET activo=FALSE WHERE id_usuario=1");vencimientos.programar(clock.instant());assertTrue(enviar(clock).isEmpty());
        assertEquals(0,count("SELECT COUNT(*) FROM correo_cambio_cita"));
    }
    @Test void unAvisoPendienteSeOmiteSiLaCitaPasoDuranteLaSuspension()throws Exception {
        cita();vencimientos.programar(clock.instant());assertTrue(enviar(Clock.offset(clock,Duration.ofDays(2))).isEmpty());
        assertEquals(1,count("SELECT COUNT(*) FROM correo_cambio_cita WHERE estado='OMITIDO'"));
    }
    @Test void programadoresConcurrentesNoDuplicanElAviso()throws Exception {
        cita();var tareas=Executors.newFixedThreadPool(2);
        try {
            var a=tareas.submit(()->vencimientos.programar(clock.instant()));var b=tareas.submit(()->vencimientos.programar(clock.instant()));
            a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);
            assertEquals(1,count("SELECT COUNT(*) FROM correo_cambio_cita"));assertEquals(1,enviar(clock).size());
        } finally {tareas.shutdownNow();}
    }
    @Test void unaEntregaInciertaNoSeReiniciaEnOtroCiclo()throws Exception {
        cita();vencimientos.programar(clock.instant());
        var servicio=new CorreosServicio(new CorreosCambiosJdbc(this::abrir),c->{throw new EnvioCorreoException("Entrega incierta",false);},clock);
        assertTrue(servicio.procesarUno());vencimientos.programar(clock.instant());assertFalse(servicio.procesarUno());
        assertEquals(1,count("SELECT COUNT(*) FROM correo_cambio_cita WHERE estado='NO_CONFIRMADO'"));
    }
}

