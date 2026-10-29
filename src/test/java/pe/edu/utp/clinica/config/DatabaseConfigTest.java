package pe.edu.utp.clinica.config;

import java.sql.SQLException;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {
    @Test void identifiesTlsFailureWithoutLoggingMessagesOrCredentials() {
        var tls = new SSLHandshakeException("password=secreto jdbc:mysql://servidor");
        var error = new SQLException("usuario sensible", "08001", 1045, tls);
        assertEquals("SQLException SQLState=08001 codigo=1045 -> SSLHandshakeException",
                DatabaseConfig.diagnosticoSeguro(error));
    }

    @Test void ignoresInvalidSqlStateAndStopsCyclicCauses() {
        var primero = new SQLException("secreto", "password=secreto");
        var segundo = new IllegalStateException("secreto", primero);
        primero.initCause(segundo);
        assertEquals("SQLException codigo=0 -> IllegalStateException",
                DatabaseConfig.diagnosticoSeguro(primero));
    }
}
