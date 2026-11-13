package pe.edu.utp.clinica.repository;

import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public final class ProgramacionCorreosRepository {
    private final Connection conexion;

    public ProgramacionCorreosRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public int programar(LocalDateTime ahora, LocalDateTime utc, Integer horas)
            throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var seleccionadas =
                            s.createQuery(consulta(s, ahora, horas, null))
                                    .setMaxResults(100)
                                    .getResultList();
                    if (seleccionadas.isEmpty()) return 0;
                    var bloqueo = cb.createQuery(CitaBloqueoEntidad.class);
                    var bloqueada = bloqueo.from(CitaBloqueoEntidad.class);
                    bloqueo.select(bloqueada)
                            .where(bloqueada.get("id").in(seleccionadas))
                            .orderBy(cb.asc(bloqueada.get("id")));
                    s.createQuery(bloqueo)
                            .setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
                            .getResultList();
                    var ids =
                            s.createQuery(consulta(s, ahora, horas, seleccionadas)).getResultList();
                    for (int id : ids) {
                        if (horas != null) s.persist(new CorreoRecordatorioEntidad(id, utc));
                        else
                            s.persist(
                                    new CorreoCambioEntidad(id, null, "CONFIRMACION_VENCIDA", utc));
                    }
                    s.flush();
                    return ids.size();
                });
    }

    private static jakarta.persistence.criteria.CriteriaQuery<Integer> consulta(
            org.hibernate.Session s,
            LocalDateTime ahora,
            Integer horas,
            java.util.List<Integer> restringidas) {
        var cb = s.getCriteriaBuilder();
        var q = cb.createQuery(Integer.class);
        var c = q.from(CitaEntidad.class);
        var a = c.join("confirmacion");
        var u = c.join("pacienteRelacion").join("usuario");
        var filtros = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
        filtros.add(cb.greaterThan(c.get("inicio"), ahora));
        filtros.add(cb.isNull(a.get("confirmada")));
        filtros.add(cb.isTrue(u.get("activo")));
        filtros.add(cb.equal(u.get("rol"), "PACIENTE"));
        var anteriores = q.subquery(Integer.class);
        if (horas != null) {
            filtros.add(cb.equal(c.get("estado"), "CONFIRMADA"));
            filtros.add(cb.lessThanOrEqualTo(c.get("inicio"), ahora.plusHours(horas)));
            filtros.add(cb.greaterThan(a.get("vence"), ahora));
            var r = anteriores.from(CorreoRecordatorioEntidad.class);
            anteriores.select(cb.literal(1)).where(cb.equal(r.get("id"), c.get("id")));
        } else {
            filtros.add(c.get("estado").in("SOLICITADA", "CONFIRMADA"));
            filtros.add(cb.lessThanOrEqualTo(a.get("vence"), ahora));
            var r = anteriores.from(CorreoCambioEntidad.class);
            anteriores
                    .select(cb.literal(1))
                    .where(
                            cb.equal(r.get("cita"), c.get("id")),
                            cb.equal(r.get("tipo"), "CONFIRMACION_VENCIDA"));
        }
        filtros.add(cb.not(cb.exists(anteriores)));
        if (restringidas != null) filtros.add(c.get("id").in(restringidas));
        q.select(c.get("id"))
                .where(filtros.toArray(jakarta.persistence.criteria.Predicate[]::new))
                .orderBy(
                        cb.asc(horas != null ? c.get("inicio") : a.get("vence")),
                        cb.asc(c.get("id")));
        return q;
    }

    public boolean recordatorioPendiente(int cita, LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var a = q.from(ConfirmacionEntidad.class);
                    q.select(a.get("id"))
                            .where(
                                    cb.equal(a.get("id"), cita),
                                    cb.isNull(a.get("confirmada")),
                                    cb.greaterThan(a.get("vence"), ahora));
                    return !s.createQuery(q).getResultList().isEmpty();
                });
    }
}
