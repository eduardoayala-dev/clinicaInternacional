package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.edu.utp.clinica.api.*;
import pe.edu.utp.clinica.dao.*;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.util.TransporteHttp;
import pe.edu.utp.clinica.util.TransporteServlet;
import java.io.IOException;
import java.time.LocalDate;

@WebServlet("/admin/reasignaciones")
public class SeguimientoOfertasServlet extends HttpServlet {
    protected SeguimientoOfertasDAO.Panel consultar(int actor,SeguimientoOfertasDAO.Filtro filtro){return new SeguimientoOfertasDAO().consultar(actor,filtro);}
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res) {
        res.setHeader("Cache-Control","no-store");
        try {
            var sesion=req.getSession(false);
            if(sesion==null || !(sesion.getAttribute("usuarioLogueado") instanceof Usuario u) || !"PERSONAL_ADMINISTRATIVO".equals(u.getRol()))
                throw new AgendaException(403,"ACCESO_DENEGADO","Ingresa con una cuenta de admisión.");
            var fecha=parametro(req,"fecha");
            var filtro=new SeguimientoOfertasDAO.Filtro(parametro(req,"estado"),fecha.isEmpty()?null:fecha(fecha),
                    numero(parametro(req,"especialidad"),0),numero(parametro(req,"pagina"),1));
            var panel=consultar(u.getIdUsuario(),filtro);
            if(json(req))ApiJson.write(res,200,new ApiResponse<>("SEGUIMIENTO_OFERTAS_OBTENIDO","Ofertas de la sede consultadas.",panel));
            else {req.setAttribute("panelOfertas",panel);req.getRequestDispatcher("/WEB-INF/views/admin/reasignaciones.jsp").forward(req,res);}
        } catch(IllegalArgumentException | java.time.DateTimeException e){error(req,res,400,"FILTRO_INVALIDO","Selecciona filtros válidos.");}
        catch(AgendaException e){error(req,res,e.status(),e.code(),e.getMessage());}
        catch(IOException | ServletException e){TransporteServlet.fallo(res);}
    }
    private static String parametro(HttpServletRequest req,String nombre) {
        String[] valores=req.getParameterValues(nombre);
        if(valores==null)return "";
        if(valores.length!=1 || valores[0]==null)throw new IllegalArgumentException("Parámetro repetido.");
        return valores[0];
    }
    private static int numero(String valor,int alternativa) {
        if(valor.isEmpty())return alternativa;
        if(!valor.matches("[0-9]{1,8}"))throw new IllegalArgumentException("Número inválido.");
        return Integer.parseInt(valor);
    }
    private static LocalDate fecha(String valor) {
        if(!valor.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))throw new IllegalArgumentException("Fecha inválida.");
        return LocalDate.parse(valor);
    }
    private static boolean json(HttpServletRequest req){String accept=req.getHeader("Accept");return accept!=null&&accept.contains("application/json");}
    private static void error(HttpServletRequest req,HttpServletResponse res,int estado,String codigo,String mensaje) {
        if(!json(req)){TransporteHttp.error(res,estado);return;}
        try{ApiJson.write(res,estado,new ApiResponse<>(codigo,mensaje,null));}catch(IOException e){TransporteServlet.fallo(res);}
    }
}
