package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.edu.utp.clinica.dao.*;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.util.EnlaceCita;
import pe.edu.utp.clinica.util.TransporteHttp;
import java.io.IOException;
import java.time.Clock;

@WebServlet("/cita")
public class EnlaceCitaServlet extends HttpServlet {
    protected EnlaceCitaDAO almacen(){return new EnlaceCitaDAO();}
    private static Usuario usuario(HttpServletRequest r){var s=r.getSession(false);return s!=null&&s.getAttribute("usuarioLogueado") instanceof Usuario u&&"PACIENTE".equals(u.getRol())?u:null;}
    private static void cabeceras(HttpServletResponse res){
        res.setHeader("Cache-Control","no-store");res.setHeader("Referrer-Policy","no-referrer");
        res.setHeader("X-Frame-Options","DENY");res.setHeader("X-Content-Type-Options","nosniff");
        res.setHeader("Content-Security-Policy","frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
    }
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws IOException {
        cabeceras(res);
        try{
            var enlace=EnlaceCita.leer(req.getParameter("cita"),req.getParameter("accion"));
            var actor=usuario(req);
            if(actor==null){
                if(req.getSession(false)!=null&&req.getSession(false).getAttribute("usuarioLogueado")!=null){TransporteHttp.error(res,403);return;}
                enlace.recordar(req.getSession(true),Clock.systemUTC());
                TransporteHttp.redirigir(res,req.getContextPath()+"/login/paciente");return;
            }
            req.setAttribute("enlaceCita",enlace);req.setAttribute("resumenCita",almacen().consultar(actor.getIdUsuario(),enlace.cita()));
            req.getRequestDispatcher("/WEB-INF/views/paciente/enlace-cita.jsp").forward(req,res);
        }catch(IllegalArgumentException e){TransporteHttp.error(res,400);}
        catch(AgendaException e){TransporteHttp.error(res,e.status());}
        catch(jakarta.servlet.ServletException | IOException e){TransporteHttp.error(res,500);}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws IOException {
        cabeceras(res);var actor=usuario(req);
        if(actor==null){TransporteHttp.error(res,403);return;}
        try{
            var enlace=EnlaceCita.leer(req.getParameter("cita"),req.getParameter("accion"));
            if(enlace.accion()==EnlaceCita.Accion.CANCELAR) {
                String motivo=pe.edu.utp.clinica.util.MotivoCancelacion.validar(req.getParameter("motivo"));
                almacen().ejecutar(actor.getIdUsuario(),enlace,motivo);
            } else almacen().ejecutar(actor.getIdUsuario(),enlace);
            req.getSession(false).setAttribute("flashMessage",enlace.accion()==EnlaceCita.Accion.CONFIRMAR?"Tu asistencia quedó confirmada.":"Tu cita quedó cancelada.");
            res.setStatus(303);res.setHeader("Location",req.getContextPath()+"/paciente/historial");
        }catch(IllegalArgumentException e){TransporteHttp.error(res,400);}
        catch(AgendaException e){req.getSession(false).setAttribute("agendaError",e.getMessage());res.setStatus(303);res.setHeader("Location",req.getContextPath()+"/paciente/historial");}
    }
}

