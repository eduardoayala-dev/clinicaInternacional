package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

abstract class AgendaCitaContrato {
    protected String url;
    protected String user = "sa";
    protected String password = "";
    protected AgendaCitaDAO dao;
    protected final Clock clock = Clock.fixed(Instant.parse("2026-10-27T15:10:00Z"), ZoneId.of("America/Lima"));
    protected abstract void configurar();
    protected Connection abrir() throws SQLException { return url.startsWith("jdbc:mysql:") ? MySqlPruebas.abrir() : DriverManager.getConnection(url, user, password); }

    @BeforeEach void preparar() throws Exception {
        configurar();
        try (var conn = abrir()) {
            String catalogo = conn.getCatalog();
            if (url.startsWith("jdbc:mysql:")) assertTrue(catalogo.startsWith("clinica_agendas_test_"));
        }
        try (var conn=abrir()) { EsquemaPruebas.limpiar(conn); }
        sql("CREATE TABLE usuario(id_usuario INT PRIMARY KEY,nombres VARCHAR(100),apellidos VARCHAR(100),rol VARCHAR(30),activo BOOLEAN)");
        sql("CREATE TABLE paciente(id_paciente INT PRIMARY KEY,id_usuario INT)");
        sql("CREATE TABLE especialidad(id_especialidad INT PRIMARY KEY,estado VARCHAR(20))");
        sql("CREATE TABLE sede(id_sede INT PRIMARY KEY,estado VARCHAR(20))");
        sql("CREATE TABLE medico(id_medico INT PRIMARY KEY,id_usuario INT,id_sede INT,id_especialidad INT,estado VARCHAR(20))");
        sql("CREATE TABLE motivo_cita(id_motivo INT PRIMARY KEY,id_especialidad INT,estado VARCHAR(20))");
        sql("CREATE TABLE disponibilidad(id_disponibilidad INT AUTO_INCREMENT PRIMARY KEY,id_medico INT,id_sede INT,fecha DATE,hora_inicio TIME,hora_fin TIME,duracion_minutos INT,estado VARCHAR(20))");
        sql("CREATE TABLE cita(id_cita INT AUTO_INCREMENT PRIMARY KEY,id_paciente INT,id_medico INT,id_disponibilidad INT,id_motivo INT,fecha_hora TIMESTAMP,estado VARCHAR(20),nivel_urgencia VARCHAR(20),riesgo_cancelacion VARCHAR(20))");
        sql("CREATE TABLE notificacion(id_notificacion INT AUTO_INCREMENT PRIMARY KEY,id_usuario INT,id_cita INT,tipo VARCHAR(30),canal VARCHAR(30),mensaje VARCHAR(300),estado_envio VARCHAR(30),leida INT,fecha_envio DATETIME)");
        sql("CREATE TABLE lista_espera(id_lista_espera INT AUTO_INCREMENT PRIMARY KEY,id_paciente INT,id_especialidad INT,id_sede INT,nivel_urgencia VARCHAR(20),rango_horario VARCHAR(20),canal_notificacion VARCHAR(40),fecha_registro DATETIME,estado VARCHAR(20),posicion INT,anticipacion_minutos INT DEFAULT 180,id_medico_preferido INT)");
        sql("CREATE TABLE reasignacion_inteligente(id_reasignacion INT AUTO_INCREMENT PRIMARY KEY,id_cita_original INT,id_lista_espera INT,id_disponibilidad INT,fecha_oferta DATETIME,fecha_limite_respuesta DATETIME,estado VARCHAR(20),id_cita_asignada INT)");
        sql("INSERT INTO usuario VALUES(1,'Paciente','Ñúñez','PACIENTE',TRUE),(2,'Otro','Pérez','PACIENTE',TRUE),(3,'Médico','Prueba','MEDICO',TRUE),(4,'Segundo','Médico','MEDICO',TRUE)");
        sql("INSERT INTO paciente VALUES(1,1),(2,2)");
        sql("INSERT INTO especialidad VALUES(1,'ACTIVO'),(2,'INACTIVO')");
        sql("INSERT INTO sede VALUES(1,'ACTIVO'),(2,'ACTIVO')");
        sql("INSERT INTO medico VALUES(1,3,1,1,'ACTIVO'),(2,4,1,1,'ACTIVO')");
        sql("INSERT INTO motivo_cita VALUES(1,1,'ACTIVO'),(2,2,'ACTIVO'),(3,1,'INACTIVO')");
        dao = new AgendaCitaDAO(this::abrir, clock);
    }

    protected void sql(String sql) throws Exception {
        try (var conn = abrir(); var st = conn.createStatement()) { st.execute(sql); }
    }
    protected int count(String sql) throws Exception {
        try (var conn = abrir(); var st = conn.createStatement(); var rs = st.executeQuery(sql)) { rs.next(); return rs.getInt(1); }
    }
    private int publicar() { return dao.publicar(1, 1, "2026-12-01", "09:00", "11:00", 30); }
    private int reservar(int paciente, String hora) { return dao.reservar(paciente, 1, "2026-12-01", hora, "MEDIA"); }

    @Test void publicationAndReservationUseActualBlockAndCreateNotification() throws Exception {
        int bloque = publicar(); int cita = reservar(1,"09:00");
        assertTrue(cita > 0);
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE id_disponibilidad="+bloque+" AND estado='CONFIRMADA'"));
        assertEquals(1,count("SELECT COUNT(*) FROM notificacion WHERE id_usuario=1 AND id_cita="+cita));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE fecha_hora='2026-12-01 09:00:00'"));
    }

    @Test void neverInventsAnAgendaWhenThereAreNoPublishedSlots() throws Exception {
        assertEquals(409,assertThrows(AgendaException.class,()->reservar(1,"09:00")).status());
        assertEquals(0,count("SELECT COUNT(*) FROM disponibilidad"));
        assertEquals(0,count("SELECT COUNT(*) FROM cita"));
    }

    @Test void rejectsOutsideMisalignedAndIncompleteSlots() throws Exception {
        publicar();
        for(String hora:List.of("08:30","09:15","11:00","10:45")) assertThrows(AgendaException.class,()->reservar(1,hora));
        assertEquals(0,count("SELECT COUNT(*) FROM cita"));
    }

    @Test void rejectsPastInvalidDatesSecondsAndUrgency() throws Exception {
        publicar();
        for(String fecha:List.of("2026-10-26","2026-02-30","2026-2-03","invalida")) assertThrows(AgendaException.class,()->dao.reservar(1,1,fecha,"09:00","BAJA"));
        assertThrows(AgendaException.class,()->reservar(1,"09:00:01"));
        assertThrows(AgendaException.class,()->dao.reservar(1,1,"2026-12-01","09:00","URGENTISIMA"));
        assertThrows(AgendaException.class,()->dao.reservar(1,1,null,null,"BAJA"));
        assertEquals(0,count("SELECT COUNT(*) FROM cita"));
    }

    @Test void rejectsWrongClinicOverlappingAndInvalidPublications() throws Exception {
        assertThrows(AgendaException.class,()->dao.publicar(1,2,"2026-12-01","09:00","11:00",30));
        assertThrows(AgendaException.class,()->dao.publicar(1,1,"2026-12-01","11:00","09:00",30));
        assertThrows(AgendaException.class,()->dao.publicar(1,1,"2026-12-01","09:00","09:10",30));
        assertThrows(AgendaException.class,()->dao.publicar(1,1,"2026-12-01","09:00","11:00",0));
        publicar();
        assertThrows(AgendaException.class,()->dao.publicar(1,1,"2026-12-01","10:30","12:00",30));
        assertTrue(dao.publicar(1,1,"2026-12-01","11:00","12:00",30)>0);
        assertEquals(2,count("SELECT COUNT(*) FROM disponibilidad"));
    }

    @Test void inactivePatientsAndDoctorsCannotReserve() throws Exception {
        publicar();
        sql("UPDATE usuario SET activo=FALSE WHERE id_usuario=1");
        assertThrows(AgendaException.class,()->reservar(1,"09:00"));
        sql("UPDATE usuario SET activo=TRUE WHERE id_usuario=1");
        sql("UPDATE medico SET estado='INACTIVO' WHERE id_medico=1");
        assertThrows(AgendaException.class,()->reservar(1,"09:00"));
        assertEquals(0,count("SELECT COUNT(*) FROM cita"));
    }

    @Test void cancelledOrDifferentClinicAgendaCannotBeBooked() throws Exception {
        int bloque=publicar(); dao.cambiarEstado(bloque,"CANCELADO");
        assertThrows(AgendaException.class,()->reservar(1,"09:00"));
        sql("UPDATE disponibilidad SET estado='PUBLICADO',id_sede=2");
        assertThrows(AgendaException.class,()->reservar(1,"09:00"));
        assertEquals(0,count("SELECT COUNT(*) FROM cita"));
    }

    @Test void onlyOneOfTwoConcurrentReservationsCanWin() throws Exception {
        publicar();
        CountDownLatch start=new CountDownLatch(1);
        var executor=Executors.newFixedThreadPool(2);
        try {
            var a=executor.submit(()->intentarReserva(start,1));
            var b=executor.submit(()->intentarReserva(start,2));
            start.countDown();
            assertEquals(1,a.get(15,TimeUnit.SECONDS)+b.get(15,TimeUnit.SECONDS));
        } finally { executor.shutdownNow(); }
        assertEquals(1,count("SELECT COUNT(*) FROM cita"));
        assertEquals(1,count("SELECT COUNT(*) FROM notificacion"));
    }

    private int intentarReserva(CountDownLatch start,int paciente) throws Exception {
        assertTrue(start.await(5,TimeUnit.SECONDS));
        try { reservar(paciente,"09:00"); return 1; }
        catch(AgendaException e) { assertEquals(409,e.status()); return 0; }
    }

    @Test void cancellationReleasesSlotAndCannotBeRepeated() {
        publicar(); int id=reservar(1,"09:00");
        CitaDAO citas = new CitaDAO(){ @Override protected Connection abrirConexion() throws SQLException { return abrir(); } };
        assertFalse(citas.actualizarEstadoCitaPaciente(id,2,"CANCELADA"));
        assertTrue(citas.actualizarEstadoCitaPaciente(id,1,"CANCELADA"));
        assertFalse(citas.actualizarEstadoCitaPaciente(id,1,"CANCELADA"));
        assertTrue(reservar(2,"09:00")>id);
    }

    @Test void reprogrammingMovesAppointmentAndReleasesOriginalSlot() throws Exception {
        publicar(); int id=reservar(1,"09:00");
        assertTrue(dao.reprogramar(id,1,1,"2026-12-01","09:30",1,"ALTA"));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE id_cita="+id+" AND estado='REPROGRAMADA'"));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA' AND id_motivo=1"));
        assertTrue(reservar(2,"09:00")>id);
    }

    @Test void occupiedReprogrammingDoesNotChangeOriginal() throws Exception {
        publicar(); int id=reservar(1,"09:00"); reservar(2,"09:30");
        assertThrows(AgendaException.class,()->dao.reprogramar(id,1,1,"2026-12-01","09:30",null,"BAJA"));
        assertEquals(2,count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA'"));
        assertEquals(2,count("SELECT COUNT(*) FROM notificacion"));
    }

    @Test void reprogrammingRequiresOwnerValidStateAndMatchingReason() throws Exception {
        publicar(); int id=reservar(1,"09:00");
        assertEquals(404,assertThrows(AgendaException.class,()->dao.reprogramar(id,2,1,"2026-12-01","09:30",null,"BAJA")).status());
        for(int motivo:List.of(0,2,3,999)) assertThrows(AgendaException.class,()->dao.reprogramar(id,1,1,"2026-12-01","09:30",motivo,"BAJA"));
        sql("UPDATE cita SET estado='ATENDIDA'");
        assertThrows(AgendaException.class,()->dao.reprogramar(id,1,1,"2026-12-01","09:30",null,"BAJA"));
        assertEquals(1,count("SELECT COUNT(*) FROM cita"));
    }

    @Test void notificationFailureRollsBackReservationAndReprogramming() throws Exception {
        publicar(); int id=reservar(1,"09:00");
        sql("DROP TABLE notificacion");
        assertEquals(503,assertThrows(AgendaException.class,()->reservar(2,"10:00")).status());
        assertEquals(503,assertThrows(AgendaException.class,()->dao.reprogramar(id,1,1,"2026-12-01","09:30",null,"BAJA")).status());
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA'"));
        assertEquals(1,count("SELECT COUNT(*) FROM cita"));
    }

    @Test void blocksWithActiveAppointmentsCannotBeWithdrawn() throws Exception {
        int bloque=publicar(); int cita=reservar(1,"09:00");
        assertEquals("TURNO_CON_CITAS",assertThrows(AgendaException.class,()->dao.cambiarEstado(bloque,"CANCELADO")).code());
        sql("UPDATE cita SET estado='CANCELADA' WHERE id_cita="+cita);
        dao.cambiarEstado(bloque,"CANCELADO");
        assertEquals(1,count("SELECT COUNT(*) FROM disponibilidad WHERE estado='CANCELADO'"));
    }

    @Test void publishingExistingBlockValidatesOverlapsAndStates() throws Exception {
        int bloque=publicar(); dao.cambiarEstado(bloque,"CANCELADO");
        publicar();
        assertThrows(AgendaException.class,()->dao.cambiarEstado(bloque,"PUBLICADO"));
        assertThrows(AgendaException.class,()->dao.cambiarEstado(bloque,"DESCONOCIDO"));
        assertThrows(AgendaException.class,()->dao.cambiarEstado(999,"PUBLICADO"));
        assertEquals(1,count("SELECT COUNT(*) FROM disponibilidad WHERE estado='PUBLICADO'"));
    }

    @Test void oldOverlappingBlocksStillCannotCreateOverlappingAppointments() throws Exception {
        publicar(); reservar(1,"09:00");
        sql("INSERT INTO disponibilidad(id_medico,id_sede,fecha,hora_inicio,hora_fin,duracion_minutos,estado) VALUES(1,1,'2026-12-01','09:15','10:15',30,'PUBLICADO')");
        assertThrows(AgendaException.class,()->reservar(2,"09:15"));
        assertEquals(1,count("SELECT COUNT(*) FROM cita"));
    }

    @Test void administrativeStateChangeCannotReactivateCancelledOrReprogrammedAppointments() throws Exception {
        publicar(); int id=reservar(1,"09:00");
        CitaDAO citas = new CitaDAO(){ @Override protected Connection abrirConexion() throws SQLException { return abrir(); } };
        assertTrue(citas.actualizarEstadoCita(id,"CANCELADA"));
        reservar(2,"09:00");
        assertFalse(citas.actualizarEstadoCita(id,"CONFIRMADA"));
        assertFalse(citas.actualizarEstadoCita(id,"EN_SALA"));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA'"));
    }

    @Test void concurrentReprogrammingAndReservationCannotTakeTheSameSlot() throws Exception {
        publicar(); int anterior=reservar(1,"09:00");
        CountDownLatch start=new CountDownLatch(1);
        var executor=Executors.newFixedThreadPool(2);
        try {
            var reprogramar=executor.submit(()->{
                assertTrue(start.await(5,TimeUnit.SECONDS));
                try { dao.reprogramar(anterior,1,1,"2026-12-01","09:30",null,"BAJA"); return 1; }
                catch(AgendaException e) { assertEquals(409,e.status()); return 0; }
            });
            var reservar=executor.submit(()->{
                assertTrue(start.await(5,TimeUnit.SECONDS));
                try { reservar(2,"09:30"); return 1; }
                catch(AgendaException e) { assertEquals(409,e.status()); return 0; }
            });
            start.countDown();
            assertEquals(1,reprogramar.get(15,TimeUnit.SECONDS)+reservar.get(15,TimeUnit.SECONDS));
        } finally { executor.shutdownNow(); }
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE fecha_hora='2026-12-01 09:30:00' AND estado='CONFIRMADA'"));
        assertEquals(2,count("SELECT COUNT(*) FROM cita"));
    }
}
