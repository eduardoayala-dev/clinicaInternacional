package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;
import static org.junit.jupiter.api.Assertions.*;

class EventosConfirmacionesServletTest {
    @Test void noAbreUnCanalSinAutenticacion() {
        var exchange = new HttpExchange();
        var servlet = new EventosConfirmacionesServlet();
        servlet.doGet(exchange.request, exchange.response);
        assertEquals(403, exchange.status);
    }

    @Test void unPacienteNoPuedeAbrirElCanalAdministrativo() {
        var exchange = new HttpExchange();
        exchange.session.setAttribute("usuarioLogueado",
                new Usuario(1, "Paciente", "Prueba", "prueba@example.test", "", "PACIENTE", null, true));
        var servlet = new EventosConfirmacionesServlet();
        servlet.doGet(exchange.request, exchange.response);
        assertEquals(403, exchange.status);
    }
}
