package pe.edu.utp.clinica.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

public final class NombrePersona {
    private static final Locale SPANISH = Locale.forLanguageTag("es-PE");
    private static final Set<String> PARTICLES = Set.of("de", "del", "la", "las", "los", "y");
    private NombrePersona() {}

    public static String formatear(String value) {
        if (value == null) return null;
        String clean = Normalizer.normalize(value, Normalizer.Form.NFC)
                .replaceAll("[\\s\\p{Z}]+", " ").trim();
        if (clean.isEmpty()) return clean;
        String[] words = clean.split(" ");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String lower = word.toLowerCase(SPANISH);
            if (i > 0 && PARTICLES.contains(lower)) {
                words[i] = lower;
                continue;
            }
            if (word.codePoints().anyMatch(Character::isLowerCase)
                    && word.codePoints().anyMatch(Character::isUpperCase)) continue;
            StringBuilder formatted = new StringBuilder();
            boolean initial = true;
            for (int code : lower.codePoints().toArray()) {
                formatted.appendCodePoint(initial && Character.isLetter(code) ? Character.toTitleCase(code) : code);
                if (Character.isLetter(code)) initial = false;
                else if (code == '-' || code == '\'' || code == '’') initial = true;
            }
            words[i] = formatted.toString();
        }
        return String.join(" ", words);
    }
}
