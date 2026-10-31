package pe.edu.utp.clinica.ia;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.AgendaException;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GeminiCatalogoTest {
    private final List<OrientacionCatalogo.Especialidad> catalogo=List.of(new OrientacionCatalogo.Especialidad(72,"Dermatología"));
    private String respuesta(String texto)throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("candidates",List.of(java.util.Map.of("finishReason","STOP","content",java.util.Map.of("parts",List.of(java.util.Map.of("text",texto)))))));
    }
    @Test void enviaClaveSoloEnCabeceraYLeeJsonEstructurado() throws Exception {
        String cuerpo=respuesta("{\"idEspecialidad\":72,\"motivo\":\"CATALOGO\"}");
        var cliente=new GeminiCatalogo("clave_de_prueba_123456","modelo-de-prueba",req->{
            assertEquals("generativelanguage.googleapis.com",req.uri().getHost());assertNull(req.uri().getQuery());
            assertEquals("clave_de_prueba_123456",req.headers().firstValue("x-goog-api-key").orElseThrow());
            assertTrue(req.timeout().orElseThrow().toSeconds()<=12);return new GeminiCatalogo.Respuesta(200,cuerpo);
        });
        assertEquals(72,cliente.elegir("Caso ficticio sin información personal",catalogo).idEspecialidad());
    }
    @Test void noAceptaTextoLibreCamposExtraNiIdsFraccionarios() throws Exception {
        for(String texto:List.of("Toma un medicamento", "{\"idEspecialidad\":72,\"motivo\":\"CATALOGO\",\"diagnostico\":\"algo\"}","{\"idEspecialidad\":72.5,\"motivo\":\"CATALOGO\"}")) {
            String cuerpo=respuesta(texto);var c=new GeminiCatalogo("clave_de_prueba_123456","modelo-de-prueba",r->new GeminiCatalogo.Respuesta(200,cuerpo));
            assertThrows(AgendaException.class,()->c.elegir("Caso ficticio de prueba",catalogo));
        }
    }
    @Test void cuotasFallosYBloqueosNoExponenElCuerpoDelProveedor() {
        for(int status:new int[]{401,429,500}) {
            var c=new GeminiCatalogo("clave_de_prueba_123456","modelo-de-prueba",r->new GeminiCatalogo.Respuesta(status,"contenido_privado_de_prueba"));
            var e=assertThrows(AgendaException.class,()->c.elegir("Caso ficticio de prueba",catalogo));assertFalse(e.getMessage().contains("contenido_privado"));
        }
        for(String cuerpo:List.of("null","{}","{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}")) {
            var c=new GeminiCatalogo("clave_de_prueba_123456","modelo-de-prueba",r->new GeminiCatalogo.Respuesta(200,cuerpo));
            assertThrows(AgendaException.class,()->c.elegir("Caso ficticio de prueba",catalogo));
        }
    }
    @Test void erroresDeRedYConfiguracionNoSeConviertenEnRecomendaciones() {
        var c=new GeminiCatalogo("clave_de_prueba_123456","modelo-de-prueba",r->{throw new IOException("detalle privado");});
        assertEquals(503,assertThrows(AgendaException.class,()->c.elegir("Caso ficticio de prueba",catalogo)).status());
        assertThrows(IllegalArgumentException.class,()->new GeminiCatalogo("clave_de_prueba_123456","../otro"));
    }
}
