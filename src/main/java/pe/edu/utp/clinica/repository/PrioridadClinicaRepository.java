package pe.edu.utp.clinica.repository;

import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;

public final class PrioridadClinicaRepository {
    private final Connection conexion;

    public PrioridadClinicaRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public int vincularEspecialidad(int medico, int cita) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(AtencionPrioridadEntidad.class);
                    var a = q.from(AtencionPrioridadEntidad.class);
                    var especialidad = q.subquery(Integer.class);
                    var m = especialidad.from(MedicoEntidad.class);
                    especialidad
                            .select(m.get("especialidad").get("id"))
                            .where(cb.equal(m.get("id"), medico));
                    q.set(a.<Integer>get("especialidad"), especialidad)
                            .where(
                                    cb.equal(a.get("id"), cita),
                                    cb.equal(a.get("medico"), medico),
                                    cb.isNotNull(a.get("fin")));
                    return s.createMutationQuery(q).executeUpdate();
                });
    }

    public Integer usuarioMedico(int medico) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var m = q.from(MedicoEntidad.class);
                    q.select(m.get("usuario").get("id")).where(cb.equal(m.get("id"), medico));
                    return s.createQuery(q).getResultList().stream().findFirst().orElse(null);
                });
    }
}
