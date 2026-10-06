package pe.edu.utp.clinica.model;

import java.time.LocalDateTime;

public class Cita {
    private int idCita;
    private int pacienteId;
    private int medicoId;
    private int disponibilidadId;
    private int motivoId;
    private LocalDateTime fechaHora;
    private String estado;
    private String nivelUrgencia;
    private LocalDateTime fechaCreacion;

    public Cita() {}

    public Cita(int idCita, int pacienteId, int medicoId, int disponibilidadId, int motivoId, LocalDateTime fechaHora, String estado, String nivelUrgencia, LocalDateTime fechaCreacion) {
        this.idCita = idCita;
        this.pacienteId = pacienteId;
        this.medicoId = medicoId;
        this.disponibilidadId = disponibilidadId;
        this.motivoId = motivoId;
        this.fechaHora = fechaHora;
        this.estado = estado;
        this.nivelUrgencia = nivelUrgencia;
        this.fechaCreacion = fechaCreacion;
    }

    public int getIdCita() { return idCita; }
    public void setIdCita(int idCita) { this.idCita = idCita; }

    public int getPacienteId() { return pacienteId; }
    public void setPacienteId(int pacienteId) { this.pacienteId = pacienteId; }

    public int getMedicoId() { return medicoId; }
    public void setMedicoId(int medicoId) { this.medicoId = medicoId; }

    public int getDisponibilidadId() { return disponibilidadId; }
    public void setDisponibilidadId(int disponibilidadId) { this.disponibilidadId = disponibilidadId; }

    public int getMotivoId() { return motivoId; }
    public void setMotivoId(int motivoId) { this.motivoId = motivoId; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getNivelUrgencia() { return nivelUrgencia; }
    public void setNivelUrgencia(String nivelUrgencia) { this.nivelUrgencia = nivelUrgencia; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
