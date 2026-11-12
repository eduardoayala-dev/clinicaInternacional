package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.SQLException;
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
        try(var conexion=conexiones.open()) {
            return new pe.edu.utp.clinica.repository.MotivoCitaRepository(conexion).activos(especialidad)
                    .stream().map(motivo -> Map.<String,Object>of("idMotivo",motivo.id(),
                            "idEspecialidad",motivo.especialidad(),"descripcion",motivo.descripcion())).toList();
        } catch(SQLException e){throw new AgendaException(503,"MOTIVOS_NO_DISPONIBLES","No se pudo cargar el catálogo de motivos. Inténtalo nuevamente.");}
    }
}
