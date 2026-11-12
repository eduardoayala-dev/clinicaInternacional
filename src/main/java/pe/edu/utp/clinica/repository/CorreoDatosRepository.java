package pe.edu.utp.clinica.repository;

import pe.edu.utp.clinica.correo.ConfirmacionCita;
import pe.edu.utp.clinica.correo.OfertaCupo;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

public final class CorreoDatosRepository {
    private final Connection conexion;

    public CorreoDatosRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public Optional<ConfirmacionCita> cita(int id, LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(ConfirmacionCita.class);
                    var c = q.from(CitaEntidad.class);
                    var u = c.join("pacienteRelacion").join("usuario");
                    var m = c.join("medico");
                    var um = m.join("usuario");
                    var e = m.join("especialidad");
                    var sede = m.join("sede");
                    q.select(
                                    cb.construct(
                                            ConfirmacionCita.class,
                                            c.get("id"),
                                            u.get("email"),
                                            u.get("nombres"),
                                            cb.concat(
                                                    cb.concat(um.get("nombres"), " "),
                                                    um.get("apellidos")),
                                            e.get("nombre"),
                                            sede.get("nombre"),
                                            c.get("inicio")))
                            .where(
                                    cb.equal(c.get("id"), id),
                                    cb.equal(c.get("estado"), "CONFIRMADA"),
                                    cb.isTrue(u.get("activo")),
                                    cb.equal(u.get("rol"), "PACIENTE"),
                                    cb.greaterThan(c.get("inicio"), ahora));
                    return s.createQuery(q).getResultList().stream().findFirst();
                });
    }

    public void encolarOferta(int id, LocalDateTime ahora) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    s.persist(new CorreoOfertaEntidad(id, ahora));
                    return null;
                });
    }

    public Optional<OfertaCupo> oferta(int id, LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(OfertaCupo.class);
                    var r = q.from(OfertaEntidad.class);
                    var l = r.join("lista");
                    var u = l.join("paciente").join("usuario");
                    var c = r.join("cita");
                    var d = r.join("disponibilidad");
                    var m = c.join("medico");
                    var um = m.join("usuario");
                    var e = m.join("especialidad");
                    var sede = m.join("sede");
                    var ocupadas = q.subquery(Integer.class);
                    var o = ocupadas.from(CitaEntidad.class);
                    ocupadas.select(cb.literal(1))
                            .where(
                                    cb.equal(o.get("medico").get("id"), m.get("id")),
                                    cb.equal(o.get("inicio"), c.get("inicio")),
                                    cb.not(o.get("estado").in("CANCELADA", "REPROGRAMADA")));
                    q.select(
                                    cb.construct(
                                            OfertaCupo.class,
                                            r.get("id"),
                                            u.get("email"),
                                            u.get("nombres"),
                                            cb.concat(
                                                    cb.concat(um.get("nombres"), " "),
                                                    um.get("apellidos")),
                                            e.get("nombre"),
                                            sede.get("nombre"),
                                            c.get("inicio"),
                                            r.get("vence")))
                            .where(
                                    cb.equal(r.get("id"), id),
                                    cb.equal(r.get("estado"), "PENDIENTE"),
                                    cb.equal(l.get("estado"), "OFERTADA"),
                                    cb.like(l.get("canales"), "%CORREO%"),
                                    cb.greaterThan(r.get("vence"), ahora),
                                    cb.greaterThan(c.get("inicio"), ahora),
                                    cb.equal(c.get("estado"), "CANCELADA"),
                                    cb.equal(d.get("estado"), "PUBLICADO"),
                                    cb.equal(d.get("sede"), sede.get("id")),
                                    cb.equal(m.get("estado"), "ACTIVO"),
                                    cb.isTrue(um.get("activo")),
                                    cb.equal(e.get("estado"), "ACTIVO"),
                                    cb.equal(sede.get("estado"), "ACTIVO"),
                                    cb.isTrue(u.get("activo")),
                                    cb.equal(u.get("rol"), "PACIENTE"),
                                    cb.not(cb.exists(ocupadas)));
                    return s.createQuery(q).getResultList().stream().findFirst();
                });
    }

    public record Cambio(
            String tipo,
            int cita,
            String estado,
            LocalDateTime fecha,
            String correo,
            String paciente,
            String medico,
            String apellidos,
            String especialidad,
            String sede,
            Integer origen,
            LocalDateTime fechaAnterior,
            String estadoAnterior,
            LocalDateTime confirmada,
            LocalDateTime vence) {}

    public Optional<Cambio> cambio(int id) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Cambio.class);
                    var a = q.from(CorreoCambioEntidad.class);
                    var c = a.join("citaRelacion");
                    var u = c.join("pacienteRelacion").join("usuario");
                    var m = c.join("medico");
                    var um = m.join("usuario");
                    var e = m.join("especialidad");
                    var d = c.join("disponibilidad", jakarta.persistence.criteria.JoinType.LEFT);
                    var sede = q.from(SedeEntidad.class);
                    var anterior =
                            a.join("anteriorRelacion", jakarta.persistence.criteria.JoinType.LEFT);
                    anterior.on(cb.equal(anterior.get("paciente"), c.get("paciente")));
                    var asistencia =
                            c.join("confirmacion", jakarta.persistence.criteria.JoinType.LEFT);
                    q.select(
                                    cb.construct(
                                            Cambio.class,
                                            a.get("tipo"),
                                            c.get("id"),
                                            c.get("estado"),
                                            c.get("inicio"),
                                            u.get("email"),
                                            u.get("nombres"),
                                            um.get("nombres"),
                                            um.get("apellidos"),
                                            e.get("nombre"),
                                            sede.get("nombre"),
                                            anterior.get("id"),
                                            anterior.get("inicio"),
                                            anterior.get("estado"),
                                            asistencia.get("confirmada"),
                                            asistencia.get("vence")))
                            .where(
                                    cb.equal(a.get("id"), id),
                                    cb.isTrue(u.get("activo")),
                                    cb.equal(u.get("rol"), "PACIENTE"),
                                    cb.equal(
                                            sede.get("id"),
                                            cb.coalesce(d.get("sede"), m.get("sede").get("id"))));
                    return s.createQuery(q).getResultList().stream().findFirst();
                });
    }
}
