package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.model.*;
import pe.edu.utp.clinica.service.AuthService;
import pe.edu.utp.clinica.service.dni.*;
import pe.edu.utp.clinica.support.HttpExchange;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class RegistroDniConsultadoTest {
    private Usuario register(boolean consulted, String numero) throws Exception {
        var saved = new AtomicReference<Usuario>();
        var servlet = new RegistroPacienteServlet();
        var field = RegistroPacienteServlet.class.getDeclaredField("authService");
        field.setAccessible(true);
        field.set(servlet, new AuthService() {
            @Override public boolean existeDocumentoOCorreo(String documento, String email) { return false; }
            @Override public boolean registrarPaciente(Usuario usuario, Paciente paciente) {
                assertEquals("Correo", paciente.getCanalAviso());
                usuario.setIdUsuario(42); saved.set(usuario); return true;
            }
        });
        var x = new HttpExchange();
        x.parameters.put("dni", numero);
        x.parameters.put("nombres", "NOMBRES MODIFICADOS");
        x.parameters.put("apellidos", "APELLIDOS MODIFICADOS");
        x.parameters.put("password", "PruebaSegura2026!");
        x.parameters.put("terminos", "on");
        x.parameters.put("canalAviso", "WhatsApp");
        x.parameters.put("fechaNacimiento", "1995-01-01");
        if (consulted) DniConsultaSesion.guardar(x.session, new DniDatos("99000001", "PRUEBA", "FICTICIO", "LOCAL"));
        servlet.doPost(x.request, x.response);
        assertEquals("/paciente/dashboard?bienvenida=1", x.redirect);
        return saved.get();
    }
    @Test void ignoresTamperedNamesAfterSuccessfulLookup() throws Exception {
        var user = register(true, "99000001");
        assertEquals("Prueba", user.getNombres());
        assertEquals("Ficticio Local", user.getApellidos());
    }
    @Test void retainsManualFallbackForOtherDocumentsAndSessions() throws Exception {
        assertEquals("Nombres Modificados", register(false, "99000001").getNombres());
        assertEquals("Nombres Modificados", register(true, "99000002").getNombres());
    }
}
