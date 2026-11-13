package pe.edu.utp.clinica.model;

import java.io.Serializable;
import pe.edu.utp.clinica.util.NombrePersona;
import java.time.LocalDateTime;

public class Usuario implements Serializable {
    private int idUsuario;
    private String nombres;
    private String apellidos;
    private String email;
    private String passwordHash;
    private String rol;
    private LocalDateTime fechaRegistro;
    private boolean activo;

    private String cmp;
    private String especialidad;
    private String codigoEmpleado;
    private String cargo;
    private Integer idEspecialidad;
    private Integer idSede;
    private String nombreSede;
    private String estadoDetallado;
    private String motivoEstado;

    public Usuario() {
    }

    public Usuario(int idUsuario, String nombres, String apellidos, String email, String passwordHash, String rol, LocalDateTime fechaRegistro, boolean activo) {
        this.idUsuario = idUsuario;
        setNombres(nombres);
        setApellidos(apellidos);
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
        this.nombres = NombrePersona.formatear(nombres);
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = NombrePersona.formatear(apellidos);
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

    public Integer getIdEspecialidad() { return idEspecialidad; }
    public void setIdEspecialidad(Integer idEspecialidad) { this.idEspecialidad = idEspecialidad; }

    public Integer getIdSede() { return idSede; }
    public void setIdSede(Integer idSede) { this.idSede = idSede; }

    public String getNombreSede() {
        if (nombreSede != null && !nombreSede.trim().isEmpty()) return nombreSede;
        if ("ADMINISTRADOR".equalsIgnoreCase(rol)) return "Multisede";
        return "Sede San Borja";
    }
    public void setNombreSede(String nombreSede) { this.nombreSede = nombreSede; }

    public String getEstadoDetallado() {
        if (estadoDetallado != null && !estadoDetallado.trim().isEmpty()) return estadoDetallado;
        return isActivo() ? "Activo" : "Inactivo";
    }
    public void setEstadoDetallado(String estadoDetallado) { this.estadoDetallado = estadoDetallado; }

    public String getMotivoEstado() { return motivoEstado; }
    public void setMotivoEstado(String motivoEstado) { this.motivoEstado = motivoEstado; }

    public String getRolEtiqueta() {
        if ("MEDICO".equalsIgnoreCase(rol)) return "Médico";
        if ("PERSONAL_ADMINISTRATIVO".equalsIgnoreCase(rol)) return "Personal administrativo";
        if ("ADMINISTRADOR".equalsIgnoreCase(rol)) return "Administrador";
        return rol != null ? rol : "Usuario";
    }

    public String getFechaRegistroFormateada() {
        if (fechaRegistro != null) {
            return String.format("%02d/%02d/%d", fechaRegistro.getDayOfMonth(), fechaRegistro.getMonthValue(), fechaRegistro.getYear());
        }
        return "02/03/2025";
    }

    public String getNombreMostrar() {
        String nombreCompleto = getNombreCompleto().trim();
        if ("MEDICO".equalsIgnoreCase(rol) && !nombreCompleto.toLowerCase().startsWith("dr.") && !nombreCompleto.toLowerCase().startsWith("dra.")) {
            return "Dr. " + nombreCompleto;
        }
        return nombreCompleto;
    }
}
