package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.model.catalogo.FilaImportacionMotivo;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ImportacionMotivosDAOTest extends MotivosFixture {
    private FilaImportacionMotivo fila(int n,int especialidad,String texto){return new FilaImportacionMotivo(n,especialidad,texto,"");}
    @Test void previsualizaErroresPorFilaSinGuardar()throws Exception{
        dao.guardar(1,0,1,"Motivo existente","127.0.0.1");
        var filas=List.of(fila(2,1,"Control importado"),fila(3,1,"control importado"),fila(4,1,"Motivo existente"),fila(5,3,"Especialidad inactiva"),fila(6,1,"ab"));
        var vista=dao.revisarImportacion(1,filas);
        assertTrue(vista.get(0).error().isEmpty());for(int i=1;i<vista.size();i++)assertFalse(vista.get(i).error().isEmpty());
        assertEquals(1,cantidad("SELECT COUNT(*) FROM motivo_cita"));assertEquals(1,cantidad("SELECT COUNT(*) FROM auditoria_seguridad"));
    }
    @Test void importaTodasLasFilasYAuditaCadaAlta()throws Exception{
        assertEquals(2,dao.importar(1,List.of(fila(2,1,"Control importado"),fila(3,2,"Revisión de piel")),"127.0.0.1").importados());
        assertEquals(2,cantidad("SELECT COUNT(*) FROM motivo_cita"));assertEquals(2,cantidad("SELECT COUNT(*) FROM auditoria_seguridad WHERE evento='MOTIVO_IMPORTADO'"));
    }
    @Test void unaFilaDuplicadaNoBloqueaLasCorrectas()throws Exception{
        dao.guardar(1,0,1,"Motivo existente","127.0.0.1");
        var resultado=dao.importar(1,List.of(fila(2,1,"Debe persistir"),fila(3,1,"Motivo existente")),"127.0.0.1");
        assertEquals(1,resultado.importados());assertFalse(resultado.filas().get(1).error().isEmpty());
        assertEquals(2,cantidad("SELECT COUNT(*) FROM motivo_cita"));assertEquals(2,cantidad("SELECT COUNT(*) FROM auditoria_seguridad"));
    }
    @Test void revalidaLaEspecialidadAlConfirmar()throws Exception{
        var filas=dao.revisarImportacion(1,List.of(fila(2,1,"Control importado")));assertTrue(filas.get(0).error().isEmpty());
        sql("UPDATE especialidad SET estado='INACTIVO' WHERE id_especialidad=1");
        var resultado=dao.importar(1,filas,"127.0.0.1");assertEquals(0,resultado.importados());assertFalse(resultado.filas().get(0).error().isEmpty());assertEquals(0,cantidad("SELECT COUNT(*) FROM motivo_cita"));
    }
    @Test void falloDeAuditoriaRevierteElLote()throws Exception{
        sql("DROP TABLE auditoria_seguridad");assertEquals(503,assertThrows(AgendaException.class,()->dao.importar(1,List.of(fila(2,1,"Control importado")),"127.0.0.1")).status());assertEquals(0,cantidad("SELECT COUNT(*) FROM motivo_cita"));
    }
    @Test void pacienteNoPuedePrevisualizarNiConfirmar(){
        var filas=List.of(fila(2,1,"Control importado"));
        assertEquals(403,assertThrows(AgendaException.class,()->dao.revisarImportacion(3,filas)).status());assertEquals(403,assertThrows(AgendaException.class,()->dao.importar(3,filas,"127.0.0.1")).status());
    }
    @Test void dosImportacionesConcurrentesConservanLasFilasCorrectas()throws Exception{
        var pool=Executors.newFixedThreadPool(2);var inicio=new CountDownLatch(1);
        try{
            var resultados=new java.util.ArrayList<Future<Integer>>();
            for(int actor:new int[]{1,5})resultados.add(pool.submit(()->{inicio.await();try{return dao.importar(actor,List.of(fila(2,1,"Motivo simultáneo"),fila(3,2,"Exclusivo de "+actor)),"127.0.0.1").importados();}catch(AgendaException e){return e.status();}}));
            inicio.countDown();var estados=new java.util.HashSet<Integer>();for(var r:resultados)estados.add(r.get(15,TimeUnit.SECONDS));
            assertEquals(java.util.Set.of(1,2),estados);assertEquals(3,cantidad("SELECT COUNT(*) FROM motivo_cita"));assertEquals(3,cantidad("SELECT COUNT(*) FROM auditoria_seguridad"));
        }finally{pool.shutdownNow();}
    }
    @Test void admisionHabilitadaPuedeImportarSegunRF06()throws Exception{
        var resultado=dao.importar(2,List.of(fila(2,1,"Motivo importado por admisión")),"127.0.0.1");assertEquals(1,resultado.importados());assertEquals(1,cantidad("SELECT COUNT(*) FROM auditoria_seguridad WHERE id_usuario=2"));
    }
}
