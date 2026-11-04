package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.InvitacionPersonalDAO;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.service.InvitacionPersonalServicio;
import pe.edu.utp.clinica.support.HttpExchange;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GestionPersonalServletTest {
    private static class Servicio extends InvitacionPersonalServicio {
        int llamadas;
        Servicio(){super(null,null,"http://localhost:8080");}
        @Override public Resultado invitarAcceso(int actor,InvitacionPersonalDAO.Solicitud datos){assertEquals(90,actor);assertEquals(1,datos.sede());assertNull(datos.cmp());assertNull(datos.especialidad());assertEquals("MEDICO",datos.rol());llamadas++;return new Resultado(100,true);}
        @Override public Resultado reenviar(int actor,int usuario){assertEquals(90,actor);assertEquals(100,usuario);llamadas++;return new Resultado(100,false);}
    }
    private HttpExchange peticion(String rol){var x=new HttpExchange();x.session.setAttribute("usuarioLogueado",new Usuario(90,"Admin","Prueba","admin@example.test","",rol,null,true));return x;}
    private GestionPersonalServlet servlet(Servicio servicio){return new GestionPersonalServlet(){@Override protected InvitacionPersonalServicio servicio(){return servicio;}};}
    @Test void soloAdministradorPuedeCrearYElActorNoVieneDelFormulario()throws Exception {
        var servicio=new Servicio();var x=peticion("PERSONAL_ADMINISTRATIVO");x.parameters.put("action","registrar");
        servlet(servicio).doPost(x.request,x.response);assertEquals(403,x.status);assertEquals(0,servicio.llamadas);
        x=peticion("ADMINISTRADOR");x.parameters.putAll(Map.of("action","registrar","rol","MEDICO","id_sede","1","id_especialidad","1","cmp","123456","idActor","999","password","Password123!","estado","ACTIVO"));
        servlet(servicio).doPost(x.request,x.response);assertEquals(303,x.status);assertEquals(1,servicio.llamadas);assertTrue(x.session.getAttribute("flashMessage").toString().contains("pendiente"));
    }
    @Test void reenvioFallidoNoSePresentaComoCorreoEntregado()throws Exception {
        var servicio=new Servicio();var x=peticion("ADMINISTRADOR");x.parameters.putAll(Map.of("action","reinvitar","idUsuario","100"));
        servlet(servicio).doPost(x.request,x.response);assertEquals(303,x.status);assertNotNull(x.session.getAttribute("flashError"));assertEquals(1,servicio.llamadas);
    }
    @Test void rutaAntiguaNoPermiteCrearCuentasYAdmisionNoPuedeCrearMedicos()throws Exception {
        var x=peticion("PERSONAL_ADMINISTRATIVO");x.parameters.put("action","registrarMedico");
        new AdminDashboardServlet().doPost(x.request,x.response);assertEquals(403,x.status);
        x=peticion("ADMINISTRADOR");new UsuariosServlet().doPost(x.request,x.response);assertEquals(410,x.status);
    }
}
