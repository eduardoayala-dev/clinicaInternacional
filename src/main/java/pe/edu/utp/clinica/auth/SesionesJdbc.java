package pe.edu.utp.clinica.auth;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.SesionEntidad;
import pe.edu.utp.clinica.repository.SesionRepository;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class SesionesJdbc implements AlmacenSesiones {
    @FunctionalInterface
    public interface Conexion {
        Connection abrir() throws SQLException;
    }

    private final Conexion conexion;

    public SesionesJdbc() {
        this(DatabaseConfig::getConnection);
    }

    public SesionesJdbc(Conexion conexion) {
        this.conexion = conexion;
    }

    private static LocalDateTime utc(Instant instante) {
        return LocalDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    private static Instant instante(LocalDateTime fecha) {
        return fecha.toInstant(ZoneOffset.UTC);
    }

    @Override
    public void crear(
            String id, Usuario usuario, byte[] refresh, String csrf, Instant now, Instant vence)
            throws SQLException {
        try (var conn = conexion.abrir()) {
            PersistenciaJpa.transaccion(
                    conn,
                    () -> {
                        var repo = new SesionRepository(conn);
                        var actual = repo.usuario(usuario.getIdUsuario());
                        if (actual == null
                                || !actual.isActivo()
                                || !actual.getRol().equals(usuario.getRol())
                                || !actual.getPasswordHash().equals(usuario.getPasswordHash()))
                            throw new SQLException("La identidad cambió antes de crear el acceso.");
                        repo.crear(
                                new SesionEntidad(
                                        id,
                                        usuario.getIdUsuario(),
                                        refresh,
                                        csrf,
                                        usuario.getRol(),
                                        SecretosSesion.hash(usuario.getPasswordHash()),
                                        utc(now),
                                        utc(vence)));
                        return null;
                    });
        }
    }

    @Override
    public SesionAutenticada resolver(String id, byte[] refresh, Instant now, boolean actividad)
            throws SQLException {
        try (var conn = conexion.abrir()) {
            return PersistenciaJpa.transaccion(
                    conn,
                    () -> {
                        var repo = new SesionRepository(conn);
                        Integer idUsuario = repo.usuarioDeSesion(id, refresh);
                        if (idUsuario == null) return null;
                        var usuario = repo.usuario(idUsuario);
                        var acceso = repo.bloquear(id, refresh);
                        if (acceso == null) return null;
                        boolean valida =
                                usuario != null
                                        && acceso.usuario() == idUsuario
                                        && !acceso.revocada()
                                        && usuario.isActivo()
                                        && now.isBefore(instante(acceso.vence()))
                                        && now.isBefore(
                                                instante(acceso.actividad())
                                                        .plusSeconds(
                                                                PoliticaCredenciales
                                                                        .INACTIVIDAD_SEGUNDOS))
                                        && acceso.rol().equals(usuario.getRol())
                                        && MessageDigest.isEqual(
                                                acceso.credencial(),
                                                SecretosSesion.hash(usuario.getPasswordHash()));
                        if (!valida) {
                            acceso.revocar(utc(now));
                            return null;
                        }
                        if (actividad) acceso.registrarActividad(utc(now));
                        return new SesionAutenticada(
                                acceso.id(), usuario, acceso.csrf(), instante(acceso.vence()));
                    });
        }
    }

    @Override
    public void revocar(String id, Instant now) throws SQLException {
        try (var conn = conexion.abrir()) {
            PersistenciaJpa.transaccion(
                    conn,
                    () -> {
                        new SesionRepository(conn).revocar(id, utc(now));
                        return null;
                    });
        }
    }
}
