package pe.edu.utp.clinica.service.dni;

public final class DniException extends Exception {
    private final int status;
    private final String code;
    private final int retryAfter;

    public DniException(int status, String code, String message) {
        this(status, code, message, 0);
    }

    public DniException(int status, String code, String message, int retryAfter) {
        super(message);
        this.status = status;
        this.code = code;
        this.retryAfter = retryAfter;
    }

    public int status() { return status; }
    public String code() { return code; }
    public int retryAfter() { return retryAfter; }
}
