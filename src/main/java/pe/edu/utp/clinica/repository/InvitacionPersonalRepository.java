package pe.edu.utp.clinica.repository;

import jakarta.persistence.LockModeType;

import pe.edu.utp.clinica.dao.InvitacionPersonalDAO.Solicitud;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public final class InvitacionPersonalRepository {
    public record Persona(String correo, String rol, boolean activa) {}

    public record Invitacion(
            int usuario,
            String correo,
            boolean activa,
            LocalDateTime utilizada,
            LocalDateTime creada,
            LocalDateTime vence,
            LocalDateTime revocada,
            String envio) {}

    public record Medico(int id, int sede, int especialidad) {}

    private final Connection conexion;

    public InvitacionPersonalRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public int crearUsuario(Solicitud datos, String hash) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var u =
                            new UsuarioEntidad(
                                    datos.nombres().trim(),
                                    datos.apellidos().trim(),
                                    datos.correo().trim().toLowerCase(Locale.ROOT),
                                    hash,
                                    datos.rol(),
                                    false);
                    s.persist(u);
                    return u.id();
                });
    }

    public Persona persona(int usuario) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Persona.class);
                    var u = q.from(UsuarioEntidad.class);
                    q.select(
                                    cb.construct(
                                            Persona.class,
                                            u.get("email"),
                                            u.get("rol"),
                                            u.get("activo")))
                            .where(cb.equal(u.get("id"), usuario));
                    var consulta = s.createQuery(q);
                    if (s.getTransaction().isActive())
                        consulta.setLockMode(LockModeType.PESSIMISTIC_WRITE);
                    return consulta.getResultList().stream().findFirst().orElse(null);
                });
    }

    public Invitacion invitacion(Integer usuario, byte[] token, boolean bloquear)
            throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Invitacion.class);
                    var i = q.from(InvitacionPerfilEntidad.class);
                    var u = i.join("usuario");
                    q.select(
                            cb.construct(
                                    Invitacion.class,
                                    u.get("id"),
                                    u.get("email"),
                                    u.get("activo"),
                                    i.get("utilizada"),
                                    i.get("creada"),
                                    i.get("vence"),
                                    i.get("revocada"),
                                    i.get("envio")));
                    if (usuario != null) q.where(cb.equal(u.get("id"), usuario));
                    else
                        q.where(
                                cb.equal(i.get("token"), token),
                                u.get("rol")
                                        .in("MEDICO", "PERSONAL_ADMINISTRATIVO", "ADMINISTRADOR"));
                    var consulta = s.createQuery(q);
                    if (bloquear) consulta.setLockMode(LockModeType.PESSIMISTIC_WRITE);
                    return consulta.getResultList().stream().findFirst().orElse(null);
                });
    }

    public Map<Integer, String> estados(LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var i = q.from(InvitacionPerfilEntidad.class);
                    var u = i.join("usuario");
                    q.multiselect(i.get("id"), i.get("vence"), i.get("revocada"), i.get("envio"))
                            .where(cb.isNull(i.get("utilizada")), cb.isFalse(u.get("activo")));
                    var resultado = new HashMap<Integer, String>();
                    for (var f : s.createQuery(q).getResultList()) {
                        String estado =
                                f.get(2) != null
                                        ? "Invitación revocada"
                                        : !ahora.isBefore(f.get(1, LocalDateTime.class))
                                                ? "Invitación vencida"
                                                : "NO_CONFIRMADO".equals(f.get(3))
                                                        ? "Envío sin confirmar"
                                                        : "Pendiente de activación";
                        resultado.put(f.get(0, Integer.class), estado);
                    }
                    return Map.copyOf(resultado);
                });
    }

    public int guardarInvitacion(
            int usuario, int actor, byte[] token, boolean actualizar, LocalDateTime ahora)
            throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    if (!actualizar) {
                        s.persist(new InvitacionPerfilEntidad(usuario, actor, token, ahora));
                        s.flush();
                        return 1;
                    }
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(InvitacionPerfilEntidad.class);
                    var i = q.from(InvitacionPerfilEntidad.class);
                    q.set("token", token)
                            .set("actor", actor)
                            .set("creada", ahora)
                            .set("vence", ahora.plusMinutes(30))
                            .set(i.<LocalDateTime>get("revocada"), (LocalDateTime) null)
                            .set("envio", "PENDIENTE")
                            .where(cb.equal(i.get("id"), usuario), cb.isNull(i.get("utilizada")));
                    return s.createMutationQuery(q).executeUpdate();
                });
    }

    public void registrarEnvio(int usuario, byte[] token, boolean enviado) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(InvitacionPerfilEntidad.class);
                    var i = q.from(InvitacionPerfilEntidad.class);
                    q.set("envio", enviado ? "ENVIADO" : "NO_CONFIRMADO")
                            .where(cb.equal(i.get("id"), usuario), cb.equal(i.get("token"), token));
                    s.createMutationQuery(q).executeUpdate();
                    return null;
                });
    }

    public int activar(int usuario, String hash, LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(UsuarioEntidad.class);
                    var u = q.from(UsuarioEntidad.class);
                    q.set("passwordHash", hash)
                            .set("activo", true)
                            .where(cb.equal(u.get("id"), usuario), cb.isFalse(u.get("activo")));
                    int cambios = s.createMutationQuery(q).executeUpdate();
                    if (cambios == 1) {
                        var usada = cb.createCriteriaUpdate(InvitacionPerfilEntidad.class);
                        var i = usada.from(InvitacionPerfilEntidad.class);
                        usada.set("utilizada", ahora).where(cb.equal(i.get("id"), usuario));
                        s.createMutationQuery(usada).executeUpdate();
                    }
                    return cambios;
                });
    }

    public void actualizarUsuario(int usuario, String nombres, String apellidos, boolean activo)
            throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(UsuarioEntidad.class);
                    var u = q.from(UsuarioEntidad.class);
                    q.set("activo", activo);
                    if (nombres != null)
                        q.set("nombres", nombres.trim()).set("apellidos", apellidos.trim());
                    q.where(cb.equal(u.get("id"), usuario));
                    s.createMutationQuery(q).executeUpdate();
                    return null;
                });
    }

    public boolean activacionPendiente(int usuario) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var i = q.from(InvitacionPerfilEntidad.class);
                    q.select(i.get("id"))
                            .where(cb.equal(i.get("id"), usuario), cb.isNull(i.get("utilizada")));
                    return !s.createQuery(q).getResultList().isEmpty();
                });
    }

    public int asignarDestino(int usuario, int sede) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(InvitacionPerfilEntidad.class);
                    var i = q.from(InvitacionPerfilEntidad.class);
                    q.set("sede", s.getReference(SedeEntidad.class, sede))
                            .where(cb.equal(i.get("id"), usuario));
                    return s.createMutationQuery(q).executeUpdate();
                });
    }

    public Medico medico(int usuario, boolean bloquear) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Medico.class);
                    var m = q.from(MedicoEntidad.class);
                    q.select(
                                    cb.construct(
                                            Medico.class,
                                            m.get("id"),
                                            m.get("sede").get("id"),
                                            m.get("especialidad").get("id")))
                            .where(cb.equal(m.get("usuario").get("id"), usuario));
                    var consulta = s.createQuery(q);
                    if (bloquear) consulta.setLockMode(LockModeType.PESSIMISTIC_WRITE);
                    return consulta.getResultList().stream().findFirst().orElse(null);
                });
    }

    public boolean sedeActiva(int sede, boolean bloquear) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var se = q.from(SedeEntidad.class);
                    q.select(se.get("id"))
                            .where(
                                    cb.equal(se.get("id"), sede),
                                    cb.equal(se.get("estado"), "ACTIVO"));
                    var consulta = s.createQuery(q);
                    if (bloquear) consulta.setLockMode(LockModeType.PESSIMISTIC_WRITE);
                    return !consulta.getResultList().isEmpty();
                });
    }

    public boolean cmpOcupado(String cmp, int excluir) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var m = q.from(MedicoEntidad.class);
                    q.select(m.get("id"))
                            .where(
                                    cb.equal(m.get("cmp"), cmp),
                                    cb.notEqual(m.get("usuario").get("id"), excluir));
                    return !s.createQuery(q).getResultList().isEmpty();
                });
    }

    public boolean agendaImpideCambio(int medico, LocalDateTime ahora, boolean baja)
            throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var m = q.from(MedicoEntidad.class);
                    var citas = q.subquery(Integer.class);
                    var c = citas.from(CitaEntidad.class);
                    var vigentes =
                            baja
                                    ? cb.or(
                                            c.get("estado").in("EN_SALA", "EN_ATENCION"),
                                            cb.and(
                                                    cb.greaterThan(c.get("inicio"), ahora),
                                                    c.get("estado").in("SOLICITADA", "CONFIRMADA")))
                                    : cb.and(
                                            cb.greaterThan(c.get("inicio"), ahora),
                                            cb.not(
                                                    c.get("estado")
                                                            .in("CANCELADA", "REPROGRAMADA")));
                    citas.select(cb.literal(1))
                            .where(cb.equal(c.get("medico").get("id"), m.get("id")), vigentes);
                    var conflicto = cb.exists(citas);
                    if (!baja) {
                        var turnos = q.subquery(Integer.class);
                        var d = turnos.from(DisponibilidadEntidad.class);
                        turnos.select(cb.literal(1))
                                .where(
                                        cb.equal(d.get("medico").get("id"), m.get("id")),
                                        cb.equal(d.get("estado"), "PUBLICADO"),
                                        cb.greaterThanOrEqualTo(
                                                d.get("fecha"), ahora.toLocalDate()));
                        conflicto = cb.or(conflicto, cb.exists(turnos));
                    }
                    q.select(m.get("id")).where(cb.equal(m.get("id"), medico), conflicto);
                    return !s.createQuery(q).getResultList().isEmpty();
                });
    }

    public int actualizarPerfil(int usuario, Solicitud datos) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    if ("MEDICO".equals(datos.rol())) {
                        var q = cb.createCriteriaUpdate(MedicoEntidad.class);
                        var m = q.from(MedicoEntidad.class);
                        q.set("cmp", datos.cmp().trim())
                                .set(
                                        "especialidad",
                                        s.getReference(
                                                EspecialidadEntidad.class, datos.especialidad()))
                                .set("sede", s.getReference(SedeEntidad.class, datos.sede()))
                                .where(cb.equal(m.get("usuario").get("id"), usuario));
                        return s.createMutationQuery(q).executeUpdate();
                    }
                    if ("PERSONAL_ADMINISTRATIVO".equals(datos.rol())) {
                        var q = cb.createCriteriaUpdate(PersonalEntidad.class);
                        var p = q.from(PersonalEntidad.class);
                        q.set("sede", s.getReference(SedeEntidad.class, datos.sede()))
                                .set("cargo", cargo(datos))
                                .where(cb.equal(p.get("usuario").get("id"), usuario));
                        return s.createMutationQuery(q).executeUpdate();
                    }
                    return 1;
                });
    }

    public void crearPerfil(int usuario, Solicitud datos) throws SQLException {
        if ("MEDICO".equals(datos.rol())) {
            new PerfilProfesionalRepository(conexion)
                    .crear(usuario, datos.cmp().trim(), datos.especialidad(), datos.sede());
            return;
        }
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var u = s.getReference(UsuarioEntidad.class, usuario);
                    if ("PERSONAL_ADMINISTRATIVO".equals(datos.rol()))
                        s.persist(
                                new PersonalEntidad(
                                        u,
                                        s.getReference(SedeEntidad.class, datos.sede()),
                                        "ADM-" + usuario,
                                        cargo(datos)));
                    else s.persist(new AdministradorEntidad(u));
                    return null;
                });
    }

    private static String cargo(Solicitud datos) {
        return datos.cargo() == null || datos.cargo().isBlank()
                ? "Personal de admisión"
                : datos.cargo().trim();
    }

    public void revocar(int usuario, LocalDateTime ahora) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(InvitacionPerfilEntidad.class);
                    var i = q.from(InvitacionPerfilEntidad.class);
                    q.set("revocada", ahora)
                            .where(cb.equal(i.get("id"), usuario), cb.isNull(i.get("utilizada")));
                    s.createMutationQuery(q).executeUpdate();
                    return null;
                });
    }
}
