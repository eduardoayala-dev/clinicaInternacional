package pe.edu.utp.clinica.service.dni;

import pe.edu.utp.clinica.api.ApiException;

public final class DniException extends ApiException {
    private static final long serialVersionUID = 1L;
    private final int retryAfter;

    public DniException(int status, String code, String message) {
        this(status, code, message, 0);
    }

    public DniException(int status, String code, String message, int retryAfter) {
        super(status, code, message);
        this.retryAfter = retryAfter;
    }

    public int retryAfter() { return retryAfter; }
}
