package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class PanelSistemaDAO {
    public record Filtro(LocalDate desde,LocalDate hasta,int sede,int especialidad) {
        public Filtro {
            if(desde==null||hasta==null||desde.getYear()<1000||hasta.getYear()>9998||hasta.isBefore(desde)||ChronoUnit.DAYS.between(desde,hasta)>365||sede<0||especialidad<0)
                throw new AgendaException(400,"FILTROS_INVALIDOS","Selecciona fechas válidas, hasta un año, y filtros válidos.");
        }
    }
    public record Opcion(int id,String nombre){}
    public record Indicadores(long citas,long atendidas,long canceladas,long inasistencias,long reasignadas,long ofertas,long aceptadas,long pendientes,long bajo,long medio,long alto,long sinRiesgo){
        public static Indicadores cero(){return new Indicadores(0,0,0,0,0,0,0,0,0,0,0,0);}
    }
    public record Sede(int id,String nombre,String direccion,String estado,Indicadores datos){}
    public record Semana(LocalDate desde,LocalDate hasta,long citas,long atendidas){}
    public record Auditoria(LocalDateTime fecha,String actor,String evento,String descripcion){}
    public record Totales(long usuarios,long pacientes,long medicos,long personal,long administradores,long sedes){}
    public record Panel(Filtro filtro,Indicadores datos,List<Sede> sedes,List<Opcion> catalogoSedes,List<Opcion> especialidades,List<Semana> semanas,List<Auditoria> auditoria,Totales totales,LocalDateTime actualizado){}
    private final AgendaCitaDAO.ConnectionSource conexiones;
    private final Clock reloj;
    public PanelSistemaDAO(){this(DatabaseConfig::getConnection,Clock.system(ZoneId.of("America/Lima")));}
    PanelSistemaDAO(AgendaCitaDAO.ConnectionSource conexiones,Clock reloj){this.conexiones=conexiones;this.reloj=reloj.withZone(ZoneId.of("America/Lima"));}
    public Panel consultar(int actor,Filtro filtro){
        Objects.requireNonNull(filtro);
        return TransaccionJdbc.ejecutar(conexiones,c->{
            InvitacionPersonalDAO.administrador(c,actor);
            var sedes=opciones(c,"SELECT id_sede,nombre FROM sede ORDER BY nombre,id_sede");
            var especialidades=opciones(c,"SELECT id_especialidad,nombre FROM especialidad ORDER BY nombre,id_especialidad");
            if(filtro.sede()>0&&sedes.stream().noneMatch(s->s.id()==filtro.sede())||filtro.especialidad()>0&&especialidades.stream().noneMatch(e->e.id()==filtro.especialidad()))
                throw new AgendaException(400,"FILTROS_INVALIDOS","La sede o especialidad seleccionada no existe.");
            var porSede=new LinkedHashMap<Integer,Sede>();
            try(var st=c.prepareStatement("SELECT id_sede,nombre,direccion,estado FROM sede WHERE (?=0 OR id_sede=?) ORDER BY nombre,id_sede")){
                st.setInt(1,filtro.sede());st.setInt(2,filtro.sede());try(var r=st.executeQuery()){while(r.next())porSede.put(r.getInt(1),new Sede(r.getInt(1),r.getString(2),r.getString(3),r.getString(4),Indicadores.cero()));}
            }
            String sql="""
                SELECT COALESCE(d.id_sede,m.id_sede),COUNT(*),
                SUM(CASE WHEN ci.estado='ATENDIDA' THEN 1 ELSE 0 END),
                SUM(CASE WHEN ci.estado='CANCELADA' THEN 1 ELSE 0 END),
                SUM(CASE WHEN i.id_cita IS NOT NULL THEN 1 ELSE 0 END),
                SUM(CASE WHEN EXISTS(SELECT 1 FROM reasignacion_inteligente ra WHERE ra.id_cita_asignada=ci.id_cita AND ra.estado='ACEPTADA') THEN 1 ELSE 0 END),
                SUM(CASE WHEN ci.riesgo_cancelacion='BAJO' THEN 1 ELSE 0 END),
                SUM(CASE WHEN ci.riesgo_cancelacion='MEDIO' THEN 1 ELSE 0 END),
                SUM(CASE WHEN ci.riesgo_cancelacion='ALTO' THEN 1 ELSE 0 END),
                SUM(CASE WHEN ci.riesgo_cancelacion IS NULL OR ci.riesgo_cancelacion NOT IN ('BAJO','MEDIO','ALTO') THEN 1 ELSE 0 END)
                FROM cita ci JOIN medico m ON m.id_medico=ci.id_medico
                LEFT JOIN disponibilidad d ON d.id_disponibilidad=ci.id_disponibilidad
                LEFT JOIN inasistencia_cita i ON i.id_cita=ci.id_cita
                WHERE ci.fecha_hora>=? AND ci.fecha_hora<? AND (?=0 OR COALESCE(d.id_sede,m.id_sede)=?) AND (?=0 OR m.id_especialidad=?)
                GROUP BY COALESCE(d.id_sede,m.id_sede)
                """;
            try(var st=c.prepareStatement(sql)){parametros(st,filtro);try(var r=st.executeQuery()){while(r.next()){
                var anterior=porSede.get(r.getInt(1));
                if(anterior!=null)porSede.put(anterior.id(),new Sede(anterior.id(),anterior.nombre(),anterior.direccion(),anterior.estado(),new Indicadores(r.getLong(2),r.getLong(3),r.getLong(4),r.getLong(5),r.getLong(6),0,0,0,r.getLong(7),r.getLong(8),r.getLong(9),r.getLong(10))));
            }}}
            String ofertas="""
                SELECT COALESCE(d.id_sede,m.id_sede),COUNT(*),
                SUM(CASE WHEN r.estado='ACEPTADA' THEN 1 ELSE 0 END),
                SUM(CASE WHEN r.estado='PENDIENTE' AND r.fecha_limite_respuesta>? AND ci.fecha_hora>? AND ci.estado='CANCELADA' THEN 1 ELSE 0 END)
                FROM reasignacion_inteligente r JOIN cita ci ON ci.id_cita=r.id_cita_original
                JOIN medico m ON m.id_medico=ci.id_medico LEFT JOIN disponibilidad d ON d.id_disponibilidad=ci.id_disponibilidad
                WHERE ci.fecha_hora>=? AND ci.fecha_hora<? AND (?=0 OR COALESCE(d.id_sede,m.id_sede)=?) AND (?=0 OR m.id_especialidad=?)
                GROUP BY COALESCE(d.id_sede,m.id_sede)
                """;
            try(var st=c.prepareStatement(ofertas)){st.setObject(1,LocalDateTime.now(reloj));st.setObject(2,LocalDateTime.now(reloj));parametros(st,filtro,3);try(var r=st.executeQuery()){while(r.next()){
                var s=porSede.get(r.getInt(1));if(s==null)continue;var v=s.datos();
                porSede.put(s.id(),new Sede(s.id(),s.nombre(),s.direccion(),s.estado(),new Indicadores(v.citas(),v.atendidas(),v.canceladas(),v.inasistencias(),v.reasignadas(),r.getLong(2),r.getLong(3),r.getLong(4),v.bajo(),v.medio(),v.alto(),v.sinRiesgo())));
            }}}
            long[] t=new long[12];for(var s:porSede.values()){var v=s.datos();long[] valores={v.citas(),v.atendidas(),v.canceladas(),v.inasistencias(),v.reasignadas(),v.ofertas(),v.aceptadas(),v.pendientes(),v.bajo(),v.medio(),v.alto(),v.sinRiesgo()};for(int n=0;n<t.length;n++)t[n]+=valores[n];}
            var datos=new Indicadores(t[0],t[1],t[2],t[3],t[4],t[5],t[6],t[7],t[8],t[9],t[10],t[11]);
            return new Panel(filtro,datos,List.copyOf(porSede.values()),sedes,especialidades,semanas(c,filtro),auditoria(c),totales(c),LocalDateTime.now(reloj));
        });
    }
    private static List<Opcion> opciones(Connection c,String sql)throws SQLException{
        var lista=new ArrayList<Opcion>();try(var s=c.prepareStatement(sql);var r=s.executeQuery()){while(r.next())lista.add(new Opcion(r.getInt(1),r.getString(2)));}return List.copyOf(lista);
    }
    private static void parametros(PreparedStatement s,Filtro f)throws SQLException{parametros(s,f,1);}
    private static void parametros(PreparedStatement s,Filtro f,int inicio)throws SQLException{
        s.setObject(inicio,f.desde().atStartOfDay());s.setObject(inicio+1,f.hasta().plusDays(1).atStartOfDay());s.setInt(inicio+2,f.sede());s.setInt(inicio+3,f.sede());s.setInt(inicio+4,f.especialidad());s.setInt(inicio+5,f.especialidad());
    }
    private static List<Semana> semanas(Connection c,Filtro f)throws SQLException{
        String sql="SELECT CAST(ci.fecha_hora AS DATE),COUNT(*),SUM(CASE WHEN ci.estado='ATENDIDA' THEN 1 ELSE 0 END) FROM cita ci JOIN medico m ON m.id_medico=ci.id_medico LEFT JOIN disponibilidad d ON d.id_disponibilidad=ci.id_disponibilidad WHERE ci.fecha_hora>=? AND ci.fecha_hora<? AND (?=0 OR COALESCE(d.id_sede,m.id_sede)=?) AND (?=0 OR m.id_especialidad=?) GROUP BY CAST(ci.fecha_hora AS DATE) ORDER BY CAST(ci.fecha_hora AS DATE)";
        int cantidad=(int)(ChronoUnit.DAYS.between(f.desde(),f.hasta())/7)+1;long[][] valores=new long[cantidad][2];
        try(var st=c.prepareStatement(sql)){parametros(st,f);try(var r=st.executeQuery()){while(r.next()){int n=(int)(ChronoUnit.DAYS.between(f.desde(),r.getObject(1,LocalDate.class))/7);valores[n][0]+=r.getLong(2);valores[n][1]+=r.getLong(3);}}}
        var lista=new ArrayList<Semana>();for(int n=0;n<cantidad;n++){var desde=f.desde().plusDays(n*7L);var hasta=desde.plusDays(6);lista.add(new Semana(desde,hasta.isAfter(f.hasta())?f.hasta():hasta,valores[n][0],valores[n][1]));}return List.copyOf(lista);
    }
    private static List<Auditoria> auditoria(Connection c)throws SQLException{
        var lista=new ArrayList<Auditoria>();try(var s=c.prepareStatement("SELECT a.fecha_registro,COALESCE(NULLIF(TRIM(CONCAT(COALESCE(u.nombres,''),' ',COALESCE(u.apellidos,''))),''),'Sistema'),a.evento,a.descripcion FROM auditoria_seguridad a LEFT JOIN usuario u ON u.id_usuario=a.id_usuario ORDER BY a.fecha_registro DESC,a.id_auditoria DESC LIMIT 8");var r=s.executeQuery()){
            while(r.next())lista.add(new Auditoria(r.getObject(1,LocalDateTime.class),r.getString(2),r.getString(3),r.getString(4)));
        }return List.copyOf(lista);
    }
    private static Totales totales(Connection c)throws SQLException{
        long[] n=new long[6];try(var s=c.prepareStatement("SELECT rol,COUNT(*) FROM usuario GROUP BY rol");var r=s.executeQuery()){while(r.next()){long v=r.getLong(2);n[0]+=v;switch(r.getString(1)){case "PACIENTE"->n[1]+=v;case "MEDICO"->n[2]+=v;case "PERSONAL_ADMINISTRATIVO"->n[3]+=v;case "ADMINISTRADOR"->n[4]+=v;default->{}}}}
        try(var s=c.prepareStatement("SELECT COUNT(*) FROM sede");var r=s.executeQuery()){r.next();n[5]=r.getLong(1);}return new Totales(n[0],n[1],n[2],n[3],n[4],n[5]);
    }
}
