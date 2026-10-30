package pe.edu.utp.clinica.correo;

import java.util.Objects;
import java.util.regex.Pattern;

public record Correo(String destinatario, String asunto, String texto) {
    private static final Pattern DIRECCION = Pattern.compile(
            "[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+");

    public Correo {
        destinatario = validarDireccion(destinatario);
        Objects.requireNonNull(asunto, "Falta el asunto.");
        Objects.requireNonNull(texto, "Falta el contenido.");
        if (asunto.isBlank() || asunto.length() > 200 || asunto.contains("\r") || asunto.contains("\n")
                || texto.isBlank() || texto.length() > 20_000) {
            throw new IllegalArgumentException("Contenido de correo inválido.");
        }
    }

    static String validarDireccion(String direccion) {
        if (direccion == null || direccion.length() > 254 || !DIRECCION.matcher(direccion).matches()) {
            throw new IllegalArgumentException("Dirección de correo inválida.");
        }
        return direccion;
    }

    @Override public String toString() {
        return "Correo[contenido privado]";
    }
}
