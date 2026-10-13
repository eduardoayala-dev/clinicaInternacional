package pe.edu.utp.clinica.service;

import pe.edu.utp.clinica.dao.UsuarioDAO;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.util.SecurityUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());
    private static final int MAX_INTENTOS = 5;
    private static final long TIEMPO_BLOQUEO_MS = 15 * 60 * 1000;

    private static final Map<String, Integer> intentosFallidos = new ConcurrentHashMap<>();
    private static final Map<String, Long> bloqueosTemporales = new ConcurrentHashMap<>();

    /**
     * Modo demostración: habilita usuarios sintéticos y contraseñas de prueba.
     * Desactivado por defecto. Activar SOLO en desarrollo local con
     * la variable de entorno DEMO_MODE=true o la propiedad -Ddemo.mode=true.
     */
    private static final boolean DEMO_MODE = Boolean.parseBoolean(
            System.getenv().getOrDefault("DEMO_MODE", System.getProperty("demo.mode", "false")));

    static {
        if (DEMO_MODE) {
            LOGGER.warning("⚠️ DEMO_MODE activo: credenciales de prueba habilitadas. NO usar en producción.");
        }
    }

    private final UsuarioDAO usuarioDAO;

    public AuthService() {
        this.usuarioDAO = new UsuarioDAO();
    }

    public AuthService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    public Usuario autenticar(String identificador, String password, String ipOrigen) {
        String idLimpio = SecurityUtil.sanitizeInput(identificador).toLowerCase();

        if (estaBloqueado(idLimpio)) {
            LOGGER.warning("Intento de acceso rechazado: Usuario o IP bloqueada temporalmente: " + idLimpio);
            usuarioDAO.registrarAuditoria(null, ipOrigen, "LOGIN_BLOQUEADO", "Intento de acceso en cuenta bloqueada: " + idLimpio, true);
            return null;
        }

        Usuario usuario = null;
        try {
            usuario = usuarioDAO.buscarPorEmailODni(idLimpio);
        } catch (Exception e) {
            LOGGER.warning("Autenticando usuario sintético de demostración local");
        }

        if (usuario == null && DEMO_MODE) {
            boolean passValida = esPasswordDemo(password);
            if (passValida) {
                if ("eduardo@paciente.com".equals(idLimpio) || "paciente@clinica.pe".equals(idLimpio) || "paciente".equals(idLimpio) || "juan.perez@correo.com".equals(idLimpio) || "72345678".equals(idLimpio) || "73910284".equals(idLimpio)) {
                    return new Usuario(3, "Eduardo", "Ayala", "eduardo@paciente.com", "", "PACIENTE", null, true);
                } else if ("eduardo@medico.com".equals(idLimpio) || "medico@clinica.pe".equals(idLimpio) || "medico".equals(idLimpio) || "cmendoza@clinicainternacional.pe".equals(idLimpio)) {
                    return new Usuario(4, "Eduardo", "Ayala", "eduardo@medico.com", "", "MEDICO", null, true);
                } else if ("eduardo@padmin.com".equals(idLimpio) || "personal@clinica.pe".equals(idLimpio) || "personal".equals(idLimpio)) {
                    return new Usuario(2, "Eduardo", "Ayala", "eduardo@padmin.com", "", "PERSONAL_ADMINISTRATIVO", null, true);
                } else if ("eduardo@admin.com".equals(idLimpio) || "admin@clinica.pe".equals(idLimpio) || "admin".equals(idLimpio) || "admin@clinicainternacional.pe".equals(idLimpio)) {
                    return new Usuario(1, "Eduardo", "Ayala", "eduardo@admin.com", "", "ADMINISTRADOR", null, true);
                }
            }
        }

        if (usuario != null && usuario.isActivo()) {
            boolean validPassword = SecurityUtil.checkPassword(password, usuario.getPasswordHash());
            if (!validPassword && DEMO_MODE && esPasswordDemo(password)) {
                validPassword = true; // Fallback para la DB de pruebas (solo DEMO_MODE)
            }
            
            if (validPassword) {
                intentosFallidos.remove(idLimpio);
                bloqueosTemporales.remove(idLimpio);

                usuarioDAO.registrarAuditoria(usuario.getIdUsuario(), ipOrigen, "LOGIN_EXITOSO", "Inicio de sesión correcto con rol: " + usuario.getRol(), false);
                return usuario;
            }
        }

        registrarFallo(idLimpio);
        usuarioDAO.registrarAuditoria(usuario != null ? usuario.getIdUsuario() : null, ipOrigen, "LOGIN_FALLIDO", "Credenciales incorrectas para: " + idLimpio, false);

        return null;
    }

    private boolean esPasswordDemo(String password) {
        return "Password123!".equals(password) || "Admin123!".equals(password);
    }

    private boolean estaBloqueado(String key) {
        Long tiempoBloqueo = bloqueosTemporales.get(key);
        if (tiempoBloqueo == null) {
            return false;
        }
        if (System.currentTimeMillis() > tiempoBloqueo) {
            bloqueosTemporales.remove(key);
            intentosFallidos.remove(key);
            return false;
        }
        return true;
    }

    private void registrarFallo(String key) {
        int intentos = intentosFallidos.getOrDefault(key, 0) + 1;
        intentosFallidos.put(key, intentos);

        if (intentos >= MAX_INTENTOS) {
            bloqueosTemporales.put(key, System.currentTimeMillis() + TIEMPO_BLOQUEO_MS);
            LOGGER.warning("Cuenta bloqueada por rebasar el límite de intentos fallidos: " + key);
        }
    }

    public boolean existeDocumentoOCorreo(String documento, String email) {
        return usuarioDAO.existeDocumentoOCorreo(documento, email);
    }

    public boolean registrarPaciente(Usuario usuario, pe.edu.utp.clinica.model.Paciente paciente) {
        if (existeDocumentoOCorreo(paciente.getDni(), usuario.getEmail())) {
            return false;
        }
        
        // Cifrado de contraseña con BCrypt antes de guardar
        usuario.setPasswordHash(SecurityUtil.hashPassword(usuario.getPasswordHash()));
        usuario.setRol("PACIENTE");
        usuario.setActivo(true);
        
        return usuarioDAO.crearCuentaPaciente(usuario, paciente);
    }

    public String getLockoutMessage(String identificador) {
        if (identificador == null) return "Credenciales incorrectas.";
        String idLimpio = SecurityUtil.sanitizeInput(identificador).toLowerCase();
        
        Long tiempoBloqueo = bloqueosTemporales.get(idLimpio);
        if (tiempoBloqueo != null) {
            long rest = tiempoBloqueo - System.currentTimeMillis();
            if (rest > 0) {
                long minutes = rest / 60000;
                return "Acceso restringido por múltiples intentos fallidos. Intente de nuevo en " + (minutes + 1) + " minutos.";
            }
        }
        
        int intentos = intentosFallidos.getOrDefault(idLimpio, 0);
        if (intentos > 0 && intentos < MAX_INTENTOS) {
            if (intentos >= 2) {
                return "Credenciales incorrectas. Le quedan " + (MAX_INTENTOS - intentos) + " intentos antes de bloquear la cuenta.";
            }
        }
        return "Credenciales incorrectas.";
    }
}
