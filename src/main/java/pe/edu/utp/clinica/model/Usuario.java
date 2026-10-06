package pe.edu.utp.clinica.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Usuario implements Serializable {
    private int idUsuario;
    private String nombres;
    private String apellidos;
    private String email;
    private String passwordHash;
    private String rol; // PACIENTE, MEDICO, PERSONAL_ADMINISTRATIVO, ADMINISTRADOR
    private LocalDateTime fechaRegistro;
    private boolean activo;

    // Transient UI Fields (from JOINs)
    private String cmp;
    private String especialidad;
    private String codigoEmpleado;
    private String cargo;

    public Usuario() {
    }

    public Usuario(int idUsuario, String nombres, String apellidos, String email, String passwordHash, String rol, LocalDateTime fechaRegistro, boolean activo) {
        this.idUsuario = idUsuario;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.fechaRegistro = fechaRegistro;
        this.activo = activo;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getNombreCompleto() {
        return (nombres != null ? nombres : "") + " " + (apellidos != null ? apellidos : "");
    }

    public String getCmp() { return cmp; }
    public void setCmp(String cmp) { this.cmp = cmp; }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public String getCodigoEmpleado() { return codigoEmpleado; }
    public void setCodigoEmpleado(String codigoEmpleado) { this.codigoEmpleado = codigoEmpleado; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
}
