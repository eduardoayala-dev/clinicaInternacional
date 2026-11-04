package pe.edu.utp.clinica.auth;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PoliticaCredencialesTest {
    @Test void exigeLongitudComposicionYLimiteBcrypt(){
        assertFalse(PoliticaCredenciales.valida("Password1!"));
        assertTrue(PoliticaCredenciales.valida("MiClaveSegura123!"));
        assertFalse(PoliticaCredenciales.valida("abcdefghijk123!"));
        assertFalse(PoliticaCredenciales.valida("ABCDEFGHIJK123!"));
        assertFalse(PoliticaCredenciales.valida("MiClaveSegura123 "));
        assertFalse(PoliticaCredenciales.valida("MiClaveSegura123!\n"));
        assertFalse(PoliticaCredenciales.valida("Ñ".repeat(35)+"abc123!"));
        assertTrue(PoliticaCredenciales.valida("A"+"a".repeat(68)+"1!"));
        assertEquals(900,PoliticaCredenciales.INACTIVIDAD_SEGUNDOS);
    }
}
