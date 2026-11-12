package pe.edu.utp.clinica.repository;

import jakarta.persistence.criteria.*;

import pe.edu.utp.clinica.persistence.FilaConsulta;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public final class ConsultaMedicaRepository {
    private final Connection conexion;

    public ConsultaMedicaRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public FilaConsulta perfil(int usuario) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var m = q.from(MedicoEntidad.class);
                    var u = m.join("usuario");
                    var e = m.join("especialidad");
                    var se = m.join("sede");
                    q.multiselect(
                                    m.get("id").alias("id_medico"),
                                    m.get("cmp").alias("cmp"),
                                    u.get("nombres").alias("nombres"),
                                    u.get("apellidos").alias("apellidos"),
                                    u.get("email").alias("email"),
                                    e.get("nombre").alias("especialidad"),
                                    se.get("nombre").alias("sedeNombre"))
                            .where(
                                    cb.equal(u.get("id"), usuario),
                                    cb.equal(m.get("estado"), "ACTIVO"),
                                    cb.isTrue(u.get("activo")),
                                    cb.equal(u.get("rol"), "MEDICO"),
                                    cb.equal(e.get("estado"), "ACTIVO"),
                                    cb.equal(se.get("estado"), "ACTIVO"));
                    return s.createQuery(q).getResultList().stream()
                            .map(FilaConsulta::new)
                            .findFirst()
                            .orElse(null);
                });
    }

    public boolean perfilPendiente(int usuario) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var u = q.from(UsuarioEntidad.class);
                    var perfiles = q.subquery(Integer.class);
                    var m = perfiles.from(MedicoEntidad.class);
                    perfiles.select(cb.literal(1))
                            .where(cb.equal(m.get("usuario").get("id"), u.get("id")));
                    q.select(u.get("id"))
                            .where(
                                    cb.equal(u.get("id"), usuario),
                                    cb.equal(u.get("rol"), "MEDICO"),
                                    cb.isTrue(u.get("activo")),
                                    cb.not(cb.exists(perfiles)));
                    return !s.createQuery(q).getResultList().isEmpty();
                });
    }

    static Expression<String> indicaciones(
            CriteriaBuilder cb, CommonAbstractCriteria q, Expression<Integer> cita) {
        var ultima = q.subquery(Integer.class);
        var h = ultima.from(HistorialAsistenciaEntidad.class);
        ultima.select(cb.max(h.<Integer>get("id"))).where(cb.equal(h.get("cita"), cita));
        var texto = q.subquery(String.class);
        var historial = texto.from(HistorialAsistenciaEntidad.class);
        texto.select(historial.get("observacion")).where(cb.equal(historial.get("id"), ultima));
        return texto;
    }

    public List<FilaConsulta> citas(int medico, LocalDate desde, LocalDate hasta, Integer id)
            throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var c = q.from(CitaEntidad.class);
                    var p = c.join("pacienteRelacion");
                    var u = p.join("usuario");
                    var motivo = c.join("motivoRelacion", JoinType.LEFT);
                    var a = c.join("admision", JoinType.LEFT);
                    var t = c.join("atencion", JoinType.LEFT);
                    var ocupada = q.subquery(Integer.class);
                    var ac = ocupada.from(AtencionPrioridadEntidad.class);
                    ocupada.select(cb.literal(1))
                            .where(
                                    cb.or(
                                            cb.equal(
                                                    ac.get("medicoActivo"),
                                                    c.get("medico").get("id")),
                                            cb.equal(ac.get("pacienteActivo"), c.get("paciente"))));
                    var reasignada = q.subquery(Integer.class);
                    var r = reasignada.from(OfertaEntidad.class);
                    reasignada
                            .select(cb.literal(1))
                            .where(
                                    cb.equal(r.get("asignada"), c.get("id")),
                                    cb.equal(r.get("estado"), "ACEPTADA"));
                    var campos = new ArrayList<Selection<?>>();
                    campos.add(cb.exists(ocupada).alias("consulta_ocupada"));
                    campos.add(t.get("inicio").alias("fecha_inicio"));
                    campos.add(t.get("fin").alias("fecha_fin"));
                    campos.add(t.get("motivo").alias("motivo_no_atencion"));
                    campos.add(a.get("llegada").alias("fecha_llegada"));
                    campos.add(a.get("tarde").alias("llego_tarde"));
                    campos.add(a.get("tolerancia").alias("tolerancia_minutos"));
                    campos.add(c.get("id").alias("id_cita"));
                    campos.add(c.get("inicio").alias("fecha_hora"));
                    campos.add(c.get("estado").alias("estado"));
                    campos.add(p.get("dni").alias("dni"));
                    campos.add(u.get("nombres").alias("nombres"));
                    campos.add(u.get("apellidos").alias("apellidos"));
                    campos.add(motivo.get("descripcion").alias("motivo"));
                    campos.add(cb.exists(reasignada).alias("reasignada"));
                    if (id != null) {
                        var cc = c.join("consultaClinica", JoinType.LEFT);
                        var m = c.join("medico");
                        var um = m.join("usuario");
                        var e = m.join("especialidad");
                        var se = m.join("sede");
                        for (String campo :
                                List.of(
                                        "antecedentes",
                                        "hallazgos",
                                        "evaluacion",
                                        "diagnostico",
                                        "plan")) campos.add(cc.get(campo).alias(campo));
                        campos.add(cc.get("motivo").alias("motivo_clinico"));
                        campos.add(c.get("paciente").alias("id_paciente"));
                        campos.add(c.get("prioridad").alias("nivel_urgencia"));
                        campos.add(c.get("recomendada").alias("fecha_recomendada_proxima"));
                        campos.add(c.get("creada").alias("fecha_creacion"));
                        campos.add(p.get("telefono").alias("telefono"));
                        campos.add(p.get("nacimiento").alias("fecha_nacimiento"));
                        campos.add(p.get("direccion").alias("direccion"));
                        campos.add(p.get("alergias").alias("alergias"));
                        campos.add(p.get("cobertura").alias("tipo_cobertura"));
                        campos.add(p.get("canal").alias("canal_aviso"));
                        campos.add(u.get("email").alias("email"));
                        campos.add(e.get("nombre").alias("especialidad"));
                        campos.add(se.get("nombre").alias("sede"));
                        campos.add(m.get("cmp").alias("cmp"));
                        campos.add(um.get("nombres").alias("medico_nombres"));
                        campos.add(um.get("apellidos").alias("medico_apellidos"));
                        campos.add(indicaciones(cb, q, c.get("id")).alias("indicaciones"));
                    }
                    var filtros = new ArrayList<Predicate>();
                    filtros.add(cb.equal(c.get("medico").get("id"), medico));
                    if (id != null) filtros.add(cb.equal(c.get("id"), id));
                    else {
                        filtros.add(cb.greaterThanOrEqualTo(c.get("inicio"), desde.atStartOfDay()));
                        filtros.add(cb.lessThan(c.get("inicio"), hasta.atStartOfDay()));
                    }
                    q.multiselect(campos)
                            .where(filtros.toArray(Predicate[]::new))
                            .orderBy(cb.asc(c.get("inicio")), cb.asc(c.get("id")));
                    return s.createQuery(q).getResultList().stream()
                            .map(FilaConsulta::new)
                            .toList();
                });
    }

    public int activa(int medico) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var a = q.from(AtencionPrioridadEntidad.class);
                    q.select(a.get("id")).where(cb.equal(a.get("medicoActivo"), medico));
                    return s.createQuery(q).setMaxResults(1).getResultList().stream()
                            .findFirst()
                            .orElse(-1);
                });
    }
}
