package pe.edu.utp.clinica.service.dni;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ApiPeruClient implements DniProvider {
    private static final int MAX_RESPONSE_BYTES = 16 * 1024;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(6);
    private static final ObjectMapper JSON = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private final String apiKey;
    private final Transport transport;

    @FunctionalInterface
    interface Transport { Reply send(HttpRequest request) throws IOException, InterruptedException; }
    record Reply(int status, byte[] body) {}

    public ApiPeruClient(String apiKey) {
        this(apiKey, realTransport());
    }

    ApiPeruClient(String apiKey, Transport transport) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.transport = transport;
    }

    private static Transport realTransport() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        return request -> {
            var pending = client.sendAsync(request, info -> new LimitedBodySubscriber());
            try {
                var response = pending.get(REQUEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                return new Reply(response.statusCode(), response.body());
            } catch (TimeoutException e) {
                pending.cancel(true);
                throw new HttpTimeoutException("Tiempo de consulta agotado");
            } catch (InterruptedException e) {
                pending.cancel(true);
                throw e;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof IOException cause) throw cause;
                throw new IOException("Fallo de transporte externo", e);
            }
        };
    }

    @Override
    public DniDatos consultar(String numero) throws DniException {
        DniService.validarNumero(numero);
        if (apiKey.length() > 4096 || !apiKey.matches("[A-Za-z0-9._~+/-]+=*")) {
            throw unavailable();
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                "https://dniruc.apisperu.com/api/v1/dni/" + numero
                        + "?token=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json").header("Content-Type", "application/json").GET().build();
        try {
            Reply reply = transport.send(request);
            if (reply.status() == 404) throw new DniException(404, "DNI_NO_ENCONTRADO",
                    "No se encontraron datos. Puedes completar el formulario manualmente.");
            if (reply.status() == 401 || reply.status() == 403 || reply.status() == 429
                    || reply.status() >= 500) throw unavailable();
            if (reply.status() != 200 || reply.body().length > MAX_RESPONSE_BYTES) throw invalidResponse();
            JsonNode root = JSON.readTree(reply.body());
            if (root != null && root.path("success").isBoolean() && !root.path("success").asBoolean()) {
                throw unavailable();
            }
            if (root == null || !root.isObject() || !numero.equals(text(root, "dni", false))) {
                throw invalidResponse();
            }
            return new DniDatos(numero, text(root, "nombres", false),
                    text(root, "apellidoPaterno", false), text(root, "apellidoMaterno", true));
        } catch (HttpTimeoutException e) {
            throw new DniException(504, "DNI_TIEMPO_AGOTADO",
                    "La consulta demoró demasiado. Puedes completar tus datos manualmente.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (IOException | IllegalArgumentException e) {
            throw invalidResponse();
        }
    }

    private static String text(JsonNode root, String field, boolean emptyAllowed) throws DniException {
        JsonNode value = root.get(field);
        if (value == null || !value.isTextual()) throw invalidResponse();
        String text = value.asText().trim();
        if ((!emptyAllowed && text.isEmpty()) || text.length() > 120
                || text.chars().anyMatch(Character::isISOControl)) throw invalidResponse();
        return text;
    }

    private static DniException unavailable() {
        return new DniException(503, "DNI_NO_DISPONIBLE",
                "La consulta no está disponible. Puedes completar tus datos manualmente.");
    }

    private static DniException invalidResponse() {
        return new DniException(502, "DNI_RESPUESTA_INVALIDA",
                "No fue posible obtener los datos. Puedes completar el formulario manualmente.");
    }

    static final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final HttpResponse.BodySubscriber<byte[]> delegate = HttpResponse.BodySubscribers.ofByteArray();
        private Flow.Subscription subscription;
        private long received;
        private boolean done;
        public CompletionStage<byte[]> getBody() { return delegate.getBody(); }
        public void onSubscribe(Flow.Subscription s) { subscription = s; delegate.onSubscribe(s); }
        public void onNext(List<ByteBuffer> buffers) {
            if (done) return;
            for (ByteBuffer buffer : buffers) received += buffer.remaining();
            if (received > MAX_RESPONSE_BYTES) {
                done = true;
                subscription.cancel();
                delegate.onError(new IOException("Respuesta excede el límite permitido"));
            } else delegate.onNext(buffers);
        }
        public void onError(Throwable error) { if (!done) { done = true; delegate.onError(error); } }
        public void onComplete() { if (!done) { done = true; delegate.onComplete(); } }
    }
}
