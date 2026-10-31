package pe.edu.utp.clinica.eventos;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public final class EventosAgenda {
    public static final EventosAgenda INSTANCIA = new EventosAgenda();
    private static final Logger LOGGER = Logger.getLogger(EventosAgenda.class.getName());
    private final AtomicLong secuencia = new AtomicLong();
    private final Map<Long, Suscriptor> suscriptores = new HashMap<>();
    private record Suscriptor(int sede, int usuario, Runnable aviso) {}
    public interface Suscripcion extends AutoCloseable { @Override void close(); }

    public synchronized Suscripcion suscribir(int sede, int usuario, Runnable aviso) {
        if (sede <= 0 || usuario <= 0) throw new IllegalArgumentException("Suscripción no válida.");
        long conexiones = suscriptores.values().stream().filter(s -> s.usuario() == usuario).count();
        if (suscriptores.size() >= 128 || conexiones >= 3) return null;
        long id = secuencia.incrementAndGet();
        suscriptores.put(id, new Suscriptor(sede, usuario, aviso));
        return () -> retirar(id);
    }

    private synchronized void retirar(long id) { suscriptores.remove(id); }

    public void publicar(int sede) {
        java.util.List<Runnable> avisos;
        synchronized (this) {
            avisos = suscriptores.values().stream().filter(s -> s.sede() == sede).map(Suscriptor::aviso).toList();
        }
        for (Runnable aviso : avisos) {
            try { aviso.run(); }
            catch (RuntimeException e) { LOGGER.warning("No se pudo entregar un aviso de actualización de agenda."); }
        }
    }
}
