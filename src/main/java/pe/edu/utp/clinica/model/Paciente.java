package pe.edu.utp.clinica.model;

import java.time.LocalDate;
import java.time.Period;

public class Paciente {
    private int idPaciente;
    private int usuarioId;
    private String dni;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String direccion;
    private String canalAviso = "WhatsApp";
    private String sedePreferida = "Sede San Borja";
    private String alergias = "";
    private String tipoCobertura = "EPS";

    public Paciente() {}

    public Paciente(int idPaciente, int usuarioId, String dni, LocalDate fechaNacimiento, String telefono, String direccion) {
        this.idPaciente = idPaciente;
        this.usuarioId = usuarioId;
        this.dni = dni;
        this.fechaNacimiento = fechaNacimiento;
        this.telefono = telefono;
        this.direccion = direccion;
    }

    public Paciente(int idPaciente, int usuarioId, String dni, LocalDate fechaNacimiento, String telefono, String direccion,
                    String canalAviso, String sedePreferida, String alergias, String tipoCobertura) {
        this.idPaciente = idPaciente;
        this.usuarioId = usuarioId;
        this.dni = dni;
        this.fechaNacimiento = fechaNacimiento;
        this.telefono = telefono;
        this.direccion = direccion;
        this.canalAviso = (canalAviso != null && !canalAviso.trim().isEmpty()) ? canalAviso : "WhatsApp";
        this.sedePreferida = (sedePreferida != null && !sedePreferida.trim().isEmpty()) ? sedePreferida : "Sede San Borja";
        this.alergias = (alergias != null) ? alergias : "";
        this.tipoCobertura = (tipoCobertura != null && !tipoCobertura.trim().isEmpty()) ? tipoCobertura : "EPS";
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

    public String getCanalAviso() { return canalAviso != null ? canalAviso : "WhatsApp"; }
    public void setCanalAviso(String canalAviso) { this.canalAviso = canalAviso; }

    public String getSedePreferida() { return sedePreferida != null ? sedePreferida : "Sede San Borja"; }
    public void setSedePreferida(String sedePreferida) { this.sedePreferida = sedePreferida; }

    public String getAlergias() { return alergias != null ? alergias : ""; }
    public void setAlergias(String alergias) { this.alergias = alergias; }

    public String getTipoCobertura() { return tipoCobertura != null ? tipoCobertura : "EPS"; }
    public void setTipoCobertura(String tipoCobertura) { this.tipoCobertura = tipoCobertura; }

    public String getCodigoRegistro() {
        int id = idPaciente > 0 ? idPaciente : (usuarioId > 0 ? usuarioId : 0);
        return "PAC-" + String.format("%06d", id);
    }

    public String getEdadCalculada() {
        if (fechaNacimiento == null) {
            return "--";
        }
        int anios = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        return anios + " años";
    }
}

