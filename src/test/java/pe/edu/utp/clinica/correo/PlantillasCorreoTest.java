package pe.edu.utp.clinica.correo;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class PlantillasCorreoTest {
    @Test void codigoTieneVersionHtmlYTextoSinRecursosRemotos() {
        var correo=PlantillasCorreo.verificacion("p@example.test","cambiar tu contraseña","123456");
        assertTrue(correo.texto().contains("123456"));assertTrue(correo.html().contains("123456"));
        assertTrue(correo.html().contains("10 minutos"));assertTrue(correo.html().contains("5 intentos"));
        assertFalse(correo.html().contains("https://"));assertFalse(correo.html().contains("<script"));
        assertFalse(correo.toString().contains("123456"));
    }
    @Test void textoDinamicoSeEscapaAntesDeInsertarseEnHtml() {
        var correo=PlantillasCorreo.verificacion("p@example.test","<script>alert('x')</script>","123456");
        assertFalse(correo.html().contains("<script>"));assertTrue(correo.html().contains("&lt;script&gt;"));
        assertThrows(IllegalArgumentException.class,()->PlantillasCorreo.verificacion("p@example.test","cambio","<b>123"));
    }
    @Test void mimeIncluyeAmbasAlternativasYConservaUtf8() {
        var correo=PlantillasCorreo.verificacion("p@example.test","confirmar tu nueva dirección de correo","123456");
        String mime=GmailCorreo.mime(correo,"clinica@gmail.com");
        var matcher=Pattern.compile("boundary=\"([^\"]+)\"").matcher(mime);assertTrue(matcher.find());
        String limite=matcher.group(1);assertTrue(mime.contains("multipart/alternative"));
        String[] partes=mime.split(Pattern.quote("--"+limite));
        assertEquals(correo.texto(),decodificar(partes[1]));assertEquals(correo.html(),decodificar(partes[2]));
        assertTrue(mime.endsWith("--"+limite+"--\r\n"));
        assertNotEquals(mime,GmailCorreo.mime(correo,"clinica@gmail.com"));
    }
    @Test void avisoDeCorreoActualizadoTieneAmbasVersiones() {
        var correo=PlantillasCorreo.correoActualizado("anterior@example.test");
        assertNotNull(correo.html());assertTrue(correo.texto().contains("otras sesiones"));
        assertTrue(correo.html().contains("Tu correo se actualizó"));
    }
    private String decodificar(String parte) {
        return new String(Base64.getMimeDecoder().decode(parte.split("\r\n\r\n",2)[1].strip()),StandardCharsets.UTF_8);
    }
}
