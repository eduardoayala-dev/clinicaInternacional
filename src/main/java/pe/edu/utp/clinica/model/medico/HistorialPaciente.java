package pe.edu.utp.clinica.model.medico;

public record HistorialPaciente(PacienteResumen paciente, Pagina<AtencionResumen> atenciones) {}
