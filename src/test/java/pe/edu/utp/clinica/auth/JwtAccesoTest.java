package pe.edu.utp.clinica.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;
import javax.crypto.spec.SecretKeySpec;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class JwtAccesoTest {
    private final String key=Base64.getEncoder().encodeToString(new byte[32]);
    private final Clock now=Clock.fixed(Instant.parse("2026-10-29T12:00:00Z"),ZoneOffset.UTC);
    private final String sid=UUID.randomUUID().toString();
    private JwtAcceso jwt(Clock clock) { return new JwtAcceso(key,"clinica","web",clock); }
    @Test void issuesUniqueSignedAccessForEachRoleWithoutPersonalData() {
        for (String rol:JwtAcceso.ROLES) {
            String one=jwt(now).emitir(42,sid,rol), two=jwt(now).emitir(42,sid,rol);
            assertNotEquals(one,two);
            assertEquals(new JwtAcceso.Identidad(42,sid,rol),jwt(now).verificar(one));
            String payload=new String(Base64.getUrlDecoder().decode(one.split("\\.")[1]),java.nio.charset.StandardCharsets.UTF_8);
            assertFalse(payload.contains("password")); assertFalse(payload.contains("dni")); assertFalse(payload.contains("nombres"));
        }
    }
    @Test void expiredAccessCannotAuthenticate() {
        String token=jwt(now).emitir(42,sid,"PACIENTE");
        assertThrows(ExpiredJwtException.class,()->jwt(Clock.offset(now,Duration.ofSeconds(301))).verificar(token));
    }
    @Test void rejectsTamperingWrongKeyIssuerAudienceAndUnsignedTokens() {
        String token=jwt(now).emitir(42,sid,"MEDICO");
        byte[] other=new byte[32]; Arrays.fill(other,(byte)1);
        assertThrows(JwtException.class,()->new JwtAcceso(Base64.getEncoder().encodeToString(other),"clinica","web",now).verificar(token));
        assertThrows(JwtException.class,()->new JwtAcceso(key,"otro","web",now).verificar(token));
        assertThrows(JwtException.class,()->new JwtAcceso(key,"clinica","otra",now).verificar(token));
        assertThrows(JwtException.class,()->jwt(now).verificar(token.substring(0,token.lastIndexOf('.')+1)));
        assertThrows(JwtException.class,()->jwt(now).verificar("eyJhbGciOiJub25lIn0.e30."));
    }
    @Test void rejectsMissingClaimsAndDifferentAlgorithms() {
        var key512=new SecretKeySpec(new byte[64],"HmacSHA512");
        String wrong=Jwts.builder().subject("42").signWith(key512,Jwts.SIG.HS512).compact();
        assertThrows(JwtException.class,()->jwt(now).verificar(wrong));
        var signing=new SecretKeySpec(new byte[32],"HmacSHA256");
        String missing=Jwts.builder().issuer("clinica").audience().add("web").and().subject("42").signWith(signing,Jwts.SIG.HS256).compact();
        assertThrows(JwtException.class,()->jwt(now).verificar(missing));
    }
    @Test void rejectsWeakConfigurationAndFutureTokens() {
        assertThrows(IllegalArgumentException.class,()->new JwtAcceso("short","clinica","web",now));
        assertThrows(IllegalArgumentException.class,()->new JwtAcceso(key,"","web",now));
        String token=jwt(Clock.offset(now,Duration.ofMinutes(1))).emitir(42,sid,"MEDICO");
        assertThrows(JwtException.class,()->jwt(now).verificar(token));
    }
}
