package pe.edu.utp.clinica.repository;

import jakarta.persistence.LockModeType;

import pe.edu.utp.clinica.dao.ConfiguracionOfertasDAO.Cambios;
import pe.edu.utp.clinica.dao.ConfiguracionOfertasDAO.Parametros;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.ConfiguracionOfertasEntidad;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public final class ConfiguracionOfertasRepository {
    private final Connection conexion;

    public ConfiguracionOfertasRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public Parametros leer() throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Parametros.class);
                    var c = q.from(ConfiguracionOfertasEntidad.class);
                    q.select(
                                    cb.construct(
                                            Parametros.class,
                                            c.get("minutos"),
                                            c.get("anticipacion"),
                                            c.get("version"),
                                            c.get("confirmacion"),
                                            c.get("tolerancia"),
                                            c.get("recordatorio"),
                                            c.get("intentos")))
                            .where(cb.equal(c.get("id"), 1));
                    return s.createQuery(q).getResultList().stream().findFirst().orElse(null);
                });
    }

    public Long bloquearVersion() throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Long.class);
                    var c = q.from(ConfiguracionOfertasEntidad.class);
                    q.select(c.get("version")).where(cb.equal(c.get("id"), 1));
                    return s
                            .createQuery(q)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .getResultList()
                            .stream()
                            .findFirst()
                            .orElse(null);
                });
    }

    public int guardar(int actor, Cambios cambios, LocalDateTime ahora) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(ConfiguracionOfertasEntidad.class);
                    var c = q.from(ConfiguracionOfertasEntidad.class);
                    q.set("minutos", cambios.minutosRespuesta())
                            .set("anticipacion", cambios.anticipacionMinima())
                            .set("version", cambios.version() + 1)
                            .set("actor", actor)
                            .set("actualizada", ahora)
                            .set("confirmacion", cambios.confirmacionHoras())
                            .set("tolerancia", cambios.toleranciaLlegada())
                            .set("recordatorio", cambios.recordatorioHoras())
                            .set("intentos", cambios.intentosConfirmacion())
                            .where(
                                    cb.equal(c.get("id"), 1),
                                    cb.equal(c.get("version"), cambios.version()));
                    return s.createMutationQuery(q).executeUpdate();
                });
    }
}
