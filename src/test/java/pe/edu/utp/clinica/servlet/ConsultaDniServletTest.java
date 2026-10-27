package pe.edu.utp.clinica.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.filter.CsrfFilter;
import pe.edu.utp.clinica.service.dni.*;
import pe.edu.utp.clinica.support.HttpExchange;
import pe.edu.utp.clinica.util.CsrfToken;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ConsultaDniServletTest {
    private final AtomicInteger calls = new AtomicInteger();
    private final ConsultaDniServlet servlet = new ConsultaDniServlet(new DniService(numero -> {
        calls.incrementAndGet(); return new DniDatos(numero, "PACIENTE PRUEBA", "FICTICIO", "LOCAL");
    }, new DniRateLimiter(100)));
    private HttpExchange request() {
        var x = new HttpExchange(); x.path = "/api/v1/documentos/dni/consulta";
        x.requestBody = "{\"numero\":\"99000001\"}";
        x.headers.put("X-CSRF-Token", CsrfToken.getOrCreate(x.session));
        return x;
    }
    private void execute(HttpExchange x) throws Exception {
        new CsrfFilter().doFilter(x.request, x.response, (req, res) -> servlet.service(x.request, x.response));
    }
    private void assertContract(HttpExchange x, int status, String code) throws Exception {
        assertEquals(status, x.status);
        var json = new ObjectMapper().readTree(x.body.toString());
        assertEquals(3, json.size()); assertEquals(code, json.get("code").asText());
        assertTrue(json.get("message").isTextual()); assertTrue(json.has("data"));
        assertEquals("no-store", x.responseHeaders.get("Cache-Control"));
    }
    @Test void returnsOwnContractAndNoProviderKeyOrFullName() throws Exception {
        var x = request(); execute(x); assertContract(x, 200, "DNI_ENCONTRADO");
        var data = new ObjectMapper().readTree(x.body.toString()).get("data");
        assertEquals(4, data.size()); assertEquals("99000001", data.get("numero").asText());
        assertEquals("Paciente Prueba", DniConsultaSesion.obtener(x.session, "99000001").nombres());
        assertEquals("Paciente Prueba", data.get("nombres").asText());
        assertEquals(1, calls.get()); assertFalse(x.body.toString().contains("codVerifica"));
    }
    @Test void rejectsMissingForeignAndExpiredCsrfBeforeProvider() throws Exception {
        for (int mode = 0; mode < 3; mode++) {
            var x = request();
            if (mode == 0) x.headers.clear();
            if (mode == 1) x.headers.put("X-CSRF-Token", CsrfToken.getOrCreate(new HttpExchange().session));
            if (mode == 2) x.session = null;
            execute(x); assertContract(x, 403, "CSRF_INVALIDO");
        }
        assertEquals(0, calls.get());
    }
    @Test void rejectsWrongMediaTypeOversizedAndMalformedInput() throws Exception {
        var media = request(); media.requestContentType = "text/plain"; execute(media);
        assertContract(media, 415, "CONTENIDO_NO_ADMITIDO");
        var large = request(); large.requestBody = " ".repeat(1025); execute(large);
        assertContract(large, 413, "SOLICITUD_MUY_GRANDE");
        for (String body : new String[]{"{", "{} {}"}) {
            var x = request(); x.requestBody = body; execute(x); assertContract(x, 400, "JSON_INVALIDO");
        }
        for (String body : new String[]{"null", "[]", "{}", "{\"numero\":99000001}",
                "{\"numero\":\"99000001\",\"extra\":true}"}) {
            var x = request(); x.requestBody = body; execute(x); assertContract(x, 400, "SOLICITUD_INVALIDA");
        }
        assertEquals(0, calls.get());
    }
    @Test void invalidDniAndUnsupportedMethodNeverReachProvider() throws Exception {
        var x = request(); x.requestBody = "{\"numero\":\"123\"}"; execute(x);
        assertContract(x, 400, "DNI_INVALIDO");
        var get = request(); get.method = "GET"; execute(get);
        assertContract(get, 405, "METODO_NO_PERMITIDO"); assertEquals("POST", get.responseHeaders.get("Allow"));
        assertEquals(0, calls.get());
    }
    @Test void excessRequestsReturn429AndDoNotReachProvider() throws Exception {
        for (int i = 0; i < 5; i++) execute(request());
        var x = request(); execute(x); assertContract(x, 429, "DNI_LIMITE_CONSULTAS");
        assertNotNull(x.responseHeaders.get("Retry-After")); assertEquals(5, calls.get());
    }
    @Test void repeatedLookupInSameSessionReusesDataWithoutSpendingBudget() throws Exception {
        var original = request(); execute(original);
        for (int i = 0; i < 8; i++) {
            var reload = request(); reload.session = original.session;
            reload.headers.put("X-CSRF-Token", CsrfToken.getOrCreate(reload.session));
            execute(reload); assertContract(reload, 200, "DNI_ENCONTRADO");
        }
        assertEquals(1, calls.get());
        execute(request()); assertEquals(2, calls.get());
    }
    @Test void cachedDniRemainsAvailableWhenOtherLookupsReachLimit() throws Exception {
        var original = request(); execute(original);
        for (int i = 0; i < 4; i++) execute(request());
        var blocked = request(); execute(blocked);
        assertContract(blocked, 429, "DNI_LIMITE_CONSULTAS");
        assertTrue(Integer.parseInt(blocked.responseHeaders.get("Retry-After")) > 0);
        var cached = request(); cached.session = original.session;
        cached.headers.put("X-CSRF-Token", CsrfToken.getOrCreate(cached.session));
        execute(cached); assertContract(cached, 200, "DNI_ENCONTRADO");
        assertEquals(5, calls.get());
    }
    @Test void providerFailuresRetainSameEnvelope() throws Exception {
        var unavailable = new ConsultaDniServlet(new DniService(numero -> {
            throw new DniException(503, "DNI_NO_DISPONIBLE", "Completa tus datos manualmente.");
        }, new DniRateLimiter(100)));
        var x = request(); unavailable.service(x.request,x.response);
        assertContract(x, 503, "DNI_NO_DISPONIBLE");
        assertTrue(new ObjectMapper().readTree(x.body.toString()).get("data").isNull());
    }
}
