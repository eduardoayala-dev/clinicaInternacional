package pe.edu.utp.clinica.recuperacion;

import pe.edu.utp.clinica.auth.SecretosSesion;
import java.sql.*;
import java.time.Instant;

final class LimitesVerificacion {
    private LimitesVerificacion() {}
    static boolean permitir(Connection conn,String clave,int maximo,int segundos,Instant ahora) throws SQLException {
        byte[] hash=SecretosSesion.hash(clave);
        try(var st=conn.prepareStatement("INSERT IGNORE INTO limite_recuperacion(clave_hash,solicitudes,vence_en) VALUES(?,0,?)")) {
            st.setBytes(1,hash); VerificacionJdbc.fecha(st,2,ahora); st.executeUpdate();
        }
        try(var st=conn.prepareStatement("SELECT solicitudes,vence_en FROM limite_recuperacion WHERE clave_hash=? FOR UPDATE")) {
            st.setBytes(1,hash);
            try(var rs=st.executeQuery()) {
                if(!rs.next()) throw new SQLException("No se pudo comprobar el límite.");
                boolean vencido=!ahora.isBefore(VerificacionJdbc.fecha(rs,"vence_en"));
                int contador=vencido?0:rs.getInt("solicitudes");
                if(contador>=maximo) return false;
                Instant vence=vencido?ahora.plusSeconds(segundos):VerificacionJdbc.fecha(rs,"vence_en");
                try(var actualizar=conn.prepareStatement("UPDATE limite_recuperacion SET solicitudes=?,vence_en=? WHERE clave_hash=?")) {
                    actualizar.setInt(1,contador+1); VerificacionJdbc.fecha(actualizar,2,vence); actualizar.setBytes(3,hash); actualizar.executeUpdate();
                }
                return true;
            }
        }
    }
    static boolean solicitud(Connection conn,String ip,String destino,Instant ahora) throws SQLException {
        boolean global=permitir(conn,"solicitudes:global",100,3600,ahora);
        if(global) {
            try(var st=conn.prepareStatement("DELETE FROM limite_recuperacion WHERE vence_en<?")) {
                VerificacionJdbc.fecha(st,1,ahora.minusSeconds(86400)); st.executeUpdate();
            }
        }
        return global && permitir(conn,"solicitudes:ip:"+ip,10,3600,ahora)
                && permitir(conn,"solicitudes:destino:"+destino,3,3600,ahora);
    }
}
