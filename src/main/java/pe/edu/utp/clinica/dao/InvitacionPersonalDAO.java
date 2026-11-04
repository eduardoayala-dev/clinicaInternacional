package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.auth.SecretosSesion;
import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.correo.Correo;
import pe.edu.utp.clinica.recuperacion.VerificacionJdbc;
import pe.edu.utp.clinica.util.SecurityUtil;
import java.sql.*;
import java.time.*;
import java.util.Locale;
import java.util.Set;

public class InvitacionPersonalDAO {
    private static final String ROL_MEDICO="MEDICO";
    private static final String ROL_ADMISION="PERSONAL_ADMINISTRATIVO";
    private static final String ROL_ADMIN="ADMINISTRADOR";

    private final AgendaCitaDAO.ConnectionSource conexiones;
    private final Clock reloj;
    public record Solicitud(String nombres,String apellidos,String correo,String rol,String cmp,Integer especialidad,Integer sede,String cargo) {}
    public record Entrega(int usuario,String correo) {
        @Override public String toString(){return "Entrega[privada]";}
    }
    public InvitacionPersonalDAO(){this(DatabaseConfig::getConnection,Clock.systemUTC());}
    InvitacionPersonalDAO(AgendaCitaDAO.ConnectionSource conexiones,Clock reloj){this.conexiones=conexiones;this.reloj=reloj;}

    public Entrega crear(int actor,Solicitud s,byte[] token) {
        return crear(actor,s,token,false);
    }
    public Entrega crearAcceso(int actor,Solicitud s,byte[] token) {
        if(s==null)throw error(400,"DATOS_INVALIDOS","Completa los datos de acceso.");
        return crear(actor,s,token,ROL_MEDICO.equals(s.rol()));
    }
    private Entrega crear(int actor,Solicitud s,byte[] token,boolean accesoMedico) {
        if(accesoMedico)s=new Solicitud(s.nombres(),s.apellidos(),s.correo(),ROL_MEDICO,null,null,s.sede(),null);
        Solicitud datos=s;
        return crearValidado(actor,datos,token,accesoMedico);
    }
    private Entrega crearValidado(int actor,Solicitud s,byte[] token,boolean accesoMedico) {
        if(accesoMedico)validarIdentidad(s);else validar(s);
        validarHash(token);
        String hash=SecurityUtil.hashPassword(SecretosSesion.nuevo());
        return TransaccionJdbc.ejecutar(conexiones,c->{
            administrador(c,actor);
            if(accesoMedico)validarSedeDestino(c,s.sede());
            if(!accesoMedico)validarCatalogos(c,s,0);
            try(var st=c.prepareStatement("SELECT id_usuario FROM usuario WHERE email=?")) {
                st.setString(1,s.correo().trim().toLowerCase(Locale.ROOT));
                try(var rs=st.executeQuery()){if(rs.next())throw error(409,"CORREO_EXISTENTE","El correo ya pertenece a una cuenta. No se modificó esa cuenta.");}
            }
            int id;
            try(var st=c.prepareStatement("INSERT INTO usuario(nombres,apellidos,email,password_hash,rol,activo) VALUES(?,?,?,?,?,FALSE)",Statement.RETURN_GENERATED_KEYS)) {
                st.setString(1,s.nombres().trim());st.setString(2,s.apellidos().trim());st.setString(3,s.correo().trim().toLowerCase(Locale.ROOT));st.setString(4,hash);st.setString(5,s.rol());st.executeUpdate();
                try(var rs=st.getGeneratedKeys()){if(!rs.next()){throw new SQLException("No se obtuvo el usuario");}
                    id=rs.getInt(1);}
            }
            if(!accesoMedico)perfil(c,id,s);
            guardarInvitacion(c,id,actor,token,false);
            if(accesoMedico)asignarDestino(c,id,s.sede());
            auditar(c,actor,"PERSONAL_INVITADO",id);
            return new Entrega(id,s.correo().trim().toLowerCase(Locale.ROOT));
        });
    }

    public Entrega reenviar(int actor,int usuario,byte[] token) {
        validarHash(token);
        return TransaccionJdbc.ejecutar(conexiones,c->{
            administrador(c,actor);
            String correo;
            try(var st=c.prepareStatement("SELECT u.email,u.activo,i.utilizada_en,i.creada_en FROM usuario u JOIN invitacion_personal i ON i.id_usuario=u.id_usuario WHERE u.id_usuario=? FOR UPDATE")) {
                st.setInt(1,usuario);try(var rs=st.executeQuery()) {
                    if(!rs.next() || rs.getBoolean(2) || rs.getObject(3)!=null)throw error(409,"INVITACION_NO_REENVIABLE","Solo puedes reenviar una invitación pendiente de activación.");
                    if(rs.getObject(4,LocalDateTime.class).plusSeconds(60).isAfter(ahora()))throw error(429,"ESPERA_REENVIO","Espera un minuto antes de reenviar.");
                    correo=rs.getString(1);
                }
            }
            guardarInvitacion(c,usuario,actor,token,true);
            auditar(c,actor,"INVITACION_REENVIADA",usuario);
            return new Entrega(usuario,correo);
        });
    }

    public void registrarEnvio(int usuario,byte[] token,boolean enviado) {
        try(var c=conexiones.open();var st=c.prepareStatement("UPDATE invitacion_personal SET estado_envio=? WHERE id_usuario=? AND token_hash=?")) {
            st.setString(1,enviado?"ENVIADO":"NO_CONFIRMADO");st.setInt(2,usuario);st.setBytes(3,token);st.executeUpdate();
        } catch(SQLException e){throw error(503,"ENVIO_NO_REGISTRADO","La cuenta está pendiente; no se pudo registrar el resultado del correo.");}
    }

    public boolean vigente(byte[] token) {
        if(token==null || token.length!=32)return false;
        try(var c=conexiones.open();var st=c.prepareStatement("SELECT i.vence_en FROM invitacion_personal i JOIN usuario u ON u.id_usuario=i.id_usuario WHERE i.token_hash=? AND i.utilizada_en IS NULL AND i.revocada_en IS NULL AND u.activo=FALSE AND u.rol IN ('MEDICO','PERSONAL_ADMINISTRATIVO','ADMINISTRADOR')")) {
            st.setBytes(1,token);try(var rs=st.executeQuery()){return rs.next() && ahora().isBefore(rs.getObject(1,LocalDateTime.class));}
        } catch(SQLException e){throw error(503,"INVITACION_NO_DISPONIBLE","No se pudo comprobar la invitación.");}
    }
    public java.util.Map<Integer,String> estados(int actor) {
        try(var c=conexiones.open()) {
            administrador(c,actor);
            var estados=new java.util.HashMap<Integer,String>();
            try(var st=c.prepareStatement("SELECT i.id_usuario,i.vence_en,i.revocada_en,i.estado_envio FROM invitacion_personal i JOIN usuario u ON u.id_usuario=i.id_usuario WHERE i.utilizada_en IS NULL AND u.activo=FALSE");var rs=st.executeQuery()) {
                while(rs.next()) {
                    String estado="Pendiente de activación";
                    if(rs.getObject(3)!=null)estado="Invitación revocada";
                    else if(!ahora().isBefore(rs.getObject(2,LocalDateTime.class)))estado="Invitación vencida";
                    else if("NO_CONFIRMADO".equals(rs.getString(4)))estado="Envío sin confirmar";
                    estados.put(rs.getInt(1),estado);
                }
            }
            return java.util.Map.copyOf(estados);
        } catch(SQLException e){throw error(503,"INVITACION_NO_DISPONIBLE","No se pudo consultar el estado de las invitaciones.");}
    }

    public void activar(byte[] token,String password) {
        validarHash(token);
        if(!VerificacionJdbc.passwordValida(password))throw error(400,"PASSWORD_INVALIDA","Usa al menos 12 caracteres, mayúscula, minúscula, número y símbolo; máximo 72 bytes.");
        String hash=SecurityUtil.hashPassword(password);
        TransaccionJdbc.ejecutar(conexiones,c->{
            int id;
            try(var st=c.prepareStatement("SELECT u.id_usuario,i.vence_en,u.activo,i.utilizada_en,i.revocada_en FROM invitacion_personal i JOIN usuario u ON u.id_usuario=i.id_usuario WHERE i.token_hash=? AND u.rol IN ('MEDICO','PERSONAL_ADMINISTRATIVO','ADMINISTRADOR') FOR UPDATE")) {
                st.setBytes(1,token);try(var rs=st.executeQuery()) {
                    if(!rs.next() || rs.getBoolean(3) || rs.getObject(4)!=null || rs.getObject(5)!=null || !ahora().isBefore(rs.getObject(2,LocalDateTime.class)))throw invalida();
                    id=rs.getInt(1);
                }
            }
            try(var st=c.prepareStatement("UPDATE usuario SET password_hash=?,activo=TRUE WHERE id_usuario=? AND activo=FALSE")){st.setString(1,hash);st.setInt(2,id);if(st.executeUpdate()!=1)throw invalida();}
            try(var st=c.prepareStatement("UPDATE invitacion_personal SET utilizada_en=? WHERE id_usuario=?")){st.setObject(1,ahora());st.setInt(2,id);st.executeUpdate();}
            auditar(c,id,"PERSONAL_ACTIVADO",id);
            return null;
        });
    }

    public void cambiarEstado(int actor,int usuario,boolean activo) {
        if(actor==usuario)throw error(409,"CAMBIO_PROPIO_NO_PERMITIDO","No puedes cambiar el estado de tu propia cuenta.");
        TransaccionJdbc.ejecutar(conexiones,c->{
            administrador(c,actor);
            try(var st=c.prepareStatement("SELECT u.rol,i.utilizada_en FROM usuario u LEFT JOIN invitacion_personal i ON i.id_usuario=u.id_usuario WHERE u.id_usuario=? FOR UPDATE")) {
                st.setInt(1,usuario);try(var rs=st.executeQuery()) {
                    if(!rs.next() || "PACIENTE".equals(rs.getString(1)))throw error(404,"PERSONAL_NO_ENCONTRADO","No se encontró una cuenta de personal.");
                }
            }
            if(activo) {
                validarActivacionPanel(c,usuario);
            } else {
                validarBajaMedico(c,usuario);
                revocar(c,usuario);
            }
            try(var st=c.prepareStatement("UPDATE usuario SET activo=? WHERE id_usuario=?")){st.setBoolean(1,activo);st.setInt(2,usuario);st.executeUpdate();}
            auditar(c,actor,activo?"PERSONAL_HABILITADO":"PERSONAL_DESHABILITADO",usuario);
            return null;
        });
    }

    public void editar(int actor,int usuario,Solicitud solicitud,boolean activo) {
        validar(solicitud);
        TransaccionJdbc.ejecutar(conexiones,c->{
            administrador(c,actor);
            boolean anterior=validarEdicion(c,actor,usuario,solicitud,activo);
            if(anterior && !activo)validarBajaMedico(c,usuario);
            validarCatalogos(c,solicitud,usuario);
            validarCambioDeAgenda(c,usuario,solicitud);
            try(var st=c.prepareStatement("UPDATE usuario SET nombres=?,apellidos=?,activo=? WHERE id_usuario=?")){st.setString(1,solicitud.nombres().trim());st.setString(2,solicitud.apellidos().trim());st.setBoolean(3,activo);st.setInt(4,usuario);st.executeUpdate();}
            actualizarPerfil(c,usuario,solicitud);
            if(!activo && anterior)revocar(c,usuario);
            auditar(c,actor,"PERSONAL_EDITADO",usuario);
            return null;
        });
    }
    public void editarAcceso(int actor,int usuario,Solicitud s,boolean activo) {
        if(!ROL_MEDICO.equals(s.rol())){editar(actor,usuario,s,activo);return;}
        validarIdentidad(s);
        TransaccionJdbc.ejecutar(conexiones,c->{
            administrador(c,actor);
            boolean anterior=validarEdicion(c,actor,usuario,s,activo);
            Integer sedePerfil=null;
            try(var st=c.prepareStatement("SELECT id_sede FROM medico WHERE id_usuario=? FOR UPDATE")){
                st.setInt(1,usuario);try(var r=st.executeQuery()){if(r.next())sedePerfil=r.getInt(1);}
            }
            if(sedePerfil==null){validarSedeDestino(c,s.sede());asignarDestino(c,usuario,s.sede());}
            else if(s.sede()!=null&&!s.sede().equals(sedePerfil))throw error(409,"MEDICO_CON_PERFIL","El cambio de sede requiere coordinar el perfil profesional y su agenda.");
            if(anterior&&!activo)validarBajaMedico(c,usuario);
            try(var st=c.prepareStatement("UPDATE usuario SET nombres=?,apellidos=?,activo=? WHERE id_usuario=?")){
                st.setString(1,s.nombres().trim());st.setString(2,s.apellidos().trim());st.setBoolean(3,activo);st.setInt(4,usuario);st.executeUpdate();
            }
            if(!activo&&anterior)revocar(c,usuario);
            auditar(c,actor,"ACCESO_MEDICO_EDITADO",usuario);return null;
        });
    }
    private static boolean validarEdicion(Connection c,int actor,int usuario,Solicitud s,boolean activo)throws SQLException {
        boolean anterior;
        try(var st=c.prepareStatement("SELECT email,rol,activo FROM usuario WHERE id_usuario=? FOR UPDATE")) {
            st.setInt(1,usuario);try(var rs=st.executeQuery()) {
                if(!rs.next() || "PACIENTE".equals(rs.getString(2)))throw error(404,"PERSONAL_NO_ENCONTRADO","No se encontró una cuenta de personal.");
                if(!s.rol().equals(rs.getString(2))||!s.correo().trim().equalsIgnoreCase(rs.getString(1)))throw error(400,"IDENTIDAD_NO_MODIFICABLE","El rol y el correo no se cambian desde esta edición.");
                anterior=rs.getBoolean(3);
            }
        }
        if(actor==usuario && !activo)throw error(409,"CAMBIO_PROPIO_NO_PERMITIDO","No puedes deshabilitar tu propia cuenta.");
        if(activo)validarActivacionPanel(c,usuario);
        return anterior;
    }
    private static void validarActivacionPanel(Connection c,int usuario)throws SQLException {
        try(var st=c.prepareStatement("SELECT id_usuario FROM invitacion_personal WHERE id_usuario=? AND utilizada_en IS NULL")){st.setInt(1,usuario);try(var rs=st.executeQuery()){if(rs.next())throw error(409,"ACTIVACION_PENDIENTE","Debe crear su contraseña desde la invitación.");}}
    }
    private static void validarSedeDestino(Connection c,Integer sede)throws SQLException{
        if(sede==null||sede<1)throw error(400,"SEDE_REQUERIDA","Selecciona la sede que completará el perfil médico.");
        try(var st=c.prepareStatement("SELECT id_sede FROM sede WHERE id_sede=? AND estado='ACTIVO' FOR UPDATE")){
            st.setInt(1,sede);try(var r=st.executeQuery()){if(!r.next())throw error(400,"SEDE_INVALIDA","Selecciona una sede activa.");}
        }
    }
    private static void asignarDestino(Connection c,int usuario,int sede)throws SQLException{
        try(var st=c.prepareStatement("UPDATE invitacion_personal SET id_sede_destino=? WHERE id_usuario=?")){
            st.setInt(1,sede);st.setInt(2,usuario);if(st.executeUpdate()!=1)throw error(409,"INVITACION_REQUERIDA","La cuenta no tiene una invitación para asignar la sede.");
        }
    }
    private void validarCambioDeAgenda(Connection c,int usuario,Solicitud s)throws SQLException {
        if(!ROL_MEDICO.equals(s.rol()))return;
        try(var st=c.prepareStatement("SELECT m.id_medico FROM medico m WHERE m.id_usuario=? AND (m.id_sede<>? OR m.id_especialidad<>?) AND (EXISTS(SELECT 1 FROM disponibilidad d WHERE d.id_medico=m.id_medico AND d.estado='PUBLICADO' AND d.fecha>=?) OR EXISTS(SELECT 1 FROM cita ci WHERE ci.id_medico=m.id_medico AND ci.fecha_hora>? AND ci.estado NOT IN ('CANCELADA','REPROGRAMADA')))")) {
            st.setInt(1,usuario);st.setInt(2,s.sede());st.setInt(3,s.especialidad());st.setObject(4,ahoraClinica().toLocalDate());st.setObject(5,ahoraClinica());
            try(var rs=st.executeQuery()){if(rs.next())throw error(409,"MEDICO_CON_AGENDA","Coordina la agenda vigente antes de cambiar la sede o especialidad del médico.");}
        }
    }
    private void validarBajaMedico(Connection c,int usuario)throws SQLException {
        try(var st=c.prepareStatement("SELECT m.id_medico FROM medico m WHERE m.id_usuario=? AND EXISTS(SELECT 1 FROM cita ci WHERE ci.id_medico=m.id_medico AND (ci.estado IN ('EN_SALA','EN_ATENCION') OR (ci.fecha_hora>? AND ci.estado IN ('SOLICITADA','CONFIRMADA'))))")) {
            st.setInt(1,usuario);st.setObject(2,ahoraClinica());
            try(var rs=st.executeQuery()){if(rs.next())throw error(409,"MEDICO_CON_CITAS","Coordina las citas pendientes antes de deshabilitar al médico.");}
        }
    }
    private static void actualizarPerfil(Connection c,int usuario,Solicitud s)throws SQLException {
        if(ROL_MEDICO.equals(s.rol())) {
            try(var st=c.prepareStatement("UPDATE medico SET cmp=?,id_especialidad=?,id_sede=? WHERE id_usuario=?")){st.setString(1,s.cmp().trim());st.setInt(2,s.especialidad());st.setInt(3,s.sede());st.setInt(4,usuario);if(st.executeUpdate()!=1)throw error(409,"PERFIL_INVALIDO","No se encontró el perfil médico.");}
        } else if(ROL_ADMISION.equals(s.rol())) {
            try(var st=c.prepareStatement("UPDATE personal_administrativo SET id_sede=?,cargo=? WHERE id_usuario=?")){st.setInt(1,s.sede());st.setString(2,s.cargo()==null||s.cargo().isBlank()?"Personal de admisión":s.cargo().trim());st.setInt(3,usuario);if(st.executeUpdate()!=1)throw error(409,"PERFIL_INVALIDO","No se encontró el perfil de admisión.");}
        }
    }
    private void revocar(Connection c,int usuario)throws SQLException {
        try(var st=c.prepareStatement("UPDATE invitacion_personal SET revocada_en=? WHERE id_usuario=? AND utilizada_en IS NULL")){st.setObject(1,ahora());st.setInt(2,usuario);st.executeUpdate();}
    }

    static void administrador(Connection c,int actor)throws SQLException {
        try(var st=c.prepareStatement("SELECT id_usuario FROM usuario WHERE id_usuario=? AND rol='ADMINISTRADOR' AND activo=TRUE FOR UPDATE")){st.setInt(1,actor);try(var rs=st.executeQuery()){if(!rs.next())throw error(403,"ADMINISTRADOR_REQUERIDO","Se requiere un administrador activo.");}}
    }
    private void guardarInvitacion(Connection c,int id,int actor,byte[] token,boolean actualizar)throws SQLException {
        String sql=actualizar?"UPDATE invitacion_personal SET token_hash=?,creada_por=?,creada_en=?,vence_en=?,revocada_en=NULL,estado_envio='PENDIENTE' WHERE id_usuario=? AND utilizada_en IS NULL"
                :"INSERT INTO invitacion_personal(token_hash,creada_por,creada_en,vence_en,id_usuario) VALUES(?,?,?,?,?)";
        try(var st=c.prepareStatement(sql)){st.setBytes(1,token);st.setInt(2,actor);st.setObject(3,ahora());st.setObject(4,ahora().plusMinutes(30));st.setInt(5,id);if(st.executeUpdate()!=1)throw invalida();}
    }
    private static void perfil(Connection c,int id,Solicitud s)throws SQLException {
        if(ROL_MEDICO.equals(s.rol())) {
            try(var st=c.prepareStatement("INSERT INTO medico(id_usuario,cmp,id_especialidad,id_sede,estado) VALUES(?,?,?,?,'ACTIVO')")){st.setInt(1,id);st.setString(2,s.cmp().trim());st.setInt(3,s.especialidad());st.setInt(4,s.sede());st.executeUpdate();}
        } else if(ROL_ADMISION.equals(s.rol())) {
            try(var st=c.prepareStatement("INSERT INTO personal_administrativo(id_usuario,id_sede,codigo_empleado,cargo) VALUES(?,?,?,?)")){st.setInt(1,id);st.setInt(2,s.sede());st.setString(3,"ADM-"+id);st.setString(4,s.cargo()==null||s.cargo().isBlank()?"Personal de admisión":s.cargo().trim());st.executeUpdate();}
        } else {
            try(var st=c.prepareStatement("INSERT INTO administrador(id_usuario,estado) VALUES(?,'ACTIVO')")){st.setInt(1,id);st.executeUpdate();}
        }
    }
    private static void validarCatalogos(Connection c,Solicitud s,int excluirUsuario)throws SQLException {
        if(ROL_ADMIN.equals(s.rol()))return;
        try(var st=c.prepareStatement("SELECT id_sede FROM sede WHERE id_sede=? AND estado='ACTIVO'")){st.setInt(1,s.sede());try(var rs=st.executeQuery()){if(!rs.next())throw error(400,"SEDE_INVALIDA","Selecciona una sede activa.");}}
        if(ROL_MEDICO.equals(s.rol())) {
            try(var st=c.prepareStatement("SELECT id_especialidad FROM especialidad WHERE id_especialidad=? AND estado='ACTIVO'")){st.setInt(1,s.especialidad());try(var rs=st.executeQuery()){if(!rs.next())throw error(400,"ESPECIALIDAD_INVALIDA","Selecciona una especialidad activa.");}}
            try(var st=c.prepareStatement("SELECT id_medico FROM medico WHERE cmp=? AND id_usuario<>?")){st.setString(1,s.cmp().trim());st.setInt(2,excluirUsuario);try(var rs=st.executeQuery()){if(rs.next())throw error(409,"CMP_EXISTENTE","El CMP ya está registrado.");}}
        }
    }
    private static void validar(Solicitud s) {
        validarIdentidad(s);
        if(!ROL_ADMIN.equals(s.rol()) && (s.sede()==null||s.sede()<=0))throw error(400,"SEDE_INVALIDA","Selecciona una sede.");
        if(ROL_MEDICO.equals(s.rol()) && (s.especialidad()==null||s.especialidad()<=0||s.cmp()==null||!s.cmp().trim().matches("[A-Za-z0-9-]{4,20}")))throw error(400,"MEDICO_INVALIDO","Completa CMP y especialidad.");
    }
    private static void validarIdentidad(Solicitud s) {
        if(s==null || !texto(s.nombres(),100) || !texto(s.apellidos(),100) || s.rol()==null || !Set.of(ROL_MEDICO,ROL_ADMISION,ROL_ADMIN).contains(s.rol()))throw error(400,"DATOS_INVALIDOS","Revisa los nombres y el rol del personal.");
        try{Correo.validarDireccion(s.correo()==null?null:s.correo().trim());}catch(IllegalArgumentException e){throw error(400,"CORREO_INVALIDO","Introduce un correo válido.");}
        if(s.correo().trim().length()>150)throw error(400,"CORREO_INVALIDO","El correo supera 150 caracteres.");
        if(s.cargo()!=null && s.cargo().length()>100)throw error(400,"CARGO_INVALIDO","El cargo supera 100 caracteres.");
    }
    private static boolean texto(String s,int max){return s!=null&&!s.isBlank()&&s.length()<=max&&s.chars().noneMatch(c->Character.isISOControl(c)||c=='<'||c=='>');}
    private static void validarHash(byte[] hash){if(hash==null||hash.length!=32)throw invalida();}
    private static void auditar(Connection c,int actor,String evento,int objetivo)throws SQLException {
        try(var st=c.prepareStatement("INSERT INTO auditoria_seguridad(id_usuario,evento,descripcion,sospechoso) VALUES(?,?,?,FALSE)")){st.setInt(1,actor);st.setString(2,evento);st.setString(3,"Cuenta de personal #"+objetivo);st.executeUpdate();}
    }
    private LocalDateTime ahora(){return LocalDateTime.ofInstant(reloj.instant(),ZoneOffset.UTC);}
    private LocalDateTime ahoraClinica(){return LocalDateTime.ofInstant(reloj.instant(),ZoneId.of("America/Lima"));}
    private static AgendaException invalida(){return error(410,"INVITACION_INVALIDA","La invitación no es válida, venció o ya se utilizó. Solicita otra al administrador.");}
    private static AgendaException error(int estado,String codigo,String mensaje){return new AgendaException(estado,codigo,mensaje);}
}
