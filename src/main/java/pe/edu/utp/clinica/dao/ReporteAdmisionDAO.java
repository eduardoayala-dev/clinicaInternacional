package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.reporte.FiltroReporte;
import java.sql.*;
import java.time.*;
import java.util.*;

public class ReporteAdmisionDAO {
    private static final String INDICADORES="indicadores";
    private final AgendaCitaDAO.ConnectionSource conexiones;
    private final Clock clock;
    public record Cita(int id,LocalDateTime fecha,String paciente,String documento,String correo,String telefono,String medico,String especialidad,String estado,String riesgo,String confirmacion) {}
    public record Resumen(long total,long atendidas,long enAtencion,long pendientes,long canceladas,long bajo,long medio,long alto,long sinRiesgo,Double esperaMedia,long medicos,long ofertas,long aceptadas) {}
    public record Reporte(String sede,FiltroReporte filtro,Resumen resumen,List<Cita> citas,int pagina,int paginas,LocalDateTime actualizado) {}
    private static final String FILTROS="""
         FROM cita c
         JOIN medico m ON m.id_medico=c.id_medico
         JOIN usuario um ON um.id_usuario=m.id_usuario
         JOIN especialidad e ON e.id_especialidad=m.id_especialidad
         JOIN paciente p ON p.id_paciente=c.id_paciente
         JOIN usuario u ON u.id_usuario=p.id_usuario
         LEFT JOIN confirmacion_asistencia ca ON ca.id_cita=c.id_cita
         LEFT JOIN admision_cita a ON a.id_cita=c.id_cita
         LEFT JOIN atencion_cita t ON t.id_cita=c.id_cita
         LEFT JOIN disponibilidad d ON d.id_disponibilidad=c.id_disponibilidad
         WHERE COALESCE(d.id_sede,m.id_sede)=? AND c.fecha_hora>=? AND c.fecha_hora<?
         AND (?='' OR c.estado=?)
         AND (?<>'contacto' OR (c.estado IN ('SOLICITADA','CONFIRMADA')
             AND ca.confirmada_en IS NULL AND (c.riesgo_cancelacion IN ('ALTO','MEDIO') OR ca.vence_en<=?)))
        """;
    private static final String SQL_RESUMEN="""
        SELECT COUNT(*),
        COALESCE(SUM(CASE WHEN c.estado='ATENDIDA' THEN 1 ELSE 0 END),0),
        COALESCE(SUM(CASE WHEN c.estado='EN_ATENCION' THEN 1 ELSE 0 END),0),
        COALESCE(SUM(CASE WHEN c.estado IN ('SOLICITADA','CONFIRMADA','EN_SALA','EN_ATENCION') THEN 1 ELSE 0 END),0),
        COALESCE(SUM(CASE WHEN c.estado='CANCELADA' THEN 1 ELSE 0 END),0),
        COALESCE(SUM(CASE WHEN c.estado IN ('SOLICITADA','CONFIRMADA','EN_SALA','EN_ATENCION') AND c.riesgo_cancelacion='BAJO' THEN 1 ELSE 0 END),0),
        COALESCE(SUM(CASE WHEN c.estado IN ('SOLICITADA','CONFIRMADA','EN_SALA','EN_ATENCION') AND c.riesgo_cancelacion='MEDIO' THEN 1 ELSE 0 END),0),
        COALESCE(SUM(CASE WHEN c.estado IN ('SOLICITADA','CONFIRMADA','EN_SALA','EN_ATENCION') AND c.riesgo_cancelacion='ALTO' THEN 1 ELSE 0 END),0),
        AVG(CASE WHEN t.fecha_inicio>=a.fecha_llegada THEN TIMESTAMPDIFF(SECOND,a.fecha_llegada,t.fecha_inicio)/60.0 ELSE NULL END)
        """+FILTROS;
    private static final String SQL_CITAS="""
        SELECT c.id_cita,c.fecha_hora,CONCAT(u.nombres,' ',u.apellidos),p.dni,u.email,p.telefono,
        CONCAT(um.nombres,' ',um.apellidos),e.nombre,c.estado,c.riesgo_cancelacion,ca.confirmada_en,ca.vence_en
        """+FILTROS+" ORDER BY c.fecha_hora,c.id_cita LIMIT ? OFFSET ?";
    private static final String SQL_OFERTAS="""
        SELECT COUNT(*),COALESCE(SUM(CASE WHEN r.estado='ACEPTADA' THEN 1 ELSE 0 END),0)
        FROM reasignacion_inteligente r JOIN cita c ON c.id_cita=r.id_cita_original
        JOIN medico m ON m.id_medico=c.id_medico
        LEFT JOIN disponibilidad d ON d.id_disponibilidad=c.id_disponibilidad
        WHERE COALESCE(d.id_sede,m.id_sede)=? AND c.fecha_hora>=? AND c.fecha_hora<? AND (?='' OR c.estado=?)
        """;
    public ReporteAdmisionDAO(){this(DatabaseConfig::getConnection,Clock.system(ZoneId.of("America/Lima")));}
    ReporteAdmisionDAO(AgendaCitaDAO.ConnectionSource conexiones,Clock clock){this.conexiones=conexiones;this.clock=clock.withZone(ZoneId.of("America/Lima"));}
    public Reporte consultar(int usuario,FiltroReporte filtro,int pagina,boolean exportar) {
        if(pagina<1 || pagina>100000)throw new AgendaException(400,"PAGINA_INVALIDA","Selecciona una página válida.");
        try(Connection c=conexiones.open()) {
            return consultarEnTransaccion(c,usuario,filtro,pagina,exportar);
        } catch(SQLException e){throw new AgendaException(503,"REPORTE_NO_DISPONIBLE","No se pudo consultar el reporte. Inténtalo nuevamente.");}
    }
    private Reporte consultarEnTransaccion(Connection c,int usuario,FiltroReporte filtro,int pagina,boolean exportar)throws SQLException {
        c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        c.setAutoCommit(false);
        try {
            int sede=AmbitoAdmisionDAO.sede(c,usuario);
            LocalDateTime ahora=LocalDateTime.now(clock).withNano(0);
            Resumen resumen=resumen(c,sede,filtro,ahora);
            if(exportar && !INDICADORES.equals(filtro.tipo()) && resumen.total()>2000)throw new AgendaException(422,"REPORTE_DEMASIADO_GRANDE","El reporte supera 2000 citas. Reduce el período o filtra por estado.");
            int paginas=Math.max(1,(int)((resumen.total()+19)/20));
            int actual=Math.min(pagina,paginas);
            int limite=exportar?2000:20;
            int offset=exportar?0:(actual-1)*20;
            List<Cita> citas=INDICADORES.equals(filtro.tipo())?List.of():citas(c,sede,filtro,ahora,limite,offset);
            String nombre=nombreSede(c,sede);
            c.commit();
            return new Reporte(nombre,filtro,resumen,List.copyOf(citas),actual,paginas,ahora);
        } catch(SQLException|RuntimeException e){c.rollback();throw e;}
    }
    private static String nombreSede(Connection c,int sede)throws SQLException {
        try(var ps=c.prepareStatement("SELECT nombre FROM sede WHERE id_sede=?")) {
            ps.setInt(1,sede);
            try(var rs=ps.executeQuery()){rs.next();return rs.getString(1);}
        }
    }
    private static void filtros(PreparedStatement ps,int sede,FiltroReporte f,LocalDateTime ahora)throws SQLException {
        ps.setInt(1,sede);
        ps.setObject(2,f.desde().atStartOfDay());
        ps.setObject(3,f.hasta().plusDays(1).atStartOfDay());
        ps.setString(4,f.estado());ps.setString(5,f.estado());ps.setString(6,f.tipo());ps.setObject(7,ahora);
    }
    private Resumen resumen(Connection c,int sede,FiltroReporte f,LocalDateTime ahora)throws SQLException {
        long[] n=new long[8];
        Double espera;
        try(var ps=c.prepareStatement(SQL_RESUMEN)) {
            filtros(ps,sede,f,ahora);
            try(var rs=ps.executeQuery()) {
                rs.next();
                for(int i=0;i<8;i++){n[i]=rs.getLong(i+1);}
                espera=rs.getObject(9)==null?null:rs.getDouble(9);
            }
        }
        long medicos;
        try(var ps=c.prepareStatement("SELECT COUNT(*) FROM medico m JOIN usuario u ON u.id_usuario=m.id_usuario WHERE m.id_sede=? AND m.estado='ACTIVO' AND u.activo=TRUE AND u.rol='MEDICO'")) {
            ps.setInt(1,sede);try(var rs=ps.executeQuery()){rs.next();medicos=rs.getLong(1);}
        }
        long ofertas;
        long aceptadas;
        try(var ps=c.prepareStatement(SQL_OFERTAS)) {
            ps.setInt(1,sede);ps.setObject(2,f.desde().atStartOfDay());ps.setObject(3,f.hasta().plusDays(1).atStartOfDay());ps.setString(4,f.estado());ps.setString(5,f.estado());
            try(var rs=ps.executeQuery()){rs.next();ofertas=rs.getLong(1);aceptadas=rs.getLong(2);}
        }
        return new Resumen(n[0],n[1],n[2],n[3],n[4],n[5],n[6],n[7],Math.max(0,n[3]-n[5]-n[6]-n[7]),espera,medicos,ofertas,aceptadas);
    }
    private List<Cita> citas(Connection c,int sede,FiltroReporte f,LocalDateTime ahora,int limite,int offset)throws SQLException {
        try(var ps=c.prepareStatement(SQL_CITAS)) {
            filtros(ps,sede,f,ahora);ps.setInt(8,limite);ps.setInt(9,offset);
            try(var rs=ps.executeQuery()) {
                List<Cita> lista=new ArrayList<>();
                while(rs.next()){lista.add(leerCita(rs,ahora));}
                return lista;
            }
        }
    }
    private static Cita leerCita(ResultSet rs,LocalDateTime ahora)throws SQLException {
        LocalDateTime vencimiento=rs.getObject(12,LocalDateTime.class);
        String confirmacion;
        if(rs.getObject(11)!=null)confirmacion="Confirmada";
        else if(vencimiento==null)confirmacion="Sin plazo registrado";
        else confirmacion=ahora.isBefore(vencimiento)?"Pendiente":"Plazo vencido";
        return new Cita(rs.getInt(1),rs.getObject(2,LocalDateTime.class),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),Objects.toString(rs.getString(9),"SIN_ESTADO"),Objects.toString(rs.getString(10),"SIN_DATO"),confirmacion);
    }
}
