package pe.edu.utp.clinica.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class ApiJson {
    private static final ObjectMapper JSON = new ObjectMapper();
    private ApiJson() {}

    public static void write(HttpServletResponse response, int status, ApiResponse<?> body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        JSON.writeValue(response.getWriter(), body);
    }
}
