package pe.edu.utp.clinica.eventos;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.edu.utp.clinica.model.Usuario;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class CanalAgendaTomcatTest {
    @TempDir Path temporal;

    @Test void entregaEventosPorHttpSinEsperarElCierreDeLaRespuesta() throws Exception {
        var eventos = new EventosAgenda();
        var canalActivo = new AtomicReference<CanalAgenda>();
        var tomcat = new Tomcat();
        var lector = Executors.newSingleThreadExecutor();
        tomcat.setBaseDir(temporal.toString());
        tomcat.setPort(0);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        var contexto = tomcat.addContext("", temporal.toString());
        var servlet = new HttpServlet() {
            @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
                request.getSession().setAttribute("usuarioLogueado",
                        new Usuario(1, "Personal", "Sintético", "prueba@example.test", "", "PERSONAL_ADMINISTRATIVO", null, true));
                response.setContentType("text/event-stream");
                var canal = new CanalAgenda(request.startAsync(), response.getOutputStream(), request.getSession(), 1, ignorado -> {});
                canalActivo.set(canal);
                canal.iniciar(eventos.suscribir(1, 1, canal::cambio), 15000);
            }
        };
        Tomcat.addServlet(contexto, "eventos-prueba", servlet).setAsyncSupported(true);
        contexto.addServletMappingDecoded("/eventos", "eventos-prueba");
        try {
            tomcat.start();
            var cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            var peticion = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/eventos"))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            var respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofInputStream());
            assertEquals(200, respuesta.statusCode());
            try (var entrada = new BufferedReader(new InputStreamReader(respuesta.body(), StandardCharsets.UTF_8))) {
                assertEquals("sincronizar", lector.submit(() -> siguienteEvento(entrada)).get(5, TimeUnit.SECONDS));
                eventos.publicar(1);
                assertEquals("cambio", lector.submit(() -> siguienteEvento(entrada)).get(5, TimeUnit.SECONDS));
            }
        } finally {
            if (canalActivo.get() != null) canalActivo.get().close();
            lector.shutdownNow();
            tomcat.stop();
            tomcat.destroy();
        }
    }

    private static String siguienteEvento(BufferedReader entrada) throws IOException {
        String evento = null;
        String linea;
        while ((linea = entrada.readLine()) != null) {
            if (linea.startsWith("event: ")) evento = linea.substring(7);
            if (linea.startsWith("data: ")) assertEquals("data: {}", linea);
            if (linea.isEmpty() && evento != null) return evento;
        }
        throw new IOException("El canal terminó antes del evento esperado.");
    }
}
