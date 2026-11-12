package pe.edu.utp.clinica.repository;

import jakarta.persistence.criteria.JoinType;

import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class InicioOperativoRepository {
    public record Conteo(int citas, int canceladas) {}

    public record Operacion(
            LocalDateTime fecha,
            String tipo,
            String mensaje,
            int cita,
            LocalDateTime programada,
            String paciente) {}

    private final Connection conexion;

    public InicioOperativoRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public Conteo contar(int sede, LocalDate fecha) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var c = q.from(CitaEntidad.class);
                    var m = c.join("medico");
                    var d = c.join("disponibilidad", JoinType.LEFT);
                    q.multiselect(
                                    cb.count(c),
                                    cb.sum(
                                            cb.<Long>selectCase()
                                                    .when(
                                                            cb.equal(c.get("estado"), "CANCELADA"),
                                                            1L)
                                                    .otherwise(0L)))
                            .where(
                                    cb.equal(
                                            cb.coalesce(d.get("sede"), m.get("sede").get("id")),
                                            sede),
                                    cb.greaterThanOrEqualTo(c.get("inicio"), fecha.atStartOfDay()),
                                    cb.lessThan(c.get("inicio"), fecha.plusDays(1).atStartOfDay()));
                    var f = s.createQuery(q).getSingleResult();
                    return new Conteo(
                            ((Number) f.get(0)).intValue(),
                            f.get(1) == null ? 0 : ((Number) f.get(1)).intValue());
                });
    }

    public int ofertas(int sede, LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Long.class);
                    var o = q.from(OfertaEntidad.class);
                    var d = o.join("disponibilidad");
                    var c = o.join("cita");
                    q.select(cb.count(o))
                            .where(
                                    cb.equal(d.get("sede"), sede),
                                    cb.equal(o.get("estado"), "PENDIENTE"),
                                    cb.greaterThan(o.get("vence"), ahora),
                                    cb.greaterThan(c.get("inicio"), ahora),
                                    cb.equal(c.get("estado"), "CANCELADA"),
                                    cb.equal(d.get("estado"), "PUBLICADO"));
                    return s.createQuery(q).getSingleResult().intValue();
                });
    }

    public List<Operacion> operaciones(int sede) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Operacion.class);
                    var n = q.from(NotificacionEntidad.class);
                    var c = n.join("citaRelacion");
                    var p = c.join("pacienteRelacion");
                    var u = p.join("usuario");
                    var m = c.join("medico");
                    var d = c.join("disponibilidad", JoinType.LEFT);
                    q.select(
                                    cb.construct(
                                            Operacion.class,
                                            n.get("fecha"),
                                            n.get("tipo"),
                                            n.get("mensaje"),
                                            c.get("id"),
                                            c.get("inicio"),
                                            cb.concat(
                                                    cb.concat(u.get("nombres"), " "),
                                                    u.get("apellidos"))))
                            .where(
                                    cb.equal(
                                            cb.coalesce(d.get("sede"), m.get("sede").get("id")),
                                            sede))
                            .orderBy(cb.desc(n.get("fecha")), cb.desc(n.get("id")));
                    return s.createQuery(q).setMaxResults(5).getResultList();
                });
    }
}
