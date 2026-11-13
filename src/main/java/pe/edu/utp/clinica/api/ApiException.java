package pe.edu.utp.clinica.api;

public class ApiException extends Exception {
    private static final long serialVersionUID = 1L;
    private final int status;
    private final String code;

    public ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }
    public String code() { return code; }
}
