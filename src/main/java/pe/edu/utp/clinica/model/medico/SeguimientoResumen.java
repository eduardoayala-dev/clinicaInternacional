package pe.edu.utp.clinica.model.medico;

public record SeguimientoResumen(int idCita, int idPaciente, String paciente, String dni, java.time.LocalDate fecha, String prioridad, String estado, Integer idControl, java.time.LocalDateTime fechaControl) {}
