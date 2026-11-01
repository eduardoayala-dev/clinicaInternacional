package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class ConfiguracionOfertasDAO {
    public record Parametros(int minutosRespuesta,int anticipacionMinima,long version) {}
    private final AgendaCitaDAO.ConnectionSource conexiones;
    private final Clock reloj;
    public ConfiguracionOfertasDAO(){this(DatabaseConfig::getConnection,Clock.systemUTC());}
    ConfiguracionOfertasDAO(AgendaCitaDAO.ConnectionSource conexiones,Clock reloj){this.conexiones=conexiones;this.reloj=reloj;}
    public Parametros leer() {
        try(var conexion=conexiones.open()){return leer(conexion);}
        catch(SQLException e){throw noDisponible();}
    }
    static Parametros leer(Connection conexion)throws SQLException {
        try(var consulta=conexion.prepareStatement("SELECT minutos_respuesta,anticipacion_minima,version FROM configuracion_ofertas WHERE id_configuracion=1");var filas=consulta.executeQuery()) {
            if(!filas.next())throw noDisponible();
            return new Parametros(filas.getInt(1),filas.getInt(2),filas.getLong(3));
        }
    }
    public Parametros guardar(int actor,int minutos,int anticipacion,long version) {
        if(minutos<5||minutos>60||anticipacion<0||anticipacion>10080||version<0)throw new AgendaException(400,"CONFIGURACION_INVALIDA","El plazo debe ser de 5 a 60 minutos y la anticipación de 0 a 10080 minutos.");
        return TransaccionJdbc.ejecutar(conexiones,conexion->{
            exigirAdministrador(conexion,actor);
            try(var consulta=conexion.prepareStatement("SELECT version FROM configuracion_ofertas WHERE id_configuracion=1 FOR UPDATE");var filas=consulta.executeQuery()) {
                if(!filas.next())throw noDisponible();
                if(filas.getLong(1)!=version)throw new AgendaException(409,"CONFIGURACION_CAMBIADA","Otro administrador cambió la configuración. Recarga la página antes de guardar.");
            }
            Parametros anterior=leer(conexion);
            try(var cambio=conexion.prepareStatement("UPDATE configuracion_ofertas SET minutos_respuesta=?,anticipacion_minima=?,version=version+1,actualizado_por=?,actualizado_en=? WHERE id_configuracion=1 AND version=?")) {
                cambio.setInt(1,minutos);cambio.setInt(2,anticipacion);cambio.setInt(3,actor);
                cambio.setObject(4,LocalDateTime.ofInstant(reloj.instant(),ZoneOffset.UTC));cambio.setLong(5,version);
                if(cambio.executeUpdate()!=1)throw noDisponible();
            }
            try(var auditoria=conexion.prepareStatement("INSERT INTO auditoria_seguridad(id_usuario,evento,descripcion,sospechoso) VALUES(?,'CONFIGURACION_OFERTAS',?,FALSE)")) {
                auditoria.setInt(1,actor);auditoria.setString(2,"Plazo "+anterior.minutosRespuesta()+"→"+minutos+" min; anticipación "+anterior.anticipacionMinima()+"→"+anticipacion+" min; versión "+(version+1));auditoria.executeUpdate();
            }
            return new Parametros(minutos,anticipacion,version+1);
        });
    }
    private static void exigirAdministrador(Connection conexion,int actor)throws SQLException {
        try(var consulta=conexion.prepareStatement("SELECT id_usuario FROM usuario WHERE id_usuario=? AND activo=TRUE AND rol='ADMINISTRADOR' FOR UPDATE")) {
            consulta.setInt(1,actor);
            try(var filas=consulta.executeQuery()){if(!filas.next())throw new AgendaException(403,"ADMINISTRADOR_REQUERIDO","La configuración solo puede cambiarla un administrador activo.");}
        }
    }
    private static AgendaException noDisponible(){return new AgendaException(503,"CONFIGURACION_NO_DISPONIBLE","No se pudo obtener la configuración de ofertas.");}
}
