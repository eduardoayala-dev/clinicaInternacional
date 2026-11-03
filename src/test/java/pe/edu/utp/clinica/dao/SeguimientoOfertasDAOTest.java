package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SeguimientoOfertasDAOTest {
    private String url;
    protected Connection abrir()throws SQLException{return DriverManager.getConnection(url);}
    private void sql(String texto)throws SQLException{try(var c=abrir();var s=c.createStatement()){s.execute(texto);}}
    private SeguimientoOfertasDAO dao(){return new SeguimientoOfertasDAO(this::abrir,Clock.fixed(Instant.parse("2026-11-02T15:00:00Z"),ZoneId.of("America/Lima")));}
    private SeguimientoOfertasDAO.Filtro todos(){return new SeguimientoOfertasDAO.Filtro("",null,0,1);}
    @BeforeEach void preparar()throws Exception {
        url="jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try(var c=abrir()){EsquemaPruebas.limpiar(c);}
        sql("CREATE TABLE usuario(id_usuario INT PRIMARY KEY,nombres VARCHAR(100),apellidos VARCHAR(100),rol VARCHAR(30),activo BOOLEAN DEFAULT TRUE)");
        sql("CREATE TABLE sede(id_sede INT PRIMARY KEY,nombre VARCHAR(60),estado VARCHAR(20))");
        sql("CREATE TABLE personal_administrativo(id_usuario INT,id_sede INT)");
        sql("CREATE TABLE especialidad(id_especialidad INT PRIMARY KEY,nombre VARCHAR(60))");
        sql("CREATE TABLE medico(id_medico INT PRIMARY KEY,id_usuario INT,id_especialidad INT,id_sede INT)");
        sql("CREATE TABLE paciente(id_paciente INT PRIMARY KEY,id_usuario INT)");
        sql("CREATE TABLE lista_espera(id_lista_espera INT PRIMARY KEY,id_paciente INT)");
        sql("CREATE TABLE disponibilidad(id_disponibilidad INT PRIMARY KEY,id_sede INT)");
        sql("CREATE TABLE cita(id_cita INT PRIMARY KEY,id_medico INT,fecha_hora DATETIME)");
        sql("CREATE TABLE reasignacion_inteligente(id_reasignacion INT PRIMARY KEY,id_cita_original INT,id_lista_espera INT,id_disponibilidad INT,fecha_oferta DATETIME,fecha_limite_respuesta DATETIME,estado VARCHAR(20),id_cita_asignada INT)");
        sql("INSERT INTO usuario VALUES(1,'Paciente','Uno','PACIENTE',TRUE),(2,'Admisión','Uno','PERSONAL_ADMINISTRATIVO',TRUE),(3,'Admisión','Dos','PERSONAL_ADMINISTRATIVO',TRUE),(4,'Médico','Uno','MEDICO',TRUE)");
        sql("INSERT INTO sede VALUES(1,'San Borja','ACTIVO'),(2,'Otra sede','ACTIVO')");
        sql("INSERT INTO personal_administrativo VALUES(2,1),(3,2)");
        sql("INSERT INTO paciente VALUES(1,1)");sql("INSERT INTO lista_espera VALUES(1,1)");
        sql("INSERT INTO especialidad VALUES(1,'Cardiología'),(2,'General')");
        sql("INSERT INTO medico VALUES(1,4,1,1)");sql("INSERT INTO disponibilidad VALUES(1,1),(2,2)");
        sql("INSERT INTO cita VALUES(7,1,'2026-11-03 10:00:00')");
        sql("INSERT INTO reasignacion_inteligente VALUES(1,7,1,1,'2026-11-01 09:00:00','2026-11-02 10:00:00','PENDIENTE',NULL),(2,7,1,2,'2026-11-02 09:00:00','2026-11-02 11:00:00','ACEPTADA',20)");
    }
    @Test void muestraSoloOfertasDeLaSedeAutorizadaYNoTotalesGlobales() {
        var panel=dao().consultar(2,todos());assertEquals("San Borja",panel.sede());assertEquals(1,panel.total());
        assertEquals(1,panel.ofertas().get(0).id());assertEquals(0L,panel.estados().get("ACEPTADA"));
        assertEquals(2,dao().consultar(3,todos()).ofertas().get(0).id());
    }
    @Test void usaLaSedeDelBloqueDeOfertaAunqueCambieElMedico()throws Exception {
        sql("UPDATE medico SET id_sede=2");assertEquals(1,dao().consultar(2,todos()).total());
    }
    @Test void combinaFiltrosYRespetaLimitesDelDiaDeOferta()throws Exception {
        sql("INSERT INTO reasignacion_inteligente VALUES(3,7,1,1,'2026-11-01 23:59:59','2026-11-02 10:00:00','ACEPTADA',21),(4,7,1,1,'2026-11-02 00:00:00','2026-11-02 11:00:00','ACEPTADA',22)");
        var panel=dao().consultar(2,new SeguimientoOfertasDAO.Filtro("ACEPTADA",LocalDate.of(2026,11,1),1,1));
        assertEquals(1,panel.total());assertEquals(3,panel.ofertas().get(0).id());assertEquals(21,panel.ofertas().get(0).citaAsignada());
        assertEquals(0,dao().consultar(2,new SeguimientoOfertasDAO.Filtro("ACEPTADA",null,2,1)).total());
    }
    @Test void paginacionDeterministaSinFilasRepetidasYRecuperaPaginaFueraDeRango()throws Exception {
        for(int i=3;i<=20;i++)sql("INSERT INTO reasignacion_inteligente VALUES("+i+",7,1,1,'2026-11-01 09:00:00','2026-11-02 11:00:00','RECHAZADA',NULL)");
        var primera=dao().consultar(2,todos());var segunda=dao().consultar(2,new SeguimientoOfertasDAO.Filtro("",null,0,2));
        assertEquals(19,primera.total());assertEquals(15,primera.ofertas().size());assertEquals(4,segunda.ofertas().size());
        assertTrue(primera.ofertas().stream().noneMatch(x->segunda.ofertas().stream().anyMatch(y->x.id()==y.id())));
        assertEquals(20,primera.ofertas().get(0).id());assertEquals(2,dao().consultar(2,new SeguimientoOfertasDAO.Filtro("",null,0,999)).pagina());
    }
    @Test void plazoVencidoSeEtiquetaSinMutarElResultadoGuardado() {
        var fila=dao().consultar(2,todos()).ofertas().get(0);assertTrue(fila.plazoVencido());assertEquals("PENDIENTE",fila.estado());
        assertEquals("PENDIENTE",dao().consultar(2,todos()).ofertas().get(0).estado());
    }
    @Test void datosVaciosSonCeroSinRegistrosDeDemostracion()throws Exception {
        sql("DELETE FROM reasignacion_inteligente");var panel=dao().consultar(2,todos());
        assertEquals(0,panel.total());assertTrue(panel.ofertas().isEmpty());assertEquals(1,panel.paginas());assertTrue(panel.especialidades().isEmpty());
    }
    @Test void cuentasInactivasOtrosRolesYSedesInactivasNoAcceden()throws Exception {
        for(int actor:new int[]{1,4,99})assertEquals(403,assertThrows(AgendaException.class,()->dao().consultar(actor,todos())).status());
        sql("UPDATE usuario SET activo=FALSE WHERE id_usuario=2");assertEquals(403,assertThrows(AgendaException.class,()->dao().consultar(2,todos())).status());
        sql("UPDATE usuario SET activo=TRUE WHERE id_usuario=2");sql("UPDATE sede SET estado='INACTIVO' WHERE id_sede=1");
        assertEquals(403,assertThrows(AgendaException.class,()->dao().consultar(2,todos())).status());
    }
    @Test void noAceptaEstadoNiPaginaManipulados() {
        assertThrows(AgendaException.class,()->new SeguimientoOfertasDAO.Filtro("' OR 1=1",null,0,1));
        assertThrows(AgendaException.class,()->new SeguimientoOfertasDAO.Filtro("",null,0,0));
    }
    @Test void nuevasConsultasReflejanElResultadoAceptadoSinConfundirloConAsistencia()throws Exception {
        sql("UPDATE reasignacion_inteligente SET estado='ACEPTADA',id_cita_asignada=21 WHERE id_reasignacion=1");
        var panel=dao().consultar(2,todos());assertEquals(1L,panel.estados().get("ACEPTADA"));
        assertEquals(0L,panel.estados().get("PENDIENTE"));assertEquals(21,panel.ofertas().get(0).citaAsignada());
        assertFalse(panel.ofertas().get(0).plazoVencido());
    }
    @Test void registroAntiguoSinEstadoSeIdentificaSinInventarUnResultado()throws Exception {
        sql("UPDATE reasignacion_inteligente SET estado=NULL WHERE id_reasignacion=1");
        var panel=dao().consultar(2,todos());assertEquals(1,panel.total());
        assertEquals("SIN_ESTADO",panel.ofertas().get(0).estado());assertEquals(0L,panel.estados().get("ACEPTADA"));
    }
}
