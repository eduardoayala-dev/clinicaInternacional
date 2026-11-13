package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.SeguimientoOfertasDAO;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SeguimientoOfertasServletTest {
    private HttpExchange peticion(String rol){var x=new HttpExchange();x.headers.put("Accept","application/json");x.session.setAttribute("usuarioLogueado",new Usuario(2,"A","Uno","a@test","",rol,null,true));return x;}
    @Test void otrosRolesNoConsultanElPanel(){
        var servlet=new SeguimientoOfertasServlet(){@Override protected SeguimientoOfertasDAO.Panel consultar(int actor,SeguimientoOfertasDAO.Filtro filtro){fail("No debe consultar");return null;}};
        for(String rol:new String[]{"PACIENTE","MEDICO","ADMINISTRADOR"}){var x=peticion(rol);servlet.doGet(x.request,x.response);assertEquals(403,x.status);}
    }
    @Test void fechaImposibleParametrosRepetidosYEstadoInvalidoSeRechazan(){
        var servlet=new SeguimientoOfertasServlet();
        for(String fecha:new String[]{"2026-02-30","2026-2-1","texto"}){var x=peticion("PERSONAL_ADMINISTRATIVO");x.parameters.put("fecha",fecha);servlet.doGet(x.request,x.response);assertEquals(400,x.status);}
        var x=peticion("PERSONAL_ADMINISTRATIVO");x.multipleParameters.put("estado",new String[]{"ACEPTADA","RECHAZADA"});servlet.doGet(x.request,x.response);assertEquals(400,x.status);
        x=peticion("PERSONAL_ADMINISTRATIVO");x.parameters.put("estado","AJENO");servlet.doGet(x.request,x.response);assertEquals(400,x.status);
    }
    @Test void usaIdentidadDeSesionYContratoUniformeSinAceptarSedeForzada(){
        var servlet=new SeguimientoOfertasServlet(){@Override protected SeguimientoOfertasDAO.Panel consultar(int actor,SeguimientoOfertasDAO.Filtro filtro){
            assertEquals(2,actor);assertEquals(1,filtro.pagina());return new SeguimientoOfertasDAO.Panel("San Borja",filtro,1,1,0,Map.of(),List.of(),List.of(),LocalDateTime.of(2026,11,2,10,0));}};
        var x=peticion("PERSONAL_ADMINISTRATIVO");x.parameters.put("sede","99");x.parameters.put("actor","99");servlet.doGet(x.request,x.response);
        assertEquals(200,x.status);assertEquals("no-store",x.responseHeaders.get("Cache-Control"));assertTrue(x.body.toString().contains("SEGUIMIENTO_OFERTAS_OBTENIDO"));
    }
}
