package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.*;

public class AmbitoAdmisionDAO {
    public enum Recurso {
        CITA("SELECT c.id_cita FROM cita c JOIN medico m ON m.id_medico=c.id_medico WHERE c.id_cita=? AND m.id_sede=?"),
        MEDICO("SELECT id_medico FROM medico WHERE id_medico=? AND id_sede=?"),
        HORARIO("SELECT id_disponibilidad FROM disponibilidad WHERE id_disponibilidad=? AND id_sede=?"),
        PACIENTE("SELECT p.id_paciente FROM paciente p WHERE p.id_paciente=? AND (EXISTS(SELECT 1 FROM sede s WHERE s.id_sede=? AND s.nombre=p.sede_preferida) OR EXISTS(SELECT 1 FROM cita c JOIN medico m ON m.id_medico=c.id_medico WHERE c.id_paciente=p.id_paciente AND m.id_sede=?))");
        final String sql;
        Recurso(String sql){this.sql=sql;}
    }
    private final AgendaCitaDAO.ConnectionSource conexiones;
    public AmbitoAdmisionDAO(){this(DatabaseConfig::getConnection);}
    AmbitoAdmisionDAO(AgendaCitaDAO.ConnectionSource conexiones){this.conexiones=conexiones;}
    public int sede(int usuario) {
        try(var c=conexiones.open()){return sede(c,usuario);}
        catch(SQLException e){throw new AgendaException(503,"AMBITO_NO_DISPONIBLE","No se pudo comprobar la sede del personal.");}
    }
    public String nombreSede(int usuario) {
        try(var c=conexiones.open();var st=c.prepareStatement("SELECT nombre FROM sede WHERE id_sede=?")) {
            st.setInt(1,sede(c,usuario));try(var rs=st.executeQuery()){if(!rs.next()){throw new AgendaException(403,"SEDE_NO_DISPONIBLE","La sede no está habilitada.");}
                return rs.getString(1);}
        }catch(SQLException e){throw new AgendaException(503,"AMBITO_NO_DISPONIBLE","No se pudo comprobar la sede.");}
    }
    static int sede(Connection c,int usuario)throws SQLException {
        try(var st=c.prepareStatement("SELECT p.id_sede FROM personal_administrativo p JOIN usuario u ON u.id_usuario=p.id_usuario JOIN sede s ON s.id_sede=p.id_sede WHERE p.id_usuario=? AND u.activo=TRUE AND u.rol='PERSONAL_ADMINISTRATIVO' AND s.estado='ACTIVO' FOR UPDATE")) {
            st.setInt(1,usuario);try(var rs=st.executeQuery()){if(!rs.next()){throw new AgendaException(403,"ADMISION_NO_HABILITADA","El perfil de admisión no está habilitado.");}
                return rs.getInt(1);}
        }
    }
    public void exigir(int usuario,Recurso recurso,int id) {
        TransaccionJdbc.ejecutar(conexiones,c->{exigir(c,sede(c,usuario),recurso,id);return null;});
    }
    public record EdicionPaciente(String nombres,String apellidos,String telefono,String direccion,String cobertura) {}
    public void editarPaciente(int usuario,int paciente,EdicionPaciente datos) {
        if(datos==null||!texto(datos.nombres(),100)||!texto(datos.apellidos(),100)||!texto(datos.telefono(),20)||!texto(datos.direccion(),255)||!texto(datos.cobertura(),50))throw new AgendaException(400,"DATOS_INVALIDOS","Completa los datos del paciente sin superar sus límites.");
        TransaccionJdbc.ejecutar(conexiones,c->{
            int sede=sede(c,usuario);
            try(var st=c.prepareStatement("SELECT id_paciente FROM paciente WHERE id_paciente=? FOR UPDATE")){st.setInt(1,paciente);try(var rs=st.executeQuery()){if(!rs.next())throw new AgendaException(404,"PACIENTE_NO_ENCONTRADO","No se encontró el paciente.");}}
            exigir(c,sede,Recurso.PACIENTE,paciente);
            try(var st=c.prepareStatement("UPDATE paciente SET telefono=?,direccion=?,tipo_cobertura=?,canal_aviso='Correo' WHERE id_paciente=?")){st.setString(1,datos.telefono().trim());st.setString(2,datos.direccion().trim());st.setString(3,datos.cobertura().trim());st.setInt(4,paciente);st.executeUpdate();}
            try(var st=c.prepareStatement("UPDATE usuario SET nombres=?,apellidos=? WHERE id_usuario=(SELECT id_usuario FROM paciente WHERE id_paciente=?)")){st.setString(1,datos.nombres().trim());st.setString(2,datos.apellidos().trim());st.setInt(3,paciente);st.executeUpdate();}
            try(var st=c.prepareStatement("INSERT INTO auditoria_seguridad(id_usuario,evento,descripcion,sospechoso) VALUES(?,'PACIENTE_EDITADO',?,FALSE)")){st.setInt(1,usuario);st.setString(2,"Paciente #"+paciente+"; sede #"+sede);st.executeUpdate();}
            return null;
        });
    }
    private static boolean texto(String s,int max){return s!=null&&!s.isBlank()&&s.length()<=max&&s.chars().noneMatch(Character::isISOControl);}
    static void exigir(Connection c,int sede,Recurso recurso,int id)throws SQLException {
        if(id<=0)throw new AgendaException(400,"DATOS_INVALIDOS","El identificador no es válido.");
        try(var st=c.prepareStatement(recurso.sql)) {
            st.setInt(1,id);st.setInt(2,sede);if(recurso==Recurso.PACIENTE)st.setInt(3,sede);
            try(var rs=st.executeQuery()){if(!rs.next())throw new AgendaException(404,"RECURSO_NO_ENCONTRADO","No se encontró un recurso autorizado en tu sede.");}
        }
    }
}
