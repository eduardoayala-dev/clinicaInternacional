package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SeguimientoIaDAOTest {
    private String url;
    protected Connection abrir() throws SQLException {return DriverManager.getConnection(url);}
    private final Clock reloj=Clock.fixed(Instant.parse("2026-11-02T15:00:00Z"),ZoneId.of("America/Lima"));
    private SeguimientoIaDAO dao(){return new SeguimientoIaDAO(this::abrir,reloj);}
    private void ejecutar(String sql) throws SQLException {try(var c=abrir();var s=c.createStatement()){s.execute(sql);}}
    @BeforeEach void preparar() throws Exception {
        url="jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try(var c=abrir();var s=c.createStatement()) {
            EsquemaPruebas.limpiar(c);
            s.execute("CREATE TABLE usuario(id_usuario INT PRIMARY KEY,rol VARCHAR(30),activo BOOLEAN DEFAULT TRUE)");
            s.execute("CREATE TABLE paciente(id_paciente INT PRIMARY KEY,id_usuario INT)");
            s.execute("CREATE TABLE medico(id_medico INT PRIMARY KEY,id_sede INT)");
            s.execute("CREATE TABLE disponibilidad(id_disponibilidad INT PRIMARY KEY,id_sede INT)");
            s.execute("CREATE TABLE sede(id_sede INT PRIMARY KEY,estado VARCHAR(20))");
            s.execute("CREATE TABLE personal_administrativo(id_usuario INT,id_sede INT)");
            s.execute("CREATE TABLE cita(id_cita INT PRIMARY KEY,id_paciente INT,id_medico INT,id_disponibilidad INT,fecha_hora DATETIME,estado VARCHAR(20))");
            s.execute("CREATE TABLE confirmacion_asistencia(id_cita INT PRIMARY KEY,creada_en DATETIME,vence_en DATETIME,confirmada_en DATETIME)");
            s.execute("CREATE TABLE inasistencia_cita(id_cita INT PRIMARY KEY,id_usuario_personal INT,fecha_registro DATETIME)");
            s.execute("CREATE TABLE atencion_cita(id_cita INT PRIMARY KEY,id_paciente INT,id_medico INT,fecha_inicio DATETIME,fecha_fin DATETIME)");
            s.execute("INSERT INTO usuario VALUES(1,'PACIENTE',TRUE),(2,'PERSONAL_ADMINISTRATIVO',TRUE),(3,'PERSONAL_ADMINISTRATIVO',TRUE),(4,'MEDICO',TRUE)");
            s.execute("INSERT INTO paciente VALUES(1,1),(2,1)");
            s.execute("INSERT INTO sede VALUES(1,'ACTIVO'),(2,'ACTIVO')");
            s.execute("INSERT INTO personal_administrativo VALUES(2,1),(3,2)");
            s.execute("INSERT INTO medico VALUES(1,1)");
            s.execute("INSERT INTO disponibilidad VALUES(1,1),(2,2)");
            s.execute("INSERT INTO cita VALUES(7,1,1,1,'2026-11-04 10:00:00','CONFIRMADA')");
            s.execute("INSERT INTO confirmacion_asistencia VALUES(7,'2026-10-30 10:00:00','2026-11-02 10:00:00',NULL)");
        }
    }
    @Test void obtieneCitaDeLaSedeSinConvertirEstadoEnConfirmacionExpresa() {
        var x=dao().consultar(2,7);
        assertFalse(x.entrada().confirmadaPorPaciente()); assertTrue(x.plazoVencido());
        assertEquals(5,x.entrada().anticipacionDias()); assertEquals(0,x.entrada().inasistenciasPrevias());
    }
    @Test void rechazaOtraSedeYRolAunqueSeManipuleElIdentificador() {
        for(int actor:new int[]{1,3,4,99}) assertEquals(404,assertThrows(AgendaException.class,()->dao().consultar(actor,7)).status());
    }
    @Test void autorizaPorSedeDelBloqueOriginalAunqueCambieLaSedeActualDelMedico() throws Exception {
        ejecutar("UPDATE medico SET id_sede=2"); assertEquals(7,dao().consultar(2,7).cita());
        assertEquals(404,assertThrows(AgendaException.class,()->dao().consultar(3,7)).status());
    }
    @Test void cuentaSoloResultadosDocumentadosConocidosAntesDeReservar() throws Exception {
        ejecutar("INSERT INTO cita VALUES(8,1,1,1,'2026-10-01 09:00:00','NO_ASISTIO'),(9,1,1,1,'2026-10-02 09:00:00','ATENDIDA'),(10,1,1,1,'2026-10-03 09:00:00','NO_ASISTIO'),(11,1,1,1,'2026-10-04 09:00:00','CANCELADA'),(12,2,1,1,'2026-10-01 09:00:00','NO_ASISTIO')");
        ejecutar("INSERT INTO inasistencia_cita VALUES(8,2,'2026-10-01 10:00:00'),(10,2,'2026-10-30 10:00:00'),(11,2,'2026-10-04 10:00:00'),(12,2,'2026-10-01 10:00:00')");
        ejecutar("INSERT INTO atencion_cita VALUES(9,1,1,'2026-10-02 09:00:00','2026-10-02 09:20:00')");
        var x=dao().consultar(2,7).entrada(); assertEquals(1,x.inasistenciasPrevias()); assertEquals(1,x.atencionesPrevias());
    }
    @Test void noCuentaEstadoSinRegistroNiResultadosPosterioresALaReserva() throws Exception {
        ejecutar("INSERT INTO cita VALUES(8,1,1,1,'2026-10-01 09:00:00','NO_ASISTIO'),(9,1,1,1,'2026-10-31 09:00:00','ATENDIDA')");
        ejecutar("INSERT INTO atencion_cita VALUES(9,1,1,'2026-10-31 09:00:00','2026-10-31 09:20:00')");
        var x=dao().consultar(2,7).entrada(); assertEquals(0,x.inasistenciasPrevias()); assertEquals(0,x.atencionesPrevias());
    }
    @Test void confirmarActualizaLaEntradaSinCambiarElHistorial() throws Exception {
        ejecutar("UPDATE confirmacion_asistencia SET confirmada_en='2026-11-02 09:00:00'");
        var x=dao().consultar(2,7); assertTrue(x.entrada().confirmadaPorPaciente());assertFalse(x.plazoVencido());
    }
    @Test void noInventaDatosCuandoFaltaInstantaneaOFechasSonInvalidas() throws Exception {
        ejecutar("UPDATE confirmacion_asistencia SET creada_en=NULL");
        assertEquals("DATOS_IA_INSUFICIENTES",assertThrows(AgendaException.class,()->dao().consultar(2,7)).code());
    }
    @Test void noEvaluaCitasCanceladasNiIniciadas() throws Exception {
        ejecutar("UPDATE cita SET estado='CANCELADA'");
        assertEquals(409,assertThrows(AgendaException.class,()->dao().consultar(2,7)).status());
        ejecutar("UPDATE cita SET estado='CONFIRMADA',fecha_hora='2026-11-02 10:00:00'");
        assertEquals(409,assertThrows(AgendaException.class,()->dao().consultar(2,7)).status());
    }
    @Test void desactivarPersonalOPacienteRevocaAcceso() throws Exception {
        ejecutar("UPDATE usuario SET activo=FALSE WHERE id_usuario=2");
        assertEquals(404,assertThrows(AgendaException.class,()->dao().consultar(2,7)).status());
        ejecutar("UPDATE usuario SET activo=TRUE WHERE id_usuario=2");ejecutar("UPDATE usuario SET activo=FALSE WHERE id_usuario=1");
        assertEquals(404,assertThrows(AgendaException.class,()->dao().consultar(2,7)).status());
    }
}
