package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.ia.OrientacionCatalogo;
import pe.edu.utp.clinica.repository.OrientacionRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class CatalogoOrientacionDAO {
    protected Connection abrir() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    public List<OrientacionCatalogo.Especialidad> activas() throws SQLException {
        try (var conn = abrir()) {
            return new OrientacionRepository(conn).activas();
        }
    }
}
