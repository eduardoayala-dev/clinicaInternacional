package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CatalogoSedesDAO {
    public record Sede(int id,String codigo,String nombre,String direccion,String telefono,String estado) implements java.io.Serializable {}
    public record Fila(int numero,Sede sede,String resultado) implements java.io.Serializable {}
    private final AgendaCitaDAO.ConnectionSource conexiones;
    public CatalogoSedesDAO(){this(DatabaseConfig::getConnection);}
    CatalogoSedesDAO(AgendaCitaDAO.ConnectionSource conexiones){this.conexiones=conexiones;}
    public List<Sede> listar(int actor) {
        return TransaccionJdbc.ejecutar(conexiones,c->{
            InvitacionPersonalDAO.administrador(c,actor);
            var sedes=new ArrayList<Sede>();
            try(var s=c.prepareStatement("SELECT id_sede,codigo,nombre,direccion,telefono,estado FROM sede ORDER BY nombre,id_sede");var r=s.executeQuery()){
                while(r.next())sedes.add(new Sede(r.getInt(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6)));
            }
            return List.copyOf(sedes);
        });
    }
    public void guardar(int actor,Sede sede,String ip){
        boolean automatico=sede!=null&&sede.id()==0&&(sede.codigo()==null||sede.codigo().isBlank());
        Sede validada=validar(automatico?conCodigo(sede,"AUTO"):sede);
        TransaccionJdbc.ejecutar(conexiones,c->{
            serializarCodigos(c);InvitacionPersonalDAO.administrador(c,actor);
            Sede definitiva=automatico?conCodigo(validada,codigoDisponible(c,validada.nombre())):validada;
            guardar(c,actor,definitiva,ip,false);return null;
        });
    }
    public List<Fila> importar(int actor,List<Fila> filas,String ip){
        if(filas==null||filas.isEmpty()||filas.size()>200)throw invalida("Importa entre 1 y 200 filas.");
        return TransaccionJdbc.ejecutar(conexiones,c->{
            serializarCodigos(c);
            InvitacionPersonalDAO.administrador(c,actor);
            var resultado=new ArrayList<Fila>();var vistos=new java.util.HashSet<String>();
            for(var fila:filas){
                var punto=c.setSavepoint();
                try{
                    if(!fila.resultado().isEmpty())throw invalida(fila.resultado());
                    Sede sede=validar(fila.sede());
                    if(!vistos.add(sede.codigo()))throw invalida("Código repetido en el archivo.");
                    guardar(c,actor,sede,ip,true);
                    resultado.add(new Fila(fila.numero(),sede,"Guardada"));
                }catch(AgendaException e){c.rollback(punto);resultado.add(new Fila(fila.numero(),fila.sede(),e.getMessage()));}
                finally{c.releaseSavepoint(punto);}
            }
            return List.copyOf(resultado);
        });
    }
    private static Sede conCodigo(Sede s,String codigo){return new Sede(s.id(),codigo,s.nombre(),s.direccion(),s.telefono(),s.estado());}
    private static void serializarCodigos(Connection c)throws SQLException{
        try(var s=c.prepareStatement("SELECT id_usuario FROM usuario WHERE rol='ADMINISTRADOR' ORDER BY id_usuario LIMIT 1 FOR UPDATE");var r=s.executeQuery()){
            if(!r.next())throw invalida("Se requiere un administrador registrado.");
        }
    }
    private static String codigoDisponible(Connection c,String nombre)throws SQLException{
        final String base;
        try{base=pe.edu.utp.clinica.catalogo.CodigoSede.base(nombre);}catch(IllegalArgumentException e){throw invalida(e.getMessage());}
        try(var s=c.prepareStatement("SELECT id_sede FROM sede WHERE codigo=?")){
            for(int numero=1;numero<=9999;numero++){
                String codigo=numero==1?base:base+"-"+String.format(Locale.ROOT,"%02d",numero);
                s.setString(1,codigo);try(var r=s.executeQuery()){if(!r.next())return codigo;}
            }
        }
        throw invalida("No se pudo generar otro código con esas iniciales.");
    }
    private static void guardar(Connection c,int actor,Sede sede,String ip,boolean porCodigo)throws SQLException {
        int id=sede.id();
        try(var s=c.prepareStatement("SELECT id_sede FROM sede WHERE codigo=? FOR UPDATE")){
            s.setString(1,sede.codigo());try(var r=s.executeQuery()){
                if(r.next()){
                    int existente=r.getInt(1);
                    if(!porCodigo && id!=existente)throw invalida("El código ya pertenece a otra sede.");
                    if(porCodigo)id=existente;
                }
            }
        }
        if(id>0){
            String nombreAnterior=exigirSede(c,id);
            try(var s=c.prepareStatement("SELECT codigo FROM sede WHERE id_sede=?")){
                s.setInt(1,id);try(var r=s.executeQuery()){r.next();if(!r.getString(1).equals(sede.codigo()))throw invalida("El código de una sede registrada no puede cambiarse.");}
            }
            if(!"ACTIVO".equals(sede.estado()))exigirSinAgenda(c,id);
            try(var s=c.prepareStatement("UPDATE sede SET codigo=?,nombre=?,direccion=?,telefono=?,estado=? WHERE id_sede=?")){
                parametros(s,sede);s.setInt(6,id);s.executeUpdate();
            }
            if(!nombreAnterior.equals(sede.nombre())){
                try(var s=c.prepareStatement("UPDATE paciente SET sede_preferida=? WHERE sede_preferida=?")){
                    s.setString(1,sede.nombre());s.setString(2,nombreAnterior);s.executeUpdate();
                }
            }
        }else{
            try(var s=c.prepareStatement("INSERT INTO sede(codigo,nombre,direccion,telefono,estado) VALUES(?,?,?,?,?)",java.sql.Statement.RETURN_GENERATED_KEYS)){
                parametros(s,sede);s.executeUpdate();try(var r=s.getGeneratedKeys()){if(!r.next())throw new SQLException("Sede sin identificador");id=r.getInt(1);}
            }
        }
        try(var s=c.prepareStatement("INSERT INTO auditoria_seguridad(id_usuario,ip_origen,evento,descripcion,sospechoso) VALUES(?,?,'SEDE_GUARDADA',?,FALSE)")){
            s.setInt(1,actor);s.setString(2,ip);s.setString(3,"Sede #"+id+"; código "+sede.codigo()+"; estado "+sede.estado());s.executeUpdate();
        }
    }
    private static void parametros(java.sql.PreparedStatement s,Sede sede)throws SQLException{
        s.setString(1,sede.codigo());s.setString(2,sede.nombre());s.setString(3,sede.direccion());s.setString(4,sede.telefono());s.setString(5,sede.estado());
    }
    private static String exigirSede(Connection c,int id)throws SQLException{
        try(var s=c.prepareStatement("SELECT nombre FROM sede WHERE id_sede=? FOR UPDATE")){s.setInt(1,id);try(var r=s.executeQuery()){if(!r.next())throw new AgendaException(404,"SEDE_NO_ENCONTRADA","La sede ya no existe.");return r.getString(1);}}
    }
    private static void exigirSinAgenda(Connection c,int id)throws SQLException{
        try(var s=c.prepareStatement("SELECT id_sede FROM sede WHERE id_sede=? AND (EXISTS(SELECT 1 FROM disponibilidad d WHERE d.id_sede=sede.id_sede AND d.estado='PUBLICADO' AND d.fecha>=?) OR EXISTS(SELECT 1 FROM cita ci JOIN medico m ON m.id_medico=ci.id_medico WHERE m.id_sede=sede.id_sede AND (ci.estado IN ('EN_SALA','EN_ATENCION') OR (ci.fecha_hora>? AND ci.estado IN ('SOLICITADA','CONFIRMADA')))))")){
            var ahora=java.time.LocalDateTime.now(java.time.ZoneId.of("America/Lima"));s.setInt(1,id);s.setObject(2,ahora.toLocalDate());s.setObject(3,ahora);
            try(var r=s.executeQuery()){if(r.next())throw new AgendaException(409,"SEDE_CON_AGENDA","Coordina las citas y los horarios publicados antes de deshabilitar la sede.");}
        }
    }
    public static Sede validar(Sede s){
        if(s==null)throw invalida("Completa los datos de la sede.");
        String codigo=texto(s.codigo(),2,20).toUpperCase(Locale.ROOT);
        if(!codigo.matches("[A-Z0-9][A-Z0-9_-]{1,19}"))throw invalida("El código debe contener letras, números, guion o guion bajo.");
        if(!Set.of("ACTIVO","INACTIVO","MANTENIMIENTO").contains(s.estado()==null?"":s.estado()))throw invalida("Selecciona un estado válido.");
        return new Sede(s.id(),codigo,texto(s.nombre(),3,100),texto(s.direccion(),5,255),texto(s.telefono(),6,20),s.estado());
    }
    private static String texto(String s,int min,int max){String t=s==null?"":s.strip();if(t.length()<min||t.length()>max||t.chars().anyMatch(Character::isISOControl)||t.contains("<")||t.contains(">"))throw invalida("Revisa la longitud y el contenido de los datos de la sede.");return t;}
    private static AgendaException invalida(String mensaje){return new AgendaException(422,"SEDE_INVALIDA",mensaje);}
}
