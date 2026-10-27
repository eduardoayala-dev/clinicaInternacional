package pe.edu.utp.clinica.service.dni;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Flow;
import java.nio.ByteBuffer;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DecolectaClientTest {
    @Test void cancelsOversizedStreamingBodyBeforeAccumulatingIt() {
        var subscriber = new DecolectaClient.LimitedBodySubscriber();
        var cancelled = new AtomicInteger();
        subscriber.onSubscribe(new Flow.Subscription() {
            public void request(long n) {}
            public void cancel() { cancelled.incrementAndGet(); }
        });
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[16384])));
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[1])));
        subscriber.onComplete();
        assertEquals(1, cancelled.get());
        assertThrows(java.util.concurrent.CompletionException.class, () -> subscriber.getBody().toCompletableFuture().join());
    }

    @Test void acceptsSmallStreamingBody() {
        var subscriber = new DecolectaClient.LimitedBodySubscriber();
        subscriber.onSubscribe(new Flow.Subscription() { public void request(long n) {} public void cancel() {} });
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{1,2,3})));
        subscriber.onComplete();
        assertArrayEquals(new byte[]{1,2,3}, subscriber.getBody().toCompletableFuture().join());
    }
    private static final String BODY = """
            {"first_name":"PACIENTE PRUEBA","first_last_name":"FICTICIO",
             "second_last_name":"LOCAL","full_name":"NO EXPONER",
             "document_number":"99000001"}
            """;
    private DecolectaClient client(int status, String body) {
        return new DecolectaClient("token-simulado", req -> new DecolectaClient.Reply(status, body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test void buildsFixedHttpsRequestWithPrivateHeaderAndTimeout() throws Exception {
        var client = new DecolectaClient("token-simulado", req -> {
            assertEquals("https://api.decolecta.com/v1/reniec/dni?numero=99000001", req.uri().toString());
            assertEquals("GET", req.method());
            assertEquals("Bearer token-simulado", req.headers().firstValue("Authorization").orElseThrow());
            assertEquals(Duration.ofSeconds(6), req.timeout().orElseThrow());
            return new DecolectaClient.Reply(200, BODY.getBytes(StandardCharsets.UTF_8));
        });
        assertEquals(new DniDatos("99000001", "PACIENTE PRUEBA", "FICTICIO", "LOCAL"), client.consultar("99000001"));
    }

    @Test void missingKeyNeverSendsNetworkRequest() {
        AtomicInteger calls = new AtomicInteger();
        var client = new DecolectaClient("", req -> { calls.incrementAndGet(); throw new IOException(); });
        assertEquals(503, assertThrows(DniException.class, () -> client.consultar("99000001")).status());
        assertEquals(0, calls.get());
    }

    @Test void rejectsInvalidDniBeforeNetwork() {
        var client = client(200, BODY);
        for (String numero : new String[]{"123", "１２３４５６７８", "9900000X", "99000001&otro=1"}) {
            assertEquals(400, assertThrows(DniException.class, () -> client.consultar(numero)).status());
        }
    }

    @Test void rejectsMalformedMissingWrongDocumentAndUnexpectedTypes() {
        for (String body : new String[]{"<html>error</html>", "null", "[]", "{}",
                BODY.replace("99000001", "99000002"), BODY.replace("\"PACIENTE PRUEBA\"", "42"),
                BODY.replace("PACIENTE PRUEBA", ""), BODY.replace("PACIENTE PRUEBA", "X".repeat(121)),
                BODY + " {}", BODY + " ".repeat(16384)}) {
            assertEquals(502, assertThrows(DniException.class, () -> client(200, body).consultar("99000001")).status());
        }
    }

    @Test void permitsSingleSurname() throws Exception {
        assertEquals("", client(200, BODY.replace("\"LOCAL\"", "\"\"")).consultar("99000001").apellidoMaterno());
    }

    @Test void translatesProviderFailuresWithoutLeakingBodyOrToken() {
        for (int status : new int[]{301, 400, 401, 403, 404, 429, 500, 503}) {
            DniException ex = assertThrows(DniException.class,
                    () -> client(status, "token-simulado datos privados").consultar("99000001"));
            int expected = status == 404 ? 404 : status == 301 || status == 400 ? 502 : 503;
            assertEquals(expected, ex.status());
            assertFalse(ex.getMessage().contains("token-simulado"));
            assertFalse(ex.getMessage().contains("datos privados"));
        }
    }

    @Test void handlesTimeoutNetworkFailureAndInterruptedThread() {
        var timeout = new DecolectaClient("simulado", req -> { throw new HttpTimeoutException("privado"); });
        assertEquals(504, assertThrows(DniException.class, () -> timeout.consultar("99000001")).status());
        var network = new DecolectaClient("simulado", req -> { throw new IOException("privado"); });
        assertEquals(502, assertThrows(DniException.class, () -> network.consultar("99000001")).status());
        var interrupted = new DecolectaClient("simulado", req -> { throw new InterruptedException(); });
        try {
            assertEquals(503, assertThrows(DniException.class, () -> interrupted.consultar("99000001")).status());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
    }
}
