package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import pe.edu.utp.clinica.dao.*;
import pe.edu.utp.clinica.catalogo.SedesExcel;
import pe.edu.utp.clinica.model.Usuario;
import java.io.IOException;
import java.util.List;

@WebServlet("/sysadmin/catalogo-sedes")
@MultipartConfig(maxFileSize=1_048_576,maxRequestSize=1_200_000)
public class CatalogoSedesServlet extends HttpServlet {
    protected CatalogoSedesDAO dao(){return new CatalogoSedesDAO();}
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res){
        res.setHeader("Cache-Control","no-store");
        try{
            int actor=actor(req);var lista=dao().listar(actor);String accion=req.getParameter("accion");
            if("plantilla".equals(accion)||"exportar".equals(accion)){
                byte[] bytes=SedesExcel.escribir("plantilla".equals(accion)?List.of():lista);
                res.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");res.setHeader("Content-Disposition","attachment; filename=sedes.xlsx");res.getOutputStream().write(bytes);return;
            }
            req.setAttribute("sedesCatalogo",lista);
            req.getRequestDispatcher("/WEB-INF/views/sysadmin/catalogo-sedes.jsp").forward(req,res);
        }catch(AgendaException e){error(req,res,e);}
        catch(IOException|jakarta.servlet.ServletException e){pe.edu.utp.clinica.util.TransporteServlet.fallo(res);}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res){
        try{
            int actor=actor(req);
            if("importar".equals(req.getParameter("accion"))){
                var archivo=req.getPart("archivo");if(archivo==null||archivo.getSize()>1_048_576)throw new AgendaException(422,"EXCEL_INVALIDO","Selecciona un XLSX de hasta 1 MB.");
                List<CatalogoSedesDAO.Fila> filas;
                try(var entrada=archivo.getInputStream()){filas=SedesExcel.leer(entrada.readNBytes(1_048_577));}finally{archivo.delete();}
                var resultado=dao().importar(actor,filas,req.getRemoteAddr());
                req.getSession(false).setAttribute("resultadoSedes",resultado);
                req.getSession(false).setAttribute("mensajeSedes","Importación procesada. Revisa el resultado de cada fila.");
            }else if("guardar".equals(req.getParameter("accion"))){
                String raw=req.getParameter("id");int id=raw==null||raw.isBlank()?0:Integer.parseInt(raw);
                if(id<0)throw new NumberFormatException();
                dao().guardar(actor,new CatalogoSedesDAO.Sede(id,id==0?null:req.getParameter("codigo"),req.getParameter("nombre"),req.getParameter("direccion"),req.getParameter("telefono"),req.getParameter("estado")),req.getRemoteAddr());
                req.getSession(false).setAttribute("mensajeSedes","Sede guardada correctamente.");
            }else throw new AgendaException(400,"ACCION_INVALIDA","Selecciona una acción válida.");
            res.setStatus(303);res.setHeader("Location",req.getContextPath()+"/sysadmin/catalogo-sedes");
        }catch(AgendaException e){error(req,res,e);}
        catch(NumberFormatException|IllegalStateException e){error(req,res,new AgendaException(422,"DATOS_INVALIDOS","Revisa el identificador o el tamaño del archivo."));}
        catch(IOException|jakarta.servlet.ServletException e){pe.edu.utp.clinica.util.TransporteServlet.fallo(res);}
    }
    private static int actor(HttpServletRequest req){var s=req.getSession(false);if(s==null||!(s.getAttribute("usuarioLogueado") instanceof Usuario u)||!"ADMINISTRADOR".equals(u.getRol()))throw new AgendaException(403,"ADMINISTRADOR_REQUERIDO","Inicia sesión como administrador.");return u.getIdUsuario();}
    private static void error(HttpServletRequest req,HttpServletResponse res,AgendaException e){try{if(e.status()==403){res.sendError(403);return;}req.getSession(false).setAttribute("errorSedes",e.getMessage());res.setStatus(303);res.setHeader("Location",req.getContextPath()+"/sysadmin/catalogo-sedes");}catch(IOException ex){pe.edu.utp.clinica.util.TransporteServlet.fallo(res);}}
}
