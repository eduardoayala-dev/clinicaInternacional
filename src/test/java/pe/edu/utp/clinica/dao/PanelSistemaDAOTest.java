package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PanelSistemaDAOTest {
    private String url;private PanelSistemaDAO dao;
    private Connection abrir()throws SQLException{return DriverManager.getConnection(url);}
    private void sql(String texto)throws SQLException{try(var c=abrir();var s=c.createStatement()){s.execute(texto);}}
    private PanelSistemaDAO.Filtro filtro(int sede,int especialidad){return new PanelSistemaDAO.Filtro(LocalDate.of(2026,11,1),LocalDate.of(2026,11,7),sede,especialidad);}
    @BeforeEach void preparar()throws SQLException{
        url="jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1";
        dao=new PanelSistemaDAO(this::abrir,Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"),ZoneOffset.UTC));
        sql("CREATE TABLE usuario(id_usuario INT PRIMARY KEY,nombres VARCHAR(100),apellidos VARCHAR(100),rol VARCHAR(40),activo BOOLEAN)");
        sql("CREATE TABLE administrador(id_usuario INT PRIMARY KEY,estado VARCHAR(20))");
        sql("CREATE TABLE sede(id_sede INT PRIMARY KEY,nombre VARCHAR(100),direccion VARCHAR(200),estado VARCHAR(20))");
        sql("CREATE TABLE especialidad(id_especialidad INT PRIMARY KEY,nombre VARCHAR(100))");
        sql("CREATE TABLE medico(id_medico INT PRIMARY KEY,id_sede INT,id_especialidad INT)");
        sql("CREATE TABLE disponibilidad(id_disponibilidad INT PRIMARY KEY,id_sede INT)");
        sql("CREATE TABLE cita(id_cita INT PRIMARY KEY,id_medico INT,id_disponibilidad INT,fecha_hora DATETIME,estado VARCHAR(20),riesgo_cancelacion VARCHAR(20))");
        sql("CREATE TABLE inasistencia_cita(id_cita INT PRIMARY KEY)");
        sql("CREATE TABLE reasignacion_inteligente(id_reasignacion INT PRIMARY KEY,id_cita_original INT,id_cita_asignada INT,estado VARCHAR(20),fecha_limite_respuesta DATETIME)");
        sql("CREATE TABLE auditoria_seguridad(id_auditoria INT PRIMARY KEY,id_usuario INT,evento VARCHAR(100),descripcion VARCHAR(300),fecha_registro DATETIME)");
        sql("INSERT INTO usuario VALUES(1,'Admin','Uno','ADMINISTRADOR',TRUE),(2,'Paciente','Uno','PACIENTE',TRUE)");sql("INSERT INTO administrador VALUES(1,'ACTIVO')");
        sql("INSERT INTO sede VALUES(1,'San Borja','Avenida uno','ACTIVO'),(2,'Lima','Avenida dos','ACTIVO'),(3,'Sin citas','Avenida tres','INACTIVO')");
        sql("INSERT INTO especialidad VALUES(1,'Cardiología'),(2,'General')");sql("INSERT INTO medico VALUES(1,2,1),(2,2,2)");sql("INSERT INTO disponibilidad VALUES(1,1),(2,2)");
        sql("INSERT INTO cita VALUES(1,1,1,'2026-11-03 09:00:00','ATENDIDA','BAJO'),(2,1,1,'2026-11-05 09:00:00','CANCELADA','MEDIO'),(3,2,2,'2026-11-06 09:00:00','NO_ASISTIO',NULL),(4,2,2,'2026-11-06 10:00:00','CONFIRMADA','BAJO'),(5,1,1,'2026-11-08 00:00:00','ATENDIDA','ALTO')");
        sql("INSERT INTO inasistencia_cita VALUES(3)");
        sql("INSERT INTO reasignacion_inteligente VALUES(1,2,4,'ACEPTADA','2026-11-05 08:00:00'),(2,2,NULL,'PENDIENTE','2026-11-04 20:00:00'),(3,2,NULL,'PENDIENTE','2026-11-01 08:00:00')");
        sql("INSERT INTO auditoria_seguridad VALUES(1,1,'SEDE_GUARDADA','Sede editada','2026-11-03 10:00:00'),(2,NULL,'MANTENIMIENTO','Proceso registrado','2026-11-04 10:00:00')");
    }
    @Test void agregaDatosRealesSinDuplicarOfertasYRespetaLimiteDeFechas(){
        var p=dao.consultar(1,filtro(0,0));var d=p.datos();
        assertEquals(4,d.citas());assertEquals(1,d.atendidas());assertEquals(1,d.canceladas());assertEquals(1,d.inasistencias());assertEquals(1,d.reasignadas());
        assertEquals(3,d.ofertas());assertEquals(1,d.aceptadas());assertEquals(1,d.pendientes());assertEquals(2,d.bajo());assertEquals(1,d.medio());assertEquals(1,d.sinRiesgo());
        assertEquals(4,p.semanas().stream().mapToLong(PanelSistemaDAO.Semana::citas).sum());assertEquals(2,p.totales().usuarios());assertEquals(3,p.totales().sedes());
        assertEquals("MANTENIMIENTO",p.auditoria().get(0).evento());assertEquals("Sistema",p.auditoria().get(0).actor());
    }
    @Test void filtraPorSedeHistoricaYEspecialidadSinInventarActividad(){
        assertEquals(2,dao.consultar(1,filtro(1,0)).datos().citas());
        assertEquals(0,dao.consultar(1,filtro(1,2)).datos().citas());
        assertEquals(2,dao.consultar(1,filtro(0,2)).datos().citas());
        var vacio=dao.consultar(1,filtro(3,0));assertEquals(0,vacio.datos().citas());assertEquals(1,vacio.sedes().size());assertEquals("Sin citas",vacio.sedes().get(0).nombre());
    }
    @Test void validaPermisosYFiltros(){
        assertEquals(403,assertThrows(AgendaException.class,()->dao.consultar(2,filtro(0,0))).status());
        assertEquals(400,assertThrows(AgendaException.class,()->dao.consultar(1,filtro(999,0))).status());
        assertThrows(AgendaException.class,()->filtro(-1,0));
    }
    @Test void falloDeConsultaNoDevuelveCerosComoSiFueranDatosValidos()throws SQLException{
        sql("DROP TABLE auditoria_seguridad");assertEquals(503,assertThrows(AgendaException.class,()->dao.consultar(1,filtro(0,0))).status());
    }
}
