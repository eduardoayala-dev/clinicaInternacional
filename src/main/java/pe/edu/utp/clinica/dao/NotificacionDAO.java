package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class NotificacionDAO {
    private static final String RECORDATORIO = "Recordatorio de cita";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd MMM yyyy",Locale.forLanguageTag("es"));
    private final AgendaCitaDAO.ConnectionSource connections;
    private final Clock clock;

    public NotificacionDAO() { this(DatabaseConfig::getConnection,Clock.system(ZoneId.of("America/Lima"))); }
    NotificacionDAO(AgendaCitaDAO.ConnectionSource connections,Clock clock) { this.connections=connections;this.clock=clock; }

    public boolean crearNotificacion(int usuario,Integer cita,String tipo,String mensaje) {
        if(usuario<=0 || mensaje==null || mensaje.isBlank()) return false;
        String sql="INSERT INTO notificacion(id_usuario,id_cita,tipo,canal,mensaje,estado_envio,leida,fecha_envio) VALUES(?,?,?,'PLATAFORMA',?,'ENVIADA',0,?)";
        try(Connection conn=connections.open();PreparedStatement ps=conn.prepareStatement(sql)) {
            ps.setInt(1,usuario);
            if(cita==null) ps.setNull(2,Types.INTEGER); else ps.setInt(2,cita);
            ps.setString(3,tipo==null?"Aviso del Sistema":tipo);
            ps.setString(4,mensaje);
            ps.setObject(5,LocalDateTime.now(clock));
            return ps.executeUpdate()==1;
        } catch(SQLException e) { throw new CatalogoDatosException(e); }
    }

    public List<Map<String,Object>> obtenerNotificacionesUsuario(int usuario) {
        if(usuario<=0) return List.of();
        String sql="SELECT n.id_notificacion,n.id_cita,n.tipo,n.mensaje,n.fecha_envio,n.leida,EXISTS(SELECT 1 FROM cita c JOIN paciente p ON c.id_paciente=p.id_paciente WHERE c.id_cita=n.id_cita AND p.id_usuario=n.id_usuario AND c.estado IN ('SOLICITADA','CONFIRMADA')) AS confirmable FROM notificacion n WHERE n.id_usuario=? ORDER BY n.fecha_envio DESC,n.id_notificacion DESC";
        List<Map<String,Object>> rows=new ArrayList<>();
        try(Connection conn=connections.open();PreparedStatement ps=conn.prepareStatement(sql)) {
            ps.setInt(1,usuario);
            try(ResultSet rs=ps.executeQuery()) { while(rs.next()) rows.add(presentar(rs)); }
            return rows;
        } catch(SQLException e) { throw new CatalogoDatosException(e); }
    }

    private static Map<String,Object> presentar(ResultSet rs) throws SQLException {
        Map<String,Object> data=new LinkedHashMap<>();
        String tipo=rs.getString("tipo");
        String mensaje=rs.getString("mensaje");
        LocalDateTime fecha=rs.getObject("fecha_envio",LocalDateTime.class);
        boolean leida=rs.getInt("leida")==1;
        String categoria=categoria(tipo,mensaje);
        String dia=fecha==null?"Sin fecha":fecha.format(FECHA);
        String hora=fecha==null?"—":fecha.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        data.put("idNotificacion",rs.getInt("id_notificacion"));
        data.put("idCita",rs.getObject("id_cita"));
        data.put("mensaje",mensaje);data.put("detalle",mensaje);
        data.put("fecha",dia);data.put("hora",hora);data.put("fechaEnvio",dia+" · "+hora);
        data.put("leida",leida);data.put("estado",leida?"Leído":"Pendiente");
        data.put("tipo",tipo);data.put("categoria",categoria);
        String tipoAviso=tipo==null?"":tipo;
        boolean recordatorio=Set.of(RECORDATORIO,"Recordatorio").contains(tipoAviso);
        data.put("accion",!leida && recordatorio && rs.getBoolean("confirmable")?"Confirmar":"Ver");
        return data;
    }
    private static String categoria(String tipo,String mensaje) {
        String clase=tipo==null?"":tipo.toUpperCase(Locale.ROOT);
        String texto=mensaje==null?"":mensaje.toUpperCase(Locale.ROOT);
        if(clase.startsWith("RECORDATORIO")) return RECORDATORIO;
        if(clase.contains("ESPERA")) return "Lista de espera";
        if(clase.contains("RESERVA") || texto.startsWith("CITA CONFIRMADA") || texto.startsWith("CITA AGENDADA")) return "Reserva registrada";
        return "Aviso del Sistema";
    }

    public int contarNotificacionesSinLeer(int usuario) {
        if(usuario<=0) return 0;
        try(Connection conn=connections.open();PreparedStatement ps=conn.prepareStatement("SELECT COUNT(*) FROM notificacion WHERE id_usuario=? AND leida=0")) {
            ps.setInt(1,usuario);
            try(ResultSet rs=ps.executeQuery()) { rs.next();return rs.getInt(1); }
        } catch(SQLException e) { throw new CatalogoDatosException(e); }
    }

    public boolean marcarNotificacionesComoLeidas(int usuario) {
        if(usuario<=0) return false;
        try(Connection conn=connections.open();PreparedStatement ps=conn.prepareStatement("UPDATE notificacion SET leida=1,estado_envio='LEIDA' WHERE id_usuario=? AND leida=0")) {
            ps.setInt(1,usuario);ps.executeUpdate();return true;
        } catch(SQLException e) { throw new CatalogoDatosException(e); }
    }

    public boolean marcarNotificacionIndividualLeida(int id,int usuario) {
        if(id<=0 || usuario<=0) return false;
        try(Connection conn=connections.open();PreparedStatement ps=conn.prepareStatement("UPDATE notificacion SET leida=1,estado_envio='LEIDA' WHERE id_notificacion=? AND id_usuario=?")) {
            ps.setInt(1,id);ps.setInt(2,usuario);return ps.executeUpdate()==1;
        } catch(SQLException e) { throw new CatalogoDatosException(e); }
    }

    public boolean confirmarRecordatorioCita(int id,int usuario) {
        if(id<=0 || usuario<=0) return false;
        return TransaccionJdbc.ejecutar(connections,conn->{
            int cita;
            String estado;
            String sql="SELECT c.id_cita,c.estado FROM notificacion n JOIN cita c ON n.id_cita=c.id_cita JOIN paciente p ON c.id_paciente=p.id_paciente WHERE n.id_notificacion=? AND n.id_usuario=? AND p.id_usuario=? AND n.tipo IN ('Recordatorio de cita','Recordatorio') FOR UPDATE";
            try(PreparedStatement ps=conn.prepareStatement(sql)) {
                ps.setInt(1,id);ps.setInt(2,usuario);ps.setInt(3,usuario);
                try(ResultSet rs=ps.executeQuery()) {
                    if(!rs.next()) return false;
                    cita=rs.getInt(1);estado=rs.getString(2);
                }
            }
            if(!Set.of("SOLICITADA","CONFIRMADA").contains(estado)) return false;
            try(PreparedStatement ps=conn.prepareStatement("UPDATE cita SET estado='CONFIRMADA' WHERE id_cita=? AND estado='SOLICITADA'")) {
                ps.setInt(1,cita);ps.executeUpdate();
            }
            try(PreparedStatement ps=conn.prepareStatement("UPDATE notificacion SET leida=1,estado_envio='LEIDA' WHERE id_notificacion=? AND id_usuario=?")) {
                ps.setInt(1,id);ps.setInt(2,usuario);return ps.executeUpdate()==1;
            }
        });
    }
}
