package pe.edu.utp.clinica.ia;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.AgendaException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OrientacionCatalogoTest {
    private final List<OrientacionCatalogo.Especialidad> catalogo=List.of(new OrientacionCatalogo.Especialidad(72,"Dermatología"),new OrientacionCatalogo.Especialidad(83,"Medicina General"));
    @Test void demoUsaElIdentificadorRealSinInventarIA() {
        var r=new OrientacionCatalogo(null,false).orientar("Caso ficticio: picazón y manchas en la piel",true,catalogo);
        assertEquals(72,r.idEspecialidad());assertEquals("EJEMPLO_LOCAL",r.proveedor());assertTrue(r.demostracion());
    }
    @Test void textoLibreSinProveedorNoSeClasificaConReglasOcultas() {
        var servicio=new OrientacionCatalogo(null,false);
        var error=assertThrows(AgendaException.class,()->servicio.orientar("Un relato libre distinto de los ejemplos",true,catalogo));assertEquals(503,error.status());
    }
    @Test void validaDatosAntesDeConsultarAlProveedor() {
        var s=new OrientacionCatalogo((relato,lista)->{fail("No debe enviar información");return null;},true);
        assertThrows(AgendaException.class,()->s.orientar("Texto largo ficticio",false,catalogo));
        assertThrows(AgendaException.class,()->s.orientar("corto",true,catalogo));
        String largo="x".repeat(1201);
        var vacio=List.<OrientacionCatalogo.Especialidad>of();
        assertThrows(AgendaException.class,()->s.orientar(largo,true,catalogo));
        assertThrows(AgendaException.class,()->s.orientar("Caso ficticio válido",true,vacio));
    }
    @Test void rechazaIdentificadorAjenoYCualquierTextoDiagnosticoAdicional() {
        var ajeno=new OrientacionCatalogo((r,c)->new OrientacionCatalogo.Eleccion(999,"CATALOGO"),true);
        assertThrows(AgendaException.class,()->ajeno.orientar("Caso ficticio de prueba",true,catalogo));
        var texto=new OrientacionCatalogo((r,c)->new OrientacionCatalogo.Eleccion(72,"Toma un medicamento"),true);
        assertThrows(AgendaException.class,()->texto.orientar("Caso ficticio de prueba",true,catalogo));
    }
    @Test void aclaracionNoProduceEspecialidadNiPrioridad() {
        var s=new OrientacionCatalogo((r,c)->new OrientacionCatalogo.Eleccion(0,"ACLARAR"),true);
        var r=s.orientar("Caso ficticio sin detalles",true,catalogo);
        assertNull(r.idEspecialidad());assertEquals("ACLARAR",r.tipo());
    }
    @Test void recomendacionSeResuelveEnCatalogoYNoExponeElRelato() {
        var s=new OrientacionCatalogo((r,c)->new OrientacionCatalogo.Eleccion(83,"CATALOGO"),true);
        var r=s.orientar("Caso ficticio de prueba irrepetible",true,catalogo);
        assertEquals("Medicina General",r.especialidad());assertEquals("GEMINI",r.proveedor());assertFalse(r.toString().contains("irrepetible"));
    }
    @Test void rodillaReconoceElNombreCompletoDelCatalogoSinDependerDelId() {
        var s=new OrientacionCatalogo(null,false);
        var r=s.orientar("Caso ficticio: molestia articular en la rodilla",true,List.of(new OrientacionCatalogo.Especialidad(95,"Traumatología y Ortopedia")));
        assertEquals(95,r.idEspecialidad());assertEquals("EJEMPLO_LOCAL",r.proveedor());
    }
    @Test void ejemploSinEspecialidadODuplicadoNoInformaFalsamenteUnFalloDeGemini() {
        var s=new OrientacionCatalogo(null,false);
        var e=assertThrows(AgendaException.class,()->s.orientar("Caso ficticio: molestia articular en la rodilla",true,catalogo));
        assertEquals("EJEMPLO_SIN_ESPECIALIDAD",e.code());assertFalse(e.getMessage().contains("Gemini"));
        var ambiguo=List.of(new OrientacionCatalogo.Especialidad(5,"Traumatología"),new OrientacionCatalogo.Especialidad(6,"Traumatología y Ortopedia"));
        assertThrows(AgendaException.class,()->s.orientar("Caso ficticio: molestia articular en la rodilla",true,ambiguo));
    }
}
