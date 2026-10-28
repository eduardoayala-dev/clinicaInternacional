package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import static org.junit.jupiter.api.Assertions.*;

abstract class AdmisionCitaContrato extends CitaAutorizacionTest {
    @BeforeEach void personal() throws Exception {
        sql("INSERT INTO usuario(id_usuario,nombres,rol) VALUES(40,'Admisión','PERSONAL_ADMINISTRATIVO'),(50,'Otra sede','PERSONAL_ADMINISTRATIVO')");
        sql("INSERT INTO sede(id_sede,nombre) VALUES(2,'Otra sede')");
        sql("INSERT INTO personal_administrativo(id_personal,id_usuario,id_sede) VALUES(1,40,1),(2,50,2)");
    }
    private void sql(String sql) throws Exception {
        try (Connection c=abrir(); Statement s=c.createStatement()) { s.execute(sql); }
    }
    private AdmisionCitaDAO admision(String instante) {
        return new AdmisionCitaDAO(this::abrir, Clock.fixed(Instant.parse(instante), ZoneId.of("UTC")), 10);
    }
    private AdmisionCitaDAO admision() { return admision("2026-10-28T15:00:00Z"); }
    private void error(int status, Runnable operacion) {
        assertEquals(status, assertThrows(AgendaException.class, operacion::run).status());
    }

    @Test void registraHoraServidorResponsableEstadoYRetraso() throws Exception {
        var llegada=admision().registrar(40,7);
        assertEquals("2026-10-28T10:00", llegada.fechaLlegada());
        assertTrue(llegada.llegoTarde()); assertEquals(10,llegada.toleranciaMinutos());
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE estado='EN_SALA'"));
        assertEquals(1,count("SELECT COUNT(*) FROM admision_cita WHERE id_cita=7 AND id_usuario_personal=40 AND fecha_llegada='2026-10-28 10:00:00' AND llego_tarde=TRUE"));
        assertEquals(0,count("SELECT COUNT(*) FROM historial_asistencia"));
    }
    @Test void limiteToleranciaInclusivoYHoraLima() throws Exception {
        assertFalse(admision("2026-10-28T14:10:00Z").registrar(40,7).llegoTarde());
        assertEquals(1,count("SELECT COUNT(*) FROM admision_cita WHERE llego_tarde=FALSE"));
    }
    @Test void unSegundoFueraDelLimiteEsRetraso() {
        assertTrue(admision("2026-10-28T14:10:01Z").registrar(40,7).llegoTarde());
    }
    @Test void permiteLlegadaAnticipadaDelMismoDia() {
        assertFalse(admision("2026-10-28T13:30:00Z").registrar(40,7).llegoTarde());
    }
    @Test void medianocheUtcNoCambiaElDiaClinico() throws Exception {
        sql("UPDATE cita SET fecha_hora='2026-10-27 23:00:00'");
        assertEquals("2026-10-27T23:30",admision("2026-10-28T04:30:00Z").registrar(40,7).fechaLlegada());
    }
    @Test void perfilInactivoNoPuedeUsarSesionAnterior() throws Exception {
        sql("UPDATE usuario SET activo=FALSE WHERE id_usuario=40");
        error(403,()->admision().registrar(40,7));
        assertEquals(0,count("SELECT COUNT(*) FROM admision_cita"));
    }
    @Test void rolCambiadoYSedeDesactivadaSonRechazados() throws Exception {
        sql("UPDATE usuario SET rol='PACIENTE' WHERE id_usuario=40");
        error(403,()->admision().registrar(40,7));
        sql("UPDATE usuario SET rol='PERSONAL_ADMINISTRATIVO' WHERE id_usuario=40");
        sql("UPDATE sede SET estado='INACTIVO' WHERE id_sede=1");
        error(403,()->admision().registrar(40,7));
    }
    @Test void pacienteMedicoYSinPerfilNoPuedenAdmitir() {
        for(int usuario:List.of(10,20,999)) error(403,()->admision().registrar(usuario,7));
    }
    @Test void noExponeCitasDeOtrasSedesONoExistentes() {
        error(404,()->admision().registrar(50,7));
        error(404,()->admision().registrar(40,999));
    }
    @Test void rechazaIdentificadoresInvalidos() {
        error(400,()->admision().registrar(0,7));
        error(400,()->admision().registrar(40,-1));
    }
    @Test void noAdmiteOtrosDiasNiEstadosTerminales() throws Exception {
        for(String estado:List.of("SOLICITADA","CANCELADA","REPROGRAMADA","ATENDIDA","NO_ASISTIO","EN_SALA")) {
            sql("UPDATE cita SET estado='"+estado+"'");
            error(409,()->admision().registrar(40,7));
        }
        sql("UPDATE cita SET estado='CONFIRMADA',fecha_hora='2026-10-29 09:00:00'");
        error(409,()->admision().registrar(40,7));
        sql("UPDATE cita SET fecha_hora='2026-10-27 09:00:00'");
        error(409,()->admision().registrar(40,7));
    }
    @Test void repetirLlegadaNoSobrescribeHoraNiResponsable() throws Exception {
        admision().registrar(40,7);
        error(409,()->admision("2026-10-28T16:00:00Z").registrar(40,7));
        assertEquals(1,count("SELECT COUNT(*) FROM admision_cita WHERE fecha_llegada='2026-10-28 10:00:00'"));
    }
    @Test void falloDelRegistroNoModificaEstado() throws Exception {
        sql("DROP TABLE admision_cita");
        error(503,()->admision().registrar(40,7));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA'"));
    }
    @Test void dosSolicitudesSimultaneasSoloRegistranUnaLlegada() throws Exception {
        var pool=Executors.newFixedThreadPool(2);
        var iniciar=new CountDownLatch(1);
        java.util.concurrent.Callable<Integer> tarea=()->{iniciar.await();try {admision().registrar(40,7);return 200;} catch(AgendaException e){return e.status();}};
        try {
            var a=pool.submit(tarea);var b=pool.submit(tarea);iniciar.countDown();
            assertEquals(List.of(200,409),java.util.stream.Stream.of(a.get(),b.get()).sorted().toList());
            assertEquals(1,count("SELECT COUNT(*) FROM admision_cita"));
        } finally { pool.shutdownNow(); }
    }
    @Test void medicoVeLlegadaRealYNoSeFabricanDatosAnteriores() {
        assertEquals("Sin registrar",dao.obtenerDetalleCitaMedico(7,1).get("horaLlegada"));
        admision().registrar(40,7);
        var detalle=dao.obtenerDetalleCitaMedico(7,1);
        assertEquals("10:00:00",detalle.get("horaLlegada"));assertEquals(true,detalle.get("llegoTarde"));
        assertEquals("2026-10-28T10:00",dao.obtenerAgendaMedico(1,java.time.LocalDate.of(2026,10,28)).get(0).get("fechaLlegada"));
        assertTrue(dao.obtenerDetalleCitaMedico(7,2).isEmpty());
    }
    @Test void rutaGenericaNoPermiteInventarUnaLlegada() throws Exception {
        assertFalse(dao.actualizarEstadoCita(7,"EN_SALA"));
        assertEquals(0,count("SELECT COUNT(*) FROM admision_cita"));
    }
}
