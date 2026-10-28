package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;
import pe.edu.utp.clinica.util.Html;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

class MedicoPortalServletTest {
    private static class Dao extends CitaDAO {
        int activa=-1;
        int medico,escrituras,consultas;LocalDate fecha;AgendaException fallo;
        List<Map<String,Object>> agenda=List.of();
        @Override public int obtenerAtencionActivaMedico(int medico){return activa;}
        @Override public boolean iniciarAtencionMedica(int cita,int medico){escrituras++;this.medico=medico;return true;}
        @Override public boolean registrarNoAtencionMedica(int cita,int medico,String motivo){escrituras++;this.medico=medico;return true;}
        @Override public Map<String,Object> obtenerPerfilPortalMedico(int usuario){
            consultas++;if(fallo!=null) throw fallo;
            assertEquals(20,usuario);return Map.of("idMedico",8,"nombreMedico","Médico de prueba","especialidad","General","sedeNombre","Central");
        }
        @Override public List<Map<String,Object>> obtenerAgendaMedico(int medico,LocalDate fecha){this.medico=medico;this.fecha=fecha;return agenda;}
        @Override public Map<String,Object> obtenerDetalleCitaMedico(int cita,int medico){this.medico=medico;return cita==7?Map.of("idCita",7,"estado","ATENDIDA","indicaciones","Control real"):Map.of();}
        @Override public boolean actualizarAtencionMedicaValidada(int cita,int medico,String estado,String prioridad,String fecha,String notas){
            escrituras++;this.medico=medico;if(cita!=7) throw new AgendaException(404,"CITA_NO_ENCONTRADA","Cita ajena.");return true;
        }
    }
    private HttpExchange request(){
        var x=new HttpExchange();x.method="GET";x.path="/medico/agenda";x.headers.put("Accept","application/json");
        x.session.setAttribute("usuarioLogueado",new Usuario(20,"Médico","Prueba","m@test","","MEDICO",null,true));return x;
    }
    private MedicoAgendaServlet agenda(Dao dao){
        var servlet=new MedicoAgendaServlet(){@Override protected CitaDAO crearCitaDAO(){return dao;}@Override protected Clock reloj(){return Clock.fixed(Instant.parse("2026-10-29T04:30:00Z"),ZoneId.of("America/Lima"));}};
        servlet.init();return servlet;
    }
    private MedicoAtencionServlet atencion(Dao dao){var s=new MedicoAtencionServlet(){@Override protected CitaDAO crearCitaDAO(){return dao;}};s.init();return s;}
    @Test void onlySessionIdentityIsUsedAndClinicalDateIsLima()throws Exception{
        var x=request();var dao=new Dao();x.parameters.put("idMedico","999");agenda(dao).doGet(x.request,x.response);
        assertEquals(200,x.status);assertEquals(8,dao.medico);assertEquals(LocalDate.of(2026,10,28),dao.fecha);
        assertEquals("AGENDA_MEDICA_OBTENIDA",new ObjectMapper().readTree(x.body.toString()).get("code").asText());
    }
    @Test void explicitDateEnablesHistoryAndInvalidDatesAreRejected(){
        var x=request();var dao=new Dao();x.parameters.put("fecha","2026-10-01");agenda(dao).doGet(x.request,x.response);assertEquals(LocalDate.of(2026,10,1),dao.fecha);
        for(String fecha:List.of("2026-02-30","","2026-2-1","texto")){x=request();x.parameters.put("fecha",fecha);agenda(dao).doGet(x.request,x.response);assertEquals(400,x.status);}
    }
    @Test void patientAndMissingSessionCannotQueryMedicalData(){
        var x=request();var dao=new Dao();x.session=null;agenda(dao).doGet(x.request,x.response);assertEquals(403,x.status);
        x=request();x.session.setAttribute("usuarioLogueado",new Usuario(20,"Paciente","Prueba","p@test","","PACIENTE",null,true));agenda(dao).doGet(x.request,x.response);
        assertEquals(403,x.status);assertEquals(0,dao.consultas);
    }
    @Test void disabledMedicalProfileCannotUseStaleSession(){
        var x=request();var dao=new Dao();dao.fallo=new AgendaException(403,"MEDICO_NO_HABILITADO","Perfil inactivo.");agenda(dao).doGet(x.request,x.response);assertEquals(403,x.status);
    }
    @Test void getActionsNeverPerformWritesAndForeignDetailsReturn404(){
        var x=request();var dao=new Dao();x.parameters.putAll(Map.of("accion","guardar","idCita","7"));atencion(dao).doGet(x.request,x.response);
        assertEquals(405,x.status);assertEquals(0,dao.escrituras);
        x=request();x.parameters.put("idCita","999");atencion(dao).doGet(x.request,x.response);assertEquals(404,x.status);
    }
    @Test void viewingCompletedRecordDoesNotSaveOrAlterIt()throws Exception{
        var x=request();var dao=new Dao();x.parameters.put("idCita","7");atencion(dao).doGet(x.request,x.response);
        assertEquals(200,x.status);assertEquals(0,dao.escrituras);
        assertEquals("Control real",new ObjectMapper().readTree(x.body.toString()).get("data").get("indicaciones").asText());
    }
    @Test void writesUseAuthenticatedDoctorAndReturnActualResult()throws Exception{
        var x=request();var dao=new Dao();x.method="POST";x.parameters.putAll(Map.of("accion","guardar","idCita","7","idMedico","999"));
        atencion(dao).doPost(x.request,x.response);assertEquals(8,dao.medico);assertEquals(200,x.status);assertEquals(1,dao.escrituras);
        assertEquals(7,new ObjectMapper().readTree(x.body.toString()).get("data").get("idCita").asInt());
    }
    @Test void startAndNoAttentionUseSessionIdentityAndAbsenceIsForbidden()throws Exception{
        for(String accion:List.of("iniciar","no_atender")){
            var x=request();var dao=new Dao();x.method="POST";x.parameters.putAll(Map.of("accion",accion,"idCita","7","idMedico","999","motivo","Motivo comprobado"));
            atencion(dao).doPost(x.request,x.response);assertEquals(200,x.status);assertEquals(8,dao.medico);assertEquals(1,dao.escrituras);
        }
        var x=request();var dao=new Dao();x.method="POST";x.parameters.putAll(Map.of("accion","no_asistio","idCita","7"));
        atencion(dao).doPost(x.request,x.response);assertEquals(403,x.status);assertEquals(0,dao.escrituras);
    }
    @Test void currentAttentionWithoutIdResumesTheAssignedDoctorsRecord()throws Exception{
        var x=request();var dao=new Dao();dao.activa=7;atencion(dao).doGet(x.request,x.response);
        assertEquals(200,x.status);assertEquals(8,dao.medico);assertEquals(0,dao.escrituras);
    }
    @Test void malformedIdAndInvalidActionNeverReachWrites(){
        for(String id:List.of("0","-1","abc","2147483648")){var x=request();var dao=new Dao();x.method="POST";x.parameters.putAll(Map.of("accion","guardar","idCita",id));atencion(dao).doPost(x.request,x.response);assertEquals(400,x.status);assertEquals(0,dao.escrituras);}
    }
    @Test void dashboardCountsOnlyRealRoomStates()throws Exception{
        var x=request();var dao=new Dao();dao.agenda=List.of(Map.of("estado","CONFIRMADA"),Map.of("estado","CANCELADA"),Map.of("estado","NO_ASISTIO"));
        var s=new MedicoInicioServlet(){@Override protected CitaDAO crearCitaDAO(){return dao;}};s.init();s.doGet(x.request,x.response);
        var data=new ObjectMapper().readTree(x.body.toString()).get("data");
        assertEquals(0,data.get("enEspera").asInt());assertTrue(data.get("pacienteEnSala").isNull());assertEquals(1,data.get("pendientes").asInt());assertEquals(1,data.get("inasistencias").asInt());
    }
    @Test void csvAndPrintOnlyExportOwnAgendaAndEscapeValues(){
        var x=request();var dao=new Dao();dao.agenda=List.of(Map.of("idCita",7,"paciente","<script>alert(1)</script>","dni","=1+1"));
        x.parameters.putAll(Map.of("format","print","idMedico","999"));
        var s=new MedicoExportServlet(){@Override protected CitaDAO crearCitaDAO(){return dao;}};s.init();s.doGet(x.request,x.response);
        assertEquals(8,dao.medico);assertEquals(200,x.status);assertFalse(x.body.toString().contains("<script>"));assertTrue(x.body.toString().contains("&lt;script&gt;"));
        assertEquals("\"'=1+1\"",MedicoExportServlet.csv("=1+1"));assertEquals("\"'\t=1+1\"",MedicoExportServlet.csv("\t=1+1"));
    }
    @Test void htmlOutputEscapesSpecialCharactersWithoutStrippingLegitimateText(){
        assertEquals("Niño &amp; &lt;control&gt; &#39;nota&#39; &quot;dato&quot;",Html.texto("Niño & <control> 'nota' \"dato\""));
    }
    @Test void htmlAgendaForwardsToItsViewAndReadOnlyPostReturns405(){
        var x=request();var dao=new Dao();x.headers.clear();agenda(dao).doGet(x.request,x.response);assertEquals("/WEB-INF/views/medico/agenda.jsp",x.forward);
        x=request();x.method="POST";agenda(dao).doPost(x.request,x.response);assertEquals(405,x.status);assertEquals("GET",x.responseHeaders.get("Allow"));
    }
}
