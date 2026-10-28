package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.AdmisionCitaDAO;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AdmisionCitaServletTest {
    private static class Dao extends AdmisionCitaDAO {
        int usuario,cita,escrituras; AgendaException fallo;
        @Override public Inasistencia registrarInasistencia(int usuario,int cita,String motivo){
            escrituras++;this.usuario=usuario;this.cita=cita;return new Inasistencia(cita,"2026-10-28T10:00:00",motivo);
        }
        @Override public Llegada registrar(int usuario,int cita) {
            escrituras++;this.usuario=usuario;this.cita=cita;
            if(fallo!=null) throw fallo;
            return new Llegada(cita,"2026-10-28T09:05:00",false,10);
        }
    }
    private AdmisionCitaServlet servlet(Dao dao) {
        var s=new AdmisionCitaServlet(){@Override protected AdmisionCitaDAO crearDAO(){return dao;}};
        s.init();return s;
    }
    private HttpExchange request() {
        var x=new HttpExchange();x.path="/admin/admision";
        x.headers.put("Accept","application/json");x.parameters.put("idCita","7");
        x.session.setAttribute("usuarioLogueado",new Usuario(40,"Admisión","Prueba","a@test","","PERSONAL_ADMINISTRATIVO",null,true));
        return x;
    }
    @Test void soloUsaIdentidadDeSesionYHoraDelServidor() throws Exception {
        var dao=new Dao();var x=request();x.parameters.put("idUsuario","999");x.parameters.put("fechaLlegada","2000-01-01T00:00");
        servlet(dao).doPost(x.request,x.response);
        assertEquals(200,x.status);assertEquals(40,dao.usuario);assertEquals(7,dao.cita);
        assertTrue(x.body.toString().contains("LLEGADA_REGISTRADA"));assertTrue(x.body.toString().contains("2026-10-28T09:05:00"));
    }
    @Test void ausenciaUsaResponsableDeSesionYContratoPropio()throws Exception{
        var dao=new Dao();var x=request();x.parameters.put("accion","no_asistio");x.parameters.put("motivo","Ausencia verificada");x.parameters.put("idUsuario","999");
        servlet(dao).doPost(x.request,x.response);assertEquals(200,x.status);assertEquals(40,dao.usuario);assertTrue(x.body.toString().contains("INASISTENCIA_REGISTRADA"));
    }
    @Test void sinSesionOPacienteNoEjecutaDao() throws Exception {
        var dao=new Dao();var x=request();x.session=null;servlet(dao).doPost(x.request,x.response);assertEquals(403,x.status);
        x=request();x.session.setAttribute("usuarioLogueado",new Usuario(10,"Paciente","Prueba","p@test","","PACIENTE",null,true));
        servlet(dao).doPost(x.request,x.response);assertEquals(403,x.status);assertEquals(0,dao.escrituras);
    }
    @Test void getNoRegistraLlegadas() throws Exception {
        var dao=new Dao();var x=request();servlet(dao).doGet(x.request,x.response);
        assertEquals(405,x.status);assertEquals("POST",x.responseHeaders.get("Allow"));assertEquals(0,dao.escrituras);
    }
    @Test void identificadoresMalformadosNoLleganAlDao() throws Exception {
        var dao=new Dao();
        for(String id:List.of("0","-1","1 OR 1=1","2147483648","7.0","")) {
            var x=request();x.parameters.put("idCita",id);servlet(dao).doPost(x.request,x.response);assertEquals(400,x.status);
        }
        assertEquals(0,dao.escrituras);
    }
    @Test void fallosNoDevuelvenExitoNiRedirigen() throws Exception {
        for(int status:List.of(403,404,409,503)) {
            var dao=new Dao();dao.fallo=new AgendaException(status,"PRUEBA_ERROR","No se registró.");var x=request();
            servlet(dao).doPost(x.request,x.response);assertEquals(status,x.status);assertNull(x.redirect);assertNull(x.session.getAttribute("flashMessage"));
        }
    }
    @Test void formularioRedirigeSoloDespuesDeGuardar() throws Exception {
        var dao=new Dao();var x=request();x.headers.clear();servlet(dao).doPost(x.request,x.response);
        assertEquals(302,x.status);assertEquals("/admin/dashboard",x.redirect);assertNotNull(x.session.getAttribute("flashMessage"));
    }
    @Test void rutaAnteriorDeAdmisionUsaElNuevoControlador() throws Exception {
        var x=request();x.parameters.put("action","admitir_cita");
        new AdminDashboardServlet().doPost(x.request,x.response);assertEquals("/admin/admision",x.forward);
    }
}
