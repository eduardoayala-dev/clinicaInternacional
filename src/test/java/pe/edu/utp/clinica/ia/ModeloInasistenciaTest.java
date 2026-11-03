package pe.edu.utp.clinica.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ModeloInasistenciaTest {
    private final ObjectMapper json=new ObjectMapper();
    private ObjectNode artefacto() throws IOException {
        try(var input=getClass().getResourceAsStream("/ia/riesgo-inasistencia-demo.json")) { return (ObjectNode)json.readTree(input); }
    }
    private ModeloInasistencia cargar(ObjectNode node) throws IOException {return ModeloInasistencia.cargar(new ByteArrayInputStream(json.writeValueAsBytes(node)));}
    private ModeloInasistencia cargarJson(String contenido) throws IOException {
        return ModeloInasistencia.cargar(new ByteArrayInputStream(contenido.getBytes(StandardCharsets.UTF_8)));
    }

    @Test void rechazaCamposAusentesNulosYConversionesSilenciosas() throws IOException {
        for (String campo : new String[]{"esquema","version","objetivo","origen","experimental","intercepto","umbralMedio","umbralAlto","variables"}) {
            var ausente=artefacto();ausente.remove(campo);
            assertThrows(IOException.class,()->cargar(ausente));
            var nulo=artefacto();nulo.putNull(campo);
            assertThrows(IOException.class,()->cargar(nulo));
        }
        var version=artefacto();version.put("esquema",1.7);assertThrows(IOException.class,()->cargar(version));
        var numero=artefacto();numero.put("intercepto","-0.9");assertThrows(IOException.class,()->cargar(numero));
        var booleano=artefacto();booleano.put("experimental","true");assertThrows(IOException.class,()->cargar(booleano));
        var variableNula=artefacto();((com.fasterxml.jackson.databind.node.ArrayNode)variableNula.get("variables")).set(0,json.nullNode());
        assertThrows(IOException.class,()->cargar(variableNula));
    }

    @Test void rechazaJsonDuplicadoExtraDesconocidoYDesmesurado() throws IOException {
        String valido=json.writeValueAsString(artefacto());
        String duplicado=valido.replace("\"esquema\":1","\"esquema\":1,\"esquema\":1");
        String extra=valido+" {}";
        String grande=" ".repeat(65537)+valido;
        String profundo="[".repeat(20)+"0"+"]".repeat(20);
        assertThrows(IOException.class,()->cargarJson(duplicado));
        assertThrows(IOException.class,()->cargarJson(extra));
        assertThrows(IOException.class,()->cargarJson(grande));
        assertThrows(IOException.class,()->cargarJson(profundo));
        var desconocido=artefacto();desconocido.put("prioridadClinica","ALTA");
        assertThrows(IOException.class,()->cargar(desconocido));
    }

    @Test void extremosNumericosYUmbralInclusivoNoProducenNaN() throws IOException {
        for(double intercepto:new double[]{-10000,0,10000}) {
            var a=artefacto();a.put("intercepto",intercepto);
            a.get("variables").forEach(v->((ObjectNode)v).put("coeficiente",0));
            var p=cargar(a).predecir(new EntradaInasistencia(0,0,0,false));
            assertTrue(Double.isFinite(p.probabilidad()));
            assertEquals(intercepto==0?0.5:intercepto<0?0:1,p.probabilidad());
            assertEquals(intercepto<0?"BAJO":"ALTO",p.nivel());
        }
    }

    @Test void javaReproduceLasProbabilidadesExportadasDeSklearn() throws Exception {
        var modelo=ModeloInasistencia.demostracion();
        try(var input=getClass().getResourceAsStream("/ia/paridad-inasistencia.json")) {
            var ejemplos=json.readTree(input);assertEquals(5,ejemplos.size());
            for(var ejemplo:ejemplos) {
                var x=ejemplo.get("entrada");
                var p=modelo.predecir(new EntradaInasistencia(x.get(0).asInt(),x.get(1).asInt(),x.get(2).asInt(),x.get(3).asInt()==1));
                assertEquals(ejemplo.get("probabilidad").asDouble(),p.probabilidad(),1e-12);
                assertTrue(p.experimental());assertEquals("SINTETICO",p.origen());
            }
        }
    }

    @Test void explicaLaPrediccionConContribucionesQueReconstruyenElLogit() throws Exception {
        var modelo=ModeloInasistencia.demostracion();var p=modelo.predecir(new EntradaInasistencia(3,2,30,false));
        double logit=modelo.informacion().intercepto()+p.factores().stream().mapToDouble(PrediccionInasistencia.Factor::contribucionLogit).sum();
        assertEquals(p.probabilidad(),1/(1+Math.exp(-logit)),1e-12);
        assertEquals(4,p.factores().size());
        for(int i=1;i<p.factores().size();i++) assertTrue(Math.abs(p.factores().get(i-1).contribucionLogit())>=Math.abs(p.factores().get(i).contribucionLogit()));
        assertTrue(p.advertencias().stream().anyMatch(a->a.contains("no causas")));
    }

    @Test void confirmacionExpresaTieneElEfectoAprendidoYNoCambiaElHistorial() throws Exception {
        var modelo=ModeloInasistencia.demostracion();
        var sin=modelo.predecir(new EntradaInasistencia(3,2,30,false));var con=modelo.predecir(new EntradaInasistencia(3,2,30,true));
        assertTrue(con.probabilidad()<sin.probabilidad());
        assertTrue(sin.accionSugerida().contains("No cancelar"));
        assertTrue(sin.factores().stream().anyMatch(f->f.descripcion().equals("Sin confirmación expresa del paciente") && f.valor()==0));
    }

    @Test void identificaAusenciaDeHistorialYExtrapolacionSinOcultarlas() throws Exception {
        var modelo=ModeloInasistencia.demostracion();
        assertTrue(modelo.predecir(new EntradaInasistencia(0,0,7,true)).advertencias().stream().anyMatch(a->a.contains("Sin historial")));
        var extrema=modelo.predecir(new EntradaInasistencia(100,100,180,false));
        assertTrue(Double.isFinite(extrema.probabilidad()));assertTrue(extrema.probabilidad()>=0 && extrema.probabilidad()<=1);
        assertTrue(extrema.advertencias().stream().anyMatch(a->a.contains("fuera del rango")));
        assertThrows(IllegalArgumentException.class,()->new EntradaInasistencia(-1,0,0,false));
        assertThrows(IllegalArgumentException.class,()->new EntradaInasistencia(0,101,0,false));
        assertThrows(IllegalArgumentException.class,()->new EntradaInasistencia(0,0,181,false));
    }

    @Test void rechazaEscalaCeroYVariablesReordenadas() throws Exception {
        var escala=artefacto();((ObjectNode)escala.get("variables").get(0)).put("escala",0);
        assertThrows(IOException.class,()->cargar(escala));
        var orden=artefacto();((ObjectNode)orden.get("variables").get(0)).put("nombre","atenciones_previas");
        assertThrows(IOException.class,()->cargar(orden));
    }

    @Test void noPermitePresentarElModeloSinteticoComoValidado() throws Exception {
        var experimental=artefacto();experimental.put("experimental",false);assertThrows(IOException.class,()->cargar(experimental));
        var real=artefacto();real.put("origen","REAL");assertThrows(IOException.class,()->cargar(real));
        assertThrows(IOException.class,()->cargarJson("{}"));
    }

    @Test void metadatosYExplicacionesSonInmutables() throws Exception {
        var modelo=ModeloInasistencia.demostracion();var p=modelo.predecir(new EntradaInasistencia(0,5,7,true));
        var variables=modelo.informacion().variables();var factores=p.factores();var advertencias=p.advertencias();
        assertThrows(UnsupportedOperationException.class,variables::clear);
        assertThrows(UnsupportedOperationException.class,factores::clear);
        assertThrows(UnsupportedOperationException.class,advertencias::clear);
    }
}
