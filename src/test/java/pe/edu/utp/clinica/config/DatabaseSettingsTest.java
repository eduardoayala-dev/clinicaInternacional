package pe.edu.utp.clinica.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseSettingsTest {
    @TempDir Path temporary;

    private Map<String, String> valid() {
        return new HashMap<>(Map.of("APP_ENV", "production", "DB_HOST", "db.example.test",
                "DB_NAME", "clinica_db", "DB_USER", "clinica_app", "DB_PASSWORD", "valor-de-prueba"));
    }

    @Test void productionVerifiesServerIdentityAndNeverEnablesPublicKeyRetrieval() {
        var config = new DatabaseSettings(valid()).mysqlConfig();
        assertEquals("VERIFY_IDENTITY", config.getDataSourceProperties().getProperty("sslMode"));
        assertEquals("false", config.getDataSourceProperties().getProperty("allowPublicKeyRetrieval"));
        assertEquals("jdbc:mysql://db.example.test:3306/clinica_db", config.getJdbcUrl());
        assertFalse(config.getJdbcUrl().contains("valor-de-prueba"));
    }

    @Test void productionRejectsAllTlsDowngrades() {
        for (String mode : new String[]{"DISABLED", "PREFERRED", "REQUIRED", "VERIFY_CA", "", "desconocido"}) {
            var env = valid(); env.put("DB_SSL_MODE", mode);
            assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings(env), mode);
        }
    }

    @Test void productionCannotUseH2() {
        var env = valid(); env.put("DB_MODE", "h2");
        assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings(env));
    }

    @Test void railwayCannotBypassProductionPolicyByClaimingDevelopment() {
        for (String marker : new String[]{"RAILWAY_ENVIRONMENT", "RAILWAY_ENVIRONMENT_NAME", "RAILWAY_PROJECT_ID"}) {
            var env = valid(); env.put("APP_ENV", "development"); env.put(marker, "test"); env.put("DB_SSL_MODE", "DISABLED");
            assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings(env), marker);
        }
    }

    @Test void developmentRequiresMysqlAndRejectsTemporaryModes() {
        for (String mode : new String[]{"h2", "sqlite", "desconocido"}) {
            var local = valid(); local.put("APP_ENV", "development"); local.put("DB_MODE", mode);
            assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings(local));
        }
        assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings(Map.of()));
        var env = valid(); env.put("APP_ENV", "development");
        assertEquals("REQUIRED", new DatabaseSettings(env).mysqlConfig().getDataSourceProperties().getProperty("sslMode"));
    }

    @Test void rejectsMissingConnectionVariables() {
        for (String key : new String[]{"DB_HOST", "DB_NAME", "DB_USER", "DB_PASSWORD"}) {
            var env = valid(); env.remove(key);
            assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings(env), key);
        }
    }

    @Test void rejectsRootAndBlankPasswordsWithoutExposingCredentials() {
        var env = valid(); env.put("DB_USER", " ROOT ");
        assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings(env));
        env.put("DB_USER", "clinica_app"); env.put("DB_PASSWORD", " ");
        assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings(env));
        assertFalse(new DatabaseSettings(valid()).toString().contains("valor-de-prueba"));
    }

    @Test void preservesPasswordWhitespaceInsteadOfChangingCredentials() {
        var env = valid(); env.put("DB_PASSWORD", " clave con espacios ");
        assertEquals(" clave con espacios ", new DatabaseSettings(env).mysqlConfig().getPassword());
    }

    @Test void rejectsJdbcParameterInjectionThroughHostOrDatabase() {
        for (String key : new String[]{"DB_HOST", "DB_NAME"}) {
            var env = valid(); env.put(key, "localhost/db?sslMode=DISABLED");
            var error = assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings(env));
            assertFalse(error.getMessage().contains("sslMode=DISABLED"));
        }
    }

    @Test void rejectsInvalidPortAndEnvironmentTypo() {
        for (String port : new String[]{"0", "65536", "3306?sslMode=DISABLED", "abc"}) {
            var env = valid(); env.put("DB_PORT", port);
            assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings(env));
        }
        var env = valid(); env.put("APP_ENV", "prodution");
        assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings(env));
    }

    @Test void customTrustStoreIsPassedSeparatelyFromJdbcUrl() throws Exception {
        Path store = Files.writeString(temporary.resolve("ca.p12"), "test-fixture");
        var env = valid(); env.put("DB_TRUSTSTORE_PATH", store.toString()); env.put("DB_TRUSTSTORE_PASSWORD", "store-test");
        var config = new DatabaseSettings(env).mysqlConfig();
        assertEquals(store.toUri().toString(), config.getDataSourceProperties().getProperty("trustCertificateKeyStoreUrl"));
        assertEquals("PKCS12", config.getDataSourceProperties().getProperty("trustCertificateKeyStoreType"));
        assertEquals("false", config.getDataSourceProperties().getProperty("fallbackToSystemTrustStore"));
        assertFalse(config.getJdbcUrl().contains("store-test"));
    }

    @Test void rejectsMissingRelativeOrUnsupportedTrustStore() throws Exception {
        for (String path : new String[]{"relative.p12", temporary.resolve("missing.p12").toString()}) {
            var env = valid(); env.put("DB_TRUSTSTORE_PATH", path);
            assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings(env));
        }
        Path store = Files.writeString(temporary.resolve("ca.p12"), "fixture");
        var env = valid(); env.put("DB_TRUSTSTORE_PATH", store.toString()); env.put("DB_TRUSTSTORE_TYPE", "PEM");
        assertThrows(IllegalArgumentException.class, () -> new DatabaseSettings(env));
    }
}
