package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.support.HttpExchange;
import pe.edu.utp.clinica.recuperacion.*;
import java.time.Clock;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class RecuperacionServletTest {
    private void configurar(HttpExchange exchange) {
        var codigos=new CodigosVerificacion(Base64.getEncoder().encodeToString(new byte[32]));
        var dao=new VerificacionJdbc(()->{throw new AssertionError("Esta petición no debe abrir la base.");},codigos);
        exchange.contextAttributes.put(RecuperacionServicio.CONTEXTO,new RecuperacionServicio(dao,c->{throw new AssertionError("No debe enviar correo.");},Clock.systemUTC(),tarea->{}));
    }
    @Test void sinConfiguracionNoSimulaEnvios() {
        var e=new HttpExchange();e.path="/recuperar";new RecuperacionServlet().doPost(e.request,e.response);
        assertEquals(503,e.status);assertEquals("/WEB-INF/views/recuperacion.jsp",e.forward);
        assertEquals("no-referrer",e.responseHeaders.get("Referrer-Policy"));assertEquals("no-store",e.responseHeaders.get("Cache-Control"));
    }
    @Test void noMuestraCambioSinVerificarNiAceptaTokensDeUrl() {
        var e=new HttpExchange();e.path="/restablecer";e.method="GET";e.parameters.put("token","ignorado");
        new RecuperacionServlet().doGet(e.request,e.response);assertEquals("/verificar-codigo",e.redirect);
    }
    @Test void postDirectoNoSaltaLaVerificacion() {
        for(String path:new String[]{"/restablecer","/cuenta/nuevo-correo","/verificar-codigo"}) {
            var e=new HttpExchange();e.path=path;configurar(e);e.parameters.put("password","NuevaClave!2026Segura");
            new RecuperacionServlet().doPost(e.request,e.response);assertEquals(403,e.status);assertEquals("codigo",e.attributes.get("vistaVerificacion"));
        }
    }
    @Test void cuentaRequiereSesionJwt() {
        var e=new HttpExchange();e.path="/cuenta/seguridad";e.method="GET";new RecuperacionServlet().doGet(e.request,e.response);assertEquals("/login",e.redirect);
    }
    @Test void iniciarRecuperacionNoRevelaSiExisteNiEsperaProveedor() {
        for(String documento:new String[]{"12345678","99999999"}){
            var e=new HttpExchange();e.path="/recuperar";e.parameters.put("identificador",documento);e.parameters.put("portal","paciente");configurar(e);
            new RecuperacionServlet().doPost(e.request,e.response);assertEquals("/verificar-codigo",e.redirect);
        }
    }
    @Test void portalPersonalSeConservaEnVistaInicial() {
        var e=new HttpExchange();e.path="/recuperar";e.parameters.put("portal","personal");new RecuperacionServlet().doGet(e.request,e.response);
        assertEquals(true,e.attributes.get("portalPersonal"));assertEquals("solicitud",e.attributes.get("vistaVerificacion"));
    }
    @Test void falloAlRenderizarNoSePresentaComoExito() {
        var e=new HttpExchange();e.path="/recuperar";e.forwardFailure=new jakarta.servlet.ServletException("prueba");new RecuperacionServlet().doGet(e.request,e.response);assertEquals(500,e.status);
    }
    @Test void vistaIntegradaEntregaSoloElFormularioSinCambiarLaSeguridad() {
        var e=new HttpExchange();e.path="/recuperar";e.method="GET";
        e.headers.put("X-Recuperacion-Inline","1");e.parameters.put("portal","personal");
        new RecuperacionServlet().doGet(e.request,e.response);
        assertEquals("/WEB-INF/views/recuperacion-contenido.jsp",e.forward);
        assertEquals("1",e.responseHeaders.get("X-Recuperacion-Vista"));
        assertEquals("no-store",e.responseHeaders.get("Cache-Control"));
        assertEquals(true,e.attributes.get("portalPersonal"));
    }
    @Test void formularioIntegradoNoAutorizaUnCambioDirecto() {
        var e=new HttpExchange();e.path="/restablecer";configurar(e);
        e.headers.put("X-Recuperacion-Inline","1");e.parameters.put("password","NuevaClave!2026Segura");
        new RecuperacionServlet().doPost(e.request,e.response);
        assertEquals(403,e.status);
        assertEquals("/WEB-INF/views/recuperacion-contenido.jsp",e.forward);
        assertEquals("codigo",e.attributes.get("vistaVerificacion"));
    }
}
