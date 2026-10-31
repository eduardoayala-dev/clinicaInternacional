package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.ia.OrientacionCatalogo;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PacienteOrientacionServletTest {
    private PacienteOrientacionServlet servlet() {
        var s=new PacienteOrientacionServlet(){
            @Override protected OrientacionCatalogo crearServicio(){return new OrientacionCatalogo(null,false);}
            @Override protected List<OrientacionCatalogo.Especialidad> catalogo(){return List.of(new OrientacionCatalogo.Especialidad(72,"Dermatología"));}
        };s.init();return s;
    }
    private HttpExchange request(String rol) {
        var x=new HttpExchange();x.path="/paciente/orientacion";
        x.session.setAttribute("usuarioLogueado",new Usuario(2,"Paciente","Prueba","demo@example.test","",rol,null,true));
        x.parameters.put("relato","Caso ficticio: picazón y manchas en la piel");x.parameters.put("casoFicticio","true");return x;
    }
    @Test void respondeConCatalogoRealYContratoSinPrioridad() throws Exception {
        var x=request("PACIENTE");servlet().doPost(x.request,x.response);assertEquals(200,x.status);
        var body=new com.fasterxml.jackson.databind.ObjectMapper().readTree(x.body.toString());
        assertEquals(72,body.get("data").get("idEspecialidad").asInt());assertFalse(body.get("data").has("prioridad"));assertEquals("no-store",x.responseHeaders.get("Cache-Control"));
    }
    @Test void otroRolOIdentidadAusenteNoConsultaDatos() throws Exception {
        for(String rol:List.of("MEDICO","ADMINISTRADOR","PERSONAL_ADMINISTRATIVO")) {var x=request(rol);servlet().doPost(x.request,x.response);assertEquals(403,x.status);}
        var x=request("PACIENTE");x.session=null;servlet().doPost(x.request,x.response);assertEquals(403,x.status);
    }
    @Test void declaracionFicticiaEsObligatoriaYTextoLibreNoInventaResultados() throws Exception {
        var x=request("PACIENTE");x.parameters.remove("casoFicticio");servlet().doPost(x.request,x.response);assertEquals(400,x.status);
        x=request("PACIENTE");x.parameters.put("relato","Otro caso de texto libre no predefinido");servlet().doPost(x.request,x.response);assertEquals(503,x.status);
    }
    @Test void getRechazadoMantieneElContratoJson() throws Exception {
        var x=request("PACIENTE");x.method="GET";servlet().doGet(x.request,x.response);
        assertEquals(405,x.status);assertEquals("POST",x.responseHeaders.get("Allow"));
        assertTrue(x.body.toString().contains("METODO_NO_PERMITIDO"));
    }
    @Test void desconexionDelClienteNoPropagaUnaExcepcionDelServlet() {
        var post=request("PACIENTE");post.writerFailure=new java.io.IOException("Desconexión de prueba");
        var s=servlet();assertDoesNotThrow(()->s.doPost(post.request,post.response));assertEquals(500,post.status);
        var get=request("PACIENTE");get.writerFailure=new java.io.IOException("Desconexión de prueba");
        assertDoesNotThrow(()->s.doGet(get.request,get.response));assertEquals(500,get.status);
    }
}
