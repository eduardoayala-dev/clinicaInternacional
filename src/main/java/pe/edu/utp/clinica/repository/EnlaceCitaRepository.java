package pe.edu.utp.clinica.repository;

import jakarta.persistence.criteria.JoinType;

import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public final class EnlaceCitaRepository {
    public record Resumen(
            int id,
            LocalDateTime fecha,
            String medico,
            String especialidad,
            String sede,
            String estado,
            LocalDateTime confirmada,
            LocalDateTime vence) {}

    private final Connection conexion;

    public EnlaceCitaRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public Resumen consultar(int usuario, int cita) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Resumen.class);
                    var c = q.from(CitaEntidad.class);
                    var p = c.join("pacienteRelacion");
                    var u = p.join("usuario");
                    var m = c.join("medico");
                    var um = m.join("usuario");
                    var e = m.join("especialidad");
                    var d = c.join("disponibilidad", JoinType.LEFT);
                    var a = c.join("confirmacion");
                    var se = q.from(SedeEntidad.class);
                    q.select(
                                    cb.construct(
                                            Resumen.class,
                                            c.get("id"),
                                            c.get("inicio"),
                                            cb.concat(
                                                    cb.concat(um.get("nombres"), " "),
                                                    um.get("apellidos")),
                                            e.get("nombre"),
                                            se.get("nombre"),
                                            c.get("estado"),
                                            a.get("confirmada"),
                                            a.get("vence")))
                            .where(
                                    cb.equal(c.get("id"), cita),
                                    cb.equal(u.get("id"), usuario),
                                    cb.equal(u.get("rol"), "PACIENTE"),
                                    cb.isTrue(u.get("activo")),
                                    cb.equal(
                                            se.get("id"),
                                            cb.coalesce(d.get("sede"), m.get("sede").get("id"))));
                    return s.createQuery(q).getResultList().stream().findFirst().orElse(null);
                });
    }
}
