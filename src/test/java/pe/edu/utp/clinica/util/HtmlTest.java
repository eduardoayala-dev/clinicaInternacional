package pe.edu.utp.clinica.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class HtmlTest {
    @Test void datosJsonNoPuedenCerrarElScriptYConservanElValor() throws Exception {
        String entrada="</script><img src=x onerror=alert(1)>&\u2028\u2029";
        String salida=Html.json(Map.of("nombre",entrada));
        assertFalse(salida.contains("<"));
        assertFalse(salida.contains("&"));
        assertEquals(entrada,new ObjectMapper().readTree(salida).get("nombre").asText());
    }
    @Test void atributosEscapanComillasYTexto() {
        assertEquals("&quot;&#39;&lt;&amp;&gt;",Html.texto("\"'<&>"));
    }
}
