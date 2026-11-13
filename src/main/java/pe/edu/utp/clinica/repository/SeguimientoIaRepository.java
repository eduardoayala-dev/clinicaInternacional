package pe.edu.utp.clinica.repository;

import jakarta.persistence.criteria.JoinType;

import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public final class SeguimientoIaRepository {
    public record Datos(
            int cita,
            LocalDateTime fecha,
            String estado,
            LocalDateTime creada,
            LocalDateTime vence,
            LocalDateTime confirmada,
            long faltas,
            long atenciones) {}

    private final Connection conexion;

    public SeguimientoIaRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public Datos consultar(int actor, int cita) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Datos.class);
                    var c = q.from(CitaEntidad.class);
                    var p = c.join("pacienteRelacion");
                    var paciente = p.join("usuario");
                    var m = c.join("medico");
                    var d = c.join("disponibilidad", JoinType.LEFT);
                    var a = c.join("confirmacion", JoinType.LEFT);
                    var pa = q.from(PersonalEntidad.class);
                    var u = pa.join("usuario");
                    var se = pa.join("sede");
                    var faltas = q.subquery(Long.class);
                    var h = faltas.from(CitaEntidad.class);
                    var i = faltas.from(InasistenciaEntidad.class);
                    var responsable = faltas.from(UsuarioEntidad.class);
                    faltas.select(cb.count(h))
                            .where(
                                    cb.equal(i.get("id"), h.get("id")),
                                    cb.equal(responsable.get("id"), i.get("usuario")),
                                    cb.equal(h.get("paciente"), c.get("paciente")),
                                    cb.equal(h.get("estado"), "NO_ASISTIO"),
                                    cb.equal(responsable.get("rol"), "PERSONAL_ADMINISTRATIVO"),
                                    cb.lessThan(h.get("inicio"), a.get("creada")),
                                    cb.lessThan(i.get("fecha"), a.get("creada")));
                    var atendidas = q.subquery(Long.class);
                    var anterior = atendidas.from(CitaEntidad.class);
                    var t = atendidas.from(AtencionPrioridadEntidad.class);
                    atendidas
                            .select(cb.count(anterior))
                            .where(
                                    cb.equal(t.get("id"), anterior.get("id")),
                                    cb.equal(anterior.get("paciente"), c.get("paciente")),
                                    cb.equal(anterior.get("estado"), "ATENDIDA"),
                                    cb.equal(t.get("paciente"), anterior.get("paciente")),
                                    cb.equal(t.get("medico"), anterior.get("medico").get("id")),
                                    cb.isNotNull(t.get("inicio")),
                                    cb.greaterThanOrEqualTo(t.get("fin"), t.get("inicio")),
                                    cb.lessThan(anterior.get("inicio"), a.get("creada")),
                                    cb.lessThan(t.get("fin"), a.get("creada")));
                    q.select(
                                    cb.construct(
                                            Datos.class,
                                            c.get("id"),
                                            c.get("inicio"),
                                            c.get("estado"),
                                            a.get("creada"),
                                            a.get("vence"),
                                            a.get("confirmada"),
                                            faltas,
                                            atendidas))
                            .where(
                                    cb.equal(c.get("id"), cita),
                                    cb.isTrue(paciente.get("activo")),
                                    cb.equal(paciente.get("rol"), "PACIENTE"),
                                    cb.equal(
                                            se.get("id"),
                                            cb.coalesce(d.get("sede"), m.get("sede").get("id"))),
                                    cb.equal(u.get("id"), actor),
                                    cb.isTrue(u.get("activo")),
                                    cb.equal(u.get("rol"), "PERSONAL_ADMINISTRATIVO"),
                                    cb.equal(se.get("estado"), "ACTIVO"));
                    return s.createQuery(q).getResultList().stream().findFirst().orElse(null);
                });
    }
}
