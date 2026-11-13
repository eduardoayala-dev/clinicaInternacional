package pe.edu.utp.clinica.repository;

import jakarta.persistence.LockModeType;

import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.SesionEntidad;
import pe.edu.utp.clinica.persistence.entity.UsuarioEntidad;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public final class SesionRepository {
    private final Connection conexion;

    public SesionRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public Usuario usuario(int id) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var u = q.from(UsuarioEntidad.class);
                    q.multiselect(
                                    u.get("id"),
                                    u.get("nombres"),
                                    u.get("apellidos"),
                                    u.get("email"),
                                    u.get("passwordHash"),
                                    u.get("rol"),
                                    u.get("activo"))
                            .where(cb.equal(u.get("id"), id));
                    var consulta = sesion.createQuery(q);
                    if (sesion.getTransaction().isActive())
                        consulta.setLockMode(LockModeType.PESSIMISTIC_READ);
                    var filas = consulta.getResultList();
                    if (filas.isEmpty()) return null;
                    var f = filas.get(0);
                    return new Usuario(
                            f.get(0, Integer.class),
                            f.get(1, String.class),
                            f.get(2, String.class),
                            f.get(3, String.class),
                            f.get(4, String.class),
                            f.get(5, String.class),
                            null,
                            f.get(6, Boolean.class));
                });
    }

    public void crear(SesionEntidad acceso) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                sesion -> {
                    sesion.persist(acceso);
                    return null;
                });
    }

    public Integer usuarioDeSesion(String id, byte[] refresh) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var s = q.from(SesionEntidad.class);
                    q.select(s.get("usuario"))
                            .where(
                                    id != null
                                            ? cb.equal(s.get("id"), id)
                                            : cb.equal(s.get("refresh"), refresh));
                    return sesion.createQuery(q).getResultList().stream().findFirst().orElse(null);
                });
    }

    public SesionEntidad bloquear(String id, byte[] refresh) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var q = cb.createQuery(SesionEntidad.class);
                    var s = q.from(SesionEntidad.class);
                    q.select(s)
                            .where(
                                    id != null
                                            ? cb.equal(s.get("id"), id)
                                            : cb.equal(s.get("refresh"), refresh));
                    return sesion
                            .createQuery(q)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .getResultList()
                            .stream()
                            .findFirst()
                            .orElse(null);
                });
    }

    public void revocar(String id, LocalDateTime ahora) throws SQLException {
        var acceso = bloquear(id, null);
        if (acceso != null) acceso.revocar(ahora);
    }
}
