package pe.edu.utp.clinica.repository;

import jakarta.persistence.criteria.*;

import pe.edu.utp.clinica.persistence.*;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.*;
import java.time.*;
import java.util.*;

public final class PadronAdmisionRepository {
    private final Connection conexion;

    public PadronAdmisionRepository(Connection conexion) {
        this.conexion = conexion;
    }

    private static String patron(String valor) {
        return "%"
                + valor.strip()
                        .toLowerCase(Locale.ROOT)
                        .replace("!", "!!")
                        .replace("%", "!%")
                        .replace("_", "!_")
                + "%";
    }

    public List<FilaConsulta> pacientes(String buscar, String cobertura, String sede, int ambito)
            throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var p = q.from(PacienteEntidad.class);
                    var u = p.join("usuario");
                    var sc = q.subquery(Integer.class);
                    var se = sc.from(SedeEntidad.class);
                    sc.select(cb.literal(1))
                            .where(
                                    cb.equal(se.get("id"), ambito),
                                    cb.equal(se.get("nombre"), p.get("sedePreferida")));
                    var citas = q.subquery(Integer.class);
                    var c = citas.from(CitaEntidad.class);
                    citas.select(cb.literal(1))
                            .where(
                                    cb.equal(c.get("paciente"), p.get("id")),
                                    cb.equal(c.get("medico").get("sede").get("id"), ambito));
                    var activas = q.subquery(Long.class);
                    var ac = activas.from(CitaEntidad.class);
                    activas.select(cb.count(ac))
                            .where(
                                    cb.equal(ac.get("paciente"), p.get("id")),
                                    ac.get("estado")
                                            .in(
                                                    "SOLICITADA",
                                                    "CONFIRMADA",
                                                    "EN_SALA",
                                                    "EN_ATENCION"));
                    var condiciones = new ArrayList<Predicate>();
                    condiciones.add(cb.or(cb.exists(sc), cb.exists(citas)));
                    if (buscar != null && !buscar.isBlank()) {
                        var v = patron(buscar);
                        condiciones.add(
                                cb.or(
                                        cb.like(cb.lower(p.get("dni")), v, '!'),
                                        cb.like(cb.lower(u.get("nombres")), v, '!'),
                                        cb.like(cb.lower(u.get("apellidos")), v, '!'),
                                        cb.like(cb.lower(p.get("telefono")), v, '!')));
                    }
                    if (cobertura != null
                            && !cobertura.isBlank()
                            && !cobertura.toLowerCase(Locale.ROOT).contains("todas"))
                        condiciones.add(
                                cb.like(cb.lower(p.get("cobertura")), patron(cobertura), '!'));
                    if (sede != null
                            && !sede.isBlank()
                            && !sede.toLowerCase(Locale.ROOT).contains("todas"))
                        condiciones.add(
                                cb.like(cb.lower(p.get("sedePreferida")), patron(sede), '!'));
                    q.multiselect(
                                    p.get("id").alias("id_paciente"),
                                    p.get("dni").alias("dni"),
                                    p.get("nacimiento").alias("fecha_nacimiento"),
                                    p.get("telefono").alias("telefono"),
                                    p.get("direccion").alias("direccion"),
                                    cb.coalesce(p.get("canal"), "Correo").alias("canal_aviso"),
                                    p.get("sedePreferida").alias("sede_preferida"),
                                    p.get("cobertura").alias("tipo_cobertura"),
                                    u.get("nombres").alias("nombres"),
                                    u.get("apellidos").alias("apellidos"),
                                    u.get("email").alias("email"),
                                    activas.alias("citas_activas"))
                            .where(condiciones.toArray(Predicate[]::new))
                            .orderBy(cb.desc(p.get("id")));
                    return s.createQuery(q).getResultList().stream()
                            .map(FilaConsulta::new)
                            .toList();
                });
    }

    public List<FilaConsulta> sala(int sede, LocalDate hoy) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var c = q.from(CitaEntidad.class);
                    var p = c.join("pacienteRelacion");
                    var up = p.join("usuario");
                    var m = c.join("medico");
                    var um = m.join("usuario");
                    var se = m.join("sede");
                    var d = c.join("disponibilidad", JoinType.LEFT);
                    var a = c.join("admision", JoinType.LEFT);
                    var nombre = q.subquery(String.class);
                    var actual = nombre.from(SedeEntidad.class);
                    nombre.select(actual.get("nombre"))
                            .where(
                                    cb.equal(
                                            actual.get("id"),
                                            cb.coalesce(d.get("sede"), se.get("id"))));
                    q.multiselect(
                                    c.get("id").alias("id_cita"),
                                    c.get("inicio").alias("fecha_hora"),
                                    c.get("estado").alias("estado"),
                                    up.get("nombres").alias("pac_nom"),
                                    up.get("apellidos").alias("pac_ape"),
                                    p.get("dni").alias("dni"),
                                    p.get("telefono").alias("telefono"),
                                    um.get("nombres").alias("med_nom"),
                                    um.get("apellidos").alias("med_ape"),
                                    nombre.alias("sede_nombre"),
                                    a.get("llegada").alias("fecha_llegada"),
                                    a.get("tarde").alias("llego_tarde"))
                            .where(
                                    cb.equal(cb.coalesce(d.get("sede"), se.get("id")), sede),
                                    cb.greaterThanOrEqualTo(c.get("inicio"), hoy.atStartOfDay()),
                                    cb.lessThan(c.get("inicio"), hoy.plusDays(1).atStartOfDay()))
                            .orderBy(cb.asc(c.get("inicio")), cb.asc(c.get("id")));
                    return s.createQuery(q).getResultList().stream()
                            .map(FilaConsulta::new)
                            .toList();
                });
    }

    public List<FilaConsulta> disponibilidades(int sede) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var d = q.from(DisponibilidadEntidad.class);
                    var nombre = q.subquery(String.class);
                    var se = nombre.from(SedeEntidad.class);
                    nombre.select(se.get("nombre")).where(cb.equal(se.get("id"), d.get("sede")));
                    var citas = q.subquery(Long.class);
                    var c = citas.from(CitaEntidad.class);
                    citas.select(cb.count(c))
                            .where(
                                    cb.equal(c.get("disponibilidad").get("id"), d.get("id")),
                                    cb.not(c.get("estado").in("CANCELADA", "REPROGRAMADA")));
                    q.multiselect(
                                    d.get("id").alias("id_disponibilidad"),
                                    d.get("medico").get("id").alias("id_medico"),
                                    d.get("sede").alias("id_sede"),
                                    nombre.alias("sede_nombre"),
                                    d.get("fecha").alias("fecha"),
                                    d.get("inicio").alias("hora_inicio"),
                                    d.get("fin").alias("hora_fin"),
                                    cb.coalesce(d.get("minutos"), 30).alias("duracion_minutos"),
                                    cb.coalesce(d.get("estado"), "PUBLICADO").alias("estado"),
                                    citas.alias("citas_agendadas"))
                            .where(cb.equal(d.get("sede"), sede))
                            .orderBy(cb.desc(d.get("fecha")), cb.asc(d.get("inicio")));
                    return s.createQuery(q).getResultList().stream()
                            .map(FilaConsulta::new)
                            .toList();
                });
    }

    public int paciente(String dni) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var p = q.from(PacienteEntidad.class);
                    q.select(p.get("id")).where(cb.equal(p.get("dni"), dni));
                    return s.createQuery(q).setMaxResults(1).getResultList().stream()
                            .findFirst()
                            .orElse(-1);
                });
    }
}
