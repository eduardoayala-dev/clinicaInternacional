package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.model.Paciente;
import pe.edu.utp.clinica.model.PerfilPaciente;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.repository.UsuarioRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UsuarioDAO {
    private static final Logger LOGGER = Logger.getLogger(UsuarioDAO.class.getName());

    protected Connection abrirConexion() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    public Usuario buscarPorEmailODni(String identificador) {
        try (var c = abrirConexion()) {
            return new UsuarioRepository(c).buscar(identificador);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar usuario.", e);
            return null;
        }
    }

    public int crearUsuario(Usuario usuario) {
        try (var c = abrirConexion()) {
            return PersistenciaJpa.transaccion(c, () -> new UsuarioRepository(c).crear(usuario));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al crear usuario.", e);
            return -1;
        }
    }

    public void registrarAuditoria(
            Integer usuario, String ip, String evento, String descripcion, boolean sospechoso) {
        try (var c = abrirConexion()) {
            PersistenciaJpa.transaccion(
                    c,
                    () -> {
                        new UsuarioRepository(c)
                                .auditar(usuario, ip, evento, descripcion, sospechoso);
                        return null;
                    });
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al registrar auditoría.", e);
        }
    }

    public boolean existeDocumentoOCorreo(String documento, String email) {
        try (var c = abrirConexion()) {
            return new UsuarioRepository(c).existe(documento, email, false);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al comprobar documento o correo.", e);
            return false;
        }
    }

    public boolean existeEmail(String email) {
        try (var c = abrirConexion()) {
            return new UsuarioRepository(c).existe(null, email != null ? email.trim() : "", true);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al comprobar correo.", e);
            return false;
        }
    }

    public boolean crearCuentaPaciente(Usuario usuario, Paciente paciente) {
        try (var c = abrirConexion()) {
            int[] ids =
                    PersistenciaJpa.transaccion(
                            c,
                            () -> {
                                var repo = new UsuarioRepository(c);
                                int id = repo.crear(usuario);
                                return new int[] {id, repo.crearPaciente(id, paciente)};
                            });
            usuario.setIdUsuario(ids[0]);
            paciente.setUsuarioId(ids[0]);
            paciente.setIdPaciente(ids[1]);
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al crear cuenta de paciente.", e);
            return false;
        }
    }

    public List<Usuario> obtenerTodos() {
        try (var c = abrirConexion()) {
            return new UsuarioRepository(c).personal();
        } catch (SQLException e) {
            throw new AgendaException(
                    503,
                    "PERSONAL_NO_DISPONIBLE",
                    "No se pudo consultar el personal. Inténtalo nuevamente.");
        }
    }

    public Map<String, Object> obtenerMetricasPersonal() {
        try (var c = abrirConexion()) {
            return new UsuarioRepository(c).metricas();
        } catch (SQLException e) {
            throw new AgendaException(
                    503,
                    "PERSONAL_NO_DISPONIBLE",
                    "No se pudieron consultar los indicadores de personal.");
        }
    }

    public Paciente obtenerPacientePorUsuarioId(int usuario) {
        try (var c = abrirConexion()) {
            return new UsuarioRepository(c).paciente(usuario);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener paciente.", e);
            return null;
        }
    }

    public boolean actualizarPerfilCompletoPaciente(PerfilPaciente perfil) {
        try (var c = abrirConexion()) {
            return PersistenciaJpa.transaccion(
                    c,
                    () -> {
                        new UsuarioRepository(c).guardarPerfil(perfil);
                        return true;
                    });
        } catch (SQLException | RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar el perfil del paciente.", e);
            return false;
        }
    }
}
