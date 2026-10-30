package pe.edu.utp.clinica.filter;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.support.HttpExchange;
import pe.edu.utp.clinica.util.CsrfToken;
import pe.edu.utp.clinica.model.Usuario;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class CsrfFilterTest {
    private final CsrfFilter filter = new CsrfFilter();

    private int execute(HttpExchange exchange) throws Exception {
        AtomicInteger writes = new AtomicInteger();
        filter.doFilter(exchange.request, exchange.response, (req, res) -> writes.incrementAndGet());
        return writes.get();
    }

    @Test void createsUnpredictableTokenPerSessionAndKeepsItStable() throws Exception {
        HttpExchange a = new HttpExchange();
        a.method = "GET";
        assertEquals(1, execute(a));
        String first = (String) a.session.getAttribute("csrfToken");
        assertTrue(first.matches("[A-Za-z0-9_-]{43}"));
        execute(a);
        assertEquals(first, a.session.getAttribute("csrfToken"));
        HttpExchange b = new HttpExchange(); b.method = "GET"; execute(b);
        assertNotEquals(first, b.session.getAttribute("csrfToken"));
    }

    @Test void rejectsPostWithoutTokenBeforeAnyWrite() throws Exception {
        HttpExchange x = new HttpExchange();
        CsrfToken.getOrCreate(x.session);
        assertEquals(0, execute(x));
        assertEquals(403, x.status);
        assertTrue(x.body.toString().contains("CSRF_INVALIDO"));
    }

    @Test void rejectsTokenFromAnotherSession() throws Exception {
        HttpExchange a = new HttpExchange(), b = new HttpExchange();
        CsrfToken.getOrCreate(a.session);
        b.parameters.put("csrfToken", CsrfToken.getOrCreate(a.session));
        CsrfToken.getOrCreate(b.session);
        assertEquals(0, execute(b));
        assertEquals(403, b.status);
    }

    @Test void acceptsCorrectFormToken() throws Exception {
        HttpExchange x = new HttpExchange();
        x.parameters.put("csrfToken", CsrfToken.getOrCreate(x.session));
        assertEquals(1, execute(x));
    }

    @Test void acceptsCorrectAjaxHeader() throws Exception {
        HttpExchange x = new HttpExchange();
        x.headers.put("X-CSRF-Token", CsrfToken.getOrCreate(x.session));
        assertEquals(1, execute(x));
    }

    @Test void rejectsWrongHeaderEvenIfFormTokenIsCorrect() throws Exception {
        HttpExchange x = new HttpExchange();
        x.parameters.put("csrfToken", CsrfToken.getOrCreate(x.session));
        x.headers.put("X-CSRF-Token", "incorrecto");
        assertEquals(0, execute(x));
    }

    @Test void rejectsExpiredSessionAndDoesNotCreateOneForPost() throws Exception {
        HttpExchange x = new HttpExchange();
        x.parameters.put("csrfToken", CsrfToken.getOrCreate(x.session));
        x.session = null;
        assertEquals(0, execute(x));
        assertNull(x.session);
    }

    @Test void rotatesTokenWhenSessionIsReplaced() throws Exception {
        HttpExchange x = new HttpExchange();
        String old = CsrfToken.getOrCreate(x.session);
        x.session = x.newSession();
        assertNotEquals(old, CsrfToken.getOrCreate(x.session));
        x.parameters.put("csrfToken", old);
        assertEquals(0, execute(x));
    }

    @Test void protectsAllUnsafeMethodsAndPublicEntryPoints() throws Exception {
        for (String method : new String[]{"POST", "PUT", "PATCH", "DELETE"}) {
            for (String path : new String[]{"/registro", "/logout", "/login/paciente", "/admin/dashboard", "/api/citas"}) {
                HttpExchange x = new HttpExchange(); x.method = method; x.path = path;
                assertEquals(0, execute(x), method + " " + path);
                assertEquals(403, x.status);
            }
        }
    }

    @Test void doesNotCreateSessionsForStaticAssets() throws Exception {
        HttpExchange x = new HttpExchange(); x.method = "GET"; x.path = "/assets/css/app.css"; x.session = null;
        assertEquals(1, execute(x));
        assertNull(x.session);
    }

    @Test void expiredHtmlLoginReturnsToSamePortalWithoutExecutingPostOrCreatingSession() throws Exception {
        for (String path : new String[]{"/login", "/login/paciente", "/login/personal"}) {
            HttpExchange x = new HttpExchange();
            x.path = path;
            x.session = null;
            x.headers.put("Accept", "text/html,application/xhtml+xml");
            x.parameters.put("csrfToken", "formulario-antiguo");
            x.parameters.put("password", "no-debe-procesarse");
            assertEquals(0, execute(x));
            assertEquals(303, x.status);
            assertEquals(path + "?error=sesion_formulario", x.responseHeaders.get("Location"));
            assertEquals("no-store", x.responseHeaders.get("Cache-Control"));
            assertEquals("", x.body.toString());
            assertNull(x.session);
        }
    }

    @Test void htmlRecoveryDoesNotBypassProtectionOrChangeJsonContract() throws Exception {
        HttpExchange valid = new HttpExchange();
        valid.path = "/login/personal";
        valid.headers.put("Accept", "text/html");
        valid.parameters.put("csrfToken", CsrfToken.getOrCreate(valid.session));
        assertEquals(1, execute(valid));
        for (String path : new String[]{"/registro", "/logout", "/api/citas"}) {
            HttpExchange x = new HttpExchange();
            x.path = path;
            x.headers.put("Accept", "text/html");
            assertEquals(0, execute(x));
            if ("/logout".equals(path)) {
                assertEquals(303, x.status);
                assertEquals("/login?msg=sesion_cerrada", x.responseHeaders.get("Location"));
                continue;
            }
            assertEquals(403, x.status);
            if (!path.startsWith("/api/")) {
                assertEquals("/WEB-INF/views/error.jsp", x.forward);
                assertEquals(true, x.attributes.get("csrfRejected"));
            }
        }
        HttpExchange api = new HttpExchange();
        api.path = "/login/paciente";
        api.headers.put("Accept", "application/json,text/html");
        assertEquals(0, execute(api));
        assertEquals(403, api.status);
        assertTrue(api.body.toString().contains("CSRF_INVALIDO"));
    }

    @Test void staleLogoutReturnsToSafePortalWithoutInvalidatingCurrentSession() throws Exception {
        HttpExchange x = new HttpExchange();
        x.path = "/logout";
        x.headers.put("Accept", "text/html");
        x.session.setAttribute("usuarioLogueado", new Usuario(10,"Paciente","Prueba","p@example.test","","PACIENTE",null,true));
        String sessionId = x.session.getId();
        String currentToken = CsrfToken.getOrCreate(x.session);
        x.parameters.put("csrfToken", "token-de-otra-pestana");
        assertEquals(0, execute(x));
        assertEquals(303, x.status);
        assertEquals("/login?error=sesion_formulario", x.responseHeaders.get("Location"));
        assertEquals(sessionId, x.session.getId());
        assertEquals(currentToken, x.session.getAttribute("csrfToken"));
        assertEquals("", x.body.toString());
    }

    @Test void repeatedHtmlLogoutWithoutActiveSessionSimplyReturnsToLogin() throws Exception {
        HttpExchange x = new HttpExchange();
        x.path = "/logout";
        x.session = null;
        x.headers.put("Accept", "text/html");
        assertEquals(0, execute(x));
        assertEquals(303, x.status);
        assertEquals("/login?msg=sesion_cerrada", x.responseHeaders.get("Location"));
        assertNull(x.session);
    }
}
