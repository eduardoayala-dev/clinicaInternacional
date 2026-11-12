package pe.edu.utp.clinica.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.AtencionPrioridadEntidad;
import pe.edu.utp.clinica.persistence.entity.AuditoriaEntidad;
import pe.edu.utp.clinica.persistence.entity.CitaBloqueoEntidad;
import pe.edu.utp.clinica.persistence.entity.CitaEntidad;
import pe.edu.utp.clinica.persistence.entity.ConfiguracionCitaEntidad;
import pe.edu.utp.clinica.persistence.entity.ConfirmacionEntidad;
import pe.edu.utp.clinica.persistence.entity.CorreoCambioEntidad;
import pe.edu.utp.clinica.persistence.entity.CorreoCitaEntidad;
import pe.edu.utp.clinica.persistence.entity.DisponibilidadEntidad;
import pe.edu.utp.clinica.persistence.entity.MedicoBloqueoEntidad;
import pe.edu.utp.clinica.persistence.entity.MedicoEntidad;
import pe.edu.utp.clinica.persistence.entity.MotivoCitaEntidad;
import pe.edu.utp.clinica.persistence.entity.NotificacionEntidad;
import pe.edu.utp.clinica.persistence.entity.PacienteBloqueoEntidad;
import pe.edu.utp.clinica.persistence.entity.RetencionEntidad;
import pe.edu.utp.clinica.persistence.entity.SeguimientoEntidad;
import pe.edu.utp.clinica.persistence.entity.UsuarioEntidad;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.BiFunction;

public final class ReservaCitaRepository {
    public record Retencion(String identificador, LocalDateTime vence) {}

    private final Connection conexion;

    public ReservaCitaRepository(Connection conexion) {
        this.conexion = conexion;
    }

    private <E> List<E> bloquear(
            Class<E> tipo, BiFunction<CriteriaBuilder, Root<E>, Predicate> filtro)
            throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var consulta = cb.createQuery(tipo);
                    var entidad = consulta.from(tipo);
                    consulta.select(entidad)
                            .where(filtro.apply(cb, entidad))
                            .orderBy(cb.asc(entidad.get("id")));
                    return sesion.createQuery(consulta)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .getResultList();
                });
    }

    public PacienteBloqueoEntidad paciente(int id) throws SQLException {
        return bloquear(PacienteBloqueoEntidad.class, (cb, p) -> cb.equal(p.get("id"), id)).stream()
                .findFirst()
                .orElse(null);
    }

    public MedicoBloqueoEntidad medico(int id) throws SQLException {
        return bloquear(MedicoBloqueoEntidad.class, (cb, m) -> cb.equal(m.get("id"), id)).stream()
                .findFirst()
                .orElse(null);
    }

    public boolean usuarioActivo(int id, String rol) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var consulta = cb.createQuery(Integer.class);
                    var usuario = consulta.from(UsuarioEntidad.class);
                    consulta.select(usuario.get("id"))
                            .where(
                                    cb.equal(usuario.get("id"), id),
                                    cb.isTrue(usuario.get("activo")),
                                    cb.equal(usuario.get("rol"), rol));
                    return !sesion.createQuery(consulta).setMaxResults(1).getResultList().isEmpty();
                });
    }

    public boolean medicoActivo(int id) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var consulta = cb.createQuery(Integer.class);
                    var medico = consulta.from(MedicoEntidad.class);
                    var usuario = medico.join("usuario");
                    consulta.select(medico.get("id"))
                            .where(
                                    cb.equal(medico.get("id"), id),
                                    cb.isTrue(usuario.get("activo")),
                                    cb.equal(usuario.get("rol"), "MEDICO"),
                                    cb.equal(medico.join("sede").get("estado"), "ACTIVO"),
                                    cb.equal(medico.join("especialidad").get("estado"), "ACTIVO"));
                    return !sesion.createQuery(consulta).setMaxResults(1).getResultList().isEmpty();
                });
    }

    public List<DisponibilidadEntidad> bloques(int medico, int sede, LocalDate fecha)
            throws SQLException {
        return bloquear(
                DisponibilidadEntidad.class,
                (cb, d) ->
                        cb.and(
                                cb.equal(d.get("medico").get("id"), medico),
                                        cb.equal(d.get("sede"), sede),
                                cb.equal(d.get("fecha"), fecha),
                                        cb.equal(d.get("estado"), "PUBLICADO")));
    }

    public DisponibilidadEntidad bloque(int id) throws SQLException {
        return bloquear(DisponibilidadEntidad.class, (cb, d) -> cb.equal(d.get("id"), id)).stream()
                .findFirst()
                .orElse(null);
    }

    public Integer medicoDeBloque(int id) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var consulta = cb.createQuery(Integer.class);
                    var bloque = consulta.from(DisponibilidadEntidad.class);
                    consulta.select(bloque.get("medico").get("id"))
                            .where(cb.equal(bloque.get("id"), id));
                    return sesion.createQuery(consulta).getResultList().stream()
                            .findFirst()
                            .orElse(null);
                });
    }

    public int publicar(
            int medico, int sede, LocalDate fecha, LocalTime inicio, LocalTime fin, int minutos)
            throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                sesion -> {
                    var bloque =
                            new DisponibilidadEntidad(
                                    sesion.getReference(MedicoEntidad.class, medico),
                                    sede,
                                    fecha,
                                    inicio,
                                    fin,
                                    minutos);
                    sesion.persist(bloque);
                    return bloque.id();
                });
    }

    public boolean solapamiento(
            int medico, LocalDate fecha, LocalTime inicio, LocalTime fin, int excluir)
            throws SQLException {
        return !bloquear(
                        DisponibilidadEntidad.class,
                        (cb, d) ->
                                cb.and(
                                        cb.equal(d.get("medico").get("id"), medico),
                                                cb.equal(d.get("fecha"), fecha),
                                        cb.equal(d.get("estado"), "PUBLICADO"),
                                                cb.lessThan(d.get("inicio"), fin),
                                        cb.greaterThan(d.get("fin"), inicio),
                                                cb.notEqual(d.get("id"), excluir)))
                .isEmpty();
    }

    public boolean bloqueConCitas(int id, LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var consulta = cb.createQuery(Integer.class);
                    var cita = consulta.from(CitaEntidad.class);
                    consulta.select(cita.get("id"))
                            .where(
                                    cb.equal(cita.get("disponibilidad").get("id"), id),
                                    cb.greaterThan(cita.get("inicio"), ahora),
                                    cb.not(cita.get("estado").in("CANCELADA", "REPROGRAMADA")));
                    return !sesion.createQuery(consulta).setMaxResults(1).getResultList().isEmpty();
                });
    }

    public Retencion retencion(int paciente, int medico, LocalDateTime inicio, LocalDateTime ahora)
            throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var consulta = cb.createQuery(Retencion.class);
                    var retencion = consulta.from(RetencionEntidad.class);
                    consulta.select(
                                    cb.construct(
                                            Retencion.class,
                                            retencion.get("identificador"),
                                            retencion.get("vence")))
                            .where(
                                    cb.equal(retencion.get("id"), paciente),
                                    cb.equal(retencion.get("medico").get("id"), medico),
                                    cb.equal(retencion.get("inicio"), inicio),
                                    cb.greaterThan(retencion.get("vence"), ahora));
                    return sesion.createQuery(consulta).getResultList().stream()
                            .findFirst()
                            .orElse(null);
                });
    }

    public void limpiarRetenciones(int paciente, int medico, LocalDateTime ahora)
            throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var borrar = cb.createCriteriaDelete(RetencionEntidad.class);
                    var r = borrar.from(RetencionEntidad.class);
                    borrar.where(
                            cb.or(
                                    cb.equal(r.get("id"), paciente),
                                    cb.and(
                                            cb.equal(r.get("medico").get("id"), medico),
                                            cb.lessThanOrEqualTo(r.get("vence"), ahora))));
                    return sesion.createMutationQuery(borrar).executeUpdate();
                });
    }

    public int liberarRetencion(
            int paciente,
            String identificador,
            Integer medico,
            LocalDateTime inicio,
            LocalDateTime ahora)
            throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var borrar = cb.createCriteriaDelete(RetencionEntidad.class);
                    var r = borrar.from(RetencionEntidad.class);
                    var condiciones = new java.util.ArrayList<Predicate>();
                    condiciones.add(cb.equal(r.get("id"), paciente));
                    condiciones.add(cb.equal(r.get("identificador"), identificador));
                    if (medico != null)
                        condiciones.add(cb.equal(r.get("medico").get("id"), medico));
                    if (inicio != null) condiciones.add(cb.equal(r.get("inicio"), inicio));
                    if (ahora != null) condiciones.add(cb.greaterThan(r.get("vence"), ahora));
                    borrar.where(condiciones.toArray(Predicate[]::new));
                    return sesion.createMutationQuery(borrar).executeUpdate();
                });
    }

    public void retener(
            int paciente,
            int medico,
            LocalDateTime inicio,
            int minutos,
            String identificador,
            LocalDateTime vence)
            throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                sesion -> {
                    sesion.persist(
                            new RetencionEntidad(
                                    paciente,
                                    sesion.getReference(MedicoEntidad.class, medico),
                                    inicio,
                                    minutos,
                                    identificador,
                                    vence));
                    sesion.flush();
                    return null;
                });
    }

    public CitaBloqueoEntidad anterior(int cita, int paciente) throws SQLException {
        return bloquear(
                        CitaBloqueoEntidad.class,
                        (cb, c) ->
                                cb.and(
                                        cb.equal(c.get("id"), cita),
                                        cb.equal(c.get("paciente"), paciente)))
                .stream()
                .findFirst()
                .orElse(null);
    }

    public int estado(
            int cita,
            Integer paciente,
            String estado,
            List<String> anteriores,
            LocalDateTime futura)
            throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var cambio = cb.createCriteriaUpdate(CitaEntidad.class);
                    var c = cambio.from(CitaEntidad.class);
                    var condiciones = new java.util.ArrayList<Predicate>();
                    condiciones.add(cb.equal(c.get("id"), cita));
                    if (paciente != null) condiciones.add(cb.equal(c.get("paciente"), paciente));
                    if (anteriores != null) condiciones.add(c.get("estado").in(anteriores));
                    if (futura != null) condiciones.add(cb.greaterThan(c.get("inicio"), futura));
                    cambio.set("estado", estado).where(condiciones.toArray(Predicate[]::new));
                    return sesion.createMutationQuery(cambio).executeUpdate();
                });
    }

    public boolean motivoValido(int motivo, int especialidad) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var consulta = cb.createQuery(Integer.class);
                    var m = consulta.from(MotivoCitaEntidad.class);
                    consulta.select(m.get("id"))
                            .where(
                                    cb.equal(m.get("id"), motivo),
                                    cb.equal(m.get("especialidad").get("id"), especialidad),
                                    cb.equal(m.get("estado"), "ACTIVO"));
                    return !sesion.createQuery(consulta).setMaxResults(1).getResultList().isEmpty();
                });
    }

    public int crearCita(
            int paciente,
            int medico,
            int bloque,
            LocalDateTime inicio,
            Integer motivo,
            String prioridad,
            String riesgo)
            throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                sesion -> {
                    var cita =
                            new CitaEntidad(
                                    paciente,
                                    sesion.getReference(MedicoEntidad.class, medico),
                                    sesion.getReference(DisponibilidadEntidad.class, bloque),
                                    inicio,
                                    motivo,
                                    prioridad,
                                    riesgo);
                    sesion.persist(cita);
                    return cita.id();
                });
    }

    public void confirmar(int cita, LocalDateTime creada, LocalDateTime vence) throws SQLException {
        guardar(new ConfirmacionEntidad(cita, creada, vence));
    }

    public void notificar(int usuario, int cita, String mensaje, LocalDateTime ahora)
            throws SQLException {
        guardar(new NotificacionEntidad(usuario, cita, mensaje, ahora));
    }

    public void correoReserva(int cita, java.time.Instant ahora) throws SQLException {
        guardar(
                new CorreoCitaEntidad(
                        cita, LocalDateTime.ofInstant(ahora, java.time.ZoneOffset.UTC)));
    }

    public void correoCambio(int cita, Integer anterior, String tipo, java.time.Instant ahora)
            throws SQLException {
        guardar(
                new CorreoCambioEntidad(
                        cita,
                        anterior,
                        tipo,
                        LocalDateTime.ofInstant(ahora, java.time.ZoneOffset.UTC)));
    }

    private void guardar(Object entidad) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                sesion -> {
                    sesion.persist(entidad);
                    sesion.flush();
                    return null;
                });
    }

    public long canceladas(int paciente) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var consulta = cb.createQuery(Long.class);
                    var cita = consulta.from(CitaEntidad.class);
                    consulta.select(cb.count(cita))
                            .where(
                                    cb.equal(cita.get("paciente"), paciente),
                                    cb.equal(cita.get("estado"), "CANCELADA"));
                    return sesion.createQuery(consulta).getSingleResult();
                });
    }

    public Integer horasConfirmacion() throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var consulta = cb.createQuery(Integer.class);
                    var configuracion = consulta.from(ConfiguracionCitaEntidad.class);
                    consulta.select(configuracion.get("confirmacionHoras"))
                            .where(cb.equal(configuracion.get("id"), 1));
                    return sesion.createQuery(consulta).getResultList().stream()
                            .findFirst()
                            .orElse(null);
                });
    }

    public String prioridad(int paciente, int especialidad, LocalDate fecha) throws SQLException {
        String evaluada = prioridadEvaluada(paciente, especialidad, fecha);
        return evaluada == null ? "MEDIA" : evaluada;
    }

    public String prioridadEvaluada(int paciente, int especialidad, LocalDate fecha)
            throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var consulta = cb.createQuery(String.class);
                    var cita = consulta.from(CitaEntidad.class);
                    var atencion = consulta.from(AtencionPrioridadEntidad.class);
                    var controles = consulta.subquery(Integer.class);
                    var seguimiento = controles.from(SeguimientoEntidad.class);
                    var control = seguimiento.join("control");
                    controles
                            .select(cb.literal(1))
                            .where(
                                    cb.equal(seguimiento.get("id"), cita.get("id")),
                                    cb.equal(control.get("paciente"), cita.get("paciente")),
                                    cb.equal(control.get("estado"), "ATENDIDA"));
                    var vigente =
                            cb.and(
                                    cb.or(
                                            cb.isNull(cita.get("recomendada")),
                                            cb.greaterThanOrEqualTo(
                                                    cita.get("recomendada"), fecha)),
                                    cb.not(cb.exists(controles)));
                    consulta.select(
                                    cb.<String>selectCase()
                                            .when(vigente, cita.<String>get("prioridad"))
                                            .otherwise(cb.nullLiteral(String.class)))
                            .where(
                                    cb.equal(atencion.get("id"), cita.get("id")),
                                    cb.equal(atencion.get("medico"), cita.get("medico").get("id")),
                                    cb.equal(cita.get("estado"), "ATENDIDA"),
                                    cb.isNotNull(atencion.get("fin")),
                                    cita.get("prioridad").in("ALTA", "MEDIA", "BAJA"),
                                    cb.equal(cita.get("paciente"), paciente),
                                    cb.equal(atencion.get("especialidad"), especialidad))
                            .orderBy(cb.desc(atencion.get("fin")), cb.desc(cita.get("id")));
                    var resultado = sesion.createQuery(consulta).setMaxResults(1).getResultList();
                    return resultado.isEmpty() ? null : resultado.get(0);
                });
    }

    public Integer sedeCita(int id) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var consulta = cb.createQuery(Integer.class);
                    var cita = consulta.from(CitaEntidad.class);
                    consulta.select(cita.join("medico").get("sede").get("id"))
                            .where(cb.equal(cita.get("id"), id));
                    return sesion.createQuery(consulta).getResultList().stream()
                            .findFirst()
                            .orElse(null);
                });
    }

    public void auditarCancelacion(int cita, int paciente, String motivo) throws SQLException {
        var usuarios =
                PersistenciaJpa.consultar(
                        conexion,
                        sesion -> {
                            var cb = sesion.getCriteriaBuilder();
                            var consulta = cb.createQuery(Integer.class);
                            var p = consulta.from(PacienteBloqueoEntidad.class);
                            consulta.select(p.get("usuario"))
                                    .where(cb.equal(p.get("id"), paciente));
                            return sesion.createQuery(consulta).getResultList();
                        });
        if (usuarios.size() != 1)
            throw new SQLException("No se pudo registrar el motivo de cancelación.");
        guardar(
                new AuditoriaEntidad(
                        usuarios.get(0),
                        "CITA_CANCELADA_PACIENTE",
                        "Cita #" + cita + "; motivo: " + motivo));
    }
}
