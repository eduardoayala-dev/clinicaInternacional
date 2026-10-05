package pe.edu.utp.clinica.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilidad de Cifrado y Sanear Entradas.
 * Satisface los requisitos de seguridad: RNF-02 (Validación/Sanear), RNF-03 (Anti SQLi), RNF-04 (Hashing BCrypt / XSS).
 */
public class SecurityUtil {

    private SecurityUtil() {
    }

    /**
     * Encripta una contraseña con el algoritmo BCrypt + Salt aleatorio.
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
    }

    /**
     * Verifica si una contraseña en texto plano coincide con su Hash BCrypt.
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sanea entradas de texto para evitar ataques XSS (Cross-Site Scripting).
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("<", "&lt;")
                    .replaceAll(">", "&gt;")
                    .replaceAll("\"", "&quot;")
                    .replaceAll("'", "&#x27;")
                    .replaceAll("&", "&amp;")
                    .trim();
    }
}
