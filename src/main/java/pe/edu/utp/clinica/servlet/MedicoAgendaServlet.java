package pe.edu.utp.clinica.servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet(name="MedicoAgendaServlet",urlPatterns={"/medico/agenda"})
public class MedicoAgendaServlet extends MedicoPortalServlet {
    @Override protected void mostrar(HttpServletRequest request,HttpServletResponse response,Contexto contexto)throws IOException,ServletException{
        var periodo=periodo(request);agenda(request,contexto,periodo);
        if(json(request)) ApiJson.write(response,200,new ApiResponse<>("AGENDA_MEDICA_OBTENIDA","Consulta completada.",Map.of("fecha",periodo.fecha().toString(),"vista",periodo.vista(),"desde",periodo.desde().toString(),"hasta",periodo.hasta().minusDays(1).toString(),"citas",request.getAttribute("agenda"))));
        else request.getRequestDispatcher("/WEB-INF/views/medico/agenda.jsp").forward(request,response);
    }
}
