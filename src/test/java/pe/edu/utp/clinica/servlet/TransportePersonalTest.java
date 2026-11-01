package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.support.HttpExchange;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class TransportePersonalTest {
    @Test void desconexionAlRechazarUnaCuentaNoPropagaLaExcepcion() {
        var intercambio=new HttpExchange();
        intercambio.errorFailure=new IOException("Cliente desconectado");
        assertDoesNotThrow(()->new GestionPersonalServlet().doPost(intercambio.request,intercambio.response));
        assertEquals(500,intercambio.status);
    }
    @Test void desconexionEnLaRespuestaDeActivacionNoExponeElSecreto() {
        var intercambio=new HttpExchange();
        intercambio.parameters.put("action","inexistente");
        intercambio.writerFailure=new IOException("Cliente desconectado");
        assertDoesNotThrow(()->new ActivacionPersonalServlet().doPost(intercambio.request,intercambio.response));
        assertEquals(500,intercambio.status);
        assertEquals("",intercambio.body.toString());
    }
    @Test void redireccionSinSesionControlaElFalloDeTransporte() {
        var intercambio=new HttpExchange();
        intercambio.redirectFailure=new IOException("Cliente desconectado");
        assertDoesNotThrow(()->new SysAdminServlet().doPost(intercambio.request,intercambio.response));
        assertEquals(500,intercambio.status);
    }
}
