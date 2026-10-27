package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Proxy;
import static org.junit.jupiter.api.Assertions.*;

class ReservarCitaServletTest {
    private static class Dao extends CitaDAO {
        int propietario;
        int llamadas;
        AgendaException error;
        @Override public int obtenerIdPacientePorUsuarioId(int id) { return 5; }
        @Override public int reservarCitaValidada(int paciente,int medico,String fecha,String hora,String urgencia) {
            llamadas++; propietario=paciente;
            if(error!=null) throw error;
            return 72;
        }
    }
    private HttpExchange request() {
        var x=new HttpExchange();
        x.session.setAttribute("usuarioLogueado",new Usuario(10,"Paciente","Prueba","p@test","","PACIENTE",null,true));
        x.parameters.put("idMedico","1");
        x.parameters.put("fecha","2026-12-01");
        x.parameters.put("hora","09:00");
        x.parameters.put("idPaciente","999");
        x.headers.put("Accept","application/json");
        return x;
    }
    private void enviar(Dao dao,HttpExchange x) throws Exception {
        var servlet=new ReservarCitaServlet(){ @Override protected CitaDAO crearCitaDAO(){return dao;} };
        servlet.init(); servlet.doPost(x.request,x.response);
    }
    @Test void createdContractUsesAuthenticatedPatientAndActualAppointmentId() throws Exception {
        var x=request(); var dao=new Dao(); enviar(dao,x);
        assertEquals(201,x.status); assertEquals(5,dao.propietario);
        var body=new ObjectMapper().readTree(x.body.toString());
        assertEquals("CITA_RESERVADA",body.get("code").asText());
        assertEquals(72,body.get("data").get("idCita").asInt());
        assertTrue(body.get("data").get("codigoCita").asText().endsWith("000072"));
    }
    @Test void conflictHasExplicitHttpStatusAndNoSuccessData() throws Exception {
        var x=request(); var dao=new Dao(); dao.error=new AgendaException(409,"HORARIO_NO_DISPONIBLE","Selecciona otro horario.");
        enviar(dao,x); assertEquals(409,x.status);
        var body=new ObjectMapper().readTree(x.body.toString());
        assertTrue(body.get("data").isNull()); assertEquals("HORARIO_NO_DISPONIBLE",body.get("code").asText());
    }
    @Test void invalidDoctorAndUnauthenticatedRequestsNeverReachReservation() throws Exception {
        var x=request(); var dao=new Dao(); x.parameters.put("idMedico","otro");
        enviar(dao,x); assertEquals(400,x.status); assertEquals(0,dao.llamadas);
        x=request(); x.session.removeAttribute("usuarioLogueado");
        enviar(dao,x); assertEquals(403,x.status); assertEquals(0,dao.llamadas);
    }

    @Test void disconnectedClientDoesNotPropagateResponseExceptions() throws Exception {
        var x=request(); var dao=new Dao();
        var servlet=new ReservarCitaServlet(){ @Override protected CitaDAO crearCitaDAO(){return dao;} };
        servlet.init();
        HttpServletResponse disconnected=(HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),new Class<?>[]{HttpServletResponse.class},
                (proxy,method,args)->{
                    if("getWriter".equals(method.getName())) throw new IOException("cliente desconectado");
                    return method.invoke(x.response,args);
                });
        assertDoesNotThrow(()->servlet.doPost(x.request,disconnected));
        assertEquals(500,x.status);
        assertEquals(1,dao.llamadas);
        assertEquals("",x.body.toString());
    }

    @Test void failedReprogrammingRedirectDoesNotPropagateTransportExceptions() {
        var x=new HttpExchange();
        HttpServletResponse disconnected=(HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),new Class<?>[]{HttpServletResponse.class},
                (proxy,method,args)->{
                    if("sendRedirect".equals(method.getName())) throw new IOException("cliente desconectado");
                    return method.invoke(x.response,args);
                });
        assertDoesNotThrow(()->new PacienteReprogramarServlet().doPost(x.request,disconnected));
        assertEquals(500,x.status);
    }
}
