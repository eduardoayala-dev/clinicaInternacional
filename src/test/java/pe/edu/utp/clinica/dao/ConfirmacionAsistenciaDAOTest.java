package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.*;

class ConfirmacionAsistenciaDAOTest extends CitaAutorizacionTest {
    private ConfirmacionAsistenciaDAO confirmaciones() { return new ConfirmacionAsistenciaDAO(this::abrir, reloj()); }

    @Test void deadlineIsExpiredAtTheExactBoundaryWithoutCancellingTheAppointment() throws Exception {
        var daoConfirmacion = confirmaciones();
        try (Connection c = abrir(); Statement s = c.createStatement()) {
            s.execute("UPDATE cita SET fecha_hora='2026-10-30 09:00:00' WHERE id_cita=7");
            s.execute("UPDATE confirmacion_asistencia SET vence_en='2026-10-28 10:00:00' WHERE id_cita=7");
        }
        var estado = daoConfirmacion.estadoDelPaciente(10, java.util.List.of(7)).get(7);
        assertTrue(estado.vencida());
        assertEquals("PLAZO_CONFIRMACION_VENCIDO", assertThrows(AgendaException.class,
                () -> daoConfirmacion.confirmarPaciente(10, 7)).code());
        assertEquals(0, count("SELECT COUNT(*) FROM confirmacion_asistencia WHERE confirmada_en IS NOT NULL"));
        assertEquals(1, count("SELECT COUNT(*) FROM cita WHERE id_cita=7 AND estado='CONFIRMADA'"));
    }

    @Test void patientConfirmationIsSeparateFromAppointmentStatusAndIdempotent() throws Exception {
        var daoConfirmacion = confirmaciones();
        try (Connection c=abrir(); Statement s=c.createStatement()) { s.execute("UPDATE cita SET fecha_hora='2026-10-30 09:00:00' WHERE id_cita=7"); }
        daoConfirmacion.confirmarPaciente(10,7);
        daoConfirmacion.confirmarPaciente(10,7);
        assertEquals(1,count("SELECT COUNT(*) FROM confirmacion_asistencia WHERE id_cita=7 AND confirmada_en IS NOT NULL AND canal='WEB' AND id_usuario_confirmo=10"));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE id_cita=7 AND estado='CONFIRMADA'"));
    }

    @Test void patientCannotConfirmAnotherPatientsOrExpiredAppointment() throws Exception {
        var daoConfirmacion = confirmaciones();
        try (Connection c=abrir(); Statement s=c.createStatement()) {
            s.execute("INSERT INTO usuario(id_usuario,nombres,apellidos,email,rol) VALUES(11,'Otra','Paciente','otro@test','PACIENTE')");
            s.execute("INSERT INTO paciente(id_paciente,id_usuario,dni) VALUES(2,11,'87654321')");
            s.execute("UPDATE cita SET fecha_hora='2026-10-30 09:00:00' WHERE id_cita=7");
            s.execute("UPDATE confirmacion_asistencia SET vence_en='2026-10-28 09:59:00' WHERE id_cita=7");
        }
        assertEquals(409,assertThrows(AgendaException.class,()->daoConfirmacion.confirmarPaciente(11,7)).status());
        assertEquals("PLAZO_CONFIRMACION_VENCIDO",assertThrows(AgendaException.class,()->daoConfirmacion.confirmarPaciente(10,7)).code());
        assertEquals(0,count("SELECT COUNT(*) FROM confirmacion_asistencia WHERE confirmada_en IS NOT NULL"));
    }

    @Test void admissionFollowupRecordsAttemptsAndOnlyExplicitResponseConfirms() throws Exception {
        var daoConfirmacion = confirmaciones();
        try (Connection c=abrir(); Statement s=c.createStatement()) {
            s.execute("INSERT INTO usuario(id_usuario,nombres,apellidos,email,rol) VALUES(60,'Personal','Prueba','pa@test','PERSONAL_ADMINISTRATIVO')");
            s.execute("INSERT INTO personal_administrativo(id_personal,id_usuario,id_sede) VALUES(60,60,1)");
            s.execute("UPDATE cita SET fecha_hora='2026-10-30 09:00:00' WHERE id_cita=7");
        }
        daoConfirmacion.registrarContacto(60,7,"LLAMADA","SIN_RESPUESTA");
        assertEquals(1,count("SELECT COUNT(*) FROM contacto_admision_cita WHERE id_cita=7 AND resultado='SIN_RESPUESTA'"));
        assertEquals(0,count("SELECT COUNT(*) FROM confirmacion_asistencia WHERE confirmada_en IS NOT NULL"));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE id_cita=7 AND estado='CONFIRMADA'"));
        var listado=daoConfirmacion.listarPendientes(60);
        assertEquals(1,listado.size()); assertEquals(1,listado.get(0).intentos());
        daoConfirmacion.registrarContacto(60,7,"CORREO","CONFIRMA");
        assertEquals(1,count("SELECT COUNT(*) FROM confirmacion_asistencia WHERE confirmada_en IS NOT NULL AND canal='ADMISION' AND id_usuario_confirmo=60"));
        assertTrue(daoConfirmacion.listarPendientes(60).isEmpty());
    }

    @Test void admissionCannotSeeOrRecordAppointmentsFromAnotherClinic() throws Exception {
        var daoConfirmacion = confirmaciones();
        try (Connection c=abrir(); Statement s=c.createStatement()) {
            s.execute("INSERT INTO sede(id_sede,nombre,estado) VALUES(2,'Otra','ACTIVO')");
            s.execute("INSERT INTO usuario(id_usuario,nombres,apellidos,email,rol) VALUES(70,'Personal','Otra sede','pa2@test','PERSONAL_ADMINISTRATIVO')");
            s.execute("INSERT INTO personal_administrativo(id_personal,id_usuario,id_sede) VALUES(70,70,2)");
            s.execute("UPDATE cita SET fecha_hora='2026-10-30 09:00:00' WHERE id_cita=7");
        }
        assertTrue(daoConfirmacion.listarPendientes(70).isEmpty());
        assertEquals(409,assertThrows(AgendaException.class,()->daoConfirmacion.registrarContacto(70,7,"LLAMADA","SIN_RESPUESTA")).status());
        assertEquals(0,count("SELECT COUNT(*) FROM contacto_admision_cita"));
    }
}
