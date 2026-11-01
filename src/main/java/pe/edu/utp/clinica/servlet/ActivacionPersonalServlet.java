package pe.edu.utp.clinica.servlet;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.edu.utp.clinica.api.*;
import pe.edu.utp.clinica.auth.SecretosSesion;
import pe.edu.utp.clinica.dao.*;
import java.io.IOException;

@WebServlet("/activar-personal")
public class ActivacionPersonalServlet extends HttpServlet {
    private static final String VINCULO="invitacionPersonalHash";
    protected InvitacionPersonalDAO almacen(){return new InvitacionPersonalDAO();}
    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response) {
        pe.edu.utp.clinica.util.TransporteServlet.entregar(response,()->mostrarActivacion(request,response));
    }
    private void mostrarActivacion(HttpServletRequest request,HttpServletResponse response) throws IOException,ServletException {
        response.setHeader("Cache-Control","no-store");response.setHeader("Referrer-Policy","no-referrer");
        response.setHeader("X-Frame-Options","DENY");response.setHeader("X-Content-Type-Options","nosniff");
        request.setAttribute("portalPersonal",true);
        try {
            byte[] hash=vinculo(request);
            request.setAttribute("invitacionVigente",almacen().vigente(hash));
            if(hash!=null)request.setAttribute("referenciaInvitacion",java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash));
        }
        catch(AgendaException e){response.setStatus(e.status());request.setAttribute("mensajeInvitacion",e.getMessage());}
        request.getRequestDispatcher("/WEB-INF/views/activar-personal.jsp").forward(request,response);
    }
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response) {
        pe.edu.utp.clinica.util.TransporteServlet.entregar(response,()->procesarActivacion(request,response));
    }
    private void procesarActivacion(HttpServletRequest request,HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control","no-store");response.setHeader("Referrer-Policy","no-referrer");
        try {
            if("preparar".equals(request.getParameter("action"))) {
                preparar(request);
                ApiJson.write(response,200,new ApiResponse<>("INVITACION_PREPARADA","Crea tu contraseña para continuar.",null));
            } else if("activar".equals(request.getParameter("action"))) {
                byte[] hash=vinculo(request);
                String referencia=hash==null?null:java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
                if(referencia==null||!referencia.equals(request.getParameter("referencia")))throw new AgendaException(409,"INVITACION_CAMBIADA","La invitación cambió en otra pestaña. Abre de nuevo el enlace de tu correo.");
                if(!java.util.Objects.equals(request.getParameter("password"),request.getParameter("confirmacion")))throw new AgendaException(400,"PASSWORD_NO_COINCIDE","Las contraseñas no coinciden.");
                almacen().activar(hash,request.getParameter("password"));
                request.getSession().removeAttribute(VINCULO);
                ApiJson.write(response,200,new ApiResponse<>("PERSONAL_ACTIVADO","Tu cuenta quedó activa. Ya puedes iniciar sesión.",null));
            } else throw new AgendaException(400,"ACCION_INVALIDA","La acción no es válida.");
        } catch(AgendaException e) {ApiJson.write(response,e.status(),new ApiResponse<>(e.code(),e.getMessage(),null));}
    }
    private void preparar(HttpServletRequest request) {
        request.getSession().removeAttribute(VINCULO);
        String token=request.getParameter("token");
        if(token==null||!token.matches("[A-Za-z0-9_-]{43}"))throw new AgendaException(410,"INVITACION_INVALIDA","La invitación no es válida o venció.");
        byte[] hash=SecretosSesion.hash(token);
        if(!almacen().vigente(hash))throw new AgendaException(410,"INVITACION_INVALIDA","La invitación no es válida, venció o ya se utilizó.");
        request.getSession().setAttribute(VINCULO,hash);
    }
    private static byte[] vinculo(HttpServletRequest request) {
        var sesion=request.getSession(false);
        return sesion!=null && sesion.getAttribute(VINCULO) instanceof byte[] hash?hash:null;
    }
}
