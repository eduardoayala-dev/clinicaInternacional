package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class SeguimientoConfirmacionesServletTest {
    @Test void disconnectedAnonymousRedirectDoesNotEscapeTheServlet() {
        var exchange = new HttpExchange();
        exchange.redirectFailure = new IOException("Cliente desconectado");
        var servlet = new SeguimientoConfirmacionesServlet();
        assertDoesNotThrow(() -> servlet.doGet(exchange.request, exchange.response));
    }

    @Test void disconnectedForbiddenResponseDoesNotEscapeTheServlet() {
        var exchange = new HttpExchange();
        exchange.writerFailure = new IOException("Cliente desconectado");
        exchange.errorFailure = new IOException("Cliente desconectado");
        var servlet = new SeguimientoConfirmacionesServlet();
        assertDoesNotThrow(() -> servlet.doPost(exchange.request, exchange.response));
    }

    @Test void invalidAppointmentDoesNotRegisterContactAndHandlesDisconnectedRedirect() {
        var exchange = new HttpExchange();
        exchange.session.setAttribute("usuarioLogueado",
                new Usuario(60, "Admisión", "Prueba", "admision@example.test", "hash", "PERSONAL_ADMINISTRATIVO", null, true));
        exchange.parameters.put("idCita", "0");
        exchange.redirectFailure = new IOException("Cliente desconectado");
        var servlet = new SeguimientoConfirmacionesServlet();
        assertDoesNotThrow(() -> servlet.doPost(exchange.request, exchange.response));
        assertEquals("Selecciona una cita válida.", exchange.session.getAttribute("flashMessage"));
    }
}
