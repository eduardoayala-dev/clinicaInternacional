package pe.edu.utp.clinica.correo;

import java.util.Map;

public final class ConfiguracionCorreo {
    private ConfiguracionCorreo() {}

    public static EmisorCorreo crear(Map<String, String> entorno) {
        return switch (entorno.getOrDefault("MAIL_MODE", "disabled")) {
            case "gmail" -> new GmailCorreo(entorno.get("GMAIL_CLIENT_ID"),
                    entorno.get("GMAIL_CLIENT_SECRET"), entorno.get("GMAIL_REFRESH_TOKEN"),
                    entorno.get("MAIL_FROM"));
            case "disabled" -> correo -> {
                throw new EnvioCorreoException("El envío de correo no está configurado.", false);
            };
            default -> throw new IllegalArgumentException("MAIL_MODE debe ser disabled o gmail.");
        };
    }
}
