package pe.edu.utp.clinica.auth;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.util.CsrfToken;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

public final class GestorAutenticacion {
    public static final String CONTEXTO=GestorAutenticacion.class.getName();
    public static final String SESION=CONTEXTO+".sesion";
    private static final String AUTH_SID="authSid";
    private final JwtAcceso jwt;
    private final AlmacenSesiones almacen;
    private final Clock clock;
    private final boolean segura;
    public GestorAutenticacion(JwtAcceso jwt,AlmacenSesiones almacen,Clock clock,boolean segura) {
        this.jwt=jwt; this.almacen=almacen; this.clock=clock; this.segura=segura;
    }
    public static GestorAutenticacion obtener(HttpServletRequest request) {
        var context=request.getServletContext();
        return context==null?null:(GestorAutenticacion)context.getAttribute(CONTEXTO);
    }
    public void iniciar(HttpServletRequest request,HttpServletResponse response,Usuario usuario) throws SQLException {
        String refresh=SecretosSesion.nuevo();
        String sid=UUID.randomUUID().toString();
        String csrf=SecretosSesion.nuevo();
        var now=clock.instant();
        var sesion=new SesionAutenticada(sid,usuario,csrf,now.plusSeconds(28800));
        almacen.crear(sid,usuario,SecretosSesion.hash(refresh),csrf,now,sesion.vence());
        var previa=(SesionAutenticada)request.getAttribute(SESION);
        if (previa!=null) almacen.revocar(previa.id(),now);
        proyectar(request,sesion,true);
        CookiesAutenticacion.escribir(request,response,CookiesAutenticacion.RENOVACION,refresh,28800,segura);
        emitir(request,response,sesion);
    }
    public SesionAutenticada resolver(HttpServletRequest request,HttpServletResponse response,boolean actividad) throws SQLException {
        String access=CookiesAutenticacion.leer(request,CookiesAutenticacion.ACCESO);
        String refresh=CookiesAutenticacion.leer(request,CookiesAutenticacion.RENOVACION);
        JwtAcceso.Identidad identidad=null;
        if (access!=null) {
            try { identidad=jwt.verificar(access); }
            catch (ExpiredJwtException e) { identidad=null; }
            catch (JwtException | IllegalArgumentException e) { limpiar(request,response); return null; }
        }
        SesionAutenticada sesion;
        if (identidad!=null) {
            sesion=almacen.resolver(identidad.sesion(),null,clock.instant(),actividad);
            if (!coincide(identidad,sesion)) sesion=null;
        } else if (refresh!=null && refresh.matches("[A-Za-z0-9_-]{43}")) {
            sesion=almacen.resolver(null,SecretosSesion.hash(refresh),clock.instant(),actividad);
            if (sesion!=null) emitir(request,response,sesion);
        } else sesion=null;
        if (sesion==null) { limpiar(request,response); return null; }
        proyectar(request,sesion,false);
        return sesion;
    }
    private static boolean coincide(JwtAcceso.Identidad identidad,SesionAutenticada sesion) {
        return sesion!=null && identidad.usuario()==sesion.usuario().getIdUsuario()
                && identidad.rol().equals(sesion.usuario().getRol());
    }
    private void emitir(HttpServletRequest request,HttpServletResponse response,SesionAutenticada sesion) {
        int segundos=(int)Math.min(JwtAcceso.DURACION_SEGUNDOS,Duration.between(clock.instant(),sesion.vence()).toSeconds());
        CookiesAutenticacion.escribir(request,response,CookiesAutenticacion.ACCESO,
                jwt.emitir(sesion.usuario().getIdUsuario(),sesion.id(),sesion.usuario().getRol()),Math.max(1,segundos),segura);
    }
    private void proyectar(HttpServletRequest request,SesionAutenticada sesion,boolean nueva) {
        var http=request.getSession(false);
        if (http!=null && (nueva || !sesion.id().equals(http.getAttribute(AUTH_SID)))) {
            http.invalidate(); http=null;
        }
        if (http==null) http=request.getSession(true);
        http.setAttribute("usuarioLogueado",sesion.usuario());
        http.setAttribute("usuarioId",sesion.usuario().getIdUsuario());
        http.setAttribute("nombreUsuario",sesion.usuario().getNombreCompleto());
        http.setAttribute("rol",sesion.usuario().getRol());
        http.setAttribute(AUTH_SID,sesion.id());
        http.setAttribute(CsrfToken.ATTRIBUTE,sesion.csrf());
        http.setMaxInactiveInterval(1800);
        request.setAttribute(SESION,sesion);
    }
    public void cerrar(HttpServletRequest request,HttpServletResponse response) throws SQLException {
        var sesion=(SesionAutenticada)request.getAttribute(SESION);
        if (sesion!=null) almacen.revocar(sesion.id(),clock.instant());
        limpiar(request,response);
        var http=request.getSession(false);
        if (http!=null) http.invalidate();
    }
    private void limpiar(HttpServletRequest request,HttpServletResponse response) {
        if (CookiesAutenticacion.leer(request,CookiesAutenticacion.ACCESO)!=null
                || CookiesAutenticacion.leer(request,CookiesAutenticacion.RENOVACION)!=null) {
            CookiesAutenticacion.borrar(request,response,segura);
        }
        request.removeAttribute(SESION);
        var http=request.getSession(false);
        if (http!=null && (http.getAttribute("usuarioLogueado")!=null || http.getAttribute(AUTH_SID)!=null)) http.invalidate();
    }
}
