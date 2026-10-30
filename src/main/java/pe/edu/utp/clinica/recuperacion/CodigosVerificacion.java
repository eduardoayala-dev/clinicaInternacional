package pe.edu.utp.clinica.recuperacion;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public final class CodigosVerificacion {
    private static final SecureRandom RANDOM=new SecureRandom();
    private final byte[] clave;

    public CodigosVerificacion(String base64) {
        try { clave=Base64.getDecoder().decode(base64); }
        catch (RuntimeException e) { throw new IllegalArgumentException("Falta ACCOUNT_VERIFICATION_KEY en Base64."); }
        if (clave.length!=32) throw new IllegalArgumentException("ACCOUNT_VERIFICATION_KEY requiere 32 bytes aleatorios.");
    }

    public String nuevo() { return String.format(Locale.ROOT,"%06d",RANDOM.nextInt(1_000_000)); }

    public byte[] hash(String id,String proposito,String etapa,String codigo) {
        try {
            Mac mac=Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave,"HmacSHA256"));
            return mac.doFinal((id+":"+proposito+":"+etapa+":"+codigo).getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) { throw new IllegalStateException("HMAC-SHA256 no disponible.",e); }
    }
}
