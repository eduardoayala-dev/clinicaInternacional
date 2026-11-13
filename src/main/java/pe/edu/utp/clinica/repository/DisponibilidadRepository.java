package pe.edu.utp.clinica.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.DisponibilidadEntidad;
import pe.edu.utp.clinica.persistence.entity.CitaEntidad;
import pe.edu.utp.clinica.persistence.entity.ListaEsperaEntidad;
import pe.edu.utp.clinica.persistence.entity.OfertaEntidad;
import pe.edu.utp.clinica.persistence.entity.RetencionEntidad;

public final class DisponibilidadRepository {
    public record Agenda(int medico, LocalDate fecha) {}
    public record Turno(int medico, LocalDate fecha, LocalTime inicio, LocalTime fin, int minutos) {}
    public record Ocupacion(int medico, LocalDateTime inicio, int minutos) {}

    private final Connection conexion;

    public DisponibilidadRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public List<Agenda> agendasSemana(int especialidad, Integer sede, Integer medico,
            LocalDate desde, LocalDate hoy) throws SQLException {
        return PersistenciaJpa.consultar(conexion, sesion -> {
            var cb = sesion.getCriteriaBuilder();
            var consulta = cb.createQuery(Agenda.class);
            var turno = consulta.from(DisponibilidadEntidad.class);
            var profesional = turno.join("medico");
            var condiciones = publicados(cb, turno, profesional, sede == null ? 0 : sede, desde, desde.plusDays(7));
            condiciones.add(cb.equal(profesional.get("especialidad").get("id"), especialidad));
            condiciones.add(cb.greaterThanOrEqualTo(turno.get("fecha"), hoy));
            if (medico != null && medico != 0) condiciones.add(cb.equal(profesional.get("id"), medico));
            consulta.select(cb.construct(Agenda.class, profesional.get("id"), turno.get("fecha")))
                    .distinct(true).where(condiciones.toArray(Predicate[]::new))
                    .orderBy(cb.asc(turno.get("fecha")), cb.asc(profesional.get("id")));
            return sesion.createQuery(consulta).getResultList();
        });
    }

    public List<Turno> turnosPublicados(int sede, LocalDate desde, LocalDate hasta) throws SQLException {
        return turnosPublicados(sede, desde, hasta, null);
    }

    public List<Turno> turnosMedico(int medico, LocalDate fecha) throws SQLException {
        return turnosPublicados(0, fecha, fecha.plusDays(1), medico);
    }

    public List<Ocupacion> ocupacionesReserva(int medico, LocalDate fecha, LocalDateTime ahora, int excluirOferta) throws SQLException {
        var resultado = new ArrayList<Ocupacion>();
        resultado.addAll(citasOcupadas(0, fecha, fecha.plusDays(1), medico));
        resultado.addAll(ofertasVigentes(0, fecha, fecha.plusDays(1), ahora, medico, excluirOferta));
        return List.copyOf(resultado);
    }

    public List<Ocupacion> retencionesMedico(int medico, LocalDate fecha, LocalDateTime ahora) throws SQLException {
        return retencionesVigentes(0, fecha, fecha.plusDays(1), ahora, medico);
    }

    public List<LocalDate> fechasMedico(int medico, LocalDate hoy) throws SQLException {
        return PersistenciaJpa.consultar(conexion, sesion -> {
            var cb = sesion.getCriteriaBuilder();
            var consulta = cb.createQuery(LocalDate.class);
            var turno = consulta.from(DisponibilidadEntidad.class);
            var profesional = turno.join("medico");
            var condiciones = publicados(cb, turno, profesional, 0, hoy, null);
            condiciones.add(cb.equal(profesional.get("id"), medico));
            consulta.select(turno.get("fecha")).distinct(true)
                    .where(condiciones.toArray(Predicate[]::new)).orderBy(cb.asc(turno.get("fecha")));
            return sesion.createQuery(consulta).getResultList();
        });
    }

    public List<Ocupacion> ocupacionesMedico(int medico, LocalDate fecha, LocalDateTime ahora) throws SQLException {
        var resultado = new ArrayList<Ocupacion>();
        resultado.addAll(citasOcupadas(0, fecha, fecha.plusDays(1), medico));
        resultado.addAll(ofertasVigentes(0, fecha, fecha.plusDays(1), ahora, medico, 0));
        resultado.addAll(retencionesVigentes(0, fecha, fecha.plusDays(1), ahora, medico));
        return List.copyOf(resultado);
    }

    private List<Turno> turnosPublicados(int sede, LocalDate desde, LocalDate hasta, Integer idMedico) throws SQLException {
        return PersistenciaJpa.consultar(conexion, sesion -> {
            var cb = sesion.getCriteriaBuilder();
            var consulta = cb.createQuery(Turno.class);
            var turno = consulta.from(DisponibilidadEntidad.class);
            var medico = turno.join("medico");
            var condiciones = publicados(cb, turno, medico, sede, desde, hasta);
            filtrarMedico(cb, medico, idMedico, condiciones);
            consulta.select(cb.construct(Turno.class, medico.get("id"), turno.get("fecha"),
                    turno.get("inicio"), turno.get("fin"), turno.get("minutos")))
                    .where(condiciones.toArray(Predicate[]::new));
            return sesion.createQuery(consulta).getResultList();
        });
    }

    public List<Ocupacion> citasOcupadas(int sede, LocalDate desde, LocalDate hasta) throws SQLException {
        return citasOcupadas(sede, desde, hasta, null);
    }

    private List<Ocupacion> citasOcupadas(int sede, LocalDate desde, LocalDate hasta, Integer idMedico) throws SQLException {
        return PersistenciaJpa.consultar(conexion, sesion -> {
            var cb = sesion.getCriteriaBuilder();
            var consulta = cb.createQuery(Ocupacion.class);
            var cita = consulta.from(CitaEntidad.class);
            var medico = cita.join("medico");
            var turno = cita.join("disponibilidad");
            var condiciones = rangoHoras(cb, cita, medico, sede, desde, hasta);
            filtrarMedico(cb, medico, idMedico, condiciones);
            condiciones.add(cb.not(cita.get("estado").in("CANCELADA", "REPROGRAMADA")));
            consulta.select(cb.construct(Ocupacion.class, medico.get("id"), cita.get("inicio"), turno.get("minutos")))
                    .where(condiciones.toArray(Predicate[]::new));
            return sesion.createQuery(consulta).getResultList();
        });
    }

    public List<Ocupacion> ofertasVigentes(int sede, LocalDate desde, LocalDate hasta,
            LocalDateTime ahora) throws SQLException {
        return ofertasVigentes(sede, desde, hasta, ahora, null, 0);
    }

    private List<Ocupacion> ofertasVigentes(int sede, LocalDate desde, LocalDate hasta,
            LocalDateTime ahora, Integer idMedico, int excluirOferta) throws SQLException {
        return PersistenciaJpa.consultar(conexion, sesion -> {
            var cb = sesion.getCriteriaBuilder();
            var consulta = cb.createQuery(Ocupacion.class);
            var oferta = consulta.from(OfertaEntidad.class);
            var cita = oferta.join("cita");
            var medico = cita.join("medico");
            var turno = oferta.join("disponibilidad");
            var condiciones = rangoHoras(cb, cita, medico, sede, desde, hasta);
            filtrarMedico(cb, medico, idMedico, condiciones);
            condiciones.add(cb.equal(oferta.get("estado"), "PENDIENTE"));
            if (excluirOferta != 0) condiciones.add(cb.notEqual(oferta.get("id"), excluirOferta));
            condiciones.add(cb.greaterThan(oferta.get("vence"), ahora));
            condiciones.add(cb.greaterThan(cita.get("inicio"), ahora));
            condiciones.add(cb.equal(cita.get("estado"), "CANCELADA"));
            condiciones.add(cb.equal(turno.get("estado"), "PUBLICADO"));
            condiciones.add(cb.equal(turno.get("sede"), medico.get("sede").get("id")));
            var elegible = consulta.subquery(Integer.class);
            var lista = elegible.from(ListaEsperaEntidad.class);
            var usuario = lista.join("paciente").join("usuario");
            elegible.select(cb.literal(1)).where(cb.equal(lista.get("id"), oferta.get("lista").get("id")),
                    cb.equal(lista.get("estado"), "OFERTADA"), cb.isTrue(usuario.get("activo")),
                    cb.equal(usuario.get("rol"), "PACIENTE"));
            condiciones.add(cb.exists(elegible));
            consulta.select(cb.construct(Ocupacion.class, medico.get("id"), cita.get("inicio"), turno.get("minutos")))
                    .where(condiciones.toArray(Predicate[]::new));
            return sesion.createQuery(consulta).getResultList();
        });
    }

    public List<Ocupacion> retencionesVigentes(int sede, LocalDate desde, LocalDate hasta,
            LocalDateTime ahora) throws SQLException {
        return retencionesVigentes(sede, desde, hasta, ahora, null);
    }

    private List<Ocupacion> retencionesVigentes(int sede, LocalDate desde, LocalDate hasta,
            LocalDateTime ahora, Integer idMedico) throws SQLException {
        return PersistenciaJpa.consultar(conexion, sesion -> {
            var cb = sesion.getCriteriaBuilder();
            var consulta = cb.createQuery(Ocupacion.class);
            var retencion = consulta.from(RetencionEntidad.class);
            var medico = retencion.join("medico");
            var condiciones = rangoHoras(cb, retencion, medico, sede, desde, hasta);
            filtrarMedico(cb, medico, idMedico, condiciones);
            condiciones.add(cb.greaterThan(retencion.get("vence"), ahora));
            consulta.select(cb.construct(Ocupacion.class, medico.get("id"), retencion.get("inicio"), retencion.get("minutos")))
                    .where(condiciones.toArray(Predicate[]::new));
            return sesion.createQuery(consulta).getResultList();
        });
    }

    private static void filtrarMedico(CriteriaBuilder cb, From<?, ?> medico, Integer id, List<Predicate> condiciones) {
        if (id != null) condiciones.add(cb.equal(medico.get("id"), id));
    }

    private static List<Predicate> publicados(CriteriaBuilder cb, From<?, ?> turno,
            From<?, ?> medico, int sede, LocalDate desde, LocalDate hasta) {
        var usuario = medico.join("usuario");
        var especialidad = medico.join("especialidad");
        var clinica = medico.join("sede");
        var condiciones = new ArrayList<Predicate>();
        condiciones.add(cb.greaterThanOrEqualTo(turno.get("fecha"), desde));
        if (hasta != null) condiciones.add(cb.lessThan(turno.get("fecha"), hasta));
        condiciones.add(cb.equal(turno.get("estado"), "PUBLICADO"));
        condiciones.add(cb.equal(turno.get("sede"), clinica.get("id")));
        condiciones.add(cb.equal(medico.get("estado"), "ACTIVO"));
        condiciones.add(cb.isTrue(usuario.get("activo")));
        condiciones.add(cb.equal(especialidad.get("estado"), "ACTIVO"));
        condiciones.add(cb.equal(clinica.get("estado"), "ACTIVO"));
        if (sede != 0) condiciones.add(cb.equal(clinica.get("id"), sede));
        return condiciones;
    }

    private static List<Predicate> rangoHoras(CriteriaBuilder cb, From<?, ?> registro,
            From<?, ?> medico, int sede, LocalDate desde, LocalDate hasta) {
        var condiciones = new ArrayList<Predicate>();
        condiciones.add(cb.greaterThanOrEqualTo(registro.get("inicio"), desde.atStartOfDay()));
        condiciones.add(cb.lessThan(registro.get("inicio"), hasta.atStartOfDay()));
        if (sede != 0) condiciones.add(cb.equal(medico.get("sede").get("id"), sede));
        return condiciones;
    }
}
