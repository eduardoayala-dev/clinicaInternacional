package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.auth.SesionesJdbc;
import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.*;
import java.time.*;
import java.util.*;

public final class SeguimientoOfertasDAO {
    public static final int POR_PAGINA=15;
    public static final List<String> ESTADOS=List.of("PENDIENTE","ACEPTADA","RECHAZADA","EXPIRADA","CANCELADA");
    public record Filtro(String estado, LocalDate fecha, int especialidad, int pagina) {
        public Filtro {
            if(estado==null || (!estado.isEmpty() && !ESTADOS.contains(estado)) || especialidad<0 || pagina<1 || pagina>10000)
                throw new AgendaException(400,"FILTRO_INVALIDO","Selecciona filtros y una página válidos.");
        }
    }
    public record Especialidad(int id,String nombre) {}
    public record Oferta(int id,int citaOriginal,int lista,String paciente,String medico,String especialidad,
                         LocalDateTime cita,LocalDateTime ofrecida,LocalDateTime limite,String estado,
                         Integer citaAsignada,boolean plazoVencido) {}
    public record Panel(String sede,Filtro filtro,int pagina,int paginas,long total,Map<String,Long> estados,
                        List<Oferta> ofertas,List<Especialidad> especialidades,LocalDateTime consultadoEn) {
        public Panel {estados=Map.copyOf(estados);ofertas=List.copyOf(ofertas);especialidades=List.copyOf(especialidades);}
    }
    private record Sede(int id,String nombre) {}
    private static final String ORIGEN="""
            FROM reasignacion_inteligente r JOIN cita c ON c.id_cita=r.id_cita_original
            JOIN disponibilidad d ON d.id_disponibilidad=r.id_disponibilidad
            JOIN medico m ON m.id_medico=c.id_medico JOIN usuario um ON um.id_usuario=m.id_usuario
            JOIN especialidad e ON e.id_especialidad=m.id_especialidad
            JOIN lista_espera le ON le.id_lista_espera=r.id_lista_espera
            JOIN paciente p ON p.id_paciente=le.id_paciente JOIN usuario up ON up.id_usuario=p.id_usuario
            WHERE d.id_sede=?
            """;
    private final SesionesJdbc.Conexion conexiones;
    private final Clock reloj;
    public SeguimientoOfertasDAO(){this(DatabaseConfig::getConnection,Clock.system(ZoneId.of("America/Lima")));}
    public SeguimientoOfertasDAO(SesionesJdbc.Conexion conexiones,Clock reloj){this.conexiones=conexiones;this.reloj=reloj.withZone(ZoneId.of("America/Lima"));}

    public Panel consultar(int actor,Filtro filtro) {
        try(var c=conexiones.abrir()) {
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            c.setAutoCommit(false);
            Sede sede=sede(c,actor);
            String condiciones=(filtro.estado().isEmpty()?"":" AND r.estado=?")
                    +(filtro.fecha()==null?"":" AND r.fecha_oferta>=? AND r.fecha_oferta<?")
                    +(filtro.especialidad()==0?"":" AND m.id_especialidad=?");
            Map<String,Long> conteos=contar(c,sede.id(),filtro,condiciones);
            long total=conteos.values().stream().mapToLong(Long::longValue).sum();
            int paginas=(int)Math.max(1,(total+POR_PAGINA-1)/POR_PAGINA);
            int pagina=Math.min(filtro.pagina(),paginas);
            var ahora=LocalDateTime.now(reloj);
            var ofertas=listar(c,sede.id(),filtro,condiciones,pagina,ahora);
            var especialidades=especialidades(c,sede.id());
            c.commit();
            return new Panel(sede.nombre(),filtro,pagina,paginas,total,conteos,ofertas,especialidades,ahora);
        } catch(SQLException e){throw new AgendaException(503,"OFERTAS_NO_DISPONIBLES","No se pudo consultar el seguimiento de ofertas.");}
    }
    private static Sede sede(Connection c,int actor)throws SQLException {
        try(var s=c.prepareStatement("SELECT s.id_sede,s.nombre FROM personal_administrativo p JOIN usuario u ON u.id_usuario=p.id_usuario JOIN sede s ON s.id_sede=p.id_sede WHERE u.id_usuario=? AND u.rol='PERSONAL_ADMINISTRATIVO' AND u.activo=TRUE AND s.estado='ACTIVO'")) {
            s.setInt(1,actor);try(var r=s.executeQuery()) {
                if(!r.next())throw new AgendaException(403,"ACCESO_DENEGADO","La cuenta de admisión no está habilitada en una sede activa.");
                return new Sede(r.getInt(1),r.getString(2));
            }
        }
    }
    private static int parametros(PreparedStatement s,int sede,Filtro filtro)throws SQLException {
        int i=1;s.setInt(i++,sede);
        if(!filtro.estado().isEmpty())s.setString(i++,filtro.estado());
        if(filtro.fecha()!=null){s.setObject(i++,filtro.fecha().atStartOfDay());s.setObject(i++,filtro.fecha().plusDays(1).atStartOfDay());}
        if(filtro.especialidad()!=0)s.setInt(i++,filtro.especialidad());
        return i;
    }
    private static Map<String,Long> contar(Connection c,int sede,Filtro filtro,String condiciones)throws SQLException {
        Map<String,Long> conteos=new LinkedHashMap<>();ESTADOS.forEach(estado->conteos.put(estado,0L));
        try(var s=c.prepareStatement("SELECT r.estado,COUNT(*) "+ORIGEN+condiciones+" GROUP BY r.estado")) {
            parametros(s,sede,filtro);try(var r=s.executeQuery()){while(r.next())conteos.put(Objects.requireNonNullElse(r.getString(1),"SIN_ESTADO"),r.getLong(2));}
        }
        return conteos;
    }
    private static List<Oferta> listar(Connection c,int sede,Filtro filtro,String condiciones,int pagina,LocalDateTime ahora)throws SQLException {
        String sql="SELECT r.id_reasignacion,r.id_cita_original,r.id_lista_espera,CONCAT(up.nombres,' ',up.apellidos),CONCAT(um.nombres,' ',um.apellidos),e.nombre,c.fecha_hora,r.fecha_oferta,r.fecha_limite_respuesta,r.estado,r.id_cita_asignada "
                +ORIGEN+condiciones+" ORDER BY r.fecha_oferta DESC,r.id_reasignacion DESC LIMIT ? OFFSET ?";
        List<Oferta> ofertas=new ArrayList<>();
        try(var s=c.prepareStatement(sql)) {
            int i=parametros(s,sede,filtro);s.setInt(i++,POR_PAGINA);s.setInt(i,(pagina-1)*POR_PAGINA);
            try(var r=s.executeQuery()){while(r.next()) {
                var limite=r.getObject(9,LocalDateTime.class);
                String estado=Objects.requireNonNullElse(r.getString(10),"SIN_ESTADO");
                ofertas.add(new Oferta(r.getInt(1),r.getInt(2),r.getInt(3),r.getString(4),r.getString(5),r.getString(6),
                        r.getObject(7,LocalDateTime.class),r.getObject(8,LocalDateTime.class),limite,estado,
                        (Integer)r.getObject(11),"PENDIENTE".equals(estado)&&limite!=null&&!ahora.isBefore(limite)));
            }}
        }
        return ofertas;
    }
    private static List<Especialidad> especialidades(Connection c,int sede)throws SQLException {
        List<Especialidad> lista=new ArrayList<>();
        try(var s=c.prepareStatement("SELECT DISTINCT e.id_especialidad,e.nombre "+ORIGEN+" ORDER BY e.nombre,e.id_especialidad")) {
            s.setInt(1,sede);try(var r=s.executeQuery()){while(r.next())lista.add(new Especialidad(r.getInt(1),r.getString(2)));}
        }
        return lista;
    }
}
