package pe.edu.utp.clinica.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.dao.ListaEsperaDAO;
import pe.edu.utp.clinica.dao.OfertasDAO;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ListaEsperaPortalTest {
    private static class Citas extends CitaDAO {
        @Override public int obtenerIdPacientePorUsuarioId(int usuario) { return 5; }
        @Override public List<Map<String,Object>> obtenerNotificacionesUsuario(int usuario) { return List.of(); }
        @Override public int contarNotificacionesSinLeer(int usuario) { return 0; }
        @Override public boolean marcarNotificacionIndividualLeida(int id,int usuario) { return id==20 && usuario==10; }
    }
    private static class Lista extends ListaEsperaDAO {
        int llamadas, paciente, aviso;
        Integer sede, medico;
        String canales, urgencia;
        @Override public int registrar(int paciente,int especialidad,Integer sede,String horario,String urgencia,String canales,int aviso,Integer medico) {
            llamadas++; this.paciente=paciente; this.sede=sede; this.medico=medico;
            this.canales=canales; this.urgencia=urgencia; this.aviso=aviso; return 35;
        }
    }
    private static class Ofertas extends OfertasDAO {
        int llamadas, paciente;
        AgendaException error;
        @Override public int aceptar(int paciente,int oferta) {
            llamadas++; this.paciente=paciente;
            if(error!=null) throw error;
            return 72;
        }
        @Override public Map<String,Object> obtenerParaPaciente(int paciente) { return null; }
        @Override public boolean tieneOfertaVencida(int paciente) { return false; }
    }
    private HttpExchange request() {
        var x=new HttpExchange(); x.headers.put("Accept","application/json");
        x.session.setAttribute("usuarioLogueado",new Usuario(10,"Paciente","Prueba","p@test","","PACIENTE",null,true));
        x.parameters.put("idPaciente","999"); return x;
    }
    private PacienteListaEsperaServlet lista(Lista dao) {
        var servlet=new PacienteListaEsperaServlet() {
            @Override protected CitaDAO crearCitaDAO() { return new Citas(); }
            @Override protected ListaEsperaDAO crearListaDAO() { return dao; }
        };
        servlet.init(); return servlet;
    }
    private PacienteNotificacionesServlet notificaciones(Ofertas dao) {
        var servlet=new PacienteNotificacionesServlet() {
            @Override protected CitaDAO crearCitaDAO() { return new Citas(); }
            @Override protected OfertasDAO crearOfertasDAO() { return dao; }
        };
        servlet.init(); return servlet;
    }
    @Test void registrationUsesSessionIdentityAndDoesNotTurnShortNoticeIntoClinicalUrgency() throws Exception {
        var x=request(); var dao=new Lista();
        x.parameters.putAll(Map.of("especialidadId","1","sede","cualquiera","medicoId","4","disponibilidad","3h","urgencia","ALTA"));
        x.multipleParameters.put("horarios",new String[]{"manana","tarde"});
        x.multipleParameters.put("canales",new String[]{"email","whatsapp"});
        lista(dao).doPost(x.request,x.response);
        assertEquals(201,x.status); assertEquals(5,dao.paciente); assertNull(dao.sede);
        assertEquals(4,dao.medico); assertEquals(180,dao.aviso); assertEquals("MEDIA",dao.urgencia);
        assertEquals("CORREO,WHATSAPP",dao.canales);
        var body=new ObjectMapper().readTree(x.body.toString());
        assertEquals("INSCRIPCION_REGISTRADA",body.get("code").asText());
        assertEquals(35,body.get("data").get("idListaEspera").asInt());
    }
    @Test void invalidPreferencesAndMissingSessionNeverReachRegistration() {
        var x=request(); var dao=new Lista(); lista(dao).doPost(x.request,x.response);
        assertEquals(400,x.status); assertEquals(0,dao.llamadas);
        x=request(); x.session.removeAttribute("usuarioLogueado"); lista(dao).doPost(x.request,x.response);
        assertEquals(403,x.status); assertEquals(0,dao.llamadas);
    }
    @Test void acceptingRequiresTheRealOfferIdAndReturnsActualAppointment() throws Exception {
        var x=request(); var dao=new Ofertas();
        x.parameters.putAll(Map.of("accion","aceptar","idOferta","11","idListaEspera","999"));
        notificaciones(dao).doPost(x.request,x.response);
        assertEquals(200,x.status); assertEquals(5,dao.paciente);
        var body=new ObjectMapper().readTree(x.body.toString());
        assertEquals("OFERTA_ACEPTADA",body.get("code").asText());
        assertEquals(72,body.get("data").get("idCita").asInt());
    }
    @Test void foreignAndExpiredOffersKeepTheirErrorStatusAndHaveNoSuccessData() throws Exception {
        for(int status:List.of(404,410)) {
            var x=request(); var dao=new Ofertas();
            x.parameters.putAll(Map.of("accion","aceptar","idOferta","11"));
            dao.error=new AgendaException(status,"OFERTA_INVALIDA","Oferta no disponible.");
            notificaciones(dao).doPost(x.request,x.response);
            assertEquals(status,x.status);
            assertTrue(new ObjectMapper().readTree(x.body.toString()).get("data").isNull());
        }
    }
    @Test void malformedOfferAndGetMutationsAreRejectedBeforeWriting() {
        var x=request(); var dao=new Ofertas();
        x.parameters.putAll(Map.of("accion","aceptar","idOferta","-1"));
        notificaciones(dao).doPost(x.request,x.response);
        assertEquals(400,x.status); assertEquals(0,dao.llamadas);
        x.method="GET"; notificaciones(dao).doGet(x.request,x.response);
        assertEquals(405,x.status); assertEquals("POST",x.responseHeaders.get("Allow")); assertEquals(0,dao.llamadas);
    }
    @Test void queryParametersCannotInventAnOfferOrExpiry() throws Exception {
        var x=request(); x.method="GET"; x.parameters.putAll(Map.of("simular","true","estado","expirada"));
        notificaciones(new Ofertas()).doGet(x.request,x.response);
        assertEquals(200,x.status);
        var data=new ObjectMapper().readTree(x.body.toString()).get("data");
        assertTrue(data.get("oferta").isNull()); assertFalse(data.get("ofertaVencida").asBoolean());
    }
    @Test void foreignNotificationCannotBeMarkedAsRead() {
        var x=request(); x.parameters.putAll(Map.of("action","marcarLeida","id","999"));
        notificaciones(new Ofertas()).doPost(x.request,x.response);
        assertEquals(404,x.status);
        x=request(); x.parameters.putAll(Map.of("action","marcarLeida","id","20"));
        notificaciones(new Ofertas()).doPost(x.request,x.response);
        assertEquals(200,x.status);
    }
}
