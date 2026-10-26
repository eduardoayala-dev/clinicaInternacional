package pe.edu.utp.clinica.filter;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.support.HttpExchange;
import pe.edu.utp.clinica.util.CsrfToken;
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
}
