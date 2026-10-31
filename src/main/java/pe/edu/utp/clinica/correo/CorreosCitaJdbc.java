package pe.edu.utp.clinica.correo;

import pe.edu.utp.clinica.auth.SesionesJdbc;
import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.*;
import java.time.*;
import java.util.Optional;

public final class CorreosCitaJdbc {
    public record Pendiente(int cita,int intentos) {}
    public enum Resultado { ENVIADO, NO_CONFIRMADO, OMITIDO }
    private final SesionesJdbc.Conexion conexiones;

    public CorreosCitaJdbc(){this(DatabaseConfig::getConnection);}
    public CorreosCitaJdbc(SesionesJdbc.Conexion conexiones){this.conexiones=conexiones;}

    public static void encolar(Connection conn,int cita,Instant ahora)throws SQLException {
        try(var st=conn.prepareStatement("INSERT INTO correo_cita(id_cita,estado,intentos,disponible_en) VALUES(?,'PENDIENTE',0,?)")) {
            st.setInt(1,cita);
            st.setObject(2,utc(ahora));
            st.executeUpdate();
        }
    }

    public Optional<Pendiente> tomar(Instant ahora)throws SQLException {
        try(var conn=conexiones.abrir()) {
            return tomarEnTransaccion(conn,ahora);
        }
    }

    private Optional<Pendiente> tomarEnTransaccion(Connection conn,Instant ahora)throws SQLException {
        conn.setAutoCommit(false);
        try {
            recuperarInterrumpidos(conn,ahora);
            Optional<Pendiente> pendiente=seleccionar(conn,ahora);
            conn.commit();
            return pendiente;
        } catch(SQLException|RuntimeException e) {
            conn.rollback();
            throw e;
        }
    }

    public Optional<Instant> proxima()throws SQLException {
        try(var conn=conexiones.abrir()) {
            Instant pendiente=fechaMinima(conn,false);
            Instant enviando=fechaMinima(conn,true);
            if(enviando!=null)enviando=enviando.plusSeconds(901);
            if(pendiente==null)return Optional.ofNullable(enviando);
            return Optional.of(enviando==null || pendiente.isBefore(enviando)?pendiente:enviando);
        }
    }

    private Instant fechaMinima(Connection conn,boolean enviando)throws SQLException {
        try(var st=conn.prepareStatement(enviando
                ?"SELECT MIN(iniciada_en) FROM correo_cita WHERE estado='ENVIANDO'"
                :"SELECT MIN(disponible_en) FROM correo_cita WHERE estado='PENDIENTE'" );var rs=st.executeQuery()) {
            rs.next();LocalDateTime fecha=rs.getObject(1,LocalDateTime.class);
            return fecha==null?null:fecha.toInstant(ZoneOffset.UTC);
        }
    }

    private Optional<Pendiente> seleccionar(Connection conn,Instant ahora)throws SQLException {
        String sql="SELECT id_cita,intentos FROM correo_cita WHERE estado='PENDIENTE' AND disponible_en<=? AND intentos<3 ORDER BY disponible_en,id_cita LIMIT 1 FOR UPDATE";
        Pendiente pendiente;
        try(var st=conn.prepareStatement(sql)) {
            st.setObject(1,utc(ahora));
            try(var rs=st.executeQuery()) {
                if(!rs.next())return Optional.empty();
                pendiente=new Pendiente(rs.getInt(1),rs.getInt(2)+1);
            }
        }
        try(var st=conn.prepareStatement("UPDATE correo_cita SET estado='ENVIANDO',intentos=intentos+1,iniciada_en=? WHERE id_cita=? AND estado='PENDIENTE'")) {
            st.setObject(1,utc(ahora));st.setInt(2,pendiente.cita());
            if(st.executeUpdate()!=1)return Optional.empty();
        }
        return Optional.of(pendiente);
    }

    private void recuperarInterrumpidos(Connection conn,Instant ahora)throws SQLException {
        try(var st=conn.prepareStatement("UPDATE correo_cita SET estado='NO_CONFIRMADO',finalizada_en=? WHERE estado='ENVIANDO' AND iniciada_en<?")) {
            st.setObject(1,utc(ahora));st.setObject(2,utc(ahora.minusSeconds(900)));st.executeUpdate();
        }
    }

    public Optional<ConfirmacionCita> datos(int cita,Instant ahora)throws SQLException {
        String sql="SELECT u.email,u.nombres,c.fecha_hora,um.nombres AS medico_nombres,um.apellidos AS medico_apellidos,e.nombre AS especialidad,s.nombre AS sede FROM cita c JOIN paciente p ON p.id_paciente=c.id_paciente JOIN usuario u ON u.id_usuario=p.id_usuario JOIN medico m ON m.id_medico=c.id_medico JOIN usuario um ON um.id_usuario=m.id_usuario JOIN especialidad e ON e.id_especialidad=m.id_especialidad JOIN sede s ON s.id_sede=m.id_sede WHERE c.id_cita=? AND c.estado='CONFIRMADA' AND u.activo=1 AND u.rol='PACIENTE'";
        try(var conn=conexiones.abrir();var st=conn.prepareStatement(sql)) {
            st.setInt(1,cita);
            try(var rs=st.executeQuery()) {
                if(!rs.next())return Optional.empty();
                LocalDateTime fecha=rs.getObject("fecha_hora",LocalDateTime.class);
                if(fecha==null || !fecha.isAfter(LocalDateTime.ofInstant(ahora,ZoneId.of("America/Lima"))))return Optional.empty();
                return Optional.of(new ConfirmacionCita(cita,rs.getString("email"),rs.getString("nombres"),
                        rs.getString("medico_nombres")+" "+rs.getString("medico_apellidos"),rs.getString("especialidad"),rs.getString("sede"),fecha));
            }
        }
    }

    public void terminar(int cita,Resultado resultado,Instant ahora)throws SQLException {
        try(var conn=conexiones.abrir();var st=conn.prepareStatement("UPDATE correo_cita SET estado=?,finalizada_en=? WHERE id_cita=? AND estado='ENVIANDO'")) {
            st.setString(1,resultado.name());st.setObject(2,utc(ahora));st.setInt(3,cita);st.executeUpdate();
        }
    }

    public void reintentar(Pendiente pendiente,Instant ahora)throws SQLException {
        if(pendiente.intentos()>=3) { terminar(pendiente.cita(),Resultado.NO_CONFIRMADO,ahora);return; }
        try(var conn=conexiones.abrir();var st=conn.prepareStatement("UPDATE correo_cita SET estado='PENDIENTE',disponible_en=?,iniciada_en=NULL WHERE id_cita=? AND estado='ENVIANDO'")) {
            st.setObject(1,utc(ahora.plusSeconds(pendiente.intentos()==1?60:300)));
            st.setInt(2,pendiente.cita());st.executeUpdate();
        }
    }

    private static LocalDateTime utc(Instant instante){return LocalDateTime.ofInstant(instante,ZoneOffset.UTC);}
}
