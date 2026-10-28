package pe.edu.utp.clinica.servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

@WebServlet(name="MedicoInicioServlet",urlPatterns={"/medico/inicio","/medico/dashboard"})
public class MedicoInicioServlet extends MedicoPortalServlet {
    @Override protected void mostrar(HttpServletRequest request,HttpServletResponse response,Contexto contexto)throws IOException,ServletException{
        agenda(request,contexto,LocalDate.now(reloj()));
        if(json(request)){
            var data=new java.util.LinkedHashMap<String,Object>();
            for(String campo:new String[]{"agenda","totalCitas","atendidos","enEspera","pendientes","inasistencias","pacienteEnSala"}) data.put(campo,request.getAttribute(campo));
            ApiJson.write(response,200,new ApiResponse<>("INICIO_MEDICO_OBTENIDO","Consulta completada.",data));
        }else request.getRequestDispatcher("/WEB-INF/views/medico/inicio.jsp").forward(request,response);
    }
}
