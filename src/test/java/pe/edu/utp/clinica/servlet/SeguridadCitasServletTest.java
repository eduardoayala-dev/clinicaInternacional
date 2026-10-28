package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SeguridadCitasServletTest {
    private static class Dao extends CitaDAO {
        @Override public Map<String,Object> obtenerPerfilPortalMedico(int usuario) { return Map.of("idMedico",8,"nombreMedico","Medico de prueba","especialidad","General","sedeNombre","Central"); }
        boolean allowed;
        int owner;
        int attempts;
        int notifications;
        int offers;
        @Override public int obtenerIdPacientePorUsuarioId(int id) { return id == 10 ? 5 : -1; }
        @Override public int obtenerIdMedicoPorUsuarioId(int id) { return id == 20 ? 8 : -1; }
        @Override public boolean actualizarEstadoCitaPaciente(int id, int patient, String state) {
            attempts++; owner = patient; return allowed;
        }
        @Override public String ejecutarReasignacionInteligente(int id) { offers++; return ""; }
        @Override public boolean crearNotificacion(int user, Integer id, String type, String message) {
            notifications++; return true;
        }
        @Override public Map<String,Object> obtenerDetalleCitaMedico(int id, int doctor) {
            owner = doctor; return Map.of();
        }
        @Override public boolean actualizarAtencionMedicaValidada(int id, int doctor, String state,
                String priority, String date, String instructions) {
            attempts++; owner = doctor; return allowed;
        }
    }

    private HttpExchange patient() {
        HttpExchange x = new HttpExchange();
        x.session.setAttribute("usuarioLogueado", new Usuario(10,"P","Uno","p@test","","PACIENTE",null,true));
        x.parameters.put("action","cancelar"); x.parameters.put("idCita","7");
        return x;
    }

    private PacienteHistorialServlet patientServlet(Dao dao) {
        return new PacienteHistorialServlet() { @Override protected CitaDAO crearCitaDAO() { return dao; } };
    }

    @Test void unauthorizedCancellationHasNoOfferOrNotification() throws Exception {
        Dao dao = new Dao(); HttpExchange x = patient();
        x.parameters.put("idPaciente", "999");
        patientServlet(dao).doPost(x.request, x.response);
        assertEquals(5, dao.owner, "El propietario proviene de la sesión, no del formulario");
        assertEquals(404, x.status);
        assertEquals(0, dao.offers); assertEquals(0, dao.notifications);
        assertNull(x.redirect);
    }

    @Test void authorizedCancellationRunsEffectsAfterUpdate() throws Exception {
        Dao dao = new Dao(); dao.allowed = true; HttpExchange x = patient();
        patientServlet(dao).doPost(x.request, x.response);
        assertEquals(1, dao.attempts); assertEquals(1, dao.offers); assertEquals(1, dao.notifications);
        assertEquals("/paciente/historial", x.redirect);
    }

    @Test void missingOrInvalidIdCannotDefaultToAppointmentOne() throws Exception {
        for (String id : new String[]{null,"","abc","0","-1"}) {
            Dao dao = new Dao(); HttpExchange x = patient(); x.parameters.put("idCita", id);
            patientServlet(dao).doPost(x.request, x.response);
            assertEquals(400,x.status); assertEquals(0,dao.attempts);
        }
    }

    @Test void doctorReadUsesDoctorIdentityAndDeniesUnassignedAppointment() throws Exception {
        Dao dao = new Dao(); HttpExchange x = new HttpExchange(); x.method="GET";
        x.session.setAttribute("usuarioLogueado", new Usuario(20,"M","Uno","m@test","","MEDICO",null,true));
        x.parameters.put("idCita","7"); x.parameters.put("idMedico","999");
        MedicoAtencionServlet servlet = new MedicoAtencionServlet() {
            @Override protected CitaDAO crearCitaDAO() { return dao; }
        };
        servlet.init(); servlet.doGet(x.request,x.response);
        assertEquals(8,dao.owner); assertEquals(404,x.status);
    }

    @Test void rejectedMedicalUpdateDoesNotReportSuccess() throws Exception {
        Dao dao = new Dao(); HttpExchange x = new HttpExchange();
        x.session.setAttribute("usuarioLogueado", new Usuario(20,"M","Uno","m@test","","MEDICO",null,true));
        x.parameters.put("idCita","7"); x.parameters.put("accion","guardar"); x.parameters.put("prioridad","MEDIA");
        MedicoAtencionServlet servlet = new MedicoAtencionServlet() {
            @Override protected CitaDAO crearCitaDAO() { return dao; }
        };
        servlet.init(); servlet.doPost(x.request,x.response);
        assertEquals(8,dao.owner); assertEquals(404,x.status); assertNull(x.redirect);
        assertFalse(x.attributes.containsKey("mensajeExito"));
    }
}
