package pe.edu.utp.clinica.api;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public final class ParametrosApi {
    private static final Pattern INTEGER = Pattern.compile("\\d+");
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private ParametrosApi() {}

    public static int positivo(String value, String name) throws ApiException {
        String clean = value == null ? "" : value.trim();
        if (!INTEGER.matcher(clean).matches()) throw invalidId(name);
        try {
            int id = Integer.parseInt(clean);
            if (id <= 0) throw invalidId(name);
            return id;
        } catch (NumberFormatException e) {
            throw invalidId(name);
        }
    }

    public static Integer positivoOpcional(String value, String name) throws ApiException {
        return value == null || value.isBlank() ? null : positivo(value, name);
    }

    public static LocalDate fechaOpcional(String value) throws ApiException {
        if (value == null || value.isBlank()) return null;
        String clean = value.trim();
        if (!DATE.matcher(clean).matches()) throw invalidDate();
        try {
            LocalDate date = LocalDate.parse(clean);
            if (date.getYear() < 1000) throw invalidDate();
            return date;
        } catch (DateTimeParseException e) {
            throw invalidDate();
        }
    }

    private static ApiException invalidId(String name) {
        return new ApiException(400, "PARAMETRO_INVALIDO", name + " debe ser un entero positivo.");
    }

    private static ApiException invalidDate() {
        return new ApiException(400, "FECHA_INVALIDA", "fecha debe ser una fecha válida en formato AAAA-MM-DD.");
    }
}
