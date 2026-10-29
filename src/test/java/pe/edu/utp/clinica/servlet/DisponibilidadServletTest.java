package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.filter.CsrfFilter;
import pe.edu.utp.clinica.support.HttpExchange;
import static org.junit.jupiter.api.Assertions.*;

class DisponibilidadServletTest {
    @Test void readinessIsAnonymousUncachedAndKeepsApiContract() throws Exception {
        var exchange = new HttpExchange();
        exchange.session = null;
        exchange.path = "/health/ready";
        exchange.method = "GET";
        var servlet = new DisponibilidadServlet() {
            @Override protected boolean disponible() { return true; }
        };
        new CsrfFilter().doFilter(exchange.request, exchange.response,
                (req, res) -> servlet.doGet(exchange.request, exchange.response));
        assertEquals(200, exchange.status);
        assertNull(exchange.session);
        assertEquals("no-store", exchange.responseHeaders.get("Cache-Control"));
        assertTrue(exchange.body.toString().contains("SISTEMA_DISPONIBLE"));
        assertTrue(exchange.body.toString().contains("\"data\":null"));
    }

    @Test void unavailableReturns503AndRetryWithoutInternalDetails() throws Exception {
        var exchange = new HttpExchange();
        var servlet = new DisponibilidadServlet() {
            @Override protected boolean disponible() { return false; }
        };
        servlet.doGet(exchange.request, exchange.response);
        assertEquals(503, exchange.status);
        assertEquals("3", exchange.responseHeaders.get("Retry-After"));
        assertFalse(exchange.body.toString().contains("jdbc"));
        assertFalse(exchange.body.toString().contains("SQLException"));
    }
}
