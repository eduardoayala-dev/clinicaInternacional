package pe.edu.utp.clinica.repository;

import jakarta.persistence.LockModeType;

import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.UsuarioEntidad;

import java.sql.Connection;
import java.sql.SQLException;

public final class AutorizacionRepository {
    private final Connection conexion;

    public AutorizacionRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public String rolActivo(int usuario) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(String.class);
                    var u = q.from(UsuarioEntidad.class);
                    q.select(u.get("rol"))
                            .where(cb.equal(u.get("id"), usuario), cb.isTrue(u.get("activo")));
                    var consulta = s.createQuery(q);
                    if (s.getTransaction().isActive())
                        consulta.setLockMode(LockModeType.PESSIMISTIC_WRITE);
                    return consulta.getResultList().stream().findFirst().orElse(null);
                });
    }

    public boolean serializarAdministradores() throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var u = q.from(UsuarioEntidad.class);
                    q.select(u.get("id"))
                            .where(cb.equal(u.get("rol"), "ADMINISTRADOR"))
                            .orderBy(cb.asc(u.get("id")));
                    return !s.createQuery(q)
                            .setMaxResults(1)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .getResultList()
                            .isEmpty();
                });
    }
}
