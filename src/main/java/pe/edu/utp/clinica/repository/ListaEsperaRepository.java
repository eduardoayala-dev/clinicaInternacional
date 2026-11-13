package pe.edu.utp.clinica.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.JoinType;

import pe.edu.utp.clinica.dao.ListaEsperaDAO.Preferencias;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;

public final class ListaEsperaRepository {
    public record Estado(
            int id,
            String horario,
            String canales,
            int anticipacion,
            String especialidad,
            String sede,
            String estado,
            long posicion,
            int especialidadId) {}

    private final Connection conexion;

    public ListaEsperaRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public int crear(int paciente, Preferencias preferencias, LocalDateTime ahora)
            throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var lista =
                            new ListaEsperaEntidad(
                                    s.getReference(PacienteEntidad.class, paciente),
                                    s.getReference(
                                            EspecialidadEntidad.class, preferencias.especialidad()),
                                    preferencias.sede() == null
                                            ? null
                                            : s.getReference(
                                                    SedeEntidad.class, preferencias.sede()),
                                    preferencias.urgencia(),
                                    preferencias.horario(),
                                    preferencias.canales(),
                                    ahora,
                                    preferencias.anticipacion(),
                                    preferencias.medico());
                    s.persist(lista);
                    return lista.id();
                });
    }

    public String bloquearPropia(int lista, int paciente) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(String.class);
                    var l = q.from(ListaEsperaEntidad.class);
                    q.select(l.get("estado"))
                            .where(
                                    cb.equal(l.get("id"), lista),
                                    cb.equal(l.get("paciente").get("id"), paciente));
                    return s
                            .createQuery(q)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .getResultList()
                            .stream()
                            .findFirst()
                            .orElse(null);
                });
    }

    public boolean catalogoActivo(int id, boolean sede) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var e = sede ? q.from(SedeEntidad.class) : q.from(EspecialidadEntidad.class);
                    q.select(e.get("id"))
                            .where(cb.equal(e.get("id"), id), cb.equal(e.get("estado"), "ACTIVO"));
                    return !s.createQuery(q).getResultList().isEmpty();
                });
    }

    public int actualIgual(int paciente, Preferencias preferencias) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var l = q.from(ListaEsperaEntidad.class);
                    q.multiselect(
                                    l.get("id"),
                                    l.get("especialidad").get("id"),
                                    l.get("sede").get("id"),
                                    l.get("horario"),
                                    l.get("canales"),
                                    l.get("anticipacion"),
                                    l.get("medico"))
                            .where(
                                    cb.equal(l.get("paciente").get("id"), paciente),
                                    l.get("estado").in("ACTIVA", "OFERTADA"));
                    for (var f :
                            s.createQuery(q)
                                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                                    .getResultList()) {
                        if (f.get(1, Integer.class) == preferencias.especialidad()
                                && Objects.equals(f.get(2), preferencias.sede())
                                && preferencias.horario().equals(f.get(3))
                                && preferencias.canales().equals(f.get(4))
                                && f.get(5, Integer.class) == preferencias.anticipacion()
                                && Objects.equals(f.get(6), preferencias.medico()))
                            return f.get(0, Integer.class);
                    }
                    return 0;
                });
    }

    public void cancelarInscripciones(int paciente) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var ofertas = cb.createCriteriaUpdate(OfertaEntidad.class);
                    var o = ofertas.from(OfertaEntidad.class);
                    var listas = ofertas.subquery(Integer.class);
                    var l = listas.from(ListaEsperaEntidad.class);
                    listas.select(l.get("id"))
                            .where(cb.equal(l.get("paciente").get("id"), paciente));
                    ofertas.set("estado", "CANCELADA")
                            .where(
                                    cb.equal(o.get("estado"), "PENDIENTE"),
                                    o.get("lista").get("id").in(listas));
                    s.createMutationQuery(ofertas).executeUpdate();
                    var q = cb.createCriteriaUpdate(ListaEsperaEntidad.class);
                    var lista = q.from(ListaEsperaEntidad.class);
                    q.set("estado", "CANCELADA")
                            .where(
                                    cb.equal(lista.get("paciente").get("id"), paciente),
                                    lista.get("estado").in("ACTIVA", "OFERTADA"));
                    s.createMutationQuery(q).executeUpdate();
                    return null;
                });
    }

    public void cancelarOfertas(int lista) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(OfertaEntidad.class);
                    var o = q.from(OfertaEntidad.class);
                    q.set("estado", "CANCELADA")
                            .where(
                                    cb.equal(o.get("lista").get("id"), lista),
                                    cb.equal(o.get("estado"), "PENDIENTE"));
                    s.createMutationQuery(q).executeUpdate();
                    return null;
                });
    }

    public Estado estado(int paciente, LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Estado.class);
                    var l = q.from(ListaEsperaEntidad.class);
                    var e = l.join("especialidad");
                    var sede = l.join("sede", JoinType.LEFT);
                    var ofertas = q.subquery(Integer.class);
                    var o = ofertas.from(OfertaEntidad.class);
                    ofertas.select(cb.literal(1))
                            .where(
                                    cb.equal(o.get("lista").get("id"), l.get("id")),
                                    cb.equal(o.get("estado"), "PENDIENTE"),
                                    cb.greaterThan(o.get("vence"), ahora));
                    var posicion = q.subquery(Long.class);
                    var otra = posicion.from(ListaEsperaEntidad.class);
                    posicion.select(cb.count(otra))
                            .where(
                                    cb.equal(otra.get("especialidad").get("id"), e.get("id")),
                                    cb.or(
                                            cb.equal(otra.get("sede").get("id"), sede.get("id")),
                                            cb.isNull(otra.get("sede")),
                                            cb.isNull(l.get("sede"))),
                                    otra.get("estado").in("ACTIVA", "OFERTADA"),
                                    cb.lessThanOrEqualTo(otra.get("id"), l.get("id")));
                    q.select(
                                    cb.construct(
                                            Estado.class,
                                            l.get("id"),
                                            l.get("horario"),
                                            l.get("canales"),
                                            l.get("anticipacion"),
                                            e.get("nombre"),
                                            sede.get("nombre"),
                                            cb.<String>selectCase()
                                                    .when(cb.exists(ofertas), "OFERTADA")
                                                    .otherwise("ACTIVA"),
                                            posicion,
                                            e.get("id")))
                            .where(
                                    cb.equal(l.get("paciente").get("id"), paciente),
                                    l.get("estado").in("ACTIVA", "OFERTADA"))
                            .orderBy(cb.desc(l.get("id")));
                    return s.createQuery(q).setMaxResults(1).getResultList().stream()
                            .findFirst()
                            .orElse(null);
                });
    }
}
