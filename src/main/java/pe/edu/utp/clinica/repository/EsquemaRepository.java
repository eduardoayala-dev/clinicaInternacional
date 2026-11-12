package pe.edu.utp.clinica.repository;

import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.*;
import java.util.*;

public final class EsquemaRepository {
    private final Connection conexion;

    public EsquemaRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public boolean configuracion() throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var c = q.from(ConfiguracionOfertasEntidad.class);
                    q.select(c.get("id")).where(cb.equal(c.get("id"), 1));
                    return !s.createQuery(q).setTimeout(2).getResultList().isEmpty();
                });
    }

    public boolean avisoVencido() throws SQLException {
        String esquema = conexion.getCatalog();
        return PersistenciaJpa.consultar(
                conexion,
                s -> {
                    var cb = s.getCriteriaBuilder();
                    var q = cb.createQuery(String.class);
                    var r = q.from(RestriccionEsquemaEntidad.class);
                    q.select(r.get("nombre"))
                            .where(
                                    cb.equal(r.get("esquema"), esquema),
                                    cb.equal(r.get("tabla"), "correo_cambio_cita"),
                                    cb.equal(r.get("tipo"), "CHECK"),
                                    r.get("nombre")
                                            .in("ck_aviso_cambio_tipo", "ck_aviso_cambio_tipo_v2"));
                    var nombres = s.createQuery(q).setTimeout(2).getResultList();
                    return nombres.contains("ck_aviso_cambio_tipo_v2")
                            && !nombres.contains("ck_aviso_cambio_tipo");
                });
    }

    public Map<String, Set<String>> columnas() throws SQLException {
        var resultado = new HashMap<String, Set<String>>();
        try (var filas = conexion.getMetaData().getColumns(conexion.getCatalog(), null, "%", "%")) {
            while (filas.next())
                resultado
                        .computeIfAbsent(
                                filas.getString("TABLE_NAME").toLowerCase(Locale.ROOT),
                                k -> new HashSet<>())
                        .add(filas.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
        }
        return resultado;
    }
}
