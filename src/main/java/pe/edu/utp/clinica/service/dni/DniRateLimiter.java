package pe.edu.utp.clinica.service.dni;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public final class DniRateLimiter {
    private static final long CLIENT_WINDOW = Duration.ofMinutes(15).toMillis();
    private static final long HOUR = Duration.ofHours(1).toMillis();
    private static final long DAY = Duration.ofDays(1).toMillis();
    private static final int CLIENT_LIMIT = 5;
    private static final int HOURLY_LIMIT = 30;
    private static final int MAX_CLIENTS = 10000;
    private record Counter(long until, int used) {}
    private final Map<String, Counter> clients = new HashMap<>();
    private final Clock clock;
    private final int dailyLimit;
    private Counter hour = new Counter(0, 0);
    private Counter day = new Counter(0, 0);

    public DniRateLimiter(int dailyLimit) { this(Clock.systemUTC(), dailyLimit); }

    public DniRateLimiter(Clock clock, int dailyLimit) {
        if (dailyLimit < 1 || dailyLimit > 10000) throw new IllegalArgumentException("Límite DNI inválido");
        this.clock = clock;
        this.dailyLimit = dailyLimit;
    }

    public synchronized void acquire(String ip, String sessionId) throws DniException {
        long now = clock.millis();
        clients.entrySet().removeIf(entry -> entry.getValue().until <= now);
        hour = resetWindow(hour, now, HOUR);
        day = resetWindow(day, now, DAY);
        String ipKey = "ip:" + (ip == null ? "unknown" : ip);
        String sessionKey = "session:" + sessionId;
        Counter address = clients.get(ipKey);
        Counter session = clients.get(sessionKey);
        long blockedUntil = blockedUntil(address, session, now);
        if (blockedUntil > now) {
            throw new DniException(429, "DNI_LIMITE_CONSULTAS",
                    "Se alcanzó el límite de consultas. Completa tus datos manualmente o inténtalo más tarde.",
                    (int) ((blockedUntil - now + 999) / 1000));
        }
        clients.put(ipKey, increment(address, now, CLIENT_WINDOW));
        clients.put(sessionKey, increment(session, now, CLIENT_WINDOW));
        hour = increment(hour, now, HOUR);
        day = increment(day, now, DAY);
    }

    private long blockedUntil(Counter address, Counter session, long now) {
        long until = Math.max(deadline(address, CLIENT_LIMIT, now), deadline(session, CLIENT_LIMIT, now));
        until = Math.max(until, deadline(hour, HOURLY_LIMIT, now));
        until = Math.max(until, deadline(day, dailyLimit, now));
        if (clients.size() >= MAX_CLIENTS) until = Math.max(until, now + CLIENT_WINDOW);
        return until;
    }

    private static long deadline(Counter counter, int limit, long now) {
        return counter != null && counter.used >= limit ? counter.until : now;
    }

    private static Counter resetWindow(Counter counter, long now, long duration) {
        return counter.until <= now ? new Counter(now + duration, 0) : counter;
    }

    private static Counter increment(Counter counter, long now, long duration) {
        return counter == null ? new Counter(now + duration, 1) : new Counter(counter.until, counter.used + 1);
    }
}
