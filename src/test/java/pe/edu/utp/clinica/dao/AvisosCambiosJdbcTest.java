package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.correo.*;
import java.time.Clock;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class AvisosCambiosJdbcTest extends CorreoCitaContrato {
    @Override protected void configurar(){url="jdbc:h2:mem:"+java.util.UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";}
    private int cita(){dao.publicar(1,1,"2026-10-28","09:00","12:00",20);return dao.reservar(1,1,"2026-10-28","09:00","MEDIA");}
    private CitaDAO paciente(){return new CitaDAO(){
        @Override protected java.sql.Connection abrirConexion()throws java.sql.SQLException{return abrir();}
        @Override protected Clock relojClinico(){return clock;}
        @Override protected boolean correosActivos(){return true;}
    };}
    private ArrayList<Correo> enviar()throws Exception {
        var lista=new ArrayList<Correo>();var servicio=new CorreosServicio(new CorreosCambiosJdbc(this::abrir),c->{lista.add(c);return "prueba";},clock);
        for(int i=0;i<10;i++){if(!servicio.procesarUno())return lista;}
        throw new AssertionError("La cola de avisos no terminó de procesarse.");
    }
    @Test void cancelacionPropiaEncolaUnSoloAvisoConEstadoReal()throws Exception {
        int id=cita();assertTrue(paciente().actualizarEstadoCitaPaciente(id,1,"CANCELADA","Imprevisto personal"));
        assertFalse(paciente().actualizarEstadoCitaPaciente(id,1,"CANCELADA","Imprevisto personal"));
        var lista=enviar();assertEquals(1,lista.size());assertTrue(lista.get(0).asunto().contains("cancelada"));
        assertEquals("paciente@example.test",lista.get(0).destinatario());assertTrue(lista.get(0).html().contains("Sede Ñúñez"));
    }
    @Test void otroPacienteNoCancelaNiGeneraAvisos()throws Exception {
        int id=cita();assertFalse(paciente().actualizarEstadoCitaPaciente(id,2,"CANCELADA","Imprevisto personal"));assertTrue(enviar().isEmpty());
    }
    @Test void unaFallaDeLaColaRevierteLaCancelacion()throws Exception {
        int id=cita();sql("DROP TABLE correo_cambio_cita");assertFalse(paciente().actualizarEstadoCitaPaciente(id,1,"CANCELADA","Imprevisto personal"));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA'"));
    }
    @Test void unaFallaDeLaColaRevierteLaReprogramacion()throws Exception {
        int id=cita();sql("DROP TABLE correo_cambio_cita");assertThrows(AgendaException.class,()->dao.reprogramar(id,1,1,"2026-10-28","09:20",null,"MEDIA"));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA'"));assertEquals(1,count("SELECT COUNT(*) FROM cita"));
    }
    @Test void reprogramacionIncluyeAmbosHorariosSinOtraConfirmacionInicial()throws Exception {
        int id=cita();assertTrue(dao.reprogramar(id,1,1,"2026-10-28","09:20",null,"MEDIA"));
        var lista=enviar();assertEquals(1,lista.size());assertTrue(lista.get(0).texto().contains("09:00"));assertTrue(lista.get(0).texto().contains("09:20"));
        assertEquals(1,count("SELECT COUNT(*) FROM correo_cita"));assertTrue(enviar().isEmpty());
    }
    @Test void cancelarLaNuevaCitaOmiteElAvisoDeReprogramacionPendiente()throws Exception {
        int id=cita();dao.reprogramar(id,1,1,"2026-10-28","09:20",null,"MEDIA");
        int nueva=count("SELECT MAX(id_cita) FROM cita");assertTrue(paciente().actualizarEstadoCitaPaciente(nueva,1,"CANCELADA","Imprevisto personal"));
        var lista=enviar();assertEquals(1,lista.size());assertTrue(lista.get(0).asunto().contains("cancelada"));
    }
    @Test void cuentaInactivaNoRecibeAvisosDeCancelacion()throws Exception {
        int id=cita();paciente().actualizarEstadoCitaPaciente(id,1,"CANCELADA","Imprevisto personal");sql("UPDATE usuario SET activo=FALSE WHERE id_usuario=1");assertTrue(enviar().isEmpty());
    }
    @Test void usaLaSedeDeLaReservaAunqueElMedicoCambieDeSede()throws Exception {
        int id=cita();paciente().actualizarEstadoCitaPaciente(id,1,"CANCELADA","Imprevisto personal");
        sql("UPDATE sede SET nombre='Otra sede' WHERE id_sede=2");sql("UPDATE medico SET id_sede=2 WHERE id_medico=1");
        var lista=enviar();assertEquals(1,lista.size());assertTrue(lista.get(0).texto().contains("Sede Ñúñez"));assertFalse(lista.get(0).texto().contains("Otra sede"));
    }
    @Test void cancelacionPorAdmisionTambienEsTransaccionalYSeNotifica()throws Exception {
        int id=cita();sql("INSERT INTO usuario(id_usuario,nombres,rol,activo) VALUES(40,'Admisión','PERSONAL_ADMINISTRATIVO',TRUE)");
        sql("CREATE TABLE personal_administrativo(id_personal INT PRIMARY KEY,id_usuario INT,id_sede INT)");
        sql("CREATE TABLE admision_cita(id_cita INT PRIMARY KEY)");
        sql("INSERT INTO personal_administrativo VALUES(1,40,1)");
        sql("CREATE TABLE IF NOT EXISTS auditoria_seguridad(id_usuario INT,evento VARCHAR(50),descripcion VARCHAR(255),sospechoso BOOLEAN)");
        var admision=new AdmisionCitaDAO(this::abrir,clock,10){@Override protected boolean correosActivos(){return true;}};
        admision.cancelar(40,id,"Imprevisto personal");assertEquals(1,enviar().size());admision.cancelar(40,id,"Imprevisto personal");assertTrue(enviar().isEmpty());
    }
}

