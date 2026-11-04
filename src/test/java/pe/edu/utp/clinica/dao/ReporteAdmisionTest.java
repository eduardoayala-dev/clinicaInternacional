package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.*;
import pe.edu.utp.clinica.reporte.FiltroReporte;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class ReporteAdmisionTest {
    private CitaAutorizacionTest esquema;
    protected CitaAutorizacionTest crearEsquema(){return new CitaAutorizacionTest();}
    private java.sql.Connection abrir() throws java.sql.SQLException{return esquema.abrir();}
    private Clock reloj(){return esquema.reloj();}
    private ReporteAdmisionDAO reportes;
    private FiltroReporte criterios;
    private void ejecutar(String sql)throws Exception{try(var c=abrir();var s=c.createStatement()){s.execute(sql);}}
    private FiltroReporte filtro(){return new FiltroReporte(LocalDate.of(2026,10,28),LocalDate.of(2026,10,28),"","citas");}
    @BeforeEach void prepararReportes() throws Exception {
        esquema=crearEsquema();esquema.setUp();criterios=filtro();
        reportes=new ReporteAdmisionDAO(this::abrir,reloj());
        ejecutar("INSERT INTO usuario(id_usuario,nombres,rol) VALUES(40,'Admisión','PERSONAL_ADMINISTRATIVO'),(41,'Otra sede','PERSONAL_ADMINISTRATIVO')");
        ejecutar("INSERT INTO sede(id_sede,nombre) VALUES(2,'Otra sede')");
        ejecutar("INSERT INTO personal_administrativo(id_personal,id_usuario,id_sede) VALUES(1,40,1),(2,41,2)");
        ejecutar("UPDATE medico SET id_sede=2 WHERE id_medico=2");
        ejecutar("ALTER TABLE reasignacion_inteligente ADD COLUMN id_cita_original INT");
        ejecutar("ALTER TABLE reasignacion_inteligente ADD COLUMN id_disponibilidad INT");
        ejecutar("ALTER TABLE reasignacion_inteligente ADD COLUMN fecha_limite_respuesta DATETIME");
        ejecutar("UPDATE cita SET riesgo_cancelacion='ALTO' WHERE id_cita=7");
        ejecutar("INSERT INTO cita(id_cita,id_paciente,id_medico,fecha_hora,estado) VALUES(8,1,2,'2026-10-28 09:00:00','CONFIRMADA'),(9,1,1,'2026-10-29 00:00:00','CONFIRMADA'),(10,1,1,'2026-10-28 23:59:59','CANCELADA')");
    }
    @Test void datosYResumenRespetanSedeEstadoYLimitesDelDia() {
        var r=reportes.consultar(40,filtro(),1,false);
        assertEquals("Central",r.sede());assertEquals(2,r.resumen().total());assertEquals(1,r.resumen().alto());assertEquals(1,r.resumen().canceladas());assertEquals(2,r.citas().size());assertEquals(7,r.citas().get(0).id());assertNull(r.resumen().esperaMedia());
        var otra=reportes.consultar(41,filtro(),1,false);assertEquals(1,otra.resumen().total());assertEquals(8,otra.citas().get(0).id());
        var canceladas=new FiltroReporte(filtro().desde(),filtro().hasta(),"CANCELADA","citas");
        assertEquals(1,reportes.consultar(40,canceladas,1,true).citas().size());
    }
    @Test void laSedeHistoricaDelHorarioPrevaleceSobreLaAsignacionActualDelMedico() throws Exception {
        ejecutar("INSERT INTO disponibilidad(id_disponibilidad,id_medico,id_sede,fecha,hora_inicio,hora_fin,duracion_minutos,estado) VALUES(50,1,2,'2026-10-28','08:00:00','12:00:00',30,'PUBLICADO')");
        ejecutar("UPDATE cita SET id_disponibilidad=50 WHERE id_cita=7");
        assertEquals(1,reportes.consultar(40,filtro(),1,true).resumen().total());
        assertTrue(reportes.consultar(41,filtro(),1,true).citas().stream().anyMatch(c->c.id()==7));
    }
    @Test void usuarioDeshabilitadoPacienteYPersonalSinSedeNoObtienenDatos() throws Exception {
        assertEquals(403,assertThrows(AgendaException.class,()->reportes.consultar(10,criterios,1,true)).status());
        ejecutar("UPDATE usuario SET activo=FALSE WHERE id_usuario=40");
        assertEquals(403,assertThrows(AgendaException.class,()->reportes.consultar(40,criterios,1,true)).status());
        ejecutar("UPDATE sede SET estado='INACTIVO' WHERE id_sede=2");
        assertEquals(403,assertThrows(AgendaException.class,()->reportes.consultar(41,criterios,1,true)).status());
    }
    @Test void esperaYReasignacionesSeCalculanDesdeEventosReales() throws Exception {
        ejecutar("INSERT INTO admision_cita VALUES(7,40,'2026-10-28 08:55:00',10,FALSE)");
        ejecutar("INSERT INTO atencion_cita(id_cita,id_medico,id_paciente,fecha_inicio) VALUES(7,1,1,'2026-10-28 09:10:00')");
        ejecutar("INSERT INTO reasignacion_inteligente(id_reasignacion,id_cita_original,id_cita_asignada,estado) VALUES(1,10,7,'ACEPTADA'),(2,10,NULL,'RECHAZADA')");
        var r=reportes.consultar(40,filtro(),1,false);assertEquals(15.0,r.resumen().esperaMedia(),0.01);assertEquals(2,r.resumen().ofertas());assertEquals(1,r.resumen().aceptadas());
    }
    @Test void contactoPreventivoExcluyeConfirmadasYCanceladas() throws Exception {
        var f=new FiltroReporte(filtro().desde(),filtro().hasta(),"","contacto");
        assertEquals(1,reportes.consultar(40,f,1,true).citas().size());
        ejecutar("UPDATE confirmacion_asistencia SET confirmada_en='2026-10-28 09:30:00' WHERE id_cita=7");
        assertTrue(reportes.consultar(40,f,1,true).citas().isEmpty());
    }
    @Test void paginacionNoTruncaLaExportacionYLimitaArchivosGrandes() throws Exception {
        try(var c=abrir();var ps=c.prepareStatement("INSERT INTO cita(id_cita,id_paciente,id_medico,fecha_hora,estado) VALUES(?,1,1,'2026-10-28 12:00:00','CONFIRMADA')")) {
            for(int i=100;i<121;i++){ps.setInt(1,i);ps.addBatch();}ps.executeBatch();
        }
        assertEquals(20,reportes.consultar(40,filtro(),1,false).citas().size());assertEquals(3,reportes.consultar(40,filtro(),2,false).citas().size());assertEquals(23,reportes.consultar(40,filtro(),1,true).citas().size());
        try(var c=abrir();var ps=c.prepareStatement("INSERT INTO cita(id_cita,id_paciente,id_medico,fecha_hora,estado) VALUES(?,1,1,'2026-10-28 12:00:00','CONFIRMADA')")) {
            for(int i=1000;i<3000;i++){ps.setInt(1,i);ps.addBatch();}ps.executeBatch();
        }
        assertEquals(422,assertThrows(AgendaException.class,()->reportes.consultar(40,criterios,1,true)).status());
        var f=new FiltroReporte(filtro().desde(),filtro().hasta(),"","indicadores");
        var indicadores=reportes.consultar(40,f,1,true);assertEquals(2023,indicadores.resumen().total());assertTrue(indicadores.citas().isEmpty());
    }
    @Test void ocupacionUsaBloquesPublicadosYCuposSinDuplicar() throws Exception {
        ejecutar("INSERT INTO disponibilidad(id_disponibilidad,id_medico,id_sede,fecha,hora_inicio,hora_fin,duracion_minutos,estado) VALUES(60,1,1,'2026-10-28','09:00:00','11:00:00',30,'PUBLICADO'),(61,1,2,'2026-10-28','09:00:00','11:00:00',30,'PUBLICADO')");
        ejecutar("UPDATE cita SET id_disponibilidad=60,fecha_hora='2026-10-28 09:00:00' WHERE id_cita=7");
        var r=reportes.consultar(40,filtro(),1,false);
        assertEquals(4,r.ocupacion().capacidad());assertEquals(1,r.ocupacion().ocupados());
        ejecutar("UPDATE cita SET estado='CANCELADA' WHERE id_cita=7");
        assertEquals(0,reportes.consultar(40,filtro(),1,false).ocupacion().ocupados());
    }
    @Test void pendientesExcluyenVencidasYOtrasSedes() throws Exception {
        ejecutar("INSERT INTO disponibilidad(id_disponibilidad,id_medico,id_sede,fecha,hora_inicio,hora_fin,duracion_minutos,estado) VALUES(60,1,1,'2026-10-28','20:00:00','23:59:00',30,'PUBLICADO')");
        ejecutar("INSERT INTO reasignacion_inteligente(id_cita_original,id_disponibilidad,estado,fecha_limite_respuesta) VALUES(10,60,'PENDIENTE','2026-10-28 23:59:59')");
        assertEquals(1,reportes.consultar(40,filtro(),1,false).ocupacion().ofertasPendientes());
        assertEquals(0,reportes.consultar(41,filtro(),1,false).ocupacion().ofertasPendientes());
        ejecutar("UPDATE reasignacion_inteligente SET fecha_limite_respuesta='2026-10-27 00:00:00'");
        assertEquals(0,reportes.consultar(40,filtro(),1,false).ocupacion().ofertasPendientes());
    }
    @Test void falloDeBaseNoSePresentaComoUnReporteVacio() throws Exception {
        ejecutar("DROP TABLE reasignacion_inteligente");assertEquals(503,assertThrows(AgendaException.class,()->reportes.consultar(40,criterios,1,true)).status());
    }
}
