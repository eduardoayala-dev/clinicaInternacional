package pe.edu.utp.clinica.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.CatalogoDatosException;
import pe.edu.utp.clinica.dao.MedicoDAO;
import pe.edu.utp.clinica.filter.CsrfFilter;
import pe.edu.utp.clinica.model.MedicoCatalogo;
import pe.edu.utp.clinica.service.CatalogoService;
import pe.edu.utp.clinica.support.HttpExchange;
import pe.edu.utp.clinica.util.CsrfToken;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class CatalogosApiServletTest {
    private final AtomicInteger calls = new AtomicInteger();
    private final CatalogoService catalogos = new CatalogoService(new MedicoDAO() {
        @Override public List<MedicoCatalogo> consultarMedicos(int especialidad, Integer sede) {
            calls.incrementAndGet();
            if (Integer.valueOf(2).equals(sede)) return List.of();
            return List.of(new MedicoCatalogo(42, "Médico de Prueba", ""));
        }
        @Override public List<String> obtenerFechasDisponibles(int medico) {
            calls.incrementAndGet(); return List.of("2026-10-28");
        }
        @Override public List<String> obtenerHorariosDisponibles(int medico, String fecha) {
            calls.incrementAndGet(); return List.of("09:00");
        }
    });

    private HttpExchange request() {
        var x = new HttpExchange(); x.method = "GET"; return x;
    }

    private com.fasterxml.jackson.databind.JsonNode assertBody(HttpExchange x, int status, String code) throws Exception {
        assertEquals(status, x.status);
        var body = new ObjectMapper().readTree(x.body.toString());
        assertEquals(3, body.size());
        assertEquals(code, body.path("code").asText());
        assertTrue(body.path("message").isTextual());
        assertTrue(body.has("data"));
        assertEquals("no-store", x.responseHeaders.get("Cache-Control"));
        assertTrue(x.contentType.startsWith("application/json"));
        return body;
    }

    @Test void medicalSuccessUsesTypedEnvelope() throws Exception {
        var x = request(); x.parameters.put("idEspecialidad", "1");
        new ApiMedicosServlet(catalogos).service(x.request, x.response);
        var data = assertBody(x, 200, "MEDICOS_OBTENIDOS").get("data");
        assertTrue(data.isArray()); assertEquals(1, data.size());
        assertEquals(3, data.get(0).size()); assertEquals(42, data.get(0).get("idMedico").asInt());
    }

    @Test void emptySedeDoesNotFallBackToOtherClinics() throws Exception {
        var x = request(); x.parameters.put("idEspecialidad", "1"); x.parameters.put("idSede", "2");
        new ApiMedicosServlet(catalogos).service(x.request, x.response);
        assertEquals(0, assertBody(x, 200, "MEDICOS_OBTENIDOS").get("data").size());
        assertEquals(1, calls.get());
    }

    @Test void invalidIdsAre400BeforeQuery() throws Exception {
        var servlet = new ApiMedicosServlet(catalogos);
        for (String value : new String[]{"", "0", "-1", "abc", "1.2", "999999999999999999", "１２３"}) {
            var x = request(); x.parameters.put("idEspecialidad", value);
            servlet.service(x.request, x.response);
            assertTrue(assertBody(x, 400, "PARAMETRO_INVALIDO").get("data").isNull());
        }
        var missing = request(); servlet.service(missing.request, missing.response);
        assertBody(missing, 400, "PARAMETRO_INVALIDO");
        var sede = request(); sede.parameters.put("idEspecialidad", "1"); sede.parameters.put("idSede", "0");
        servlet.service(sede.request, sede.response); assertBody(sede, 400, "PARAMETRO_INVALIDO");
        assertEquals(0, calls.get());
    }

    @Test void datesAndHoursHaveSameEnvelope() throws Exception {
        var servlet = new ApiHorariosServlet(catalogos);
        var dates = request(); dates.parameters.put("idMedico", "42");
        servlet.service(dates.request, dates.response);
        assertEquals("2026-10-28", assertBody(dates, 200, "FECHAS_OBTENIDAS").get("data").get(0).asText());
        var hours = request(); hours.parameters.put("idMedico", "42"); hours.parameters.put("fecha", "2026-10-28");
        servlet.service(hours.request, hours.response);
        assertEquals("09:00", assertBody(hours, 200, "HORARIOS_OBTENIDOS").get("data").get(0).asText());
    }

    @Test void rejectsImpossibleAndNonIsoDatesBeforeQuery() throws Exception {
        var servlet = new ApiHorariosServlet(catalogos);
        for (String date : new String[]{"2026-02-30", "28/10/2026", "2026-1-2", "+10000-01-01", "0000-01-01", "abc"}) {
            var x = request(); x.parameters.put("idMedico", "42"); x.parameters.put("fecha", date);
            servlet.service(x.request, x.response);
            assertBody(x, 400, "FECHA_INVALIDA");
        }
        assertEquals(0, calls.get());
    }

    @Test void databaseFailureIs503WithoutPrivateDetails() throws Exception {
        var service = new CatalogoService(new MedicoDAO() {
            @Override public List<MedicoCatalogo> consultarMedicos(int especialidad, Integer sede) {
                throw new CatalogoDatosException(new java.sql.SQLException("credencial-privada"));
            }
        });
        var x = request(); x.parameters.put("idEspecialidad", "1");
        new ApiMedicosServlet(service).service(x.request, x.response);
        assertTrue(assertBody(x, 503, "CATALOGO_NO_DISPONIBLE").get("data").isNull());
        assertFalse(x.body.toString().contains("credencial-privada"));
    }

    @Test void unexpectedFailureIs500WithSameContract() throws Exception {
        var service = new CatalogoService(new MedicoDAO() {
            @Override public List<String> obtenerFechasDisponibles(int medico) { throw new IllegalStateException("detalle privado"); }
        });
        var x = request(); x.parameters.put("idMedico", "1");
        new ApiHorariosServlet(service).service(x.request, x.response);
        assertBody(x, 500, "ERROR_INTERNO"); assertFalse(x.body.toString().contains("detalle privado"));
    }

    @Test void rejectsWriteMethodsAndRetainsCsrfBeforeHandler() throws Exception {
        var servlet = new ApiMedicosServlet(catalogos);
        var x = request(); x.method = "POST";
        new CsrfFilter().doFilter(x.request, x.response, (req, res) -> servlet.service(x.request, x.response));
        assertBody(x, 403, "CSRF_INVALIDO");
        var token = request(); token.method = "POST";
        token.headers.put("X-CSRF-Token", CsrfToken.getOrCreate(token.session));
        new CsrfFilter().doFilter(token.request, token.response, (req, res) -> servlet.service(token.request, token.response));
        assertBody(token, 405, "METODO_NO_PERMITIDO"); assertEquals("GET", token.responseHeaders.get("Allow"));
        assertEquals(0, calls.get());
    }
}
