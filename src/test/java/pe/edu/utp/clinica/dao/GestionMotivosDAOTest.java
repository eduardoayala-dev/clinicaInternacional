package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.*;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class GestionMotivosDAOTest extends MotivosFixture {
    @Test void creaEditaDesactivaYAuditaSinEliminar()throws Exception{
        int id=dao.guardar(1,0,1,"  Control   general  ","127.0.0.1");
        dao.guardar(1,id,1,"Control preventivo","127.0.0.1");dao.estado(1,id,"INACTIVO","127.0.0.1");
        assertEquals(0,new MotivoCitaDAO(this::abrir).listarActivos().size());
        assertEquals(1,cantidad("SELECT COUNT(*) FROM motivo_cita"));assertEquals(3,cantidad("SELECT COUNT(*) FROM auditoria_seguridad"));
        dao.estado(1,id,"ACTIVO","127.0.0.1");assertEquals(1,new MotivoCitaDAO(this::abrir).listarActivos().size());
    }
    @Test void preservaDescripcionYRelacionConCitas()throws Exception{
        int id=dao.guardar(1,0,1,"Evaluación general","127.0.0.1");sql("INSERT INTO cita VALUES(1,"+id+")");
        assertEquals("MOTIVO_CON_HISTORIAL",assertThrows(AgendaException.class,()->dao.guardar(1,id,1,"Texto diferente","127.0.0.1")).code());
        dao.estado(1,id,"INACTIVO","127.0.0.1");assertEquals(1,cantidad("SELECT COUNT(*) FROM cita WHERE id_motivo="+id));
        assertEquals(1,cantidad("SELECT COUNT(*) FROM motivo_cita WHERE descripcion='Evaluación general'"));
    }
    @Test void impideDuplicadosInclusoInactivosYDejaUsarlosEnOtraEspecialidad(){
        int id=dao.guardar(1,0,1,"Control general","127.0.0.1");dao.estado(1,id,"INACTIVO","127.0.0.1");
        assertEquals(409,assertThrows(AgendaException.class,()->dao.guardar(1,0,1,"control general","127.0.0.1")).status());
        assertTrue(dao.guardar(1,0,2,"Control general","127.0.0.1")>0);
    }
    @Test void elCatalogoLoGestionaPersonalHabilitadoSegunRF06(){
        var filtro=new GestionMotivosDAO.Filtro(0,"","",1);
        assertFalse(dao.consultar(2,filtro).administrador());assertTrue(dao.consultar(1,filtro).administrador());
        assertTrue(dao.guardar(2,0,1,"Motivo de admisión","127.0.0.1")>0);
        for(int actor:new int[]{3,4,99})assertEquals(403,assertThrows(AgendaException.class,()->dao.guardar(actor,0,1,"Control general","127.0.0.1")).status());
        assertEquals(403,assertThrows(AgendaException.class,()->dao.consultar(3,filtro)).status());
    }
    @Test void controlaEspecialidadesInactivasYNoTrasladaIds(){
        int id=dao.guardar(1,0,1,"Control general","127.0.0.1");
        assertEquals("ESPECIALIDAD_INMUTABLE",assertThrows(AgendaException.class,()->dao.guardar(1,id,2,"Control general","127.0.0.1")).code());
        assertEquals(409,assertThrows(AgendaException.class,()->dao.guardar(1,0,3,"Control general","127.0.0.1")).status());
        assertEquals(404,assertThrows(AgendaException.class,()->dao.estado(1,999,"INACTIVO","127.0.0.1")).status());
    }
    @Test void auditoriaEsParteDeLaMismaTransaccion()throws Exception{
        sql("DROP TABLE auditoria_seguridad");assertEquals(503,assertThrows(AgendaException.class,()->dao.guardar(1,0,1,"Control general","127.0.0.1")).status());
        assertEquals(0,cantidad("SELECT COUNT(*) FROM motivo_cita"));
    }
    @Test void paginaYBuscaLiteralesSinConvertirComodines(){
        for(int i=0;i<23;i++)dao.guardar(1,0,1,"Control "+i,"127.0.0.1");
        dao.guardar(1,0,2,"Control 100%_local","127.0.0.1");
        var panel=dao.consultar(2,new GestionMotivosDAO.Filtro(1,"ACTIVO","",999));assertEquals(2,panel.filtro().pagina());assertEquals(3,panel.motivos().size());assertEquals(23,panel.total());
        assertEquals(1,dao.consultar(1,new GestionMotivosDAO.Filtro(0,"","%_",1)).total());
        assertEquals(0,dao.consultar(1,new GestionMotivosDAO.Filtro(0,"","' OR 1=1 --",1)).total());
    }
    @Test void rechazaTextoInvalidoSinGuardar()throws Exception{
        for(String texto:new String[]{"ab","<script>texto</script>","Control\nlocal","x".repeat(201)})assertEquals(400,assertThrows(AgendaException.class,()->dao.guardar(1,0,1,texto,"127.0.0.1")).status());
        assertEquals(0,cantidad("SELECT COUNT(*) FROM motivo_cita"));
    }
    @Test void administradoresConcurrentesNoCreanElMismoMotivo()throws Exception{
        var pool=Executors.newFixedThreadPool(2);var inicio=new CountDownLatch(1);
        try{
            java.util.List<Future<Integer>> resultados=new java.util.ArrayList<>();
            for(int actor:new int[]{1,5})resultados.add(pool.submit(()->{inicio.await();try{dao.guardar(actor,0,1,"Control concurrente","127.0.0.1");return 200;}catch(AgendaException e){return e.status();}}));
            inicio.countDown();var estados=new java.util.HashSet<Integer>();for(var f:resultados)estados.add(f.get(10,TimeUnit.SECONDS));
            assertEquals(java.util.Set.of(200,409),estados);assertEquals(1,cantidad("SELECT COUNT(*) FROM motivo_cita"));
        }finally{pool.shutdownNow();}
    }
    @Test void admisionConSedeInactivaNoPuedeModificar()throws Exception{
        sql("UPDATE sede SET estado='INACTIVO' WHERE id_sede=1");assertEquals(403,assertThrows(AgendaException.class,()->dao.guardar(2,0,1,"Control no autorizado","127.0.0.1")).status());
    }
}
