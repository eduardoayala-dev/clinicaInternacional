package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.dao.AgendaCitaDAO;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.model.Usuario;
import java.io.IOException;

@WebServlet("/paciente/retencion")
public class RetencionCupoServlet extends HttpServlet {
    protected AgendaCitaDAO agenda() { return new AgendaCitaDAO(); }
    protected CitaDAO citas() { return new CitaDAO(); }
    protected AgendaCitaDAO.Retencion retener(int paciente,int medico,String fecha,String hora) {
        return agenda().retener(paciente,medico,fecha,hora);
    }
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response) {
        try { procesar(request,response); }
        catch(IOException e) {
            java.util.logging.Logger.getLogger(RetencionCupoServlet.class.getName()).log(java.util.logging.Level.WARNING,"No se pudo entregar la respuesta de retención.",e);
            if(!response.isCommitted())response.setStatus(500);
        }
    }
    private void procesar(HttpServletRequest request,HttpServletResponse response)throws IOException {
        var sesion=request.getSession(false);
        Usuario usuario=sesion==null?null:(Usuario)sesion.getAttribute("usuarioLogueado");
        if(usuario==null || !"PACIENTE".equals(usuario.getRol())) {
            ApiJson.write(response,403,new ApiResponse<>("ACCESO_DENEGADO","Acceso exclusivo para pacientes.",null));return;
        }
        try {
            int paciente=citas().obtenerIdPacientePorUsuarioId(usuario.getIdUsuario());
            if("liberar".equals(request.getParameter("accion"))) {
                agenda().liberarRetencion(paciente,request.getParameter("retencion"));
                ApiJson.write(response,200,new ApiResponse<>("RETENCION_LIBERADA","Cupo liberado.",null));
            } else {
                var retencion=retener(paciente,Integer.parseInt(request.getParameter("idMedico")),request.getParameter("fecha"),request.getParameter("hora"));
                ApiJson.write(response,200,new ApiResponse<>("CUPO_RETENIDO","Cupo reservado temporalmente.",retencion));
            }
        } catch(NumberFormatException e) {
            ApiJson.write(response,400,new ApiResponse<>("DATOS_INVALIDOS","Selecciona un médico y horario válidos.",null));
        } catch(AgendaException e) {
            ApiJson.write(response,e.status(),new ApiResponse<>(e.code(),e.getMessage(),null));
        } catch(RuntimeException e) {
            ApiJson.write(response,503,new ApiResponse<>("RETENCION_NO_DISPONIBLE","No se pudo retener el horario. Inténtalo nuevamente.",null));
        }
    }
}
