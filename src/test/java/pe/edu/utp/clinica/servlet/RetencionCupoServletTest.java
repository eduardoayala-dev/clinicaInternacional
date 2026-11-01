package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.AgendaCitaDAO;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.support.HttpExchange;
import static org.junit.jupiter.api.Assertions.*;

class RetencionCupoServletTest {
    @Test void soloElPacienteAutenticadoPuedeRetener() throws Exception {
        var x=new HttpExchange();var servlet=new RetencionCupoServlet();
        servlet.doPost(x.request,x.response);assertEquals(403,x.status);
        x=new HttpExchange();x.session.setAttribute("usuarioLogueado",new Usuario(1,"Personal","Prueba","p@test","","MEDICO",null,true));
        servlet.doPost(x.request,x.response);assertEquals(403,x.status);
    }
    @Test void ignoraElPropietarioEnviadoPorElCliente() throws Exception {
        var x=new HttpExchange();x.session.setAttribute("usuarioLogueado",new Usuario(10,"Paciente","Prueba","p@test","","PACIENTE",null,true));
        x.parameters.put("idPaciente","999");x.parameters.put("idMedico","1");x.parameters.put("fecha","2026-12-01");x.parameters.put("hora","09:00");
        var servlet=new RetencionCupoServlet(){
            @Override protected CitaDAO citas(){return new CitaDAO(){@Override public int obtenerIdPacientePorUsuarioId(int usuario){assertEquals(10,usuario);return 5;}};}
            @Override protected AgendaCitaDAO.Retencion retener(int paciente,int medico,String fecha,String hora){assertEquals(5,paciente);assertEquals(1,medico);return new AgendaCitaDAO.Retencion("12345678-1234-1234-1234-123456789012",240);}
        };
        servlet.doPost(x.request,x.response);assertEquals(200,x.status);assertTrue(x.body.toString().contains("CUPO_RETENIDO"));assertTrue(x.body.toString().contains("240"));
        x.parameters.put("idMedico","otro");servlet.doPost(x.request,x.response);assertEquals(400,x.status);
    }
}
