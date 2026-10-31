package pe.edu.utp.clinica.model.medico;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public record PropuestaHorario(int id, int medico, int sede, String nombreMedico, String nombreSede,
        int dias, LocalDate desde, LocalDate hasta, LocalTime inicio, LocalTime fin, int minutos,
        String estado, String motivo) {
    public boolean incluye(LocalDate fecha) {
        return (dias & (1 << (fecha.getDayOfWeek().getValue() - 1))) != 0;
    }

    public String diasTexto() {
        String[] nombres = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
        List<String> seleccion = new ArrayList<>();
        for (DayOfWeek dia : DayOfWeek.values()) {
            if ((dias & (1 << (dia.getValue() - 1))) != 0) seleccion.add(nombres[dia.getValue() - 1]);
        }
        return String.join(" · ", seleccion);
    }

    public String estadoVisible(LocalDate hoy) {
        if (("PUBLICADO".equals(estado) || "PENDIENTE".equals(estado)) && hasta.isBefore(hoy)) return "Vencido";
        if ("PUBLICADO".equals(estado) && !hasta.isAfter(hoy.plusDays(7))) return "Por vencer";
        return switch (estado) {
            case "PUBLICADO" -> "Publicado";
            case "RECHAZADO" -> "Rechazado";
            default -> "Pendiente de revisión";
        };
    }
}
