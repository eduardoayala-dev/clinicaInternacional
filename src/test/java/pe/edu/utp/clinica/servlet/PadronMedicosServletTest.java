package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.PadronMedicosDAO;
import pe.edu.utp.clinica.filter.CsrfFilter;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PadronMedicosServletTest {
    private static final PadronMedicosDAO.Medico MEDICO=new PadronMedicosDAO.Medico(4,"=Nombre de prueba","001234",1,"Cardiología","San Borja","ACTIVO");
    private static class Servlet extends PadronMedicosServlet {
        int lecturas,escrituras;
        PadronMedicosDAO.Filtro filtro;
        @Override protected List<PadronMedicosDAO.Medico> listar(int actor,PadronMedicosDAO.Filtro filtro) { assertEquals(17,actor);lecturas++;this.filtro=filtro;return List.of(MEDICO); }
        @Override protected PadronMedicosDAO.Edicion consultar(int actor,int id) { assertEquals(17,actor);assertEquals(4,id);lecturas++;return new PadronMedicosDAO.Edicion(MEDICO,List.of()); }
        @Override protected void guardar(int actor,int id,int especialidad,String estado,String ip) { assertEquals(17,actor);assertEquals(4,id);assertEquals(1,especialidad);assertEquals("LICENCIA",estado);escrituras++; }
    }
    private HttpExchange peticion(String ruta,String rol) {
        var x=new HttpExchange();x.path=ruta;x.headers.put("Accept","application/json");
        x.session.setAttribute("usuarioLogueado",new Usuario(17,"Admision","Prueba","a@example.test","",rol,null,true));return x;
    }
    @Test void exportaArchivoRealConFiltrosYTextoSinFormulas()throws Exception {
        var x=peticion("/admin/medicos/exportar","PERSONAL_ADMINISTRATIVO");var servlet=new Servlet();
        x.parameters.putAll(java.util.Map.of("query","Pérez","especialidad","Cardiología","sede","San Borja","estado","Todos"));
        servlet.doGet(x.request,x.response);
        assertEquals(200,x.status);assertEquals("Pérez",servlet.filtro.buscar());assertEquals("",servlet.filtro.estado());
        assertTrue(x.contentType.contains("spreadsheetml"));
        try(var libro=new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(x.binary.toByteArray()))) {
            var hoja=libro.getSheetAt(0);assertEquals(1,hoja.getLastRowNum());
            assertEquals("001234",hoja.getRow(1).getCell(0).getStringCellValue());
            assertEquals(org.apache.poi.ss.usermodel.CellType.STRING,hoja.getRow(1).getCell(1).getCellType());
            assertEquals("=Nombre de prueba",hoja.getRow(1).getCell(1).getStringCellValue());
        }
    }
    @Test void rolAjenoNoDescargaNiEdita() {
        for(String rol:List.of("PACIENTE","MEDICO","ADMINISTRADOR")) {
            var x=peticion("/admin/medicos/exportar",rol);var servlet=new Servlet();servlet.doGet(x.request,x.response);assertEquals(403,x.status);assertEquals(0,servlet.lecturas);
        }
    }
    @Test void consultaYActualizacionMantienenContratoEIdentidadDeSesion() {
        var x=peticion("/admin/medicos/editar","PERSONAL_ADMINISTRATIVO");var servlet=new Servlet();x.parameters.put("id","4");servlet.doGet(x.request,x.response);
        assertTrue(x.body.toString().contains("MEDICO_OBTENIDO"));
        x.parameters.putAll(java.util.Map.of("especialidad","1","estado","LICENCIA","idUsuario","999"));servlet.doPost(x.request,x.response);
        assertEquals(1,servlet.escrituras);assertTrue(x.body.toString().contains("MEDICO_ACTUALIZADO"));
    }
    @Test void parametrosRepetidosOIdentificadorInvalidoSeRechazan() {
        var x=peticion("/admin/medicos/editar","PERSONAL_ADMINISTRATIVO");var servlet=new Servlet();x.multipleParameters.put("id",new String[]{"4","5"});servlet.doPost(x.request,x.response);
        assertEquals(400,x.status);assertEquals(0,servlet.escrituras);
        x.multipleParameters.clear();x.parameters.put("id","0");servlet.doGet(x.request,x.response);assertEquals(400,x.status);assertEquals(0,servlet.lecturas);
    }
    @Test void csrfInvalidoBloqueaLaEdicion()throws Exception {
        var x=peticion("/admin/medicos/editar","PERSONAL_ADMINISTRATIVO");var servlet=new Servlet();
        new CsrfFilter().doFilter(x.request,x.response,(req,res)->servlet.doPost(x.request,x.response));
        assertEquals(403,x.status);assertEquals(0,servlet.escrituras);
    }
    @Test void postDeExportacionNoModificaDatos() {
        var x=peticion("/admin/medicos/exportar","PERSONAL_ADMINISTRATIVO");var servlet=new Servlet();servlet.doPost(x.request,x.response);assertEquals(405,x.status);assertEquals(0,servlet.escrituras);
    }
}
