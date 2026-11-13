package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.*;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.reporte.FiltroReporte;
import pe.edu.utp.clinica.support.HttpExchange;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExportServletTest {
    private static class Reportes extends ReporteAdmisionDAO {
        int llamadas;AgendaException error;
        @Override public Reporte consultar(int usuario,FiltroReporte f,int pagina,boolean exportar) {
            llamadas++;assertEquals(40,usuario);assertTrue(exportar);assertEquals(LocalDate.of(2026,10,28),f.desde());
            if(error!=null)throw error;
            return new Reporte("Sede autorizada",f,new Resumen(0,0,0,0,0,0,0,0,0,null,0,0,0),List.of(),1,1,f.desde().atTime(10,0));
        }
    }
    private HttpExchange solicitud(){var x=new HttpExchange();x.method="GET";x.parameters.put("desde","2026-10-28");x.parameters.put("hasta","2026-10-28");x.parameters.put("format","pdf");x.parameters.put("idSede","999");x.session.setAttribute("usuarioLogueado",new Usuario(40,"Personal","Prueba","p@test","","PERSONAL_ADMINISTRATIVO",null,true));return x;}
    private ExportServlet servlet(Reportes dao){return new ExportServlet(){@Override protected ReporteAdmisionDAO reportes(){return dao;}};}
    @Test void descargaBinariosConFormatoCorrectoYNoUsaLaSedeSolicitada() {
        for(String formato:List.of("csv","pdf","xlsx")) {
            var x=solicitud();var dao=new Reportes();x.parameters.put("format",formato);servlet(dao).doGet(x.request,x.response);assertEquals(200,x.status);assertEquals(1,dao.llamadas);assertEquals("no-store",x.responseHeaders.get("Cache-Control"));assertTrue(x.responseHeaders.get("Content-Disposition").contains("."+formato));assertTrue(x.binary.size()>0);
            if("pdf".equals(formato))assertTrue(x.binary.toString(java.nio.charset.StandardCharsets.ISO_8859_1).startsWith("%PDF-"));
        }
    }
    @Test void rolesFiltrosYFormatosInvalidosNoConsultanDatos() {
        var dao=new Reportes();var x=solicitud();x.session.removeAttribute("usuarioLogueado");servlet(dao).doGet(x.request,x.response);assertEquals(403,x.status);
        x=solicitud();x.session.setAttribute("usuarioLogueado",new Usuario(40,"Paciente","Prueba","p@test","","PACIENTE",null,true));servlet(dao).doGet(x.request,x.response);assertEquals(403,x.status);
        x=solicitud();x.parameters.put("format","html");servlet(dao).doGet(x.request,x.response);assertEquals(400,x.status);
        x=solicitud();x.parameters.put("desde","ayer");servlet(dao).doGet(x.request,x.response);assertEquals(400,x.status);assertEquals(0,dao.llamadas);
    }
    @Test void errorDeDatosNoDescargaUnArchivoDeExitoVacio() {
        var x=solicitud();var dao=new Reportes();dao.error=new AgendaException(503,"REPORTE_NO_DISPONIBLE","No disponible");servlet(dao).doGet(x.request,x.response);assertEquals(503,x.status);assertEquals(0,x.binary.size());assertTrue(x.body.toString().contains("REPORTE_NO_DISPONIBLE"));
    }
}
