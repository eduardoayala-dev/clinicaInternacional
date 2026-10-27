package pe.edu.utp.clinica.service.dni;

import pe.edu.utp.clinica.util.NombrePersona;
import java.util.regex.Pattern;

public final class DniService {
    private static final Pattern DNI = Pattern.compile("[0-9]{8}");
    private final DniProvider provider;
    private final DniRateLimiter limiter;

    public DniService(DniProvider provider, DniRateLimiter limiter) {
        this.provider = provider;
        this.limiter = limiter;
    }

    public DniDatos consultar(String numero, String ip, String sessionId) throws DniException {
        validarNumero(numero);
        limiter.acquire(ip, sessionId);
        DniDatos datos = provider.consultar(numero);
        return new DniDatos(datos.numero(), NombrePersona.formatear(datos.nombres()),
                NombrePersona.formatear(datos.apellidoPaterno()), NombrePersona.formatear(datos.apellidoMaterno()));
    }

    static void validarNumero(String numero) throws DniException {
        if (numero == null || !DNI.matcher(numero).matches()) {
            throw new DniException(400, "DNI_INVALIDO", "El DNI debe tener exactamente 8 dígitos.");
        }
    }
}
