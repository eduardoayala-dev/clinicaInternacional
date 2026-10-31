package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.ia.OrientacionCatalogo;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CatalogoOrientacionDAO {
    protected Connection abrir() throws SQLException {return DatabaseConfig.getConnection();}
    public List<OrientacionCatalogo.Especialidad> activas() throws SQLException {
        try(var conn=abrir();var ps=conn.prepareStatement("SELECT id_especialidad,nombre FROM especialidad WHERE estado='ACTIVO' ORDER BY id_especialidad");var rs=ps.executeQuery()) {
            var lista=new ArrayList<OrientacionCatalogo.Especialidad>();
            while(rs.next()) lista.add(new OrientacionCatalogo.Especialidad(rs.getInt(1),rs.getString(2)));
            return List.copyOf(lista);
        }
    }
}
