package pe.edu.utp.clinica.repository;

import jakarta.persistence.LockModeType;

import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.LimiteRecuperacionEntidad;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;

public final class LimiteVerificacionRepository {
    public static final class CreacionConcurrente extends SQLException {
        public CreacionConcurrente(SQLException causa) {
            super("Se debe repetir la creación concurrente del límite.", causa);
        }
    }

    private final Connection conexion;

    public LimiteVerificacionRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public boolean permitir(byte[] hash, int maximo, int segundos, Instant ahora)
            throws SQLException {
        try {
            return PersistenciaJpa.escribir(
                    conexion,
                    s -> {
                        var limite =
                                s.find(
                                        LimiteRecuperacionEntidad.class,
                                        hash,
                                        LockModeType.PESSIMISTIC_WRITE);
                        if (limite == null) {
                            limite = new LimiteRecuperacionEntidad(hash, ahora);
                            s.persist(limite);
                            s.flush();
                        }
                        return limite.permitir(maximo, segundos, ahora);
                    });
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())
                    || ("23000".equals(e.getSQLState()) && e.getErrorCode() == 1062)
                    || "40001".equals(e.getSQLState())) throw new CreacionConcurrente(e);
            throw e;
        }
    }

    public void limpiar(Instant antes) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaDelete(LimiteRecuperacionEntidad.class);
                    var l = q.from(LimiteRecuperacionEntidad.class);
                    q.where(
                            cb.lessThan(
                                    l.get("vence"),
                                    LocalDateTime.ofInstant(antes, ZoneOffset.UTC)));
                    s.createMutationQuery(q).executeUpdate();
                    return null;
                });
    }
}
