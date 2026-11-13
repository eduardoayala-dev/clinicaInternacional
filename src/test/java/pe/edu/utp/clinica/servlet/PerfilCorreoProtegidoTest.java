package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;
import static org.junit.jupiter.api.Assertions.*;

class PerfilCorreoProtegidoTest {
    @Test void perfilRechazaUnCambioDeCorreoPorPostDirecto()throws Exception {
        var exchange=new HttpExchange();exchange.path="/paciente/perfil";
        var usuario=new Usuario(1,"Paciente","Prueba","original@example.test","hash","PACIENTE",null,true);
        exchange.session.setAttribute("usuarioLogueado",usuario);
        exchange.parameters.put("email","sin-verificar@example.test");
        var servlet=new PacientePerfilServlet();servlet.init();servlet.doPost(exchange.request,exchange.response);
        assertEquals("/paciente/perfil",exchange.redirect);
        assertEquals("original@example.test",usuario.getEmail());
        assertNotNull(exchange.session.getAttribute("mensajeError"));
    }
    @Test void desconexionAlRechazarCorreoNoPropagaIOException()throws Exception {
        var exchange=new HttpExchange();exchange.path="/paciente/perfil";
        var usuario=new Usuario(1,"Paciente","Prueba","original@example.test","hash","PACIENTE",null,true);
        exchange.session.setAttribute("usuarioLogueado",usuario);
        exchange.parameters.put("email","sin-verificar@example.test");
        exchange.redirectFailure=new java.io.IOException("Cliente desconectado de prueba");
        var servlet=new PacientePerfilServlet();servlet.init();
        assertDoesNotThrow(()->servlet.doPost(exchange.request,exchange.response));
        assertEquals("original@example.test",usuario.getEmail());
    }
}
