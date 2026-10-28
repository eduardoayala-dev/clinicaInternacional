package pe.edu.utp.clinica.servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import java.io.IOException;

@WebServlet(name="MedicoPerfilServlet",urlPatterns={"/medico/perfil"})
public class MedicoPerfilServlet extends MedicoPortalServlet {
    @Override protected void mostrar(HttpServletRequest request,HttpServletResponse response,Contexto contexto)throws IOException,ServletException{
        if(json(request)) ApiJson.write(response,200,new ApiResponse<>("PERFIL_MEDICO_OBTENIDO","Consulta completada.",contexto.perfil()));
        else request.getRequestDispatcher("/WEB-INF/views/medico/perfil.jsp").forward(request,response);
    }
}
