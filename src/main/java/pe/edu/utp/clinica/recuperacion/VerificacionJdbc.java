package pe.edu.utp.clinica.recuperacion;

import pe.edu.utp.clinica.auth.*;
import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.correo.Correo;
import pe.edu.utp.clinica.util.SecurityUtil;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public final class VerificacionJdbc {
    public enum Proposito { RECUPERAR, CAMBIAR_PASSWORD, CAMBIAR_CORREO }
    public enum Etapa { CODIGO_ACTUAL, AUTORIZADO, CODIGO_NUEVO, FINALIZADO, BLOQUEADO }
    public record Entrega(String correo,String codigo) {
        @Override public String toString(){return "Entrega[privada]";}
    }
    public record CambioCorreo(boolean cambiado,String anterior) {
        @Override public String toString(){return "CambioCorreo[privado]";}
    }
    public record InicioCambio(int usuario,String sesion,String vinculo,Proposito proposito,String password,String ip) {
        @Override public String toString(){return "InicioCambio[privado]";}
    }
    private record Cuenta(int id,String correo,String password,String rol) {
        @Override public String toString(){return "Cuenta[privada]";}
    }
    private record Reto(String id,int usuario,Proposito proposito,String sesion,byte[] vinculo,
                        byte[] credencial,byte[] correo,byte[] codigo,Etapa etapa,int intentos,
                        String nuevo,Instant creada,Instant vence) {
        @Override public String toString(){return "Reto[privado]";}
        @Override public boolean equals(Object objeto) {
            if(this==objeto)return true;
            if(!(objeto instanceof Reto otro))return false;
            return usuario==otro.usuario && intentos==otro.intentos && proposito==otro.proposito && etapa==otro.etapa
                    && Objects.equals(id,otro.id) && Objects.equals(sesion,otro.sesion) && Objects.equals(nuevo,otro.nuevo)
                    && Objects.equals(creada,otro.creada) && Objects.equals(vence,otro.vence)
                    && Arrays.equals(vinculo,otro.vinculo) && Arrays.equals(credencial,otro.credencial)
                    && Arrays.equals(correo,otro.correo) && Arrays.equals(codigo,otro.codigo);
        }
        @Override public int hashCode() {
            return Objects.hash(id,usuario,proposito,sesion,Arrays.hashCode(vinculo),Arrays.hashCode(credencial),
                    Arrays.hashCode(correo),Arrays.hashCode(codigo),etapa,intentos,nuevo,creada,vence);
        }
    }
    private final SesionesJdbc.Conexion conexiones;
    private final CodigosVerificacion codigos;

    public VerificacionJdbc(CodigosVerificacion codigos){this(DatabaseConfig::getConnection,codigos);}
    public VerificacionJdbc(SesionesJdbc.Conexion conexiones,CodigosVerificacion codigos){this.conexiones=conexiones;this.codigos=codigos;}
    static void fecha(PreparedStatement st,int indice,Instant instante)throws SQLException {
        st.setTimestamp(indice,Timestamp.from(instante),Calendar.getInstance(TimeZone.getTimeZone("UTC")));
    }
    static Instant fecha(ResultSet rs,String columna)throws SQLException {
        return rs.getTimestamp(columna,Calendar.getInstance(TimeZone.getTimeZone("UTC"))).toInstant();
    }

    public Optional<Entrega> iniciarRecuperacion(String id,String identificador,boolean personal,String vinculo,String ip,Instant ahora)throws SQLException {
        return transaccion(conn->{
            if(!LimitesVerificacion.solicitud(conn,ip,(personal?"personal:":"paciente:")+identificador,ahora))return Optional.empty();
            try(var st=conn.prepareStatement(personal
                    ?"SELECT id_usuario,email,password_hash,rol FROM usuario WHERE LOWER(email)=? AND rol IN ('MEDICO','PERSONAL_ADMINISTRATIVO','ADMINISTRADOR') AND activo=1 FOR UPDATE"
                    :"SELECT u.id_usuario,u.email,u.password_hash,u.rol FROM usuario u JOIN paciente p ON p.id_usuario=u.id_usuario WHERE p.dni=? AND u.rol='PACIENTE' AND u.activo=1 FOR UPDATE")) {
                st.setString(1,identificador);
                try(var rs=st.executeQuery()){
                    if(!rs.next())return Optional.empty();
                    return Optional.of(crear(conn,id,cuenta(rs),Proposito.RECUPERAR,null,vinculo,ahora));
                }
            }
        });
    }

    public Optional<Entrega> iniciarCambio(String id,InicioCambio cambio,Instant ahora)throws SQLException {
        int usuario=cambio.usuario();
        String sesion=cambio.sesion();
        String vinculo=cambio.vinculo();
        Proposito proposito=cambio.proposito();
        String password=cambio.password();
        String ip=cambio.ip();
        if(proposito==Proposito.RECUPERAR)throw new IllegalArgumentException("Operación autenticada inválida.");
        return transaccion(conn->{
            if(!LimitesVerificacion.permitir(conn,"reauth:"+usuario,5,900,ahora)
                    || !LimitesVerificacion.solicitud(conn,ip,"usuario:"+usuario,ahora))return Optional.empty();
            Cuenta cuenta=cuenta(conn,usuario);
            if(cuenta==null || !sesionVigente(conn,cuenta,sesion,ahora) || !SecurityUtil.checkPassword(password,cuenta.password()))return Optional.empty();
            return Optional.of(crear(conn,id,cuenta,proposito,sesion,vinculo,ahora));
        });
    }

    private Entrega crear(Connection conn,String id,Cuenta cuenta,Proposito proposito,String sesion,String vinculo,Instant ahora)throws SQLException {
        UUID.fromString(id);
        try(var st=conn.prepareStatement("UPDATE verificacion_cuenta SET etapa='BLOQUEADO',codigo_hash=NULL WHERE id_usuario=? AND proposito=? AND etapa NOT IN ('FINALIZADO','BLOQUEADO')")) {
            st.setInt(1,cuenta.id());st.setString(2,proposito.name());st.executeUpdate();
        }
        String codigo=codigos.nuevo();
        try(var st=conn.prepareStatement("INSERT INTO verificacion_cuenta(id_verificacion,id_usuario,proposito,id_sesion,vinculo_hash,credencial_hash,correo_hash,codigo_hash,etapa,intentos,creada_en,vence_en) VALUES(?,?,?,?,?,?,?,?,'CODIGO_ACTUAL',0,?,?)")) {
            st.setString(1,id);st.setInt(2,cuenta.id());st.setString(3,proposito.name());st.setString(4,sesion);
            st.setBytes(5,SecretosSesion.hash(vinculo));st.setBytes(6,SecretosSesion.hash(cuenta.password()));st.setBytes(7,SecretosSesion.hash(cuenta.correo()));
            st.setBytes(8,codigos.hash(id,proposito.name(),Etapa.CODIGO_ACTUAL.name(),codigo));fecha(st,9,ahora);fecha(st,10,ahora.plusSeconds(600));st.executeUpdate();
        }
        return new Entrega(cuenta.correo(),codigo);
    }

    public boolean verificar(String id,String vinculo,Proposito proposito,String sesion,String codigo,Instant ahora)throws SQLException {
        return transaccion(conn->{
            Cuenta cuenta=cuentaDelReto(conn,id);
            Reto reto=reto(conn,id);
            if(!vigente(conn,reto,cuenta,vinculo,proposito,sesion,ahora) || reto.etapa()!=Etapa.CODIGO_ACTUAL)return false;
            if(!codigoCorrecto(conn,reto,codigo))return false;
            try(var st=conn.prepareStatement("UPDATE verificacion_cuenta SET etapa='AUTORIZADO',codigo_hash=NULL,intentos=0,vence_en=? WHERE id_verificacion=?")){
                fecha(st,1,minimo(ahora.plusSeconds(300),reto.creada().plusSeconds(1200)));st.setString(2,id);st.executeUpdate();
            }
            return true;
        });
    }

    public boolean autorizada(String id,String vinculo,Proposito proposito,String sesion,Instant ahora)throws SQLException {
        return transaccion(conn->{
            Cuenta cuenta=cuentaDelReto(conn,id);
            Reto reto=reto(conn,id);
            return vigente(conn,reto,cuenta,vinculo,proposito,sesion,ahora) && reto.etapa()==Etapa.AUTORIZADO;
        });
    }

    public boolean cambiarPassword(String id,String vinculo,Proposito proposito,String sesion,String password,Instant ahora)throws SQLException {
        if(proposito==Proposito.CAMBIAR_CORREO || !passwordValida(password))throw new IllegalArgumentException("Contraseña inválida.");
        return transaccion(conn->{
            Cuenta cuenta=cuentaDelReto(conn,id);
            Reto reto=reto(conn,id);
            if(!vigente(conn,reto,cuenta,vinculo,proposito,sesion,ahora) || reto.etapa()!=Etapa.AUTORIZADO || contieneDni(conn,cuenta.id(),password))return false;
            String hash=SecurityUtil.hashPassword(password);
            try(var st=conn.prepareStatement("UPDATE usuario SET password_hash=? WHERE id_usuario=?")){
                st.setString(1,hash);st.setInt(2,cuenta.id());st.executeUpdate();
            }
            sesionesTrasCambio(conn,cuenta.id(),sesion,hash,ahora);
            auditar(conn,cuenta.id(),proposito.name());
            finalizar(conn,id);
            return true;
        });
    }

    public Optional<Entrega> prepararCorreo(String id,String vinculo,String sesion,String nuevo,Instant ahora)throws SQLException {
        String correo=Correo.validarDireccion(nuevo.trim().toLowerCase(Locale.ROOT));
        if(correo.length()>150)throw new IllegalArgumentException("El correo supera el tamaño admitido por la cuenta.");
        return transaccion(conn->{
            if(!LimitesVerificacion.permitir(conn,"nuevo-correo:"+id,3,3600,ahora))return Optional.empty();
            Cuenta cuenta=cuentaDelReto(conn,id);
            Reto reto=reto(conn,id);
            if(!vigente(conn,reto,cuenta,vinculo,Proposito.CAMBIAR_CORREO,sesion,ahora) || reto.etapa()!=Etapa.AUTORIZADO
                    || cuenta.correo().equalsIgnoreCase(correo) || correoOcupado(conn,correo))return Optional.empty();
            String codigo=codigos.nuevo();
            try(var st=conn.prepareStatement("UPDATE verificacion_cuenta SET correo_nuevo=?,codigo_hash=?,etapa='CODIGO_NUEVO',intentos=0,vence_en=? WHERE id_verificacion=?")) {
                st.setString(1,correo);st.setBytes(2,codigos.hash(id,Proposito.CAMBIAR_CORREO.name(),Etapa.CODIGO_NUEVO.name(),codigo));
                fecha(st,3,minimo(ahora.plusSeconds(600),reto.creada().plusSeconds(1200)));st.setString(4,id);st.executeUpdate();
            }
            return Optional.of(new Entrega(correo,codigo));
        });
    }

    public CambioCorreo confirmarCorreo(String id,String vinculo,String sesion,String codigo,Instant ahora)throws SQLException {
        return transaccion(conn->{
            Cuenta cuenta=cuentaDelReto(conn,id);
            Reto reto=reto(conn,id);
            if(!vigente(conn,reto,cuenta,vinculo,Proposito.CAMBIAR_CORREO,sesion,ahora) || reto.etapa()!=Etapa.CODIGO_NUEVO
                    || !codigoCorrecto(conn,reto,codigo) || correoOcupado(conn,reto.nuevo()))return new CambioCorreo(false,null);
            try(var st=conn.prepareStatement("UPDATE usuario SET email=? WHERE id_usuario=?")){
                st.setString(1,reto.nuevo());st.setInt(2,cuenta.id());st.executeUpdate();
            }
            sesionesTrasCambio(conn,cuenta.id(),sesion,cuenta.password(),ahora);auditar(conn,cuenta.id(),Proposito.CAMBIAR_CORREO.name());finalizar(conn,id);
            return new CambioCorreo(true,cuenta.correo());
        });
    }

    private boolean codigoCorrecto(Connection conn,Reto reto,String codigo)throws SQLException {
        boolean formato=codigo!=null && codigo.matches("\\d{6}");
        boolean coincide=formato && MessageDigest.isEqual(reto.codigo(),codigos.hash(reto.id(),reto.proposito().name(),reto.etapa().name(),codigo));
        try(var st=conn.prepareStatement("UPDATE verificacion_cuenta SET intentos=intentos+1,etapa=?,codigo_hash=? WHERE id_verificacion=?")){
            boolean bloquear=!coincide && reto.intentos()+1>=5;
            st.setString(1,bloquear?Etapa.BLOQUEADO.name():reto.etapa().name());st.setBytes(2,bloquear?null:reto.codigo());st.setString(3,reto.id());st.executeUpdate();
        }
        return coincide;
    }

    private boolean vigente(Connection conn,Reto reto,Cuenta cuenta,String vinculo,Proposito proposito,String sesion,Instant ahora)throws SQLException {
        return reto!=null && cuenta!=null && reto.proposito()==proposito && Objects.equals(reto.sesion(),sesion)
                && MessageDigest.isEqual(reto.vinculo(),SecretosSesion.hash(vinculo)) && ahora.isBefore(reto.vence())
                && reto.etapa()!=Etapa.BLOQUEADO && reto.etapa()!=Etapa.FINALIZADO && reto.intentos()<5
                && MessageDigest.isEqual(reto.credencial(),SecretosSesion.hash(cuenta.password()))
                && MessageDigest.isEqual(reto.correo(),SecretosSesion.hash(cuenta.correo()))
                && (sesion==null || sesionVigente(conn,cuenta,sesion,ahora));
    }

    private Cuenta cuentaDelReto(Connection conn,String id)throws SQLException {
        try(var st=conn.prepareStatement("SELECT id_usuario FROM verificacion_cuenta WHERE id_verificacion=?")){
            st.setString(1,id);try(var rs=st.executeQuery()){return rs.next()?cuenta(conn,rs.getInt(1)):null;}
        }
    }
    private Cuenta cuenta(Connection conn,int usuario)throws SQLException {
        try(var st=conn.prepareStatement("SELECT id_usuario,email,password_hash,rol FROM usuario WHERE id_usuario=? AND activo=1 FOR UPDATE")){
            st.setInt(1,usuario);try(var rs=st.executeQuery()){return rs.next()?cuenta(rs):null;}
        }
    }
    private static Cuenta cuenta(ResultSet rs)throws SQLException {return new Cuenta(rs.getInt("id_usuario"),rs.getString("email"),rs.getString("password_hash"),rs.getString("rol"));}
    private Reto reto(Connection conn,String id)throws SQLException {
        try(var st=conn.prepareStatement("SELECT id_verificacion,id_usuario,proposito,id_sesion,vinculo_hash,credencial_hash,correo_hash,codigo_hash,etapa,intentos,correo_nuevo,creada_en,vence_en FROM verificacion_cuenta WHERE id_verificacion=? FOR UPDATE")){
            st.setString(1,id);try(var rs=st.executeQuery()){
                if(!rs.next())return null;
                return new Reto(id,rs.getInt("id_usuario"),Proposito.valueOf(rs.getString("proposito")),rs.getString("id_sesion"),rs.getBytes("vinculo_hash"),
                        rs.getBytes("credencial_hash"),rs.getBytes("correo_hash"),rs.getBytes("codigo_hash"),Etapa.valueOf(rs.getString("etapa")),rs.getInt("intentos"),rs.getString("correo_nuevo"),fecha(rs,"creada_en"),fecha(rs,"vence_en"));
            }
        }
    }
    private boolean sesionVigente(Connection conn,Cuenta cuenta,String sesion,Instant ahora)throws SQLException {
        try(var st=conn.prepareStatement("SELECT id_sesion FROM sesion_autenticacion WHERE id_sesion=? AND id_usuario=? AND revocada_en IS NULL AND vence_en>? AND ultima_actividad_en>? AND rol_inicial=? AND credencial_hash=? FOR UPDATE")){
            st.setString(1,sesion);st.setInt(2,cuenta.id());fecha(st,3,ahora);fecha(st,4,ahora.minusSeconds(1800));st.setString(5,cuenta.rol());st.setBytes(6,SecretosSesion.hash(cuenta.password()));try(var rs=st.executeQuery()){return rs.next();}
        }
    }
    private boolean correoOcupado(Connection conn,String correo)throws SQLException {
        try(var st=conn.prepareStatement("SELECT id_usuario FROM usuario WHERE LOWER(email)=?")){
            st.setString(1,correo);try(var rs=st.executeQuery()){return rs.next();}
        }
    }
    private void sesionesTrasCambio(Connection conn,int usuario,String conservar,String hash,Instant ahora)throws SQLException {
        try(var st=conn.prepareStatement("UPDATE sesion_autenticacion SET revocada_en=? WHERE id_usuario=? AND revocada_en IS NULL AND (? IS NULL OR id_sesion<>?)")){
            fecha(st,1,ahora);st.setInt(2,usuario);st.setString(3,conservar);st.setString(4,conservar);st.executeUpdate();
        }
        if(conservar!=null)try(var st=conn.prepareStatement("UPDATE sesion_autenticacion SET credencial_hash=? WHERE id_sesion=? AND id_usuario=? AND revocada_en IS NULL")){
            st.setBytes(1,SecretosSesion.hash(hash));st.setString(2,conservar);st.setInt(3,usuario);st.executeUpdate();
        }
    }
    private void finalizar(Connection conn,String id)throws SQLException {
        try(var st=conn.prepareStatement("UPDATE verificacion_cuenta SET etapa='FINALIZADO',codigo_hash=NULL WHERE id_verificacion=?")){st.setString(1,id);st.executeUpdate();}
    }
    private void auditar(Connection conn,int usuario,String evento)throws SQLException {
        try(var st=conn.prepareStatement("INSERT INTO auditoria_seguridad(id_usuario,evento,descripcion,sospechoso) VALUES(?,?,?,FALSE)")){
            st.setInt(1,usuario);st.setString(2,evento);st.setString(3,"Cambio de seguridad confirmado mediante código de correo.");st.executeUpdate();
        }
    }
    private boolean contieneDni(Connection conn,int usuario,String password)throws SQLException {
        try(var st=conn.prepareStatement("SELECT dni FROM paciente WHERE id_usuario=?")){
            st.setInt(1,usuario);try(var rs=st.executeQuery()){if(!rs.next()){return false;}
                String dni=rs.getString(1);
                return dni!=null && !dni.isBlank() && password.contains(dni);}
        }
    }
    private static Instant minimo(Instant uno,Instant dos){return uno.isBefore(dos)?uno:dos;}
    public static boolean passwordValida(String password){
        return password!=null && password.length()>=12 && password.getBytes(StandardCharsets.UTF_8).length<=72
                && password.chars().anyMatch(Character::isUpperCase) && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isDigit) && password.chars().anyMatch(c->!Character.isLetterOrDigit(c));
    }
    @FunctionalInterface private interface Operacion<T>{T ejecutar(Connection conn)throws SQLException;}
    private <T>T transaccion(Operacion<T> operacion)throws SQLException {
        try(var conn=conexiones.abrir()){
            conn.setAutoCommit(false);
            try{T resultado=operacion.ejecutar(conn);conn.commit();return resultado;}
            catch(SQLException|RuntimeException e){conn.rollback();throw e;}
        }
    }
}
