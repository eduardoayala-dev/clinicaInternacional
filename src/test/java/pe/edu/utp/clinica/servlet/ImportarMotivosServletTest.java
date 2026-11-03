package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.catalogo.*;
import pe.edu.utp.clinica.dao.*;
import pe.edu.utp.clinica.filter.CsrfFilter;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.model.catalogo.FilaImportacionMotivo;
import pe.edu.utp.clinica.support.HttpExchange;
import java.io.*;
import java.time.*;
import java.util.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

class ImportarMotivosServletTest {
    private static class Servlet extends ImportarMotivosServlet {
        byte[] archivo;int lecturas,guardados;
        @Override protected Clock reloj(){return Clock.fixed(Instant.parse("2026-11-03T18:00:00Z"),ZoneOffset.UTC);}
        @Override protected List<GestionMotivosDAO.Especialidad> especialidades(int actor){assertEquals(17,actor);return List.of(new GestionMotivosDAO.Especialidad(13,"Medicina General"));}
        @Override protected byte[] leerArchivo(jakarta.servlet.http.HttpServletRequest req){lecturas++;return archivo;}
        @Override protected List<FilaImportacionMotivo> revisar(int actor,List<FilaImportacionMotivo> filas){assertEquals(17,actor);return filas;}
        @Override protected GestionMotivosDAO.ResultadoImportacion guardar(int actor,List<FilaImportacionMotivo> filas,String ip){assertEquals(17,actor);assertEquals("Control original",filas.get(0).descripcion());guardados++;return new GestionMotivosDAO.ResultadoImportacion((int)filas.stream().filter(f->f.error().isEmpty()).count(),filas);}
    }
    private HttpExchange peticion(String rol){var x=new HttpExchange();x.path="/sysadmin/motivos/importar";x.headers.put("Accept","application/json");x.session.setAttribute("usuarioLogueado",new Usuario(17,"Admin","Prueba","admin@example.test","",rol,null,true));return x;}
    private byte[] archivo()throws Exception{
        try(var wb=new XSSFWorkbook(new ByteArrayInputStream(ImportacionMotivosExcel.plantilla(List.of(new GestionMotivosDAO.Especialidad(13,"Medicina General")))));var out=new ByteArrayOutputStream()){
            var fila=wb.getSheet("Motivos").createRow(1);fila.createCell(0).setCellValue(13);fila.createCell(1).setCellValue("Control original");wb.write(out);return out.toByteArray();
        }
    }
    @Test void vistaPreviaNoGuardaYConfirmacionUsaSoloLosDatosDelServidor()throws Exception{
        var servlet=new Servlet();servlet.archivo=archivo();var x=peticion("ADMINISTRADOR");x.parameters.put("accion","previsualizar");servlet.doPost(x.request,x.response);assertEquals(200,x.status);assertEquals(0,servlet.guardados);
        String token=new ObjectMapper().readTree(x.body.toString()).path("data").path("confirmacion").asText();assertFalse(token.isEmpty());
        var confirmar=peticion("ADMINISTRADOR");confirmar.session=x.session;confirmar.parameters.putAll(Map.of("accion","confirmar","confirmacion",token,"descripcion","Intento de reemplazo","idUsuario","99"));
        servlet.doPost(confirmar.request,confirmar.response);assertEquals(200,confirmar.status);assertEquals(1,servlet.guardados);
        servlet.doPost(confirmar.request,confirmar.response);assertEquals(409,confirmar.status);assertEquals(1,servlet.guardados);
    }
    @Test void archivoNuevoInvalidoDescartaConfirmacionAnterior()throws Exception{
        var servlet=new Servlet();servlet.archivo=archivo();var x=peticion("ADMINISTRADOR");x.parameters.put("accion","previsualizar");servlet.doPost(x.request,x.response);
        String token=new ObjectMapper().readTree(x.body.toString()).path("data").path("confirmacion").asText();
        servlet.archivo="No es un Excel".getBytes();servlet.doPost(x.request,x.response);assertEquals(422,x.status);
        var confirmar=peticion("ADMINISTRADOR");confirmar.session=x.session;confirmar.parameters.putAll(Map.of("accion","confirmar","confirmacion",token));servlet.doPost(confirmar.request,confirmar.response);assertEquals(409,confirmar.status);assertEquals(0,servlet.guardados);
    }
    @Test void pacienteNoPuedeSubirNiDescargarLaPlantilla(){var servlet=new Servlet();var x=peticion("PACIENTE");x.parameters.put("accion","previsualizar");servlet.doPost(x.request,x.response);assertEquals(403,x.status);assertEquals(0,servlet.lecturas);servlet.doGet(x.request,x.response);assertEquals(403,x.status);}
    @Test void descargaPlantillaSinCrearMotivos(){var servlet=new Servlet();var x=peticion("ADMINISTRADOR");x.parameters.put("accion","plantilla");servlet.doGet(x.request,x.response);assertEquals(200,x.status);assertTrue(x.binary.size()>0);assertTrue(x.responseHeaders.get("Content-Disposition").contains("plantilla-motivos.xlsx"));assertEquals(0,servlet.guardados);}
    @Test void csrfInvalidoNoLlegaALeerElArchivo()throws Exception{var servlet=new Servlet();var x=peticion("ADMINISTRADOR");new CsrfFilter().doFilter(x.request,x.response,(r,s)->servlet.doPost(x.request,x.response));assertEquals(403,x.status);assertEquals(0,servlet.lecturas);}
    @Test void formatoDeCargaIncorrectoNoProduceErrorInterno(){var x=peticion("ADMINISTRADOR");var error=assertThrows(AgendaException.class,()->new ImportarMotivosServlet().leerArchivo(x.request));assertEquals(415,error.status());}
    @Test void accionDuplicadaNoProcesaArchivo(){var servlet=new Servlet();var x=peticion("ADMINISTRADOR");x.multipleParameters.put("accion",new String[]{"previsualizar","confirmar"});servlet.doPost(x.request,x.response);assertEquals(400,x.status);assertEquals(0,servlet.lecturas);}
    @Test void admisionPuedeRevisarPorSuRutaSegunRF06()throws Exception{
        var servlet=new Servlet();servlet.archivo=archivo();var x=peticion("PERSONAL_ADMINISTRATIVO");x.path="/admin/motivos/importar";x.parameters.put("accion","previsualizar");servlet.doPost(x.request,x.response);assertEquals(200,x.status);assertEquals(1,servlet.lecturas);assertEquals(0,servlet.guardados);
    }
    @Test void filasConErrorNoBloqueanConfirmacionDeLasCorrectas()throws Exception{
        var servlet=new Servlet();try(var wb=new XSSFWorkbook(new ByteArrayInputStream(archivo()));var out=new ByteArrayOutputStream()){var r=wb.getSheet("Motivos").createRow(2);r.createCell(0).setCellValue(13);r.createCell(1).setCellFormula("1+1");wb.write(out);servlet.archivo=out.toByteArray();}
        var x=peticion("ADMINISTRADOR");x.parameters.put("accion","previsualizar");servlet.doPost(x.request,x.response);String token=new ObjectMapper().readTree(x.body.toString()).path("data").path("confirmacion").asText();assertFalse(token.isEmpty());
        var confirmar=peticion("ADMINISTRADOR");confirmar.session=x.session;confirmar.parameters.putAll(Map.of("accion","confirmar","confirmacion",token));servlet.doPost(confirmar.request,confirmar.response);
        var datos=new ObjectMapper().readTree(confirmar.body.toString()).path("data");assertEquals(1,datos.path("importados").asInt());assertFalse(datos.path("filas").get(1).path("error").asText().isEmpty());
    }
    @Test void resultadoHtmlUsaRedireccionYRefrescarNoRepiteImportacion()throws Exception{
        var servlet=new Servlet();servlet.archivo=archivo();var previa=peticion("ADMINISTRADOR");previa.parameters.put("accion","previsualizar");servlet.doPost(previa.request,previa.response);String token=new ObjectMapper().readTree(previa.body.toString()).path("data").path("confirmacion").asText();
        var confirmar=peticion("ADMINISTRADOR");confirmar.session=previa.session;confirmar.headers.put("Accept","text/html");confirmar.parameters.putAll(Map.of("accion","confirmar","confirmacion",token));servlet.doPost(confirmar.request,confirmar.response);assertEquals(303,confirmar.status);
        var resultado=peticion("ADMINISTRADOR");resultado.session=previa.session;resultado.parameters.put("resultado","1");servlet.doGet(resultado.request,resultado.response);servlet.doGet(resultado.request,resultado.response);assertEquals(1,servlet.guardados);assertTrue(resultado.body.toString().contains("IMPORTACION_RESULTADO"));assertFalse(resultado.body.toString().contains("todavía no se guardaron"));
    }
}
