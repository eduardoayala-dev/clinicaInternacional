package pe.edu.utp.clinica.servlet;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;

class ApiCitasServletTest {
    @Test void listadoUsaLaIdentidadDeSesion() {
        var x = paciente();
        x.method = "GET";
        x.parameters.put("idPaciente", "999");
        var api = new ApiCitasServlet() {
            @Override protected CitaDAO citas() {
                return new CitaDAO() {
                    @Override public int obtenerIdPacientePorUsuarioId(int id) {
                        assertEquals(10, id);
                        return 5;
                    }
                    @Override public List<Map<String, Object>> obtenerProximasCitasPaciente(int id) {
                        assertEquals(5, id);
                        return List.of(Map.of("idCita", 8));
                    }
                };
            }
        };
        api.service(x.request, x.response);
        assertEquals(200, x.status);
        assertTrue(x.body.toString().contains("CITAS_OBTENIDAS"));
    }

    @Test void protegeEscriturasYConservaLaRespuestaJson() {
        var x = new HttpExchange();
        var api = new ApiCitasServlet();
        var req = new HttpServletRequestWrapper(x.request) {
            @Override public String getPathInfo() { return "/8/cancelacion"; }
        };
        api.service(req, x.response);
        assertEquals(401, x.status);
        assertTrue(x.body.toString().contains("SESION_REQUERIDA"));
    }

    private static HttpExchange paciente() {
        var x = new HttpExchange();
        x.session.setAttribute("usuarioLogueado", new Usuario(10, "P", "Uno", "p@test", "", "PACIENTE", null, true));
        return x;
    }
}
