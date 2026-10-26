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
    private String url;
    private CitaDAO dao;

    @BeforeEach void setUp() throws Exception {
        url = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=MySQL";
        dao = new CitaDAO() {
            @Override protected Connection abrirConexion() throws SQLException {
                return DriverManager.getConnection(url);
            }
        };
        try (Connection conn = DriverManager.getConnection(url); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE usuario(id_usuario INT PRIMARY KEY,nombres VARCHAR,apellidos VARCHAR,email VARCHAR)");
            st.execute("CREATE TABLE paciente(id_paciente INT PRIMARY KEY,id_usuario INT,dni VARCHAR,telefono VARCHAR,fecha_nacimiento DATE,direccion VARCHAR,alergias VARCHAR,tipo_cobertura VARCHAR,canal_aviso VARCHAR)");
            st.execute("CREATE TABLE medico(id_medico INT PRIMARY KEY,id_usuario INT,cmp VARCHAR,id_especialidad INT,id_sede INT)");
            st.execute("CREATE TABLE especialidad(id_especialidad INT PRIMARY KEY,nombre VARCHAR)");
            st.execute("CREATE TABLE sede(id_sede INT PRIMARY KEY,nombre VARCHAR)");
            st.execute("CREATE TABLE motivo_cita(id_motivo INT PRIMARY KEY,descripcion VARCHAR)");
            st.execute("CREATE TABLE cita(id_cita INT PRIMARY KEY,id_paciente INT,id_medico INT,id_disponibilidad INT,id_motivo INT,fecha_hora TIMESTAMP,estado VARCHAR,nivel_urgencia VARCHAR,riesgo_cancelacion VARCHAR,fecha_recomendada_proxima DATE,fecha_creacion TIMESTAMP)");
            st.execute("CREATE TABLE historial_asistencia(id_paciente INT,id_cita INT,asistio BOOLEAN,observacion VARCHAR)");
            st.execute("INSERT INTO usuario VALUES(10,'Paciente','Uno','p@test'),(20,'Medico','Uno','m@test'),(30,'Medico','Dos','m2@test')");
            st.execute("INSERT INTO paciente(id_paciente,id_usuario,dni) VALUES(1,10,'12345678')");
            st.execute("INSERT INTO medico VALUES(1,20,'1',1,1),(2,30,'2',1,1)");
            st.execute("INSERT INTO especialidad VALUES(1,'General')");
            st.execute("INSERT INTO sede VALUES(1,'Central')");
            st.execute("INSERT INTO cita(id_cita,id_paciente,id_medico,fecha_hora,estado,nivel_urgencia) VALUES(7,1,1,CURRENT_TIMESTAMP,'CONFIRMADA','MEDIA')");
        }
    }

    private int count(String query) throws Exception {
        try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement(); var rs = s.executeQuery(query)) {
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
        try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement()) {
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
