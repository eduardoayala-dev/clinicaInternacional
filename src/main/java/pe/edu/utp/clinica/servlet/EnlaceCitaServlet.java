package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.edu.utp.clinica.dao.*;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.util.EnlaceCita;
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
                if(req.getSession(false)!=null&&req.getSession(false).getAttribute("usuarioLogueado")!=null){res.sendError(403);return;}
                enlace.recordar(req.getSession(true),Clock.systemUTC());
                res.sendRedirect(req.getContextPath()+"/login/paciente");return;
            }
            req.setAttribute("enlaceCita",enlace);req.setAttribute("resumenCita",almacen().consultar(actor.getIdUsuario(),enlace.cita()));
            req.getRequestDispatcher("/WEB-INF/views/paciente/enlace-cita.jsp").forward(req,res);
        }catch(IllegalArgumentException e){res.sendError(400);}
        catch(AgendaException e){res.sendError(e.status());}
        catch(jakarta.servlet.ServletException e){res.sendError(500);}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws IOException {
        cabeceras(res);var actor=usuario(req);
        if(actor==null){res.sendError(403);return;}
        try{
            var enlace=EnlaceCita.leer(req.getParameter("cita"),req.getParameter("accion"));
            almacen().ejecutar(actor.getIdUsuario(),enlace);
            req.getSession(false).setAttribute("flashMessage",enlace.accion()==EnlaceCita.Accion.CONFIRMAR?"Tu asistencia quedó confirmada.":"Tu cita quedó cancelada.");
            res.setStatus(303);res.setHeader("Location",req.getContextPath()+"/paciente/historial");
        }catch(IllegalArgumentException e){res.sendError(400);}
        catch(AgendaException e){req.getSession(false).setAttribute("agendaError",e.getMessage());res.setStatus(303);res.setHeader("Location",req.getContextPath()+"/paciente/historial");}
    }
}

