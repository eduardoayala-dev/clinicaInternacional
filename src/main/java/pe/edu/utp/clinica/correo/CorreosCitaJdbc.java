package pe.edu.utp.clinica.correo;

import pe.edu.utp.clinica.auth.SesionesJdbc;
import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.*;
import java.time.*;
import java.util.Optional;

public final class CorreosCitaJdbc extends CorreosPendientesJdbc {
    public CorreosCitaJdbc() {
        this(DatabaseConfig::getConnection);
    }

    public CorreosCitaJdbc(SesionesJdbc.Conexion conexiones) {
        super(conexiones, CITAS);
    }

    public static void encolar(Connection conn, int cita, Instant ahora) throws SQLException {
        new pe.edu.utp.clinica.repository.ReservaCitaRepository(conn).correoReserva(cita, ahora);
    }

    public Optional<ConfirmacionCita> datos(int cita, Instant ahora) throws SQLException {
        try (var conn = conexiones.abrir()) {
            return new pe.edu.utp.clinica.repository.CorreoDatosRepository(conn)
                    .cita(cita, LocalDateTime.ofInstant(ahora, ZoneId.of("America/Lima")));
        }
    }

    @Override
    public Optional<Correo> mensaje(int id, Instant ahora) throws SQLException {
        return datos(id, ahora).map(ConfirmacionCita::correo);
    }
}
