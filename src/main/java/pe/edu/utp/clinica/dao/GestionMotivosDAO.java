package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class GestionMotivosDAO {
    private static final String ACTIVO="ACTIVO";
    private static final String FILTRO_SQL=" FROM motivo_cita m JOIN especialidad e ON e.id_especialidad=m.id_especialidad WHERE (?=0 OR m.id_especialidad=?) AND (?='' OR m.estado=?) AND LOWER(m.descripcion) LIKE LOWER(?) ESCAPE '!'";
    private static final String CONTAR_SQL="SELECT COUNT(*)"+FILTRO_SQL;
    private static final String LISTAR_SQL="SELECT m.id_motivo,m.id_especialidad,e.nombre,m.descripcion,m.estado,EXISTS(SELECT 1 FROM cita c WHERE c.id_motivo=m.id_motivo)"+FILTRO_SQL+" ORDER BY e.nombre,m.descripcion,m.id_motivo LIMIT 20 OFFSET ?";
    public record Especialidad(int id,String nombre) {}
    public record Motivo(int id,int especialidad,String nombreEspecialidad,String descripcion,String estado,boolean utilizado) {}
    public record Filtro(int especialidad,String estado,String busqueda,int pagina) {
        public Filtro {
            estado=estado==null?"":estado;
            busqueda=busqueda==null?"":busqueda.strip();
            if(especialidad<0 || pagina<1 || !Set.of("",ACTIVO,"INACTIVO").contains(estado) || busqueda.length()>80 || busqueda.chars().anyMatch(Character::isISOControl))
                throw new AgendaException(400,"FILTRO_INVALIDO","Revisa los filtros del catálogo.");
        }
    }
    public record Panel(boolean administrador,boolean puedeEditar,List<Especialidad> especialidades,List<Motivo> motivos,Filtro filtro,long total,int paginas) {}
    private final AgendaCitaDAO.ConnectionSource conexiones;
    public GestionMotivosDAO(){this(DatabaseConfig::getConnection);}
    GestionMotivosDAO(AgendaCitaDAO.ConnectionSource conexiones){this.conexiones=conexiones;}

    public Panel consultar(int actor,Filtro filtro) {
        return TransaccionJdbc.ejecutar(conexiones,c->{
            boolean administrador=autorizar(c,actor);
            List<Especialidad> especialidades=new ArrayList<>();
            try(var s=c.prepareStatement("SELECT id_especialidad,nombre FROM especialidad WHERE estado='ACTIVO' ORDER BY nombre,id_especialidad");var r=s.executeQuery()){
                while(r.next())especialidades.add(new Especialidad(r.getInt(1),r.getString(2)));
            }
            long total;
            try(var s=c.prepareStatement(CONTAR_SQL)){filtros(s,filtro);try(var r=s.executeQuery()){r.next();total=r.getLong(1);}}
            int paginas=(int)Math.max(1,(total+19)/20);
            int pagina=Math.min(filtro.pagina(),paginas);
            List<Motivo> motivos=new ArrayList<>();
            try(var s=c.prepareStatement(LISTAR_SQL)){
                filtros(s,filtro);s.setInt(6,(pagina-1)*20);
                try(var r=s.executeQuery()){while(r.next())motivos.add(new Motivo(r.getInt(1),r.getInt(2),r.getString(3),r.getString(4),r.getString(5),r.getBoolean(6)));}
            }
            return new Panel(administrador,true,List.copyOf(especialidades),List.copyOf(motivos),new Filtro(filtro.especialidad(),filtro.estado(),filtro.busqueda(),pagina),total,paginas);
        });
    }
    public int guardar(int actor,int id,int especialidad,String descripcion,String ip) {
        String texto=descripcionValida(id,especialidad,descripcion);
        return TransaccionJdbc.ejecutar(conexiones,c->{
            autorizar(c,actor);
            especialidad(c,especialidad,true);
            if(id>0 && sinCambios(c,id,especialidad,texto))return id;
            exigirUnico(c,id,especialidad,texto);
            int resultado=id==0?insertar(c,especialidad,texto):actualizar(c,id,texto);
            auditar(c,actor,ip,id==0?"MOTIVO_CREADO":"MOTIVO_EDITADO",resultado,especialidad);
            return resultado;
        });
    }
    private static String descripcionValida(int id,int especialidad,String descripcion) {
        String texto=descripcion==null?"":descripcion.strip().replaceAll(" +"," ");
        if(id<0 || especialidad<=0 || texto.length()<3 || texto.length()>200 || texto.chars().anyMatch(ch->Character.isISOControl(ch)||ch=='<'||ch=='>'))
            throw new AgendaException(400,"MOTIVO_INVALIDO","Escribe un motivo de 3 a 200 caracteres, sin etiquetas ni caracteres de control.");
        return texto;
    }
    private static boolean sinCambios(Connection c,int id,int especialidad,String texto)throws SQLException {
        Motivo previo=motivo(c,id);
        if(previo.especialidad()!=especialidad)throw new AgendaException(409,"ESPECIALIDAD_INMUTABLE","Crea otro motivo para cambiar de especialidad.");
        if(previo.utilizado()&&!previo.descripcion().equals(texto))throw new AgendaException(409,"MOTIVO_CON_HISTORIAL","Este motivo ya aparece en citas. Desactívalo y crea otro para conservar el historial.");
        return previo.descripcion().equals(texto);
    }
    private static void exigirUnico(Connection c,int id,int especialidad,String texto)throws SQLException {
        try(var s=c.prepareStatement("SELECT id_motivo FROM motivo_cita WHERE id_especialidad=? AND LOWER(descripcion)=LOWER(?) AND id_motivo<>?")){
            s.setInt(1,especialidad);s.setString(2,texto);s.setInt(3,id);
            try(var r=s.executeQuery()){if(r.next())throw new AgendaException(409,"MOTIVO_DUPLICADO","Ya existe ese motivo en la especialidad, incluso si está inactivo.");}
        }
    }
    private static int insertar(Connection c,int especialidad,String texto)throws SQLException {
        try(var s=c.prepareStatement("INSERT INTO motivo_cita(id_especialidad,descripcion,estado) VALUES(?,?,'ACTIVO')",Statement.RETURN_GENERATED_KEYS)){
            s.setInt(1,especialidad);s.setString(2,texto);s.executeUpdate();
            try(var r=s.getGeneratedKeys()){if(!r.next()){throw new SQLException("Identificador no disponible");}
                return r.getInt(1);}
        }
    }
    private static int actualizar(Connection c,int id,String texto)throws SQLException {
        try(var s=c.prepareStatement("UPDATE motivo_cita SET descripcion=? WHERE id_motivo=?")){s.setString(1,texto);s.setInt(2,id);s.executeUpdate();}
        return id;
    }
    public void estado(int actor,int id,String estado,String ip) {
        if(id<=0 || !Set.of(ACTIVO,"INACTIVO").contains(estado==null?"":estado))throw new AgendaException(400,"ESTADO_INVALIDO","Selecciona un estado válido.");
        TransaccionJdbc.ejecutar(conexiones,c->{
            autorizar(c,actor);
            int especialidad;
            try(var s=c.prepareStatement("SELECT id_especialidad FROM motivo_cita WHERE id_motivo=?")){s.setInt(1,id);try(var r=s.executeQuery()){if(!r.next()){throw noEncontrado();}
                especialidad=r.getInt(1);}}
            especialidad(c,especialidad,ACTIVO.equals(estado));
            var previo=motivo(c,id);
            if(estado.equals(previo.estado()))return null;
            try(var s=c.prepareStatement("UPDATE motivo_cita SET estado=? WHERE id_motivo=?")){s.setString(1,estado);s.setInt(2,id);s.executeUpdate();}
            auditar(c,actor,ip,ACTIVO.equals(estado)?"MOTIVO_ACTIVADO":"MOTIVO_DESACTIVADO",id,especialidad);
            return null;
        });
    }
    private static boolean autorizar(Connection c,int actor)throws SQLException {
        try(var s=c.prepareStatement("SELECT rol FROM usuario WHERE id_usuario=? AND activo=TRUE FOR UPDATE")){s.setInt(1,actor);try(var r=s.executeQuery()){
            if(!r.next()){throw denegado();}
            String rol=r.getString(1);
            if("ADMINISTRADOR".equals(rol))return true;
            if("PERSONAL_ADMINISTRATIVO".equals(rol)){AmbitoAdmisionDAO.sede(c,actor);return false;}
            throw denegado();
        }}
    }
    private static void especialidad(Connection c,int id,boolean activa)throws SQLException {
        try(var s=c.prepareStatement("SELECT estado FROM especialidad WHERE id_especialidad=? FOR UPDATE")){s.setInt(1,id);try(var r=s.executeQuery()){
            if(!r.next() || (activa&&!ACTIVO.equals(r.getString(1))))throw new AgendaException(409,"ESPECIALIDAD_NO_DISPONIBLE","La especialidad no está activa.");
        }}
    }
    private static Motivo motivo(Connection c,int id)throws SQLException {
        try(var s=c.prepareStatement("SELECT m.id_especialidad,m.descripcion,m.estado,EXISTS(SELECT 1 FROM cita c WHERE c.id_motivo=m.id_motivo) FROM motivo_cita m WHERE m.id_motivo=? FOR UPDATE")){s.setInt(1,id);try(var r=s.executeQuery()){
            if(!r.next()){throw noEncontrado();}
            return new Motivo(id,r.getInt(1),"",r.getString(2),r.getString(3),r.getBoolean(4));
        }}
    }
    private static void filtros(PreparedStatement s,Filtro f)throws SQLException {
        s.setInt(1,f.especialidad());s.setInt(2,f.especialidad());s.setString(3,f.estado());s.setString(4,f.estado());
        s.setString(5,"%"+f.busqueda().replace("!","!!").replace("%","!%").replace("_","!_")+"%");
    }
    private static void auditar(Connection c,int actor,String ip,String evento,int motivo,int especialidad)throws SQLException {
        if(ip==null||ip.length()>45)throw new SQLException("Origen inválido");
        try(var s=c.prepareStatement("INSERT INTO auditoria_seguridad(id_usuario,ip_origen,evento,descripcion,sospechoso) VALUES(?,?,?,?,FALSE)")){
            s.setInt(1,actor);s.setString(2,ip);s.setString(3,evento);s.setString(4,"Motivo #"+motivo+"; especialidad #"+especialidad);s.executeUpdate();
        }
    }
    private static AgendaException denegado(){return new AgendaException(403,"CATALOGO_NO_AUTORIZADO","Tu cuenta no puede realizar esta operación del catálogo.");}
    private static AgendaException noEncontrado(){return new AgendaException(404,"MOTIVO_NO_ENCONTRADO","No se encontró el motivo de consulta.");}
}
