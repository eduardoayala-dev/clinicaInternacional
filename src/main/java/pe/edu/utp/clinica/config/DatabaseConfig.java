package pe.edu.utp.clinica.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Conexión Segura a la Base de Datos MySQL utilizando Pool de Conexiones HikariCP.
 * Cumple con las exigencias de seguridad (PreparedStatement) y arquitectura limpia en 3 capas.
 */
public class DatabaseConfig {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());
    private static HikariDataSource dataSource;

    static {
        try {
            // Lectura de variables de entorno con fallback seguro para desarrollo local
            String host = System.getenv().getOrDefault("DB_HOST", "localhost");
            String port = System.getenv().getOrDefault("DB_PORT", "3306");
            String dbName = System.getenv().getOrDefault("DB_NAME", "clinica_db");
            String user = System.getenv().getOrDefault("DB_USER", "root");
            String password = System.getenv().getOrDefault("DB_PASSWORD", "root");

            String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8",
                    host, port, dbName);

            HikariConfig config = new HikariConfig();
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(user);
            config.setPassword(password);

            // Optimización y seguridad del Pool HikariCP
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(10000);
            config.setMaxLifetime(1800000);

            // Cache de PreparedStatements para prevenir SQLi y optimizar rendimiento
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");

            dataSource = new HikariDataSource(config);
            LOGGER.info("Pool de conexiones HikariCP inicializado correctamente.");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Base de datos local no conectada aún. La aplicación web iniciará en modo mock/demo: " + e.getMessage());
        }
    }

    private DatabaseConfig() {
        // Constructor privado para clase de utilidad
    }

    /**
     * Obtiene una conexión activa del pool.
     * @return Connection
     * @throws SQLException si ocurre un error de conexión
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("El DataSource HikariCP no se encuentra inicializado.");
        }
        return dataSource.getConnection();
    }

    /**
     * Cierra el pool de conexiones al apagar la aplicación.
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("Pool de conexiones HikariCP cerrado limpiamente.");
        }
    }
}
