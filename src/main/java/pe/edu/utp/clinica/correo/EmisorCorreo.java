package pe.edu.utp.clinica.correo;

@FunctionalInterface
public interface EmisorCorreo {
    String enviar(Correo correo) throws EnvioCorreoException;
}
