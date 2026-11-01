package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.ConfiguracionOfertasDAO;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.util.TransporteServlet;
import java.io.IOException;

@WebServlet("/sysadmin/configuracion-ofertas")
public class ConfiguracionOfertasServlet extends HttpServlet {
    private static final String RUTA="/sysadmin/priorizacion";
    protected ConfiguracionOfertasDAO almacen(){return new ConfiguracionOfertasDAO();}
    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response) {
        try {mostrar(request,response);}
        catch(AgendaException e){rechazar(response,e.status(),e.getMessage());}
        catch(IOException|ServletException e){TransporteServlet.fallo(response);}
    }
    private void mostrar(HttpServletRequest request,HttpServletResponse response)throws IOException,ServletException {
        actor(request);
        response.setHeader("Cache-Control","no-store");
        request.setAttribute("configuracionOfertas",almacen().leer());
        request.getRequestDispatcher("/WEB-INF/views/sysadmin/priorizacion.jsp").forward(request,response);
    }
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response) {
        try {guardar(request,response);}
        catch(AgendaException e){rechazar(response,e.status(),e.getMessage());}
        catch(IOException e){TransporteServlet.fallo(response);}
    }
    private void guardar(HttpServletRequest request,HttpServletResponse response)throws IOException {
        Usuario usuario=actor(request);
        try {
            almacen().guardar(usuario.getIdUsuario(),Integer.parseInt(request.getParameter("tiempoLimite")),
                    Integer.parseInt(request.getParameter("anticipacionMinima")),Long.parseLong(request.getParameter("version")));
            request.getSession().setAttribute("flashMessage","Configuración guardada. Se aplicará a las nuevas ofertas; las ofertas vigentes conservan su vencimiento.");
        } catch(NumberFormatException e){request.getSession().setAttribute("flashError","Completa los parámetros con números válidos.");}
        catch(AgendaException e){request.getSession().setAttribute("flashError",e.getMessage());}
        response.setStatus(303);response.setHeader("Location",request.getContextPath()+RUTA);
    }
    private static Usuario actor(HttpServletRequest request) {
        var sesion=request.getSession(false);
        if(sesion==null||!(sesion.getAttribute("usuarioLogueado") instanceof Usuario usuario)||!"ADMINISTRADOR".equals(usuario.getRol()))throw new AgendaException(403,"ADMINISTRADOR_REQUERIDO","Ingresa como administrador del sistema.");
        return usuario;
    }
    private static void rechazar(HttpServletResponse response,int estado,String mensaje) {
        try {response.sendError(estado,mensaje);}
        catch(IOException e){TransporteServlet.fallo(response);}
    }
}
