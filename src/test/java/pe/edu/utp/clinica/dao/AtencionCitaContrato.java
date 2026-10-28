package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

abstract class AtencionCitaContrato extends CitaAutorizacionTest {
    private AtencionCitaDAO atencion;
    private AdmisionCitaDAO admision;
    @BeforeEach void servicios() throws Exception {
        atencion=new AtencionCitaDAO(this::abrir,reloj());
        admision=new AdmisionCitaDAO(this::abrir,reloj(),10);
        sql("INSERT INTO usuario(id_usuario,nombres,rol) VALUES(40,'Admisión','PERSONAL_ADMINISTRATIVO'),(11,'Segundo paciente','PACIENTE')");
        sql("INSERT INTO personal_administrativo(id_personal,id_usuario,id_sede) VALUES(1,40,1)");
        sql("INSERT INTO paciente(id_paciente,id_usuario,dni) VALUES(2,11,'12345679')");
    }
    private void sql(String query)throws Exception {try(var c=abrir();var s=c.createStatement()){s.execute(query);}}
    private void error(int estado,Runnable accion){assertEquals(estado,assertThrows(AgendaException.class,accion::run).status());}
    private void sala() {admision.registrar(40,7);}
    private AtencionCitaDAO aLas(String instante){return new AtencionCitaDAO(this::abrir,Clock.fixed(Instant.parse(instante),ZoneId.of("UTC")));}
    private void otraCita(int paciente,int medico)throws Exception {
        sql("INSERT INTO cita(id_cita,id_paciente,id_medico,fecha_hora,estado,nivel_urgencia) VALUES(8,"+paciente+","+medico+",'2026-10-28 11:00:00','CONFIRMADA','ALTA')");
        admision.registrar(40,8);
    }

    @Test void iniciarGuardaHoraRealYPermiteAdelantarCita()throws Exception {
        sql("UPDATE cita SET fecha_hora='2026-10-28 11:00:00'");sala();
        assertTrue(atencion.iniciar(1,7));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE estado='EN_ATENCION'"));
        assertEquals(1,count("SELECT COUNT(*) FROM atencion_cita WHERE id_medico=1 AND id_paciente=1 AND fecha_inicio='2026-10-28 10:00:00' AND fecha_fin IS NULL"));
        assertEquals(7,dao.obtenerAtencionActivaMedico(1));assertEquals(-1,dao.obtenerAtencionActivaMedico(2));
    }
    @Test void noPuedeComenzarSinLlegadaRegistrada()throws Exception {
        error(409,()->atencion.iniciar(1,7));
        sql("UPDATE cita SET estado='EN_SALA'");error(409,()->atencion.iniciar(1,7));
        assertEquals(0,count("SELECT COUNT(*) FROM atencion_cita"));
    }
    @Test void iniciarNoAdmiteDiasDistintosNiEstadosTerminales()throws Exception {
        sala();sql("UPDATE cita SET fecha_hora='2026-10-29 09:00:00'");error(409,()->atencion.iniciar(1,7));
        sql("UPDATE cita SET fecha_hora='2026-10-28 09:00:00'");
        for(String estado:List.of("CANCELADA","ATENDIDA","NO_ATENDIDA","NO_ASISTIO","REPROGRAMADA")) {
            sql("UPDATE cita SET estado='"+estado+"'");error(409,()->atencion.iniciar(1,7));
        }
    }
    @Test void medicoAjenoODesactivadoNoIniciaNiCierra()throws Exception {
        sala();error(404,()->atencion.iniciar(2,7));error(404,()->atencion.noAtender(2,7,"Motivo válido"));
        error(404,()->atencion.finalizar(2,7,"MEDIA",null,"Control"));
        sql("UPDATE usuario SET activo=FALSE WHERE id_usuario=20");error(403,()->atencion.iniciar(1,7));
        assertEquals(0,count("SELECT COUNT(*) FROM atencion_cita"));
    }
    @Test void finalizarSinIniciarNoCambiaLaCita()throws Exception {
        sala();error(409,()->atencion.finalizar(1,7,"MEDIA",null,"Control"));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE estado='EN_SALA'"));
    }
    @Test void cerrarConservaTiemposDatosClinicosYLiberaConsulta()throws Exception {
        sala();atencion.iniciar(1,7);
        assertTrue(aLas("2026-10-28T15:25:00Z").finalizar(1,7,"ALTA","2026-11-15","Evolución: niño con tos. <control> & revisión"));
        var cita=dao.obtenerDetalleCitaMedico(7,1);
        assertEquals("ATENDIDA",cita.get("estado"));assertEquals(25L,cita.get("duracionMinutos"));assertEquals(0L,cita.get("esperaMinutos"));
        assertEquals("2026-11-15",cita.get("fechaRecomendadaProxima"));assertEquals(false,cita.get("editable"));
        assertEquals(-1,dao.obtenerAtencionActivaMedico(1));
        assertEquals(1,count("SELECT COUNT(*) FROM historial_asistencia WHERE asistio=TRUE"));
        error(409,()->atencion.finalizar(1,7,"BAJA",null,"Reescritura"));error(409,()->atencion.noAtender(1,7,"Reescritura"));
    }
    @Test void cierreFallidoRevierteHoraEstadoYConservaConsultaActiva()throws Exception {
        sala();atencion.iniciar(1,7);sql("DROP TABLE historial_asistencia");
        error(503,()->atencion.finalizar(1,7,"ALTA",null,"Control"));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE estado='EN_ATENCION' AND nivel_urgencia='MEDIA'"));
        assertEquals(1,count("SELECT COUNT(*) FROM atencion_cita WHERE medico_activo=1 AND fecha_fin IS NULL"));
    }
    @Test void indicacionesYMotivosNoPuedenEstarVaciosNiTenerControles()throws Exception {
        sala();atencion.iniciar(1,7);
        for(String nota:List.of("","  ","x".repeat(201),"Prueba\u0000")) error(400,()->atencion.finalizar(1,7,"MEDIA",null,nota));
        for(String motivo:List.of("","No","x".repeat(201),"Prueba\u0000")) error(400,()->atencion.noAtender(1,7,motivo));
        assertEquals(1,count("SELECT COUNT(*) FROM atencion_cita WHERE fecha_fin IS NULL"));
    }
    @Test void noAtendidaConLlegadaExigeMotivoYPreservaPrioridad()throws Exception {
        sala();assertTrue(atencion.noAtender(1,7,"Paciente solicita retirarse antes de la consulta."));
        var cita=dao.obtenerDetalleCitaMedico(7,1);
        assertEquals("NO_ATENDIDA",cita.get("estado"));assertEquals("",cita.get("fechaInicio"));assertNull(cita.get("duracionMinutos"));
        assertEquals("Paciente solicita retirarse antes de la consulta.",cita.get("motivoNoAtencion"));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE nivel_urgencia='MEDIA'"));
        assertEquals(0,count("SELECT COUNT(*) FROM historial_asistencia"));
    }
    @Test void noAtendidaDesdeConsultaActivaGuardaCierreYLiberaMedico()throws Exception {
        sala();atencion.iniciar(1,7);aLas("2026-10-28T15:05:00Z").noAtender(1,7,"Paciente derivado por necesidad de otro servicio.");
        assertEquals(-1,dao.obtenerAtencionActivaMedico(1));
        assertEquals(5L,dao.obtenerDetalleCitaMedico(7,1).get("duracionMinutos"));
    }
    @Test void unaConsultaPuedeFinalizarDespuesDeMedianoche()throws Exception {
        sql("UPDATE cita SET fecha_hora='2026-10-28 23:55:00'");
        var llegada=new AdmisionCitaDAO(this::abrir,Clock.fixed(Instant.parse("2026-10-29T04:40:00Z"),ZoneId.of("UTC")),10);
        llegada.registrar(40,7);aLas("2026-10-29T04:50:00Z").iniciar(1,7);
        aLas("2026-10-29T05:10:00Z").finalizar(1,7,"MEDIA","2026-10-30","Control al día siguiente.");
        assertEquals(20L,dao.obtenerDetalleCitaMedico(7,1).get("duracionMinutos"));
        assertEquals(10L,dao.obtenerDetalleCitaMedico(7,1).get("esperaMinutos"));
    }
    @Test void unMedicoNoPuedeIniciarDosPacientesDesdeDosPestanas()throws Exception {
        sala();otraCita(2,1);atencion.iniciar(1,7);error(409,()->atencion.iniciar(1,8));
        assertEquals(false,dao.obtenerDetalleCitaMedico(8,1).get("puedeIniciar"));
        atencion.finalizar(1,7,"MEDIA",null,"Control");assertTrue(atencion.iniciar(1,8));
    }
    @Test void unPacienteNoPuedeSerAtendidoPorDosMedicosALaVez()throws Exception {
        sala();otraCita(1,2);atencion.iniciar(1,7);error(409,()->atencion.iniciar(2,8));
        assertEquals(1,count("SELECT COUNT(*) FROM atencion_cita WHERE paciente_activo=1"));
    }
    @Test void solicitudesConcurrentesDeDistintasCitasSoloInicianUna()throws Exception {
        sala();otraCita(2,1);var salida=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            var a=pool.submit(()->iniciarConcurrente(salida,7));var b=pool.submit(()->iniciarConcurrente(salida,8));salida.countDown();
            assertEquals(1,a.get(30,TimeUnit.SECONDS)+b.get(30,TimeUnit.SECONDS));
        } finally {pool.shutdownNow();}
        assertEquals(1,count("SELECT COUNT(*) FROM atencion_cita WHERE medico_activo=1"));
    }
    private int iniciarConcurrente(CountDownLatch salida,int cita)throws Exception {
        assertTrue(salida.await(5,TimeUnit.SECONDS));
        try{atencion.iniciar(1,cita);return 1;}catch(AgendaException e){assertEquals(409,e.status());return 0;}
    }
    @Test void citaEnConsultaNoPuedeCancelarsePorLasRutasAnteriores()throws Exception {
        sala();atencion.iniciar(1,7);
        assertFalse(dao.actualizarEstadoCita(7,"CANCELADA"));assertFalse(dao.actualizarEstadoCitaPaciente(7,1,"CANCELADA"));
        assertFalse(dao.actualizarAtencionMedica(7,1,"NO_ASISTIO",null,null,null));
    }
    @Test void inasistenciaExigeHoraToleranciaAusenciaYResponsable()throws Exception {
        admision.registrarInasistencia(40,7,"Ausencia verificada en admisión y sala.");
        assertEquals(1,count("SELECT COUNT(*) FROM inasistencia_cita WHERE id_usuario_personal=40 AND tolerancia_minutos=10"));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE estado='NO_ASISTIO' AND nivel_urgencia='MEDIA'"));
        assertEquals(1,count("SELECT COUNT(*) FROM historial_asistencia WHERE asistio=FALSE"));
        error(409,()->admision.registrarInasistencia(40,7,"Repetida"));
    }
    @Test void unPacienteQueLlegoNoPuedeDeclararseAusente()throws Exception {
        sala();error(409,()->admision.registrarInasistencia(40,7,"No estuvo presente"));
        assertEquals(0,count("SELECT COUNT(*) FROM inasistencia_cita"));
    }
    @Test void noSeDeclaraAusenciaAntesDelLimiteNiEnOtroDia()throws Exception {
        var limite=new AdmisionCitaDAO(this::abrir,Clock.fixed(Instant.parse("2026-10-28T14:10:00Z"),ZoneId.of("UTC")),10);
        error(409,()->limite.registrarInasistencia(40,7,"Ausencia verificada"));
        sql("UPDATE cita SET fecha_hora='2026-10-29 09:00:00'");error(409,()->admision.registrarInasistencia(40,7,"Ausencia verificada"));
    }
    @Test void falloDeHistorialRevierteInasistenciaYRegistroOperativo()throws Exception {
        sql("DROP TABLE historial_asistencia");error(503,()->admision.registrarInasistencia(40,7,"Ausencia verificada"));
        assertEquals(0,count("SELECT COUNT(*) FROM inasistencia_cita"));assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA'"));
    }
    @Test void historialPacienteUsaResultadoRealYNoElPasoDelTiempo()throws Exception {
        assertEquals(1,dao.obtenerProximasCitasPaciente(1).size());
        assertTrue(dao.obtenerHistorialPaciente(1).isEmpty());
        sala();atencion.iniciar(1,7);
        assertEquals("EN_ATENCION",dao.obtenerProximasCitasPaciente(1).get(0).get("estado"));
        assertTrue(dao.obtenerHistorialPaciente(1).isEmpty());
        atencion.noAtender(1,7,"Derivación a otro servicio.");
        assertTrue(dao.obtenerProximasCitasPaciente(1).isEmpty());
        var registro=dao.obtenerHistorialPaciente(1).get(0);
        assertEquals("No atendida",registro.get("estadoTexto"));assertFalse(registro.containsKey("diagnostico"));
        assertEquals("Sin asignar",registro.get("consultorio"));
    }
    @Test void baseDeDatosImpideDosConsultasActivasAunqueSeOmitaElServicio()throws Exception {
        sala();otraCita(2,1);atencion.iniciar(1,7);
        assertThrows(java.sql.SQLException.class,()->sql("INSERT INTO atencion_cita(id_cita,id_medico,id_paciente,fecha_inicio) VALUES(8,1,2,'2026-10-28 10:00:00')"));
        sql("UPDATE cita SET id_medico=2,id_paciente=1 WHERE id_cita=8");
        assertThrows(java.sql.SQLException.class,()->sql("INSERT INTO atencion_cita(id_cita,id_medico,id_paciente,fecha_inicio) VALUES(8,2,1,'2026-10-28 10:00:00')"));
        assertEquals(1,count("SELECT COUNT(*) FROM atencion_cita"));
    }
    @Test void ausenciaNoPuedeRegistrarlaUnMedicoNiOtraSede()throws Exception {
        error(403,()->admision.registrarInasistencia(20,7,"Ausencia verificada"));
        sql("INSERT INTO sede(id_sede,nombre) VALUES(2,'Otra sede')");
        sql("INSERT INTO usuario(id_usuario,nombres,rol) VALUES(50,'Otra sede','PERSONAL_ADMINISTRATIVO')");
        sql("INSERT INTO personal_administrativo(id_personal,id_usuario,id_sede) VALUES(2,50,2)");
        error(404,()->admision.registrarInasistencia(50,7,"Ausencia verificada"));
        assertEquals(0,count("SELECT COUNT(*) FROM inasistencia_cita"));
    }
    @Test void cierreNoPuedeTenerHoraAnteriorAlInicio()throws Exception {
        sala();atencion.iniciar(1,7);error(409,()->aLas("2026-10-28T14:59:00Z").finalizar(1,7,"MEDIA",null,"Control"));
        assertEquals(1,count("SELECT COUNT(*) FROM atencion_cita WHERE fecha_fin IS NULL"));
    }
}
