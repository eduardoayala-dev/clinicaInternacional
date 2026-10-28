package pe.edu.utp.clinica.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class FormatoClinico {
    private static final String SIN_REGISTRAR = "Sin registrar";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Map<String,String> ESTADOS = Map.ofEntries(
            Map.entry("PENDIENTE","Pendiente"), Map.entry("VENCIDO","Vencido"),
            Map.entry("RESERVADO","Reserva vinculada"), Map.entry("REALIZADO","Realizado"),
            Map.entry("ATENDIDA","Atendida"), Map.entry("EN_ATENCION","En atención"),
            Map.entry("NO_ATENDIDA","No atendida"), Map.entry("CONFIRMADA","Confirmada"),
            Map.entry("SOLICITADA","Solicitada"), Map.entry("EN_SALA","En sala"));
    private FormatoClinico() {}
    public static String fecha(LocalDate fecha) { return fecha == null ? SIN_REGISTRAR : fecha.format(FECHA); }
    public static String fechaHora(LocalDateTime fecha) { return fecha == null ? SIN_REGISTRAR : fecha.format(FECHA_HORA); }
    public static String estado(String estado) { return estado == null ? SIN_REGISTRAR : ESTADOS.getOrDefault(estado, estado); }
}
