package pe.edu.utp.clinica.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.*;

import pe.edu.utp.clinica.dao.GestionMotivosDAO.*;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class GestionMotivosRepository {
    private final Connection conexion;

    public GestionMotivosRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public List<Especialidad> especialidades() throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Especialidad.class);
                    var e = q.from(EspecialidadEntidad.class);
                    q.select(cb.construct(Especialidad.class, e.get("id"), e.get("nombre")))
                            .where(cb.equal(e.get("estado"), "ACTIVO"))
                            .orderBy(cb.asc(e.get("nombre")), cb.asc(e.get("id")));
                    return s.createQuery(q).getResultList();
                });
    }

    private static Predicate[] filtros(
            CriteriaBuilder cb, Root<MotivoCitaEntidad> m, Filtro filtro) {
        var condiciones = new java.util.ArrayList<Predicate>();
        if (filtro.especialidad() > 0)
            condiciones.add(cb.equal(m.get("especialidad").get("id"), filtro.especialidad()));
        if (!filtro.estado().isEmpty()) condiciones.add(cb.equal(m.get("estado"), filtro.estado()));
        var patron =
                "%"
                        + filtro.busqueda()
                                .replace("!", "!!")
                                .replace("%", "!%")
                                .replace("_", "!_")
                                .toLowerCase(java.util.Locale.ROOT)
                        + "%";
        condiciones.add(cb.like(cb.lower(m.get("descripcion")), patron, '!'));
        return condiciones.toArray(Predicate[]::new);
    }

    public long contar(Filtro filtro) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Long.class);
                    var m = q.from(MotivoCitaEntidad.class);
                    m.join("especialidad");
                    q.select(cb.count(m)).where(filtros(cb, m, filtro));
                    return s.createQuery(q).getSingleResult();
                });
    }

    public List<Motivo> listar(Filtro filtro, Integer id, boolean bloquear) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Motivo.class);
                    var m = q.from(MotivoCitaEntidad.class);
                    var e = m.join("especialidad");
                    var citas = q.subquery(Integer.class);
                    var c = citas.from(CitaEntidad.class);
                    citas.select(cb.literal(1)).where(cb.equal(c.get("motivo"), m.get("id")));
                    q.select(
                            cb.construct(
                                    Motivo.class,
                                    m.get("id"),
                                    e.get("id"),
                                    e.get("nombre"),
                                    m.get("descripcion"),
                                    m.get("estado"),
                                    cb.exists(citas)));
                    if (id != null) q.where(cb.equal(m.get("id"), id));
                    else q.where(filtros(cb, m, filtro));
                    q.orderBy(
                            cb.asc(e.get("nombre")),
                            cb.asc(m.get("descripcion")),
                            cb.asc(m.get("id")));
                    var consulta = s.createQuery(q);
                    if (bloquear) consulta.setLockMode(LockModeType.PESSIMISTIC_WRITE);
                    if (filtro != null)
                        consulta.setMaxResults(20).setFirstResult((filtro.pagina() - 1) * 20);
                    return consulta.getResultList();
                });
    }

    public Integer especialidadMotivo(int id) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var m = q.from(MotivoCitaEntidad.class);
                    q.select(m.get("especialidad").get("id")).where(cb.equal(m.get("id"), id));
                    return s.createQuery(q).getResultList().stream().findFirst().orElse(null);
                });
    }

    public String bloquearEspecialidad(int id) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(String.class);
                    var e = q.from(EspecialidadEntidad.class);
                    q.select(e.get("estado")).where(cb.equal(e.get("id"), id));
                    return s
                            .createQuery(q)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .getResultList()
                            .stream()
                            .findFirst()
                            .orElse(null);
                });
    }

    public boolean duplicado(int id, int especialidad, String descripcion) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var m = q.from(MotivoCitaEntidad.class);
                    q.select(m.get("id"))
                            .where(
                                    cb.equal(m.get("especialidad").get("id"), especialidad),
                                    cb.equal(
                                            cb.lower(m.get("descripcion")),
                                            cb.lower(cb.literal(descripcion))),
                                    cb.notEqual(m.get("id"), id));
                    return !s.createQuery(q).setMaxResults(1).getResultList().isEmpty();
                });
    }

    public boolean repetido(int especialidad, List<String> anteriores, String descripcion)
            throws SQLException {
        if (anteriores.isEmpty()) return false;
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Boolean.class);
                    var e = q.from(EspecialidadEntidad.class);
                    var comparacion = cb.in(cb.lower(cb.literal(descripcion)));
                    for (var anterior : anteriores)
                        comparacion.value(cb.lower(cb.literal(anterior)));
                    q.select(comparacion).where(cb.equal(e.get("id"), especialidad));
                    return s.createQuery(q).getSingleResult();
                });
    }

    public int crear(int especialidad, String descripcion) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var m =
                            new MotivoCitaEntidad(
                                    s.getReference(EspecialidadEntidad.class, especialidad),
                                    descripcion);
                    s.persist(m);
                    return m.id();
                });
    }

    public void actualizar(int id, String valor, boolean estado) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(MotivoCitaEntidad.class);
                    var m = q.from(MotivoCitaEntidad.class);
                    q.set(estado ? "estado" : "descripcion", valor)
                            .where(cb.equal(m.get("id"), id));
                    s.createMutationQuery(q).executeUpdate();
                    return null;
                });
    }
}
