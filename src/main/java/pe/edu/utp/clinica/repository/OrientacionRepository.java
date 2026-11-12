package pe.edu.utp.clinica.repository;

import jakarta.persistence.criteria.JoinType;

import pe.edu.utp.clinica.ia.OrientacionCatalogo;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.EspecialidadEntidad;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class OrientacionRepository {
    private final Connection conexion;

    public OrientacionRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public List<OrientacionCatalogo.Especialidad> activas() throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var e = q.from(EspecialidadEntidad.class);
                    var m = e.join("motivos", JoinType.LEFT);
                    m.on(cb.equal(m.get("estado"), "ACTIVO"));
                    q.multiselect(e.get("id"), e.get("nombre"), m.get("id"), m.get("descripcion"))
                            .where(cb.equal(e.get("estado"), "ACTIVO"))
                            .orderBy(cb.asc(e.get("id")), cb.asc(m.get("id")));
                    var nombres = new LinkedHashMap<Integer, String>();
                    var motivos = new LinkedHashMap<Integer, List<OrientacionCatalogo.Motivo>>();
                    for (var fila : sesion.createQuery(q).getResultList()) {
                        int id = fila.get(0, Integer.class);
                        nombres.putIfAbsent(id, fila.get(1, String.class));
                        var lista = motivos.computeIfAbsent(id, clave -> new ArrayList<>());
                        Integer motivo = fila.get(2, Integer.class);
                        if (motivo != null)
                            lista.add(
                                    new OrientacionCatalogo.Motivo(
                                            motivo, fila.get(3, String.class)));
                    }
                    var lista = new ArrayList<OrientacionCatalogo.Especialidad>();
                    nombres.forEach(
                            (id, nombre) ->
                                    lista.add(
                                            new OrientacionCatalogo.Especialidad(
                                                    id, nombre, motivos.get(id))));
                    return List.copyOf(lista);
                });
    }
}
