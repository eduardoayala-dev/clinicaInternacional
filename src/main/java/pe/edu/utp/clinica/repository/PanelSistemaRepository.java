package pe.edu.utp.clinica.repository;

import jakarta.persistence.criteria.*;

import pe.edu.utp.clinica.dao.PanelSistemaDAO.*;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.*;
import java.time.*;
import java.util.*;

public final class PanelSistemaRepository {
    public record Metricas(
            int sede,
            long citas,
            long atendidas,
            long canceladas,
            long ausencias,
            long reasignadas,
            long bajo,
            long medio,
            long alto,
            long sinRiesgo) {}

    public record Ofertas(int sede, long total, long aceptadas, long pendientes) {}

    public record Dia(LocalDate fecha, long citas, long atendidas) {}

    private final Connection conexion;

    public PanelSistemaRepository(Connection conexion) {
        this.conexion = conexion;
    }

    private static Expression<Long> contar(CriteriaBuilder cb, Predicate condicion) {
        return cb.sum(cb.<Long>selectCase().when(condicion, 1L).otherwise(0L));
    }

    private static Predicate filtros(
            CriteriaBuilder cb,
            Root<CitaEntidad> c,
            Join<?, ?> m,
            Expression<Integer> sede,
            Filtro f) {
        return cb.and(
                cb.greaterThanOrEqualTo(c.get("inicio"), f.desde().atStartOfDay()),
                cb.lessThan(c.get("inicio"), f.hasta().plusDays(1).atStartOfDay()),
                f.sede() == 0 ? cb.conjunction() : cb.equal(sede, f.sede()),
                f.especialidad() == 0
                        ? cb.conjunction()
                        : cb.equal(m.get("especialidad").get("id"), f.especialidad()));
    }

    public List<Opcion> sedes() throws SQLException {
        return opciones(SedeEntidad.class);
    }

    public List<Opcion> especialidades() throws SQLException {
        return opciones(EspecialidadEntidad.class);
    }

    private <T> List<Opcion> opciones(Class<T> tipo) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Opcion.class);
                    var r = q.from(tipo);
                    q.select(cb.construct(Opcion.class, r.get("id"), r.get("nombre")))
                            .orderBy(cb.asc(r.get("nombre")), cb.asc(r.get("id")));
                    return s.createQuery(q).getResultList();
                });
    }

    public List<Sede> detalleSedes(int sede) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var r = q.from(SedeEntidad.class);
                    q.multiselect(r.get("id"), r.get("nombre"), r.get("direccion"), r.get("estado"))
                            .where(sede == 0 ? cb.conjunction() : cb.equal(r.get("id"), sede))
                            .orderBy(cb.asc(r.get("nombre")), cb.asc(r.get("id")));
                    return s.createQuery(q).getResultList().stream()
                            .map(
                                    t ->
                                            new Sede(
                                                    t.get(0, Integer.class),
                                                    t.get(1, String.class),
                                                    t.get(2, String.class),
                                                    t.get(3, String.class),
                                                    Indicadores.cero()))
                            .toList();
                });
    }

    public List<Metricas> citas(Filtro f) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Metricas.class);
                    var c = q.from(CitaEntidad.class);
                    var m = c.join("medico");
                    var d = c.join("disponibilidad", JoinType.LEFT);
                    var i = c.join("inasistencia", JoinType.LEFT);
                    var sede = cb.<Integer>coalesce(d.get("sede"), m.get("sede").get("id"));
                    var asignada = q.subquery(Integer.class);
                    var r = asignada.from(OfertaEntidad.class);
                    asignada.select(cb.literal(1))
                            .where(
                                    cb.equal(r.get("asignada"), c.get("id")),
                                    cb.equal(r.get("estado"), "ACEPTADA"));
                    q.select(
                                    cb.construct(
                                            Metricas.class,
                                            sede,
                                            cb.count(c),
                                            contar(cb, cb.equal(c.get("estado"), "ATENDIDA")),
                                            contar(cb, cb.equal(c.get("estado"), "CANCELADA")),
                                            contar(cb, cb.isNotNull(i.get("id"))),
                                            contar(cb, cb.exists(asignada)),
                                            contar(cb, cb.equal(c.get("riesgo"), "BAJO")),
                                            contar(cb, cb.equal(c.get("riesgo"), "MEDIO")),
                                            contar(cb, cb.equal(c.get("riesgo"), "ALTO")),
                                            contar(
                                                    cb,
                                                    cb.or(
                                                            cb.isNull(c.get("riesgo")),
                                                            cb.not(
                                                                    c.get("riesgo")
                                                                            .in(
                                                                                    "BAJO", "MEDIO",
                                                                                    "ALTO"))))))
                            .where(filtros(cb, c, m, sede, f))
                            .groupBy(sede);
                    return s.createQuery(q).getResultList();
                });
    }

    public List<Ofertas> ofertas(Filtro f, LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Ofertas.class);
                    var r = q.from(OfertaEntidad.class);
                    var c = r.join("cita");
                    var m = c.join("medico");
                    var d = c.join("disponibilidad", JoinType.LEFT);
                    var sede = cb.<Integer>coalesce(d.get("sede"), m.get("sede").get("id"));
                    var condiciones =
                            cb.and(
                                    cb.greaterThanOrEqualTo(
                                            c.get("inicio"), f.desde().atStartOfDay()),
                                    cb.lessThan(
                                            c.get("inicio"), f.hasta().plusDays(1).atStartOfDay()),
                                    f.sede() == 0 ? cb.conjunction() : cb.equal(sede, f.sede()),
                                    f.especialidad() == 0
                                            ? cb.conjunction()
                                            : cb.equal(
                                                    m.get("especialidad").get("id"),
                                                    f.especialidad()));
                    var pendiente =
                            cb.and(
                                    cb.equal(r.get("estado"), "PENDIENTE"),
                                    cb.greaterThan(r.get("vence"), ahora),
                                    cb.greaterThan(c.get("inicio"), ahora),
                                    cb.equal(c.get("estado"), "CANCELADA"));
                    q.select(
                                    cb.construct(
                                            Ofertas.class,
                                            sede,
                                            cb.count(r),
                                            contar(cb, cb.equal(r.get("estado"), "ACEPTADA")),
                                            contar(cb, pendiente)))
                            .where(condiciones)
                            .groupBy(sede);
                    return s.createQuery(q).getResultList();
                });
    }

    public List<Dia> dias(Filtro f) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Dia.class);
                    var c = q.from(CitaEntidad.class);
                    var m = c.join("medico");
                    var d = c.join("disponibilidad", JoinType.LEFT);
                    var sede = cb.<Integer>coalesce(d.get("sede"), m.get("sede").get("id"));
                    var dia = c.<LocalDateTime>get("inicio").cast(LocalDate.class);
                    q.select(
                                    cb.construct(
                                            Dia.class,
                                            dia,
                                            cb.count(c),
                                            contar(cb, cb.equal(c.get("estado"), "ATENDIDA"))))
                            .where(filtros(cb, c, m, sede, f))
                            .groupBy(dia)
                            .orderBy(cb.asc(dia));
                    return s.createQuery(q).getResultList();
                });
    }

    public List<Auditoria> auditoria() throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Auditoria.class);
                    var a = q.from(AuditoriaEntidad.class);
                    var u = a.join("actor", JoinType.LEFT);
                    var nombre =
                            cb.trim(
                                    cb.concat(
                                            cb.concat(cb.coalesce(u.get("nombres"), ""), " "),
                                            cb.coalesce(u.get("apellidos"), "")));
                    q.select(
                                    cb.construct(
                                            Auditoria.class,
                                            a.get("fecha"),
                                            cb.coalesce(cb.nullif(nombre, ""), "Sistema"),
                                            a.get("evento"),
                                            a.get("descripcion")))
                            .orderBy(cb.desc(a.get("fecha")), cb.desc(a.get("id")));
                    return s.createQuery(q).setMaxResults(8).getResultList();
                });
    }

    public Totales totales() throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var u = q.from(UsuarioEntidad.class);
                    q.multiselect(u.get("rol"), cb.count(u)).groupBy(u.get("rol"));
                    long[] n = new long[6];
                    for (var t : s.createQuery(q).getResultList()) {
                        long v = t.get(1, Long.class);
                        n[0] += v;
                        switch (t.get(0, String.class)) {
                            case "PACIENTE" -> n[1] += v;
                            case "MEDICO" -> n[2] += v;
                            case "PERSONAL_ADMINISTRATIVO" -> n[3] += v;
                            case "ADMINISTRADOR" -> n[4] += v;
                            default -> {}
                        }
                    }
                    var sedes = cb.createQuery(Long.class);
                    sedes.select(cb.count(sedes.from(SedeEntidad.class)));
                    n[5] = s.createQuery(sedes).getSingleResult();
                    return new Totales(n[0], n[1], n[2], n[3], n[4], n[5]);
                });
    }
}
