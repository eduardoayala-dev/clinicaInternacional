package pe.edu.utp.clinica.config;

import pe.edu.utp.clinica.dao.OfertasDAO;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

final class MantenimientoOfertas implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(MantenimientoOfertas.class.getName());
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "mantenimiento-ofertas");
        thread.setDaemon(true);
        return thread;
    });

    void iniciar() { executor.scheduleWithFixedDelay(this::procesar, 60, 60, TimeUnit.SECONDS); }
    private void procesar() {
        try { new OfertasDAO().procesarPendientes(); }
        catch (RuntimeException e) {
            LOGGER.warning(() -> "No se pudo completar el mantenimiento de ofertas: " + e.getClass().getSimpleName());
        }
    }
    @Override public void close() { executor.shutdownNow(); }
}
