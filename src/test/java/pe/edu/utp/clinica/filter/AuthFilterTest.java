package pe.edu.utp.clinica.filter;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class AuthFilterTest {
    @Test void jsonWithoutSessionIsForbiddenInsteadOfRedirectingToHtml() throws Exception {
        var x=new HttpExchange(); x.session=null; x.headers.put("Accept","application/json");
        new AuthFilter().doFilter(x.request,x.response,(request,response)->fail("No debe continuar"));
        assertEquals(403,x.status); assertNull(x.redirect); assertTrue(x.body.toString().contains("SESION_REQUERIDA"));
    }
    @Test void wrongRoleCannotAccessPatientJson() throws Exception {
        var x=new HttpExchange(); x.headers.put("Accept","application/json");
        x.session.setAttribute("usuarioLogueado",new Usuario(1,"Médico","Prueba","m@test","","MEDICO",null,true));
        new AuthFilter().doFilter(x.request,x.response,(request,response)->fail("No debe continuar"));
        assertEquals(403,x.status); assertTrue(x.body.toString().contains("ACCESO_DENEGADO"));
    }
    @Test void authenticatedPatientContinuesWithCacheProtection() throws Exception {
        var x=new HttpExchange(); var called=new AtomicBoolean();
        x.session.setAttribute("usuarioLogueado",new Usuario(1,"Paciente","Prueba","p@test","","PACIENTE",null,true));
        new AuthFilter().doFilter(x.request,x.response,(request,response)->called.set(true));
        assertTrue(called.get()); assertTrue(x.responseHeaders.get("Cache-Control").contains("no-store"));
    }
}
