package pe.edu.utp.clinica.service.dni;

public record DniDatos(String numero, String nombres, String apellidoPaterno, String apellidoMaterno)
        implements java.io.Serializable {}
