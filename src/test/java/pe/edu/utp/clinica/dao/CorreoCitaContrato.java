package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.correo.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

abstract class CorreoCitaContrato extends AgendaCitaContrato {
    private CorreosCitaJdbc almacen;

    @BeforeEach void configurarCorreos()throws Exception {
        sql("ALTER TABLE usuario ADD COLUMN email VARCHAR(254)");
        sql("ALTER TABLE especialidad ADD COLUMN nombre VARCHAR(100)");
        sql("ALTER TABLE sede ADD COLUMN nombre VARCHAR(100)");
        sql("UPDATE usuario SET email='paciente@example.test' WHERE id_usuario=1");
        sql("UPDATE especialidad SET nombre='Medicina general'");
        sql("UPDATE sede SET nombre='Sede Ñúñez'");
        String ddl=Files.readString(Path.of("sql/migrations/V008__correo_confirmacion_cita.sql"));
        if(url.startsWith("jdbc:h2:"))ddl=ddl.replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4","");
        sql(ddl);
        dao=new AgendaCitaDAO(this::abrir,clock,true);
        almacen=new CorreosCitaJdbc(this::abrir);
    }

    private int reservar(){
        dao.publicar(1,1,"2026-10-28","09:00","12:00",20);
        return dao.reservar(1,1,"2026-10-28","09:00","BAJA");
    }
    private String estado(int cita)throws Exception {
        try(var conn=abrir();var st=conn.prepareStatement("SELECT estado FROM correo_cita WHERE id_cita=?")) {
            st.setInt(1,cita);try(var rs=st.executeQuery()){assertTrue(rs.next());return rs.getString(1);}
        }
    }
    private CorreosCitaServicio servicio(EmisorCorreo emisor,Clock reloj){return new CorreosCitaServicio(almacen,emisor,reloj);}

    @Test void enviaUnaSolaConfirmacionConDatosDeLaBase()throws Exception {
        int cita=reservar();var enviados=new ArrayList<Correo>();
        var servicio=servicio(c->{enviados.add(c);return "entrega-ficticia";},clock);
        assertEquals("PENDIENTE",estado(cita));assertTrue(servicio.procesarUno());assertFalse(servicio.procesarUno());
        assertEquals("ENVIADO",estado(cita));assertEquals(1,enviados.size());
        assertTrue(almacen.proxima().isEmpty());
        Correo correo=enviados.get(0);assertEquals("paciente@example.test",correo.destinatario());
        assertTrue(correo.texto().contains("28/10/2026"));assertTrue(correo.texto().contains("09:00"));
        assertTrue(correo.html().contains("Sede Ñúñez"));assertFalse(correo.html().contains("codigoVerificacion"));
    }
    @Test void noEnviaUnaCitaCancelada()throws Exception {
        int cita=reservar();sql("UPDATE cita SET estado='CANCELADA' WHERE id_cita="+cita);
        assertTrue(servicio(c->{fail("No debe enviar");return "";},clock).procesarUno());
        assertEquals("OMITIDO",estado(cita));
    }
    @Test void noEnviaAUnaCuentaInactiva()throws Exception {
        int cita=reservar();sql("UPDATE usuario SET activo=FALSE WHERE id_usuario=1");
        assertTrue(servicio(c->{fail("No debe enviar");return "";},clock).procesarUno());
        assertEquals("OMITIDO",estado(cita));
    }
    @Test void reprogramarSoloConfirmaLaNuevaCitaVigente()throws Exception {
        int anterior=reservar();assertTrue(dao.reprogramar(anterior,1,1,"2026-10-28","09:20",null,"BAJA"));
        var enviados=new ArrayList<Correo>();var servicio=servicio(c->{enviados.add(c);return "simulado";},clock);
        assertTrue(servicio.procesarUno());assertEquals("OMITIDO",estado(anterior));
        assertTrue(servicio.procesarUno());assertEquals(1,enviados.size());assertTrue(enviados.get(0).texto().contains("09:20"));
    }
    @Test void usaElCorreoActualDeLaCuentaAlProcesar()throws Exception {
        reservar();sql("UPDATE usuario SET email='nuevo@example.test' WHERE id_usuario=1");
        var enviados=new ArrayList<Correo>();assertTrue(servicio(c->{enviados.add(c);return "simulado";},clock).procesarUno());
        assertEquals("nuevo@example.test",enviados.get(0).destinatario());
    }
    @Test void correoInvalidoNoSeReintentaNiAfectaLaCita()throws Exception {
        int cita=reservar();sql("UPDATE usuario SET email='direccion-invalida' WHERE id_usuario=1");
        assertTrue(servicio(c->{fail("No debe enviar");return "";},clock).procesarUno());
        assertEquals("NO_CONFIRMADO",estado(cita));
    }
    @Test void conLaFuncionDesactivadaNoNecesitaLaTablaDeCorreos()throws Exception {
        sql("DROP TABLE correo_cita");dao=new AgendaCitaDAO(this::abrir,clock,false);
        assertTrue(reservar()>0);
    }
    @Test void unaFallaDelProveedorNoBorraLaReservaYAdmiteReintentosLimitados()throws Exception {
        int cita=reservar();var intentos=new AtomicInteger();
        EmisorCorreo falla=c->{intentos.incrementAndGet();throw new EnvioCorreoException("Fallo simulado",true);};
        assertTrue(servicio(falla,clock).procesarUno());assertFalse(servicio(falla,clock).procesarUno());
        assertEquals(clock.instant().plusSeconds(60),almacen.proxima().orElseThrow());
        assertTrue(servicio(falla,Clock.offset(clock,Duration.ofSeconds(61))).procesarUno());
        assertTrue(servicio(falla,Clock.offset(clock,Duration.ofSeconds(362))).procesarUno());
        assertEquals(3,intentos.get());assertEquals("NO_CONFIRMADO",estado(cita));
        try(var conn=abrir();var st=conn.createStatement();var rs=st.executeQuery("SELECT estado FROM cita WHERE id_cita="+cita)) {
            assertTrue(rs.next());assertEquals("CONFIRMADA",rs.getString(1));
        }
    }
    @Test void unaEntregaInciertaNoSeRepiteAutomaticamente()throws Exception {
        int cita=reservar();var llamadas=new AtomicInteger();
        var servicio=servicio(c->{llamadas.incrementAndGet();throw new EnvioCorreoException("Entrega desconocida",false);},clock);
        assertTrue(servicio.procesarUno());assertFalse(servicio.procesarUno());
        assertEquals(1,llamadas.get());assertEquals("NO_CONFIRMADO",estado(cita));
    }
    @Test void recuperaElEstadoInterrumpidoSinDuplicarUnEnvio()throws Exception {
        int cita=reservar();assertTrue(almacen.tomar(clock.instant()).isPresent());
        assertEquals(clock.instant().plusSeconds(901),almacen.proxima().orElseThrow());
        var despues=Clock.offset(clock,Duration.ofSeconds(901));
        assertFalse(servicio(c->{fail("No debe reenviar");return "";},despues).procesarUno());
        assertEquals("NO_CONFIRMADO",estado(cita));
    }
    @Test void dosProcesadoresNoEnvianLaMismaCitaYNoBloqueanLaBaseDuranteElCorreo()throws Exception {
        int cita=reservar();var iniciado=new CountDownLatch(1);var terminar=new CountDownLatch(1);var llamadas=new AtomicInteger();
        EmisorCorreo emisor=c->{
            llamadas.incrementAndGet();iniciado.countDown();
            try { if(!terminar.await(10,TimeUnit.SECONDS))throw new EnvioCorreoException("Espera agotada",false); }
            catch(InterruptedException e){Thread.currentThread().interrupt();throw new EnvioCorreoException("Interrumpido",false);}
            return "entrega-ficticia";
        };
        var executor=Executors.newSingleThreadExecutor();
        try {
            var primero=executor.submit(()->servicio(emisor,clock).procesarUno());
            assertTrue(iniciado.await(10,TimeUnit.SECONDS));assertEquals("ENVIANDO",estado(cita));
            assertFalse(servicio(emisor,clock).procesarUno());terminar.countDown();assertTrue(primero.get(10,TimeUnit.SECONDS));
            assertEquals(1,llamadas.get());
        } finally {terminar.countDown();executor.shutdownNow();}
    }
    @Test void falloDeLaColaRevierteLaReservaYElAvisoDePlataforma()throws Exception {
        sql("DROP TABLE correo_cita");assertThrows(AgendaException.class,this::reservar);
        try(var conn=abrir();var st=conn.createStatement();var rs=st.executeQuery("SELECT COUNT(*) FROM cita")) {
            assertTrue(rs.next());assertEquals(0,rs.getInt(1));
        }
    }
}
