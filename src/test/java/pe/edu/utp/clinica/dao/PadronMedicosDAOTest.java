package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PadronMedicosDAOTest {
    private String url;
    private PadronMedicosDAO dao;
    protected String urlPruebas() { return "jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1"; }
    protected Connection abrir()throws SQLException { return DriverManager.getConnection(url); }
    private void sql(String texto)throws SQLException { try(var c=abrir();var s=c.createStatement()) { s.execute(texto); } }
    private String valor(String texto)throws SQLException { try(var c=abrir();var s=c.createStatement();var r=s.executeQuery(texto)) { r.next();return r.getString(1); } }
    @BeforeEach void preparar()throws SQLException {
        url=urlPruebas();
        try(var c=abrir()) { EsquemaPruebas.limpiar(c); }
        sql("CREATE TABLE usuario(id_usuario INT PRIMARY KEY,nombres VARCHAR(100),apellidos VARCHAR(100),rol VARCHAR(40),activo BOOLEAN)");
        sql("CREATE TABLE sede(id_sede INT PRIMARY KEY,nombre VARCHAR(100),estado VARCHAR(20))");
        sql("CREATE TABLE personal_administrativo(id_usuario INT PRIMARY KEY,id_sede INT)");
        sql("CREATE TABLE especialidad(id_especialidad INT PRIMARY KEY,nombre VARCHAR(100),estado VARCHAR(20))");
        sql("CREATE TABLE medico(id_medico INT PRIMARY KEY,id_usuario INT,cmp VARCHAR(20),id_especialidad INT,id_sede INT,estado VARCHAR(20))");
        sql("CREATE TABLE disponibilidad(id_medico INT,estado VARCHAR(20),fecha DATE)");
        sql("CREATE TABLE cita(id_cita INT PRIMARY KEY,id_medico INT,estado VARCHAR(20),fecha_hora DATETIME)");
        sql("CREATE TABLE auditoria_seguridad(id_usuario INT,ip_origen VARCHAR(45),evento VARCHAR(100),descripcion VARCHAR(500),sospechoso BOOLEAN)");
        sql("INSERT INTO usuario VALUES(1,'Admisión','Uno','PERSONAL_ADMINISTRATIVO',TRUE),(2,'Ana','Pérez','MEDICO',TRUE),(3,'Luis','Gómez','MEDICO',TRUE),(4,'Paciente','Prueba','PACIENTE',TRUE)");
        sql("INSERT INTO sede VALUES(1,'San Borja','ACTIVO'),(2,'Otra sede','ACTIVO')");
        sql("INSERT INTO personal_administrativo VALUES(1,1)");
        sql("INSERT INTO especialidad VALUES(1,'Cardiología','ACTIVO'),(2,'Medicina general','ACTIVO'),(3,'Inactiva','INACTIVO')");
        sql("INSERT INTO medico VALUES(1,2,'012345',1,1,'ACTIVO'),(2,3,'098765',1,2,'ACTIVO')");
        dao=new PadronMedicosDAO(this::abrir);
    }
    private PadronMedicosDAO.Filtro filtro(String buscar,String estado) { return new PadronMedicosDAO.Filtro(buscar,"","",estado); }
    @Test void exportacionRespetaSedeYFiltrosInclusoConTodos() {
        assertEquals(1,dao.listar(1,filtro("","Todos")).size());
        assertEquals("012345",dao.listar(1,filtro("Ana Pérez","ACTIVO")).get(0).cmp());
        assertTrue(dao.listar(1,filtro("Luis","ACTIVO")).isEmpty());
        assertTrue(dao.listar(1,new PadronMedicosDAO.Filtro("","","Otra sede","")).isEmpty());
        assertTrue(dao.listar(1,filtro("%","")).isEmpty());
    }
    @Test void permisosSeRevalidanEnLaBase()throws SQLException {
        assertEquals(403,assertThrows(AgendaException.class,()->dao.listar(4,filtro("",""))).status());
        sql("UPDATE usuario SET activo=FALSE WHERE id_usuario=1");
        assertEquals(403,assertThrows(AgendaException.class,()->dao.guardar(1,1,1,"LICENCIA","127.0.0.1")).status());
    }
    @Test void medicoDeOtraSedeNoPuedeLeerseNiModificarse() {
        assertEquals(404,assertThrows(AgendaException.class,()->dao.consultar(1,2)).status());
        assertEquals(404,assertThrows(AgendaException.class,()->dao.guardar(1,2,1,"LICENCIA","127.0.0.1")).status());
    }
    @Test void guardaPerfilYAuditoriaEnUnaTransaccion()throws SQLException {
        dao.guardar(1,1,2,"LICENCIA","127.0.0.1");
        assertEquals("2",valor("SELECT id_especialidad FROM medico WHERE id_medico=1"));
        assertEquals("LICENCIA",valor("SELECT estado FROM medico WHERE id_medico=1"));
        assertEquals("1",valor("SELECT COUNT(*) FROM auditoria_seguridad WHERE id_usuario=1"));
        dao.guardar(1,1,2,"LICENCIA","127.0.0.1");
        assertEquals("1",valor("SELECT COUNT(*) FROM auditoria_seguridad"));
    }
    @Test void preservaLaEspecialidadDeLasCitasHistoricas()throws SQLException {
        sql("INSERT INTO cita VALUES(1,1,'ATENDIDA',TIMESTAMP '2020-01-01 10:00:00')");
        assertEquals("HISTORIAL_PROTEGIDO",assertThrows(AgendaException.class,()->dao.guardar(1,1,2,"ACTIVO","127.0.0.1")).code());
        assertEquals("1",valor("SELECT id_especialidad FROM medico WHERE id_medico=1"));
    }
    @Test void horariosPublicadosImpidenLaBaja()throws SQLException {
        sql("INSERT INTO disponibilidad VALUES(1,'PUBLICADO',DATE '2099-01-01')");
        assertEquals("MEDICO_CON_AGENDA",assertThrows(AgendaException.class,()->dao.guardar(1,1,1,"INACTIVO","127.0.0.1")).code());
    }
    @Test void citasFuturasYEnAtencionImpidenLicencia()throws SQLException {
        sql("INSERT INTO cita VALUES(1,1,'CONFIRMADA',TIMESTAMP '2099-01-01 10:00:00')");
        assertEquals(409,assertThrows(AgendaException.class,()->dao.guardar(1,1,1,"LICENCIA","127.0.0.1")).status());
        sql("UPDATE cita SET estado='EN_ATENCION',fecha_hora=TIMESTAMP '2020-01-01 10:00:00'");
        assertEquals(409,assertThrows(AgendaException.class,()->dao.guardar(1,1,1,"INACTIVO","127.0.0.1")).status());
    }
    @Test void noActivaUnaCuentaDeshabilitada()throws SQLException {
        sql("UPDATE usuario SET activo=FALSE WHERE id_usuario=2");
        assertEquals("CUENTA_INACTIVA",assertThrows(AgendaException.class,()->dao.guardar(1,1,1,"ACTIVO","127.0.0.1")).code());
    }
    @Test void especialidadInactivaYEstadoInvalidoNoSeGuardan() {
        assertEquals(409,assertThrows(AgendaException.class,()->dao.guardar(1,1,3,"ACTIVO","127.0.0.1")).status());
        assertEquals(400,assertThrows(AgendaException.class,()->dao.guardar(1,1,1,"ADMINISTRADOR","127.0.0.1")).status());
    }
    @Test void falloDeAuditoriaRevierteElCambio()throws SQLException {
        sql("DROP TABLE auditoria_seguridad");
        assertEquals(503,assertThrows(AgendaException.class,()->dao.guardar(1,1,1,"LICENCIA","127.0.0.1")).status());
        assertEquals("ACTIVO",valor("SELECT estado FROM medico WHERE id_medico=1"));
    }
}
