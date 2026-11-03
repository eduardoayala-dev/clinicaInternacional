package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.SeguimientoIaDAO;
import pe.edu.utp.clinica.ia.ModeloInasistencia;
import pe.edu.utp.clinica.ia.SeguimientoIa;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.util.TransporteHttp;
import pe.edu.utp.clinica.util.TransporteServlet;
import java.io.IOException;

@WebServlet("/admin/seguimiento-ia")
public class SeguimientoIaServlet extends HttpServlet {
    private transient ModeloInasistencia modelo;
    protected SeguimientoIaDAO.Instantanea consultar(int actor,int cita) { return new SeguimientoIaDAO().consultar(actor,cita); }
    @Override public void init() {
        try { modelo=ModeloInasistencia.demostracion(); }
        catch(IOException e) { modelo=null; }
    }
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res) {
        res.setHeader("Cache-Control","no-store"); res.setHeader("Referrer-Policy","no-referrer");
        try {
            var sesion=req.getSession(false);
            if(sesion==null || !(sesion.getAttribute("usuarioLogueado") instanceof Usuario u)
                    || !"PERSONAL_ADMINISTRATIVO".equals(u.getRol())) throw new AgendaException(403,"ACCESO_DENEGADO","Ingresa con una cuenta de admisión.");
            String[] ids=req.getParameterValues("cita");
            if(ids==null || ids.length!=1 || ids[0]==null || !ids[0].matches("[1-9][0-9]{0,8}"))
                throw new AgendaException(400,"CITA_INVALIDA","Selecciona una cita válida.");
            if(modelo==null) throw new AgendaException(503,"MODELO_NO_DISPONIBLE","Modelo no disponible. Continúa el seguimiento habitual.");
            var evaluacion=SeguimientoIa.evaluar(consultar(u.getIdUsuario(),Integer.parseInt(ids[0])),modelo);
            if(json(req)) ApiJson.write(res,200,new ApiResponse<>("SEGUIMIENTO_IA_OBTENIDO","Evaluación experimental; la cita conserva su estado.",evaluacion));
            else {
                req.setAttribute("evaluacionIa",evaluacion);
                req.getRequestDispatcher("/WEB-INF/views/admin/seguimiento-ia.jsp").forward(req,res);
            }
        } catch(AgendaException e) {
            if(json(req)) { try { ApiJson.write(res,e.status(),new ApiResponse<>(e.code(),e.getMessage(),null)); } catch(IOException ex) { TransporteServlet.fallo(res); } }
            else TransporteHttp.error(res,e.status());
        } catch(IOException | ServletException e) { TransporteServlet.fallo(res); }
    }
    private static boolean json(HttpServletRequest req) { String accept=req.getHeader("Accept");return accept!=null && accept.contains("application/json"); }
}
