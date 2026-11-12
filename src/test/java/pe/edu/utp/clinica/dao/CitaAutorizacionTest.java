package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Integración JDBC aislada: comprueba el SQL de autorización, no un DAO simulado. */
class CitaAutorizacionTest {
    @Test void cancelacionSinMotivoNoCambiaLaReserva()throws Exception {
        try(var c=abrir();var s=c.createStatement()){s.execute("UPDATE cita SET fecha_hora='2026-10-28 11:00:00' WHERE id_cita=7");}
        for(String motivo:new String[]{null,"","    ","abc","x".repeat(161),"Texto\npartido","<script>texto</script>"}) {
            var error=assertThrows(AgendaException.class,()->dao.actualizarEstadoCitaPaciente(7,1,"CANCELADA",motivo));
            assertEquals(400,error.status());
        }
        assertFalse(dao.actualizarEstadoCitaPaciente(7,1,"CANCELADA"));
        assertFalse(dao.actualizarEstadoCita(7,"CANCELADA"));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE id_cita=7 AND estado='CONFIRMADA'"));
        assertEquals(0,count("SELECT COUNT(*) FROM auditoria_seguridad"));
    }
    @Test void cancelacionConservaMotivoYActorUnaSolaVez()throws Exception {
        try(var c=abrir();var s=c.createStatement()){s.execute("UPDATE cita SET fecha_hora='2026-10-28 11:00:00' WHERE id_cita=7");}
        assertTrue(dao.actualizarEstadoCitaPaciente(7,1,"CANCELADA","Imprevisto personal"));
        assertFalse(dao.actualizarEstadoCitaPaciente(7,1,"CANCELADA","Motivo diferente"));
        assertTrue(dao.estaCanceladaPorPaciente(7,1));
        assertFalse(dao.estaCanceladaPorPaciente(7,999));
        assertFalse(dao.estaCanceladaPorPaciente(999,1));
        assertEquals(1,count("SELECT COUNT(*) FROM auditoria_seguridad WHERE id_usuario=10 AND evento='CITA_CANCELADA_PACIENTE' AND descripcion='Cita #7; motivo: Imprevisto personal'"));
        assertEquals(1,count("SELECT COUNT(*) FROM auditoria_seguridad"));
    }
    @Test void falloAlGuardarElMotivoRevierteLaCancelacion()throws Exception {
        try(var c=abrir();var s=c.createStatement()){
            s.execute("UPDATE cita SET fecha_hora='2026-10-28 11:00:00' WHERE id_cita=7");
            s.execute("DROP TABLE auditoria_seguridad");
        }
        assertFalse(dao.actualizarEstadoCitaPaciente(7,1,"CANCELADA","Imprevisto personal"));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE id_cita=7 AND estado='CONFIRMADA'"));
    }
    protected String url;
    protected CitaDAO dao;

    protected Connection abrir() throws SQLException { return DriverManager.getConnection(url); }
    protected java.time.Clock reloj() { return java.time.Clock.fixed(java.time.Instant.parse("2026-10-28T15:00:00Z"),java.time.ZoneId.of("America/Lima")); }

    @BeforeEach void setUp() throws Exception {
        url = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=MySQL";
        dao = new CitaDAO() {
            @Override protected Connection abrirConexion() throws SQLException {
                return CitaAutorizacionTest.this.abrir();
            }
            @Override protected java.time.Clock relojClinico() { return reloj(); }
        };
        try (Connection conn = abrir(); Statement st = conn.createStatement()) {
            EsquemaPruebas.limpiar(conn);
            st.execute("CREATE TABLE usuario(id_usuario INT PRIMARY KEY,nombres VARCHAR(100),apellidos VARCHAR(100),email VARCHAR(150),activo BOOLEAN DEFAULT TRUE,rol VARCHAR(30) DEFAULT 'PACIENTE')");
            String configuracion=java.nio.file.Files.readString(java.nio.file.Path.of("sql/migrations/V013__configuracion_ofertas.sql")).replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4","");
            for(String sentencia:configuracion.split(";")){if(!sentencia.isBlank())st.execute(sentencia);}
            for(String sentencia:java.nio.file.Files.readString(java.nio.file.Path.of("sql/migrations/V016__parametros_confirmacion_llegada.sql")).split(";")){if(!sentencia.isBlank())st.execute(sentencia);}
            for(String sentencia:java.nio.file.Files.readString(java.nio.file.Path.of("sql/migrations/V019__politica_contacto_recordatorio.sql")).split(";")){if(!sentencia.isBlank())st.execute(sentencia);}
            st.execute("CREATE TABLE paciente(id_paciente INT PRIMARY KEY,id_usuario INT,dni VARCHAR(15),telefono VARCHAR(20),fecha_nacimiento DATE,direccion VARCHAR(255),alergias VARCHAR(100),tipo_cobertura VARCHAR(50),canal_aviso VARCHAR(20),FOREIGN KEY(id_usuario) REFERENCES usuario(id_usuario))");
            st.execute("CREATE TABLE especialidad(id_especialidad INT PRIMARY KEY,nombre VARCHAR(100),estado VARCHAR(20) DEFAULT 'ACTIVO')");
            st.execute("CREATE TABLE sede(id_sede INT PRIMARY KEY,nombre VARCHAR(100),estado VARCHAR(20) DEFAULT 'ACTIVO')");
            st.execute("CREATE TABLE medico(id_medico INT PRIMARY KEY,id_usuario INT,cmp VARCHAR(20),id_especialidad INT,id_sede INT,estado VARCHAR(20) DEFAULT 'ACTIVO',FOREIGN KEY(id_usuario) REFERENCES usuario(id_usuario))");
            st.execute("CREATE TABLE disponibilidad(id_disponibilidad INT PRIMARY KEY,id_medico INT,id_sede INT,fecha DATE,hora_inicio TIME,hora_fin TIME,duracion_minutos INT,estado VARCHAR(20))");
            st.execute("CREATE TABLE motivo_cita(id_motivo INT PRIMARY KEY,descripcion VARCHAR(255))");
            st.execute("CREATE TABLE seguimiento_cita(id_cita_origen INT PRIMARY KEY,id_cita_control INT,id_usuario_medico INT,fecha_vinculacion DATETIME)");
            st.execute("CREATE TABLE retencion_cupo(id_paciente INT PRIMARY KEY,id_medico INT,inicio DATETIME,duracion_minutos INT,identificador VARCHAR(36) UNIQUE,vence DATETIME,UNIQUE(id_medico,inicio))");
            st.execute("CREATE TABLE cita(id_cita INT PRIMARY KEY,id_paciente INT,id_medico INT,id_disponibilidad INT,id_motivo INT,fecha_hora DATETIME,estado VARCHAR(20),nivel_urgencia VARCHAR(20),riesgo_cancelacion VARCHAR(20),fecha_recomendada_proxima DATE,fecha_creacion DATETIME,FOREIGN KEY(id_paciente) REFERENCES paciente(id_paciente),FOREIGN KEY(id_medico) REFERENCES medico(id_medico))");
            st.execute("CREATE TABLE confirmacion_asistencia(id_cita INT PRIMARY KEY,creada_en DATETIME,vence_en DATETIME,confirmada_en DATETIME,canal VARCHAR(16),id_usuario_confirmo INT,FOREIGN KEY(id_cita) REFERENCES cita(id_cita),FOREIGN KEY(id_usuario_confirmo) REFERENCES usuario(id_usuario))");
            st.execute("CREATE TABLE contacto_admision_cita(id_contacto BIGINT AUTO_INCREMENT PRIMARY KEY,id_cita INT,id_usuario_personal INT,fecha_contacto DATETIME,canal VARCHAR(16),resultado VARCHAR(24),FOREIGN KEY(id_cita) REFERENCES cita(id_cita),FOREIGN KEY(id_usuario_personal) REFERENCES usuario(id_usuario))");
            st.execute("CREATE TABLE personal_administrativo(id_personal INT PRIMARY KEY,id_usuario INT,id_sede INT,FOREIGN KEY(id_usuario) REFERENCES usuario(id_usuario))");
            st.execute("CREATE TABLE admision_cita(id_cita INT PRIMARY KEY,id_usuario_personal INT NOT NULL,fecha_llegada DATETIME NOT NULL,tolerancia_minutos SMALLINT NOT NULL,llego_tarde BOOLEAN NOT NULL,FOREIGN KEY(id_cita) REFERENCES cita(id_cita),FOREIGN KEY(id_usuario_personal) REFERENCES usuario(id_usuario))");
            String migracion = java.nio.file.Files.readString(java.nio.file.Path.of("sql/migrations/V004__flujo_atencion.sql"));
            for (String sentencia : migracion.split(";")) {
                if (sentencia.strip().startsWith("CREATE TABLE")) st.execute(sentencia.replace(") ENGINE=InnoDB", ")"));
            }
            st.execute("ALTER TABLE atencion_cita ADD COLUMN id_especialidad_prioridad INT");
            String clinica = java.nio.file.Files.readString(java.nio.file.Path.of("sql/migrations/V005__historial_seguimientos_clinicos.sql"));
            for (String sentencia : clinica.split(";")) {
                if (sentencia.strip().startsWith("CREATE TABLE")) st.execute(sentencia.replace(") ENGINE=InnoDB", ")"));
            }
            st.execute("CREATE TABLE auditoria_seguridad(id_auditoria INT AUTO_INCREMENT PRIMARY KEY,id_usuario INT,evento VARCHAR(80),descripcion VARCHAR(255),sospechoso BOOLEAN)");
            st.execute("CREATE TABLE historial_asistencia(id_historial INT AUTO_INCREMENT PRIMARY KEY,id_paciente INT,id_cita INT,asistio BOOLEAN,observacion VARCHAR(255),fecha_registro DATETIME,FOREIGN KEY(id_cita) REFERENCES cita(id_cita),FOREIGN KEY(id_paciente) REFERENCES paciente(id_paciente))");
            st.execute("CREATE TABLE reasignacion_inteligente(id_reasignacion INT AUTO_INCREMENT PRIMARY KEY,id_cita_asignada INT,estado VARCHAR(20),FOREIGN KEY(id_cita_asignada) REFERENCES cita(id_cita))");
            st.execute("INSERT INTO usuario(id_usuario,nombres,apellidos,email,rol) VALUES(10,'Paciente','Uno','p@test','PACIENTE'),(20,'Medico','Uno','m@test','MEDICO'),(30,'Medico','Dos','m2@test','MEDICO')");
            st.execute("INSERT INTO paciente(id_paciente,id_usuario,dni) VALUES(1,10,'12345678')");
            st.execute("INSERT INTO medico(id_medico,id_usuario,cmp,id_especialidad,id_sede) VALUES(1,20,'1',1,1),(2,30,'2',1,1)");
            st.execute("INSERT INTO especialidad(id_especialidad,nombre) VALUES(1,'General')");
            st.execute("INSERT INTO sede(id_sede,nombre) VALUES(1,'Central')");
            st.execute("INSERT INTO cita(id_cita,id_paciente,id_medico,fecha_hora,estado,nivel_urgencia) VALUES(7,1,1,'2026-10-28 09:00:00','CONFIRMADA','MEDIA')");
            st.execute("INSERT INTO confirmacion_asistencia(id_cita,creada_en,vence_en) VALUES(7,'2026-10-28 10:00:00','2026-10-29 09:00:00')");
        }
    }

    protected int count(String query) throws Exception {
        try (Connection c = abrir(); Statement s = c.createStatement(); var rs = s.executeQuery(query)) {
            rs.next(); return rs.getInt(1);
        }
    }

    protected void iniciarAtencionFixture() throws Exception {
        try (Connection c = abrir(); Statement st = c.createStatement()) {
            st.execute("INSERT INTO usuario(id_usuario,nombres,rol) VALUES(99,'Admisión de prueba','PERSONAL_ADMINISTRATIVO')");
            st.execute("INSERT INTO personal_administrativo(id_personal,id_usuario,id_sede) VALUES(99,99,1)");
            st.execute("INSERT INTO admision_cita(id_cita,id_usuario_personal,fecha_llegada,tolerancia_minutos,llego_tarde) VALUES(7,99,'2026-10-28 08:55:00',10,FALSE)");
            st.execute("UPDATE cita SET estado='EN_SALA' WHERE id_cita=7");
        }
        assertTrue(dao.iniciarAtencionMedica(7,1));
    }

    @Test void patientCannotCancelAnotherPatientsAppointment() throws Exception {
        assertFalse(dao.actualizarEstadoCitaPaciente(7,2,"CANCELADA","Imprevisto personal"));
        assertEquals(1, count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA'"));
    }

    @Test void ownerCanCancelOnceAndCannotReactivateCancelledAppointment() throws Exception {
        try(var c=abrir();var s=c.createStatement()){s.execute("UPDATE cita SET fecha_hora='2026-10-28 11:00:00' WHERE id_cita=7");}
        assertTrue(dao.actualizarEstadoCitaPaciente(7,1,"CANCELADA","Imprevisto personal"));
        assertFalse(dao.actualizarEstadoCitaPaciente(7,1,"CANCELADA","Imprevisto personal"));
        assertFalse(dao.actualizarEstadoCitaPaciente(7, 1, "CONFIRMADA"));
    }

    @Test void rejectsInvalidOwnerAndClinicalStateFromPatient() {
        assertFalse(dao.actualizarEstadoCitaPaciente(7,-1,"CANCELADA","Imprevisto personal"));
        assertFalse(dao.actualizarEstadoCitaPaciente(7, 1, "ATENDIDA"));
    }
    @Test void patientCannotCancelAnAppointmentAlreadyStartedToday()throws Exception {
        assertFalse(dao.actualizarEstadoCitaPaciente(7,1,"CANCELADA","Imprevisto personal"));
        try(var c=abrir();var s=c.createStatement()){s.execute("UPDATE cita SET fecha_hora='2026-10-28 10:00:00' WHERE id_cita=7");}
        assertFalse(dao.actualizarEstadoCitaPaciente(7,1,"CANCELADA","Imprevisto personal"));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE id_cita=7 AND estado='CONFIRMADA'"));
    }

    @Test void doctorCannotReadOtherDoctorsAppointment() {
        assertTrue(dao.obtenerDetalleCitaMedico(7, 2).isEmpty());
    }

    @Test void assignedDoctorCanReadAppointment() {
        assertEquals(7, dao.obtenerDetalleCitaMedico(7, 1).get("idCita"));
    }

    @Test void doctorCannotUpdateOtherDoctorsAppointmentOrHistory() throws Exception {
        assertFalse(dao.actualizarAtencionMedica(7, 2, "ATENDIDA", "MEDIA", null, "prueba"));
        assertEquals(0, count("SELECT COUNT(*) FROM historial_asistencia"));
        assertEquals(1, count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA'"));
    }

    @Test void assignedDoctorUpdatesAppointmentAndHistoryOnce() throws Exception {
        iniciarAtencionFixture();
        assertTrue(dao.actualizarAtencionMedica(7, 1, "ATENDIDA", "ALTA", "2026-12-01", "control"));
        assertFalse(dao.actualizarAtencionMedica(7, 1, "ATENDIDA", "ALTA", null, "repetido"));
        assertEquals(1, count("SELECT COUNT(*) FROM historial_asistencia WHERE asistio=TRUE"));
        assertEquals(1, count("SELECT COUNT(*) FROM cita WHERE estado='ATENDIDA' AND nivel_urgencia='ALTA'"));
    }

    @Test void historyFailureRollsBackAppointment() throws Exception {
        iniciarAtencionFixture();
        try (Connection c = abrir(); Statement s = c.createStatement()) {
            s.execute("DROP TABLE historial_asistencia");
        }
        assertFalse(dao.actualizarAtencionMedica(7, 1, "ATENDIDA", "ALTA", null, "control"));
        assertEquals(1, count("SELECT COUNT(*) FROM cita WHERE estado='EN_ATENCION' AND nivel_urgencia='MEDIA'"));
    }

    @Test void invalidPriorityOrDateDoesNotModifyAppointment() throws Exception {
        assertFalse(dao.actualizarAtencionMedica(7, 1, "ATENDIDA", "URGENTISIMA", null, "control"));
        assertFalse(dao.actualizarAtencionMedica(7, 1, "ATENDIDA", "ALTA", "invalida", "control"));
        assertEquals(1, count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA'"));
    }

    @Test void missingIdentitiesNeverResolveToOneOrCreatePatient() throws Exception {
        int pacientesAntes = count("SELECT COUNT(*) FROM paciente");
        assertEquals(-1, dao.obtenerIdMedicoPorUsuarioId(999));
        assertEquals(-1, dao.obtenerIdUsuarioPorPacienteId(999));
        assertEquals(-1, dao.obtenerIdPacientePorUsuarioId(20));
        assertEquals(pacientesAntes, count("SELECT COUNT(*) FROM paciente"));
    }

    @Test void reservationWithMissingPatientNeverFallsBackToPatientOne() throws Exception {
        assertEquals(-1, dao.reservarCitaConId(-1, 1, "2026-12-01", "10:00", "BAJA"));
        assertEquals(-1, dao.reservarCitaConId(999, 1, "2026-12-01", "10:00", "BAJA"));
        assertEquals(1, count("SELECT COUNT(*) FROM cita"));
    }
}
