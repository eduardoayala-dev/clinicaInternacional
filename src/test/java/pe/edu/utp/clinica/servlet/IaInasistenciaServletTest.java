package pe.edu.utp.clinica.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;
import static org.junit.jupiter.api.Assertions.*;

class IaInasistenciaServletTest {
    private final ObjectMapper json=new ObjectMapper();
    private HttpExchange request(String rol) {
        var x=new HttpExchange();x.path="/sysadmin/ia/inasistencia";x.headers.put("Accept","application/json");
        x.session.setAttribute("usuarioLogueado",new Usuario(1,"Usuario","Prueba","demo@example.test","",rol,null,true));
        x.parameters.put("inasistenciasPrevias","3");x.parameters.put("atencionesPrevias","2");x.parameters.put("anticipacionDias","30");x.parameters.put("confirmadaPorPaciente","false");return x;
    }
    private IaInasistenciaServlet servlet() {var s=new IaInasistenciaServlet();s.init();return s;}

    @Test void permiteSimularAlAdministradorConContratoYProveniencia() throws Exception {
        var x=request("ADMINISTRADOR");servlet().doPost(x.request,x.response);assertEquals(200,x.status);
        var body=json.readTree(x.body.toString());assertEquals("PREDICCION_EXPERIMENTAL_OBTENIDA",body.get("code").asText());
        assertTrue(body.get("data").get("experimental").asBoolean());assertEquals("SINTETICO",body.get("data").get("origen").asText());
        assertEquals("no-store",x.responseHeaders.get("Cache-Control"));
    }
    @Test void rechazaTodosLosOtrosRolesInclusoConParametrosForjados() {
        for(String rol:new String[]{"PACIENTE","MEDICO","PERSONAL_ADMINISTRATIVO"}) {
            var x=request(rol);x.parameters.put("rol","ADMINISTRADOR");servlet().doPost(x.request,x.response);assertEquals(403,x.status);
        }
        var x=request("ADMINISTRADOR");x.session=null;servlet().doPost(x.request,x.response);assertEquals(403,x.status);
    }
    @Test void rechazaParametrosAmbiguosYFormatosNumericosNoEnteros() {
        var s=servlet();
        for(String nombre:new String[]{"inasistenciasPrevias","atencionesPrevias","anticipacionDias","confirmadaPorPaciente"}) {
            var x=request("ADMINISTRADOR");x.multipleParameters.put(nombre,new String[]{"0","1"});
            s.doPost(x.request,x.response);assertEquals(400,x.status);
        }
        for(String valor:new String[]{"+3"," 3","3.0","3e1","999999999999999999999"}) {
            var x=request("ADMINISTRADOR");x.parameters.put("inasistenciasPrevias",valor);
            s.doPost(x.request,x.response);assertEquals(400,x.status);
        }
    }

    @Test void erroresDeTransporteYVistaNoEscapanDelServlet() {
        var s=servlet();
        var post=request("ADMINISTRADOR");post.writerFailure=new java.io.IOException("Desconexión de prueba");
        assertDoesNotThrow(()->s.doPost(post.request,post.response));assertEquals(500,post.status);
        var get=request("ADMINISTRADOR");get.writerFailure=new java.io.IOException("Desconexión de prueba");
        assertDoesNotThrow(()->s.doGet(get.request,get.response));assertEquals(500,get.status);
        var vista=request("ADMINISTRADOR");vista.headers.clear();vista.forwardFailure=new jakarta.servlet.ServletException("Vista de prueba");
        assertDoesNotThrow(()->s.doGet(vista.request,vista.response));assertEquals(500,vista.status);
    }
    @Test void validaEntradasSinAceptarEstadosDeCitaComoConfirmacion() {
        for(String dato:new String[]{"-1","NaN","2147483648","<script>"}) {
            var x=request("ADMINISTRADOR");x.parameters.put("inasistenciasPrevias",dato);servlet().doPost(x.request,x.response);assertEquals(400,x.status);
        }
        var x=request("ADMINISTRADOR");x.parameters.put("confirmadaPorPaciente","CONFIRMADA");servlet().doPost(x.request,x.response);assertEquals(400,x.status);
        x=request("ADMINISTRADOR");x.parameters.remove("atencionesPrevias");servlet().doPost(x.request,x.response);assertEquals(400,x.status);
    }
    @Test void getSoloConsultaInformacionYNoCalculaPorParametros() throws Exception {
        var x=request("ADMINISTRADOR");x.method="GET";servlet().doGet(x.request,x.response);assertEquals(200,x.status);
        var body=json.readTree(x.body.toString());assertEquals("MODELO_EXPERIMENTAL_OBTENIDO",body.get("code").asText());assertEquals("NO_ASISTIO",body.get("data").get("objetivo").asText());
        assertFalse(body.get("data").has("probabilidad"));
    }
    @Test void modeloNoDisponibleNoInventaUnResultado() throws Exception {
        var x=request("ADMINISTRADOR");new IaInasistenciaServlet().doPost(x.request,x.response);assertEquals(503,x.status);
        assertEquals("MODELO_NO_DISPONIBLE",json.readTree(x.body.toString()).get("code").asText());
    }
}
