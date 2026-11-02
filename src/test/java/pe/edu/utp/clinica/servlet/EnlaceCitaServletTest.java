package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.EnlaceCitaDAO;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;
import pe.edu.utp.clinica.util.EnlaceCita;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class EnlaceCitaServletTest {
    private HttpExchange peticion(boolean autenticada){
        var x=new HttpExchange();x.parameters.put("cita","7");x.parameters.put("accion","confirmar");
        if(autenticada)x.session.setAttribute("usuarioLogueado",new Usuario(10,"P","Uno","p@test","","PACIENTE",null,true));
        return x;
    }
    @Test void enlaceAnonimoConservaSoloUnRetornoInternoYNoConsultaDatos()throws Exception {
        var x=peticion(false);new EnlaceCitaServlet().doGet(x.request,x.response);
        assertEquals("/login/paciente",x.redirect);
        assertEquals("/cita?cita=7&accion=confirmar",EnlaceCita.consumir(x.session,Clock.systemUTC()));
        assertNull(EnlaceCita.consumir(x.session,Clock.systemUTC()));
    }
    @Test void getAutenticadoSoloMuestraLaRevision()throws Exception {
        var x=peticion(true);var dao=new EnlaceCitaDAO(){
            @Override public Resumen consultar(int usuario,int cita){assertEquals(10,usuario);return new Resumen(cita,LocalDateTime.of(2026,11,5,9,0),"Médico","General","Central","CONFIRMADA",false,false,true);}
            @Override public void ejecutar(int usuario,EnlaceCita enlace){fail("GET no puede ejecutar acciones");}
        };
        new EnlaceCitaServlet(){@Override protected EnlaceCitaDAO almacen(){return dao;}}.doGet(x.request,x.response);
        assertEquals("/WEB-INF/views/paciente/enlace-cita.jsp",x.forward);
        assertEquals("DENY",x.responseHeaders.get("X-Frame-Options"));
        assertEquals("no-referrer",x.responseHeaders.get("Referrer-Policy"));
    }
    @Test void postTomaElUsuarioDeLaSesionYRedirigeTrasLaAccion()throws Exception {
        var x=peticion(true);x.parameters.put("usuario","99");
        var dao=new EnlaceCitaDAO(){@Override public void ejecutar(int usuario,EnlaceCita enlace){assertEquals(10,usuario);assertEquals(7,enlace.cita());}};
        new EnlaceCitaServlet(){@Override protected EnlaceCitaDAO almacen(){return dao;}}.doPost(x.request,x.response);
        assertEquals(303,x.status);assertEquals("/paciente/historial",x.responseHeaders.get("Location"));
        assertTrue(x.session.getAttribute("flashMessage").toString().contains("confirmada"));
    }
    @Test void rechazaPostAnonimoYAccionesNoReconocidas()throws Exception {
        var x=peticion(false);new EnlaceCitaServlet().doPost(x.request,x.response);assertEquals(403,x.status);
        x=peticion(true);x.parameters.put("accion","https://otro.test");new EnlaceCitaServlet().doGet(x.request,x.response);assertEquals(400,x.status);
    }
    @Test void retornoExpiraYNoAdmiteUnDestinoArbitrario(){
        var x=peticion(false);var reloj=Clock.fixed(Instant.parse("2026-11-02T15:00:00Z"),ZoneOffset.UTC);
        new EnlaceCita(7,EnlaceCita.Accion.CANCELAR).recordar(x.session,reloj);
        assertNull(EnlaceCita.consumir(x.session,Clock.offset(reloj,Duration.ofMinutes(10))));
        x.session.setAttribute(EnlaceCita.class.getName(),"https://otro.test");assertNull(EnlaceCita.consumir(x.session,reloj));
    }
    @Test void accionDelEnlaceTambienRequiereCsrf()throws Exception {
        var x=peticion(true);x.method="POST";
        var servlet=new EnlaceCitaServlet(){@Override protected EnlaceCitaDAO almacen(){return new EnlaceCitaDAO(){@Override public void ejecutar(int usuario,EnlaceCita enlace){fail("El filtro debe rechazar antes de ejecutar");}};}};
        new pe.edu.utp.clinica.filter.CsrfFilter().doFilter(x.request,x.response,(r,s)->servlet.doPost(x.request,x.response));
        assertEquals(403,x.status);
    }
}
