package pe.edu.utp.clinica.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;

public final class MySqlPruebas {
    private static HikariDataSource pool;
    private MySqlPruebas() { }

    public static synchronized Connection abrir() throws SQLException {
        String url = System.getenv("AGENDA_MYSQL_URL");
        validarUrl(url);
        if (pool == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(System.getenv("AGENDA_MYSQL_USER"));
            config.setPassword(System.getenv("AGENDA_MYSQL_PASSWORD"));
            config.setMaximumPoolSize(4);
            config.setMinimumIdle(1);
            config.setConnectionTimeout(10000);
            config.setValidationTimeout(3000);
            config.setInitializationFailTimeout(60000);
            pool = new HikariDataSource(config);
            Runtime.getRuntime().addShutdownHook(new Thread(pool::close));
        }
        return pool.getConnection();
    }

    static void validarUrl(String url) throws SQLException {
        try {
            if (url == null || !url.startsWith("jdbc:mysql:")) throw new IllegalArgumentException();
            URI uri = URI.create(url.substring(5));
            if (uri.getHost() == null || !uri.getPath().matches("/clinica_agendas_test_[A-Za-z0-9_]+")) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new SQLException("Las pruebas requieren una base temporal aislada.", e);
        }
    }
}
