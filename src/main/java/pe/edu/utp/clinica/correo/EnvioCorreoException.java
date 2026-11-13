package pe.edu.utp.clinica.correo;

public final class EnvioCorreoException extends Exception {
    private final boolean reintentable;

    public EnvioCorreoException(String mensaje, boolean reintentable) {
        super(mensaje);
        this.reintentable = reintentable;
    }

    public boolean reintentable() {
        return reintentable;
    }
}
