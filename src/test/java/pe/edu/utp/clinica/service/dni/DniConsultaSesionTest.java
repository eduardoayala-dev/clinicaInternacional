package pe.edu.utp.clinica.service.dni;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.support.HttpExchange;
import java.time.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class DniConsultaSesionTest {
    @Test void concurrentLookupsShareOneProviderCallWithoutBlockingOtherSessions() throws Exception {
        var first = new HttpExchange();
        var other = new HttpExchange();
        var calls = new AtomicInteger();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var datos = new DniDatos("99000001", "Prueba", "Ficticio", "Local");
        DniProvider provider = numero -> {
            calls.incrementAndGet();
            entered.countDown();
            try {
                assertTrue(release.await(10, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
            return datos;
        };
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            Future<DniDatos> pending = pool.submit(() -> DniConsultaSesion.obtenerOConsultar(first.session, "99000001", provider));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            Future<DniDatos> repeated = pool.submit(() -> DniConsultaSesion.obtenerOConsultar(first.session, "99000001", provider));
            Future<DniDatos> independent = pool.submit(() -> DniConsultaSesion.obtenerOConsultar(other.session, "99000001", numero -> datos));
            assertEquals(datos, independent.get(5, TimeUnit.SECONDS));
            release.countDown();
            assertEquals(datos, pending.get(5, TimeUnit.SECONDS));
            assertEquals(datos, repeated.get(5, TimeUnit.SECONDS));
            assertEquals(1, calls.get());
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test void providerFailureReleasesLockForTheNextRequest() throws Exception {
        var x = new HttpExchange();
        assertThrows(DniException.class, () -> DniConsultaSesion.obtenerOConsultar(x.session, "99000001", numero -> {
            throw new DniException(503, "DNI_NO_DISPONIBLE", "No disponible");
        }));
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<DniDatos> next = pool.submit(() -> DniConsultaSesion.obtenerOConsultar(x.session, "99000001",
                    numero -> new DniDatos(numero, "Prueba", "Ficticio", "Local")));
            assertEquals("Prueba", next.get(5, TimeUnit.SECONDS).nombres());
        } finally {
            pool.shutdownNow();
        }
    }
    @Test void bindsNamesToDocumentAndSession() {
        var a = new HttpExchange(); var b = new HttpExchange();
        var data = new DniDatos("99000001", "PRUEBA", "FICTICIO", "LOCAL");
        DniConsultaSesion.guardar(a.session, data);
        assertEquals(data, DniConsultaSesion.obtener(a.session, "99000001"));
        assertNull(DniConsultaSesion.obtener(a.session, "99000002"));
        assertNull(DniConsultaSesion.obtener(b.session, "99000001"));
        assertNull(DniConsultaSesion.obtener(null, "99000001"));
    }

    @Test void expiresAndBoundsStoredPersonalData() {
        var x = new HttpExchange();
        var now = Instant.parse("2026-10-27T00:00:00Z");
        for (int i = 0; i < 6; i++) {
            var clock = Clock.fixed(now.plusSeconds(i), ZoneOffset.UTC);
            DniConsultaSesion.guardar(x.session, new DniDatos("9900000" + i, "PRUEBA", "FICTICIO", "LOCAL"), clock);
        }
        assertNull(DniConsultaSesion.obtener(x.session, "99000000", Clock.fixed(now.plusSeconds(10), ZoneOffset.UTC)));
        assertNotNull(DniConsultaSesion.obtener(x.session, "99000005", Clock.fixed(now.plusSeconds(10), ZoneOffset.UTC)));
        assertNull(DniConsultaSesion.obtener(x.session, "99000005", Clock.fixed(now.plusSeconds(606), ZoneOffset.UTC)));
    }

    @Test void supportsContainerSessionSerializationWithoutLosingExpiry() throws Exception {
        var x = new HttpExchange();
        var data = new DniDatos("99000001", "Prueba", "Ficticio", "Local");
        DniConsultaSesion.guardar(x.session, data);
        String attribute = DniConsultaSesion.class.getName();
        var bytes = new ByteArrayOutputStream();
        try (var out = new ObjectOutputStream(bytes)) { out.writeObject(x.session.getAttribute(attribute)); }
        var restored = new HttpExchange();
        try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored.session.setAttribute(attribute, in.readObject());
        }
        assertEquals(data, DniConsultaSesion.obtener(restored.session, data.numero()));
    }
}
