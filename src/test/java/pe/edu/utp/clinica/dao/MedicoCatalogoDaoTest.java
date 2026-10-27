package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class MedicoCatalogoDaoTest {
    private String url;
    private MedicoDAO dao;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-27T15:10:00Z"), ZoneId.of("America/Lima"));

    @BeforeEach void prepare() throws Exception {
        url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        dao = new MedicoDAO(() -> DriverManager.getConnection(url), clock);
        sql("CREATE TABLE usuario(id_usuario INT PRIMARY KEY,nombres VARCHAR,apellidos VARCHAR,activo BOOLEAN)");
        sql("CREATE TABLE especialidad(id_especialidad INT PRIMARY KEY,estado VARCHAR)");
        sql("CREATE TABLE sede(id_sede INT PRIMARY KEY,estado VARCHAR)");
        sql("CREATE TABLE medico(id_medico INT PRIMARY KEY,id_usuario INT,id_especialidad INT,id_sede INT,cmp VARCHAR,estado VARCHAR)");
        sql("CREATE TABLE disponibilidad(id_disponibilidad INT AUTO_INCREMENT PRIMARY KEY,id_medico INT,id_sede INT DEFAULT 1,fecha DATE,hora_inicio TIME,hora_fin TIME,duracion_minutos INT,estado VARCHAR)");
        sql("CREATE TABLE cita(id_medico INT,fecha_hora TIMESTAMP,estado VARCHAR,id_disponibilidad INT DEFAULT 1)");
        sql("INSERT INTO usuario VALUES (1,'MÉDICO','PRUEBA',TRUE),(2,'OTRO','MÉDICO',TRUE),(3,'INACTIVO','PRUEBA',FALSE)");
        sql("INSERT INTO especialidad VALUES(1,'ACTIVO'),(2,'INACTIVO')");
        sql("INSERT INTO sede VALUES(1,'ACTIVO'),(2,'ACTIVO'),(3,'INACTIVO')");
        sql("INSERT INTO medico VALUES(42,1,1,1,NULL,'ACTIVO'),(43,2,1,2,'CMP-43','ACTIVO'),(44,3,1,1,'44','ACTIVO'),(45,1,2,1,'45','ACTIVO'),(46,1,1,3,'46','ACTIVO'),(47,1,1,1,'47','INACTIVO')");
    }

    private void sql(String query) throws Exception {
        try (var conn = DriverManager.getConnection(url); var st = conn.createStatement()) { st.execute(query); }
    }

    private void agenda(String day, String start, String end, int duration, String estado) throws Exception {
        try (var conn = DriverManager.getConnection(url);
                var ps = conn.prepareStatement("INSERT INTO disponibilidad(id_medico,fecha,hora_inicio,hora_fin,duracion_minutos,estado) VALUES(42,?,?,?,?,?)")) {
            ps.setDate(1, java.sql.Date.valueOf(day));
            ps.setTime(2, java.sql.Time.valueOf(java.time.LocalTime.parse(start)));
            ps.setTime(3, java.sql.Time.valueOf(java.time.LocalTime.parse(end)));
            ps.setInt(4, duration);
            ps.setString(5, estado);
            ps.executeUpdate();
        }
    }

    @Test void medicalCatalogRespectsClinicAndActiveStatesWithoutInventingCmp() {
        var medicos = dao.consultarMedicos(1, 1);
        assertEquals(1, medicos.size()); assertEquals(42, medicos.get(0).idMedico());
        assertEquals("Médico Prueba", medicos.get(0).nombreCompleto()); assertEquals("", medicos.get(0).cmp());
        assertEquals(2, dao.consultarMedicos(1, null).size());
        assertTrue(dao.consultarMedicos(1, 99).isEmpty());
    }

    @Test void absentAvailabilityDoesNotInventDatesOrHours() {
        assertTrue(dao.obtenerFechasDisponibles(42).isEmpty());
        assertTrue(dao.obtenerHorariosDisponibles(42, "2026-10-28").isEmpty());
    }

    @Test void onlyPublishedFutureDatesAreReturnedInOrder() throws Exception {
        agenda("2026-10-28", "09:00", "10:00", 30, "PUBLICADO");
        agenda("2026-10-28", "14:00", "15:00", 30, "PUBLICADO");
        agenda("2026-10-29", "09:00", "10:00", 30, "BORRADOR");
        agenda("2026-10-26", "09:00", "10:00", 30, "PUBLICADO");
        assertEquals(List.of("2026-10-28"), dao.obtenerFechasDisponibles(42));
    }

    @Test void occupiedHoursAreExcludedAndCancelledAppointmentsReleaseTheirSlot() throws Exception {
        agenda("2026-10-28", "09:00", "11:00", 30, "PUBLICADO");
        sql("INSERT INTO cita(id_medico,fecha_hora,estado) VALUES(42,'2026-10-28 09:00:00','PROGRAMADA'),(42,'2026-10-28 09:30:00','CANCELADA'),(42,'2026-10-28 10:00:00','REPROGRAMADA'),(43,'2026-10-28 10:30:00','PROGRAMADA')");
        assertEquals(List.of("09:30", "10:00", "10:30"), dao.obtenerHorariosDisponibles(42, "2026-10-28"));
    }

    @Test void anOccupiedIntervalHidesSlotsThatIntersectEvenWithDifferentStartTimes() throws Exception {
        agenda("2026-10-28", "09:00", "11:00", 30, "PUBLICADO");
        agenda("2026-10-28", "09:15", "09:45", 30, "PUBLICADO");
        sql("INSERT INTO cita(id_medico,fecha_hora,estado,id_disponibilidad) VALUES(42,'2026-10-28 09:15:00','CONFIRMADA',2)");
        assertEquals(List.of("10:00", "10:30"), dao.obtenerHorariosDisponibles(42, "2026-10-28"));
    }

    @Test void overlappingPublishedBlocksAreDeduplicatedAndTodayIsFilteredInClinicTimezone() throws Exception {
        agenda("2026-10-27", "09:00", "11:00", 30, "PUBLICADO");
        agenda("2026-10-27", "10:00", "11:00", 30, "PUBLICADO");
        assertEquals(List.of("10:30"), dao.obtenerHorariosDisponibles(42, "2026-10-27"));
        assertTrue(dao.obtenerHorariosDisponibles(42, "2026-10-26").isEmpty());
    }

    @Test void midnightAndEndBoundaryDoNotWrapOrCreateIncompleteAppointments() throws Exception {
        agenda("2026-10-28", "00:00", "01:10", 30, "PUBLICADO");
        assertEquals(List.of("00:00", "00:30"), dao.obtenerHorariosDisponibles(42, "2026-10-28"));
    }

    @Test void malformedPublishedBlocksFailInsteadOfInventingDefaultHours() throws Exception {
        agenda("2026-10-28", "09:00", "11:00", 0, "PUBLICADO");
        assertThrows(CatalogoDatosException.class, () -> dao.obtenerHorariosDisponibles(42, "2026-10-28"));
    }

    @Test void sqlFailuresAreNotReportedAsEmptyCollections() throws Exception {
        sql("DROP TABLE cita");
        agenda("2026-10-28", "09:00", "11:00", 30, "PUBLICADO");
        assertThrows(CatalogoDatosException.class, () -> dao.obtenerHorariosDisponibles(42, "2026-10-28"));
        var unavailable = new MedicoDAO(() -> { throw new SQLException("privado"); }, clock);
        assertThrows(CatalogoDatosException.class, () -> unavailable.consultarMedicos(1, 1));
        assertThrows(CatalogoDatosException.class, () -> unavailable.obtenerFechasDisponibles(42));
    }

    @Test void staleOrInactiveDoctorAvailabilityIsNotOffered() throws Exception {
        agenda("2026-10-28", "09:00", "10:00", 30, "PUBLICADO");
        sql("UPDATE disponibilidad SET id_sede=2");
        assertTrue(dao.obtenerFechasDisponibles(42).isEmpty());
        assertTrue(dao.obtenerHorariosDisponibles(42, "2026-10-28").isEmpty());
        sql("UPDATE disponibilidad SET id_sede=1");
        sql("UPDATE medico SET estado='INACTIVO' WHERE id_medico=42");
        assertTrue(dao.obtenerFechasDisponibles(42).isEmpty());
        assertTrue(dao.obtenerHorariosDisponibles(42, "2026-10-28").isEmpty());
    }
}
