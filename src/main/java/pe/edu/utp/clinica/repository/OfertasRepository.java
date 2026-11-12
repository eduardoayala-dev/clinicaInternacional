package pe.edu.utp.clinica.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.*;

import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public final class OfertasRepository {
    public record Cupo(
            int cita,
            int medico,
            int disponibilidad,
            int especialidad,
            int sede,
            int paciente,
            LocalDateTime inicio) {}

    public record Oferta(
            int id,
            int lista,
            String estado,
            LocalDateTime vence,
            Integer citaAsignada,
            Cupo cupo) {}

    public record Presentacion(
            int id,
            int lista,
            LocalDateTime vence,
            LocalDateTime inicio,
            String nombres,
            String apellidos,
            String especialidad,
            String sede) {}

    public record Ultima(String estado, LocalDateTime vence) {}

    private final Connection conexion;

    public OfertasRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public int crear(
            int cita, int lista, int disponibilidad, LocalDateTime ahora, LocalDateTime vence)
            throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var o =
                            new OfertaEntidad(
                                    s.getReference(CitaEntidad.class, cita),
                                    s.getReference(ListaEsperaEntidad.class, lista),
                                    s.getReference(DisponibilidadEntidad.class, disponibilidad),
                                    ahora,
                                    vence);
                    s.persist(o);
                    return o.id();
                });
    }

    public Cupo cupo(int cita) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Cupo.class);
                    var c = q.from(CitaEntidad.class);
                    var m = c.join("medico");
                    var d = c.join("disponibilidad");
                    q.select(
                                    cb.construct(
                                            Cupo.class,
                                            c.get("id"),
                                            m.get("id"),
                                            d.get("id"),
                                            m.get("especialidad").get("id"),
                                            m.get("sede").get("id"),
                                            c.get("paciente"),
                                            c.get("inicio")))
                            .where(
                                    cb.equal(c.get("id"), cita),
                                    cb.equal(c.get("estado"), "CANCELADA"),
                                    cb.equal(d.get("estado"), "PUBLICADO"),
                                    cb.equal(d.get("sede"), m.get("sede").get("id")));
                    return s.createQuery(q).getResultList().stream().findFirst().orElse(null);
                });
    }

    public Oferta propia(int paciente, int id, boolean bloquear) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var r = q.from(OfertaEntidad.class);
                    var l = r.join("lista");
                    var c = r.join("cita");
                    var m = c.join("medico");
                    q.multiselect(
                                    r.get("id"),
                                    l.get("id"),
                                    r.get("estado"),
                                    r.get("vence"),
                                    r.get("asignada"),
                                    m.get("id"),
                                    c.get("inicio"),
                                    m.get("especialidad").get("id"),
                                    m.get("sede").get("id"),
                                    c.get("disponibilidad").get("id"),
                                    c.get("id"))
                            .where(
                                    cb.equal(l.get("paciente").get("id"), paciente),
                                    cb.equal(r.get("id"), id));
                    var consulta = s.createQuery(q);
                    if (bloquear) consulta.setLockMode(LockModeType.PESSIMISTIC_WRITE);
                    var filas = consulta.getResultList();
                    if (filas.isEmpty()) return null;
                    var f = filas.get(0);
                    var cupo =
                            new Cupo(
                                    f.get(10, Integer.class),
                                    f.get(5, Integer.class),
                                    f.get(9, Integer.class),
                                    f.get(7, Integer.class),
                                    f.get(8, Integer.class),
                                    paciente,
                                    f.get(6, LocalDateTime.class));
                    return new Oferta(
                            f.get(0, Integer.class),
                            f.get(1, Integer.class),
                            f.get(2, String.class),
                            f.get(3, LocalDateTime.class),
                            f.get(4, Integer.class),
                            cupo);
                });
    }

    public List<Integer> canceladasFuturas(LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var c = q.from(CitaEntidad.class);
                    var d = c.join("disponibilidad");
                    q.select(c.get("id"))
                            .where(
                                    cb.equal(c.get("estado"), "CANCELADA"),
                                    cb.greaterThan(c.get("inicio"), ahora),
                                    cb.equal(d.get("estado"), "PUBLICADO"))
                            .orderBy(cb.asc(c.get("inicio")), cb.asc(c.get("id")));
                    return s.createQuery(q).getResultList();
                });
    }

    public List<Integer> medicosPendientes() throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var r = q.from(OfertaEntidad.class);
                    var c = r.join("cita");
                    q.select(c.get("medico").get("id"))
                            .distinct(true)
                            .where(cb.equal(r.get("estado"), "PENDIENTE"))
                            .orderBy(cb.asc(c.get("medico").get("id")));
                    return s.createQuery(q).getResultList();
                });
    }

    public Presentacion vigente(int paciente, LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Presentacion.class);
                    var r = q.from(OfertaEntidad.class);
                    var l = r.join("lista");
                    var c = r.join("cita");
                    var d = r.join("disponibilidad");
                    var m = c.join("medico");
                    var u = m.join("usuario");
                    var e = m.join("especialidad");
                    var sede = m.join("sede");
                    q.select(
                                    cb.construct(
                                            Presentacion.class,
                                            r.get("id"),
                                            l.get("id"),
                                            r.get("vence"),
                                            c.get("inicio"),
                                            u.get("nombres"),
                                            u.get("apellidos"),
                                            e.get("nombre"),
                                            sede.get("nombre")))
                            .where(
                                    cb.equal(l.get("paciente").get("id"), paciente),
                                    cb.equal(l.get("estado"), "OFERTADA"),
                                    cb.equal(r.get("estado"), "PENDIENTE"),
                                    cb.greaterThan(r.get("vence"), ahora),
                                    cb.greaterThan(c.get("inicio"), ahora),
                                    cb.equal(c.get("estado"), "CANCELADA"),
                                    cb.equal(d.get("estado"), "PUBLICADO"),
                                    cb.equal(d.get("sede"), sede.get("id")),
                                    cb.equal(m.get("estado"), "ACTIVO"),
                                    cb.isTrue(u.get("activo")),
                                    cb.equal(e.get("estado"), "ACTIVO"),
                                    cb.equal(sede.get("estado"), "ACTIVO"))
                            .orderBy(cb.desc(r.get("id")));
                    return s.createQuery(q).setMaxResults(1).getResultList().stream()
                            .findFirst()
                            .orElse(null);
                });
    }

    public Ultima ultima(int paciente) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Ultima.class);
                    var r = q.from(OfertaEntidad.class);
                    q.select(cb.construct(Ultima.class, r.get("estado"), r.get("vence")))
                            .where(cb.equal(r.get("lista").get("paciente").get("id"), paciente))
                            .orderBy(cb.desc(r.get("id")));
                    return s.createQuery(q).setMaxResults(1).getResultList().stream()
                            .findFirst()
                            .orElse(null);
                });
    }

    public int candidato(Cupo cupo, LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var l = q.from(ListaEsperaEntidad.class);
                    var p = l.join("paciente");
                    var u = p.join("usuario");
                    var anteriores = q.subquery(Integer.class);
                    var r = anteriores.from(OfertaEntidad.class);
                    anteriores
                            .select(cb.literal(1))
                            .where(
                                    cb.equal(r.get("cita").get("id"), cupo.cita()),
                                    cb.equal(r.get("lista").get("id"), l.get("id")),
                                    r.get("estado").in("RECHAZADA", "EXPIRADA", "ACEPTADA"));
                    var citas = q.subquery(Integer.class);
                    var c = citas.from(CitaEntidad.class);
                    citas.select(cb.literal(1))
                            .where(
                                    cb.equal(c.get("paciente"), p.get("id")),
                                    cb.equal(c.get("inicio"), cupo.inicio()),
                                    cb.not(c.get("estado").in("CANCELADA", "REPROGRAMADA")));
                    String turno =
                            cupo.inicio().getHour() < 13
                                    ? "MANANA"
                                    : cupo.inicio().getHour() < 19 ? "TARDE" : "NOCHE";
                    long minutos =
                            Duration.between(
                                            ahora.atZone(ZoneId.of("America/Lima")),
                                            cupo.inicio().atZone(ZoneId.of("America/Lima")))
                                    .toMinutes();
                    var nivel =
                            cb.coalesce(
                                    PrioridadClinicaExpression.nivel(
                                            cb,
                                            q,
                                            p.get("id"),
                                            l.get("especialidad").get("id"),
                                            cupo.inicio().toLocalDate()),
                                    "MEDIA");
                    q.select(l.get("id"))
                            .where(
                                    cb.equal(l.get("estado"), "ACTIVA"),
                                    cb.isTrue(u.get("activo")),
                                    cb.equal(u.get("rol"), "PACIENTE"),
                                    cb.equal(l.get("especialidad").get("id"), cupo.especialidad()),
                                    cb.or(
                                            cb.equal(l.get("sede").get("id"), cupo.sede()),
                                            cb.isNull(l.get("sede"))),
                                    l.get("horario").in(turno, "INDIFERENTE"),
                                    cb.notEqual(p.get("id"), cupo.paciente()),
                                    cb.not(cb.exists(anteriores)),
                                    cb.not(cb.exists(citas)),
                                    cb.lessThanOrEqualTo(l.get("anticipacion"), minutos),
                                    cb.or(
                                            cb.isNull(l.get("medico")),
                                            cb.equal(l.get("medico"), cupo.medico())))
                            .orderBy(
                                    cb.asc(
                                            cb.<Integer>selectCase()
                                                    .when(cb.equal(nivel, "ALTA"), 1)
                                                    .when(cb.equal(nivel, "MEDIA"), 2)
                                                    .otherwise(3)),
                                    cb.asc(l.get("registro")),
                                    cb.asc(l.get("id")));
                    return s
                            .createQuery(q)
                            .setMaxResults(1)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .getResultList()
                            .stream()
                            .findFirst()
                            .orElse(0);
                });
    }

    public int cambiarLista(int lista, String estado) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(ListaEsperaEntidad.class);
                    var l = q.from(ListaEsperaEntidad.class);
                    q.set("estado", estado).where(cb.equal(l.get("id"), lista));
                    return s.createMutationQuery(q).executeUpdate();
                });
    }

    public int actualizar(int id, String estado, Integer cita) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(OfertaEntidad.class);
                    var r = q.from(OfertaEntidad.class);
                    q.set("estado", estado)
                            .set(r.<Integer>get("asignada"), cita)
                            .where(
                                    cb.equal(r.get("id"), id),
                                    cb.equal(r.get("estado"), "PENDIENTE"));
                    return s.createMutationQuery(q).executeUpdate();
                });
    }

    public Integer usuarioLista(int lista) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var l = q.from(ListaEsperaEntidad.class);
                    q.select(l.get("paciente").get("usuario").get("id"))
                            .where(cb.equal(l.get("id"), lista));
                    return s.createQuery(q).getResultList().stream().findFirst().orElse(null);
                });
    }

    public void normalizar(int medico, LocalDateTime ahora) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var expirar = cb.createCriteriaUpdate(OfertaEntidad.class);
                    var r = expirar.from(OfertaEntidad.class);
                    var citas = expirar.subquery(Integer.class);
                    var c = citas.from(CitaEntidad.class);
                    citas.select(c.get("id")).where(cb.equal(c.get("medico").get("id"), medico));
                    var pasadas = expirar.subquery(Integer.class);
                    var ci = pasadas.from(CitaEntidad.class);
                    pasadas.select(ci.get("id"))
                            .where(cb.lessThanOrEqualTo(ci.get("inicio"), ahora));
                    expirar.set("estado", "EXPIRADA")
                            .where(
                                    cb.equal(r.get("estado"), "PENDIENTE"),
                                    r.get("cita").get("id").in(citas),
                                    cb.or(
                                            cb.lessThanOrEqualTo(r.get("vence"), ahora),
                                            r.get("cita").get("id").in(pasadas)));
                    s.createMutationQuery(expirar).executeUpdate();

                    var cancelar = cb.createCriteriaUpdate(OfertaEntidad.class);
                    var o = cancelar.from(OfertaEntidad.class);
                    var delMedico = cancelar.subquery(Integer.class);
                    var cm = delMedico.from(CitaEntidad.class);
                    delMedico
                            .select(cm.get("id"))
                            .where(cb.equal(cm.get("medico").get("id"), medico));
                    var noCanceladas = cancelar.subquery(Integer.class);
                    var nc = noCanceladas.from(CitaEntidad.class);
                    noCanceladas
                            .select(nc.get("id"))
                            .where(cb.notEqual(nc.get("estado"), "CANCELADA"));
                    var noPublicadas = cancelar.subquery(Integer.class);
                    var np = noPublicadas.from(DisponibilidadEntidad.class);
                    noPublicadas
                            .select(np.get("id"))
                            .where(cb.notEqual(np.get("estado"), "PUBLICADO"));
                    var listas = cancelar.subquery(Integer.class);
                    var l = listas.from(ListaEsperaEntidad.class);
                    var paciente = l.join("paciente").join("usuario");
                    listas.select(l.get("id"))
                            .where(
                                    cb.or(
                                            cb.notEqual(l.get("estado"), "OFERTADA"),
                                            cb.isFalse(paciente.get("activo"))));
                    var invalidas = cancelar.subquery(Integer.class);
                    var ic = invalidas.from(CitaEntidad.class);
                    var m = ic.join("medico");
                    var u = m.join("usuario");
                    var se = m.join("sede");
                    var e = m.join("especialidad");
                    var d = ic.join("disponibilidad");
                    invalidas
                            .select(ic.get("id"))
                            .where(
                                    cb.or(
                                            cb.notEqual(m.get("estado"), "ACTIVO"),
                                            cb.isFalse(u.get("activo")),
                                            cb.notEqual(se.get("estado"), "ACTIVO"),
                                            cb.notEqual(e.get("estado"), "ACTIVO"),
                                            cb.notEqual(d.get("sede"), se.get("id"))));
                    cancelar.set("estado", "CANCELADA")
                            .where(
                                    cb.equal(o.get("estado"), "PENDIENTE"),
                                    o.get("cita").get("id").in(delMedico),
                                    cb.or(
                                            o.get("cita").get("id").in(noCanceladas),
                                            o.get("disponibilidad").get("id").in(noPublicadas),
                                            o.get("lista").get("id").in(listas),
                                            o.get("cita").get("id").in(invalidas)));
                    s.createMutationQuery(cancelar).executeUpdate();

                    var reactivar = cb.createCriteriaUpdate(ListaEsperaEntidad.class);
                    var le = reactivar.from(ListaEsperaEntidad.class);
                    var relacionadas = reactivar.subquery(Integer.class);
                    var oferta = relacionadas.from(OfertaEntidad.class);
                    relacionadas
                            .select(oferta.get("lista").get("id"))
                            .where(cb.equal(oferta.get("cita").get("medico").get("id"), medico));
                    var pendientes = reactivar.subquery(Integer.class);
                    var pendiente = pendientes.from(OfertaEntidad.class);
                    pendientes
                            .select(cb.literal(1))
                            .where(
                                    cb.equal(pendiente.get("lista").get("id"), le.get("id")),
                                    cb.equal(pendiente.get("estado"), "PENDIENTE"),
                                    cb.greaterThan(pendiente.get("vence"), ahora));
                    reactivar
                            .set("estado", "ACTIVA")
                            .where(
                                    cb.equal(le.get("estado"), "OFERTADA"),
                                    le.get("id").in(relacionadas),
                                    cb.not(cb.exists(pendientes)));
                    s.createMutationQuery(reactivar).executeUpdate();
                    return null;
                });
    }
}
