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
            assertTrue(req.timeout().orElseThrow().toSeconds()<=20);return new GeminiCatalogo.Respuesta(200,cuerpo);
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
    @Test void admiteClaveDeAutenticacionConPuntoSinAlterarlaNiIncluirlaEnLaUrl() throws Exception {
        String clave="AQ."+"a".repeat(50);
        String cuerpo=respuesta("{\"idEspecialidad\":72,\"motivo\":\"CATALOGO\"}");
        var cliente=new GeminiCatalogo(clave,"gemini-3.1-flash-lite",req->{
            assertEquals(clave,req.headers().firstValue("x-goog-api-key").orElseThrow());
            assertFalse(req.uri().toString().contains(clave));
            return new GeminiCatalogo.Respuesta(200,cuerpo);
        });
        assertEquals(72,cliente.elegir("Caso ficticio de prueba",catalogo).idEspecialidad());
    }
    @Test void rechazaEspaciosSaltosDeLineaYClavesFueraDeLimites() {
        for(String clave:List.of("AQ."+"a".repeat(12)+" b","AQ."+"a".repeat(20)+"\r\nX-Test: valor","corta","a".repeat(201))) {
            assertThrows(IllegalArgumentException.class,()->new GeminiCatalogo(clave,"gemini-3.1-flash-lite"));
        }
    }
    @Test void erroresDeRedYConfiguracionNoSeConviertenEnRecomendaciones() {
        var c=new GeminiCatalogo("clave_de_prueba_123456","modelo-de-prueba",r->{throw new IOException("detalle privado");});
        assertEquals(503,assertThrows(AgendaException.class,()->c.elegir("Caso ficticio de prueba",catalogo)).status());
        assertThrows(IllegalArgumentException.class,()->new GeminiCatalogo("clave_de_prueba_123456","../otro"));
    }
    @Test void reintentaUnaVezSoloLaIndisponibilidadConfirmadaYConservaElPlazo() throws Exception {
        String cuerpo=respuesta("{\"idEspecialidad\":72,\"motivo\":\"CATALOGO\"}");
        var intentos=new java.util.concurrent.atomic.AtomicInteger();
        var cliente=new GeminiCatalogo("clave_de_prueba_123456","modelo-de-prueba",request->{
            assertTrue(request.timeout().orElseThrow().compareTo(java.time.Duration.ofSeconds(20))<=0);
            assertEquals("clave_de_prueba_123456",request.headers().firstValue("x-goog-api-key").orElseThrow());
            return intentos.incrementAndGet()==1?new GeminiCatalogo.Respuesta(503,"detalle privado"):new GeminiCatalogo.Respuesta(200,cuerpo);
        });
        assertEquals(72,cliente.elegir("Caso ficticio de prueba",catalogo).idEspecialidad());assertEquals(2,intentos.get());
    }
    @Test void dosRespuestasOcupadasTerminanSinReintentosIlimitados() {
        var intentos=new java.util.concurrent.atomic.AtomicInteger();
        var cliente=new GeminiCatalogo("clave_de_prueba_123456","modelo-de-prueba",request->{intentos.incrementAndGet();return new GeminiCatalogo.Respuesta(503,"detalle privado");});
        var error=assertThrows(AgendaException.class,()->cliente.elegir("Caso ficticio de prueba",catalogo));
        assertEquals("ORIENTACION_OCUPADA",error.code());assertEquals(2,intentos.get());assertFalse(error.getMessage().contains("detalle privado"));
    }
    @Test void noRepiteUnaGeneracionQueAgotoElTiempoNiUnaCuotaAgotada() {
        var intentos=new java.util.concurrent.atomic.AtomicInteger();
        var lento=new GeminiCatalogo("clave_de_prueba_123456","modelo-de-prueba",request->{intentos.incrementAndGet();throw new java.net.http.HttpTimeoutException("dato privado");});
        var error=assertThrows(AgendaException.class,()->lento.elegir("Caso ficticio de prueba",catalogo));
        assertEquals(504,error.status());assertEquals("ORIENTACION_TIEMPO_AGOTADO",error.code());assertEquals(1,intentos.get());
        intentos.set(0);
        var cuota=new GeminiCatalogo("clave_de_prueba_123456","modelo-de-prueba",request->{intentos.incrementAndGet();return new GeminiCatalogo.Respuesta(429,"dato privado");});
        assertEquals("ORIENTACION_CUOTA_AGOTADA",assertThrows(AgendaException.class,()->cuota.elegir("Caso ficticio de prueba",catalogo)).code());assertEquals(1,intentos.get());
    }
    @Test void jsonMalformadoNoSePresentaComoFalloDeConexion() {
        var cliente=new GeminiCatalogo("clave_de_prueba_123456","modelo-de-prueba",request->new GeminiCatalogo.Respuesta(200,"<html>dato privado</html>"));
        var error=assertThrows(AgendaException.class,()->cliente.elegir("Caso ficticio de prueba",catalogo));
        assertEquals("ORIENTACION_RESPUESTA_INVALIDA",error.code());assertFalse(error.getMessage().contains("dato privado"));
    }
}
