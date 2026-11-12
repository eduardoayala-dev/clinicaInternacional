package pe.edu.utp.clinica.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.*;

import pe.edu.utp.clinica.dao.PadronMedicosDAO.*;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public final class PadronMedicosRepository {
    private final Connection conexion;

    public PadronMedicosRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public List<Medico> listar(int sede, Filtro filtro, Integer id) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Medico.class);
                    var m = q.from(MedicoEntidad.class);
                    var u = m.join("usuario");
                    var e = m.join("especialidad");
                    var se = m.join("sede");
                    var nombre = cb.concat(cb.concat(u.get("nombres"), " "), u.get("apellidos"));
                    var estado =
                            cb.<String>selectCase()
                                    .when(cb.isFalse(u.get("activo")), "INACTIVO")
                                    .otherwise(m.<String>get("estado"));
                    var condiciones = new java.util.ArrayList<Predicate>();
                    condiciones.add(cb.equal(se.get("id"), sede));
                    if (id != null) condiciones.add(cb.equal(m.get("id"), id));
                    if (filtro != null) {
                        if (!filtro.buscar().isEmpty()) {
                            String patron =
                                    "%"
                                            + filtro.buscar()
                                                    .replace("!", "!!")
                                                    .replace("%", "!%")
                                                    .replace("_", "!_")
                                                    .toLowerCase(java.util.Locale.ROOT)
                                            + "%";
                            condiciones.add(
                                    cb.or(
                                            cb.like(cb.lower(nombre), patron, '!'),
                                            cb.like(cb.lower(m.get("cmp")), patron, '!')));
                        }
                        if (!filtro.especialidad().isEmpty())
                            condiciones.add(cb.equal(e.get("nombre"), filtro.especialidad()));
                        if (!filtro.sede().isEmpty())
                            condiciones.add(cb.equal(se.get("nombre"), filtro.sede()));
                        if (!filtro.estado().isEmpty())
                            condiciones.add(cb.equal(estado, filtro.estado()));
                    }
                    q.select(
                                    cb.construct(
                                            Medico.class,
                                            m.get("id"),
                                            nombre,
                                            m.get("cmp"),
                                            e.get("id"),
                                            e.get("nombre"),
                                            se.get("nombre"),
                                            estado))
                            .where(condiciones.toArray(Predicate[]::new))
                            .orderBy(cb.asc(m.get("id")));
                    var consulta = s.createQuery(q);
                    if (id != null) consulta.setLockMode(LockModeType.PESSIMISTIC_WRITE);
                    return consulta.getResultList();
                });
    }

    public List<Especialidad> especialidades(int actual) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Especialidad.class);
                    var e = q.from(EspecialidadEntidad.class);
                    q.select(cb.construct(Especialidad.class, e.get("id"), e.get("nombre")))
                            .where(
                                    cb.or(
                                            cb.equal(e.get("estado"), "ACTIVO"),
                                            cb.equal(e.get("id"), actual)))
                            .orderBy(cb.asc(e.get("nombre")));
                    return s.createQuery(q).getResultList();
                });
    }

    public boolean cuentaActiva(int medico) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Boolean.class);
                    var m = q.from(MedicoEntidad.class);
                    q.select(m.get("usuario").get("activo")).where(cb.equal(m.get("id"), medico));
                    return s.createQuery(q).getResultList().stream().anyMatch(Boolean.TRUE::equals);
                });
    }

    public boolean tieneCitas(int medico) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var c = q.from(CitaEntidad.class);
                    q.select(c.get("id")).where(cb.equal(c.get("medico").get("id"), medico));
                    return !s.createQuery(q).setMaxResults(1).getResultList().isEmpty();
                });
    }

    public boolean agendaActiva(int medico, LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var m = q.from(MedicoEntidad.class);
                    var turnos = q.subquery(Integer.class);
                    var d = turnos.from(DisponibilidadEntidad.class);
                    turnos.select(cb.literal(1))
                            .where(
                                    cb.equal(d.get("medico").get("id"), m.get("id")),
                                    cb.equal(d.get("estado"), "PUBLICADO"),
                                    cb.greaterThanOrEqualTo(d.get("fecha"), ahora.toLocalDate()));
                    var citas = q.subquery(Integer.class);
                    var c = citas.from(CitaEntidad.class);
                    citas.select(cb.literal(1))
                            .where(
                                    cb.equal(c.get("medico").get("id"), m.get("id")),
                                    cb.or(
                                            c.get("estado").in("EN_SALA", "EN_ATENCION"),
                                            cb.and(
                                                    cb.greaterThan(c.get("inicio"), ahora),
                                                    c.get("estado")
                                                            .in("SOLICITADA", "CONFIRMADA"))));
                    q.select(m.get("id"))
                            .where(
                                    cb.equal(m.get("id"), medico),
                                    cb.or(cb.exists(turnos), cb.exists(citas)));
                    return !s.createQuery(q).getResultList().isEmpty();
                });
    }

    public void guardar(int medico, int sede, int especialidad, String estado) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(MedicoEntidad.class);
                    var m = q.from(MedicoEntidad.class);
                    q.set("especialidad", s.getReference(EspecialidadEntidad.class, especialidad))
                            .set("estado", estado)
                            .where(
                                    cb.equal(m.get("id"), medico),
                                    cb.equal(m.get("sede").get("id"), sede));
                    s.createMutationQuery(q).executeUpdate();
                    return null;
                });
    }
}
