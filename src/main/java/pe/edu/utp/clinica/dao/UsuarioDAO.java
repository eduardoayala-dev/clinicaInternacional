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

public class UsuarioDAO {

    private static final Logger LOGGER = Logger.getLogger(UsuarioDAO.class.getName());

    public Usuario buscarPorEmailODni(String identificador) {
        String sql = "SELECT u.id_usuario, u.nombres, u.apellidos, u.email, u.password_hash, u.rol, u.fecha_registro, u.activo " +
                     "FROM usuario u " +
                     "LEFT JOIN paciente p ON u.id_usuario = p.id_usuario " +
                     "WHERE (u.email = ? OR p.dni = ?) AND u.activo = 1";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, identificador);
            stmt.setString(2, identificador);

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
            LOGGER.log(Level.SEVERE, "Error al buscar usuario: " + e.getMessage(), e);
        }
        return null;
    }

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
            LOGGER.log(Level.SEVERE, "Error al crear usuario: " + e.getMessage(), e);
        }
        return -1;
    }

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
            LOGGER.log(Level.SEVERE, "Error al registrar auditoría: " + e.getMessage(), e);
        }
    }
    public boolean existeDocumentoOCorreo(String documento, String email) {
        String sql = "SELECT 1 FROM usuario u LEFT JOIN paciente p ON u.id_usuario = p.id_usuario WHERE u.email = ? OR p.dni = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, documento);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al verificar existencia de documento o correo: " + e.getMessage(), e);
        }
        return false;
    }

    public boolean crearCuentaPaciente(Usuario u, pe.edu.utp.clinica.model.Paciente p) {
        String sqlUsuario = "INSERT INTO usuario (nombres, apellidos, email, password_hash, rol, activo) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlPaciente = "INSERT INTO paciente (id_usuario, dni, fecha_nacimiento, telefono, direccion) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmtUsuario = conn.prepareStatement(sqlUsuario, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmtUsuario.setString(1, u.getNombres());
                stmtUsuario.setString(2, u.getApellidos());
                stmtUsuario.setString(3, u.getEmail());
                stmtUsuario.setString(4, u.getPasswordHash());
                stmtUsuario.setString(5, u.getRol());
                stmtUsuario.setBoolean(6, u.isActivo());
                stmtUsuario.executeUpdate();

                try (ResultSet rs = stmtUsuario.getGeneratedKeys()) {
                    if (rs.next()) {
                        int userId = rs.getInt(1);
                        p.setUsuarioId(userId);
                    } else {
                        throw new SQLException("No se pudo obtener el ID del usuario insertado.");
                    }
                }
            }

            try (PreparedStatement stmtPaciente = conn.prepareStatement(sqlPaciente)) {
                stmtPaciente.setInt(1, p.getUsuarioId());
                stmtPaciente.setString(2, p.getDni());
                stmtPaciente.setDate(3, java.sql.Date.valueOf(p.getFechaNacimiento()));
                stmtPaciente.setString(4, p.getTelefono());
                stmtPaciente.setString(5, p.getDireccion());
                stmtPaciente.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error al hacer rollback: " + ex.getMessage(), ex);
                }
            }
            LOGGER.log(Level.SEVERE, "Error al crear cuenta de paciente: " + e.getMessage(), e);
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error al restaurar autoCommit/cerrar: " + e.getMessage(), e);
                }
            }
        }
    }
}
