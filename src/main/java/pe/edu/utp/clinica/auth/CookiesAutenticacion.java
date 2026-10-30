package pe.edu.utp.clinica.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class CookiesAutenticacion {
    public static final String ACCESO="CLINICA_ACCESS", RENOVACION="CLINICA_REFRESH";
    private CookiesAutenticacion() { }
    public static String leer(HttpServletRequest request, String nombre) {
        String value=null;
        if (request.getCookies()!=null) for (Cookie cookie : request.getCookies()) {
            if (nombre.equals(cookie.getName())) {
                if (value!=null) return null;
                value=cookie.getValue();
            }
        }
        return value;
    }
    public static void escribir(HttpServletRequest request, HttpServletResponse response, String nombre,
                                String value, int segundos, boolean segura) {
        Cookie cookie=new Cookie(nombre,value);
        cookie.setHttpOnly(true); cookie.setSecure(segura); cookie.setAttribute("SameSite","Lax");
        cookie.setPath(request.getContextPath().isEmpty()?"/":request.getContextPath());
        cookie.setMaxAge(segundos);
        response.addCookie(cookie);
    }
    public static void borrar(HttpServletRequest request,HttpServletResponse response,boolean segura) {
        escribir(request,response,ACCESO,"",0,segura);
        escribir(request,response,RENOVACION,"",0,segura);
    }
}
