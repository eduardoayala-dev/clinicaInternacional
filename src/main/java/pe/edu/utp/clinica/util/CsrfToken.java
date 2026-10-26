package pe.edu.utp.clinica.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Token aleatorio ligado a la sesión, independiente de JSESSIONID. */
public final class CsrfToken {
    public static final String ATTRIBUTE = "csrfToken";
    public static final String HEADER = "X-CSRF-Token";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfToken() { }

    public static String getOrCreate(HttpSession session) {
        synchronized (session) {
            String token = (String) session.getAttribute(ATTRIBUTE);
            if (token == null) {
                byte[] bytes = new byte[32];
                RANDOM.nextBytes(bytes);
                token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                session.setAttribute(ATTRIBUTE, token);
            }
            return token;
        }
    }

    public static boolean isValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        String expected = (String) session.getAttribute(ATTRIBUTE);
        String supplied = request.getHeader(HEADER);
        if (supplied == null) supplied = request.getParameter(ATTRIBUTE);
        return expected != null && supplied != null && supplied.length() <= 128
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                        supplied.getBytes(StandardCharsets.UTF_8));
    }
}
