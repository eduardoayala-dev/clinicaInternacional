package pe.edu.utp.clinica.recuperacion;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CodigosVerificacionTest {
    @Test void exigeClavePrivadaConfigurada() {
        for(String clave:new String[]{null,"","no-base64",Base64.getEncoder().encodeToString(new byte[16])}){
            assertThrows(IllegalArgumentException.class,()->new CodigosVerificacion(clave));
        }
    }
    @Test void hashCambiaConSesionPropositoEtapaYClave() {
        var uno=new CodigosVerificacion(Base64.getEncoder().encodeToString(new byte[32]));
        byte[] distinta=new byte[32];Arrays.fill(distinta,(byte)1);
        var dos=new CodigosVerificacion(Base64.getEncoder().encodeToString(distinta));
        byte[] inicial=uno.hash("id","RECUPERAR","CODIGO_ACTUAL","123456");
        assertFalse(Arrays.equals(inicial,uno.hash("otro","RECUPERAR","CODIGO_ACTUAL","123456")));
        assertFalse(Arrays.equals(inicial,uno.hash("id","CAMBIAR_PASSWORD","CODIGO_ACTUAL","123456")));
        assertFalse(Arrays.equals(inicial,uno.hash("id","RECUPERAR","CODIGO_NUEVO","123456")));
        assertFalse(Arrays.equals(inicial,dos.hash("id","RECUPERAR","CODIGO_ACTUAL","123456")));
        assertTrue(uno.nuevo().matches("[0-9]{6}"));
    }
}
