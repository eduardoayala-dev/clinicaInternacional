package pe.edu.utp.clinica.recuperacion;

import pe.edu.utp.clinica.auth.SecretosSesion;
import pe.edu.utp.clinica.repository.LimiteVerificacionRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

final class LimitesVerificacion {
    private LimitesVerificacion() {}

    static boolean permitir(Connection conn, String clave, int maximo, int segundos, Instant ahora)
            throws SQLException {
        return new LimiteVerificacionRepository(conn)
                .permitir(SecretosSesion.hash(clave), maximo, segundos, ahora);
    }

    static boolean solicitud(Connection conn, String ip, String destino, Instant ahora)
            throws SQLException {
        boolean global = permitir(conn, "solicitudes:global", 100, 3600, ahora);
        if (global) new LimiteVerificacionRepository(conn).limpiar(ahora.minusSeconds(86400));
        return global
                && permitir(conn, "solicitudes:ip:" + ip, 10, 3600, ahora)
                && permitir(conn, "solicitudes:destino:" + destino, 3, 3600, ahora);
    }
}
