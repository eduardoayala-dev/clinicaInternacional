package pe.edu.utp.clinica.catalogo;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CodigoSedeTest {
    @Test void utilizaInicialesConservandoNombresDeLugar(){
        assertEquals("SED-SB",CodigoSede.base("Sede San Borja"));assertEquals("SED-SB",CodigoSede.base("Clínica de San Borja"));
        assertEquals("SED-LM",CodigoSede.base("La Molina"));assertEquals("SED-N",CodigoSede.base("Ñaña"));
        assertThrows(IllegalArgumentException.class,()->CodigoSede.base("Sede"));
    }
}
