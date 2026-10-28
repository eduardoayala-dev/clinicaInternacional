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
            st.execute("DROP TABLE IF EXISTS reasignacion_inteligente");
            st.execute("DROP TABLE IF EXISTS historial_asistencia");
            st.execute("DROP TABLE IF EXISTS cita");
            st.execute("DROP TABLE IF EXISTS motivo_cita");
            st.execute("DROP TABLE IF EXISTS disponibilidad");
            st.execute("DROP TABLE IF EXISTS medico");
            st.execute("DROP TABLE IF EXISTS paciente");
            st.execute("DROP TABLE IF EXISTS sede");
            st.execute("DROP TABLE IF EXISTS especialidad");
            st.execute("DROP TABLE IF EXISTS usuario");
            st.execute("CREATE TABLE usuario(id_usuario INT PRIMARY KEY,nombres VARCHAR(100),apellidos VARCHAR(100),email VARCHAR(150),activo BOOLEAN DEFAULT TRUE,rol VARCHAR(30) DEFAULT 'PACIENTE')");
            st.execute("CREATE TABLE paciente(id_paciente INT PRIMARY KEY,id_usuario INT,dni VARCHAR(15),telefono VARCHAR(20),fecha_nacimiento DATE,direccion VARCHAR(255),alergias VARCHAR(100),tipo_cobertura VARCHAR(50),canal_aviso VARCHAR(20),FOREIGN KEY(id_usuario) REFERENCES usuario(id_usuario))");
            st.execute("CREATE TABLE especialidad(id_especialidad INT PRIMARY KEY,nombre VARCHAR(100),estado VARCHAR(20) DEFAULT 'ACTIVO')");
            st.execute("CREATE TABLE sede(id_sede INT PRIMARY KEY,nombre VARCHAR(100),estado VARCHAR(20) DEFAULT 'ACTIVO')");
            st.execute("CREATE TABLE medico(id_medico INT PRIMARY KEY,id_usuario INT,cmp VARCHAR(20),id_especialidad INT,id_sede INT,estado VARCHAR(20) DEFAULT 'ACTIVO',FOREIGN KEY(id_usuario) REFERENCES usuario(id_usuario))");
            st.execute("CREATE TABLE disponibilidad(id_disponibilidad INT PRIMARY KEY,id_medico INT,id_sede INT,fecha DATE,hora_inicio TIME,hora_fin TIME,duracion_minutos INT,estado VARCHAR(20))");
            st.execute("CREATE TABLE motivo_cita(id_motivo INT PRIMARY KEY,descripcion VARCHAR(255))");
            st.execute("CREATE TABLE cita(id_cita INT PRIMARY KEY,id_paciente INT,id_medico INT,id_disponibilidad INT,id_motivo INT,fecha_hora DATETIME,estado VARCHAR(20),nivel_urgencia VARCHAR(20),riesgo_cancelacion VARCHAR(20),fecha_recomendada_proxima DATE,fecha_creacion DATETIME,FOREIGN KEY(id_paciente) REFERENCES paciente(id_paciente),FOREIGN KEY(id_medico) REFERENCES medico(id_medico))");
            st.execute("CREATE TABLE historial_asistencia(id_historial INT AUTO_INCREMENT PRIMARY KEY,id_paciente INT,id_cita INT,asistio BOOLEAN,observacion VARCHAR(255),fecha_registro DATETIME,FOREIGN KEY(id_cita) REFERENCES cita(id_cita),FOREIGN KEY(id_paciente) REFERENCES paciente(id_paciente))");
            st.execute("CREATE TABLE reasignacion_inteligente(id_reasignacion INT AUTO_INCREMENT PRIMARY KEY,id_cita_asignada INT,estado VARCHAR(20),FOREIGN KEY(id_cita_asignada) REFERENCES cita(id_cita))");
            st.execute("INSERT INTO usuario(id_usuario,nombres,apellidos,email,rol) VALUES(10,'Paciente','Uno','p@test','PACIENTE'),(20,'Medico','Uno','m@test','MEDICO'),(30,'Medico','Dos','m2@test','MEDICO')");
            st.execute("INSERT INTO paciente(id_paciente,id_usuario,dni) VALUES(1,10,'12345678')");
            st.execute("INSERT INTO medico(id_medico,id_usuario,cmp,id_especialidad,id_sede) VALUES(1,20,'1',1,1),(2,30,'2',1,1)");
            st.execute("INSERT INTO especialidad(id_especialidad,nombre) VALUES(1,'General')");
            st.execute("INSERT INTO sede(id_sede,nombre) VALUES(1,'Central')");
            st.execute("INSERT INTO cita(id_cita,id_paciente,id_medico,fecha_hora,estado,nivel_urgencia) VALUES(7,1,1,'2026-10-28 09:00:00','CONFIRMADA','MEDIA')");
        }
    }

    protected int count(String query) throws Exception {
        try (Connection c = abrir(); Statement s = c.createStatement(); var rs = s.executeQuery(query)) {
            rs.next(); return rs.getInt(1);
        }
    }

    @Test void patientCannotCancelAnotherPatientsAppointment() throws Exception {
        assertFalse(dao.actualizarEstadoCitaPaciente(7, 2, "CANCELADA"));
        assertEquals(1, count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA'"));
    }

    @Test void ownerCanCancelOnceAndCannotReactivateCancelledAppointment() {
        assertTrue(dao.actualizarEstadoCitaPaciente(7, 1, "CANCELADA"));
        assertFalse(dao.actualizarEstadoCitaPaciente(7, 1, "CANCELADA"));
        assertFalse(dao.actualizarEstadoCitaPaciente(7, 1, "CONFIRMADA"));
    }

    @Test void rejectsInvalidOwnerAndClinicalStateFromPatient() {
        assertFalse(dao.actualizarEstadoCitaPaciente(7, -1, "CANCELADA"));
        assertFalse(dao.actualizarEstadoCitaPaciente(7, 1, "ATENDIDA"));
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
        assertTrue(dao.actualizarAtencionMedica(7, 1, "ATENDIDA", "ALTA", "2026-12-01", "control"));
        assertFalse(dao.actualizarAtencionMedica(7, 1, "ATENDIDA", "ALTA", null, "repetido"));
        assertEquals(1, count("SELECT COUNT(*) FROM historial_asistencia WHERE asistio=TRUE"));
        assertEquals(1, count("SELECT COUNT(*) FROM cita WHERE estado='ATENDIDA' AND nivel_urgencia='ALTA'"));
    }

    @Test void historyFailureRollsBackAppointment() throws Exception {
        try (Connection c = abrir(); Statement s = c.createStatement()) {
            s.execute("DROP TABLE historial_asistencia");
        }
        assertFalse(dao.actualizarAtencionMedica(7, 1, "ATENDIDA", "ALTA", null, "control"));
        assertEquals(1, count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA' AND nivel_urgencia='MEDIA'"));
    }

    @Test void invalidPriorityOrDateDoesNotModifyAppointment() throws Exception {
        assertFalse(dao.actualizarAtencionMedica(7, 1, "ATENDIDA", "URGENTISIMA", null, "control"));
        assertFalse(dao.actualizarAtencionMedica(7, 1, "ATENDIDA", "ALTA", "invalida", "control"));
        assertEquals(1, count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA'"));
    }

    @Test void missingIdentitiesNeverResolveToOneOrCreatePatient() throws Exception {
        assertEquals(-1, dao.obtenerIdMedicoPorUsuarioId(999));
        assertEquals(-1, dao.obtenerIdUsuarioPorPacienteId(999));
        assertEquals(-1, dao.obtenerIdPacientePorUsuarioId(20));
        assertEquals(1, count("SELECT COUNT(*) FROM paciente"));
    }

    @Test void reservationWithMissingPatientNeverFallsBackToPatientOne() throws Exception {
        assertEquals(-1, dao.reservarCitaConId(-1, 1, "2026-12-01", "10:00", "BAJA"));
        assertEquals(-1, dao.reservarCitaConId(999, 1, "2026-12-01", "10:00", "BAJA"));
        assertEquals(1, count("SELECT COUNT(*) FROM cita"));
    }
}
