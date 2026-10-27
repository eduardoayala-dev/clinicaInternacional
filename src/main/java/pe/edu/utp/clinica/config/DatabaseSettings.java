package pe.edu.utp.clinica.config;

import com.zaxxer.hikari.HikariConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Configuración validada antes de abrir conexiones. No incluye secretos en toString. */
final class DatabaseSettings {
    private final Map<String, String> env;
    private final boolean production;
    private final boolean h2;
    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;
    private final String sslMode;

    DatabaseSettings(Map<String, String> environment) {
        env = Map.copyOf(environment);
        String appEnv = value("APP_ENV", "development").toLowerCase(Locale.ROOT);
        if (!Set.of("development", "test", "production").contains(appEnv)) {
            throw new IllegalArgumentException("APP_ENV debe ser development, test o production");
        }
        production = "production".equals(appEnv) || env.containsKey("RAILWAY_ENVIRONMENT")
                || env.containsKey("RAILWAY_ENVIRONMENT_NAME") || env.containsKey("RAILWAY_PROJECT_ID");
        String mode = value("DB_MODE", "mysql").toLowerCase(Locale.ROOT);
        if (!Set.of("mysql", "h2").contains(mode)) throw new IllegalArgumentException("DB_MODE debe ser mysql o h2");
        h2 = "h2".equals(mode);
        if (production && h2) throw new IllegalArgumentException("H2 no está permitido en producción");
        if (h2) {
            host = database = user = password = sslMode = "";
            port = 0;
            return;
        }

        host = required("DB_HOST");
        if (!host.matches("[A-Za-z0-9][A-Za-z0-9._-]*|\\[[0-9a-fA-F:]+\\]")) {
            throw new IllegalArgumentException("DB_HOST no es un nombre de servidor válido");
        }
        try {
            port = Integer.parseInt(value("DB_PORT", "3306"));
            if (port < 1 || port > 65535) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("DB_PORT debe estar entre 1 y 65535");
        }
        database = required("DB_NAME");
        if (!database.matches("[A-Za-z0-9_]+")) throw new IllegalArgumentException("DB_NAME no es válido");
        user = required("DB_USER");
        if ("root".equalsIgnoreCase(user)) throw new IllegalArgumentException("DB_USER debe ser una cuenta de aplicación, no root");
        password = env.get("DB_PASSWORD");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Falta DB_PASSWORD");
        sslMode = value("DB_SSL_MODE", production ? "VERIFY_IDENTITY" : "REQUIRED").toUpperCase(Locale.ROOT);
        if (!Set.of("DISABLED", "REQUIRED", "VERIFY_CA", "VERIFY_IDENTITY").contains(sslMode)) {
            throw new IllegalArgumentException("DB_SSL_MODE no es válido; no se permite degradación automática a conexión sin TLS");
        }
        if (production && !"VERIFY_IDENTITY".equals(sslMode)) {
            throw new IllegalArgumentException("Producción exige DB_SSL_MODE=VERIFY_IDENTITY");
        }
        String store = value("DB_TRUSTSTORE_PATH", "");
        if (!store.isEmpty()) {
            Path path = Path.of(store);
            if (!path.isAbsolute() || !Files.isRegularFile(path) || !Files.isReadable(path)) {
                throw new IllegalArgumentException("DB_TRUSTSTORE_PATH debe apuntar a un archivo absoluto legible");
            }
            if (!Set.of("JKS", "PKCS12").contains(value("DB_TRUSTSTORE_TYPE", "PKCS12").toUpperCase(Locale.ROOT))) {
                throw new IllegalArgumentException("DB_TRUSTSTORE_TYPE debe ser JKS o PKCS12");
            }
        }
    }

    boolean isProduction() { return production; }
    boolean isH2() { return h2; }

    HikariConfig mysqlConfig() {
        if (h2) throw new IllegalStateException("La configuración seleccionada es H2");
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.setUsername(user);
        config.setPassword(password);
        config.addDataSourceProperty("sslMode", sslMode);
        config.addDataSourceProperty("allowPublicKeyRetrieval", "false");
        config.addDataSourceProperty("serverTimezone", "America/Lima");
        config.addDataSourceProperty("characterEncoding", "UTF-8");
        config.addDataSourceProperty("connectTimeout", "10000");
        config.addDataSourceProperty("socketTimeout", "15000");
        String store = value("DB_TRUSTSTORE_PATH", "");
        if (!store.isEmpty()) {
            config.addDataSourceProperty("trustCertificateKeyStoreUrl", Path.of(store).toUri().toString());
            config.addDataSourceProperty("trustCertificateKeyStoreType", value("DB_TRUSTSTORE_TYPE", "PKCS12").toUpperCase(Locale.ROOT));
            config.addDataSourceProperty("trustCertificateKeyStorePassword", env.getOrDefault("DB_TRUSTSTORE_PASSWORD", ""));
            config.addDataSourceProperty("fallbackToSystemTrustStore", "false");
        }
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(10000);
        config.setValidationTimeout(5000);
        config.setInitializationFailTimeout(1000);
        return config;
    }

    private String required(String key) {
        String result = value(key, "");
        if (result.isEmpty()) throw new IllegalArgumentException("Falta " + key);
        return result;
    }

    private String value(String key, String fallback) {
        return env.getOrDefault(key, fallback).trim();
    }
}
