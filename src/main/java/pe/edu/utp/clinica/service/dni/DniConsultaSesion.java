package pe.edu.utp.clinica.service.dni;

import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.Duration;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class DniConsultaSesion {
    private static final String ATTRIBUTE = DniConsultaSesion.class.getName();
    private static final long TTL = Duration.ofMinutes(10).toMillis();
    private static final int MAX_DOCUMENTS = 5;
    private record Consulta(DniDatos datos, long until) implements Serializable {}
    private static final class Cache implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Map<String, Consulta> entries = new HashMap<>();
    }
    private DniConsultaSesion() {}

    public static void guardar(HttpSession session, DniDatos datos) {
        guardar(session, datos, Clock.systemUTC());
    }

    static void guardar(HttpSession session, DniDatos datos, Clock clock) {
        synchronized (session) {
            long now = clock.millis();
            Cache cache = session.getAttribute(ATTRIBUTE) instanceof Cache existing ? existing : new Cache();
            Map<String, Consulta> entries = cache.entries;
            entries.entrySet().removeIf(e -> e.getValue().until <= now);
            if (entries.size() >= MAX_DOCUMENTS && !entries.containsKey(datos.numero())) {
                String oldest = entries.entrySet().stream().min(Comparator.comparingLong(e -> e.getValue().until))
                        .orElseThrow().getKey();
                entries.remove(oldest);
            }
            entries.put(datos.numero(), new Consulta(datos, now + TTL));
            session.setAttribute(ATTRIBUTE, cache);
        }
    }

    public static DniDatos obtener(HttpSession session, String numero) {
        return obtener(session, numero, Clock.systemUTC());
    }

    static DniDatos obtener(HttpSession session, String numero, Clock clock) {
        if (session == null) return null;
        synchronized (session) {
            if (!(session.getAttribute(ATTRIBUTE) instanceof Cache cache)) return null;
            cache.entries.entrySet().removeIf(e -> e.getValue().until <= clock.millis());
            if (cache.entries.isEmpty()) session.removeAttribute(ATTRIBUTE);
            else session.setAttribute(ATTRIBUTE, cache);
            Consulta entry = cache.entries.get(numero);
            return entry != null ? entry.datos : null;
        }
    }
}
