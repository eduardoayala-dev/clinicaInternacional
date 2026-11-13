package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.SeguimientoIaDAO;
import pe.edu.utp.clinica.ia.EntradaInasistencia;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class SeguimientoIaServletTest {
    private HttpExchange peticion(String rol) {
        var x=new HttpExchange();x.headers.put("Accept","application/json");x.parameters.put("cita","7");
        x.session.setAttribute("usuarioLogueado",new Usuario(2,"A","Uno","a@test","",rol,null,true));return x;
    }
    @Test void rechazaOtrosRolesAntesDeConsultarLaCita() {
        var s=new SeguimientoIaServlet(){@Override protected SeguimientoIaDAO.Instantanea consultar(int actor,int cita){fail("No debe consultar");return null;}};s.init();
        for(String rol:new String[]{"PACIENTE","MEDICO","ADMINISTRADOR"}) {var x=peticion(rol);s.doGet(x.request,x.response);assertEquals(403,x.status);}
    }
    @Test void identificadoresAmbiguosNoLleganALaConsulta() {
        var s=new SeguimientoIaServlet();s.init();var x=peticion("PERSONAL_ADMINISTRATIVO");
        x.multipleParameters.put("cita",new String[]{"7","8"});s.doGet(x.request,x.response);assertEquals(400,x.status);
    }
    @Test void identidadDelActorSeObtieneDeLaSesionYNoDelFormulario() {
        var s=new SeguimientoIaServlet(){@Override protected SeguimientoIaDAO.Instantanea consultar(int actor,int cita){assertEquals(2,actor);assertEquals(7,cita);
                var fecha=LocalDateTime.of(2026,11,2,10,0);return new SeguimientoIaDAO.Instantanea(cita,fecha,fecha.minusDays(1),new EntradaInasistencia(0,0,5,false),true);}
        };s.init();var x=peticion("PERSONAL_ADMINISTRATIVO");x.parameters.put("actor","99");
        s.doGet(x.request,x.response);assertEquals(200,x.status);assertEquals("no-store",x.responseHeaders.get("Cache-Control"));
        assertTrue(x.body.toString().contains("SINTETICO"));assertFalse(x.body.toString().contains("a@test"));
    }
}
