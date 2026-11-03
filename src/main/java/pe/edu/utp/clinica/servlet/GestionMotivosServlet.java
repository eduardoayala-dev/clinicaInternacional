package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.GestionMotivosDAO;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.util.TransporteServlet;
import java.io.IOException;

@WebServlet({"/sysadmin/motivos","/admin/motivos"})
public class GestionMotivosServlet extends HttpServlet {
    private static final String ESTADO="estado";
    protected GestionMotivosDAO.Panel consultar(int actor,GestionMotivosDAO.Filtro filtro){return new GestionMotivosDAO().consultar(actor,filtro);}
    protected int guardar(int actor,int id,int especialidad,String descripcion,String ip){return new GestionMotivosDAO().guardar(actor,id,especialidad,descripcion,ip);}
    protected void cambiarEstado(int actor,int id,String estado,String ip){new GestionMotivosDAO().estado(actor,id,estado,ip);}
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res){
        res.setHeader("Cache-Control","no-store");
        try{var usuario=usuario(req);mostrar(req,res,usuario.getIdUsuario());}
        catch(AgendaException e){error(req,res,e);}
        catch(IOException|ServletException e){TransporteServlet.fallo(res);}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res){
        res.setHeader("Cache-Control","no-store");
        try{
            var usuario=usuario(req);
            String accion=parametro(req,"accion");
            int id=numero(parametro(req,"id"),0);
            switch(accion){
                case "guardar" -> id=guardar(usuario.getIdUsuario(),id,numero(parametro(req,"especialidad"),0),parametro(req,"descripcion"),req.getRemoteAddr());
                case ESTADO -> cambiarEstado(usuario.getIdUsuario(),id,parametro(req,ESTADO),req.getRemoteAddr());
                default -> throw new AgendaException(400,"ACCION_INVALIDA","Selecciona una acción válida.");
            }
            if(json(req))ApiJson.write(res,200,new ApiResponse<>("MOTIVO_GUARDADO","El catálogo fue actualizado.",java.util.Map.of("idMotivo",id)));
            else{req.getSession(false).setAttribute("mensajeMotivos","El catálogo fue actualizado.");res.setStatus(303);res.setHeader("Location",req.getContextPath()+req.getServletPath());}
        }catch(AgendaException e){error(req,res,e);}
        catch(IOException e){TransporteServlet.fallo(res);}
    }
    private void mostrar(HttpServletRequest req,HttpServletResponse res,int actor)throws IOException,ServletException {
        var filtro=new GestionMotivosDAO.Filtro(numero(parametro(req,"especialidad"),0),parametro(req,ESTADO),parametro(req,"buscar"),numero(parametro(req,"pagina"),1));
        var panel=consultar(actor,filtro);
        if(json(req)){ApiJson.write(res,200,new ApiResponse<>("MOTIVOS_OBTENIDOS","Catálogo de motivos consultado.",panel));return;}
        req.setAttribute("panelMotivos",panel);
        req.getRequestDispatcher("/WEB-INF/views/catalogo/motivos.jsp").forward(req,res);
    }
    private static Usuario usuario(HttpServletRequest req){
        var sesion=req.getSession(false);
        if(sesion==null||!(sesion.getAttribute("usuarioLogueado") instanceof Usuario u))throw new AgendaException(403,"SESION_REQUERIDA","Inicia sesión para continuar.");
        String rol=u.getRol();
        boolean sistema="/sysadmin/motivos".equals(req.getServletPath());
        if((sistema&&!"ADMINISTRADOR".equals(rol))||(!sistema&&!"PERSONAL_ADMINISTRATIVO".equals(rol)))
            throw new AgendaException(403,"CATALOGO_NO_AUTORIZADO","Ingresa con un perfil autorizado para gestionar el catálogo.");
        return u;
    }
    private static String parametro(HttpServletRequest req,String nombre){
        String[] valores=req.getParameterValues(nombre);
        if(valores==null)return "";
        if(valores.length!=1||valores[0]==null)throw new AgendaException(400,"PARAMETRO_INVALIDO","La solicitud contiene parámetros repetidos o inválidos.");
        return valores[0];
    }
    private static int numero(String valor,int alternativa){
        if(valor.isEmpty())return alternativa;
        if(!valor.matches("\\d{1,8}"))throw new AgendaException(400,"PARAMETRO_INVALIDO","Selecciona identificadores y páginas válidos.");
        return Integer.parseInt(valor);
    }
    private static boolean json(HttpServletRequest req){String accept=req.getHeader("Accept");return accept!=null&&accept.contains("application/json");}
    private static void error(HttpServletRequest req,HttpServletResponse res,AgendaException e){
        try{
            if(json(req))ApiJson.write(res,e.status(),new ApiResponse<>(e.code(),e.getMessage(),null));
            else{res.setStatus(e.status());req.setAttribute("mensajeCatalogo",e.getMessage());req.getRequestDispatcher("/WEB-INF/views/catalogo/error.jsp").forward(req,res);}
        }catch(IOException|ServletException ex){TransporteServlet.fallo(res);}
    }
}
