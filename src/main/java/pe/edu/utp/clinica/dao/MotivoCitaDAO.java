package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MotivoCitaDAO {
    private final AgendaCitaDAO.ConnectionSource conexiones;
    public MotivoCitaDAO(){this(DatabaseConfig::getConnection);}
    MotivoCitaDAO(AgendaCitaDAO.ConnectionSource conexiones){this.conexiones=conexiones;}
    public List<Map<String,Object>> listarActivos(){return listar(null);}
    public List<Map<String,Object>> listarEspecialidad(int especialidad){
        if(especialidad<=0)throw new AgendaException(400,"ESPECIALIDAD_INVALIDA","Selecciona una especialidad válida.");
        return listar(especialidad);
    }
    private List<Map<String,Object>> listar(Integer especialidad) {
        String sql="SELECT m.id_motivo,m.id_especialidad,m.descripcion FROM motivo_cita m JOIN especialidad e ON e.id_especialidad=m.id_especialidad WHERE m.estado='ACTIVO' AND e.estado='ACTIVO'"
                +(especialidad==null?"":" AND m.id_especialidad=?")+" ORDER BY m.id_especialidad,m.descripcion,m.id_motivo";
        try(var conexion=conexiones.open();var consulta=conexion.prepareStatement(sql)) {
            if(especialidad!=null)consulta.setInt(1,especialidad);
            List<Map<String,Object>> motivos=new ArrayList<>();
            try(var filas=consulta.executeQuery()) {
                while(filas.next())motivos.add(Map.of("idMotivo",filas.getInt("id_motivo"),"idEspecialidad",filas.getInt("id_especialidad"),"descripcion",filas.getString("descripcion")));
            }
            return motivos;
        } catch(SQLException e){throw new AgendaException(503,"MOTIVOS_NO_DISPONIBLES","No se pudo cargar el catálogo de motivos. Inténtalo nuevamente.");}
    }
}
