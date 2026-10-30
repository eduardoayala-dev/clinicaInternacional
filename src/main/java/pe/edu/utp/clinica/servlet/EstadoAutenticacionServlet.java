package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.edu.utp.clinica.auth.*;
import pe.edu.utp.clinica.api.*;
import pe.edu.utp.clinica.util.CsrfToken;
import java.io.IOException;
import java.util.Map;

@WebServlet(urlPatterns="/auth/estado")
public final class EstadoAutenticacionServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control","no-store");
        response.setHeader("X-Content-Type-Options","nosniff");
        if (GestorAutenticacion.obtener(request)==null) { response.sendError(404); return; }
        var sesion=(SesionAutenticada)request.getAttribute(GestorAutenticacion.SESION);
        String contexto=request.getHeader("X-Session-Context");
        if (sesion==null) {
            String csrf="anonymous".equals(contexto)?CsrfToken.getOrCreate(request.getSession(true)):"";
            ApiJson.write(response,200,new ApiResponse<>("SESION_ANONIMA","",Map.of("sid","anonymous","csrf",csrf,"portal",request.getContextPath()+"/login")));
        } else {
            String csrf=sesion.id().equals(contexto)?sesion.csrf():"";
            String path=switch(sesion.usuario().getRol()) {
                case "PACIENTE" -> "/paciente/dashboard";
                case "MEDICO" -> "/medico/agenda";
                case "PERSONAL_ADMINISTRATIVO" -> "/admin/dashboard";
                default -> "/sysadmin/dashboard";
            };
            ApiJson.write(response,200,new ApiResponse<>("SESION_ACTIVA","",Map.of("sid",sesion.id(),"csrf",csrf,"portal",request.getContextPath()+path)));
        }
    }
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response) throws IOException {
        response.setHeader("Allow","GET, HEAD"); response.sendError(405);
    }
}
