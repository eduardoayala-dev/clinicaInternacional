package pe.edu.utp.clinica.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
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
            // Mensajes de configuración no contienen valores de credenciales.
            throw new SQLException("Configuración de base de datos inválida: " + e.getMessage());
        }
        if (settings.isH2()) {
            LOGGER.warning("DB_MODE=h2: base temporal de demostración habilitada explícitamente.");
            inicializarH2Desarrollo();
            if (dataSource == null || dataSource.isClosed()) throw new SQLException("No se pudo inicializar H2 de desarrollo");
            return;
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
                // No registrar URLs o propiedades suministradas por el driver.
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
        // Un fallo de MySQL nunca se sustituye por otra base de datos.
        throw ultimoError;
    }

    private static void inicializarH2Desarrollo() {
        try {
            String h2Url = "jdbc:h2:mem:clinica_db;DB_CLOSE_DELAY=-1;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;NON_KEYWORDS=KEY,VALUE,USER";

            HikariConfig config = new HikariConfig();
            config.setDriverClassName("org.h2.Driver");
            config.setJdbcUrl(h2Url);
            config.setUsername("sa");
            config.setPassword("");
            config.setMaximumPoolSize(10);

            dataSource = new HikariDataSource(config);

            // Cargar y ejecutar schema.sql en H2
            try (Connection conn = dataSource.getConnection();
                    Statement stmt = conn.createStatement()) {

                InputStream is = null;
                java.io.File fileSchema = new java.io.File("sql/schema.sql");
                if (fileSchema.exists()) {
                    is = new java.io.FileInputStream(fileSchema);
                } else {
                    is = DatabaseConfig.class.getClassLoader().getResourceAsStream("schema.sql");
                }

                if (is != null) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.trim().startsWith("--") || line.trim().isEmpty())
                            continue;
                        sb.append(line).append("\n");
                        if (line.trim().endsWith(";")) {
                            String sql = sb.toString().trim();
                            if (!sql.toLowerCase().startsWith("create database")
                                    && !sql.toLowerCase().startsWith("use ")) {
                                try {
                                    stmt.execute(sql);
                                } catch (Exception ex) {
                                    // Ignorar warnings sintácticos menores de H2 mode
                                }
                            }
                            sb.setLength(0);
                        }
                    }
                    LOGGER.info("✅ Base de datos H2 in-memory inicializada y poblada con datos sintéticos completos.");
                } else {
                    ejecutarSeedH2Directo(stmt);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar H2 Fallback: " + e.getMessage(), e);
        }
    }

    private static void ejecutarSeedH2Directo(Statement stmt) {
        try {
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS usuario (id_usuario INT AUTO_INCREMENT PRIMARY KEY, nombres VARCHAR(100), apellidos VARCHAR(100), email VARCHAR(150), password_hash VARCHAR(255), rol VARCHAR(50), fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP, activo BOOLEAN DEFAULT TRUE)");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS paciente (id_paciente INT AUTO_INCREMENT PRIMARY KEY, id_usuario INT, dni VARCHAR(15), fecha_nacimiento DATE, telefono VARCHAR(20), direccion VARCHAR(255), canal_aviso VARCHAR(20) DEFAULT 'WhatsApp', sede_preferida VARCHAR(50) DEFAULT 'Sede San Borja', alergias VARCHAR(100) DEFAULT '', tipo_cobertura VARCHAR(50) DEFAULT 'EPS')");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS medico (id_medico INT AUTO_INCREMENT PRIMARY KEY, id_usuario INT, cmp VARCHAR(20), id_especialidad INT, id_sede INT, estado VARCHAR(20))");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS personal_administrativo (id_personal INT AUTO_INCREMENT PRIMARY KEY, id_usuario INT, id_sede INT, codigo_empleado VARCHAR(20), cargo VARCHAR(100))");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS sede (id_sede INT AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(20), nombre VARCHAR(100), direccion VARCHAR(255), telefono VARCHAR(20), estado VARCHAR(20))");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS especialidad (id_especialidad INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(100), descripcion TEXT, estado VARCHAR(20))");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS cita (id_cita INT AUTO_INCREMENT PRIMARY KEY, id_paciente INT, id_medico INT, id_disponibilidad INT, id_motivo INT, fecha_hora TIMESTAMP, estado VARCHAR(50), nivel_urgencia VARCHAR(20), fecha_recomendada_proxima DATE, riesgo_cancelacion VARCHAR(20), fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS disponibilidad (id_disponibilidad INT AUTO_INCREMENT PRIMARY KEY, id_medico INT, id_sede INT, fecha DATE, hora_inicio TIME, hora_fin TIME, duracion_minutos INT, estado VARCHAR(20))");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS lista_espera (id_lista_espera INT AUTO_INCREMENT PRIMARY KEY, id_paciente INT, id_especialidad INT, id_sede INT, id_motivo INT, nivel_urgencia VARCHAR(20), rango_horario VARCHAR(20), canal_notificacion VARCHAR(20), fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP, estado VARCHAR(20), posicion INT)");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS notificacion (id_notificacion INT AUTO_INCREMENT PRIMARY KEY, id_usuario INT, id_cita INT, tipo VARCHAR(50), canal VARCHAR(20) DEFAULT 'PLATAFORMA', mensaje TEXT, fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP, estado_envio VARCHAR(20) DEFAULT 'ENVIADA', leida INT DEFAULT 0)");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS auditoria_seguridad (id_auditoria INT AUTO_INCREMENT PRIMARY KEY, id_usuario INT, ip_origen VARCHAR(45), evento VARCHAR(100), descripcion TEXT, sospechoso BOOLEAN DEFAULT FALSE, fecha_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            try {
                stmt.execute("ALTER TABLE paciente ADD COLUMN canal_aviso VARCHAR(20) DEFAULT 'WhatsApp'");
            } catch (Exception ignored) {
            }
            try {
                stmt.execute("ALTER TABLE paciente ADD COLUMN sede_preferida VARCHAR(50) DEFAULT 'Sede San Borja'");
            } catch (Exception ignored) {
            }
            try {
                stmt.execute("ALTER TABLE paciente ADD COLUMN alergias VARCHAR(100) DEFAULT ''");
            } catch (Exception ignored) {
            }
            try {
                stmt.execute("ALTER TABLE paciente ADD COLUMN tipo_cobertura VARCHAR(50) DEFAULT 'EPS'");
            } catch (Exception ignored) {
            }
            try {
                stmt.execute(
                        "ALTER TABLE cita MODIFY COLUMN estado ENUM('SOLICITADA','CONFIRMADA','EN_SALA','ATENDIDA','NO_ASISTIO','REPROGRAMADA','CANCELADA') DEFAULT 'SOLICITADA'");
            } catch (Exception ignored) {
            }

            // Inserciones iniciales
            String defaultBcrypt = "$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa"; // Password123! /
                                                                                                   // admin123
            stmt.execute(
                    "INSERT INTO usuario (id_usuario, nombres, apellidos, email, password_hash, rol, activo) VALUES (1, 'Roberto', 'Vargas', 'admin@clinicainternacional.pe', '"
                            + defaultBcrypt + "', 'ADMINISTRADOR', TRUE)");
            stmt.execute(
                    "INSERT INTO usuario (id_usuario, nombres, apellidos, email, password_hash, rol, activo) VALUES (2, 'Marina', 'Salvatierra', 'personal@clinicainternacional.pe', '"
                            + defaultBcrypt + "', 'PERSONAL_ADMINISTRATIVO', TRUE)");
            stmt.execute(
                    "INSERT INTO usuario (id_usuario, nombres, apellidos, email, password_hash, rol, activo) VALUES (3, 'Carlos', 'Mendoza Valdivia', 'cmendoza@clinicainternacional.pe', '"
                            + defaultBcrypt + "', 'MEDICO', TRUE)");
            stmt.execute(
                    "INSERT INTO usuario (id_usuario, nombres, apellidos, email, password_hash, rol, activo) VALUES (4, 'Elena', 'Rostova K.', 'erostova@clinicainternacional.pe', '"
                            + defaultBcrypt + "', 'MEDICO', TRUE)");
            stmt.execute(
                    "INSERT INTO usuario (id_usuario, nombres, apellidos, email, password_hash, rol, activo) VALUES (5, 'Jorge', 'Valdivia Núñez', 'jorge.valdivia@correo.com', '"
                            + defaultBcrypt + "', 'PACIENTE', TRUE)");
            stmt.execute(
                    "INSERT INTO usuario (id_usuario, nombres, apellidos, email, password_hash, rol, activo) VALUES (8, 'Jorge', 'Valdivia', 'paciente@clinica.pe', '"
                            + defaultBcrypt + "', 'PACIENTE', TRUE)");

            stmt.execute(
                    "INSERT INTO paciente (id_paciente, id_usuario, dni, fecha_nacimiento, telefono, direccion, canal_aviso, sede_preferida, alergias, tipo_cobertura) VALUES (1, 5, '10492837', '1976-05-14', '+51 987 654 321', 'Av. Las Palmeras 450', 'WhatsApp', 'Sede San Borja', '', 'EPS')");
            stmt.execute(
                    "INSERT INTO medico (id_medico, id_usuario, cmp, id_especialidad, id_sede, estado) VALUES (1, 3, 'CMP 45211', 1, 1, 'ACTIVO')");
            stmt.execute(
                    "INSERT INTO medico (id_medico, id_usuario, cmp, id_especialidad, id_sede, estado) VALUES (2, 4, 'CMP 45892', 2, 2, 'ACTIVO')");

            LOGGER.info("✅ H2 fallback básico inicializado con éxito.");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error al ejecutar seed H2 directo: " + ex.getMessage(), ex);
        }
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
