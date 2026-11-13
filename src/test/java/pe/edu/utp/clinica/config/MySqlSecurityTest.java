package pe.edu.utp.clinica.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import java.util.Locale;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Comprobación opcional de solo lectura contra un MySQL real configurado. */
class MySqlSecurityTest {
    private void assertTlsConnectionRejected(Map<String, String> env) {
        var config = new DatabaseSettings(env).mysqlConfig();
        Properties properties = new Properties();
        properties.putAll(config.getDataSourceProperties());
        properties.setProperty("user", config.getUsername());
        properties.setProperty("password", config.getPassword());
        SQLException error = assertThrows(SQLException.class, () -> {
            try (var ignored = DriverManager.getConnection(config.getJdbcUrl(), properties)) {
                fail("La conexión TLS inválida no debe aceptarse");
            }
        });
        boolean tlsFailure = false;
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof javax.net.ssl.SSLException
                    || cause instanceof java.security.cert.CertificateException
                    || cause instanceof java.security.cert.CertPathValidatorException) {
                tlsFailure = true;
            }
        }
        assertTrue(tlsFailure, "Debe fallar la validación TLS, no la red ni la autenticación");
    }

    @Test void rejectsCertificateForDifferentHostname() {
        assumeTrue("true".equalsIgnoreCase(System.getenv("MYSQL_TLS_NEGATIVE_TEST")),
                "Requiere un servidor aislado cuyo certificado incluya 127.0.0.1 y no localhost");
        var env = new HashMap<>(System.getenv());
        assertEquals("127.0.0.1", env.get("DB_HOST"));
        env.put("DB_HOST", "localhost");
        assertTlsConnectionRejected(env);
    }

    @Test void rejectsCertificateSignedByAnUntrustedCa() {
        assumeTrue("true".equalsIgnoreCase(System.getenv("MYSQL_TLS_NEGATIVE_TEST")),
                "Requiere un truststore de prueba con una CA ajena al servidor");
        var env = new HashMap<>(System.getenv());
        assertNotNull(env.get("MYSQL_TEST_UNTRUSTED_STORE"));
        env.put("DB_TRUSTSTORE_PATH", env.get("MYSQL_TEST_UNTRUSTED_STORE"));
        assertTlsConnectionRejected(env);
    }

    @Test void verifiesRealTlsAndDirectAccountPrivileges() throws Exception {
        assumeTrue("true".equalsIgnoreCase(System.getenv("MYSQL_SECURITY_TEST")),
                "Requiere MySQL real y MYSQL_SECURITY_TEST=true; no simula una conexión TLS");
        DatabaseSettings settings = new DatabaseSettings(System.getenv());
        try (HikariDataSource pool = new HikariDataSource(settings.mysqlConfig());
                var conn = pool.getConnection(); var statement = conn.createStatement()) {
            try (var result = statement.executeQuery("SHOW SESSION STATUS LIKE 'Ssl_cipher'")) {
                assertTrue(result.next());
                assertNotNull(result.getString(2));
                assertFalse(result.getString(2).isBlank(), "La sesión MySQL debe estar cifrada");
            }
            try (var result = statement.executeQuery("SELECT CURRENT_USER()")) {
                assertTrue(result.next());
                assertFalse(result.getString(1).toLowerCase(Locale.ROOT).startsWith("root@"));
            }
            Set<String> allowed = Set.of("USAGE", "SELECT", "INSERT", "UPDATE", "DELETE");
            Set<String> granted = new HashSet<>();
            String database = System.getenv("DB_NAME").trim().toUpperCase(Locale.ROOT);
            try (var result = statement.executeQuery("SHOW GRANTS FOR CURRENT_USER()")) {
                while (result.next()) {
                    String grant = result.getString(1).toUpperCase(Locale.ROOT);
                    assertTrue(grant.startsWith("GRANT ") && grant.contains(" ON "), "Revisar roles o privilegios no previstos");
                    assertFalse(grant.contains("WITH GRANT OPTION"));
                    String privileges = grant.substring(6, grant.indexOf(" ON "));
                    for (String privilege : privileges.split(",")) assertTrue(allowed.contains(privilege.trim()), "Permiso excesivo");
                    String target = grant.substring(grant.indexOf(" ON ") + 4, grant.indexOf(" TO "))
                            .replace("`", "").replace("\\_", "_");
                    if ("*.*".equals(target)) {
                        assertEquals("USAGE", privileges);
                    } else {
                        assertTrue(target.startsWith(database + "."), "Permisos sobre otra base de datos");
                        for (String privilege : privileges.split(",")) granted.add(privilege.trim());
                    }
                }
            }
            assertTrue(granted.containsAll(Set.of("SELECT", "INSERT", "UPDATE", "DELETE")), "Faltan permisos CRUD");
        }
    }
}
