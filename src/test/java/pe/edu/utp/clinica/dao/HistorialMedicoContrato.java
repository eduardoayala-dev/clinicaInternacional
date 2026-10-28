package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.model.medico.ConsultaClinica;
import static org.junit.jupiter.api.Assertions.*;

abstract class HistorialMedicoContrato extends CitaAutorizacionTest {
    private MedicoHistorialDAO historial;
    private AtencionCitaDAO atencion;
    private static final ConsultaClinica CONSULTA = new ConsultaClinica("Consulta de prueba", "Antecedentes referidos", "Hallazgos registrados", "Evaluación de prueba", "Impresión de prueba", "Plan documentado");
    @BeforeEach void serviciosClinicos() {
        historial = new MedicoHistorialDAO(this::abrir, reloj());
        atencion = new AtencionCitaDAO(this::abrir, reloj());
    }
    private void sql(String texto) throws Exception { try(var c=abrir();var s=c.createStatement()){s.execute(texto);} }
    private void error(int status, Runnable accion) { assertEquals(status,assertThrows(AgendaException.class,accion::run).status()); }
    private void cerrar() throws Exception {
        iniciarAtencionFixture();
        atencion.finalizar(1,7,"MEDIA","2026-11-01","Indicaciones de prueba",CONSULTA);
    }
    private void control(int id, int medico, String estado) throws Exception {
        sql("INSERT INTO cita(id_cita,id_paciente,id_medico,fecha_hora,estado) VALUES("+id+",1,"+medico+",'2026-11-01 10:00:00','"+estado+"')");
    }
    @Test void reservationAloneDoesNotExposePatientHistory() {
        assertTrue(historial.pacientes(1,"",1).items().isEmpty());
        error(404,()->historial.historial(1,1,1));
    }
    @Test void ownClinicalRelationshipAllowsHistoryButAnotherDoctorCannotReadIt() throws Exception {
        cerrar();
        var pacientes=historial.pacientes(1,"12345678",1);
        assertEquals(1,pacientes.items().size());
        assertEquals(1,pacientes.items().get(0).atenciones());
        assertEquals("Impresión de prueba",historial.historial(1,1,1).atenciones().items().get(0).diagnostico());
        assertTrue(historial.pacientes(2,"",1).items().isEmpty());
        error(404,()->historial.historial(2,1,1));
    }
    @Test void clinicalClosurePersistsStructuredRecordExactlyOnce() throws Exception {
        cerrar();
        assertEquals(1,count("SELECT COUNT(*) FROM consulta_clinica WHERE id_cita=7 AND diagnostico='Impresión de prueba'"));
        assertEquals(1,count("SELECT COUNT(*) FROM historial_asistencia WHERE id_cita=7"));
        error(409,()->atencion.finalizar(1,7,"ALTA",null,"Repetida",CONSULTA));
        assertEquals(1,count("SELECT COUNT(*) FROM consulta_clinica"));
    }
    @Test void invalidClinicalRecordDoesNotCloseAttention() throws Exception {
        iniciarAtencionFixture();
        error(400,()->atencion.finalizar(1,7,"MEDIA",null,"Nota",new ConsultaClinica("", "", "", "", "", "")));
        assertEquals(1,count("SELECT COUNT(*) FROM atencion_cita WHERE fecha_fin IS NULL"));
        assertEquals(0,count("SELECT COUNT(*) FROM consulta_clinica"));
    }
    @Test void failedClinicalInsertRollsBackCloseTimeAndState() throws Exception {
        iniciarAtencionFixture();sql("DROP TABLE consulta_clinica");
        error(503,()->atencion.finalizar(1,7,"MEDIA",null,"Nota",CONSULTA));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE estado='EN_ATENCION'"));
        assertEquals(1,count("SELECT COUNT(*) FROM atencion_cita WHERE fecha_fin IS NULL"));
        assertEquals(0,count("SELECT COUNT(*) FROM historial_asistencia"));
    }
    @Test void searchEscapesWildcardsAndRejectsInvalidLimits() throws Exception {
        cerrar();assertTrue(historial.pacientes(1,"%",1).items().isEmpty());
        assertTrue(historial.pacientes(1,"' OR 1=1",1).items().isEmpty());
        error(400,()->historial.pacientes(1,"x".repeat(101),1));
        error(400,()->historial.pacientes(1,"",0));
        error(400,()->historial.seguimientos(1,"","inventado",1));
    }
    @Test void recommendationsRemainPendingUntilReservationIsExplicitlyLinked() throws Exception {
        cerrar();control(8,1,"CONFIRMADA");
        assertEquals("PENDIENTE",historial.seguimiento(1,7).estado());
        assertEquals(8,historial.controles(1,7).get(0).idCita());
        historial.vincular(1,7,8);
        assertEquals("RESERVADO",historial.seguimiento(1,7).estado());
        assertEquals(1,historial.seguimientos(1,"","RESERVADO",1).items().size());
        assertEquals(0,historial.seguimientos(1,"","PENDIENTE",1).items().size());
    }
    @Test void overdueCancelledAndCompletedControlsHaveDifferentStatuses() throws Exception {
        cerrar();sql("UPDATE cita SET fecha_recomendada_proxima='2026-10-27' WHERE id_cita=7");
        assertEquals("VENCIDO",historial.seguimiento(1,7).estado());
        control(8,1,"CONFIRMADA");historial.vincular(1,7,8);
        sql("UPDATE cita SET estado='CANCELADA' WHERE id_cita=8");
        assertEquals("VENCIDO",historial.seguimiento(1,7).estado());
        control(9,1,"ATENDIDA");historial.vincular(1,7,9);
        assertEquals("REALIZADO",historial.seguimiento(1,7).estado());
    }
    @Test void cannotLinkForeignPatientDoctorCancelledOrEarlierAppointment() throws Exception {
        cerrar();control(8,2,"CONFIRMADA");control(9,1,"CANCELADA");
        error(404,()->historial.vincular(2,7,8));
        error(404,()->historial.vincular(1,7,8));
        error(409,()->historial.vincular(1,7,9));
        error(404,()->historial.vincular(1,7,7));
        sql("INSERT INTO usuario(id_usuario,nombres) VALUES(50,'Otro paciente')");
        sql("INSERT INTO paciente(id_paciente,id_usuario) VALUES(2,50)");
        control(10,1,"CONFIRMADA");sql("UPDATE cita SET id_paciente=2 WHERE id_cita=10");
        error(404,()->historial.vincular(1,7,10));
        assertEquals(0,count("SELECT COUNT(*) FROM seguimiento_cita"));
    }
    @Test void activeLinkCannotBeSilentlyOverwrittenAndReplayIsIdempotent() throws Exception {
        cerrar();control(8,1,"CONFIRMADA");control(9,1,"CONFIRMADA");
        historial.vincular(1,7,8);historial.vincular(1,7,8);
        error(409,()->historial.vincular(1,7,9));
        assertEquals(1,count("SELECT COUNT(*) FROM seguimiento_cita WHERE id_cita_control=8"));
    }
    @Test void patientPagesUseBoundedLookaheadWithoutRepeatingRows() throws Exception {
        try(var conn=abrir();var st=conn.createStatement()){
            conn.setAutoCommit(false);
            for(int i=100;i<121;i++){
                st.addBatch("INSERT INTO usuario(id_usuario,nombres) VALUES("+i+",'Paciente "+i+"')");
                st.addBatch("INSERT INTO paciente(id_paciente,id_usuario,dni) VALUES("+i+","+i+",'99000"+i+"')");
                st.addBatch("INSERT INTO cita(id_cita,id_paciente,id_medico,fecha_hora,estado) VALUES("+i+","+i+",1,'2026-10-28 09:00:00','ATENDIDA')");
                st.addBatch("INSERT INTO atencion_cita(id_cita,id_paciente,id_medico,fecha_inicio,fecha_fin) VALUES("+i+","+i+",1,'2026-10-28 09:00:00','2026-10-28 09:15:00')");
            }
            st.executeBatch();conn.commit();
        }
        var primera=historial.pacientes(1,"",1);var segunda=historial.pacientes(1,"",2);
        assertEquals(20,primera.items().size());assertTrue(primera.tieneSiguiente());
        assertEquals(1,segunda.items().size());assertFalse(segunda.tieneSiguiente());
        assertFalse(primera.items().stream().anyMatch(p->p.idPaciente()==segunda.items().get(0).idPaciente()));
    }

    @Test void changedControlOwnershipDoesNotExposeOrConfirmAnotherDoctorsAppointment() throws Exception {
        cerrar();control(8,1,"CONFIRMADA");historial.vincular(1,7,8);
        sql("UPDATE cita SET id_medico=2 WHERE id_cita=8");
        var seguimiento=historial.seguimiento(1,7);
        assertEquals("PENDIENTE",seguimiento.estado());assertNull(seguimiento.fechaControl());
        control(9,1,"CONFIRMADA");historial.vincular(1,7,9);
        assertEquals(Integer.valueOf(9),historial.seguimiento(1,7).idControl());
    }
}
