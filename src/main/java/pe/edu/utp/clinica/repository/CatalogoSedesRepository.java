package pe.edu.utp.clinica.repository;

import jakarta.persistence.LockModeType;

import pe.edu.utp.clinica.dao.CatalogoSedesDAO.Sede;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public final class CatalogoSedesRepository {
    private final Connection conexion;

    public CatalogoSedesRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public List<Sede> listar(Integer id) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Sede.class);
                    var se = q.from(SedeEntidad.class);
                    q.select(
                                    cb.construct(
                                            Sede.class,
                                            se.get("id"),
                                            se.get("codigo"),
                                            se.get("nombre"),
                                            se.get("direccion"),
                                            se.get("telefono"),
                                            se.get("estado")))
                            .orderBy(cb.asc(se.get("nombre")), cb.asc(se.get("id")));
                    if (id != null) q.where(cb.equal(se.get("id"), id));
                    var consulta = s.createQuery(q);
                    if (id != null) consulta.setLockMode(LockModeType.PESSIMISTIC_WRITE);
                    return consulta.getResultList();
                });
    }

    public Integer porCodigo(String codigo, boolean bloquear) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var se = q.from(SedeEntidad.class);
                    q.select(se.get("id")).where(cb.equal(se.get("codigo"), codigo));
                    var consulta = s.createQuery(q);
                    if (bloquear) consulta.setLockMode(LockModeType.PESSIMISTIC_WRITE);
                    return consulta.getResultList().stream().findFirst().orElse(null);
                });
    }

    public int guardar(Sede sede, int id) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    if (id == 0) {
                        var entidad =
                                new SedeEntidad(
                                        sede.codigo(),
                                        sede.nombre(),
                                        sede.direccion(),
                                        sede.telefono(),
                                        sede.estado());
                        s.persist(entidad);
                        return entidad.id();
                    }
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(SedeEntidad.class);
                    var se = q.from(SedeEntidad.class);
                    q.set("codigo", sede.codigo())
                            .set("nombre", sede.nombre())
                            .set("direccion", sede.direccion())
                            .set("telefono", sede.telefono())
                            .set("estado", sede.estado())
                            .where(cb.equal(se.get("id"), id));
                    s.createMutationQuery(q).executeUpdate();
                    return id;
                });
    }

    public void renombrarPreferencias(String anterior, String nuevo) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(PacienteEntidad.class);
                    var p = q.from(PacienteEntidad.class);
                    q.set("sedePreferida", nuevo).where(cb.equal(p.get("sedePreferida"), anterior));
                    s.createMutationQuery(q).executeUpdate();
                    return null;
                });
    }

    public boolean agendaActiva(int sede, LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var se = q.from(SedeEntidad.class);
                    var turnos = q.subquery(Integer.class);
                    var d = turnos.from(DisponibilidadEntidad.class);
                    turnos.select(cb.literal(1))
                            .where(
                                    cb.equal(d.get("sede"), se.get("id")),
                                    cb.equal(d.get("estado"), "PUBLICADO"),
                                    cb.greaterThanOrEqualTo(d.get("fecha"), ahora.toLocalDate()));
                    var citas = q.subquery(Integer.class);
                    var c = citas.from(CitaEntidad.class);
                    citas.select(cb.literal(1))
                            .where(
                                    cb.equal(c.get("medico").get("sede").get("id"), se.get("id")),
                                    cb.or(
                                            c.get("estado").in("EN_SALA", "EN_ATENCION"),
                                            cb.and(
                                                    cb.greaterThan(c.get("inicio"), ahora),
                                                    c.get("estado")
                                                            .in("SOLICITADA", "CONFIRMADA"))));
                    q.select(se.get("id"))
                            .where(
                                    cb.equal(se.get("id"), sede),
                                    cb.or(cb.exists(turnos), cb.exists(citas)));
                    return !s.createQuery(q).getResultList().isEmpty();
                });
    }
}
