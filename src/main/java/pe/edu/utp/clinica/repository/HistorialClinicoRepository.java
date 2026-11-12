package pe.edu.utp.clinica.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.*;

import pe.edu.utp.clinica.model.medico.*;
import pe.edu.utp.clinica.persistence.FilaConsulta;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

public final class HistorialClinicoRepository {
    public record Vinculo(int usuario, String estadoControl) {}

    public record Existente(int control, String estado) {}

    private final Connection conexion;

    public HistorialClinicoRepository(Connection conexion) {
        this.conexion = conexion;
    }

    private static Predicate relacion(
            CriteriaBuilder cb, CommonAbstractCriteria q, Root<CitaEntidad> c, Join<?, ?> t) {
        var historial = q.subquery(Integer.class);
        var h = historial.from(HistorialAsistenciaEntidad.class);
        historial
                .select(cb.literal(1))
                .where(cb.equal(h.get("cita"), c.get("id")), cb.isTrue(h.get("asistio")));
        return cb.or(cb.isNotNull(t.get("inicio")), cb.exists(historial));
    }

    private static Predicate busqueda(
            CriteriaBuilder cb, Join<?, ?> u, Join<?, ?> p, String filtro) {
        return cb.or(
                cb.like(
                        cb.lower(cb.concat(cb.concat(u.get("nombres"), " "), u.get("apellidos"))),
                        filtro,
                        '!'),
                cb.like(p.get("dni"), filtro, '!'));
    }

    public List<FilaConsulta> pacientes(int medico, String filtro, Integer paciente, int pagina)
            throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var c = q.from(CitaEntidad.class);
                    var p = c.join("pacienteRelacion");
                    var u = p.join("usuario");
                    var t = c.join("atencion", JoinType.LEFT);
                    var fecha = cb.<LocalDateTime>coalesce(t.get("inicio"), c.get("inicio"));
                    var ultima =
                            cb.greatest(fecha.cast(java.sql.Timestamp.class))
                                    .cast(LocalDateTime.class);
                    q.multiselect(
                                    p.get("id").alias("id_paciente"),
                                    p.get("dni").alias("dni"),
                                    p.get("alergias").alias("alergias"),
                                    p.get("nacimiento").alias("fecha_nacimiento"),
                                    u.get("nombres").alias("nombres"),
                                    u.get("apellidos").alias("apellidos"),
                                    ultima.alias("ultima"),
                                    cb.count(c).alias("atenciones"))
                            .where(
                                    cb.equal(c.get("medico").get("id"), medico),
                                    relacion(cb, q, c, t),
                                    paciente == null
                                            ? busqueda(cb, u, p, filtro)
                                            : cb.equal(p.get("id"), paciente))
                            .groupBy(
                                    p.get("id"),
                                    p.get("dni"),
                                    p.get("alergias"),
                                    p.get("nacimiento"),
                                    u.get("nombres"),
                                    u.get("apellidos"))
                            .orderBy(cb.desc(ultima), cb.desc(p.get("id")));
                    var consulta = s.createQuery(q);
                    if (paciente == null)
                        consulta.setMaxResults(21).setFirstResult((pagina - 1) * 20);
                    return consulta.getResultList().stream().map(FilaConsulta::new).toList();
                });
    }

    public List<AtencionResumen> atenciones(int medico, int paciente, int pagina)
            throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(AtencionResumen.class);
                    var c = q.from(CitaEntidad.class);
                    var t = c.join("atencion", JoinType.LEFT);
                    var cc = c.join("consultaClinica", JoinType.LEFT);
                    q.select(
                                    cb.construct(
                                            AtencionResumen.class,
                                            c.get("id"),
                                            c.get("inicio"),
                                            c.get("estado"),
                                            c.get("prioridad"),
                                            cb.coalesce(
                                                    ConsultaMedicaRepository.indicaciones(
                                                            cb, q, c.get("id")),
                                                    ""),
                                            cb.coalesce(cc.get("diagnostico"), ""),
                                            c.get("recomendada")))
                            .where(
                                    cb.equal(c.get("medico").get("id"), medico),
                                    cb.equal(c.get("paciente"), paciente),
                                    relacion(cb, q, c, t))
                            .orderBy(cb.desc(c.get("inicio")), cb.desc(c.get("id")));
                    return s.createQuery(q)
                            .setMaxResults(21)
                            .setFirstResult((pagina - 1) * 20)
                            .getResultList();
                });
    }

    public List<SeguimientoResumen> seguimientos(
            int medico, String filtro, String estado, Integer origen, int pagina, LocalDate hoy)
            throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(SeguimientoResumen.class);
                    var c = q.from(CitaEntidad.class);
                    var p = c.join("pacienteRelacion");
                    var u = p.join("usuario");
                    var t = c.join("atencion", JoinType.LEFT);
                    var seguimiento = c.join("seguimiento", JoinType.LEFT);
                    var control = seguimiento.join("control", JoinType.LEFT);
                    control.on(
                            cb.equal(control.get("medico").get("id"), c.get("medico").get("id")),
                            cb.equal(control.get("paciente"), c.get("paciente")));
                    var calculado =
                            cb.<String>selectCase()
                                    .when(cb.equal(control.get("estado"), "ATENDIDA"), "REALIZADO")
                                    .when(
                                            control.get("estado")
                                                    .in(
                                                            "SOLICITADA",
                                                            "CONFIRMADA",
                                                            "EN_SALA",
                                                            "EN_ATENCION"),
                                            "RESERVADO")
                                    .when(cb.lessThan(c.get("recomendada"), hoy), "VENCIDO")
                                    .otherwise("PENDIENTE");
                    var condiciones = new ArrayList<Predicate>();
                    condiciones.add(cb.equal(c.get("medico").get("id"), medico));
                    condiciones.add(cb.equal(c.get("estado"), "ATENDIDA"));
                    condiciones.add(cb.isNotNull(c.get("recomendada")));
                    condiciones.add(relacion(cb, q, c, t));
                    if (origen != null) condiciones.add(cb.equal(c.get("id"), origen));
                    else {
                        condiciones.add(busqueda(cb, u, p, filtro));
                        if (!estado.isEmpty()) condiciones.add(cb.equal(calculado, estado));
                    }
                    q.select(
                                    cb.construct(
                                            SeguimientoResumen.class,
                                            c.get("id"),
                                            p.get("id"),
                                            cb.concat(
                                                    cb.concat(u.get("nombres"), " "),
                                                    u.get("apellidos")),
                                            p.get("dni"),
                                            c.get("recomendada"),
                                            c.get("prioridad"),
                                            calculado,
                                            seguimiento.get("control").get("id"),
                                            control.get("inicio")))
                            .where(condiciones.toArray(Predicate[]::new))
                            .orderBy(cb.asc(c.get("recomendada")), cb.asc(c.get("id")));
                    var consulta = s.createQuery(q);
                    if (origen == null)
                        consulta.setMaxResults(21).setFirstResult((pagina - 1) * 20);
                    return consulta.getResultList();
                });
    }

    public List<CitaControl> controles(int medico, int paciente, int origen) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(CitaControl.class);
                    var control = q.from(CitaEntidad.class);
                    var c = q.from(CitaEntidad.class);
                    var t = c.join("atencion", JoinType.LEFT);
                    q.select(
                                    cb.construct(
                                            CitaControl.class,
                                            control.get("id"),
                                            control.get("inicio"),
                                            control.get("estado")))
                            .where(
                                    cb.equal(c.get("id"), origen),
                                    cb.equal(control.get("medico").get("id"), medico),
                                    cb.equal(control.get("paciente"), paciente),
                                    cb.notEqual(control.get("id"), c.get("id")),
                                    cb.greaterThan(
                                            control.get("inicio"),
                                            cb.<LocalDateTime>coalesce(
                                                    t.get("fin"), c.get("inicio"))),
                                    control.get("estado")
                                            .in(
                                                    "SOLICITADA",
                                                    "CONFIRMADA",
                                                    "EN_SALA",
                                                    "EN_ATENCION",
                                                    "ATENDIDA"))
                            .orderBy(cb.asc(control.get("inicio")), cb.asc(control.get("id")));
                    return s.createQuery(q).setMaxResults(100).getResultList();
                });
    }

    public Vinculo bloquearVinculo(int medico, int paciente, int origen, int control)
            throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Vinculo.class);
                    var c = q.from(CitaEntidad.class);
                    var m = c.join("medico");
                    var u = m.join("usuario");
                    var ct = q.from(CitaEntidad.class);
                    var t = c.join("atencion", JoinType.LEFT);
                    q.select(cb.construct(Vinculo.class, u.get("id"), ct.get("estado")))
                            .where(
                                    cb.equal(ct.get("id"), control),
                                    cb.equal(ct.get("medico").get("id"), m.get("id")),
                                    cb.equal(ct.get("paciente"), c.get("paciente")),
                                    cb.equal(c.get("id"), origen),
                                    cb.equal(m.get("id"), medico),
                                    cb.equal(c.get("paciente"), paciente),
                                    cb.equal(c.get("estado"), "ATENDIDA"),
                                    cb.isNotNull(c.get("recomendada")),
                                    relacion(cb, q, c, t),
                                    cb.notEqual(ct.get("id"), c.get("id")),
                                    cb.greaterThan(
                                            ct.get("inicio"),
                                            cb.<LocalDateTime>coalesce(
                                                    t.get("fin"), c.get("inicio"))),
                                    cb.isTrue(u.get("activo")),
                                    cb.equal(u.get("rol"), "MEDICO"));
                    return s
                            .createQuery(q)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .getResultList()
                            .stream()
                            .findFirst()
                            .orElse(null);
                });
    }

    public Existente existente(int origen) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Existente.class);
                    var seguimiento = q.from(SeguimientoEntidad.class);
                    var c = seguimiento.join("origen");
                    var control = seguimiento.join("control", JoinType.LEFT);
                    control.on(
                            cb.equal(control.get("medico").get("id"), c.get("medico").get("id")),
                            cb.equal(control.get("paciente"), c.get("paciente")));
                    q.select(
                                    cb.construct(
                                            Existente.class,
                                            seguimiento.get("control").get("id"),
                                            control.get("estado")))
                            .where(cb.equal(seguimiento.get("id"), origen));
                    return s
                            .createQuery(q)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .getResultList()
                            .stream()
                            .findFirst()
                            .orElse(null);
                });
    }

    public void vincular(int origen, int control, int usuario, LocalDateTime ahora)
            throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var anterior =
                            s.find(
                                    SeguimientoEntidad.class,
                                    origen,
                                    LockModeType.PESSIMISTIC_WRITE);
                    var cita = s.getReference(CitaEntidad.class, control);
                    if (anterior == null)
                        s.persist(new SeguimientoEntidad(origen, cita, usuario, ahora));
                    else anterior.vincular(cita, usuario, ahora);
                    return null;
                });
    }
}
