package pe.edu.utp.clinica.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.util.CsrfToken;
import java.sql.SQLException;

public final class InicioSesion {
    private InicioSesion() { }
    public static HttpSession iniciar(HttpServletRequest request,HttpServletResponse response,Usuario usuario) throws SQLException {
        GestorAutenticacion gestor=GestorAutenticacion.obtener(request);
        if (gestor!=null) {
            gestor.iniciar(request,response,usuario);
        } else {
            HttpSession old=request.getSession(false);
            if (old!=null) old.invalidate();
            HttpSession http=request.getSession(true);
            http.setAttribute("usuarioLogueado",usuario);
            http.setAttribute("usuarioId",usuario.getIdUsuario());
            http.setAttribute("rol",usuario.getRol());
            http.setAttribute("nombreUsuario",usuario.getNombreCompleto());
            CsrfToken.getOrCreate(http);
            http.setMaxInactiveInterval(1800);
        }
        return request.getSession(false);
    }
}
