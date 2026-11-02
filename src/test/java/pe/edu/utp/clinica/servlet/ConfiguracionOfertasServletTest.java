package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.ConfiguracionOfertasDAO;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;
import static org.junit.jupiter.api.Assertions.*;

class ConfiguracionOfertasServletTest {
    private static class Almacen extends ConfiguracionOfertasDAO {
        int llamadas;
        @Override public Parametros leer(){return new Parametros(7,90,2);}
        @Override public Parametros guardar(int actor,Cambios cambios) {
            assertEquals(1,actor);assertEquals(new Cambios(7,90,24,10,48,2,2),cambios);
            llamadas++;return new Parametros(7,90,3,24,10,48,2);
        }
    }
    private HttpExchange peticion(String rol){
        var x=new HttpExchange();
        x.session.setAttribute("usuarioLogueado",new Usuario(1,"Admin","Prueba","admin@example.test","",rol,null,true));
        return x;
    }
    private ConfiguracionOfertasServlet servlet(Almacen almacen){return new ConfiguracionOfertasServlet(){@Override protected ConfiguracionOfertasDAO almacen(){return almacen;}};}
    @Test void cargaLosValoresGuardadosEnLaVista() {
        var x=peticion("ADMINISTRADOR");servlet(new Almacen()).doGet(x.request,x.response);
        assertEquals("/WEB-INF/views/sysadmin/priorizacion.jsp",x.forward);
        assertEquals(new ConfiguracionOfertasDAO.Parametros(7,90,2),x.attributes.get("configuracionOfertas"));
        assertEquals("no-store",x.responseHeaders.get("Cache-Control"));
    }
    @Test void elActorVieneDeLaSesionYSeRedirigeTrasGuardar() {
        var x=peticion("ADMINISTRADOR");var almacen=new Almacen();
        x.parameters.put("tiempoLimite","7");x.parameters.put("anticipacionMinima","90");x.parameters.put("version","2");x.parameters.put("confirmacionHoras","24");x.parameters.put("toleranciaLlegada","10");x.parameters.put("recordatorioHoras","48");x.parameters.put("intentosConfirmacion","2");x.parameters.put("idUsuario","999");
        servlet(almacen).doPost(x.request,x.response);assertEquals(303,x.status);assertEquals(1,almacen.llamadas);
        assertNotNull(x.session.getAttribute("flashMessage"));
    }
    @Test void unRolAjenoYLosNumerosInvalidosNoGuardan() {
        var almacen=new Almacen();var x=peticion("PERSONAL_ADMINISTRATIVO");
        servlet(almacen).doPost(x.request,x.response);assertEquals(403,x.status);assertEquals(0,almacen.llamadas);
        x=peticion("ADMINISTRADOR");x.parameters.put("tiempoLimite","no-numero");
        servlet(almacen).doPost(x.request,x.response);assertEquals(303,x.status);assertNotNull(x.session.getAttribute("flashError"));assertEquals(0,almacen.llamadas);
    }
}
