package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.ConfirmacionAsistenciaDAO;
import pe.edu.utp.clinica.model.Usuario;
import java.io.IOException;
import java.util.List;

@WebServlet(name="SeguimientoConfirmacionesServlet", urlPatterns="/admin/seguimiento-confirmaciones")
public final class SeguimientoConfirmacionesServlet extends HttpServlet {
    private transient ConfirmacionAsistenciaDAO confirmaciones;
    protected ConfirmacionAsistenciaDAO crearDAO() { return new ConfirmacionAsistenciaDAO(); }
    @Override public void init() { confirmaciones=crearDAO(); }

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Usuario usuario=usuario(request);
        if(usuario==null) { response.sendRedirect(request.getContextPath()+"/login/personal?error=acceso_denegado"); return; }
        try {
            List<ConfirmacionAsistenciaDAO.Seguimiento> pendientes=confirmaciones.listarPendientes(usuario.getIdUsuario());
            request.setAttribute("seguimientos",pendientes);
            request.setAttribute("totalPendientes",pendientes.size());
            request.setAttribute("totalVencidas",pendientes.stream().filter(ConfirmacionAsistenciaDAO.Seguimiento::vencida).count());
            request.setAttribute("mensajeFlash",request.getSession(false).getAttribute("flashMessage"));
            request.getSession(false).removeAttribute("flashMessage");
            request.getRequestDispatcher("/WEB-INF/views/admin/seguimiento-confirmaciones.jsp").forward(request,response);
        } catch(AgendaException e) { error(response,e.status(),e.code(),e.getMessage()); }
    }

    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response) throws IOException {
        Usuario usuario=usuario(request);
        if(usuario==null) { error(response,403,"ACCESO_DENEGADO","Ingresa con una cuenta de admisión."); return; }
        try {
            int idCita=identificador(request.getParameter("idCita"));
            var resultado=confirmaciones.registrarContacto(usuario.getIdUsuario(),idCita,
                    request.getParameter("canal"),request.getParameter("resultado"));
            String mensaje=resultado.asistenciaConfirmada()
                    ? "Contacto registrado. La asistencia quedó confirmada por admisión."
                    : "Intento de contacto registrado. La cita conserva su estado.";
            if(json(request)) ApiJson.write(response,200,new ApiResponse<>("CONTACTO_REGISTRADO",mensaje,resultado));
            else { request.getSession(false).setAttribute("flashMessage",mensaje); response.sendRedirect(request.getContextPath()+"/admin/seguimiento-confirmaciones"); }
        } catch(AgendaException e) {
            if(json(request)) error(response,e.status(),e.code(),e.getMessage());
            else { request.getSession(false).setAttribute("flashMessage",e.getMessage()); response.sendRedirect(request.getContextPath()+"/admin/seguimiento-confirmaciones"); }
        }
    }

    private static Usuario usuario(HttpServletRequest request) {
        var session=request.getSession(false);
        if(session==null || !(session.getAttribute("usuarioLogueado") instanceof Usuario usuario)
                || !"PERSONAL_ADMINISTRATIVO".equals(usuario.getRol())) return null;
        return usuario;
    }
    private static int identificador(String valor) {
        try { if(valor==null || !valor.matches("[1-9]\\d{0,9}")) throw new NumberFormatException(); return Integer.parseInt(valor); }
        catch(NumberFormatException e) { throw new AgendaException(400,"DATOS_INVALIDOS","Selecciona una cita válida."); }
    }
    private static boolean json(HttpServletRequest request) { String accept=request.getHeader("Accept"); return accept!=null && accept.contains("application/json"); }
    private static void error(HttpServletResponse response,int estado,String codigo,String mensaje) throws IOException {
        if(!response.isCommitted()) ApiJson.write(response,estado,new ApiResponse<>(codigo,mensaje,null));
    }
}
