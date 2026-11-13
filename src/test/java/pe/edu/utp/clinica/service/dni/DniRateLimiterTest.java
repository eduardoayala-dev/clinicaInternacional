package pe.edu.utp.clinica.service.dni;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class DniRateLimiterTest {
    static class Time extends Clock {
        Instant now = Instant.parse("2026-10-27T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
    @Test void rotatingSessionsDoesNotBypassIpAndWindowExpires() throws Exception {
        Time clock = new Time(); var limiter = new DniRateLimiter(clock, 100);
        for (int i = 0; i < 5; i++) limiter.acquire("ip-a", "session-" + i);
        assertEquals(429, assertThrows(DniException.class, () -> limiter.acquire("ip-a", "new")).status());
        limiter.acquire("ip-b", "other");
        clock.now = clock.now.plusSeconds(901);
        limiter.acquire("ip-a", "new");
    }
    @Test void rotatingAddressesDoesNotBypassSession() throws Exception {
        var limiter = new DniRateLimiter(100);
        for (int i = 0; i < 5; i++) limiter.acquire("ip-" + i, "same");
        assertThrows(DniException.class, () -> limiter.acquire("new", "same"));
    }
    @Test void globalHourlyAndDailyBudgetsSurviveChangingClients() throws Exception {
        Time clock = new Time(); var limiter = new DniRateLimiter(clock, 31);
        for (int i = 0; i < 30; i++) limiter.acquire("ip-" + i, "s-" + i);
        assertThrows(DniException.class, () -> limiter.acquire("next", "next"));
        clock.now = clock.now.plusSeconds(3601);
        limiter.acquire("next", "next");
        assertThrows(DniException.class, () -> limiter.acquire("last", "last"));
        clock.now = clock.now.plusSeconds(86400);
        limiter.acquire("last", "last");
    }
    @Test void concurrentRequestsCannotOverspendBudget() throws Exception {
        var limiter = new DniRateLimiter(3); var accepted = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            var futures = new java.util.ArrayList<Future<?>>();
            for (int i = 0; i < 20; i++) {
                final int n = i;
                futures.add(pool.submit(() -> {
                    try { limiter.acquire("ip-" + n, "s-" + n); accepted.incrementAndGet(); }
                    catch (DniException expected) { assertEquals(429, expected.status()); }
                }));
            }
            for (Future<?> future : futures) future.get();
            assertEquals(3, accepted.get());
        } finally { pool.shutdownNow(); }
    }
    @Test void invalidDniDoesNotConsumeBudget() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        var service = new DniService(numero -> { calls.incrementAndGet(); return new DniDatos(numero,"P","F","L"); }, new DniRateLimiter(1));
        assertThrows(DniException.class, () -> service.consultar("ABC", "ip", "s"));
        service.consultar("99000001", "ip", "s");
        assertThrows(DniException.class, () -> service.consultar("99000001", "ip", "s"));
        assertEquals(1, calls.get());
    }
}
