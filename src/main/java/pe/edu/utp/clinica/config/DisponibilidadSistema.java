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
                    if (schema.getMetaData().getColumnCount()!=10) return false;
                }
            }
            if (Boolean.parseBoolean(System.getenv().getOrDefault("RECOVERY_ENABLED","false"))) {
                try (var schema=statement.executeQuery("SELECT id_verificacion,id_usuario,proposito,vinculo_hash,codigo_hash,etapa,intentos,vence_en FROM verificacion_cuenta WHERE 1=0")) {
                    if (schema.getMetaData().getColumnCount()!=8) return false;
                }
                try (var schema=statement.executeQuery("SELECT clave_hash,solicitudes,vence_en FROM limite_recuperacion WHERE 1=0")) {
                    if (schema.getMetaData().getColumnCount()!=3) return false;
                }
            }
            if(Boolean.parseBoolean(System.getenv().getOrDefault("APPOINTMENT_EMAIL_ENABLED","false"))
                    && !comprobarEsquemaCorreos(statement)) {
                return false;
            }
            try (var schema=statement.executeQuery("SELECT id_propuesta,id_medico,id_sede,estado FROM propuesta_horario WHERE 1=0")) {
                if (schema.getMetaData().getColumnCount()!=4) return false;
            }
            try (var schema=statement.executeQuery("SELECT id_propuesta FROM disponibilidad WHERE 1=0")) {
                if (schema.getMetaData().getColumnCount()!=1) return false;
            }
            return true;

        }
    }

    private static boolean comprobarEsquemaCorreos(java.sql.Statement statement) throws SQLException {
        try(var schema=statement.executeQuery("SELECT id_cita,estado,intentos,disponible_en,iniciada_en,finalizada_en FROM correo_cita WHERE 1=0")) {
            if(schema.getMetaData().getColumnCount()!=6)return false;
        }
        try(var schema=statement.executeQuery("SELECT id_oferta,estado,intentos,disponible_en,iniciada_en,finalizada_en FROM correo_oferta WHERE 1=0")) {
            return schema.getMetaData().getColumnCount()==6;
        }
    }

    @Override public synchronized void close() {
        cerrado = true;
        executor.shutdownNow();
    }
}
