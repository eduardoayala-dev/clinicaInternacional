package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.edu.utp.clinica.dao.*;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.service.InvitacionPersonalServicio;
import java.io.IOException;

@WebServlet("/admin/medicos/nuevo")
public class AltaMedicoAdmisionServlet extends HttpServlet {
    protected PerfilProfesionalMedicoDAO perfiles(){return new PerfilProfesionalMedicoDAO();}
    protected int sede(int actor){return new AmbitoAdmisionDAO().sede(actor);}
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res){
        try{
            int sede=sede(actor(req));
            req.setAttribute("sedeAltaMedico",sede);
            req.setAttribute("cuentasMedicasPendientes",perfiles().pendientes(actor(req)));
            req.setAttribute("especialidadesAlta",new MedicoDAO().obtenerEspecialidades());
            req.getRequestDispatcher("/WEB-INF/views/admin/alta-medico.jsp").forward(req,res);
        }catch(AgendaException e){error(req,res,e);}
        catch(IOException|jakarta.servlet.ServletException e){pe.edu.utp.clinica.util.TransporteServlet.fallo(res);}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res){
        try{
            int actor=actor(req);int sede=sede(actor);
            int especialidad=Integer.parseInt(req.getParameter("especialidad"));
            int cuenta=Integer.parseInt(req.getParameter("cuenta"));
            perfiles().completar(actor,cuenta,req.getParameter("cmp"),especialidad,req.getRemoteAddr());
            req.getSession(false).setAttribute("flashMessage","Perfil profesional completado. La cuenta debe estar activada para proponer y publicar horarios.");
            res.setStatus(303);res.setHeader("Location",req.getContextPath()+"/admin/medicos?estado=Todos");
        }catch(AgendaException e){error(req,res,e);}
        catch(NumberFormatException e){error(req,res,new AgendaException(400,"ESPECIALIDAD_INVALIDA","Selecciona una especialidad válida."));}
    }
    private static int actor(HttpServletRequest req){var s=req.getSession(false);if(s==null||!(s.getAttribute("usuarioLogueado") instanceof Usuario u)||!"PERSONAL_ADMINISTRATIVO".equals(u.getRol()))throw new AgendaException(403,"ADMISION_REQUERIDA","Ingresa como personal de admisión.");return u.getIdUsuario();}
    private static void error(HttpServletRequest req,HttpServletResponse res,AgendaException e){try{if(e.status()==403){res.sendError(403);return;}req.getSession(false).setAttribute("flashError",e.getMessage());res.setStatus(303);res.setHeader("Location",req.getContextPath()+"/admin/medicos/nuevo");}catch(IOException ex){pe.edu.utp.clinica.util.TransporteServlet.fallo(res);}}
}
