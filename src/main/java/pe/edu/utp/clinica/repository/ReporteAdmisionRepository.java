package pe.edu.utp.clinica.repository;

import jakarta.persistence.criteria.*;

import pe.edu.utp.clinica.dao.ReporteAdmisionDAO.*;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;
import pe.edu.utp.clinica.reporte.FiltroReporte;

import java.sql.*;
import java.time.*;
import java.util.*;

public final class ReporteAdmisionRepository {
    public record Cupo(int medico, LocalDateTime inicio) {}

    public record Bloque(
            int medico, LocalDate fecha, LocalTime inicio, LocalTime fin, int minutos) {}

    private final Connection conexion;

    public ReporteAdmisionRepository(Connection conexion) {
        this.conexion = conexion;
    }

    private record Consulta(
            Root<CitaEntidad> cita,
            Join<?, ?> medico,
            Join<?, ?> usuarioMedico,
            Join<?, ?> especialidad,
            Join<?, ?> paciente,
            Join<?, ?> usuario,
            Join<?, ?> confirmacion,
            Join<?, ?> admision,
            Join<?, ?> atencion,
            Predicate filtros) {}

    private static Consulta consulta(
            CriteriaBuilder cb,
            CriteriaQuery<?> q,
            int sede,
            FiltroReporte f,
            LocalDateTime ahora) {
        var c = q.from(CitaEntidad.class);
        var m = c.join("medico");
        var um = m.join("usuario");
        var e = m.join("especialidad");
        var p = c.join("pacienteRelacion");
        var u = p.join("usuario");
        var ca = c.join("confirmacion", JoinType.LEFT);
        var a = c.join("admision", JoinType.LEFT);
        var t = c.join("atencion", JoinType.LEFT);
        var d = c.join("disponibilidad", JoinType.LEFT);
        var filtros =
                cb.and(
                        cb.equal(cb.coalesce(d.get("sede"), m.get("sede").get("id")), sede),
                        cb.greaterThanOrEqualTo(c.get("inicio"), f.desde().atStartOfDay()),
                        cb.lessThan(c.get("inicio"), f.hasta().plusDays(1).atStartOfDay()),
                        f.estado().isEmpty()
                                ? cb.conjunction()
                                : cb.equal(c.get("estado"), f.estado()),
                        !"contacto".equals(f.tipo())
                                ? cb.conjunction()
                                : cb.and(
                                        c.get("estado").in("SOLICITADA", "CONFIRMADA"),
                                        cb.isNull(ca.get("confirmada")),
                                        cb.or(
                                                c.get("riesgo").in("ALTO", "MEDIO"),
                                                cb.lessThanOrEqualTo(ca.get("vence"), ahora))));
        return new Consulta(c, m, um, e, p, u, ca, a, t, filtros);
    }

    private static Expression<Long> contar(CriteriaBuilder cb, Predicate condicion) {
        return cb.sum(cb.<Long>selectCase().when(condicion, 1L).otherwise(0L));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static org.hibernate.query.criteria.JpaExpression<Duration> duracion(
            org.hibernate.query.criteria.HibernateCriteriaBuilder cb,
            Expression<LocalDateTime> fin,
            Expression<LocalDateTime> inicio) {
        return cb.durationBetween(
                (Expression)
                        ((org.hibernate.query.criteria.JpaExpression<LocalDateTime>) fin)
                                .cast(java.sql.Timestamp.class),
                (Expression)
                        ((org.hibernate.query.criteria.JpaExpression<LocalDateTime>) inicio)
                                .cast(java.sql.Timestamp.class));
    }

    public Resumen resumen(int sede, FiltroReporte f, LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var v = consulta(cb, q, sede, f, ahora);
                    var c = v.cita();
                    var pendiente =
                            c.get("estado")
                                    .in("SOLICITADA", "CONFIRMADA", "EN_SALA", "EN_ATENCION");
                    var segundos =
                            cb.durationByUnit(
                                    org.hibernate.query.sqm.TemporalUnit.SECOND,
                                    duracion(
                                            cb,
                                            v.atencion().<LocalDateTime>get("inicio"),
                                            v.admision().<LocalDateTime>get("llegada")));
                    var espera =
                            cb.avg(
                                    cb.<Double>selectCase()
                                            .when(
                                                    cb.greaterThanOrEqualTo(
                                                            v.atencion()
                                                                    .<LocalDateTime>get("inicio"),
                                                            v.admision()
                                                                    .<LocalDateTime>get("llegada")),
                                                    cb.quot(segundos, 60.0).as(Double.class))
                                            .otherwise(cb.nullLiteral(Double.class)));
                    q.multiselect(
                                    cb.count(c),
                                    contar(cb, cb.equal(c.get("estado"), "ATENDIDA")),
                                    contar(cb, cb.equal(c.get("estado"), "EN_ATENCION")),
                                    contar(cb, pendiente),
                                    contar(cb, cb.equal(c.get("estado"), "CANCELADA")),
                                    contar(
                                            cb,
                                            cb.and(pendiente, cb.equal(c.get("riesgo"), "BAJO"))),
                                    contar(
                                            cb,
                                            cb.and(pendiente, cb.equal(c.get("riesgo"), "MEDIO"))),
                                    contar(
                                            cb,
                                            cb.and(pendiente, cb.equal(c.get("riesgo"), "ALTO"))),
                                    espera)
                            .where(v.filtros());
                    var r = s.createQuery(q).getSingleResult();
                    long[] n = new long[8];
                    for (int i = 0; i < 8; i++)
                        n[i] = r.get(i) == null ? 0 : ((Number) r.get(i)).longValue();
                    var medicos = cb.createQuery(Long.class);
                    var m = medicos.from(MedicoEntidad.class);
                    var u = m.join("usuario");
                    medicos.select(cb.count(m))
                            .where(
                                    cb.equal(m.get("sede").get("id"), sede),
                                    cb.equal(m.get("estado"), "ACTIVO"),
                                    cb.isTrue(u.get("activo")),
                                    cb.equal(u.get("rol"), "MEDICO"));
                    var ofertas = cb.createTupleQuery();
                    var o = ofertas.from(OfertaEntidad.class);
                    var ci = o.join("cita");
                    var me = ci.join("medico");
                    var d = ci.join("disponibilidad", JoinType.LEFT);
                    ofertas.multiselect(
                                    cb.count(o), contar(cb, cb.equal(o.get("estado"), "ACEPTADA")))
                            .where(
                                    cb.equal(
                                            cb.coalesce(d.get("sede"), me.get("sede").get("id")),
                                            sede),
                                    cb.greaterThanOrEqualTo(
                                            ci.get("inicio"), f.desde().atStartOfDay()),
                                    cb.lessThan(
                                            ci.get("inicio"), f.hasta().plusDays(1).atStartOfDay()),
                                    f.estado().isEmpty()
                                            ? cb.conjunction()
                                            : cb.equal(ci.get("estado"), f.estado()));
                    var of = s.createQuery(ofertas).getSingleResult();
                    return new Resumen(
                            n[0],
                            n[1],
                            n[2],
                            n[3],
                            n[4],
                            n[5],
                            n[6],
                            n[7],
                            Math.max(0, n[3] - n[5] - n[6] - n[7]),
                            r.get(8) == null ? null : ((Number) r.get(8)).doubleValue(),
                            s.createQuery(medicos).getSingleResult(),
                            ((Number) of.get(0)).longValue(),
                            of.get(1) == null ? 0 : ((Number) of.get(1)).longValue());
                });
    }

    public List<Cita> citas(int sede, FiltroReporte f, LocalDateTime ahora, int limite, int offset)
            throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var v = consulta(cb, q, sede, f, ahora);
                    var c = v.cita();
                    q.multiselect(
                                    c.get("id"),
                                    c.get("inicio"),
                                    cb.concat(
                                            cb.concat(v.usuario().get("nombres"), " "),
                                            v.usuario().get("apellidos")),
                                    v.paciente().get("dni"),
                                    v.usuario().get("email"),
                                    v.paciente().get("telefono"),
                                    cb.concat(
                                            cb.concat(v.usuarioMedico().get("nombres"), " "),
                                            v.usuarioMedico().get("apellidos")),
                                    v.especialidad().get("nombre"),
                                    c.get("estado"),
                                    c.get("riesgo"),
                                    v.confirmacion().get("confirmada"),
                                    v.confirmacion().get("vence"))
                            .where(v.filtros())
                            .orderBy(cb.asc(c.get("inicio")), cb.asc(c.get("id")));
                    return s
                            .createQuery(q)
                            .setMaxResults(limite)
                            .setFirstResult(offset)
                            .getResultList()
                            .stream()
                            .map(
                                    t -> {
                                        var vence = t.get(11, LocalDateTime.class);
                                        String confirmacion =
                                                t.get(10) != null
                                                        ? "Confirmada"
                                                        : vence == null
                                                                ? "Sin plazo registrado"
                                                                : ahora.isBefore(vence)
                                                                        ? "Pendiente"
                                                                        : "Plazo vencido";
                                        return new Cita(
                                                t.get(0, Integer.class),
                                                t.get(1, LocalDateTime.class),
                                                t.get(2, String.class),
                                                t.get(3, String.class),
                                                t.get(4, String.class),
                                                t.get(5, String.class),
                                                t.get(6, String.class),
                                                t.get(7, String.class),
                                                Objects.toString(t.get(8), "SIN_ESTADO"),
                                                Objects.toString(t.get(9), "SIN_DATO"),
                                                confirmacion);
                                    })
                            .toList();
                });
    }

    public Ocupacion ocupacion(int sede, FiltroReporte f, LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var bloques = cb.createQuery(Bloque.class);
                    var d = bloques.from(DisponibilidadEntidad.class);
                    bloques.select(
                                    cb.construct(
                                            Bloque.class,
                                            d.get("medico").get("id"),
                                            d.get("fecha"),
                                            d.get("inicio"),
                                            d.get("fin"),
                                            d.get("minutos")))
                            .where(
                                    cb.equal(d.get("sede"), sede),
                                    cb.greaterThanOrEqualTo(d.get("fecha"), f.desde()),
                                    cb.lessThanOrEqualTo(d.get("fecha"), f.hasta()),
                                    cb.equal(d.get("estado"), "PUBLICADO"),
                                    cb.gt(d.get("minutos"), 0),
                                    cb.greaterThan(
                                            d.<LocalTime>get("fin"), d.<LocalTime>get("inicio")));
                    Set<Cupo> publicados = new HashSet<>();
                    for (var b : s.createQuery(bloques).getResultList()) {
                        var fin = b.fecha().atTime(b.fin());
                        for (var hora = b.fecha().atTime(b.inicio());
                                !hora.plusMinutes(b.minutos()).isAfter(fin);
                                hora = hora.plusMinutes(b.minutos()))
                            publicados.add(new Cupo(b.medico(), hora));
                    }
                    var reservas = cb.createQuery(Cupo.class);
                    var c = reservas.from(CitaEntidad.class);
                    var m = c.join("medico");
                    var ds = c.join("disponibilidad", JoinType.LEFT);
                    reservas.select(cb.construct(Cupo.class, m.get("id"), c.get("inicio")))
                            .where(
                                    cb.equal(
                                            cb.coalesce(ds.get("sede"), m.get("sede").get("id")),
                                            sede),
                                    cb.greaterThanOrEqualTo(
                                            c.get("inicio"), f.desde().atStartOfDay()),
                                    cb.lessThan(
                                            c.get("inicio"), f.hasta().plusDays(1).atStartOfDay()),
                                    cb.not(c.get("estado").in("CANCELADA", "REPROGRAMADA")));
                    var reservados = new HashSet<>(s.createQuery(reservas).getResultList());
                    reservados.retainAll(publicados);
                    var ofertas = cb.createQuery(Long.class);
                    var o = ofertas.from(OfertaEntidad.class);
                    var disponibilidad = o.join("disponibilidad");
                    var ci = o.join("cita");
                    ofertas.select(cb.count(o))
                            .where(
                                    cb.equal(disponibilidad.get("sede"), sede),
                                    cb.greaterThanOrEqualTo(
                                            ci.get("inicio"), f.desde().atStartOfDay()),
                                    cb.lessThan(
                                            ci.get("inicio"), f.hasta().plusDays(1).atStartOfDay()),
                                    cb.equal(o.get("estado"), "PENDIENTE"),
                                    cb.greaterThan(o.get("vence"), ahora),
                                    cb.greaterThan(ci.get("inicio"), ahora),
                                    cb.equal(ci.get("estado"), "CANCELADA"),
                                    cb.equal(disponibilidad.get("estado"), "PUBLICADO"));
                    return new Ocupacion(
                            publicados.size(),
                            reservados.size(),
                            s.createQuery(ofertas).getSingleResult());
                });
    }

    public String nombreSede(int sede) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(String.class);
                    var r = q.from(SedeEntidad.class);
                    q.select(r.get("nombre")).where(cb.equal(r.get("id"), sede));
                    return s.createQuery(q).getSingleResult();
                });
    }
}
