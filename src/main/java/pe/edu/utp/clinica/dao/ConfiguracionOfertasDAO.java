package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class ConfiguracionOfertasDAO {
    public record Parametros(
            int minutosRespuesta,
            int anticipacionMinima,
            long version,
            int confirmacionHoras,
            int toleranciaLlegada,
            int recordatorioHoras,
            int intentosConfirmacion) {
        public Parametros(int minutosRespuesta, int anticipacionMinima, long version) {
            this(minutosRespuesta, anticipacionMinima, version, 24, 10, 24, 2);
        }

        public Parametros(
                int minutosRespuesta,
                int anticipacionMinima,
                long version,
                int confirmacionHoras,
                int toleranciaLlegada) {
            this(
                    minutosRespuesta,
                    anticipacionMinima,
                    version,
                    confirmacionHoras,
                    toleranciaLlegada,
                    24,
                    2);
        }
    }

    public record Cambios(
            int minutosRespuesta,
            int anticipacionMinima,
            int confirmacionHoras,
            int toleranciaLlegada,
            int recordatorioHoras,
            int intentosConfirmacion,
            long version) {}

    private final AgendaCitaDAO.ConnectionSource conexiones;
    private final Clock reloj;

    public ConfiguracionOfertasDAO() {
        this(DatabaseConfig::getConnection, Clock.systemUTC());
    }

    ConfiguracionOfertasDAO(AgendaCitaDAO.ConnectionSource conexiones, Clock reloj) {
        this.conexiones = conexiones;
        this.reloj = reloj;
    }

    public Parametros leer() {
        try (var conexion = conexiones.open()) {
            return leer(conexion);
        } catch (SQLException e) {
            throw noDisponible();
        }
    }

    static Parametros leer(Connection conexion) throws SQLException {
        var parametros =
                new pe.edu.utp.clinica.repository.ConfiguracionOfertasRepository(conexion).leer();
        if (parametros == null) throw noDisponible();
        return parametros;
    }

    public Parametros guardar(int actor, int minutos, int anticipacion, long version) {
        Parametros actual = leer();
        return guardar(
                actor,
                new Cambios(
                        minutos,
                        anticipacion,
                        actual.confirmacionHoras(),
                        actual.toleranciaLlegada(),
                        actual.recordatorioHoras(),
                        actual.intentosConfirmacion(),
                        version));
    }

    public Parametros guardar(
            int actor,
            int minutos,
            int anticipacion,
            int confirmacion,
            int tolerancia,
            long version) {
        Parametros actual = leer();
        return guardar(
                actor,
                new Cambios(
                        minutos,
                        anticipacion,
                        confirmacion,
                        tolerancia,
                        actual.recordatorioHoras(),
                        actual.intentosConfirmacion(),
                        version));
    }

    public Parametros guardar(int actor, Cambios cambios) {
        java.util.Objects.requireNonNull(cambios);
        int minutos = cambios.minutosRespuesta();
        int anticipacion = cambios.anticipacionMinima();
        int confirmacion = cambios.confirmacionHoras();
        int tolerancia = cambios.toleranciaLlegada();
        int recordatorio = cambios.recordatorioHoras();
        int intentos = cambios.intentosConfirmacion();
        long version = cambios.version();
        if (minutos < 5
                || minutos > 60
                || anticipacion < 0
                || anticipacion > 10080
                || version < 0
                || confirmacion < 1
                || confirmacion > 168
                || tolerancia < 0
                || tolerancia > 120
                || recordatorio < 1
                || recordatorio > 168
                || intentos < 1
                || intentos > 10)
            throw new AgendaException(
                    400,
                    "CONFIGURACION_INVALIDA",
                    "Revisa los rangos de oferta, confirmación (1 a 168 horas) y tolerancia (0 a"
                        + " 120 minutos), recordatorio (1 a 168 horas) e intentos (1 a 10).");
        return TransaccionJdbc.ejecutar(
                conexiones,
                conexion -> {
                    exigirAdministrador(conexion, actor);
                    var repo =
                            new pe.edu.utp.clinica.repository.ConfiguracionOfertasRepository(
                                    conexion);
                    Long previa = repo.bloquearVersion();
                    if (previa == null) throw noDisponible();
                    if (previa != version)
                        throw new AgendaException(
                                409,
                                "CONFIGURACION_CAMBIADA",
                                "Otro administrador cambió la configuración. Recarga la página"
                                    + " antes de guardar.");
                    Parametros anterior = leer(conexion);
                    if (repo.guardar(
                                    actor,
                                    cambios,
                                    LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC))
                            != 1) throw noDisponible();
                    new pe.edu.utp.clinica.repository.UsuarioRepository(conexion)
                            .auditar(
                                    actor,
                                    null,
                                    "CONFIGURACION_OFERTAS",
                                    "Plazo "
                                            + anterior.minutosRespuesta()
                                            + "→"
                                            + minutos
                                            + " min; anticipación "
                                            + anterior.anticipacionMinima()
                                            + "→"
                                            + anticipacion
                                            + " min; confirmación "
                                            + anterior.confirmacionHoras()
                                            + "→"
                                            + confirmacion
                                            + " h; tolerancia "
                                            + anterior.toleranciaLlegada()
                                            + "→"
                                            + tolerancia
                                            + " min; recordatorio "
                                            + anterior.recordatorioHoras()
                                            + "→"
                                            + recordatorio
                                            + " h; intentos "
                                            + anterior.intentosConfirmacion()
                                            + "→"
                                            + intentos
                                            + "; versión "
                                            + (version + 1),
                                    false);
                    return new Parametros(
                            minutos,
                            anticipacion,
                            version + 1,
                            confirmacion,
                            tolerancia,
                            recordatorio,
                            intentos);
                });
    }

    private static void exigirAdministrador(Connection conexion, int actor) throws SQLException {
        if (!"ADMINISTRADOR"
                .equals(
                        new pe.edu.utp.clinica.repository.AutorizacionRepository(conexion)
                                .rolActivo(actor)))
            throw new AgendaException(
                    403,
                    "ADMINISTRADOR_REQUERIDO",
                    "La configuración solo puede cambiarla un administrador activo.");
    }

    private static AgendaException noDisponible() {
        return new AgendaException(
                503,
                "CONFIGURACION_NO_DISPONIBLE",
                "No se pudo obtener la configuración de ofertas.");
    }
}
