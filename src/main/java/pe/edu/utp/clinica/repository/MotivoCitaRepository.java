package pe.edu.utp.clinica.repository;

import jakarta.persistence.criteria.Predicate;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.MotivoCitaEntidad;

public final class MotivoCitaRepository {
    public record Motivo(int id, int especialidad, String descripcion) {}
    private final Connection conexion;

    public MotivoCitaRepository(Connection conexion) { this.conexion = conexion; }

    public List<Motivo> activos(Integer idEspecialidad) throws SQLException {
        return PersistenciaJpa.consultar(conexion, sesion -> {
            var cb = sesion.getCriteriaBuilder();
            var consulta = cb.createQuery(Motivo.class);
            var motivo = consulta.from(MotivoCitaEntidad.class);
            var especialidad = motivo.join("especialidad");
            var condiciones = new ArrayList<Predicate>();
            condiciones.add(cb.equal(motivo.get("estado"), "ACTIVO"));
            condiciones.add(cb.equal(especialidad.get("estado"), "ACTIVO"));
            if (idEspecialidad != null) condiciones.add(cb.equal(especialidad.get("id"), idEspecialidad));
            consulta.select(cb.construct(Motivo.class, motivo.get("id"), especialidad.get("id"), motivo.get("descripcion")))
                    .where(condiciones.toArray(Predicate[]::new))
                    .orderBy(cb.asc(especialidad.get("id")), cb.asc(motivo.get("descripcion")), cb.asc(motivo.get("id")));
            return sesion.createQuery(consulta).getResultList();
        });
    }
}
