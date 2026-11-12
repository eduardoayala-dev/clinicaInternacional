package pe.edu.utp.clinica.repository;

import jakarta.persistence.LockModeType;

import pe.edu.utp.clinica.model.medico.PropuestaHorario;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.util.List;

public final class PropuestaHorarioRepository {
    private final Connection conexion;

    public PropuestaHorarioRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public int crear(
            int medico,
            int sede,
            int dias,
            LocalDate desde,
            LocalDate hasta,
            LocalTime inicio,
            LocalTime fin,
            int minutos,
            LocalDateTime ahora)
            throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var p =
                            new PropuestaHorarioEntidad(
                                    s.getReference(MedicoEntidad.class, medico),
                                    s.getReference(SedeEntidad.class, sede),
                                    dias,
                                    desde,
                                    hasta,
                                    inicio,
                                    fin,
                                    minutos,
                                    ahora);
                    s.persist(p);
                    return p.id();
                });
    }

    public List<PropuestaHorario> listar(Integer medico, Integer sede, Integer id)
            throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(PropuestaHorario.class);
                    var p = q.from(PropuestaHorarioEntidad.class);
                    var m = p.join("medico");
                    var u = m.join("usuario");
                    var se = p.join("sede");
                    var filtros = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                    if (medico != null) filtros.add(cb.equal(m.get("id"), medico));
                    if (sede != null) filtros.add(cb.equal(se.get("id"), sede));
                    if (id != null) filtros.add(cb.equal(p.get("id"), id));
                    q.select(
                                    cb.construct(
                                            PropuestaHorario.class,
                                            p.get("id"),
                                            m.get("id"),
                                            se.get("id"),
                                            cb.concat(
                                                    cb.concat(u.get("nombres"), " "),
                                                    u.get("apellidos")),
                                            se.get("nombre"),
                                            p.get("dias"),
                                            p.get("desde"),
                                            p.get("hasta"),
                                            p.get("inicio"),
                                            p.get("fin"),
                                            p.get("minutos"),
                                            p.get("estado"),
                                            p.get("motivo")))
                            .where(filtros.toArray(jakarta.persistence.criteria.Predicate[]::new));
                    if (medico != null) q.orderBy(cb.desc(p.get("id")));
                    else
                        q.orderBy(
                                cb.asc(
                                        cb.<Integer>selectCase()
                                                .when(cb.equal(p.get("estado"), "PENDIENTE"), 0)
                                                .otherwise(1)),
                                cb.desc(p.get("id")));
                    var consulta = s.createQuery(q).setMaxResults(100);
                    if (id != null) consulta.setLockMode(LockModeType.PESSIMISTIC_WRITE);
                    return consulta.getResultList();
                });
    }

    public void revisar(int id, int usuario, boolean aprobar, String motivo, LocalDateTime ahora)
            throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(PropuestaHorarioEntidad.class);
                    var p = q.from(PropuestaHorarioEntidad.class);
                    q.set("estado", aprobar ? "PUBLICADO" : "RECHAZADO")
                            .set("revisada", ahora)
                            .set("revisor", usuario)
                            .set("motivo", motivo)
                            .where(cb.equal(p.get("id"), id));
                    s.createMutationQuery(q).executeUpdate();
                    return null;
                });
    }

    public void vincular(int bloque, int propuesta) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(DisponibilidadPropuestaEntidad.class);
                    var d = q.from(DisponibilidadPropuestaEntidad.class);
                    q.set("propuesta", propuesta).where(cb.equal(d.get("id"), bloque));
                    s.createMutationQuery(q).executeUpdate();
                    return null;
                });
    }

    public Integer medicoUsuario(int usuario) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var m = q.from(MedicoEntidad.class);
                    var u = m.join("usuario");
                    q.select(m.get("id"))
                            .where(
                                    cb.equal(u.get("id"), usuario),
                                    cb.isTrue(u.get("activo")),
                                    cb.equal(u.get("rol"), "MEDICO"),
                                    cb.equal(m.get("estado"), "ACTIVO"));
                    return s.createQuery(q).getResultList().stream().findFirst().orElse(null);
                });
    }
}
