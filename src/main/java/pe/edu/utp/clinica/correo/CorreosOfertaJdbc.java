package pe.edu.utp.clinica.correo;

import pe.edu.utp.clinica.auth.SesionesJdbc;
import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

public final class CorreosOfertaJdbc extends CorreosPendientesJdbc {
    public CorreosOfertaJdbc() {
        this(DatabaseConfig::getConnection);
    }

    public CorreosOfertaJdbc(SesionesJdbc.Conexion conexiones) {
        super(conexiones, OFERTAS);
    }

    public static void encolar(Connection conn, int oferta, Instant ahora) throws SQLException {
        new pe.edu.utp.clinica.repository.CorreoDatosRepository(conn)
                .encolarOferta(oferta, utc(ahora));
    }

    @Override
    public Optional<Correo> mensaje(int oferta, Instant ahora) throws SQLException {
        try (var conn = conexiones.abrir()) {
            return new pe.edu.utp.clinica.repository.CorreoDatosRepository(conn)
                    .oferta(oferta, LocalDateTime.ofInstant(ahora, ZoneId.of("America/Lima")))
                    .map(OfertaCupo::correo);
        }
    }
}
