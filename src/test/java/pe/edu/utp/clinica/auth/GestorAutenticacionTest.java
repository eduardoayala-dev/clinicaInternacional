package pe.edu.utp.clinica.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;
import pe.edu.utp.clinica.filter.AutenticacionJwtFilter;
import pe.edu.utp.clinica.filter.AuthFilter;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GestorAutenticacionTest {
    private final Clock now=Clock.fixed(Instant.parse("2026-10-29T12:00:00Z"),ZoneOffset.UTC);
    private final String key=Base64.getEncoder().encodeToString(new byte[32]);
    private static class Memoria implements AlmacenSesiones {
        SesionAutenticada sesion; byte[] hash; boolean revocada;
        public void crear(String id,Usuario usuario,byte[] hash,String csrf,Instant now,Instant vence) {
            this.sesion=new SesionAutenticada(id,usuario,csrf,vence); this.hash=hash;
        }
        public SesionAutenticada resolver(String id,byte[] hash,Instant now,boolean actividad) throws SQLException {
            return !revocada && sesion!=null && (sesion.id().equals(id)||Arrays.equals(this.hash,hash))?sesion:null;
        }
        public void revocar(String id,Instant now) { revocada=true; }
    }
    private GestorAutenticacion gestor(AlmacenSesiones store,Clock clock) {
        return new GestorAutenticacion(new JwtAcceso(key,"clinica","web",clock),store,clock,true);
    }
    private Usuario usuario(String rol) { return new Usuario(7,"Usuario","Prueba","u@example.test","hash",rol,null,true); }
    @Test void allRolesGetProtectedCookiesAndRenewWithoutRotatingCsrfOrSid() throws Exception {
        for(String rol:JwtAcceso.ROLES) {
            var store=new Memoria(); var gestor=gestor(store,now); var first=new HttpExchange();
            String old=first.session.getId(); gestor.iniciar(first.request,first.response,usuario(rol));
            assertNotEquals(old,first.session.getId());
            assertEquals(2,first.responseCookies.size());
            for(Cookie cookie:first.responseCookies) {
                assertTrue(cookie.isHttpOnly()); assertTrue(cookie.getSecure()); assertEquals("Lax",cookie.getAttribute("SameSite")); assertEquals("/",cookie.getPath());
            }
            var renewed=new HttpExchange(); renewed.cookies=first.responseCookies.toArray(Cookie[]::new);
            renewed.session=first.session;
            var active=gestor(store,Clock.offset(now,Duration.ofSeconds(301))).resolver(renewed.request,renewed.response,true);
            assertEquals(store.sesion.id(),active.id()); assertEquals(store.sesion.csrf(),renewed.session.getAttribute("csrfToken"));
            assertEquals(1,renewed.responseCookies.size());
            assertEquals(CookiesAutenticacion.ACCESO,renewed.responseCookies.get(0).getName());
            String refresh=first.responseCookies.stream().filter(c->c.getName().equals(CookiesAutenticacion.RENOVACION)).findFirst().orElseThrow().getValue();
            assertArrayEquals(SecretosSesion.hash(refresh),store.hash);
        }
    }
    @Test void logoutRevokesBothAccessAndRenewalIncludingOtherTabs() throws Exception {
        var store=new Memoria();var gestor=gestor(store,now);var x=new HttpExchange();
        gestor.iniciar(x.request,x.response,usuario("PACIENTE"));
        Cookie[] cookies=x.responseCookies.toArray(Cookie[]::new);
        gestor.cerrar(x.request,x.response);
        assertNull(x.session);assertTrue(store.revocada);
        var other=new HttpExchange();other.cookies=cookies;
        assertNull(gestor.resolver(other.request,other.response,true));
        assertEquals(2,other.responseCookies.size());
        assertTrue(other.responseCookies.stream().allMatch(c->c.getMaxAge()==0));
    }
    @Test void forgedHttpSessionDoesNotAuthenticateWithoutCookiesInJwtMode() throws Exception {
        var x=new HttpExchange();x.contextAttributes.put(GestorAutenticacion.CONTEXTO,gestor(new Memoria(),now));
        x.headers.put("Accept","application/json");x.session.setAttribute("usuarioLogueado",usuario("PACIENTE"));
        new AutenticacionJwtFilter().doFilter(x.request,x.response,(req,res)->new AuthFilter().doFilter(req,res,(a,b)->fail("No debe autorizar")));
        assertEquals(403,x.status);assertTrue(x.body.toString().contains("SESION_REQUERIDA"));
    }
    @Test void datastoreFailureReturns503WithoutSilentlyInvalidatingCookies() throws Exception {
        var store=new Memoria(){@Override public SesionAutenticada resolver(String id,byte[] hash,Instant now,boolean activity)throws SQLException{throw new SQLException("fallo");}};
        var gestor=gestor(store,now);var first=new HttpExchange();gestor.iniciar(first.request,first.response,usuario("MEDICO"));
        var x=new HttpExchange();x.cookies=first.responseCookies.toArray(Cookie[]::new);x.contextAttributes.put(GestorAutenticacion.CONTEXTO,gestor);
        x.headers.put("Accept","application/json");
        new AutenticacionJwtFilter().doFilter(x.request,x.response,(a,b)->fail("No debe autorizar"));
        assertEquals(503,x.status);assertEquals("3",x.responseHeaders.get("Retry-After"));assertTrue(x.responseCookies.isEmpty());
    }
}
