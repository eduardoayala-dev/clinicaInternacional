package pe.edu.utp.clinica.service.dni;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/** Un solo contador por IP (no confiar en X-Forwarded-For) y presupuesto por instancia. */
public final class DniRateLimiter {
    private record Counter(long until, int used) {}
    private final Map<String, Counter> clients = new HashMap<>();
    private final Clock clock;
    private final int dailyLimit;
    private long hourUntil;
    private int hourUsed;
    private long dayUntil;
    private int dayUsed;

    public DniRateLimiter(int dailyLimit) { this(Clock.systemUTC(), dailyLimit); }
    public DniRateLimiter(Clock clock, int dailyLimit) {
        if (dailyLimit < 1 || dailyLimit > 10000) throw new IllegalArgumentException("Límite DNI inválido");
        this.clock = clock;
        this.dailyLimit = dailyLimit;
    }

    public synchronized void acquire(String ip, String sessionId) throws DniException {
        long now = clock.millis();
        clients.entrySet().removeIf(e -> e.getValue().until <= now);
        if (now >= hourUntil) { hourUntil = now + Duration.ofHours(1).toMillis(); hourUsed = 0; }
        if (now >= dayUntil) { dayUntil = now + Duration.ofDays(1).toMillis(); dayUsed = 0; }
        String ipKey = "ip:" + (ip == null ? "unknown" : ip);
        String sessionKey = "session:" + sessionId;
        Counter address = clients.get(ipKey), session = clients.get(sessionKey);
        if ((address != null && address.used >= 5) || (session != null && session.used >= 5)
                || hourUsed >= 30 || dayUsed >= dailyLimit || clients.size() >= 10000) {
            long blockedUntil = now + 1000;
            if (address != null && address.used >= 5) blockedUntil = Math.max(blockedUntil, address.until);
            if (session != null && session.used >= 5) blockedUntil = Math.max(blockedUntil, session.until);
            if (hourUsed >= 30) blockedUntil = Math.max(blockedUntil, hourUntil);
            if (dayUsed >= dailyLimit) blockedUntil = Math.max(blockedUntil, dayUntil);
            if (clients.size() >= 10000) blockedUntil = Math.max(blockedUntil, now + 900000);
            throw new DniException(429, "DNI_LIMITE_CONSULTAS",
                    "Se alcanzó el límite de consultas. Completa tus datos manualmente o inténtalo más tarde.",
                    (int) ((blockedUntil - now + 999) / 1000));
        }
        long until = now + Duration.ofMinutes(15).toMillis();
        clients.put(ipKey, new Counter(address == null ? until : address.until, address == null ? 1 : address.used + 1));
        clients.put(sessionKey, new Counter(session == null ? until : session.until, session == null ? 1 : session.used + 1));
        hourUsed++;
        dayUsed++;
    }
}
