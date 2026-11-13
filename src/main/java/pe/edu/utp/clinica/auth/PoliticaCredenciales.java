package pe.edu.utp.clinica.auth;

import java.nio.charset.StandardCharsets;

public final class PoliticaCredenciales {
    public static final int INACTIVIDAD_SEGUNDOS=900;
    private PoliticaCredenciales() {}
    public static boolean valida(String clave) {
        return clave!=null && clave.length()>=12 && clave.getBytes(StandardCharsets.UTF_8).length<=72
                && clave.chars().noneMatch(Character::isISOControl)
                && clave.chars().anyMatch(Character::isUpperCase)
                && clave.chars().anyMatch(Character::isLowerCase)
                && clave.chars().anyMatch(Character::isDigit)
                && clave.chars().anyMatch(c->!Character.isLetterOrDigit(c) && !Character.isWhitespace(c));
    }
}
