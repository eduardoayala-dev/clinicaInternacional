package pe.edu.utp.clinica.config;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseConfig {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());
    private static volatile HikariDataSource dataSource;

    private static synchronized void inicializarBaseDatos() throws SQLException {
        if (dataSource != null && !dataSource.isClosed()) return;
        DatabaseSettings settings;
        try {
            settings = new DatabaseSettings(System.getenv());
        } catch (IllegalArgumentException e) {
            throw new SQLException("Configuración de base de datos inválida: " + e.getMessage());
        }
        SQLException ultimoError = null;
        int intentos = settings.isProduction() ? 5 : 1;
        for (int intento = 1; intento <= intentos; intento++) {
            HikariDataSource candidato = null;
            try {
                candidato = new HikariDataSource(settings.mysqlConfig());
                try (Connection conexion = candidato.getConnection()) {
                    if (!conexion.isValid(5)) throw new SQLException("Conexión MySQL no válida");
                }
                dataSource = candidato;
                LOGGER.info("Conexión MySQL establecida con la política TLS configurada.");
                return;
            } catch (Exception e) {
                ultimoError = new SQLException("No se pudo conectar con MySQL (intento "
                        + intento + "/" + intentos + ")", "08001");
                LOGGER.warning(ultimoError.getMessage());
                if (candidato != null) candidato.close();
                if (intento < intentos) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Interrumpido mientras se esperaba a MySQL", ex);
                    }
                }
            }
        }
        throw ultimoError;
    }

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            inicializarBaseDatos();
        }
        return dataSource.getConnection();
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("Pool de conexiones cerrado limpiamente.");
        }
    }
}
