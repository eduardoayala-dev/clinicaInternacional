package pe.edu.utp.clinica.repository;

import jakarta.persistence.criteria.JoinType;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.CitaEntidad;
import pe.edu.utp.clinica.persistence.entity.MedicoEntidad;
import pe.edu.utp.clinica.persistence.entity.PacienteEntidad;
import pe.edu.utp.clinica.persistence.entity.PacienteBloqueoEntidad;

public final class ConsultaCitaRepository {
    public enum Listado { PROXIMAS, HISTORIAL, CANCELADAS }
    public record Cita(int id, int medico, Integer paciente, Integer motivo, LocalDateTime inicio,
            String estado, String prioridad, String nombres, String apellidos, String cmp,
            Integer especialidad, String especialidadNombre, Integer sede, String sedeNombre) {}
    public record Riesgo(int id, LocalDateTime inicio, String prioridad, String riesgo,
            String pacienteNombres, String pacienteApellidos, String medicoNombres,
            String medicoApellidos, String especialidad, String sede) {}
    private final Connection conexion;

    public ConsultaCitaRepository(Connection conexion) { this.conexion = conexion; }

    public List<Cita> delPaciente(int paciente, Listado listado, LocalDateTime inicioDia) throws SQLException {
        return citas(paciente, listado, inicioDia, null);
    }

    public Cita detalle(int cita) throws SQLException {
        return citas(null, null, null, cita).stream().findFirst().orElse(null);
    }

    public boolean canceladaPropia(int id, int paciente) throws SQLException {
        return PersistenciaJpa.consultar(conexion, sesion -> {
            var cb = sesion.getCriteriaBuilder();
            var consulta = cb.createQuery(Integer.class);
            var cita = consulta.from(CitaEntidad.class);
            consulta.select(cita.get("id")).where(cb.equal(cita.get("id"), id),
                    cb.equal(cita.get("paciente"), paciente), cb.equal(cita.get("estado"), "CANCELADA"));
            return !sesion.createQuery(consulta).setMaxResults(1).getResultList().isEmpty();
        });
    }

    private List<Cita> citas(Integer paciente, Listado listado, LocalDateTime inicioDia, Integer id) throws SQLException {
        return PersistenciaJpa.consultar(conexion, sesion -> {
            var cb = sesion.getCriteriaBuilder();
            var consulta = cb.createQuery(Cita.class);
            var cita = consulta.from(CitaEntidad.class);
            var medico = cita.join("medico");
            var usuario = medico.join("usuario");
            var especialidad = medico.join("especialidad", JoinType.LEFT);
            var sede = medico.join("sede", JoinType.LEFT);
            var condiciones = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (id != null) condiciones.add(cb.equal(cita.get("id"), id));
            if (paciente != null) condiciones.add(cb.equal(cita.get("paciente"), paciente));
            if (listado == Listado.PROXIMAS) condiciones.add(cb.or(cita.get("estado").in("EN_SALA", "EN_ATENCION"),
                    cb.and(cita.get("estado").in("SOLICITADA", "CONFIRMADA"), cb.greaterThanOrEqualTo(cita.get("inicio"), inicioDia))));
            if (listado == Listado.HISTORIAL) condiciones.add(cb.or(cita.get("estado").in("ATENDIDA", "NO_ASISTIO", "NO_ATENDIDA"),
                    cb.and(cita.get("estado").in("SOLICITADA", "CONFIRMADA"), cb.lessThan(cita.get("inicio"), inicioDia))));
            if (listado == Listado.CANCELADAS) condiciones.add(cita.get("estado").in("CANCELADA", "REPROGRAMADA"));
            consulta.select(cb.construct(Cita.class, cita.get("id"), medico.get("id"), cita.get("paciente"),
                    id == null ? cb.nullLiteral(Integer.class) : cita.get("motivo"), cita.get("inicio"),
                    cita.get("estado"), cita.get("prioridad"), usuario.get("nombres"), usuario.get("apellidos"),
                    medico.get("cmp"), especialidad.get("id"), especialidad.get("nombre"), sede.get("id"), sede.get("nombre")))
                    .where(condiciones.toArray(jakarta.persistence.criteria.Predicate[]::new));
            consulta.orderBy(listado == Listado.PROXIMAS ? cb.asc(cita.get("inicio")) : cb.desc(cita.get("inicio")));
            return sesion.createQuery(consulta).getResultList();
        });
    }

    public List<Riesgo> riesgos(LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.consultar(conexion, sesion -> {
            var cb = sesion.getCriteriaBuilder();
            var consulta = cb.createQuery(Riesgo.class);
            var cita = consulta.from(CitaEntidad.class);
            var paciente = consulta.from(PacienteEntidad.class);
            var usuario = paciente.join("usuario");
            var medico = cita.join("medico");
            var profesional = medico.join("usuario");
            var especialidad = medico.join("especialidad", JoinType.LEFT);
            var sede = medico.join("sede", JoinType.LEFT);
            consulta.select(cb.construct(Riesgo.class, cita.get("id"), cita.get("inicio"), cita.get("prioridad"),
                    cita.get("riesgo"), usuario.get("nombres"), usuario.get("apellidos"), profesional.get("nombres"),
                    profesional.get("apellidos"), especialidad.get("nombre"), sede.get("nombre")))
                    .where(cb.equal(paciente.get("id"), cita.get("paciente")), cb.equal(cita.get("estado"), "CONFIRMADA"),
                            cb.greaterThanOrEqualTo(cita.get("inicio"), ahora))
                    .orderBy(cb.asc(cb.selectCase(cita.get("riesgo")).when("ALTO", 1).when("MEDIO", 2).otherwise(3)),
                            cb.asc(cita.get("inicio")));
            return sesion.createQuery(consulta).setMaxResults(10).getResultList();
        });
    }

    public int medicoPorUsuario(int usuario) throws SQLException {
        return PersistenciaJpa.consultar(conexion, sesion -> {
            var cb = sesion.getCriteriaBuilder();
            var consulta = cb.createQuery(Integer.class);
            var medico = consulta.from(MedicoEntidad.class);
            consulta.select(medico.get("id")).where(cb.equal(medico.get("usuario").get("id"), usuario));
            return sesion.createQuery(consulta).setMaxResults(1).getResultList().stream().findFirst().orElse(-1);
        });
    }

    public int pacientePorUsuario(int usuario) throws SQLException {
        return PersistenciaJpa.consultar(conexion, sesion -> {
            var cb = sesion.getCriteriaBuilder();
            var consulta = cb.createQuery(Integer.class);
            var paciente = consulta.from(PacienteBloqueoEntidad.class);
            consulta.select(paciente.get("id")).where(cb.equal(paciente.get("usuario"), usuario));
            return sesion.createQuery(consulta).setMaxResults(1).getResultList().stream().findFirst().orElse(-1);
        });
    }

    public int usuarioPorPaciente(int id) throws SQLException {
        return PersistenciaJpa.consultar(conexion, sesion -> {
            var cb = sesion.getCriteriaBuilder();
            var consulta = cb.createQuery(Integer.class);
            var paciente = consulta.from(PacienteBloqueoEntidad.class);
            consulta.select(paciente.get("usuario")).where(cb.equal(paciente.get("id"), id));
            var resultado = sesion.createQuery(consulta).setMaxResults(1).getResultList();
            return resultado.isEmpty() ? -1 : resultado.get(0) == null ? 0 : resultado.get(0);
        });
    }
}
