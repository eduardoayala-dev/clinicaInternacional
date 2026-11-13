package pe.edu.utp.clinica.repository;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;

import pe.edu.utp.clinica.correo.CorreosPendientesJdbc.Pendiente;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

public final class ColaCorreoRepository<T extends ColaCorreoEntidad> {
    public enum Tipo {
        CITAS(CorreoCitaEntidad.class),
        CAMBIOS(CorreoCambioEntidad.class),
        RECORDATORIOS(CorreoRecordatorioEntidad.class),
        OFERTAS(CorreoOfertaEntidad.class);
        private final Class<? extends ColaCorreoEntidad> entidad;

        Tipo(Class<? extends ColaCorreoEntidad> entidad) {
            this.entidad = entidad;
        }
    }

    private final Connection conexion;
    private final Class<T> entidad;

    private ColaCorreoRepository(Connection conexion, Class<T> entidad) {
        this.conexion = conexion;
        this.entidad = entidad;
    }

    public static ColaCorreoRepository<?> para(Connection conexion, Tipo tipo) {
        return new ColaCorreoRepository<>(conexion, tipo.entidad);
    }

    public LocalDateTime minima(boolean enviando) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(LocalDateTime.class);
                    var c = q.from(entidad);
                    var fecha = c.<LocalDateTime>get(enviando ? "iniciada" : "disponible");
                    q.select(fecha)
                            .where(
                                    cb.equal(c.get("estado"), enviando ? "ENVIANDO" : "PENDIENTE"),
                                    cb.isNotNull(fecha))
                            .orderBy(cb.asc(fecha));
                    return s.createQuery(q).setMaxResults(1).getResultList().stream()
                            .findFirst()
                            .orElse(null);
                });
    }

    public Optional<Pendiente> tomar(LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var c = q.from(entidad);
                    q.multiselect(c.get("id"), c.get("intentos"))
                            .where(
                                    cb.equal(c.get("estado"), "PENDIENTE"),
                                    cb.lessThanOrEqualTo(c.get("disponible"), ahora),
                                    cb.lessThan(c.get("intentos"), 3))
                            .orderBy(cb.asc(c.get("disponible")), cb.asc(c.get("id")));
                    var filas =
                            s.createQuery(q)
                                    .setMaxResults(1)
                                    .setLockOptions(new LockOptions(LockMode.UPGRADE_SKIPLOCKED))
                                    .getResultList();
                    if (filas.isEmpty()) return Optional.empty();
                    var f = filas.get(0);
                    int id = f.get(0, Integer.class), intentos = f.get(1, Integer.class) + 1;
                    var cambio = cb.createCriteriaUpdate(entidad);
                    var r = cambio.from(entidad);
                    cambio.set("estado", "ENVIANDO")
                            .set("intentos", intentos)
                            .set("iniciada", ahora)
                            .where(
                                    cb.equal(r.get("id"), id),
                                    cb.equal(r.get("estado"), "PENDIENTE"));
                    if (s.createMutationQuery(cambio).executeUpdate() != 1) return Optional.empty();
                    return Optional.of(new Pendiente(id, intentos));
                });
    }

    public void recuperar(LocalDateTime ahora) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(entidad);
                    var c = q.from(entidad);
                    q.set("estado", "NO_CONFIRMADO")
                            .set("finalizada", ahora)
                            .where(
                                    cb.equal(c.get("estado"), "ENVIANDO"),
                                    cb.lessThan(c.get("iniciada"), ahora.minusSeconds(900)));
                    s.createMutationQuery(q).executeUpdate();
                    return null;
                });
    }

    public void terminar(int id, String resultado, LocalDateTime ahora) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(entidad);
                    var c = q.from(entidad);
                    q.set("estado", resultado)
                            .set("finalizada", ahora)
                            .where(
                                    cb.equal(c.get("id"), id),
                                    cb.equal(c.get("estado"), "ENVIANDO"));
                    s.createMutationQuery(q).executeUpdate();
                    return null;
                });
    }

    public void reintentar(int id, LocalDateTime disponible) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(entidad);
                    var c = q.from(entidad);
                    q.set("estado", "PENDIENTE")
                            .set("disponible", disponible)
                            .set(c.<LocalDateTime>get("iniciada"), (LocalDateTime) null)
                            .where(
                                    cb.equal(c.get("id"), id),
                                    cb.equal(c.get("estado"), "ENVIANDO"));
                    s.createMutationQuery(q).executeUpdate();
                    return null;
                });
    }
}
