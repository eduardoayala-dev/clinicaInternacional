package pe.edu.utp.clinica.model;

import java.time.LocalDate;

public class Paciente {
    private int idPaciente;
    private int usuarioId;
    private String dni;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String direccion;

    public Paciente() {}

    public Paciente(int idPaciente, int usuarioId, String dni, LocalDate fechaNacimiento, String telefono, String direccion) {
        this.idPaciente = idPaciente;
        this.usuarioId = usuarioId;
        this.dni = dni;
        this.fechaNacimiento = fechaNacimiento;
        this.telefono = telefono;
        this.direccion = direccion;
    }

    public int getIdPaciente() { return idPaciente; }
    public void setIdPaciente(int idPaciente) { this.idPaciente = idPaciente; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
}
