package pe.edu.utp.clinica.correo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Flow;
import static org.junit.jupiter.api.Assertions.*;

class GmailCorreoTest {
    private static final Correo CORREO = new Correo("paciente@example.test", "Confirmación de cita", "Pérez Núñez\nCita confirmada.");
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-30T12:00:00Z"), ZoneOffset.UTC);
    private static final String ACCESO = "{\"access_token\":\"temporal\",\"token_type\":\"Bearer\",\"expires_in\":3600}";

    private GmailCorreo emisor(GmailCorreo.Transporte transporte) {
        return new GmailCorreo("cliente", "secreto", "renovacion", "clinica@gmail.com", transporte, RELOJ);
    }

    @Test void renuevaUnaVezYEnviaMimePrivadoCorrecto() throws Exception {
        var peticiones = new ArrayList<HttpRequest>();
        var emisor = emisor(peticion -> {
            peticiones.add(peticion);
            return new GmailCorreo.Respuesta(200, peticion.uri().getPath().equals("/token") ? ACCESO : "{\"id\":\"abc123\"}");
        });
        assertEquals("abc123", emisor.enviar(CORREO));
        assertEquals("abc123", emisor.enviar(CORREO));
        assertEquals(3, peticiones.size());
        assertEquals("https://oauth2.googleapis.com/token", peticiones.get(0).uri().toString());
        assertEquals("grant_type=refresh_token&client_id=cliente&client_secret=secreto&refresh_token=renovacion", cuerpo(peticiones.get(0)));
        var envio = peticiones.get(1);
        assertEquals("https://gmail.googleapis.com/gmail/v1/users/me/messages/send", envio.uri().toString());
        assertEquals("Bearer temporal", envio.headers().firstValue("Authorization").orElseThrow());
        String raw = new ObjectMapper().readTree(cuerpo(envio)).path("raw").asText();
        String mime = new String(Base64.getUrlDecoder().decode(raw), StandardCharsets.UTF_8);
        assertTrue(mime.contains("<clinica@gmail.com>\r\nTo: paciente@example.test\r\nSubject: =?UTF-8?B?"));
        assertEquals(CORREO.texto(), new String(Base64.getMimeDecoder().decode(mime.split("\r\n\r\n", 2)[1]), StandardCharsets.UTF_8));
        assertFalse(cuerpo(envio).contains("renovacion"));
        assertFalse(cuerpo(envio).contains("secreto"));
    }

    @Test void invalidaAcceso401SinRepetirEnvio() throws Exception {
        var renovaciones = new AtomicInteger();
        var envios = new AtomicInteger();
        var emisor = emisor(peticion -> {
            if (peticion.uri().getPath().equals("/token")) {
                renovaciones.incrementAndGet(); return new GmailCorreo.Respuesta(200, ACCESO);
            }
            return new GmailCorreo.Respuesta(envios.incrementAndGet() == 1 ? 401 : 200, "{\"id\":\"nuevo\"}");
        });
        assertFalse(assertThrows(EnvioCorreoException.class, () -> emisor.enviar(CORREO)).reintentable());
        assertEquals(1, envios.get());
        assertEquals("nuevo", emisor.enviar(CORREO));
        assertEquals(2, renovaciones.get());
    }

    @Test void erroresOAuthNoIntentanEnviarNiRevelanSecretos() {
        var llamadas = new AtomicInteger();
        var emisor = emisor(peticion -> {
            llamadas.incrementAndGet();
            return new GmailCorreo.Respuesta(400, "{\"error\":\"invalid_grant\",\"privado\":\"secreto\"}");
        });
        var error = assertThrows(EnvioCorreoException.class, () -> emisor.enviar(CORREO));
        assertEquals(1, llamadas.get());
        assertFalse(error.reintentable());
        assertFalse(error.getMessage().contains("secreto"));
    }

    @Test void rechazaRenovacionesIncompletas() {
        for (String datos : List.of("{}", "no JSON", "{\"access_token\":\"a\",\"expires_in\":3600}",
                "{\"access_token\":\"a\",\"token_type\":\"Bearer\",\"expires_in\":-1}")) {
            var emisor = emisor(peticion -> new GmailCorreo.Respuesta(200, datos));
            assertThrows(EnvioCorreoException.class, () -> emisor.enviar(CORREO));
        }
    }

    @Test void erroresDeEntregaNoSonAceptacionYSeClasificaCuota() {
        for (int estado : new int[]{301, 400, 403, 429, 500, 503}) {
            var emisor = emisor(peticion -> new GmailCorreo.Respuesta(
                    peticion.uri().getPath().equals("/token") ? 200 : estado,
                    peticion.uri().getPath().equals("/token") ? ACCESO : "respuesta privada"));
            var error = assertThrows(EnvioCorreoException.class, () -> emisor.enviar(CORREO));
            assertEquals(estado == 429 || estado >= 500, error.reintentable());
            assertFalse(error.getMessage().contains("privada"));
        }
    }

    @Test void exigeIdDeAceptacion() {
        for (String datos : List.of("{}", "no JSON", "{\"id\":\"\"}")) {
            var emisor = emisor(peticion -> new GmailCorreo.Respuesta(200,
                    peticion.uri().getPath().equals("/token") ? ACCESO : datos));
            assertThrows(EnvioCorreoException.class, () -> emisor.enviar(CORREO));
        }
    }

    @Test void redInciertaNoReintentaNiIncluyeCausaPrivada() {
        var llamadas = new AtomicInteger();
        var emisor = emisor(peticion -> { llamadas.incrementAndGet(); throw new IOException("secreto"); });
        var error = assertThrows(EnvioCorreoException.class, () -> emisor.enviar(CORREO));
        assertEquals(1, llamadas.get());
        assertFalse(error.reintentable());
        assertNull(error.getCause());
    }

    @Test void interrupcionNoSePierde() {
        var emisor = emisor(peticion -> { throw new InterruptedException(); });
        try {
            assertThrows(EnvioCorreoException.class, () -> emisor.enviar(CORREO));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
    }

    @Test void configuracionDesactivadaYValidacionDeDatos() {
        assertThrows(EnvioCorreoException.class, () -> ConfiguracionCorreo.crear(Map.of()).enviar(CORREO));
        assertThrows(IllegalArgumentException.class, () -> ConfiguracionCorreo.crear(Map.of("MAIL_MODE", "gmail")));
        assertThrows(IllegalArgumentException.class, () -> ConfiguracionCorreo.crear(Map.of("MAIL_MODE", "incorrecto")));
        assertThrows(IllegalArgumentException.class, () -> new Correo("a,b@example.test", "Hola", "texto"));
        assertThrows(IllegalArgumentException.class, () -> new Correo("a@example.test", "Hola\r\nBcc:otro", "texto"));
        assertFalse(CORREO.toString().contains("paciente"));
    }

    @Test void asuntosUnicodeLargosSePlieganSinRomperCaracteres() {
        String asunto = "Ñá😀".repeat(30);
        String mime = GmailCorreo.mime(new Correo("p@example.test", asunto, "texto"), "clinica@gmail.com");
        String cabecera = mime.substring(mime.indexOf("Subject: ") + 9, mime.indexOf("\r\nMIME-Version"));
        var reconstruido = new StringBuilder();
        for (String palabra : cabecera.split("\r\n ")) {
            assertTrue(palabra.length() < 75);
            reconstruido.append(new String(Base64.getDecoder().decode(palabra.substring(10, palabra.length() - 2)), StandardCharsets.UTF_8));
        }
        assertEquals(asunto, reconstruido.toString());
    }

    @Test void renovacionSeComparteEntreEnviosConcurrentes() throws Exception {
        var renovaciones = new AtomicInteger();
        var emisor = emisor(peticion -> {
            if (peticion.uri().getPath().equals("/token")) {
                renovaciones.incrementAndGet(); return new GmailCorreo.Respuesta(200, ACCESO);
            }
            return new GmailCorreo.Respuesta(200, "{\"id\":\"aceptado\"}");
        });
        var ejecutor = Executors.newFixedThreadPool(4);
        try {
            var tareas = new ArrayList<Future<String>>();
            for (int i = 0; i < 8; i++) tareas.add(ejecutor.submit(() -> emisor.enviar(CORREO)));
            for (var tarea : tareas) assertEquals("aceptado", tarea.get(5, TimeUnit.SECONDS));
            assertEquals(1, renovaciones.get());
        } finally { ejecutor.shutdownNow(); }
    }

    @Test void renuevaAntesDeCaducarElAcceso() throws Exception {
        var momento = new java.util.concurrent.atomic.AtomicReference<>(RELOJ.instant());
        Clock variable = new Clock() {
            @Override public ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(ZoneId zona) { return this; }
            @Override public Instant instant() { return momento.get(); }
        };
        var renovaciones = new AtomicInteger();
        var emisor = new GmailCorreo("cliente", "secreto", "renovacion", "clinica@gmail.com", peticion -> {
            if (peticion.uri().getPath().equals("/token")) {
                renovaciones.incrementAndGet(); return new GmailCorreo.Respuesta(200, ACCESO);
            }
            return new GmailCorreo.Respuesta(200, "{\"id\":\"aceptado\"}");
        }, variable);
        emisor.enviar(CORREO);
        momento.set(RELOJ.instant().plusSeconds(3569));
        emisor.enviar(CORREO);
        assertEquals(1, renovaciones.get());
        momento.set(RELOJ.instant().plusSeconds(3570));
        emisor.enviar(CORREO);
        assertEquals(2, renovaciones.get());
    }

    private static String cuerpo(HttpRequest peticion) throws Exception {
        var resultado = new CompletableFuture<String>();
        var contenido = new java.io.ByteArrayOutputStream();
        peticion.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            @Override public void onSubscribe(Flow.Subscription suscripcion) { suscripcion.request(Long.MAX_VALUE); }
            @Override public void onNext(ByteBuffer parte) {
                byte[] bytes = new byte[parte.remaining()]; parte.get(bytes); contenido.writeBytes(bytes);
            }
            @Override public void onError(Throwable error) { resultado.completeExceptionally(error); }
            @Override public void onComplete() { resultado.complete(contenido.toString(StandardCharsets.UTF_8)); }
        });
        return resultado.get(5, TimeUnit.SECONDS);
    }
}
