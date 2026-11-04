package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.*;
import java.util.*;

public final class PerfilProfesionalMedicoDAO {
    public record Cuenta(int id,String nombre,String correo,boolean activa){}
    private final AgendaCitaDAO.ConnectionSource conexiones;
    public PerfilProfesionalMedicoDAO(){this(DatabaseConfig::getConnection);}
    PerfilProfesionalMedicoDAO(AgendaCitaDAO.ConnectionSource conexiones){this.conexiones=conexiones;}
    public List<Cuenta> pendientes(int actor){
        return TransaccionJdbc.ejecutar(conexiones,c->{
            int sede=AmbitoAdmisionDAO.sede(c,actor);var lista=new ArrayList<Cuenta>();
            try(var st=c.prepareStatement("SELECT u.id_usuario,CONCAT(u.nombres,' ',u.apellidos),u.email,u.activo FROM usuario u JOIN invitacion_personal i ON i.id_usuario=u.id_usuario WHERE u.rol='MEDICO' AND i.id_sede_destino=? AND NOT EXISTS(SELECT 1 FROM medico m WHERE m.id_usuario=u.id_usuario) ORDER BY u.apellidos,u.nombres,u.id_usuario LIMIT 200")){
                st.setInt(1,sede);try(var r=st.executeQuery()){while(r.next())lista.add(new Cuenta(r.getInt(1),r.getString(2),r.getString(3),r.getBoolean(4)));}
            }
            return List.copyOf(lista);
        });
    }
    public void completar(int actor,int cuenta,String cmp,int especialidad,String ip){
        String colegiatura=cmp==null?"":cmp.strip();
        if(cuenta<1||especialidad<1||!colegiatura.matches("[A-Za-z0-9-]{4,20}"))throw new AgendaException(400,"PERFIL_INVALIDO","Selecciona una cuenta, una especialidad y un CMP válido.");
        try{
            TransaccionJdbc.ejecutar(conexiones,c->{
                int sede=AmbitoAdmisionDAO.sede(c,actor);
                try(var st=c.prepareStatement("SELECT id_usuario FROM invitacion_personal WHERE id_usuario=? AND id_sede_destino=? FOR UPDATE")){
                    st.setInt(1,cuenta);st.setInt(2,sede);try(var r=st.executeQuery()){if(!r.next())throw new AgendaException(403,"SEDE_NO_AUTORIZADA","El administrador debe asignar esta cuenta a tu sede antes de completar su perfil.");}
                }
                try(var st=c.prepareStatement("SELECT id_usuario FROM usuario WHERE id_usuario=? AND rol='MEDICO' FOR UPDATE")){
                    st.setInt(1,cuenta);try(var r=st.executeQuery()){if(!r.next())throw new AgendaException(404,"CUENTA_NO_ENCONTRADA","Selecciona una cuenta médica creada por el administrador.");}
                }
                try(var st=c.prepareStatement("SELECT id_medico FROM medico WHERE id_usuario=? OR cmp=? FOR UPDATE")){
                    st.setInt(1,cuenta);st.setString(2,colegiatura);try(var r=st.executeQuery()){if(r.next())throw new AgendaException(409,"PERFIL_EXISTENTE","La cuenta ya tiene perfil profesional o el CMP ya está registrado.");}
                }
                try(var st=c.prepareStatement("SELECT id_especialidad FROM especialidad WHERE id_especialidad=? AND estado='ACTIVO' FOR UPDATE")){
                    st.setInt(1,especialidad);try(var r=st.executeQuery()){if(!r.next())throw new AgendaException(409,"ESPECIALIDAD_INACTIVA","Selecciona una especialidad activa.");}
                }
                try(var st=c.prepareStatement("INSERT INTO medico(id_usuario,cmp,id_especialidad,id_sede,estado) VALUES(?,?,?,?,'ACTIVO')")){
                    st.setInt(1,cuenta);st.setString(2,colegiatura);st.setInt(3,especialidad);st.setInt(4,sede);st.executeUpdate();
                }
                try(var st=c.prepareStatement("INSERT INTO auditoria_seguridad(id_usuario,ip_origen,evento,descripcion,sospechoso) VALUES(?,?,'PERFIL_MEDICO_COMPLETADO',?,FALSE)")){
                    st.setInt(1,actor);st.setString(2,ip);st.setString(3,"Cuenta #"+cuenta+"; sede #"+sede+"; especialidad #"+especialidad);st.executeUpdate();
                }
                return null;
            });
        }catch(AgendaException e){throw e;}
    }
}
