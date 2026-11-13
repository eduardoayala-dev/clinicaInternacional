package pe.edu.utp.clinica.correo;

import pe.edu.utp.clinica.auth.SesionesJdbc;
import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.SQLException;
import java.time.*;

public final class AvisosVencimientoJdbc {
    private final SesionesJdbc.Conexion conexiones;

    public AvisosVencimientoJdbc() {
        this(DatabaseConfig::getConnection);
    }

    public AvisosVencimientoJdbc(SesionesJdbc.Conexion conexiones) {
        this.conexiones = conexiones;
    }

    public int programar(Instant ahora) throws SQLException {
        try (var c = conexiones.abrir()) {
            c.setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED);
            return pe.edu.utp.clinica.persistence.PersistenciaJpa.transaccion(
                    c,
                    () ->
                            new pe.edu.utp.clinica.repository.ProgramacionCorreosRepository(c)
                                    .programar(
                                            LocalDateTime.ofInstant(
                                                    ahora, ZoneId.of("America/Lima")),
                                            LocalDateTime.ofInstant(ahora, ZoneOffset.UTC),
                                            null));
        }
    }
}
