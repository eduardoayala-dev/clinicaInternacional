package pe.edu.utp.clinica.model.medico;

public record PacienteResumen(int idPaciente, String nombre, String dni, String alergias, java.time.LocalDate nacimiento, java.time.LocalDateTime ultimaAtencion, int atenciones) {}
