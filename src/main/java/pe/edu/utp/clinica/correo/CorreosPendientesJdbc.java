package pe.edu.utp.clinica.correo;

import pe.edu.utp.clinica.auth.SesionesJdbc;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.repository.ColaCorreoRepository;

import java.sql.*;
import java.time.*;
import java.util.Optional;

public abstract class CorreosPendientesJdbc {
    public record Pendiente(int id, int intentos) {}

    public enum Resultado {
        ENVIADO,
        NO_CONFIRMADO,
        OMITIDO
    }

    protected static final ColaCorreoRepository.Tipo CITAS = ColaCorreoRepository.Tipo.CITAS;
    protected static final ColaCorreoRepository.Tipo CAMBIOS = ColaCorreoRepository.Tipo.CAMBIOS;
    protected static final ColaCorreoRepository.Tipo RECORDATORIOS =
            ColaCorreoRepository.Tipo.RECORDATORIOS;
    protected static final ColaCorreoRepository.Tipo OFERTAS = ColaCorreoRepository.Tipo.OFERTAS;
    protected final SesionesJdbc.Conexion conexiones;
    private final ColaCorreoRepository.Tipo tipo;

    protected CorreosPendientesJdbc(
            SesionesJdbc.Conexion conexiones, ColaCorreoRepository.Tipo tipo) {
        this.conexiones = conexiones;
        this.tipo = tipo;
    }

    public abstract Optional<Correo> mensaje(int id, Instant ahora) throws SQLException;

    public Optional<Pendiente> tomar(Instant ahora) throws SQLException {
        try (var conn = conexiones.abrir()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            return PersistenciaJpa.transaccion(
                    conn,
                    () -> {
                        var repo = ColaCorreoRepository.para(conn, tipo);
                        repo.recuperar(utc(ahora));
                        return repo.tomar(utc(ahora));
                    });
        }
    }

    public Optional<Instant> proxima() throws SQLException {
        try (var conn = conexiones.abrir()) {
            var repo = ColaCorreoRepository.para(conn, tipo);
            var fechaPendiente = repo.minima(false);
            var fechaEnviando = repo.minima(true);
            Instant pendiente =
                    fechaPendiente == null ? null : fechaPendiente.toInstant(ZoneOffset.UTC);
            Instant enviando =
                    fechaEnviando == null
                            ? null
                            : fechaEnviando.toInstant(ZoneOffset.UTC).plusSeconds(901);
            if (pendiente == null) return Optional.ofNullable(enviando);
            return Optional.of(
                    enviando == null || pendiente.isBefore(enviando) ? pendiente : enviando);
        }
    }

    public void terminar(int id, Resultado resultado, Instant ahora) throws SQLException {
        try (var conn = conexiones.abrir()) {
            PersistenciaJpa.transaccion(
                    conn,
                    () -> {
                        ColaCorreoRepository.para(conn, tipo)
                                .terminar(id, resultado.name(), utc(ahora));
                        return null;
                    });
        }
    }

    public void reintentar(Pendiente pendiente, Instant ahora) throws SQLException {
        if (pendiente.intentos() >= 3) {
            terminar(pendiente.id(), Resultado.NO_CONFIRMADO, ahora);
            return;
        }
        try (var conn = conexiones.abrir()) {
            PersistenciaJpa.transaccion(
                    conn,
                    () -> {
                        ColaCorreoRepository.para(conn, tipo)
                                .reintentar(
                                        pendiente.id(),
                                        utc(
                                                ahora.plusSeconds(
                                                        pendiente.intentos() == 1 ? 60 : 300)));
                        return null;
                    });
        }
    }

    protected static LocalDateTime utc(Instant instante) {
        return LocalDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}
