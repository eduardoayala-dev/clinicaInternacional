package pe.edu.utp.clinica.config;

import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DisponibilidadSistemaTest {
    @Test void cachesChecksAndDetectsLossAndRecoveryWithoutPeriodicPolling() {
        AtomicLong clock = new AtomicLong();
        AtomicInteger calls = new AtomicInteger();
        try (var service = new DisponibilidadSistema(() -> calls.incrementAndGet() != 2, clock::get)) {
            assertTrue(service.disponible(1000));
            assertTrue(service.disponible(1000));
            assertEquals(1, calls.get());
            clock.addAndGet(TimeUnit.SECONDS.toNanos(6));
            assertFalse(service.disponible(1000));
            assertFalse(service.disponible(1000));
            assertEquals(2, calls.get());
            clock.addAndGet(TimeUnit.SECONDS.toNanos(6));
            assertTrue(service.disponible(1000));
            assertEquals(3, calls.get());
        }
    }

    @Test void sharesSlowStartupAndBoundsRequestWait() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        try (var service = new DisponibilidadSistema(() -> {
            calls.incrementAndGet();
            started.countDown();
            try { release.await(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
            return true;
        }, System::nanoTime)) {
            service.iniciar();
            assertTrue(started.await(1, TimeUnit.SECONDS));
            assertTimeoutPreemptively(java.time.Duration.ofSeconds(1), () -> {
                for (int i = 0; i < 10; i++) assertFalse(service.disponible(1));
            });
            assertEquals(1, calls.get());
            release.countDown();
            assertTrue(service.disponible(1000));
        } finally { release.countDown(); }
    }

    @Test void failureIsUnavailableAndShutdownPreventsNewChecks() {
        var service = new DisponibilidadSistema(() -> { throw new SQLException("no publicar", "08001"); }, System::nanoTime);
        try { assertFalse(service.disponible(1000)); }
        finally { service.close(); }
        assertFalse(service.disponible(1000));
    }
}
