package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.edu.utp.clinica.dao.*;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.service.InvitacionPersonalServicio;
import java.io.IOException;

@WebServlet("/sysadmin/personal")
public class GestionPersonalServlet extends HttpServlet {
    private transient InvitacionPersonalServicio invitaciones;
    protected synchronized InvitacionPersonalServicio servicio(){
        if(invitaciones==null)invitaciones=InvitacionPersonalServicio.crear(System.getenv());
        return invitaciones;
    }
    protected InvitacionPersonalDAO almacen(){return new InvitacionPersonalDAO();}
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException {
        var sesion=request.getSession(false);
        if(sesion==null||!(sesion.getAttribute("usuarioLogueado") instanceof Usuario usuario)||!"ADMINISTRADOR".equals(usuario.getRol())){response.sendError(403);return;}
        try {
            String accion=request.getParameter("action");
            if("registrar".equals(accion))resultado(sesion,servicio().invitar(usuario.getIdUsuario(),solicitud(request)));
            else if("reinvitar".equals(accion))resultado(sesion,servicio().reenviar(usuario.getIdUsuario(),positivo(request.getParameter("idUsuario"))));
            else if("cambiarEstado".equals(accion)) {
                String valor=request.getParameter("estado");
                if(!"true".equals(valor)&&!"false".equals(valor))throw new IllegalArgumentException();
                almacen().cambiarEstado(usuario.getIdUsuario(),positivo(request.getParameter("idUsuario")),Boolean.parseBoolean(valor));
                sesion.setAttribute("flashMessage","Estado del personal actualizado.");
            } else if("editar".equals(accion)) {
                String estado=request.getParameter("estado");
                if(!java.util.Set.of("ACTIVO","INACTIVO","SUSPENDIDO").contains(estado==null?"":estado))throw new IllegalArgumentException();
                almacen().editar(usuario.getIdUsuario(),positivo(request.getParameter("idUsuario")),solicitud(request),"ACTIVO".equals(estado));
                sesion.setAttribute("flashMessage","Datos del personal actualizados.");
            } else {response.sendError(400);return;}
        } catch(AgendaException e){sesion.setAttribute("flashError",e.getMessage());}
        catch(IllegalArgumentException e){sesion.setAttribute("flashError","Revisa los datos y la configuración de la dirección pública para las invitaciones.");}
        response.setStatus(303);response.setHeader("Location",request.getContextPath()+"/sysadmin/usuarios");
    }
    private static InvitacionPersonalDAO.Solicitud solicitud(HttpServletRequest r) {
        String rol=r.getParameter("rol");
        String sede="MEDICO".equals(rol)?r.getParameter("id_sede_medico"):r.getParameter("id_sede_personal");
        if(sede==null||sede.isBlank())sede=r.getParameter("id_sede");
        return new InvitacionPersonalDAO.Solicitud(r.getParameter("nombres"),r.getParameter("apellidos"),r.getParameter("email"),rol,r.getParameter("cmp"),opcional(r.getParameter("id_especialidad")),opcional(sede),r.getParameter("cargo"));
    }
    private static Integer opcional(String s){return s==null||s.isBlank()?null:positivo(s);}
    private static int positivo(String s){int valor=Integer.parseInt(s);if(valor<=0)throw new IllegalArgumentException();return valor;}
    private static void resultado(HttpSession sesion,InvitacionPersonalServicio.Resultado resultado) {
        sesion.setAttribute(resultado.enviado()?"flashMessage":"flashError",resultado.enviado()
                ?"Invitación enviada. La cuenta permanece pendiente hasta que la persona cree su contraseña."
                :"La cuenta está pendiente de activación. No se confirmó el envío del correo; revisa la configuración y utiliza Reenviar invitación.");
    }
}
