package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.GestionMotivosDAO;
import pe.edu.utp.clinica.filter.CsrfFilter;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GestionMotivosServletTest {
    private static class Servlet extends GestionMotivosServlet {
        int lecturas,escrituras;
        @Override protected GestionMotivosDAO.Panel consultar(int actor,GestionMotivosDAO.Filtro filtro){assertEquals(17,actor);lecturas++;return new GestionMotivosDAO.Panel(false,true,List.of(),List.of(),filtro,0,1);}
        @Override protected int guardar(int actor,int id,int especialidad,String descripcion,String ip){assertEquals(17,actor);assertEquals(0,id);assertEquals(5,especialidad);assertEquals("Control general",descripcion);assertEquals("127.0.0.1",ip);escrituras++;return 71;}
        @Override protected void cambiarEstado(int actor,int id,String estado,String ip){escrituras++;}
    }
    private HttpExchange peticion(String rol,String ruta){var x=new HttpExchange();x.path=ruta;x.headers.put("Accept","application/json");x.session.setAttribute("usuarioLogueado",new Usuario(17,"Admin","Prueba","admin@example.test","",rol,null,true));return x;}
    @Test void pacienteNoPuedeEscribirAunqueEnvieIdentidadDeAdministrador(){
        var x=peticion("PACIENTE","/admin/motivos");var servlet=new Servlet();x.parameters.put("idUsuario","1");
        servlet.doPost(x.request,x.response);assertEquals(403,x.status);assertEquals(0,servlet.escrituras);assertTrue(x.body.toString().contains("CATALOGO_NO_AUTORIZADO"));
    }
    @Test void lecturaUsaIdentidadDeSesionYContratoComun(){
        var x=peticion("PERSONAL_ADMINISTRATIVO","/admin/motivos");var servlet=new Servlet();servlet.doGet(x.request,x.response);
        assertEquals(200,x.status);assertEquals(1,servlet.lecturas);assertTrue(x.body.toString().contains("MOTIVOS_OBTENIDOS"));assertTrue(x.body.toString().contains("\"data\""));
    }
    @Test void escrituraNoAceptaParametrosRepetidos(){
        var x=peticion("ADMINISTRADOR","/sysadmin/motivos");var servlet=new Servlet();x.multipleParameters.put("accion",new String[]{"guardar","estado"});
        servlet.doPost(x.request,x.response);assertEquals(400,x.status);assertEquals(0,servlet.escrituras);
    }
    @Test void guardarEnvuelveElIdPersistidoEnElContrato(){
        var x=peticion("ADMINISTRADOR","/sysadmin/motivos");var servlet=new Servlet();x.parameters.putAll(java.util.Map.of("accion","guardar","especialidad","5","descripcion","Control general","idUsuario","99"));
        servlet.doPost(x.request,x.response);assertEquals(200,x.status);assertEquals(1,servlet.escrituras);assertTrue(x.body.toString().contains("\"idMotivo\":71"));
    }
    @Test void csrfBloqueaAntesDeModificarElCatalogo()throws Exception{
        var x=peticion("ADMINISTRADOR","/sysadmin/motivos");var servlet=new Servlet();
        new CsrfFilter().doFilter(x.request,x.response,(r,s)->servlet.doPost(x.request,x.response));
        assertEquals(403,x.status);assertEquals(0,servlet.escrituras);assertTrue(x.body.toString().contains("CSRF_INVALIDO"));
    }
    @Test void perfilAjenoNoLeeDatos(){var x=peticion("PACIENTE","/sysadmin/motivos");var servlet=new Servlet();servlet.doGet(x.request,x.response);assertEquals(403,x.status);assertEquals(0,servlet.lecturas);}
    @Test void paginaCeroNoConsultaLaBase(){var x=peticion("ADMINISTRADOR","/sysadmin/motivos");var servlet=new Servlet();x.parameters.put("pagina","0");servlet.doGet(x.request,x.response);assertEquals(400,x.status);assertEquals(0,servlet.lecturas);}
}
