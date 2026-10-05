package pe.edu.utp.clinica.service;

import pe.edu.utp.clinica.dao.UsuarioDAO;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias completas para AuthService y Escenarios de Seguridad / Fuerza Bruta (HU001 / RNF-04).
 */
public class AuthServiceTest {

    private AuthService authService;
    private UsuarioDAO usuarioDAOFicticio;

    @BeforeEach
    public void setUp() {
        usuarioDAOFicticio = new UsuarioDAO() {
            @Override
            public Usuario buscarPorEmail(String email) {
                if ("paciente@test.com".equals(email)) {
                    return new Usuario(
                            1,
                            "Juan",
                            "Perez",
                            "paciente@test.com",
                            SecurityUtil.hashPassword("Password123!"),
                            "PACIENTE",
                            null,
                            true
                    );
                }
                return null;
            }

            @Override
            public void registrarAuditoria(Integer idUsuario, String ip, String evento, String descripcion, boolean sospechoso) {
                // Mock de auditoria sin BD
            }
        };

        authService = new AuthService(usuarioDAOFicticio);
    }

    @Test
    public void testAutenticacionExitosa() {
        Usuario user = authService.autenticar("paciente@test.com", "Password123!", "127.0.0.1");
        assertNotNull(user, "El usuario debería autenticarse correctamente.");
        assertEquals("PACIENTE", user.getRol());
    }

    @Test
    public void testAutenticacionPasswordIncorrecta() {
        Usuario user = authService.autenticar("paciente@test.com", "WrongPassword!", "127.0.0.1");
        assertNull(user, "El inicio de sesión debe ser denegado para contraseñas incorrectas.");
    }

    @Test
    public void testBloqueoPorIntentosFallidosConsecutivos() {
        String email = "paciente@test.com";

        // Realizar 5 intentos fallidos
        for (int i = 0; i < 5; i++) {
            authService.autenticar(email, "ClaveErronea" + i, "127.0.0.1");
        }

        // El sexto intento con la contraseña CORRECTA debe ser bloqueado por seguridad
        Usuario userBloqueado = authService.autenticar(email, "Password123!", "127.0.0.1");
        assertNull(userBloqueado, "La cuenta debe ser bloqueada tras 5 intentos fallidos.");
    }
}
