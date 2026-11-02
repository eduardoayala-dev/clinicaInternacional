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
        String sql="SELECT e.id_especialidad,e.nombre,m.id_motivo,m.descripcion FROM especialidad e LEFT JOIN motivo_cita m ON m.id_especialidad=e.id_especialidad AND m.estado='ACTIVO' WHERE e.estado='ACTIVO' ORDER BY e.id_especialidad,m.id_motivo";
        try(var conn=abrir();var ps=conn.prepareStatement(sql);var rs=ps.executeQuery()) {
            var nombres=new java.util.LinkedHashMap<Integer,String>();
            var motivos=new java.util.LinkedHashMap<Integer,List<OrientacionCatalogo.Motivo>>();
            while(rs.next()) {
                int id=rs.getInt(1);nombres.putIfAbsent(id,rs.getString(2));
                var deEspecialidad=motivos.computeIfAbsent(id,clave->new ArrayList<>());
                if(rs.getObject(3)!=null)deEspecialidad.add(new OrientacionCatalogo.Motivo(rs.getInt(3),rs.getString(4)));
            }
            var lista=new ArrayList<OrientacionCatalogo.Especialidad>();
            nombres.forEach((id,nombre)->lista.add(new OrientacionCatalogo.Especialidad(id,nombre,motivos.get(id))));
            return List.copyOf(lista);
        }
    }
}
