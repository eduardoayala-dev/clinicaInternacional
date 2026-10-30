package pe.edu.utp.clinica.config;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.function.LongSupplier;
import java.util.logging.Logger;

public final class DisponibilidadSistema implements AutoCloseable {
    public static final String CONTEXT_ATTRIBUTE = DisponibilidadSistema.class.getName();
    private static final Logger LOGGER = Logger.getLogger(DisponibilidadSistema.class.getName());
    private static final long INTERVALO_NANOS = TimeUnit.SECONDS.toNanos(5);

    @FunctionalInterface
    interface Comprobacion { boolean ejecutar() throws SQLException; }

    private final Comprobacion comprobacion;
    private final ExecutorService executor;
    private final LongSupplier reloj;
    private Future<Boolean> pendiente;
    private volatile long ultimaComprobacion;
    private volatile boolean comprobado;
    private volatile boolean cerrado;

    public DisponibilidadSistema() {
        this(DisponibilidadSistema::comprobarMysql, System::nanoTime);
    }

    DisponibilidadSistema(Comprobacion comprobacion, LongSupplier reloj) {
        this.comprobacion = comprobacion;
        this.reloj = reloj;
        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "disponibilidad-mysql");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void iniciar() { solicitarComprobacion(); }

    public boolean disponible(long esperaMilisegundos) {
        Future<Boolean> resultado = solicitarComprobacion();
        if (resultado == null) return false;
        try {
            return resultado.get(esperaMilisegundos, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (TimeoutException | ExecutionException e) {
            return false;
        }
    }

    private synchronized Future<Boolean> solicitarComprobacion() {
        if (cerrado) return null;
        if (pendiente != null && (!pendiente.isDone()
                || (comprobado && reloj.getAsLong() - ultimaComprobacion < INTERVALO_NANOS))) {
            return pendiente;
        }
        pendiente = executor.submit(() -> {
            try {
                return comprobacion.ejecutar();
            } catch (SQLException | RuntimeException e) {
                LOGGER.warning("MySQL no disponible: " + DatabaseConfig.diagnosticoSeguro(e));
                return false;
            } finally {
                ultimaComprobacion = reloj.getAsLong();
                comprobado = true;
            }
        });
        return pendiente;
    }

    private static boolean comprobarMysql() throws SQLException {
        try (var connection = DatabaseConfig.getConnection(); var statement = connection.createStatement()) {
            statement.setQueryTimeout(2);
            try (var rows = statement.executeQuery("SELECT 1")) {
                if (!rows.next() || rows.getInt(1)!=1) return false;
            }
            if ("jwt".equals(System.getenv("AUTH_MODE"))) {
                try (var schema=statement.executeQuery("SELECT id_sesion,id_usuario,refresh_hash,csrf_token,rol_inicial,credencial_hash,creada_en,ultima_actividad_en,vence_en,revocada_en FROM sesion_autenticacion WHERE 1=0")) {
                    return schema.getMetaData().getColumnCount()==10;
                }
            }
            return true;

        }
    }

    @Override public synchronized void close() {
        cerrado = true;
        executor.shutdownNow();
    }
}
