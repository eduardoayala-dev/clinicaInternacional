package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.dao.HistorialMedicoRepository;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.model.medico.*;
import pe.edu.utp.clinica.support.HttpExchange;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MedicoHistorialServletTest {
    private static class Repository implements HistorialMedicoRepository {
        int medico,lecturas,escrituras;
        @Override public Pagina<PacienteResumen> pacientes(int medico,String q,int pagina){this.medico=medico;lecturas++;return new Pagina<>(List.of(),pagina,20,false);}
        @Override public HistorialPaciente historial(int medico,int id,int pagina){throw new AgendaException(404,"REGISTRO_NO_ENCONTRADO","No encontrado.");}
        @Override public Pagina<SeguimientoResumen> seguimientos(int medico,String q,String estado,int pagina){this.medico=medico;lecturas++;return new Pagina<>(List.of(),pagina,20,false);}
        @Override public SeguimientoResumen seguimiento(int medico,int id){this.medico=medico;return new SeguimientoResumen(id,1,"Paciente","12345678",LocalDate.of(2026,11,1),"MEDIA","PENDIENTE",null,null);}
        @Override public List<CitaControl> controles(int medico,int origen){this.medico=medico;return List.of();}
        @Override public void vincular(int medico,int origen,int control){this.medico=medico;escrituras++;}
    }
    private HttpExchange request(){
        var x=new HttpExchange();x.method="GET";x.headers.put("Accept","application/json");
        x.session.setAttribute("usuarioLogueado",new Usuario(20,"Medico","Prueba","m@test","","MEDICO",null,true));return x;
    }
    private void configurar(MedicoHistorialServlet servlet,Repository repo){
        servlet.historial=repo;
        servlet.citas=new CitaDAO(){@Override public Map<String,Object> obtenerPerfilPortalMedico(int usuario){assertEquals(20,usuario);return Map.of("idMedico",8,"nombreMedico","Médico","especialidad","General","sedeNombre","Central");}};
    }
    @Test void typedPatientResponseUsesSessionDoctorAndHasBoundedPageMetadata()throws Exception{
        var x=request();x.parameters.put("idMedico","999");var repo=new Repository();var servlet=new MedicoPacientesServlet();configurar(servlet,repo);
        servlet.doGet(x.request,x.response);assertEquals(200,x.status);assertEquals(8,repo.medico);
        var data=new ObjectMapper().readTree(x.body.toString()).get("data").get("pacientes");
        assertTrue(data.get("items").isArray());assertEquals(20,data.get("tamano").asInt());assertFalse(data.get("tieneSiguiente").asBoolean());
    }
    @Test void foreignHistoryAndMalformedPatientAreRejected(){
        var repo=new Repository();var servlet=new MedicoPacientesServlet();configurar(servlet,repo);
        var x=request();x.parameters.put("idPaciente","9");servlet.doGet(x.request,x.response);assertEquals(404,x.status);
        x=request();x.parameters.put("idPaciente","1 OR 1=1");servlet.doGet(x.request,x.response);assertEquals(400,x.status);
    }
    @Test void guestAndPatientCannotReadMedicalLists(){
        var repo=new Repository();var servlet=new MedicoPacientesServlet();configurar(servlet,repo);
        var x=request();x.session=null;servlet.doGet(x.request,x.response);assertEquals(403,x.status);
        x=request();x.session.setAttribute("usuarioLogueado",new Usuario(20,"Paciente","Prueba","p@test","","PACIENTE",null,true));servlet.doGet(x.request,x.response);assertEquals(403,x.status);assertEquals(0,repo.lecturas);
    }
    @Test void linksRequirePostAndUseAuthenticatedIdentity(){
        var repo=new Repository();var servlet=new MedicoSeguimientosServlet();configurar(servlet,repo);
        var x=request();x.parameters.put("accion","vincular");servlet.doGet(x.request,x.response);assertEquals(405,x.status);assertEquals(0,repo.escrituras);
        x=request();x.method="POST";x.parameters.putAll(Map.of("accion","vincular","idCitaOrigen","7","idCitaControl","8","idMedico","999"));servlet.doPost(x.request,x.response);assertEquals(200,x.status);assertEquals(8,repo.medico);assertEquals(1,repo.escrituras);
        x=request();x.method="POST";x.parameters.putAll(Map.of("accion","vincular","idCitaOrigen","7","idCitaControl","-1"));servlet.doPost(x.request,x.response);assertEquals(400,x.status);assertEquals(1,repo.escrituras);
    }
    @Test void htmlViewsUseTheirOwnTemplatesAndOnlyGetOnPatients(){
        var repo=new Repository();var servlet=new MedicoPacientesServlet();configurar(servlet,repo);
        var x=request();x.headers.clear();servlet.doGet(x.request,x.response);assertEquals("/WEB-INF/views/medico/pacientes.jsp",x.forward);
        x=request();x.method="POST";servlet.doPost(x.request,x.response);assertEquals(405,x.status);
    }
}
