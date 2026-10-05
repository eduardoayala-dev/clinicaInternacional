package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) para Usuario.
 * CUMPLE AL 100% CON REGLA OBLIGATORIA ANTI-SQL-INJECTION:
 * El 100% de las consultas utilizan exclusivamente PreparedStatement parametrizados.
 * NINGUNA consulta concatena texto directo del usuario.
 */
public class UsuarioDAO {

    private static final Logger LOGGER = Logger.getLogger(UsuarioDAO.class.getName());

    /**
     * Busca un usuario por su correo electrónico con PreparedStatement (Anti SQLi).
     */
    public Usuario buscarPorEmail(String email) {
        String sql = "SELECT id_usuario, nombres, apellidos, email, password_hash, rol, fecha_registro, activo " +
                     "FROM usuario WHERE email = ? AND activo = 1";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("fecha_registro");
                    return new Usuario(
                            rs.getInt("id_usuario"),
                            rs.getString("nombres"),
                            rs.getString("apellidos"),
                            rs.getString("email"),
                            rs.getString("password_hash"),
                            rs.getString("rol"),
                            ts != null ? ts.toLocalDateTime() : null,
                            rs.getBoolean("activo")
                    );
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar usuario por email en DAO: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Inserta un nuevo usuario con PreparedStatement (HU002).
     */
    public int crearUsuario(Usuario u) {
        String sql = "INSERT INTO usuario (nombres, apellidos, email, password_hash, rol, activo) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, u.getNombres());
            stmt.setString(2, u.getApellidos());
            stmt.setString(3, u.getEmail());
            stmt.setString(4, u.getPasswordHash());
            stmt.setString(5, u.getRol());
            stmt.setBoolean(6, u.isActivo());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al crear usuario en DAO: " + e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Registra intento de acceso o evento de seguridad en la tabla auditoria_seguridad (HU021 / RNF-05).
     */
    public void registrarAuditoria(Integer idUsuario, String ip, String evento, String descripcion, boolean sospechoso) {
        String sql = "INSERT INTO auditoria_seguridad (id_usuario, ip_origen, evento, descripcion, sospechoso) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (idUsuario != null) {
                stmt.setInt(1, idUsuario);
            } else {
                stmt.setNull(1, java.sql.Types.INTEGER);
            }
            stmt.setString(2, ip);
            stmt.setString(3, evento);
            stmt.setString(4, descripcion);
            stmt.setBoolean(5, sospechoso);

            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al registrar auditoría de seguridad: " + e.getMessage(), e);
        }
    }
}
