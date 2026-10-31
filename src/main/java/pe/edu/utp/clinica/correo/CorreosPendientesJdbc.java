package pe.edu.utp.clinica.correo;

import pe.edu.utp.clinica.auth.SesionesJdbc;
import java.sql.*;
import java.time.*;
import java.util.Optional;

public abstract class CorreosPendientesJdbc {
    public record Pendiente(int id,int intentos) {}
    public enum Resultado { ENVIADO, NO_CONFIRMADO, OMITIDO }
    protected final SesionesJdbc.Conexion conexiones;
    private final Consultas consultas;
    protected record Consultas(String enviando, String proxima, String seleccionar, String tomar, String recuperar, String terminar, String reintentar) {}
    protected static final Consultas CITAS=new Consultas(
        "SELECT MIN(iniciada_en) FROM correo_cita WHERE estado='ENVIANDO'",
        "SELECT MIN(disponible_en) FROM correo_cita WHERE estado='PENDIENTE'",
        "SELECT id_cita,intentos FROM correo_cita WHERE estado='PENDIENTE' AND disponible_en<=? AND intentos<3 ORDER BY disponible_en,id_cita LIMIT 1 FOR UPDATE SKIP LOCKED",
        "UPDATE correo_cita SET estado='ENVIANDO',intentos=intentos+1,iniciada_en=? WHERE id_cita=? AND estado='PENDIENTE'",
        "UPDATE correo_cita SET estado='NO_CONFIRMADO',finalizada_en=? WHERE estado='ENVIANDO' AND iniciada_en<?",
        "UPDATE correo_cita SET estado=?,finalizada_en=? WHERE id_cita=? AND estado='ENVIANDO'",
        "UPDATE correo_cita SET estado='PENDIENTE',disponible_en=?,iniciada_en=NULL WHERE id_cita=? AND estado='ENVIANDO'");
    protected static final Consultas OFERTAS=new Consultas(
        "SELECT MIN(iniciada_en) FROM correo_oferta WHERE estado='ENVIANDO'",
        "SELECT MIN(disponible_en) FROM correo_oferta WHERE estado='PENDIENTE'",
        "SELECT id_oferta,intentos FROM correo_oferta WHERE estado='PENDIENTE' AND disponible_en<=? AND intentos<3 ORDER BY disponible_en,id_oferta LIMIT 1 FOR UPDATE SKIP LOCKED",
        "UPDATE correo_oferta SET estado='ENVIANDO',intentos=intentos+1,iniciada_en=? WHERE id_oferta=? AND estado='PENDIENTE'",
        "UPDATE correo_oferta SET estado='NO_CONFIRMADO',finalizada_en=? WHERE estado='ENVIANDO' AND iniciada_en<?",
        "UPDATE correo_oferta SET estado=?,finalizada_en=? WHERE id_oferta=? AND estado='ENVIANDO'",
        "UPDATE correo_oferta SET estado='PENDIENTE',disponible_en=?,iniciada_en=NULL WHERE id_oferta=? AND estado='ENVIANDO'");


    protected CorreosPendientesJdbc(SesionesJdbc.Conexion conexiones, Consultas consultas) {
        this.conexiones=conexiones; this.consultas=consultas;
    }
    public abstract Optional<Correo> mensaje(int id,Instant ahora) throws SQLException;

    public Optional<Pendiente> tomar(Instant ahora)throws SQLException {
        try(var conn=conexiones.abrir()) {
            return tomarEnTransaccion(conn,ahora);
        }
    }

    private Optional<Pendiente> tomarEnTransaccion(Connection conn,Instant ahora)throws SQLException {
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
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
                ?consultas.enviando
                :consultas.proxima );var rs=st.executeQuery()) {
            rs.next();LocalDateTime fecha=rs.getObject(1,LocalDateTime.class);
            return fecha==null?null:fecha.toInstant(ZoneOffset.UTC);
        }
    }

    private Optional<Pendiente> seleccionar(Connection conn,Instant ahora)throws SQLException {
        String sql=consultas.seleccionar;
        Pendiente pendiente;
        try(var st=conn.prepareStatement(sql)) {
            st.setObject(1,utc(ahora));
            try(var rs=st.executeQuery()) {
                if(!rs.next())return Optional.empty();
                pendiente=new Pendiente(rs.getInt(1),rs.getInt(2)+1);
            }
        }
        try(var st=conn.prepareStatement(consultas.tomar)) {
            st.setObject(1,utc(ahora));st.setInt(2,pendiente.id());
            if(st.executeUpdate()!=1)return Optional.empty();
        }
        return Optional.of(pendiente);
    }

    private void recuperarInterrumpidos(Connection conn,Instant ahora)throws SQLException {
        try(var st=conn.prepareStatement(consultas.recuperar)) {
            st.setObject(1,utc(ahora));st.setObject(2,utc(ahora.minusSeconds(900)));st.executeUpdate();
        }
    }

    public void terminar(int id,Resultado resultado,Instant ahora)throws SQLException {
        try(var conn=conexiones.abrir();var st=conn.prepareStatement(consultas.terminar)) {
            st.setString(1,resultado.name());st.setObject(2,utc(ahora));st.setInt(3,id);st.executeUpdate();
        }
    }

    public void reintentar(Pendiente pendiente,Instant ahora)throws SQLException {
        if(pendiente.intentos()>=3) { terminar(pendiente.id(),Resultado.NO_CONFIRMADO,ahora);return; }
        try(var conn=conexiones.abrir();var st=conn.prepareStatement(consultas.reintentar)) {
            st.setObject(1,utc(ahora.plusSeconds(pendiente.intentos()==1?60:300)));
            st.setInt(2,pendiente.id());st.executeUpdate();
        }
    }

    protected static LocalDateTime utc(Instant instante){return LocalDateTime.ofInstant(instante,ZoneOffset.UTC);}
}
