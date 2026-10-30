package pe.edu.utp.clinica.servlet;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.edu.utp.clinica.auth.*;
import pe.edu.utp.clinica.recuperacion.*;
import pe.edu.utp.clinica.util.TransporteHttp;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Logger;
import static pe.edu.utp.clinica.recuperacion.VerificacionJdbc.Proposito;

@WebServlet(urlPatterns={"/recuperar","/verificar-codigo","/restablecer","/cuenta/seguridad","/cuenta/nuevo-correo"})
public final class RecuperacionServlet extends HttpServlet {
    private static final String RUTA_RECUPERAR="/recuperar";
    private static final String RUTA_CODIGO="/verificar-codigo";
    private static final String RUTA_RESTABLECER="/restablecer";
    private static final String RUTA_CUENTA="/cuenta/seguridad";
    private static final String RUTA_CORREO="/cuenta/nuevo-correo";
    private static final String CODIGO="codigo";
    private static final String CAMPO_CLAVE="password";
    private static final String NUEVO_CORREO="nuevo-correo";
    private static final String SOLICITUD="solicitud";
    private static final String CUENTA="cuenta";
    private static final String PACIENTE="PACIENTE";
    private static final Logger LOGGER=Logger.getLogger(RecuperacionServlet.class.getName());
    private static final String PASO="verificacionCuenta";
    private static final String MENSAJE="mensajeRecuperacion";
    private record Paso(String id,Proposito proposito,boolean personal,String sesion,boolean nuevo)implements Serializable {}

    private RecuperacionServicio servicio(HttpServletRequest request){return (RecuperacionServicio)request.getServletContext().getAttribute(RecuperacionServicio.CONTEXTO);}
    private Paso paso(HttpServletRequest request){var s=request.getSession(false);return s==null?null:(Paso)s.getAttribute(PASO);}
    private String vinculo(HttpServletRequest request){return request.getSession(true).getId();}

    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response) {
        try {
            String path=request.getServletPath();
            if(RUTA_RECUPERAR.equals(path) || RUTA_CUENTA.equals(path))request.getSession().removeAttribute(PASO);
            if(path.startsWith("/cuenta/") && !autenticada(request)){TransporteHttp.redirigir(response,request.getContextPath()+"/login");return;}
            if(RUTA_RESTABLECER.equals(path) || RUTA_CORREO.equals(path)) {
                mostrarPasoAutorizado(request,response,path);
                return;
            }

            if(RUTA_CODIGO.equals(path)){
                if(!pasoValido(request,paso(request))){TransporteHttp.redirigir(response,request.getContextPath()+RUTA_RECUPERAR);return;}
                mostrar(request,response,CODIGO);return;
            }
            mostrar(request,response,path.startsWith("/cuenta/")?CUENTA:SOLICITUD);
        }catch(SQLException e){fallo(request,response);}
    }

    private void mostrarPasoAutorizado(HttpServletRequest request,HttpServletResponse response,String path)throws SQLException {
        Paso paso=paso(request);
        if(!pasoValido(request,paso) || servicio(request)==null || !servicio(request).autorizada(paso.id(),vinculo(request),paso.proposito(),paso.sesion())) {
            TransporteHttp.redirigir(response,request.getContextPath()+RUTA_CODIGO);
            return;
        }
        boolean correo=RUTA_CORREO.equals(path);
        if(correo!=(paso.proposito()==Proposito.CAMBIAR_CORREO)) {
            response.setStatus(403);
            mostrar(request,response,CODIGO);
            return;
        }
        mostrar(request,response,correo?NUEVO_CORREO:CAMPO_CLAVE);
    }

    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response) {
        if(servicio(request)==null){response.setStatus(503);request.setAttribute(MENSAJE,"La verificación no está disponible temporalmente.");mostrar(request,response,SOLICITUD);return;}
        if(!java.util.Set.of(RUTA_RECUPERAR,RUTA_CUENTA).contains(request.getServletPath()) && !pasoValido(request,paso(request))) {
            response.setStatus(403);request.setAttribute(MENSAJE,"Inicia una verificación antes de continuar.");mostrar(request,response,CODIGO);return;
        }
        try {
            switch(request.getServletPath()){
                case RUTA_RECUPERAR->solicitar(request,response);
                case RUTA_CUENTA->iniciarCambio(request,response);
                case RUTA_CODIGO->verificar(request,response);
                case RUTA_CORREO->nuevoCorreo(request,response);
                case RUTA_RESTABLECER->restablecer(request,response);
                default->response.setStatus(405);
            }
        }catch(SQLException e){fallo(request,response);}
        catch(IllegalArgumentException e){response.setStatus(400);request.setAttribute(MENSAJE,"Revisa los datos introducidos y los requisitos indicados.");mostrar(request,response,vistaActual(request));}
        catch(RejectedExecutionException e){response.setStatus(503);response.setHeader("Retry-After","60");request.setAttribute(MENSAJE,"El servicio está ocupado. Inténtalo en un minuto.");mostrar(request,response,vistaActual(request));}
    }

    private void solicitar(HttpServletRequest request,HttpServletResponse response) {
        boolean personal="personal".equals(request.getParameter("portal"));
        String id=servicio(request).solicitar(request.getParameter("identificador"),personal,vinculo(request),request.getRemoteAddr());
        request.getSession().setAttribute(PASO,new Paso(id,Proposito.RECUPERAR,personal,null,false));
        TransporteHttp.redirigir(response,request.getContextPath()+RUTA_CODIGO);
    }
    private void iniciarCambio(HttpServletRequest request,HttpServletResponse response)throws SQLException {
        var actual=actual(request);
        if(actual==null){response.setStatus(403);mostrar(request,response,CUENTA);return;}
        Proposito proposito=switch(String.valueOf(request.getParameter("operacion"))){
            case CAMPO_CLAVE->Proposito.CAMBIAR_PASSWORD;case "correo"->Proposito.CAMBIAR_CORREO;default->throw new IllegalArgumentException("Operación inválida.");};
        var id=servicio(request).iniciarCambio(actual.usuario().getIdUsuario(),actual.id(),vinculo(request),proposito,request.getParameter("actual"),request.getRemoteAddr());
        if(id.isEmpty()){response.setStatus(400);request.setAttribute(MENSAJE,"No se pudo iniciar el cambio. Comprueba tu contraseña actual y espera si superaste el límite de solicitudes.");mostrar(request,response,CUENTA);return;}
        request.getSession().setAttribute(PASO,new Paso(id.get(),proposito,!PACIENTE.equals(actual.usuario().getRol()),actual.id(),false));
        TransporteHttp.redirigir(response,request.getContextPath()+RUTA_CODIGO);
    }
    private void verificar(HttpServletRequest request,HttpServletResponse response)throws SQLException {
        Paso paso=exigirPaso(request);
        String codigo=request.getParameter(CODIGO);
        if(paso.nuevo()){
            if(servicio(request).confirmarCorreo(paso.id(),vinculo(request),paso.sesion(),codigo)){
                request.getSession().removeAttribute(PASO);request.setAttribute(MENSAJE,"Tu correo se actualizó y se cerraron tus otras sesiones.");mostrar(request,response,"completado");return;
            }
        }else if(servicio(request).verificar(paso.id(),vinculo(request),paso.proposito(),paso.sesion(),codigo)){
            TransporteHttp.redirigir(response,request.getContextPath()+(paso.proposito()==Proposito.CAMBIAR_CORREO?RUTA_CORREO:RUTA_RESTABLECER));return;
        }
        response.setStatus(400);request.setAttribute(MENSAJE,"El código es incorrecto, venció o alcanzó el límite de intentos. Revisa el último código recibido o inicia otra solicitud.");mostrar(request,response,CODIGO);
    }
    private void nuevoCorreo(HttpServletRequest request,HttpServletResponse response)throws SQLException {
        Paso paso=exigirPaso(request);
        if(paso.proposito()!=Proposito.CAMBIAR_CORREO)throw new IllegalArgumentException("Operación inválida.");
        String nuevo=request.getParameter("email");if(nuevo==null)throw new IllegalArgumentException("Falta el correo.");
        if(servicio(request).prepararCorreo(paso.id(),vinculo(request),paso.sesion(),nuevo)){
            request.getSession().setAttribute(PASO,new Paso(paso.id(),paso.proposito(),paso.personal(),paso.sesion(),true));
            TransporteHttp.redirigir(response,request.getContextPath()+RUTA_CODIGO);return;
        }
        response.setStatus(400);request.setAttribute(MENSAJE,"No se pudo utilizar esa dirección o la verificación venció. No se cambió tu correo actual.");mostrar(request,response,NUEVO_CORREO);
    }
    private void restablecer(HttpServletRequest request,HttpServletResponse response)throws SQLException {
        Paso paso=exigirPaso(request);
        String nueva=request.getParameter(CAMPO_CLAVE);
        if(nueva==null || !nueva.equals(request.getParameter("confirmacion")))throw new IllegalArgumentException("Las contraseñas no coinciden.");
        if(servicio(request).cambiarPassword(paso.id(),vinculo(request),paso.proposito(),paso.sesion(),nueva)){
            request.setAttribute("portalFinalPersonal",paso.personal());request.getSession().removeAttribute(PASO);
            request.setAttribute(MENSAJE,paso.proposito()==Proposito.RECUPERAR
                    ?"Tu contraseña se actualizó. Inicia sesión con la nueva contraseña; las sesiones anteriores quedaron cerradas."
                    :"Tu contraseña se actualizó. Esta sesión sigue activa y tus otras sesiones quedaron cerradas.");
            mostrar(request,response,"completado");return;
        }
        response.setStatus(400);request.setAttribute(MENSAJE,"La verificación venció o no es válida. La contraseña tampoco puede contener tu DNI. Inicia otra solicitud si es necesario.");mostrar(request,response,CAMPO_CLAVE);
    }
    private Paso exigirPaso(HttpServletRequest request){Paso paso=paso(request);if(!pasoValido(request,paso)){throw new IllegalArgumentException("La verificación no está vigente.");}
        return paso;}
    private boolean pasoValido(HttpServletRequest request,Paso paso){return paso!=null && (paso.proposito()==Proposito.RECUPERAR || actual(request)!=null && actual(request).id().equals(paso.sesion()));}
    private boolean autenticada(HttpServletRequest request){return actual(request)!=null;}
    private SesionAutenticada actual(HttpServletRequest request){return (SesionAutenticada)request.getAttribute(GestorAutenticacion.SESION);}
    private String vistaActual(HttpServletRequest request){return switch(request.getServletPath()){case RUTA_CODIGO->CODIGO;case RUTA_RESTABLECER->CAMPO_CLAVE;case RUTA_CORREO->NUEVO_CORREO;case RUTA_CUENTA->CUENTA;default->SOLICITUD;};}
    private void fallo(HttpServletRequest request,HttpServletResponse response){response.setStatus(503);request.setAttribute(MENSAJE,"No se pudo completar la operación. Inténtalo más tarde.");LOGGER.warning("No se pudo completar la verificación de cuenta: SQLException.");mostrar(request,response,vistaActual(request));}
    private void mostrar(HttpServletRequest request,HttpServletResponse response,String vista){
        response.setHeader("Cache-Control","no-store");response.setHeader("Referrer-Policy","no-referrer");response.setHeader("X-Frame-Options","DENY");response.setHeader("X-Content-Type-Options","nosniff");
        Paso paso=paso(request);
        boolean personal=portalPersonal(request,paso);
        request.setAttribute("portalPersonal",personal);request.setAttribute("vistaVerificacion",vista);request.setAttribute("codigoNuevo",paso!=null && paso.nuevo());
        request.setAttribute("cambioAutenticado",paso!=null && paso.proposito()!=Proposito.RECUPERAR);
        var actual=actual(request);
        request.setAttribute("portalDestino",actual==null?"/login":switch(actual.usuario().getRol()){
            case PACIENTE->"/paciente/dashboard";case "MEDICO"->"/medico/agenda";case "PERSONAL_ADMINISTRATIVO"->"/admin/dashboard";default->"/sysadmin/dashboard";});
        request.setAttribute("recuperacionDisponible",servicio(request)!=null);
        entregarVista(request,response);
    }
    private void entregarVista(HttpServletRequest request,HttpServletResponse response){
        boolean inline="1".equals(request.getHeader("X-Recuperacion-Inline"));
        if(inline)response.setHeader("X-Recuperacion-Vista","1");
        String destino=inline?"/WEB-INF/views/recuperacion-contenido.jsp":"/WEB-INF/views/recuperacion.jsp";
        try{request.getRequestDispatcher(destino).forward(request,response);}
        catch(IOException|ServletException e){if(!response.isCommitted()){response.setStatus(500);}
            LOGGER.warning("No se pudo entregar la vista de verificación.");}
    }
    private boolean portalPersonal(HttpServletRequest request,Paso paso){
        if(request.getAttribute("portalFinalPersonal") instanceof Boolean terminado)return terminado;
        if(paso!=null)return paso.personal();
        var actual=actual(request);
        if(actual!=null)return !PACIENTE.equals(actual.usuario().getRol());
        return "personal".equals(request.getParameter("portal"));
    }
}
