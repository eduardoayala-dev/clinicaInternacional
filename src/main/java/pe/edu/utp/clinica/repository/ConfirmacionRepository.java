package pe.edu.utp.clinica.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.*;

import pe.edu.utp.clinica.dao.ConfirmacionAsistenciaDAO.EstadoAsistencia;
import pe.edu.utp.clinica.dao.ConfirmacionAsistenciaDAO.Seguimiento;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public final class ConfirmacionRepository {
    public record Cita(
            String estado,
            LocalDateTime fechaHora,
            LocalDateTime venceEn,
            boolean confirmada,
            int sede) {}

    public record Conteo(long total, long vencidas) {}

    private final Connection conexion;

    public ConfirmacionRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public Map<Integer, EstadoAsistencia> estados(
            int usuario, Set<Integer> citas, LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var c = q.from(CitaEntidad.class);
                    var a = c.join("confirmacion");
                    q.multiselect(c.get("id"), a.get("confirmada"), a.get("vence"))
                            .where(
                                    cb.equal(
                                            c.get("pacienteRelacion").get("usuario").get("id"),
                                            usuario),
                                    c.get("id").in(citas));
                    var resultado = new HashMap<Integer, EstadoAsistencia>();
                    for (var f : s.createQuery(q).getResultList()) {
                        var confirmada = f.get(1, LocalDateTime.class);
                        var vence = f.get(2, LocalDateTime.class);
                        resultado.put(
                                f.get(0, Integer.class),
                                new EstadoAsistencia(
                                        confirmada != null,
                                        confirmada == null && !ahora.isBefore(vence),
                                        pe.edu.utp.clinica.util.FormatoClinico.fechaHora(vence)));
                    }
                    return Map.copyOf(resultado);
                });
    }

    public Integer pacientePropio(int usuario) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var p = q.from(PacienteEntidad.class);
                    var u = p.join("usuario");
                    q.select(p.get("id"))
                            .where(
                                    cb.equal(u.get("id"), usuario),
                                    cb.isTrue(u.get("activo")),
                                    cb.equal(u.get("rol"), "PACIENTE"));
                    return s.createQuery(q).getResultList().stream().findFirst().orElse(null);
                });
    }

    public Integer pacienteEnSede(int cita, int sede) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var c = q.from(CitaEntidad.class);
                    q.select(c.get("paciente"))
                            .where(
                                    cb.equal(c.get("id"), cita),
                                    cb.equal(c.get("medico").get("sede").get("id"), sede));
                    return s.createQuery(q).getResultList().stream().findFirst().orElse(null);
                });
    }

    public Cita bloquear(int cita, Integer paciente, Integer sede) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var c = q.from(CitaEntidad.class);
                    var m = c.join("medico");
                    var a = c.join("confirmacion");
                    var filtros = new ArrayList<Predicate>();
                    filtros.add(cb.equal(c.get("id"), cita));
                    if (paciente != null) filtros.add(cb.equal(c.get("paciente"), paciente));
                    if (sede != null) filtros.add(cb.equal(m.get("sede").get("id"), sede));
                    q.multiselect(
                                    c.get("estado"),
                                    c.get("inicio"),
                                    a.get("vence"),
                                    a.get("confirmada"),
                                    m.get("sede").get("id"))
                            .where(filtros.toArray(Predicate[]::new));
                    var filas =
                            s.createQuery(q)
                                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                                    .getResultList();
                    if (filas.isEmpty()) return null;
                    var f = filas.get(0);
                    return new Cita(
                            f.get(0, String.class),
                            f.get(1, LocalDateTime.class),
                            f.get(2, LocalDateTime.class),
                            f.get(3) != null,
                            f.get(4, Integer.class));
                });
    }

    public int confirmar(int cita, int usuario, String canal, LocalDateTime ahora)
            throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(ConfirmacionEntidad.class);
                    var a = q.from(ConfirmacionEntidad.class);
                    q.set("confirmada", ahora)
                            .set("canal", canal)
                            .set("usuario", usuario)
                            .where(cb.equal(a.get("id"), cita), cb.isNull(a.get("confirmada")));
                    return s.createMutationQuery(q).executeUpdate();
                });
    }

    public void contacto(int cita, int usuario, String canal, String resultado, LocalDateTime ahora)
            throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    s.persist(new ContactoAdmisionEntidad(cita, usuario, ahora, canal, resultado));
                    return null;
                });
    }

    public int intentos(int cita) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Long.class);
                    var c = q.from(ContactoAdmisionEntidad.class);
                    q.select(cb.count(c))
                            .where(
                                    cb.equal(c.get("cita"), cita),
                                    c.get("resultado").in("SIN_RESPUESTA", "NO_CONFIRMA"));
                    return s.createQuery(q).getSingleResult().intValue();
                });
    }

    private static Predicate[] pendientes(
            CriteriaBuilder cb, Root<CitaEntidad> c, Join<?, ?> a, int sede, LocalDateTime ahora) {
        return new Predicate[] {
            cb.equal(c.get("medico").get("sede").get("id"), sede),
            c.get("estado").in("SOLICITADA", "CONFIRMADA"),
            cb.greaterThan(c.get("inicio"), ahora),
            cb.isNull(a.get("confirmada"))
        };
    }

    public Conteo contar(int sede, LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var c = q.from(CitaEntidad.class);
                    var a = c.join("confirmacion", JoinType.LEFT);
                    q.multiselect(
                                    cb.count(c),
                                    cb.sum(
                                            cb.<Long>selectCase()
                                                    .when(
                                                            cb.lessThanOrEqualTo(
                                                                    a.get("vence"), ahora),
                                                            1L)
                                                    .otherwise(0L)))
                            .where(pendientes(cb, c, a, sede, ahora));
                    var f = s.createQuery(q).getSingleResult();
                    return new Conteo(
                            ((Number) f.get(0)).longValue(),
                            f.get(1) == null ? 0 : ((Number) f.get(1)).longValue());
                });
    }

    public List<Seguimiento> pendientes(int sede, LocalDateTime ahora, int limite, int offset)
            throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var c = q.from(CitaEntidad.class);
                    var p = c.join("pacienteRelacion");
                    var u = p.join("usuario");
                    var a = c.join("confirmacion", JoinType.LEFT);
                    q.multiselect(
                                    c.get("id"),
                                    u.get("nombres"),
                                    u.get("apellidos"),
                                    p.get("dni"),
                                    u.get("email"),
                                    p.get("telefono"),
                                    c.get("inicio"),
                                    c.get("estado"),
                                    a.get("vence"))
                            .where(pendientes(cb, c, a, sede, ahora))
                            .orderBy(
                                    cb.asc(a.get("vence")),
                                    cb.asc(c.get("inicio")),
                                    cb.asc(c.get("id")));
                    var filas =
                            s.createQuery(q)
                                    .setMaxResults(limite)
                                    .setFirstResult(offset)
                                    .getResultList();
                    if (filas.isEmpty()) return List.of();
                    var ids = filas.stream().map(f -> f.get(0, Integer.class)).toList();
                    var contactos = cb.createTupleQuery();
                    var co = contactos.from(ContactoAdmisionEntidad.class);
                    contactos
                            .multiselect(co.get("cita"), co.get("fecha"), co.get("resultado"))
                            .where(co.get("cita").in(ids))
                            .orderBy(cb.desc(co.get("fecha")), cb.desc(co.get("id")));
                    var cantidades = new HashMap<Integer, Integer>();
                    var ultimos = new HashMap<Integer, jakarta.persistence.Tuple>();
                    for (var f : s.createQuery(contactos).getResultList()) {
                        int id = f.get(0, Integer.class);
                        cantidades.merge(id, 1, Integer::sum);
                        ultimos.putIfAbsent(id, f);
                    }
                    var resultado = new ArrayList<Seguimiento>();
                    for (var f : filas) {
                        int id = f.get(0, Integer.class);
                        var ultimo = ultimos.get(id);
                        var vence = f.get(8, LocalDateTime.class);
                        resultado.add(
                                new Seguimiento(
                                        id,
                                        f.get(1, String.class) + " " + f.get(2, String.class),
                                        f.get(3, String.class),
                                        f.get(4, String.class),
                                        f.get(5, String.class),
                                        f.get(6, LocalDateTime.class).toString(),
                                        f.get(7, String.class),
                                        cantidades.getOrDefault(id, 0),
                                        ultimo == null || ultimo.get(1) == null
                                                ? null
                                                : ultimo.get(1, LocalDateTime.class).toString(),
                                        ultimo == null ? null : ultimo.get(2, String.class),
                                        vence != null && !ahora.isBefore(vence)));
                    }
                    return List.copyOf(resultado);
                });
    }
}
