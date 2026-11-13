package pe.edu.utp.clinica.recuperacion;

import pe.edu.utp.clinica.auth.*;
import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.correo.Correo;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.VerificacionEntidad;
import pe.edu.utp.clinica.repository.VerificacionRepository;
import pe.edu.utp.clinica.repository.VerificacionRepository.Cuenta;
import pe.edu.utp.clinica.util.SecurityUtil;

import java.security.MessageDigest;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public final class VerificacionJdbc {
    public enum Proposito {
        RECUPERAR,
        CAMBIAR_PASSWORD,
        CAMBIAR_CORREO
    }

    public enum Etapa {
        CODIGO_ACTUAL,
        AUTORIZADO,
        CODIGO_NUEVO,
        FINALIZADO,
        BLOQUEADO
    }

    public record Entrega(String correo, String codigo) {
        @Override
        public String toString() {
            return "Entrega[privada]";
        }
    }

    public record CambioCorreo(boolean cambiado, String anterior) {
        @Override
        public String toString() {
            return "CambioCorreo[privado]";
        }
    }

    public record InicioCambio(
            int usuario,
            String sesion,
            String vinculo,
            Proposito proposito,
            String password,
            String ip) {
        @Override
        public String toString() {
            return "InicioCambio[privado]";
        }
    }

    private final SesionesJdbc.Conexion conexiones;
    private final CodigosVerificacion codigos;

    public VerificacionJdbc(CodigosVerificacion codigos) {
        this(DatabaseConfig::getConnection, codigos);
    }

    public VerificacionJdbc(SesionesJdbc.Conexion conexiones, CodigosVerificacion codigos) {
        this.conexiones = conexiones;
        this.codigos = codigos;
    }

    public Optional<Entrega> iniciarRecuperacion(
            String id,
            String identificador,
            boolean personal,
            String vinculo,
            String ip,
            Instant ahora)
            throws SQLException {
        return transaccion(
                conn -> {
                    if (!LimitesVerificacion.solicitud(
                            conn,
                            ip,
                            (personal ? "personal:" : "paciente:") + identificador,
                            ahora)) return Optional.empty();
                    var cuenta =
                            new VerificacionRepository(conn).buscar(identificador, personal, null);
                    return cuenta == null
                            ? Optional.empty()
                            : Optional.of(
                                    crear(
                                            conn,
                                            id,
                                            cuenta,
                                            Proposito.RECUPERAR,
                                            null,
                                            vinculo,
                                            ahora));
                });
    }

    public Optional<Entrega> iniciarCambio(String id, InicioCambio cambio, Instant ahora)
            throws SQLException {
        int usuario = cambio.usuario();
        String sesion = cambio.sesion();
        String vinculo = cambio.vinculo();
        Proposito proposito = cambio.proposito();
        String password = cambio.password();
        String ip = cambio.ip();
        if (proposito == Proposito.RECUPERAR)
            throw new IllegalArgumentException("Operación autenticada inválida.");
        return transaccion(
                conn -> {
                    if (!LimitesVerificacion.permitir(conn, "reauth:" + usuario, 5, 900, ahora)
                            || !LimitesVerificacion.solicitud(
                                    conn, ip, "usuario:" + usuario, ahora)) return Optional.empty();
                    Cuenta cuenta = cuenta(conn, usuario);
                    if (cuenta == null
                            || !sesionVigente(conn, cuenta, sesion, ahora)
                            || !SecurityUtil.checkPassword(password, cuenta.password()))
                        return Optional.empty();
                    return Optional.of(crear(conn, id, cuenta, proposito, sesion, vinculo, ahora));
                });
    }

    private Entrega crear(
            Connection conn,
            String id,
            Cuenta cuenta,
            Proposito proposito,
            String sesion,
            String vinculo,
            Instant ahora)
            throws SQLException {
        UUID.fromString(id);
        var repo = new VerificacionRepository(conn);
        repo.invalidar(cuenta.id(), proposito);
        String codigo = codigos.nuevo();
        repo.crear(
                new VerificacionEntidad(
                        id,
                        cuenta.id(),
                        proposito,
                        sesion,
                        SecretosSesion.hash(vinculo),
                        SecretosSesion.hash(cuenta.password()),
                        SecretosSesion.hash(cuenta.correo()),
                        codigos.hash(id, proposito.name(), Etapa.CODIGO_ACTUAL.name(), codigo),
                        ahora));
        return new Entrega(cuenta.correo(), codigo);
    }

    public boolean verificar(
            String id,
            String vinculo,
            Proposito proposito,
            String sesion,
            String codigo,
            Instant ahora)
            throws SQLException {
        return transaccion(
                conn -> {
                    Cuenta cuenta = cuentaDelReto(conn, id);
                    VerificacionEntidad reto = reto(conn, id);
                    if (!vigente(conn, reto, cuenta, vinculo, proposito, sesion, ahora)
                            || reto.etapa() != Etapa.CODIGO_ACTUAL) return false;
                    if (!codigoCorrecto(conn, reto, codigo)) return false;
                    reto.autorizar(minimo(ahora.plusSeconds(300), reto.creada().plusSeconds(1200)));
                    return true;
                });
    }

    public boolean autorizada(
            String id, String vinculo, Proposito proposito, String sesion, Instant ahora)
            throws SQLException {
        return transaccion(
                conn -> {
                    Cuenta cuenta = cuentaDelReto(conn, id);
                    VerificacionEntidad reto = reto(conn, id);
                    return vigente(conn, reto, cuenta, vinculo, proposito, sesion, ahora)
                            && reto.etapa() == Etapa.AUTORIZADO;
                });
    }

    public boolean cambiarPassword(
            String id,
            String vinculo,
            Proposito proposito,
            String sesion,
            String password,
            Instant ahora)
            throws SQLException {
        if (proposito == Proposito.CAMBIAR_CORREO || !passwordValida(password))
            throw new IllegalArgumentException("Contraseña inválida.");
        return transaccion(
                conn -> {
                    Cuenta cuenta = cuentaDelReto(conn, id);
                    VerificacionEntidad reto = reto(conn, id);
                    if (!vigente(conn, reto, cuenta, vinculo, proposito, sesion, ahora)
                            || reto.etapa() != Etapa.AUTORIZADO
                            || contieneDni(conn, cuenta.id(), password)) return false;
                    String hash = SecurityUtil.hashPassword(password);
                    new VerificacionRepository(conn).cambiar(cuenta.id(), hash, false);
                    sesionesTrasCambio(conn, cuenta.id(), sesion, hash, ahora);
                    auditar(conn, cuenta.id(), proposito.name());
                    finalizar(conn, id);
                    return true;
                });
    }

    public Optional<Entrega> prepararCorreo(
            String id, String vinculo, String sesion, String nuevo, Instant ahora)
            throws SQLException {
        String correo = Correo.validarDireccion(nuevo.trim().toLowerCase(Locale.ROOT));
        if (correo.length() > 150)
            throw new IllegalArgumentException(
                    "El correo supera el tamaño admitido por la cuenta.");
        return transaccion(
                conn -> {
                    if (!LimitesVerificacion.permitir(conn, "nuevo-correo:" + id, 3, 3600, ahora))
                        return Optional.empty();
                    Cuenta cuenta = cuentaDelReto(conn, id);
                    VerificacionEntidad reto = reto(conn, id);
                    if (!vigente(
                                    conn,
                                    reto,
                                    cuenta,
                                    vinculo,
                                    Proposito.CAMBIAR_CORREO,
                                    sesion,
                                    ahora)
                            || reto.etapa() != Etapa.AUTORIZADO
                            || cuenta.correo().equalsIgnoreCase(correo)
                            || correoOcupado(conn, correo)) return Optional.empty();
                    String codigo = codigos.nuevo();
                    reto.prepararCorreo(
                            correo,
                            codigos.hash(
                                    id,
                                    Proposito.CAMBIAR_CORREO.name(),
                                    Etapa.CODIGO_NUEVO.name(),
                                    codigo),
                            minimo(ahora.plusSeconds(600), reto.creada().plusSeconds(1200)));
                    return Optional.of(new Entrega(correo, codigo));
                });
    }

    public CambioCorreo confirmarCorreo(
            String id, String vinculo, String sesion, String codigo, Instant ahora)
            throws SQLException {
        return transaccion(
                conn -> {
                    Cuenta cuenta = cuentaDelReto(conn, id);
                    VerificacionEntidad reto = reto(conn, id);
                    if (!vigente(
                                    conn,
                                    reto,
                                    cuenta,
                                    vinculo,
                                    Proposito.CAMBIAR_CORREO,
                                    sesion,
                                    ahora)
                            || reto.etapa() != Etapa.CODIGO_NUEVO
                            || !codigoCorrecto(conn, reto, codigo)
                            || correoOcupado(conn, reto.nuevo()))
                        return new CambioCorreo(false, null);
                    new VerificacionRepository(conn).cambiar(cuenta.id(), reto.nuevo(), true);
                    sesionesTrasCambio(conn, cuenta.id(), sesion, cuenta.password(), ahora);
                    auditar(conn, cuenta.id(), Proposito.CAMBIAR_CORREO.name());
                    finalizar(conn, id);
                    return new CambioCorreo(true, cuenta.correo());
                });
    }

    private boolean codigoCorrecto(Connection conn, VerificacionEntidad reto, String codigo)
            throws SQLException {
        boolean formato = codigo != null && codigo.matches("\\d{6}");
        boolean coincide =
                formato
                        && MessageDigest.isEqual(
                                reto.codigo(),
                                codigos.hash(
                                        reto.id(),
                                        reto.proposito().name(),
                                        reto.etapa().name(),
                                        codigo));
        reto.registrarIntento(coincide);
        return coincide;
    }

    private boolean vigente(
            Connection conn,
            VerificacionEntidad reto,
            Cuenta cuenta,
            String vinculo,
            Proposito proposito,
            String sesion,
            Instant ahora)
            throws SQLException {
        return reto != null
                && cuenta != null
                && reto.proposito() == proposito
                && Objects.equals(reto.sesion(), sesion)
                && MessageDigest.isEqual(reto.vinculo(), SecretosSesion.hash(vinculo))
                && ahora.isBefore(reto.vence())
                && reto.etapa() != Etapa.BLOQUEADO
                && reto.etapa() != Etapa.FINALIZADO
                && reto.intentos() < 5
                && MessageDigest.isEqual(reto.credencial(), SecretosSesion.hash(cuenta.password()))
                && MessageDigest.isEqual(reto.correo(), SecretosSesion.hash(cuenta.correo()))
                && (sesion == null || sesionVigente(conn, cuenta, sesion, ahora));
    }

    private Cuenta cuentaDelReto(Connection conn, String id) throws SQLException {
        Integer usuario = new VerificacionRepository(conn).usuarioReto(id);
        return usuario == null ? null : cuenta(conn, usuario);
    }

    private Cuenta cuenta(Connection conn, int usuario) throws SQLException {
        return new VerificacionRepository(conn).buscar(null, false, usuario);
    }

    private VerificacionEntidad reto(Connection conn, String id) throws SQLException {
        return new VerificacionRepository(conn).bloquear(id);
    }

    private boolean sesionVigente(Connection conn, Cuenta cuenta, String sesion, Instant ahora)
            throws SQLException {
        var actual =
                new pe.edu.utp.clinica.repository.SesionRepository(conn).bloquear(sesion, null);
        return actual != null
                && actual.usuario() == cuenta.id()
                && !actual.revocada()
                && ahora.isBefore(actual.vence().toInstant(java.time.ZoneOffset.UTC))
                && ahora.minusSeconds(PoliticaCredenciales.INACTIVIDAD_SEGUNDOS)
                        .isBefore(actual.actividad().toInstant(java.time.ZoneOffset.UTC))
                && actual.rol().equals(cuenta.rol())
                && MessageDigest.isEqual(
                        actual.credencial(), SecretosSesion.hash(cuenta.password()));
    }

    private boolean correoOcupado(Connection conn, String correo) throws SQLException {
        return new VerificacionRepository(conn).correoOcupado(correo);
    }

    private void sesionesTrasCambio(
            Connection conn, int usuario, String conservar, String hash, Instant ahora)
            throws SQLException {
        new VerificacionRepository(conn).sesionesTrasCambio(usuario, conservar, hash, ahora);
    }

    private void finalizar(Connection conn, String id) throws SQLException {
        new VerificacionRepository(conn).bloquear(id).finalizar();
    }

    private void auditar(Connection conn, int usuario, String evento) throws SQLException {
        new pe.edu.utp.clinica.repository.UsuarioRepository(conn)
                .auditar(
                        usuario,
                        null,
                        evento,
                        "Cambio de seguridad confirmado mediante código de correo.",
                        false);
    }

    private boolean contieneDni(Connection conn, int usuario, String password) throws SQLException {
        return new VerificacionRepository(conn).contieneDni(usuario, password);
    }

    private static Instant minimo(Instant uno, Instant dos) {
        return uno.isBefore(dos) ? uno : dos;
    }

    public static boolean passwordValida(String password) {
        return pe.edu.utp.clinica.auth.PoliticaCredenciales.valida(password);
    }

    @FunctionalInterface
    private interface Operacion<T> {
        T ejecutar(Connection conn) throws SQLException;
    }

    private <T> T transaccion(Operacion<T> operacion) throws SQLException {
        for (int intento = 0; intento < 3; intento++) {
            try (var conn = conexiones.abrir()) {
                conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                return PersistenciaJpa.transaccion(conn, () -> operacion.ejecutar(conn));
            } catch (
                    pe.edu.utp.clinica.repository.LimiteVerificacionRepository.CreacionConcurrente
                            e) {
                if (intento == 2) throw e;
            }
        }
        throw new SQLException("No se pudo completar la verificación.");
    }
}
