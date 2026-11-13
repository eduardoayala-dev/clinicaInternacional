package pe.edu.utp.clinica.eventos;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class EventosAgendaTest {
    @Test void notificaSoloLaSedeSuscritaYRetiraLaConexion() {
        var eventos = new EventosAgenda();
        var primera = new AtomicInteger();
        var segunda = new AtomicInteger();
        try (var a = eventos.suscribir(1, 10, primera::incrementAndGet);
             var b = eventos.suscribir(2, 20, segunda::incrementAndGet)) {
            eventos.publicar(1);
            assertEquals(1, primera.get());
            assertEquals(0, segunda.get());
        }
        eventos.publicar(1);
        assertEquals(1, primera.get());
    }

    @Test void limitaLasConexionesPorUsuarioYRecuperaElCupoAlCerrar() {
        var eventos = new EventosAgenda();
        try (var a = eventos.suscribir(1, 10, () -> {});
             var b = eventos.suscribir(1, 10, () -> {});
             var c = eventos.suscribir(1, 10, () -> {})) {
            assertNull(eventos.suscribir(1, 10, () -> {}));
        }
        try (var nueva = eventos.suscribir(1, 10, () -> {})) { assertNotNull(nueva); }
    }

    @Test void unReceptorDesconectadoNoInterrumpeAlResto() {
        var eventos = new EventosAgenda();
        var recibidos = new AtomicInteger();
        try (var a = eventos.suscribir(1, 10, () -> { throw new IllegalStateException(); });
             var b = eventos.suscribir(1, 20, recibidos::incrementAndGet)) {
            eventos.publicar(1);
            assertEquals(1, recibidos.get());
        }
    }
}
