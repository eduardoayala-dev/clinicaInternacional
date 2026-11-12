package pe.edu.utp.clinica.repository;

import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.EspecialidadEntidad;
import pe.edu.utp.clinica.persistence.entity.SedeEntidad;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class CatalogosAdministrativosRepository {
    public record Sede(int id, String nombre, String estado) {}

    public record Especialidad(int id, String nombre) {}

    private final Connection conexion;

    public CatalogosAdministrativosRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public List<Sede> sedes() throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Sede.class);
                    var e = q.from(SedeEntidad.class);
                    q.select(
                                    cb.construct(
                                            Sede.class,
                                            e.get("id"),
                                            e.get("nombre"),
                                            e.get("estado")))
                            .orderBy(cb.asc(e.get("nombre")), cb.asc(e.get("id")));
                    return s.createQuery(q).getResultList();
                });
    }

    public List<Especialidad> especialidades() throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Especialidad.class);
                    var e = q.from(EspecialidadEntidad.class);
                    q.select(cb.construct(Especialidad.class, e.get("id"), e.get("nombre")));
                    return s.createQuery(q).getResultList();
                });
    }
}
