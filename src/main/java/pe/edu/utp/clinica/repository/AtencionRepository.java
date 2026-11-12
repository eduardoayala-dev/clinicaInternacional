package pe.edu.utp.clinica.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.JoinType;

import pe.edu.utp.clinica.model.medico.ConsultaClinica;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AtencionRepository {
    public record Cita(
            int paciente, String estado, LocalDateTime programada, LocalDateTime llegada) {}

    private final Connection conexion;

    public AtencionRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public Integer paciente(int cita, int medico) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var c = q.from(CitaEntidad.class);
                    q.select(c.get("paciente"))
                            .where(
                                    cb.equal(c.get("id"), cita),
                                    cb.equal(c.get("medico").get("id"), medico));
                    return s.createQuery(q).getResultList().stream().findFirst().orElse(null);
                });
    }

    public boolean medicoHabilitado(int medico) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var m = q.from(MedicoEntidad.class);
                    var u = m.join("usuario");
                    q.select(u.get("id"))
                            .where(
                                    cb.equal(m.get("id"), medico),
                                    cb.isTrue(u.get("activo")),
                                    cb.equal(u.get("rol"), "MEDICO"));
                    return !s.createQuery(q)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .getResultList()
                            .isEmpty();
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
                                            a.get("llegada")))
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

    public boolean ocupados(int medico, int paciente) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var a = q.from(AtencionPrioridadEntidad.class);
                    q.select(a.get("id"))
                            .where(
                                    cb.or(
                                            cb.equal(a.get("medicoActivo"), medico),
                                            cb.equal(a.get("pacienteActivo"), paciente)));
                    return !s.createQuery(q).setMaxResults(1).getResultList().isEmpty();
                });
    }

    public void registrar(
            int cita,
            int medico,
            int paciente,
            LocalDateTime inicio,
            LocalDateTime fin,
            String motivo)
            throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    s.persist(
                            new AtencionPrioridadEntidad(
                                    cita, medico, paciente, inicio, fin, motivo));
                    s.flush();
                    return null;
                });
    }

    public int cerrar(int cita, int medico, LocalDateTime ahora, String motivo)
            throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(AtencionPrioridadEntidad.class);
                    var a = q.from(AtencionPrioridadEntidad.class);
                    q.set("fin", ahora)
                            .set(a.<String>get("motivo"), motivo)
                            .where(
                                    cb.equal(a.get("id"), cita),
                                    cb.equal(a.get("medico"), medico),
                                    cb.isNotNull(a.get("inicio")),
                                    cb.isNull(a.get("fin")),
                                    cb.lessThanOrEqualTo(a.get("inicio"), ahora));
                    return s.createMutationQuery(q).executeUpdate();
                });
    }

    public int estado(
            int cita,
            int medico,
            String estado,
            String prioridad,
            LocalDate seguimiento,
            boolean finalizar)
            throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(CitaEntidad.class);
                    var c = q.from(CitaEntidad.class);
                    q.set("estado", estado);
                    if (finalizar)
                        q.set("prioridad", prioridad)
                                .set(c.<LocalDate>get("recomendada"), seguimiento);
                    q.where(
                            cb.equal(c.get("id"), cita),
                            cb.equal(c.get("medico").get("id"), medico));
                    return s.createMutationQuery(q).executeUpdate();
                });
    }

    public void consulta(int cita, ConsultaClinica consulta) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    s.persist(new ConsultaClinicaEntidad(cita, consulta));
                    s.flush();
                    return null;
                });
    }

    public void historial(int paciente, int cita, String notas, LocalDateTime ahora)
            throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    s.persist(new HistorialAsistenciaEntidad(paciente, cita, true, notas, ahora));
                    return null;
                });
    }
}
