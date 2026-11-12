package pe.edu.utp.clinica.repository;

import jakarta.persistence.LockModeType;

import pe.edu.utp.clinica.dao.AmbitoAdmisionDAO.EdicionPaciente;
import pe.edu.utp.clinica.dao.AmbitoAdmisionDAO.Recurso;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;

public final class AmbitoAdmisionRepository {
    public record Sede(int id, String nombre) {}

    private final Connection conexion;

    public AmbitoAdmisionRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public Sede sede(int usuario) throws SQLException {
        return sede(usuario, false);
    }

    public Sede sedeConNombre(int usuario) throws SQLException {
        return sede(usuario, true);
    }

    private Sede sede(int usuario, boolean nombre) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Sede.class);
                    var p = q.from(PersonalEntidad.class);
                    var u = p.join("usuario");
                    var sede = p.join("sede");
                    q.select(
                                    cb.construct(
                                            Sede.class,
                                            sede.get("id"),
                                            nombre
                                                    ? sede.get("nombre")
                                                    : cb.nullLiteral(String.class)))
                            .where(
                                    cb.equal(u.get("id"), usuario),
                                    cb.isTrue(u.get("activo")),
                                    cb.equal(u.get("rol"), "PERSONAL_ADMINISTRATIVO"),
                                    cb.equal(sede.get("estado"), "ACTIVO"));
                    var consulta = s.createQuery(q);
                    if (s.getTransaction().isActive())
                        consulta.setLockMode(LockModeType.PESSIMISTIC_WRITE);
                    return consulta.getResultList().stream().findFirst().orElse(null);
                });
    }

    public boolean autorizado(int sede, Recurso recurso, int id) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    switch (recurso) {
                        case CITA -> {
                            var c = q.from(CitaEntidad.class);
                            q.select(c.get("id"))
                                    .where(
                                            cb.equal(c.get("id"), id),
                                            cb.equal(c.get("medico").get("sede").get("id"), sede));
                        }
                        case MEDICO -> {
                            var m = q.from(MedicoEntidad.class);
                            q.select(m.get("id"))
                                    .where(
                                            cb.equal(m.get("id"), id),
                                            cb.equal(m.get("sede").get("id"), sede));
                        }
                        case HORARIO -> {
                            var d = q.from(DisponibilidadEntidad.class);
                            q.select(d.get("id"))
                                    .where(
                                            cb.equal(d.get("id"), id),
                                            cb.equal(d.get("sede"), sede));
                        }
                        case PACIENTE -> {
                            var p = q.from(PacienteEntidad.class);
                            var preferida = q.subquery(Integer.class);
                            var se = preferida.from(SedeEntidad.class);
                            preferida
                                    .select(cb.literal(1))
                                    .where(
                                            cb.equal(se.get("id"), sede),
                                            cb.equal(se.get("nombre"), p.get("sedePreferida")));
                            var citas = q.subquery(Integer.class);
                            var c = citas.from(CitaEntidad.class);
                            citas.select(cb.literal(1))
                                    .where(
                                            cb.equal(c.get("paciente"), p.get("id")),
                                            cb.equal(c.get("medico").get("sede").get("id"), sede));
                            q.select(p.get("id"))
                                    .where(
                                            cb.equal(p.get("id"), id),
                                            cb.or(cb.exists(preferida), cb.exists(citas)));
                        }
                    }
                    return !s.createQuery(q).setMaxResults(1).getResultList().isEmpty();
                });
    }

    public void editarPaciente(int paciente, EdicionPaciente datos) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(PacienteEntidad.class);
                    var p = q.from(PacienteEntidad.class);
                    q.set("telefono", datos.telefono().trim())
                            .set("direccion", datos.direccion().trim())
                            .set("cobertura", datos.cobertura().trim())
                            .set("canal", "Correo")
                            .where(cb.equal(p.get("id"), paciente));
                    s.createMutationQuery(q).executeUpdate();
                    var usuario = cb.createCriteriaUpdate(UsuarioEntidad.class);
                    var u = usuario.from(UsuarioEntidad.class);
                    var id = usuario.subquery(Integer.class);
                    var perfil = id.from(PacienteEntidad.class);
                    id.select(perfil.get("usuario").get("id"))
                            .where(cb.equal(perfil.get("id"), paciente));
                    usuario.set("nombres", datos.nombres().trim())
                            .set("apellidos", datos.apellidos().trim())
                            .where(cb.equal(u.get("id"), id));
                    s.createMutationQuery(usuario).executeUpdate();
                    return null;
                });
    }
}
