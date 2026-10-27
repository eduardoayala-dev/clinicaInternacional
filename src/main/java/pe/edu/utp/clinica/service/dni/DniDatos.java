package pe.edu.utp.clinica.service.dni;

/** Contrato propio: nunca devolver la respuesta completa del proveedor. */
public record DniDatos(String numero, String nombres, String apellidoPaterno, String apellidoMaterno) {}
