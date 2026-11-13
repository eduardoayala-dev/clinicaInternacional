package pe.edu.utp.clinica.config;

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.LongSupplier;
import java.util.logging.Logger;

public final class DisponibilidadSistema implements AutoCloseable {
    private static final String FALSE = "false";
    public static final String CONTEXT_ATTRIBUTE = DisponibilidadSistema.class.getName();
    private static final Logger LOGGER = Logger.getLogger(DisponibilidadSistema.class.getName());
    private static final long INTERVALO_NANOS = TimeUnit.SECONDS.toNanos(5);

    @FunctionalInterface
    interface Comprobacion {
        boolean ejecutar() throws SQLException;
    }

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
        this.executor =
                Executors.newSingleThreadExecutor(
                        task -> {
                            Thread thread = new Thread(task, "disponibilidad-mysql");
                            thread.setDaemon(true);
                            return thread;
                        });
    }

    public void iniciar() {
        solicitarComprobacion();
    }

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
        if (pendiente != null
                && (!pendiente.isDone()
                        || (comprobado
                                && reloj.getAsLong() - ultimaComprobacion < INTERVALO_NANOS))) {
            return pendiente;
        }
        pendiente =
                executor.submit(
                        () -> {
                            try {
                                return comprobacion.ejecutar();
                            } catch (SQLException | RuntimeException e) {
                                LOGGER.warning(
                                        "MySQL no disponible: "
                                                + DatabaseConfig.diagnosticoSeguro(e));
                                return false;
                            } finally {
                                ultimaComprobacion = reloj.getAsLong();
                                comprobado = true;
                            }
                        });
        return pendiente;
    }

    private static boolean comprobarMysql() throws SQLException {
        try (var conexion = DatabaseConfig.getConnection()) {
            if (!conexion.isValid(2)) return false;
            var repo = new pe.edu.utp.clinica.repository.EsquemaRepository(conexion);
            var columnas = repo.columnas();
            if ("jwt".equals(System.getenv("AUTH_MODE"))
                    && !columnas(
                            columnas,
                            "sesion_autenticacion",
                            "id_sesion",
                            "id_usuario",
                            "refresh_hash",
                            "csrf_token",
                            "rol_inicial",
                            "credencial_hash",
                            "creada_en",
                            "ultima_actividad_en",
                            "vence_en",
                            "revocada_en")) return false;
            if (habilitado("RECOVERY_ENABLED")
                    && (!columnas(
                                    columnas,
                                    "verificacion_cuenta",
                                    "id_verificacion",
                                    "id_usuario",
                                    "proposito",
                                    "vinculo_hash",
                                    "codigo_hash",
                                    "etapa",
                                    "intentos",
                                    "vence_en")
                            || !columnas(
                                    columnas,
                                    "limite_recuperacion",
                                    "clave_hash",
                                    "solicitudes",
                                    "vence_en"))) return false;
            if (habilitado("APPOINTMENT_EMAIL_ENABLED")) {
                if (habilitado("RECORDATORIOS_EMAIL_ENABLED")
                        && !columnas(
                                columnas,
                                "correo_recordatorio",
                                "id_cita",
                                "estado",
                                "intentos",
                                "disponible_en",
                                "iniciada_en",
                                "finalizada_en")) return false;
                if (!columnas(
                                columnas,
                                "correo_cambio_cita",
                                "id_aviso",
                                "id_cita",
                                "id_cita_anterior",
                                "tipo",
                                "estado",
                                "intentos",
                                "disponible_en",
                                "iniciada_en",
                                "finalizada_en")
                        || !columnas(
                                columnas,
                                "correo_cita",
                                "id_cita",
                                "estado",
                                "intentos",
                                "disponible_en",
                                "iniciada_en",
                                "finalizada_en")
                        || !columnas(
                                columnas,
                                "correo_oferta",
                                "id_oferta",
                                "estado",
                                "intentos",
                                "disponible_en",
                                "iniciada_en",
                                "finalizada_en")
                        || !repo.avisoVencido()) return false;
            }
            return columnas(
                            columnas,
                            "propuesta_horario",
                            "id_propuesta",
                            "id_medico",
                            "id_sede",
                            "estado")
                    && columnas(columnas, "disponibilidad", "id_propuesta")
                    && columnas(
                            columnas,
                            "invitacion_personal",
                            "id_usuario",
                            "token_hash",
                            "vence_en",
                            "utilizada_en",
                            "revocada_en",
                            "estado_envio")
                    && columnas(
                            columnas,
                            "configuracion_ofertas",
                            "minutos_respuesta",
                            "anticipacion_minima",
                            "version",
                            "confirmacion_horas",
                            "tolerancia_llegada",
                            "recordatorio_horas",
                            "intentos_confirmacion")
                    && columnas(columnas, "atencion_cita", "id_especialidad_prioridad")
                    && columnas(
                            columnas,
                            "retencion_cupo",
                            "id_paciente",
                            "id_medico",
                            "inicio",
                            "duracion_minutos",
                            "identificador",
                            "vence")
                    && repo.configuracion();
        }
    }

    private static boolean habilitado(String variable) {
        return Boolean.parseBoolean(System.getenv().getOrDefault(variable, FALSE));
    }

    private static boolean columnas(
            java.util.Map<String, java.util.Set<String>> esquema, String tabla, String... nombres) {
        return esquema.getOrDefault(tabla, java.util.Set.of())
                .containsAll(java.util.List.of(nombres));
    }

    static boolean esquemaAvisoVencido(java.sql.Connection conexion) throws SQLException {
        return new pe.edu.utp.clinica.repository.EsquemaRepository(conexion).avisoVencido();
    }

    @Override
    public synchronized void close() {
        cerrado = true;
        executor.shutdownNow();
    }
}
