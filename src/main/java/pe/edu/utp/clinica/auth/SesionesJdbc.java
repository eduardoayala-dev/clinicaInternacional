package pe.edu.utp.clinica.auth;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.model.Usuario;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;

public final class SesionesJdbc implements AlmacenSesiones {
    @FunctionalInterface public interface Conexion { Connection abrir() throws SQLException; }
    private final Conexion conexion;
    public SesionesJdbc() { this(DatabaseConfig::getConnection); }
    public SesionesJdbc(Conexion conexion) { this.conexion = conexion; }
    private static Calendar utc() { return Calendar.getInstance(TimeZone.getTimeZone("UTC")); }
    private static void fecha(PreparedStatement st, int index, Instant value) throws SQLException {
        st.setTimestamp(index, Timestamp.from(value), utc());
    }
    private static Instant fecha(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column, utc()).toInstant();
    }
    @Override public void crear(String id, Usuario usuario, byte[] refresh, String csrf, Instant now, Instant vence) throws SQLException {
        String sql = "INSERT INTO sesion_autenticacion(id_sesion,id_usuario,refresh_hash,csrf_token,rol_inicial,credencial_hash,creada_en,ultima_actividad_en,vence_en) "
                + "SELECT ?,u.id_usuario,?,?,?,?,?,?,? FROM usuario u WHERE u.id_usuario=? AND u.activo=1 AND u.rol=? AND u.password_hash=?";
        try (var conn = conexion.abrir(); var st = conn.prepareStatement(sql)) {
            st.setString(1,id); st.setBytes(2,refresh); st.setString(3,csrf); st.setString(4,usuario.getRol());
            st.setBytes(5,SecretosSesion.hash(usuario.getPasswordHash()));
            fecha(st,6,now); fecha(st,7,now); fecha(st,8,vence);
            st.setInt(9,usuario.getIdUsuario()); st.setString(10,usuario.getRol()); st.setString(11,usuario.getPasswordHash());
            if (st.executeUpdate() != 1) throw new SQLException("La identidad cambió antes de crear el acceso.");
        }
    }
    @Override public SesionAutenticada resolver(String id, byte[] refresh, Instant now, boolean actividad) throws SQLException {
        String predicate = id != null ? "s.id_sesion=?" : "s.refresh_hash=?";
        String sql = "SELECT s.*,u.nombres,u.apellidos,u.email,u.password_hash,u.rol,u.activo "
                + "FROM sesion_autenticacion s JOIN usuario u ON u.id_usuario=s.id_usuario WHERE " + predicate + " FOR UPDATE";
        try (var conn = conexion.abrir()) {
            conn.setAutoCommit(false);
            try {
                SesionAutenticada sesion = null;
                try (var st = conn.prepareStatement(sql)) {
                    if (id != null) st.setString(1,id); else st.setBytes(1,refresh);
                    try (var rs = st.executeQuery()) {
                        if (rs.next()) {
                            String sid=rs.getString("id_sesion"), rol=rs.getString("rol");
                            Instant vence=fecha(rs,"vence_en");
                            boolean valida=rs.getTimestamp("revocada_en")==null && rs.getBoolean("activo")
                                    && now.isBefore(vence) && now.isBefore(fecha(rs,"ultima_actividad_en").plusSeconds(1800))
                                    && rol.equals(rs.getString("rol_inicial"))
                                    && MessageDigest.isEqual(rs.getBytes("credencial_hash"),SecretosSesion.hash(rs.getString("password_hash")));
                            if (valida) {
                                var usuario=new Usuario(rs.getInt("id_usuario"),rs.getString("nombres"),rs.getString("apellidos"),
                                        rs.getString("email"),rs.getString("password_hash"),rol,null,true);
                                sesion=new SesionAutenticada(sid,usuario,rs.getString("csrf_token"),vence);
                                if (actividad) actualizar(conn,sid,now,false);
                            } else actualizar(conn,sid,now,true);
                        }
                    }
                }
                conn.commit();
                return sesion;
            } catch (SQLException | RuntimeException e) { conn.rollback(); throw e; }
        }
    }
    private void actualizar(Connection conn, String id, Instant now, boolean revocar) throws SQLException {
        String sql=revocar ? "UPDATE sesion_autenticacion SET revocada_en=COALESCE(revocada_en,?) WHERE id_sesion=?"
                : "UPDATE sesion_autenticacion SET ultima_actividad_en=? WHERE id_sesion=? AND revocada_en IS NULL";
        try (var st=conn.prepareStatement(sql)) { fecha(st,1,now); st.setString(2,id); st.executeUpdate(); }
    }
    @Override public void revocar(String id, Instant now) throws SQLException {
        try (var conn=conexion.abrir()) { actualizar(conn,id,now,true); }
    }
}
