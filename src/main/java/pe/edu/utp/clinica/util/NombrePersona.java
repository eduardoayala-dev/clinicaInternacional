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
            words[i] = formatWord(words[i], i == 0);
        }
        return String.join(" ", words);
    }

    private static String formatWord(String word, boolean first) {
        String lower = word.toLowerCase(SPANISH);
        if (!first && PARTICLES.contains(lower)) return lower;
        if (word.codePoints().anyMatch(Character::isLowerCase)
                && word.codePoints().anyMatch(Character::isUpperCase)) return word;
        return capitalizeCompound(lower);
    }

    private static String capitalizeCompound(String word) {
        StringBuilder formatted = new StringBuilder();
        boolean initial = true;
        for (int code : word.codePoints().toArray()) {
            formatted.appendCodePoint(initial && Character.isLetter(code) ? Character.toTitleCase(code) : code);
            if (Character.isLetter(code)) initial = false;
            else if (code == '-' || code == '\'' || code == '’') initial = true;
        }
        return formatted.toString();
    }
}
