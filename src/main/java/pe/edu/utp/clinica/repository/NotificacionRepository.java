package pe.edu.utp.clinica.repository;

import jakarta.persistence.criteria.JoinType;

import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public final class NotificacionRepository {
    public record Aviso(
            int id,
            Integer cita,
            String tipo,
            String mensaje,
            LocalDateTime enviada,
            boolean leida,
            String estadoCita,
            LocalDateTime fechaCita,
            LocalDateTime vence,
            LocalDateTime confirmada) {}

    public record Referencia(int cita, String tipo) {}

    private final Connection conexion;

    public NotificacionRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public void crear(int usuario, Integer cita, String tipo, String mensaje, LocalDateTime fecha)
            throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    s.persist(new NotificacionEntidad(usuario, cita, tipo, mensaje, fecha));
                    return null;
                });
    }

    public List<Aviso> listar(int usuario) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Aviso.class);
                    var n = q.from(NotificacionEntidad.class);
                    var c = n.join("citaRelacion", JoinType.LEFT);
                    var pacientes = q.subquery(Integer.class);
                    var p = pacientes.from(PacienteEntidad.class);
                    pacientes
                            .select(p.get("id"))
                            .where(cb.equal(p.get("usuario").get("id"), usuario));
                    c.on(c.get("paciente").in(pacientes));
                    var a = c.join("confirmacion", JoinType.LEFT);
                    q.select(
                                    cb.construct(
                                            Aviso.class,
                                            n.get("id"),
                                            n.get("cita"),
                                            n.get("tipo"),
                                            n.get("mensaje"),
                                            n.get("fecha"),
                                            n.get("leida"),
                                            c.get("estado"),
                                            c.get("inicio"),
                                            a.get("vence"),
                                            a.get("confirmada")))
                            .where(cb.equal(n.get("usuario"), usuario))
                            .orderBy(cb.desc(n.get("fecha")), cb.desc(n.get("id")));
                    return s.createQuery(q).getResultList();
                });
    }

    public int pendientes(int usuario) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Long.class);
                    var n = q.from(NotificacionEntidad.class);
                    q.select(cb.count(n))
                            .where(cb.equal(n.get("usuario"), usuario), cb.isFalse(n.get("leida")));
                    return s.createQuery(q).getSingleResult().intValue();
                });
    }

    public int marcar(Integer id, int usuario) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(NotificacionEntidad.class);
                    var n = q.from(NotificacionEntidad.class);
                    q.set("leida", true)
                            .set("estadoEnvio", "LEIDA")
                            .where(
                                    cb.equal(n.get("usuario"), usuario),
                                    id == null
                                            ? cb.isFalse(n.get("leida"))
                                            : cb.equal(n.get("id"), id));
                    return s.createMutationQuery(q).executeUpdate();
                });
    }

    public Referencia propia(int id, int usuario) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Referencia.class);
                    var n = q.from(NotificacionEntidad.class);
                    var c = n.join("citaRelacion");
                    var p = c.join("pacienteRelacion");
                    q.select(cb.construct(Referencia.class, c.get("id"), n.get("tipo")))
                            .where(
                                    cb.equal(n.get("id"), id),
                                    cb.equal(n.get("usuario"), usuario),
                                    cb.equal(p.get("usuario").get("id"), usuario));
                    return s.createQuery(q).getResultList().stream().findFirst().orElse(null);
                });
    }
}
