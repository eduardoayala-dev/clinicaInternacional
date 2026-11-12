package pe.edu.utp.clinica.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.SedeEntidad;
import pe.edu.utp.clinica.persistence.entity.EspecialidadEntidad;
import pe.edu.utp.clinica.persistence.entity.MedicoEntidad;
import pe.edu.utp.clinica.persistence.entity.CitaEntidad;

public final class CatalogoMedicoRepository {
    public record Especialidad(int id, String nombre, long medicos) {}
    public record Sede(int id, String nombre, String direccion) {}
    public record Medico(int id, String nombres, String apellidos, String cmp,
            Integer especialidad, String especialidadNombre, Integer sede, String sedeNombre) {}

    private final Connection conexion;

    public CatalogoMedicoRepository(Connection conexion) { this.conexion = conexion; }

    public List<Especialidad> especialidades() throws SQLException {
        return PersistenciaJpa.consultar(conexion, sesion -> {
            var cb = sesion.getCriteriaBuilder();
            var consulta = cb.createQuery(Especialidad.class);
            var especialidad = consulta.from(EspecialidadEntidad.class);
            var medico = especialidad.join("medicos", JoinType.LEFT);
            medico.on(cb.equal(medico.get("estado"), "ACTIVO"));
            consulta.select(cb.construct(Especialidad.class, especialidad.get("id"),
                    especialidad.get("nombre"), cb.count(medico.get("id"))))
                    .where(cb.equal(especialidad.get("estado"), "ACTIVO"))
                    .groupBy(especialidad.get("id"), especialidad.get("nombre"))
                    .orderBy(cb.asc(especialidad.get("id")));
            return sesion.createQuery(consulta).getResultList();
        });
    }

    public List<Sede> sedes() throws SQLException {
        return PersistenciaJpa.consultar(conexion, sesion -> {
            var cb = sesion.getCriteriaBuilder();
            var consulta = cb.createQuery(Sede.class);
            var sede = consulta.from(SedeEntidad.class);
            consulta.select(cb.construct(Sede.class, sede.get("id"), sede.get("nombre"), sede.get("direccion")))
                    .where(cb.equal(sede.get("estado"), "ACTIVO"));
            return sesion.createQuery(consulta).getResultList();
        });
    }

    public List<Medico> disponibles(Integer especialidad, Integer sede) throws SQLException {
        return medicos(especialidad, sede, null, null, true);
    }

    public List<Medico> detalle(int medico) throws SQLException {
        return medicos(null, null, medico, null, false);
    }

    public List<Medico> dePaciente(int paciente) throws SQLException {
        return medicos(null, null, null, paciente, false);
    }

    public List<Medico> todos() throws SQLException {
        return medicos(null, null, null, null, false);
    }

    private List<Medico> medicos(Integer idEspecialidad, Integer idSede, Integer idMedico,
            Integer idPaciente, boolean soloActivos) throws SQLException {
        return PersistenciaJpa.consultar(conexion, sesion -> {
            var cb = sesion.getCriteriaBuilder();
            var consulta = cb.createQuery(Medico.class);
            var medico = consulta.from(MedicoEntidad.class);
            var usuario = medico.join("usuario");
            var especialidad = medico.join("especialidad", soloActivos ? JoinType.INNER : JoinType.LEFT);
            var sede = medico.join("sede", soloActivos ? JoinType.INNER : JoinType.LEFT);
            var condiciones = new ArrayList<Predicate>();
            if (soloActivos) {
                condiciones.add(cb.equal(medico.get("estado"), "ACTIVO"));
                condiciones.add(cb.isTrue(usuario.get("activo")));
                condiciones.add(cb.equal(especialidad.get("estado"), "ACTIVO"));
                condiciones.add(cb.equal(sede.get("estado"), "ACTIVO"));
            }
            igualSiPresente(cb, especialidad, idEspecialidad, condiciones);
            igualSiPresente(cb, sede, idSede, condiciones);
            igualSiPresente(cb, medico, idMedico, condiciones);
            if (idPaciente != null) {
                var historial = consulta.subquery(Integer.class);
                var cita = historial.from(CitaEntidad.class);
                historial.select(cb.literal(1)).where(cb.equal(cita.get("paciente"), idPaciente),
                        cb.equal(cita.get("medico").get("id"), medico.get("id")));
                condiciones.add(cb.exists(historial));
            }
            consulta.select(cb.construct(Medico.class, medico.get("id"), usuario.get("nombres"),
                    usuario.get("apellidos"), medico.get("cmp"), especialidad.get("id"),
                    especialidad.get("nombre"), sede.get("id"), sede.get("nombre")))
                    .where(condiciones.toArray(Predicate[]::new));
            if (soloActivos) consulta.orderBy(cb.asc(usuario.get("apellidos")), cb.asc(usuario.get("nombres")), cb.asc(medico.get("id")));
            else consulta.orderBy(cb.asc(medico.get("id")));
            return sesion.createQuery(consulta).getResultList();
        });
    }

    private static void igualSiPresente(CriteriaBuilder cb, From<?, ?> entidad, Integer id, List<Predicate> condiciones) {
        if (id != null) condiciones.add(cb.equal(entidad.get("id"), id));
    }
}
