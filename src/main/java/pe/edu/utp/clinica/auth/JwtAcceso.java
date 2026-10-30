package pe.edu.utp.clinica.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public final class JwtAcceso {
    public static final long DURACION_SEGUNDOS = 300;
    public static final Set<String> ROLES = Set.of("PACIENTE", "MEDICO", "PERSONAL_ADMINISTRATIVO", "ADMINISTRADOR");
    private final SecretKey key;
    private final String issuer;
    private final String audience;
    private final Clock clock;

    public record Identidad(int usuario, String sesion, String rol) { }

    public JwtAcceso(String base64Key, String issuer, String audience, Clock clock) {
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(base64Key); }
        catch (RuntimeException e) { throw new IllegalArgumentException("AUTH_JWT_KEY debe ser una clave Base64 válida."); }
        if (bytes.length != 32) throw new IllegalArgumentException("AUTH_JWT_KEY debe contener 32 bytes aleatorios.");
        if (issuer == null || issuer.isBlank() || audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("Se requiere emisor y audiencia JWT.");
        }
        this.key = new SecretKeySpec(bytes, "HmacSHA256");
        this.issuer = issuer;
        this.audience = audience;
        this.clock = clock;
    }

    public String emitir(int usuario, String sesion, String rol) {
        if (usuario <= 0 || !ROLES.contains(rol)) throw new IllegalArgumentException("Identidad inválida.");
        UUID.fromString(sesion);
        Instant now = clock.instant();
        return Jwts.builder().header().type("at+jwt").and().issuer(issuer).audience().add(audience).and()
                .subject(Integer.toString(usuario)).id(UUID.randomUUID().toString())
                .claim("sid", sesion).claim("rol", rol).issuedAt(Date.from(now)).notBefore(Date.from(now))
                .expiration(Date.from(now.plusSeconds(DURACION_SEGUNDOS))).signWith(key, Jwts.SIG.HS256).compact();
    }

    public Identidad verificar(String token) {
        if (token == null || token.length() > 4096) throw new JwtException("Acceso inválido.");
        var signed = Jwts.parser().verifyWith(key).sig().clear().add(Jwts.SIG.HS256).and()
                .requireIssuer(issuer).requireAudience(audience).clock(() -> Date.from(clock.instant()))
                .build().parseSignedClaims(token);
        Claims c = signed.getPayload();
        try {
            int usuario = Integer.parseInt(c.getSubject());
            String sid = c.get("sid", String.class), rol = c.get("rol", String.class);
            UUID.fromString(sid);
            UUID.fromString(c.getId());
            if (!"at+jwt".equals(signed.getHeader().getType()) || usuario <= 0 || !ROLES.contains(rol)
                    || c.getIssuedAt() == null || c.getNotBefore() == null || c.getExpiration() == null
                    || c.getIssuedAt().toInstant().isAfter(clock.instant())
                    || !c.getExpiration().after(c.getIssuedAt())
                    || c.getExpiration().toInstant().isAfter(c.getIssuedAt().toInstant().plusSeconds(DURACION_SEGUNDOS))) {
                throw new IllegalArgumentException();
            }
            return new Identidad(usuario, sid, rol);
        } catch (RuntimeException e) { throw new JwtException("Claims de acceso inválidos."); }
    }
}
