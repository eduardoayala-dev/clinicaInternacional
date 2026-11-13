package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.support.HttpExchange;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class AdmisionHorariosServletTest {
    @Test void identidadAusenteNoConsultaNiRevisaPropuestas() {
        var servlet = new AdmisionHorariosServlet();
        var get = new HttpExchange(); get.session = null;
        get.headers.put("Accept", "application/json");
        servlet.doGet(get.request, get.response);
        assertEquals(403, get.status);
        assertTrue(get.body.toString().contains("ACCESO_DENEGADO"));
        var post = new HttpExchange(); post.session = null;
        post.headers.put("Accept", "application/json");
        servlet.doPost(post.request, post.response);
        assertEquals(403, post.status);
    }

    @Test void desconexionDelClienteNoPropagaErroresDelServlet() {
        var servlet = new AdmisionHorariosServlet();
        var get = new HttpExchange(); get.session = null;
        get.headers.put("Accept", "application/json");
        get.writerFailure = new IOException("Desconexión de prueba");
        assertDoesNotThrow(() -> servlet.doGet(get.request, get.response));
        assertEquals(500, get.status);
        var post = new HttpExchange(); post.session = null;
        post.errorFailure = new IOException("Desconexión de prueba");
        assertDoesNotThrow(() -> servlet.doPost(post.request, post.response));
        assertEquals(500, post.status);
    }
}
