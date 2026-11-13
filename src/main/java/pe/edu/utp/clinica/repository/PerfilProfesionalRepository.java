package pe.edu.utp.clinica.repository;

import jakarta.persistence.LockModeType;

import pe.edu.utp.clinica.dao.PerfilProfesionalMedicoDAO.Cuenta;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class PerfilProfesionalRepository {
    private final Connection conexion;

    public PerfilProfesionalRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public List<Cuenta> pendientes(int sede) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Cuenta.class);
                    var u = q.from(UsuarioEntidad.class);
                    var i = u.join("invitaciones");
                    var existentes = q.subquery(Integer.class);
                    var m = existentes.from(MedicoEntidad.class);
                    existentes
                            .select(cb.literal(1))
                            .where(cb.equal(m.get("usuario").get("id"), u.get("id")));
                    q.select(
                                    cb.construct(
                                            Cuenta.class,
                                            u.get("id"),
                                            cb.concat(
                                                    cb.concat(u.get("nombres"), " "),
                                                    u.get("apellidos")),
                                            u.get("email"),
                                            u.get("activo")))
                            .where(
                                    cb.equal(u.get("rol"), "MEDICO"),
                                    cb.equal(i.get("sede").get("id"), sede),
                                    cb.not(cb.exists(existentes)))
                            .orderBy(
                                    cb.asc(u.get("apellidos")),
                                    cb.asc(u.get("nombres")),
                                    cb.asc(u.get("id")));
                    return s.createQuery(q).setMaxResults(200).getResultList();
                });
    }

    public boolean invitacionEnSede(int cuenta, int sede) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var i = q.from(InvitacionPerfilEntidad.class);
                    q.select(i.get("id"))
                            .where(
                                    cb.equal(i.get("id"), cuenta),
                                    cb.equal(i.get("sede").get("id"), sede));
                    return !s.createQuery(q)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .getResultList()
                            .isEmpty();
                });
    }

    public boolean cuentaMedica(int cuenta) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var u = q.from(UsuarioEntidad.class);
                    q.select(u.get("id"))
                            .where(cb.equal(u.get("id"), cuenta), cb.equal(u.get("rol"), "MEDICO"));
                    return !s.createQuery(q)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .getResultList()
                            .isEmpty();
                });
    }

    public boolean existePerfil(int cuenta, String cmp) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var m = q.from(MedicoEntidad.class);
                    q.select(m.get("id"))
                            .where(
                                    cb.or(
                                            cb.equal(m.get("usuario").get("id"), cuenta),
                                            cb.equal(m.get("cmp"), cmp)));
                    return !s.createQuery(q)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .getResultList()
                            .isEmpty();
                });
    }

    public boolean especialidadActiva(int especialidad) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var e = q.from(EspecialidadEntidad.class);
                    q.select(e.get("id"))
                            .where(
                                    cb.equal(e.get("id"), especialidad),
                                    cb.equal(e.get("estado"), "ACTIVO"));
                    return !s.createQuery(q)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .getResultList()
                            .isEmpty();
                });
    }

    public void crear(int cuenta, String cmp, int especialidad, int sede) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    s.persist(
                            new MedicoEntidad(
                                    s.getReference(UsuarioEntidad.class, cuenta),
                                    cmp,
                                    s.getReference(EspecialidadEntidad.class, especialidad),
                                    s.getReference(SedeEntidad.class, sede)));
                    return null;
                });
    }
}
