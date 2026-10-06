package pe.edu.utp.clinica.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.UsuarioDAO;
import pe.edu.utp.clinica.model.Paciente;
import pe.edu.utp.clinica.model.Usuario;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class RegistroPacienteTest {

    private AuthService authService;
    private UsuarioDAO mockUsuarioDAO;

    @BeforeEach
    public void setUp() {
        mockUsuarioDAO = new UsuarioDAO() {
            @Override
            public boolean existeDocumentoOCorreo(String documento, String email) {
                // Simular que el correo o DNI ya existen
                return "11111111".equals(documento) || "yaexiste@correo.com".equals(email);
            }

            @Override
            public boolean crearCuentaPaciente(Usuario u, Paciente p) {
                return true; // Simular éxito en base de datos
            }
        };

        authService = new AuthService(mockUsuarioDAO);
    }

    @Test
    public void testRegistroExitoso() {
        Usuario u = new Usuario();
        u.setNombres("Maria");
        u.setApellidos("Gomez");
        u.setEmail("maria@correo.com");
        u.setPasswordHash("Password123!");

        Paciente p = new Paciente();
        p.setDni("88888888");
        p.setFechaNacimiento(LocalDate.of(1990, 5, 20));
        p.setTelefono("999999999");
        p.setDireccion("Lima");

        boolean resultado = authService.registrarPaciente(u, p);
        
        assertTrue(resultado, "El registro debería ser exitoso con datos nuevos.");
        assertEquals("PACIENTE", u.getRol(), "El rol asignado debe ser PACIENTE.");
        assertTrue(u.isActivo(), "El usuario debe estar activo.");
        assertNotEquals("Password123!", u.getPasswordHash(), "La contraseña debe haber sido hasheada.");
    }

    @Test
    public void testRegistroDuplicadoPorDNI() {
        Usuario u = new Usuario();
        u.setEmail("nuevo@correo.com");
        u.setPasswordHash("Password123!");

        Paciente p = new Paciente();
        p.setDni("11111111"); // DNI que ya existe según el mock

        boolean resultado = authService.registrarPaciente(u, p);
        
        assertFalse(resultado, "El registro debe fallar si el DNI ya está registrado.");
    }

    @Test
    public void testRegistroDuplicadoPorCorreo() {
        Usuario u = new Usuario();
        u.setEmail("yaexiste@correo.com"); // Correo que ya existe según el mock
        u.setPasswordHash("Password123!");

        Paciente p = new Paciente();
        p.setDni("22222222");

        boolean resultado = authService.registrarPaciente(u, p);
        
        assertFalse(resultado, "El registro debe fallar si el correo ya está registrado.");
    }
}
