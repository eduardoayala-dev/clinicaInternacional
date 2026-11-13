package pe.edu.utp.clinica.util;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.support.HttpExchange;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TransporteHttpTest {
    @Test void desconexionNoPropagaErroresDeTransporte() {
        var intercambio=new HttpExchange();
        intercambio.errorFailure=new java.io.IOException("cliente desconectado");
        intercambio.redirectFailure=new java.io.IOException("cliente desconectado");
        assertDoesNotThrow(()->TransporteHttp.error(intercambio.response,503));
        assertDoesNotThrow(()->TransporteHttp.redirigir(intercambio.response,"/login"));
    }
}
