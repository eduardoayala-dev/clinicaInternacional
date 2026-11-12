package pe.edu.utp.clinica.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.JoinType;

import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public final class AdmisionRepository {
    public record Identidad(int paciente, int medico) {}

    public record Cita(int paciente, String estado, LocalDateTime programada, boolean llegada) {}

    private final Connection conexion;

    public AdmisionRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public Identidad identidad(int cita, int sede) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Identidad.class);
                    var c = q.from(CitaEntidad.class);
                    var m = c.join("medico");
                    q.select(cb.construct(Identidad.class, c.get("paciente"), m.get("id")))
                            .where(
                                    cb.equal(c.get("id"), cita),
                                    cb.equal(m.get("sede").get("id"), sede));
                    return s.createQuery(q).getResultList().stream().findFirst().orElse(null);
                });
    }

    public Cita bloquear(int cita, int medico, int paciente) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Cita.class);
                    var c = q.from(CitaEntidad.class);
                    var a = c.join("admision", JoinType.LEFT);
                    q.select(
                                    cb.construct(
                                            Cita.class,
                                            c.get("paciente"),
                                            c.get("estado"),
                                            c.get("inicio"),
                                            cb.isNotNull(a.get("id"))))
                            .where(
                                    cb.equal(c.get("id"), cita),
                                    cb.equal(c.get("medico").get("id"), medico),
                                    cb.equal(c.get("paciente"), paciente));
                    return s
                            .createQuery(q)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .getResultList()
                            .stream()
                            .findFirst()
                            .orElse(null);
                });
    }

    public void llegada(int cita, int usuario, LocalDateTime ahora, int tolerancia, boolean tarde)
            throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    s.persist(new AdmisionEntidad(cita, usuario, ahora, tolerancia, tarde));
                    s.flush();
                    return null;
                });
    }

    public void inasistencia(
            int cita, int usuario, int paciente, LocalDateTime ahora, String motivo, int tolerancia)
            throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    s.persist(new InasistenciaEntidad(cita, usuario, ahora, motivo, tolerancia));
                    s.persist(new HistorialAsistenciaEntidad(paciente, cita, false, motivo, ahora));
                    s.flush();
                    return null;
                });
    }

    public Integer usuarioPaciente(int paciente) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var p = q.from(PacienteEntidad.class);
                    q.select(p.get("usuario").get("id")).where(cb.equal(p.get("id"), paciente));
                    return s.createQuery(q).getResultList().stream().findFirst().orElse(null);
                });
    }
}
