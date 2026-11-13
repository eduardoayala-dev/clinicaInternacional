package pe.edu.utp.clinica.service.dni;

import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.Duration;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public final class DniConsultaSesion {
    private static final String ATTRIBUTE = DniConsultaSesion.class.getName();
    private static final long TTL = Duration.ofMinutes(10).toMillis();
    private static final int MAX_DOCUMENTS = 5;
    private static final ReentrantLock CREATION_LOCK = new ReentrantLock();
    private record Consulta(DniDatos datos, long until) implements Serializable {}

    private static final class Cache implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Map<String, Consulta> entries = new HashMap<>();
        private final ReentrantLock lock = new ReentrantLock();

        private DniDatos find(String numero, long now) {
            entries.entrySet().removeIf(entry -> entry.getValue().until <= now);
            Consulta entry = entries.get(numero);
            return entry == null ? null : entry.datos;
        }

        private void store(DniDatos datos, long now) {
            entries.entrySet().removeIf(entry -> entry.getValue().until <= now);
            if (entries.size() >= MAX_DOCUMENTS && !entries.containsKey(datos.numero())) {
                String oldest = entries.entrySet().stream()
                        .min(Comparator.comparingLong(entry -> entry.getValue().until)).orElseThrow().getKey();
                entries.remove(oldest);
            }
            entries.put(datos.numero(), new Consulta(datos, now + TTL));
        }
    }

    private DniConsultaSesion() {}

    private static Cache cacheFor(HttpSession session) {
        CREATION_LOCK.lock();
        try {
            if (session.getAttribute(ATTRIBUTE) instanceof Cache cache) return cache;
            Cache cache = new Cache();
            session.setAttribute(ATTRIBUTE, cache);
            return cache;
        } finally {
            CREATION_LOCK.unlock();
        }
    }

    public static void guardar(HttpSession session, DniDatos datos) {
        guardar(session, datos, Clock.systemUTC());
    }

    static void guardar(HttpSession session, DniDatos datos, Clock clock) {
        Cache cache = cacheFor(session);
        cache.lock.lock();
        try {
            cache.store(datos, clock.millis());
            session.setAttribute(ATTRIBUTE, cache);
        } finally {
            cache.lock.unlock();
        }
    }

    public static DniDatos obtener(HttpSession session, String numero) {
        return obtener(session, numero, Clock.systemUTC());
    }

    static DniDatos obtener(HttpSession session, String numero, Clock clock) {
        if (session == null) return null;
        Cache cache = cacheFor(session);
        cache.lock.lock();
        try {
            DniDatos datos = cache.find(numero, clock.millis());
            session.setAttribute(ATTRIBUTE, cache);
            return datos;
        } finally {
            cache.lock.unlock();
        }
    }

    public static DniDatos obtenerOConsultar(HttpSession session, String numero, DniProvider provider)
            throws DniException {
        Cache cache = cacheFor(session);
        cache.lock.lock();
        try {
            DniDatos datos = cache.find(numero, System.currentTimeMillis());
            if (datos == null) {
                datos = provider.consultar(numero);
                cache.store(datos, System.currentTimeMillis());
            }
            session.setAttribute(ATTRIBUTE, cache);
            return datos;
        } finally {
            cache.lock.unlock();
        }
    }
}
