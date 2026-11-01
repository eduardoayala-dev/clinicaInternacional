package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.edu.utp.clinica.dao.*;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.service.InvitacionPersonalServicio;
import java.io.IOException;

@WebServlet("/sysadmin/personal")
public class GestionPersonalServlet extends HttpServlet {
    private static final String ID_USUARIO="idUsuario";
    private static final String FLASH_MESSAGE="flashMessage";
    private static final String FLASH_ERROR="flashError";
    private final java.util.concurrent.atomic.AtomicReference<InvitacionPersonalServicio> invitaciones=new java.util.concurrent.atomic.AtomicReference<>();
    @Override public void init(){invitaciones.set(InvitacionPersonalServicio.crear(System.getenv()));}
    protected InvitacionPersonalServicio servicio(){return invitaciones.get();}
    protected InvitacionPersonalDAO almacen(){return new InvitacionPersonalDAO();}
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response) {
        pe.edu.utp.clinica.util.TransporteServlet.entregar(response,()->procesar(request,response));
    }
    private void procesar(HttpServletRequest request,HttpServletResponse response)throws IOException {
        var sesion=request.getSession(false);
        if(sesion==null||!(sesion.getAttribute("usuarioLogueado") instanceof Usuario usuario)||!"ADMINISTRADOR".equals(usuario.getRol())){response.sendError(403);return;}
        try {
            String accion=request.getParameter("action");
            if("registrar".equals(accion))resultado(sesion,servicio().invitar(usuario.getIdUsuario(),solicitud(request)));
            else if("reinvitar".equals(accion))resultado(sesion,servicio().reenviar(usuario.getIdUsuario(),positivo(request.getParameter(ID_USUARIO))));
            else if("cambiarEstado".equals(accion))cambiarEstado(request,sesion,usuario);
            else if("editar".equals(accion))editar(request,sesion,usuario);
            else {response.sendError(400);return;}
        } catch(AgendaException e){sesion.setAttribute(FLASH_ERROR,e.getMessage());}
        catch(IllegalArgumentException e){sesion.setAttribute(FLASH_ERROR,"Revisa los datos y la configuración de la dirección pública para las invitaciones.");}
        response.setStatus(303);response.setHeader("Location",request.getContextPath()+"/sysadmin/usuarios");
    }
    private void cambiarEstado(HttpServletRequest request,HttpSession sesion,Usuario usuario) {
        String valor=request.getParameter("estado");
        if(!"true".equals(valor)&&!"false".equals(valor))throw new IllegalArgumentException();
        almacen().cambiarEstado(usuario.getIdUsuario(),positivo(request.getParameter(ID_USUARIO)),Boolean.parseBoolean(valor));
        sesion.setAttribute(FLASH_MESSAGE,"Estado del personal actualizado.");
    }
    private void editar(HttpServletRequest request,HttpSession sesion,Usuario usuario) {
        String estado=request.getParameter("estado");
        if(!java.util.Set.of("ACTIVO","INACTIVO","SUSPENDIDO").contains(estado==null?"":estado))throw new IllegalArgumentException();
        almacen().editar(usuario.getIdUsuario(),positivo(request.getParameter(ID_USUARIO)),solicitud(request),"ACTIVO".equals(estado));
        sesion.setAttribute(FLASH_MESSAGE,"Datos del personal actualizados.");
    }
    private static InvitacionPersonalDAO.Solicitud solicitud(HttpServletRequest r) {
        String rol=r.getParameter("rol");
        String sede=r.getParameter("MEDICO".equals(rol)?"id_sede_medico":"id_sede_personal");
        if(sede==null||sede.isBlank()){sede=r.getParameter("id_sede");}
        return new InvitacionPersonalDAO.Solicitud(r.getParameter("nombres"),r.getParameter("apellidos"),r.getParameter("email"),rol,r.getParameter("cmp"),opcional(r.getParameter("id_especialidad")),opcional(sede),r.getParameter("cargo"));
    }
    private static Integer opcional(String s){return s==null||s.isBlank()?null:positivo(s);}
    private static int positivo(String s){int valor=Integer.parseInt(s);if(valor<=0){throw new IllegalArgumentException();}return valor;}
    private static void resultado(HttpSession sesion,InvitacionPersonalServicio.Resultado resultado) {
        sesion.setAttribute(resultado.enviado()?FLASH_MESSAGE:FLASH_ERROR,resultado.enviado()
                ?"Invitación enviada. La cuenta permanece pendiente hasta que la persona cree su contraseña."
                :"La cuenta está pendiente de activación. No se confirmó el envío del correo; revisa la configuración y utiliza Reenviar invitación.");
    }
}
