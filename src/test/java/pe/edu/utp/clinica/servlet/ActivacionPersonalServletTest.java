package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.auth.SecretosSesion;
import pe.edu.utp.clinica.dao.InvitacionPersonalDAO;
import pe.edu.utp.clinica.support.HttpExchange;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ActivacionPersonalServletTest {
    private static class Almacen extends InvitacionPersonalDAO {
        byte[] esperado;int activaciones;
        @Override public boolean vigente(byte[] hash){return esperado!=null&&Arrays.equals(esperado,hash);}
        @Override public void activar(byte[] hash,String password){assertArrayEquals(esperado,hash);activaciones++;}
    }
    private ActivacionPersonalServlet servlet(Almacen almacen){return new ActivacionPersonalServlet(){@Override protected InvitacionPersonalDAO almacen(){return almacen;}};}
    private HttpExchange preparar(Almacen almacen)throws Exception {
        String secreto=SecretosSesion.nuevo();almacen.esperado=SecretosSesion.hash(secreto);var x=new HttpExchange();
        x.parameters.putAll(Map.of("action","preparar","token",secreto));servlet(almacen).doPost(x.request,x.response);
        assertEquals(200,x.status);assertFalse(x.body.toString().contains(secreto));assertEquals(0,almacen.activaciones);return x;
    }
    @Test void abrirEnlaceNoActivaYElTokenNoVuelveEnLaRespuesta()throws Exception {
        var almacen=new Almacen();var x=preparar(almacen);x.method="GET";
        servlet(almacen).doGet(x.request,x.response);
        assertEquals(true,x.attributes.get("invitacionVigente"));assertEquals("/WEB-INF/views/activar-personal.jsp",x.forward);assertEquals(0,almacen.activaciones);
        assertEquals("no-referrer",x.responseHeaders.get("Referrer-Policy"));
    }
    @Test void requierePreparacionYConfirmacionYNoTomaUsuariosDelCliente()throws Exception {
        var almacen=new Almacen();var x=preparar(almacen);
        x.parameters.clear();x.parameters.putAll(Map.of("action","activar","password","ClaveNueva#2026","confirmacion","ClaveNueva#2026","idUsuario","999","referencia",Base64.getUrlEncoder().withoutPadding().encodeToString(almacen.esperado)));
        servlet(almacen).doPost(x.request,x.response);assertEquals(200,x.status);assertEquals(1,almacen.activaciones);
        servlet(almacen).doPost(x.request,x.response);assertEquals(409,x.status);assertEquals(1,almacen.activaciones);
    }
    @Test void unFormularioDeOtraInvitacionNoPuedeActivarLaNueva()throws Exception {
        var almacen=new Almacen();var x=preparar(almacen);x.parameters.clear();
        x.parameters.putAll(Map.of("action","activar","password","ClaveNueva#2026","confirmacion","ClaveNueva#2026","referencia",SecretosSesion.nuevo()));
        servlet(almacen).doPost(x.request,x.response);assertEquals(409,x.status);assertEquals(0,almacen.activaciones);
    }
    @Test void intentarPrepararUnEnlaceInvalidoBorraElAnterior()throws Exception {
        var almacen=new Almacen();var x=preparar(almacen);x.parameters.put("token","incorrecto");servlet(almacen).doPost(x.request,x.response);
        assertEquals(410,x.status);assertNull(x.session.getAttribute("invitacionPersonalHash"));
    }
    @Test void confirmacionDiferenteNoConsumeLaInvitacion()throws Exception {
        var almacen=new Almacen();var x=preparar(almacen);x.parameters.clear();x.parameters.putAll(Map.of("action","activar","password","ClaveNueva#2026","confirmacion","OtraClave#2026","referencia",Base64.getUrlEncoder().withoutPadding().encodeToString(almacen.esperado)));
        servlet(almacen).doPost(x.request,x.response);assertEquals(400,x.status);assertEquals(0,almacen.activaciones);
    }
    @Test void unPostSinCsrfNoPuedePrepararLaInvitacion()throws Exception {
        var almacen=new Almacen();var x=new HttpExchange();x.path="/activar-personal";x.parameters.putAll(Map.of("action","preparar","token",SecretosSesion.nuevo()));
        new pe.edu.utp.clinica.filter.CsrfFilter().doFilter(x.request,x.response,(request,response)->{fail("No debe llegar al servlet");});
        assertEquals(403,x.status);assertNull(x.session.getAttribute("invitacionPersonalHash"));assertEquals(0,almacen.activaciones);
    }
}
