package pe.edu.utp.clinica.persistence;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class PersistenciaJpa {
    private static final Map<String, SessionFactory> FABRICAS = new HashMap<>();
    private static final ThreadLocal<Contexto> ACTUAL = new ThreadLocal<>();

    private record Contexto(Connection conexion, Session sesion) {}

    @FunctionalInterface
    public interface Operacion<T> {
        T ejecutar() throws SQLException;
    }

    private PersistenciaJpa() {}

    public static <T> T consultar(Connection conexion, Function<Session, T> lectura)
            throws SQLException {
        var actual = ACTUAL.get();
        if (actual != null && actual.conexion() == conexion)
            return aplicar(actual.sesion(), lectura);
        try (var sesion =
                fabrica(conexion)
                        .withOptions()
                        .connection(conexion)
                        .flushMode(FlushMode.MANUAL)
                        .openSession()) {
            sesion.setDefaultReadOnly(true);
            return lectura.apply(sesion);
        } catch (HibernateException error) {
            throw falloPersistencia(error);
        }
    }

    public static <T> T escribir(Connection conexion, Function<Session, T> operacion)
            throws SQLException {
        var actual = ACTUAL.get();
        if (actual == null
                || actual.conexion() != conexion
                || !actual.sesion().getTransaction().isActive()) {
            throw new SQLException("Se requiere una transacción JPA activa para guardar.");
        }
        return aplicar(actual.sesion(), operacion);
    }

    private static <T> T aplicar(Session sesion, Function<Session, T> operacion)
            throws SQLException {
        try {
            return operacion.apply(sesion);
        } catch (jakarta.persistence.PersistenceException error) {
            throw falloPersistencia(error);
        }
    }

    public static <T> T transaccion(Connection conexion, Operacion<T> operacion)
            throws SQLException {
        var anterior = ACTUAL.get();
        try (var sesion = fabrica(conexion).withOptions().connection(conexion).openSession()) {
            var transaccion = sesion.beginTransaction();
            ACTUAL.set(new Contexto(conexion, sesion));
            try {
                T resultado = operacion.ejecutar();
                transaccion.commit();
                return resultado;
            } catch (SQLException | RuntimeException error) {
                if (transaccion.isActive()) {
                    try {
                        transaccion.rollback();
                    } catch (RuntimeException falloRollback) {
                        error.addSuppressed(falloRollback);
                    }
                }
                throw error;
            }
        } catch (jakarta.persistence.PersistenceException error) {
            throw falloPersistencia(error);
        } finally {
            if (anterior == null) ACTUAL.remove();
            else ACTUAL.set(anterior);
        }
    }

    private static SQLException falloPersistencia(jakarta.persistence.PersistenceException error) {
        if (error instanceof org.hibernate.JDBCException jdbc) {
            var sql = jdbc.getSQLException();
            return new SQLException(
                    "No fue posible completar la persistencia.",
                    sql.getSQLState(),
                    sql.getErrorCode(),
                    error);
        }
        return new SQLException("No fue posible completar la persistencia.", error);
    }

    private static synchronized SessionFactory fabrica(Connection conexion) throws SQLException {
        String motor = conexion.getMetaData().getDatabaseProductName();
        String dialecto =
                switch (motor) {
                    case "MySQL" -> "org.hibernate.dialect.MySQLDialect";
                    case "H2" -> "org.hibernate.dialect.H2Dialect";
                    default -> throw new SQLException("Motor de base de datos no admitido.");
                };
        return FABRICAS.computeIfAbsent(motor, ignorado -> crear(dialecto));
    }

    private static SessionFactory crear(String dialecto) {
        var configuracion = new Configuration();
        configuracion.setProperty("hibernate.dialect", dialecto);
        configuracion.setProperty("hibernate.boot.allow_jdbc_metadata_access", "false");
        configuracion.setProperty("hibernate.hbm2ddl.auto", "none");
        configuracion.setProperty("jakarta.persistence.schema-generation.database.action", "none");
        configuracion.setProperty("hibernate.show_sql", "false");
        configuracion.setProperty("hibernate.cache.use_second_level_cache", "false");
        configuracion.setProperty("hibernate.criteria.value_handling_mode", "bind");
        configuracion.setProperty("hibernate.type.java_time_use_direct_jdbc", "true");
        for (Class<?> entidad :
                new Class<?>[] {
                    RestriccionEsquemaEntidad.class,
                    LimiteRecuperacionEntidad.class,
                    VerificacionEntidad.class,
                    PropuestaHorarioEntidad.class,
                    DisponibilidadPropuestaEntidad.class,
                    CorreoOfertaEntidad.class,
                    CorreoRecordatorioEntidad.class,
                    InasistenciaEntidad.class,
                    AdmisionEntidad.class,
                    ConsultaClinicaEntidad.class,
                    HistorialAsistenciaEntidad.class,
                    ContactoAdmisionEntidad.class,
                    ConfiguracionOfertasEntidad.class,
                    PersonalEntidad.class,
                    AdministradorEntidad.class,
                    InvitacionPerfilEntidad.class,
                    SesionEntidad.class,
                    UsuarioEntidad.class,
                    SedeEntidad.class,
                    EspecialidadEntidad.class,
                    MedicoEntidad.class,
                    DisponibilidadEntidad.class,
                    CitaEntidad.class,
                    PacienteEntidad.class,
                    ListaEsperaEntidad.class,
                    OfertaEntidad.class,
                    RetencionEntidad.class,
                    MotivoCitaEntidad.class,
                    ConfiguracionCitaEntidad.class,
                    PacienteBloqueoEntidad.class,
                    MedicoBloqueoEntidad.class,
                    CitaBloqueoEntidad.class,
                    ConfirmacionEntidad.class,
                    NotificacionEntidad.class,
                    AuditoriaEntidad.class,
                    CorreoCitaEntidad.class,
                    CorreoCambioEntidad.class,
                    AtencionPrioridadEntidad.class,
                    SeguimientoEntidad.class
                }) {
            configuracion.addAnnotatedClass(entidad);
        }
        return configuracion.buildSessionFactory();
    }

    public static synchronized void cerrar() {
        FABRICAS.values().forEach(SessionFactory::close);
        FABRICAS.clear();
    }
}
