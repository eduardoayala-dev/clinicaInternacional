package pe.edu.utp.clinica.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.service.dni.*;
import pe.edu.utp.clinica.util.CsrfToken;
import java.io.IOException;

@WebServlet(name = "ConsultaDniServlet", urlPatterns = "/api/v1/documentos/dni/consulta")
public class ConsultaDniServlet extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private DniService service;
    private record Result(String code, String message, DniDatos data) {}

    public ConsultaDniServlet() {}
    ConsultaDniServlet(DniService service) { this.service = service; }

    @Override public void init() {
        int limit = 100;
        try {
            String configured = System.getenv("DNI_DAILY_LIMIT");
            if (configured != null && !configured.isBlank()) limit = Integer.parseInt(configured);
            if (limit < 1 || limit > 10000) throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            // Configuración inválida: deshabilitar consultas, sin impedir registro manual.
            service = new DniService(numero -> { throw new DniException(503,
                    "DNI_NO_DISPONIBLE", "La consulta no está disponible. Completa tus datos manualmente."); },
                    new DniRateLimiter(100));
            return;
        }
        service = new DniService(new DecolectaClient(System.getenv("DECOLECTA_API_KEY")), new DniRateLimiter(limit));
    }

    @Override protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json;charset=UTF-8");
        res.setHeader("Cache-Control", "no-store");
        res.setHeader("X-Content-Type-Options", "nosniff");
        try {
            if (!"POST".equals(req.getMethod())) {
                res.setHeader("Allow", "POST");
                throw new DniException(405, "METODO_NO_PERMITIDO", "Utiliza POST para consultar el DNI.");
            }
            // Defensa adicional y contrato uniforme, incluso fuera del filtro global.
            if (!CsrfToken.isValid(req)) throw new DniException(403, "CSRF_INVALIDO",
                    "La sesión o el token de seguridad no es válido. Recarga la página.");
            String type = req.getContentType();
            if (type == null || !type.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) {
                throw new DniException(415, "CONTENIDO_NO_ADMITIDO", "Envía el contenido como application/json.");
            }
            char[] buffer = new char[1025];
            int total = 0, read;
            var reader = req.getReader();
            while (total < buffer.length && (read = reader.read(buffer, total, buffer.length - total)) != -1) total += read;
            if (total > 1024) throw new DniException(413, "SOLICITUD_MUY_GRANDE", "La solicitud supera el tamaño permitido.");
            JsonNode root;
            try { root = JSON.readTree(new String(buffer, 0, total)); }
            catch (IOException e) { throw new DniException(400, "JSON_INVALIDO", "El cuerpo JSON no es válido."); }
            if (root == null || !root.isObject() || root.size() != 1 || !root.path("numero").isTextual()) {
                throw new DniException(400, "SOLICITUD_INVALIDA", "Envía únicamente el campo numero como texto.");
            }
            DniDatos data = service.consultar(root.get("numero").asText(), req.getRemoteAddr(), req.getSession(false).getId());
            JSON.writeValue(res.getWriter(), new Result("DNI_ENCONTRADO", "Datos consultados correctamente.", data));
        } catch (DniException e) {
            res.setStatus(e.status());
            if (e.status() == 429) res.setHeader("Retry-After", Integer.toString(e.retryAfter()));
            JSON.writeValue(res.getWriter(), new Result(e.code(), e.getMessage(), null));
        }
    }
}
