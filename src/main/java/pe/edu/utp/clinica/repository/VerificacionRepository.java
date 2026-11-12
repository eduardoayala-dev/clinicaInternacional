package pe.edu.utp.clinica.repository;

import jakarta.persistence.LockModeType;

import pe.edu.utp.clinica.auth.SecretosSesion;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;
import pe.edu.utp.clinica.recuperacion.VerificacionJdbc.Proposito;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;

public final class VerificacionRepository {
    public record Cuenta(int id, String correo, String password, String rol) {
        @Override
        public String toString() {
            return "Cuenta[privada]";
        }
    }

    private final Connection conexion;

    public VerificacionRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public Cuenta buscar(String identificador, boolean personal, Integer id) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Cuenta.class);
                    var u = q.from(UsuarioEntidad.class);
                    var filtros = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                    filtros.add(cb.isTrue(u.get("activo")));
                    if (id != null) filtros.add(cb.equal(u.get("id"), id));
                    else if (personal) {
                        filtros.add(cb.equal(cb.lower(u.get("email")), identificador));
                        filtros.add(
                                u.get("rol")
                                        .in("MEDICO", "PERSONAL_ADMINISTRATIVO", "ADMINISTRADOR"));
                    } else {
                        filtros.add(cb.equal(u.join("pacientes").get("dni"), identificador));
                        filtros.add(cb.equal(u.get("rol"), "PACIENTE"));
                    }
                    q.select(
                                    cb.construct(
                                            Cuenta.class,
                                            u.get("id"),
                                            u.get("email"),
                                            u.get("passwordHash"),
                                            u.get("rol")))
                            .where(filtros.toArray(jakarta.persistence.criteria.Predicate[]::new));
                    return s
                            .createQuery(q)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .getResultList()
                            .stream()
                            .findFirst()
                            .orElse(null);
                });
    }

    public Integer usuarioReto(String id) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var v = q.from(VerificacionEntidad.class);
                    q.select(v.get("usuario")).where(cb.equal(v.get("id"), id));
                    return s.createQuery(q).getResultList().stream().findFirst().orElse(null);
                });
    }

    public VerificacionEntidad bloquear(String id) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> s.find(VerificacionEntidad.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    public void crear(VerificacionEntidad reto) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    s.persist(reto);
                    return null;
                });
    }

    public void invalidar(int usuario, Proposito proposito) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(VerificacionEntidad.class);
                    var v = q.from(VerificacionEntidad.class);
                    q.set("etapa", "BLOQUEADO")
                            .set(v.<byte[]>get("codigo"), (byte[]) null)
                            .where(
                                    cb.equal(v.get("usuario"), usuario),
                                    cb.equal(v.get("proposito"), proposito.name()),
                                    cb.not(v.get("etapa").in("FINALIZADO", "BLOQUEADO")));
                    s.createMutationQuery(q).executeUpdate();
                    return null;
                });
    }

    public void cambiar(int usuario, String valor, boolean correo) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(UsuarioEntidad.class);
                    var u = q.from(UsuarioEntidad.class);
                    q.set(correo ? "email" : "passwordHash", valor)
                            .where(cb.equal(u.get("id"), usuario));
                    s.createMutationQuery(q).executeUpdate();
                    return null;
                });
    }

    public boolean correoOcupado(String correo) throws SQLException {
        return new UsuarioRepository(conexion).existe(null, correo, true);
    }

    public boolean contieneDni(int usuario, String password) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(String.class);
                    var p = q.from(PacienteEntidad.class);
                    q.select(p.get("dni")).where(cb.equal(p.get("usuario").get("id"), usuario));
                    var filas = s.createQuery(q).setMaxResults(1).getResultList();
                    return !filas.isEmpty()
                            && filas.get(0) != null
                            && !filas.get(0).isBlank()
                            && password.contains(filas.get(0));
                });
    }

    public void sesionesTrasCambio(int usuario, String conservar, String hash, Instant ahora)
            throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(SesionEntidad.class);
                    var sesion = q.from(SesionEntidad.class);
                    var filtros = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                    filtros.add(cb.equal(sesion.get("usuario"), usuario));
                    filtros.add(cb.isNull(sesion.get("revocada")));
                    if (conservar != null) filtros.add(cb.notEqual(sesion.get("id"), conservar));
                    q.set("revocada", LocalDateTime.ofInstant(ahora, ZoneOffset.UTC))
                            .where(filtros.toArray(jakarta.persistence.criteria.Predicate[]::new));
                    s.createMutationQuery(q).executeUpdate();
                    if (conservar != null) {
                        var cambio = cb.createCriteriaUpdate(SesionEntidad.class);
                        var actual = cambio.from(SesionEntidad.class);
                        cambio.set("credencial", SecretosSesion.hash(hash))
                                .where(
                                        cb.equal(actual.get("id"), conservar),
                                        cb.equal(actual.get("usuario"), usuario),
                                        cb.isNull(actual.get("revocada")));
                        s.createMutationQuery(cambio).executeUpdate();
                    }
                    return null;
                });
    }
}
