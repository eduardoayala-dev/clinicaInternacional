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
    @Test void detallesConservanLetrasAcentuadasYNombresConEnie() throws Exception {
        sql("UPDATE usuario SET nombres='Dra. Ángela',apellidos='Peña Ñúñez' WHERE id_usuario=(SELECT id_usuario FROM medico WHERE id_medico=42)");
        var detalle=dao.obtenerDetallesMedico(42);
        assertEquals("Dr. Ángela Peña Ñúñez",detalle.get("nombreMedico"));
    }

    @Test void catalogosJpaConservanDatosConteosYDetalles() throws Exception {
        sql("UPDATE especialidad SET nombre='Cardiología' WHERE id_especialidad=1");
        sql("UPDATE sede SET nombre='San Borja',direccion='Avenida de prueba 123' WHERE id_sede=1");
        var especialidades = dao.obtenerEspecialidades();
        assertEquals(1, especialidades.size());
        assertEquals("Cardiología", especialidades.get(0).get("nombre"));
        assertEquals(4, especialidades.get(0).get("totalMedicos"));
        assertEquals(2, dao.obtenerSedes().size());
        var detalle = dao.obtenerDetallesMedico(42);
        assertEquals(42, detalle.get("idMedico"));
        assertEquals("San Borja", detalle.get("sedeNombre"));
        assertEquals("Cardiología", detalle.get("especialidad"));
        assertTrue(dao.obtenerDetallesMedico(999).isEmpty());
        assertEquals(6, dao.obtenerTodosMedicosDetalle().size());
    }

    @Test void historialJpaNoDuplicaMedicosNiMezclaPacientes() throws Exception {
        sql("ALTER TABLE cita ADD COLUMN id_paciente INT");
        sql("INSERT INTO cita(id_medico,id_paciente) VALUES(42,7),(42,7),(43,7),(44,8)");
        var historial = dao.obtenerMedicosDePaciente(7);
        assertEquals(2, historial.size());
        assertEquals(java.util.Set.of(42,43), historial.stream().map(m -> m.get("idMedico")).collect(java.util.stream.Collectors.toSet()));
        assertTrue(dao.obtenerMedicosDePaciente(99).isEmpty());
    }

    @Test void conteoConJpaConservaLaConexionYLaTransaccionDelLlamador() throws Exception {
        agenda("2026-10-28", "09:00", "10:00", 30, "PUBLICADO");
        var desde = java.time.LocalDate.of(2026, 10, 28);
        try (var conexion = DriverManager.getConnection(url)) {
            conexion.setAutoCommit(false);
            try (var insercion = conexion.prepareStatement(
                    "INSERT INTO cita(id_medico,fecha_hora,estado,id_disponibilidad) VALUES(42,?,'CONFIRMADA',1)")) {
                insercion.setObject(1, desde.atTime(9, 0));
                insercion.executeUpdate();
            }
            assertEquals(1, dao.contarCuposDisponibles(conexion, 1, desde, desde.plusDays(1)));
            assertFalse(conexion.isClosed());
            assertFalse(conexion.getAutoCommit());
            conexion.rollback();
            assertEquals(2, dao.contarCuposDisponibles(conexion, 1, desde, desde.plusDays(1)));
        }
    }

    @Test void erroresDeJpaMantienenElContratoDelCatalogo() throws Exception {
        sql("DROP TABLE disponibilidad");
        var error = assertThrows(CatalogoDatosException.class,
                () -> dao.disponibilidadSemanal(1, 1, 42, java.time.LocalDate.of(2026, 10, 28)));
        assertInstanceOf(SQLException.class, error.getCause());
        assertEquals("No fue posible consultar el catálogo.", error.getMessage());
    }

    @Test void semanaAgrupaConsultasAunqueAumentenMedicosYConservaFiltros()throws Exception {
        for(int i=0;i<7;i++)agenda(java.time.LocalDate.of(2026,10,28).plusDays(i).toString(),"09:00","10:00",30,"PUBLICADO");
        sql("INSERT INTO disponibilidad(id_medico,id_sede,fecha,hora_inicio,hora_fin,duracion_minutos,estado) VALUES(43,2,'2026-10-28','14:00','15:00',30,'PUBLICADO')");
        var consultas=new java.util.concurrent.atomic.AtomicInteger();
        var medido=new MedicoDAO(()->{
            var c=DriverManager.getConnection(url);
            return (java.sql.Connection)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{java.sql.Connection.class},(proxy,metodo,args)->{
                if(metodo.getName().equals("prepareStatement"))consultas.incrementAndGet();
                try{return metodo.invoke(c,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
            });
        },clock);
        var desde=java.time.LocalDate.of(2026,10,28);
        var todos=medido.disponibilidadSemanal(1,null,null,desde);
        assertEquals(16,todos.stream().mapToInt(d->d.manana()+d.tarde()).sum());
        assertEquals(5,consultas.get());
        consultas.set(0);
        assertEquals(14,medido.disponibilidadSemanal(1,1,42,desde).stream().mapToInt(d->d.manana()+d.tarde()).sum());
        assertEquals(5,consultas.get());
        assertEquals(2,medido.disponibilidadSemanal(1,2,null,desde).stream().mapToInt(d->d.manana()+d.tarde()).sum());
        sql("UPDATE sede SET estado='INACTIVO' WHERE id_sede=2");
        assertEquals(14,medido.disponibilidadSemanal(1,null,null,desde).stream().mapToInt(d->d.manana()+d.tarde()).sum());
    }
    @Test void conteoAgrupadoCoincideConHorariosYUsaCuatroConsultas()throws Exception {
        for(int i=0;i<7;i++){
            String dia=java.time.LocalDate.of(2026,10,27).plusDays(i).toString();
            agenda(dia,"09:00","14:00",30,"PUBLICADO");
            agenda(dia,"09:00","11:00",30,"PUBLICADO");
        }
        sql("INSERT INTO cita(id_medico,fecha_hora,estado,id_disponibilidad) VALUES(42,'2026-10-28 09:15:00','CONFIRMADA',1),(42,'2026-10-28 11:00:00','CANCELADA',1)");
        sql("INSERT INTO retencion_cupo VALUES(1,42,'2026-10-28 13:00:00',30,'prueba','2026-10-29 10:00:00')");
        sql("INSERT INTO usuario VALUES(91,'Paciente','Oferta',TRUE,'PACIENTE')");
        sql("INSERT INTO paciente VALUES(91,91)");
        sql("INSERT INTO lista_espera VALUES(91,91,'OFERTADA')");
        sql("INSERT INTO reasignacion_inteligente(id_lista_espera,id_cita_original,id_disponibilidad,fecha_limite_respuesta,estado) VALUES(91,2,1,'2026-10-28 10:30:00','PENDIENTE')");
        int esperado=0;
        for(int i=0;i<7;i++)esperado+=dao.obtenerHorariosDisponibles(42,java.time.LocalDate.of(2026,10,27).plusDays(i).toString()).size();
        var consultas=new java.util.concurrent.atomic.AtomicInteger();
        try(var c=DriverManager.getConnection(url)){
            var medida=(java.sql.Connection)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{java.sql.Connection.class},(proxy,metodo,args)->{
                if(metodo.getName().equals("prepareStatement"))consultas.incrementAndGet();
                try{return metodo.invoke(c,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
            });
            assertEquals(esperado,dao.contarCuposDisponibles(medida,1,java.time.LocalDate.of(2026,10,27),java.time.LocalDate.of(2026,11,3)));
            assertEquals(4,consultas.get());
            assertEquals(0,dao.contarCuposDisponibles(c,3,java.time.LocalDate.of(2026,10,27),java.time.LocalDate.of(2026,11,3)));
        }
    }
    @Test void semanaCuentaCuposRealesPorTurnoExcluyendoReservasYRetenciones()throws Exception{
        agenda("2026-10-28","09:00","10:00",30,"PUBLICADO");agenda("2026-10-28","13:00","14:00",30,"PUBLICADO");
        sql("INSERT INTO cita(id_medico,fecha_hora,estado) VALUES(42,'2026-10-28 09:00:00','CONFIRMADA')");
        sql("INSERT INTO retencion_cupo VALUES(1,42,'2026-10-28 13:00:00',30,'prueba','2026-10-29 10:00:00')");
        var semana=dao.disponibilidadSemanal(1,1,42,java.time.LocalDate.of(2026,10,26));assertEquals(7,semana.size());
        var dia=semana.stream().filter(d->d.fecha().equals("2026-10-28")).findFirst().orElseThrow();assertEquals(1,dia.manana());assertEquals(1,dia.tarde());
        assertEquals(0,dao.disponibilidadSemanal(1,2,42,java.time.LocalDate.of(2026,10,26)).stream().mapToInt(d->d.manana()+d.tarde()).sum());
        assertEquals(0,dao.disponibilidadSemanal(1,1,43,java.time.LocalDate.of(2026,10,26)).stream().mapToInt(d->d.manana()+d.tarde()).sum());
    }
    @Test void inicioOperativoCuentaCuposReservablesYNoMezclaSedes()throws Exception {
        sql("CREATE TABLE personal_administrativo(id_usuario INT PRIMARY KEY,id_sede INT)");
        sql("INSERT INTO usuario VALUES(90,'Admisión','Uno',TRUE,'PERSONAL_ADMINISTRATIVO')");
        sql("INSERT INTO personal_administrativo VALUES(90,1)");
        agenda("2026-10-27","11:00","12:00",20,"PUBLICADO");
        sql("INSERT INTO disponibilidad(id_medico,id_sede,fecha,hora_inicio,hora_fin,duracion_minutos,estado) VALUES(43,2,'2026-10-27','11:00','12:00',20,'PUBLICADO')");
        sql("INSERT INTO cita(id_medico,fecha_hora,estado,id_disponibilidad) VALUES(42,'2026-10-27 11:00:00','CONFIRMADA',1),(42,'2026-10-27 11:20:00','CANCELADA',1),(43,'2026-10-27 11:00:00','CONFIRMADA',2)");
        var resumen=new InicioOperativoDAO(()->DriverManager.getConnection(url),clock).consultar(90);
        assertEquals(2,resumen.citas());assertEquals(1,resumen.canceladas());assertEquals(2,resumen.cupos());assertEquals(0,resumen.ofertas());
        assertEquals(403,assertThrows(AgendaException.class,()->new InicioOperativoDAO(()->DriverManager.getConnection(url),clock).consultar(1)).status());
    }
    private String url;
    private MedicoDAO dao;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-27T15:10:00Z"), ZoneId.of("America/Lima"));

    @BeforeEach void prepare() throws Exception {
        url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        dao = new MedicoDAO(() -> DriverManager.getConnection(url), clock);
        sql("CREATE TABLE usuario(id_usuario INT PRIMARY KEY,nombres VARCHAR,apellidos VARCHAR,activo BOOLEAN,rol VARCHAR DEFAULT 'MEDICO')");
        sql("CREATE TABLE especialidad(id_especialidad INT PRIMARY KEY,estado VARCHAR,nombre VARCHAR)");
        sql("CREATE TABLE sede(id_sede INT PRIMARY KEY,estado VARCHAR,nombre VARCHAR,direccion VARCHAR)");
        sql("CREATE TABLE medico(id_medico INT PRIMARY KEY,id_usuario INT,id_especialidad INT,id_sede INT,cmp VARCHAR,estado VARCHAR)");
        sql("CREATE TABLE disponibilidad(id_disponibilidad INT AUTO_INCREMENT PRIMARY KEY,id_medico INT,id_sede INT DEFAULT 1,fecha DATE,hora_inicio TIME,hora_fin TIME,duracion_minutos INT,estado VARCHAR)");
        sql("CREATE TABLE retencion_cupo(id_paciente INT PRIMARY KEY,id_medico INT,inicio DATETIME,duracion_minutos INT,identificador VARCHAR(36),vence DATETIME)");
        sql("CREATE TABLE cita(id_cita INT AUTO_INCREMENT PRIMARY KEY,id_medico INT,fecha_hora TIMESTAMP,estado VARCHAR,id_disponibilidad INT DEFAULT 1)");
        sql("CREATE TABLE paciente(id_paciente INT PRIMARY KEY,id_usuario INT)");
        sql("CREATE TABLE lista_espera(id_lista_espera INT PRIMARY KEY,id_paciente INT,estado VARCHAR)");
        sql("CREATE TABLE reasignacion_inteligente(id_reasignacion INT AUTO_INCREMENT PRIMARY KEY,id_lista_espera INT,id_cita_original INT,id_disponibilidad INT,fecha_limite_respuesta DATETIME,estado VARCHAR(20))");
        sql("INSERT INTO usuario(id_usuario,nombres,apellidos,activo) VALUES (1,'MÉDICO','PRUEBA',TRUE),(2,'OTRO','MÉDICO',TRUE),(3,'INACTIVO','PRUEBA',FALSE)");
        sql("INSERT INTO especialidad(id_especialidad,estado) VALUES(1,'ACTIVO'),(2,'INACTIVO')");
        sql("INSERT INTO sede(id_sede,estado) VALUES(1,'ACTIVO'),(2,'ACTIVO'),(3,'INACTIVO')");
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

    @Test void waitingListCatalogOnlyContainsActiveDoctorsAndRealClinicIds() {
        var medicos = dao.obtenerMedicosParaEspera();
        assertEquals(2, medicos.size());
        assertTrue(medicos.stream().allMatch(m -> (int)m.get("idEspecialidad") == 1));
        assertTrue(medicos.stream().anyMatch(m -> (int)m.get("idSede") == 2));
        assertFalse(medicos.stream().anyMatch(m -> (int)m.get("idMedico") == 44));
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
