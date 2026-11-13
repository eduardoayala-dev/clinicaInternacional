package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/admin/usuarios")
public final class UsuariosServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response) {
        try {response.sendError(410,"La gestión de cuentas se realiza en el portal del administrador del sistema.");}
        catch(java.io.IOException e){pe.edu.utp.clinica.util.TransporteServlet.fallo(response);}
    }
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response) { doGet(request,response); }
}
