package pe.edu.utp.clinica.repository;

import jakarta.persistence.criteria.*;

import pe.edu.utp.clinica.dao.SeguimientoOfertasDAO;
import pe.edu.utp.clinica.dao.SeguimientoOfertasDAO.Especialidad;
import pe.edu.utp.clinica.dao.SeguimientoOfertasDAO.Filtro;
import pe.edu.utp.clinica.dao.SeguimientoOfertasDAO.Oferta;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.OfertaEntidad;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public final class SeguimientoOfertasRepository {
    private final Connection conexion;

    public SeguimientoOfertasRepository(Connection conexion) {
        this.conexion = conexion;
    }

    private record Origen(
            Root<OfertaEntidad> oferta,
            Join<?, ?> cita,
            Join<?, ?> medico,
            Join<?, ?> especialidad,
            Join<?, ?> usuarioMedico,
            Join<?, ?> usuarioPaciente,
            List<Predicate> filtros) {}

    private static Origen origen(CriteriaBuilder cb, CriteriaQuery<?> q, int sede, Filtro filtro) {
        var r = q.from(OfertaEntidad.class);
        var c = r.join("cita");
        var d = r.join("disponibilidad");
        var m = c.join("medico");
        var um = m.join("usuario");
        var e = m.join("especialidad");
        var up = r.join("lista").join("paciente").join("usuario");
        var filtros = new ArrayList<Predicate>();
        filtros.add(cb.equal(d.get("sede"), sede));
        if (filtro != null) {
            if (!filtro.estado().isEmpty()) filtros.add(cb.equal(r.get("estado"), filtro.estado()));
            if (filtro.fecha() != null) {
                filtros.add(
                        cb.greaterThanOrEqualTo(r.get("creada"), filtro.fecha().atStartOfDay()));
                filtros.add(
                        cb.lessThan(r.get("creada"), filtro.fecha().plusDays(1).atStartOfDay()));
            }
            if (filtro.especialidad() > 0)
                filtros.add(cb.equal(e.get("id"), filtro.especialidad()));
        }
        return new Origen(r, c, m, e, um, up, filtros);
    }

    public Map<String, Long> contar(int sede, Filtro filtro) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var o = origen(cb, q, sede, filtro);
                    var r = o.oferta();
                    q.multiselect(r.get("estado"), cb.count(r))
                            .where(o.filtros().toArray(Predicate[]::new))
                            .groupBy(r.get("estado"));
                    var resultado = new LinkedHashMap<String, Long>();
                    SeguimientoOfertasDAO.ESTADOS.forEach(estado -> resultado.put(estado, 0L));
                    for (var f : s.createQuery(q).getResultList())
                        resultado.put(
                                Objects.requireNonNullElse(f.get(0, String.class), "SIN_ESTADO"),
                                f.get(1, Long.class));
                    return resultado;
                });
    }

    public List<Oferta> listar(int sede, Filtro filtro, int pagina, LocalDateTime ahora)
            throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var o = origen(cb, q, sede, filtro);
                    var r = o.oferta();
                    q.multiselect(
                                    r.get("id"),
                                    o.cita().get("id"),
                                    r.get("lista").get("id"),
                                    cb.concat(
                                            cb.concat(o.usuarioPaciente().get("nombres"), " "),
                                            o.usuarioPaciente().get("apellidos")),
                                    cb.concat(
                                            cb.concat(o.usuarioMedico().get("nombres"), " "),
                                            o.usuarioMedico().get("apellidos")),
                                    o.especialidad().get("nombre"),
                                    o.cita().get("inicio"),
                                    r.get("creada"),
                                    r.get("vence"),
                                    r.get("estado"),
                                    r.get("asignada"))
                            .where(o.filtros().toArray(Predicate[]::new))
                            .orderBy(cb.desc(r.get("creada")), cb.desc(r.get("id")));
                    var resultado = new ArrayList<Oferta>();
                    for (var f :
                            s.createQuery(q)
                                    .setMaxResults(SeguimientoOfertasDAO.POR_PAGINA)
                                    .setFirstResult((pagina - 1) * SeguimientoOfertasDAO.POR_PAGINA)
                                    .getResultList()) {
                        var limite = f.get(8, LocalDateTime.class);
                        var estado =
                                Objects.requireNonNullElse(f.get(9, String.class), "SIN_ESTADO");
                        resultado.add(
                                new Oferta(
                                        f.get(0, Integer.class),
                                        f.get(1, Integer.class),
                                        f.get(2, Integer.class),
                                        f.get(3, String.class),
                                        f.get(4, String.class),
                                        f.get(5, String.class),
                                        f.get(6, LocalDateTime.class),
                                        f.get(7, LocalDateTime.class),
                                        limite,
                                        estado,
                                        f.get(10, Integer.class),
                                        "PENDIENTE".equals(estado)
                                                && limite != null
                                                && !ahora.isBefore(limite)));
                    }
                    return resultado;
                });
    }

    public List<Especialidad> especialidades(int sede) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Especialidad.class);
                    var o = origen(cb, q, sede, null);
                    var e = o.especialidad();
                    q.select(cb.construct(Especialidad.class, e.get("id"), e.get("nombre")))
                            .distinct(true)
                            .where(o.filtros().toArray(Predicate[]::new))
                            .orderBy(cb.asc(e.get("nombre")), cb.asc(e.get("id")));
                    return s.createQuery(q).getResultList();
                });
    }
}
