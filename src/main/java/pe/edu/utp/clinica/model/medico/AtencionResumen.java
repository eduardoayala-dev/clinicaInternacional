package pe.edu.utp.clinica.model.medico;

public record AtencionResumen(int idCita, java.time.LocalDateTime fecha, String estado, String prioridad, String indicaciones, String diagnostico, java.time.LocalDate seguimiento) {}
