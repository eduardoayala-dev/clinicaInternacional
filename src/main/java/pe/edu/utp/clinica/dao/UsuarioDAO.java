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

    public boolean registrarPersonal(Usuario u, String cmp, Integer idEspecialidad, Integer idSede, String cargo) {
        String sqlUsuario = "INSERT INTO usuario (nombres, apellidos, email, password_hash, rol, activo) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlMedico = "INSERT INTO medico (id_usuario, cmp, id_especialidad, id_sede, estado) VALUES (?, ?, ?, ?, 'ACTIVO')";
        String sqlPersonal = "INSERT INTO personal_administrativo (id_usuario, id_sede, codigo_empleado, cargo) VALUES (?, ?, ?, ?)";
        String sqlAdmin = "INSERT INTO administrador (id_usuario, estado) VALUES (?, 'ACTIVO')";

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
                        
                        if ("MEDICO".equals(u.getRol())) {
                            try (PreparedStatement stmtM = conn.prepareStatement(sqlMedico)) {
                                stmtM.setInt(1, userId);
                                stmtM.setString(2, cmp);
                                stmtM.setInt(3, idEspecialidad);
                                stmtM.setInt(4, idSede);
                                stmtM.executeUpdate();
                            }
                        } else if ("PERSONAL_ADMINISTRATIVO".equals(u.getRol())) {
                            String codGenerado = "ADM-" + String.format("%03d", userId);
                            try (PreparedStatement stmtP = conn.prepareStatement(sqlPersonal)) {
                                stmtP.setInt(1, userId);
                                stmtP.setInt(2, idSede);
                                stmtP.setString(3, codGenerado);
                                stmtP.setString(4, cargo);
                                stmtP.executeUpdate();
                            }
                        } else if ("ADMINISTRADOR".equals(u.getRol())) {
                            try (PreparedStatement stmtA = conn.prepareStatement(sqlAdmin)) {
                                stmtA.setInt(1, userId);
                                stmtA.executeUpdate();
                            }
                        }
                    } else {
                        throw new SQLException("No se pudo obtener el ID del usuario insertado.");
                    }
                }
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {}
            }
            LOGGER.log(Level.SEVERE, "Error al registrar personal: " + e.getMessage(), e);
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
            }
        }
    }

    public boolean actualizarPersonal(Usuario u, String cmp, Integer idEspecialidad, Integer idSede, String cargo) {
        String sqlUsuario = "UPDATE usuario SET nombres = ?, apellidos = ?, email = ? WHERE id_usuario = ?";
        String sqlMedico = "UPDATE medico SET cmp = ?, id_especialidad = ?, id_sede = ? WHERE id_usuario = ?";
        String sqlPersonal = "UPDATE personal_administrativo SET id_sede = ?, cargo = ? WHERE id_usuario = ?";

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmtU = conn.prepareStatement(sqlUsuario)) {
                stmtU.setString(1, u.getNombres());
                stmtU.setString(2, u.getApellidos());
                stmtU.setString(3, u.getEmail());
                stmtU.setInt(4, u.getIdUsuario());
                stmtU.executeUpdate();
            }

            if ("MEDICO".equals(u.getRol())) {
                try (PreparedStatement stmtM = conn.prepareStatement(sqlMedico)) {
                    stmtM.setString(1, cmp);
                    stmtM.setInt(2, idEspecialidad);
                    stmtM.setInt(3, idSede);
                    stmtM.setInt(4, u.getIdUsuario());
                    stmtM.executeUpdate();
                }
            } else if ("PERSONAL_ADMINISTRATIVO".equals(u.getRol())) {
                try (PreparedStatement stmtP = conn.prepareStatement(sqlPersonal)) {
                    stmtP.setInt(1, idSede);
                    stmtP.setString(2, cargo);
                    stmtP.setInt(3, u.getIdUsuario());
                    stmtP.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {}
            }
            LOGGER.log(Level.SEVERE, "Error al actualizar personal: " + e.getMessage(), e);
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
            }
        }
    }

    public boolean cambiarEstadoUsuario(int idUsuario, boolean activo) {
        String sql = "UPDATE usuario SET activo = ? WHERE id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setBoolean(1, activo);
            stmt.setInt(2, idUsuario);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cambiar estado de usuario: " + e.getMessage(), e);
            return false;
        }
    }

    public java.util.List<Usuario> obtenerTodos() {
        java.util.List<Usuario> lista = new java.util.ArrayList<>();
        String sql = "SELECT u.id_usuario, u.nombres, u.apellidos, u.email, u.rol, u.fecha_registro, u.activo, " +
                     "m.cmp, m.id_especialidad, m.id_sede AS id_sede_medico, e.nombre AS especialidad, " +
                     "pa.codigo_empleado, pa.cargo, pa.id_sede AS id_sede_personal " +
                     "FROM usuario u " +
                     "LEFT JOIN medico m ON u.id_usuario = m.id_usuario " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN personal_administrativo pa ON u.id_usuario = pa.id_usuario " +
                     "ORDER BY u.id_usuario DESC";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("fecha_registro");
                Usuario u = new Usuario(
                        rs.getInt("id_usuario"),
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("email"),
                        "", // No mandar el hash a la vista por seguridad
                        rs.getString("rol"),
                        ts != null ? ts.toLocalDateTime() : null,
                        rs.getBoolean("activo")
                );
                u.setCmp(rs.getString("cmp"));
                u.setEspecialidad(rs.getString("especialidad"));
                u.setCodigoEmpleado(rs.getString("codigo_empleado"));
                u.setCargo(rs.getString("cargo"));
                
                if ("MEDICO".equals(u.getRol())) {
                    u.setIdEspecialidad(rs.getInt("id_especialidad"));
                    u.setIdSede(rs.getInt("id_sede_medico"));
                } else if ("PERSONAL_ADMINISTRATIVO".equals(u.getRol())) {
                    u.setIdSede(rs.getInt("id_sede_personal"));
                }
                
                lista.add(u);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al listar usuarios: " + e.getMessage(), e);
        }
        return lista;
    }

    public pe.edu.utp.clinica.model.Paciente obtenerPacientePorUsuarioId(int usuarioId) {
        String sql = "SELECT * FROM paciente WHERE id_usuario = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    java.sql.Date fechaNac = rs.getDate("fecha_nacimiento");
                    java.time.LocalDate fn = fechaNac != null ? fechaNac.toLocalDate() : null;
                    String canalAviso = getSafeColumn(rs, "canal_aviso", "WhatsApp");
                    String sedePref = getSafeColumn(rs, "sede_preferida", "Sede San Borja");
                    String alergias = getSafeColumn(rs, "alergias", "");
                    String tipoCob = getSafeColumn(rs, "tipo_cobertura", "EPS");

                    return new pe.edu.utp.clinica.model.Paciente(
                        rs.getInt("id_paciente"),
                        rs.getInt("id_usuario"),
                        rs.getString("dni"),
                        fn,
                        rs.getString("telefono"),
                        rs.getString("direccion"),
                        canalAviso,
                        sedePref,
                        alergias,
                        tipoCob
                    );
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener paciente por usuarioId: " + e.getMessage(), e);
        }
        return null;
    }

    private String getSafeColumn(ResultSet rs, String colName, String defaultVal) {
        try {
            String val = rs.getString(colName);
            return (val != null && !val.trim().isEmpty()) ? val : defaultVal;
        } catch (SQLException e) {
            return defaultVal;
        }
    }

    public boolean actualizarContactoPaciente(int idUsuario, String email, String telefono, String direccion) {
        String sqlUsuario = "UPDATE usuario SET email = ? WHERE id_usuario = ?";
        String sqlPaciente = "UPDATE paciente SET telefono = ?, direccion = ? WHERE id_usuario = ?";
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement stmtU = conn.prepareStatement(sqlUsuario)) {
                stmtU.setString(1, email);
                stmtU.setInt(2, idUsuario);
                stmtU.executeUpdate();
            }
            try (PreparedStatement stmtP = conn.prepareStatement(sqlPaciente)) {
                stmtP.setString(1, telefono);
                stmtP.setString(2, direccion);
                stmtP.setInt(3, idUsuario);
                stmtP.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {}
            }
            LOGGER.log(Level.SEVERE, "Error al actualizar contacto del paciente: " + e.getMessage(), e);
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
            }
        }
    }

    public boolean actualizarPerfilCompletoPaciente(int idUsuario, String nombres, String apellidos, String email,
                                                    String fechaNacimiento, String telefono, String direccion,
                                                    String canalAviso, String sedePreferida, String alergias, String tipoCobertura) {
        String sqlUsuario = "UPDATE usuario SET nombres = ?, apellidos = ?, email = ? WHERE id_usuario = ?";
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement stmtU = conn.prepareStatement(sqlUsuario)) {
                stmtU.setString(1, nombres);
                stmtU.setString(2, apellidos);
                stmtU.setString(3, email);
                stmtU.setInt(4, idUsuario);
                stmtU.executeUpdate();
            }

            // Actualizar paciente basico
            boolean tieneFecha = (fechaNacimiento != null && !fechaNacimiento.trim().isEmpty());
            String sqlPaciente = tieneFecha ?
                "UPDATE paciente SET telefono = ?, direccion = ?, fecha_nacimiento = ? WHERE id_usuario = ?" :
                "UPDATE paciente SET telefono = ?, direccion = ? WHERE id_usuario = ?";

            try (PreparedStatement stmtP = conn.prepareStatement(sqlPaciente)) {
                stmtP.setString(1, telefono);
                stmtP.setString(2, direccion);
                if (tieneFecha) {
                    java.time.LocalDate ld;
                    String f = fechaNacimiento.trim();
                    if (f.contains("/")) {
                        String[] partes = f.split("/");
                        ld = java.time.LocalDate.of(Integer.parseInt(partes[2]), Integer.parseInt(partes[1]), Integer.parseInt(partes[0]));
                    } else {
                        ld = java.time.LocalDate.parse(f);
                    }
                    stmtP.setDate(3, java.sql.Date.valueOf(ld));
                    stmtP.setInt(4, idUsuario);
                } else {
                    stmtP.setInt(3, idUsuario);
                }
                stmtP.executeUpdate();
            }

            // Opcional: Actualizar campos extendidos si las columnas existen en la base de datos
            try {
                String sqlExt = "UPDATE paciente SET canal_aviso = ?, sede_preferida = ?, alergias = ?, tipo_cobertura = ? WHERE id_usuario = ?";
                try (PreparedStatement stmtExt = conn.prepareStatement(sqlExt)) {
                    stmtExt.setString(1, canalAviso != null ? canalAviso : "WhatsApp");
                    stmtExt.setString(2, sedePreferida != null ? sedePreferida : "Sede San Borja");
                    stmtExt.setString(3, alergias != null ? alergias : "");
                    stmtExt.setString(4, tipoCobertura != null ? tipoCobertura : "EPS");
                    stmtExt.setInt(5, idUsuario);
                    stmtExt.executeUpdate();
                }
            } catch (SQLException ignore) {
                // Si la tabla no tiene las columnas adicionales en MySQL/H2 antiguo, no interrumpe el guardado principal
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {}
            }
            LOGGER.log(Level.SEVERE, "Error al actualizar perfil completo del paciente: " + e.getMessage(), e);
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
            }
        }
    }
}

