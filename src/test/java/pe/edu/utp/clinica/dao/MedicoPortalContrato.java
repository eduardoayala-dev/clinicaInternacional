package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

abstract class MedicoPortalContrato extends CitaAutorizacionTest {
    private MedicoPortalDAO portal;
    private static final LocalDate HOY=LocalDate.of(2026,10,28);
    @BeforeEach void portal(){portal=new MedicoPortalDAO(this::abrir,reloj());}
    private void sql(String query)throws Exception{try(var conn=abrir();var st=conn.createStatement()){st.execute(query);}}
    private void error(int status,Runnable accion){assertEquals(status,assertThrows(AgendaException.class,accion::run).status());}

    @Test void emptyDayStaysEmptyInsteadOfShowingAppointmentsFromAnotherDate(){
        assertTrue(portal.agenda(1,HOY.plusDays(1)).isEmpty());
        assertTrue(portal.agenda(1,HOY.minusDays(1)).isEmpty());
    }
    @Test void agendaIsScopedToDoctorAndExactDateWithoutClinicalDetails()throws Exception{
        sql("INSERT INTO cita(id_cita,id_paciente,id_medico,fecha_hora,estado,nivel_urgencia) VALUES(8,1,2,'2026-10-28 09:00:00','CONFIRMADA','MEDIA'),(9,1,1,'2026-10-29 09:00:00','CONFIRMADA','MEDIA')");
        var agenda=portal.agenda(1,HOY);assertEquals(1,agenda.size());assertEquals(7,agenda.get(0).get("idCita"));
        assertFalse(agenda.get(0).containsKey("alergias"));assertFalse(agenda.get(0).containsKey("email"));
    }
    @Test void detailDoesNotInventAllergyContactOrFollowUp(){
        var detalle=portal.detalle(1,7);
        for(String campo:new String[]{"alergias","telefono","indicaciones","fechaRecomendadaProxima","fechaRegistro"}) assertEquals("",detalle.get(campo));
        assertEquals("Sin registrar",detalle.get("edad"));assertEquals("Central",detalle.get("sedeYConsultorio"));
    }
    @Test void rawClinicalNotesArePersistedAndReadWithoutMutatingTheirContents()throws Exception{
        String texto="Evolución: niño con tos. <control> & revisión";
        assertTrue(portal.guardar(1,7,"ATENDIDA","ALTA","2026-12-01",texto));
        var detalle=portal.detalle(1,7);assertEquals(texto,detalle.get("indicaciones"));
        assertEquals("2026-12-01",detalle.get("fechaRecomendadaProxima"));assertEquals(false,detalle.get("editable"));
        error(409,()->portal.guardar(1,7,"ATENDIDA","BAJA",null,"repetida"));
        assertEquals(1,count("SELECT COUNT(*) FROM historial_asistencia"));
    }
    @Test void futureAppointmentCannotBeAttendedOrMarkedAbsent()throws Exception{
        sql("UPDATE cita SET fecha_hora='2026-10-29 09:00:00'");
        error(409,()->portal.guardar(1,7,"ATENDIDA","MEDIA",null,"nota"));
        error(409,()->portal.guardar(1,7,"NO_ASISTIO",null,null,null));
        assertEquals(0,count("SELECT COUNT(*) FROM historial_asistencia"));
    }
    @Test void sameDayBeforeStartCannotBeClosed()throws Exception{
        sql("UPDATE cita SET fecha_hora='2026-10-28 11:00:00'");
        error(409,()->portal.guardar(1,7,"ATENDIDA","MEDIA",null,"nota"));assertEquals(false,portal.detalle(1,7).get("editable"));
    }
    @Test void pastDateCannotBeSilentlyClosedToday()throws Exception{
        sql("UPDATE cita SET fecha_hora='2026-10-27 09:00:00'");error(409,()->portal.guardar(1,7,"NO_ASISTIO",null,null,null));
    }
    @Test void cancelledReprogrammedAndAbsentAppointmentsCannotBeAttended()throws Exception{
        for(String estado:new String[]{"CANCELADA","REPROGRAMADA","NO_ASISTIO"}){
            sql("UPDATE cita SET estado='"+estado+"' WHERE id_cita=7");
            error(409,()->portal.guardar(1,7,"ATENDIDA","MEDIA",null,"nota"));
        }
    }
    @Test void rolloverDatesAndNonFutureFollowUpDatesAreRejected()throws Exception{
        for(String fecha:new String[]{"2026-02-30","2026-10-28","2026-10-27","2026-2-1","texto"}) error(400,()->portal.guardar(1,7,"ATENDIDA","MEDIA",fecha,"nota"));
        assertEquals(0,count("SELECT COUNT(*) FROM historial_asistencia"));
    }
    @Test void excessiveNotesAndControlCharactersAreRejected(){
        error(400,()->portal.guardar(1,7,"ATENDIDA","MEDIA",null,"x".repeat(201)));
        error(400,()->portal.guardar(1,7,"ATENDIDA","MEDIA",null,"nota\u0000"));
        error(400,()->portal.guardar(1,7,"ATENDIDA","URGENTE",null,"nota"));
    }
    @Test void absencePreservesClinicalPriorityAndExistingFollowUp()throws Exception{
        sql("UPDATE cita SET nivel_urgencia='ALTA',fecha_recomendada_proxima='2026-12-01'");
        portal.guardar(1,7,"NO_ASISTIO","BAJA","2027-01-01","inventada");
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE estado='NO_ASISTIO' AND nivel_urgencia='ALTA' AND fecha_recomendada_proxima='2026-12-01'"));
        assertEquals(1,count("SELECT COUNT(*) FROM historial_asistencia WHERE asistio=FALSE"));
    }
    @Test void scopedWritesRejectMissingAndForeignAppointments(){
        error(404,()->portal.guardar(2,7,"ATENDIDA","MEDIA",null,"nota"));
        error(404,()->portal.guardar(1,999,"ATENDIDA","MEDIA",null,"nota"));
        assertTrue(portal.detalle(2,7).isEmpty());
    }
    @Test void activeProfileIsReadFromDatabaseAndInactiveProfilesAreForbidden()throws Exception{
        assertEquals(1,portal.perfil(20).get("idMedico"));
        sql("UPDATE usuario SET activo=FALSE WHERE id_usuario=20");error(403,()->portal.perfil(20));
        sql("UPDATE usuario SET activo=TRUE WHERE id_usuario=20");sql("UPDATE medico SET estado='INACTIVO' WHERE id_medico=1");
        error(403,()->portal.perfil(20));
    }
    @Test void staleSessionCannotWriteAfterDoctorIsDisabled()throws Exception{
        sql("UPDATE usuario SET activo=FALSE WHERE id_usuario=20");
        error(403,()->portal.guardar(1,7,"ATENDIDA","MEDIA",null,"nota"));
        assertEquals(0,count("SELECT COUNT(*) FROM historial_asistencia"));
    }
    @Test void roleChangesAndInactiveSpecialtyBlockThePortal()throws Exception{
        sql("UPDATE usuario SET rol='PACIENTE' WHERE id_usuario=20");error(403,()->portal.perfil(20));
        sql("UPDATE usuario SET rol='MEDICO' WHERE id_usuario=20");sql("UPDATE especialidad SET estado='INACTIVO'");error(403,()->portal.perfil(20));
    }
    @Test void originComesFromAcceptedOfferInsteadOfRowPosition()throws Exception{
        assertEquals("Reserva",portal.agenda(1,HOY).get(0).get("origen"));
        sql("INSERT INTO reasignacion_inteligente(id_cita_asignada,estado) VALUES(7,'ACEPTADA')");
        assertEquals("Lista de espera",portal.agenda(1,HOY).get(0).get("origen"));
    }
    @Test void databaseFailuresAreNotReportedAsAnEmptyAgenda()throws Exception{
        sql("DROP TABLE motivo_cita");assertThrows(CatalogoDatosException.class,()->portal.agenda(1,HOY));
    }
    @Test void concurrentClosuresCreateExactlyOneHistoryRecord()throws Exception{
        var start=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
        try{
            var a=executor.submit(()->cerrar(start));var b=executor.submit(()->cerrar(start));start.countDown();
            assertEquals(1,a.get(20,TimeUnit.SECONDS)+b.get(20,TimeUnit.SECONDS));
        }finally{executor.shutdownNow();}
        assertEquals(1,count("SELECT COUNT(*) FROM historial_asistencia"));
    }
    private int cerrar(CountDownLatch start)throws Exception{
        assertTrue(start.await(5,TimeUnit.SECONDS));
        try{portal.guardar(1,7,"ATENDIDA","MEDIA",null,"control");return 1;}
        catch(AgendaException e){assertEquals(409,e.status());return 0;}
    }
    @Test void clinicalDayUsesLimaEvenWhenUtcHasAlreadyAdvanced()throws Exception{
        var noche=new MedicoPortalDAO(this::abrir,Clock.fixed(Instant.parse("2026-10-29T04:30:00Z"),ZoneId.of("America/Lima")));
        assertEquals(true,noche.detalle(1,7).get("editable"));assertEquals(1,noche.agenda(1,HOY).size());
    }
}
