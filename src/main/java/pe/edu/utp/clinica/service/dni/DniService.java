package pe.edu.utp.clinica.service.dni;

public final class DniService {
    private final DniProvider provider;
    private final DniRateLimiter limiter;

    public DniService(DniProvider provider, DniRateLimiter limiter) {
        this.provider = provider;
        this.limiter = limiter;
    }

    public DniDatos consultar(String numero, String ip, String sessionId) throws DniException {
        if (numero == null || !numero.matches("[0-9]{8}")) {
            throw new DniException(400, "DNI_INVALIDO", "El DNI debe tener exactamente 8 dígitos.");
        }
        limiter.acquire(ip, sessionId);
        return provider.consultar(numero);
    }
}
