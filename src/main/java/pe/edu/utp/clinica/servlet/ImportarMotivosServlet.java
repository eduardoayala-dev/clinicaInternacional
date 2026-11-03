package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.catalogo.ConfirmacionImportacionMotivos;
import pe.edu.utp.clinica.catalogo.ImportacionMotivosExcel;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.GestionMotivosDAO;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.model.catalogo.FilaImportacionMotivo;
import pe.edu.utp.clinica.util.TransporteServlet;
import java.io.IOException;
import java.time.Clock;
import java.util.List;

@WebServlet({"/sysadmin/motivos/importar","/admin/motivos/importar"})
@MultipartConfig(maxFileSize=1_048_576,maxRequestSize=1_200_000,fileSizeThreshold=0)
public class ImportarMotivosServlet extends HttpServlet {
    private static final String RESULTADO="resultadoImportacionMotivos";
    private record ResultadoVisible(int actor,GestionMotivosDAO.ResultadoImportacion datos,java.time.Instant vence) {}
    protected Clock reloj(){return Clock.systemUTC();}
    protected List<GestionMotivosDAO.Especialidad> especialidades(int actor){return new GestionMotivosDAO().especialidadesImportacion(actor);}
    protected List<FilaImportacionMotivo> revisar(int actor,List<FilaImportacionMotivo> filas){return new GestionMotivosDAO().revisarImportacion(actor,filas);}
    protected GestionMotivosDAO.ResultadoImportacion guardar(int actor,List<FilaImportacionMotivo> filas,String ip){return new GestionMotivosDAO().importar(actor,filas,ip);}
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res){
        res.setHeader("Cache-Control","no-store");
        try{
            var usuario=usuario(req);var catalogo=especialidades(usuario.getIdUsuario());
            if("plantilla".equals(parametro(req,"accion"))){
                byte[] bytes=ImportacionMotivosExcel.plantilla(catalogo);res.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");res.setHeader("Content-Disposition","attachment; filename=plantilla-motivos.xlsx");res.getOutputStream().write(bytes);return;
            }
            Object dato=req.getSession(false).getAttribute(RESULTADO);
            if("1".equals(parametro(req,"resultado")) && dato instanceof ResultadoVisible vista && vista.actor()==usuario.getIdUsuario() && reloj().instant().isBefore(vista.vence())){
                req.setAttribute("importacionTerminada",Boolean.TRUE);req.setAttribute("importados",vista.datos().importados());
                mostrar(req,res,vista.datos().filas(),"");
            }else mostrar(req,res,List.of(),"");
        }catch(AgendaException e){error(req,res,e);}catch(IOException|ServletException e){TransporteServlet.fallo(res);}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res){
        res.setHeader("Cache-Control","no-store");
        try{
            var usuario=usuario(req);
            switch(parametro(req,"accion")){
                case "previsualizar" -> previsualizar(req,res,usuario.getIdUsuario());
                case "confirmar" -> confirmar(req,res,usuario.getIdUsuario());
                default -> throw new AgendaException(400,"ACCION_INVALIDA","Selecciona revisar o confirmar el archivo.");
            }
        }catch(AgendaException e){error(req,res,e);}catch(IOException|ServletException e){TransporteServlet.fallo(res);}
    }
    private void previsualizar(HttpServletRequest req,HttpServletResponse res,int actor)throws IOException,ServletException {
        req.getSession(false).removeAttribute(RESULTADO);
        ConfirmacionImportacionMotivos.descartar(req.getSession(false));
        var filas=revisar(actor,ImportacionMotivosExcel.leer(leerArchivo(req)));
        boolean algunaValida=filas.stream().anyMatch(f->f.error().isEmpty());
        String token=!algunaValida?"":ConfirmacionImportacionMotivos.preparar(req.getSession(false),actor,filas,reloj()).token();
        mostrar(req,res,filas,token);
    }
    private void confirmar(HttpServletRequest req,HttpServletResponse res,int actor)throws IOException {
        var pendiente=ConfirmacionImportacionMotivos.consumir(req.getSession(false),actor,parametro(req,"confirmacion"),reloj());
        var resultado=guardar(actor,pendiente.filas(),req.getRemoteAddr());
        if(json(req))ApiJson.write(res,200,new ApiResponse<>("IMPORTACION_PROCESADA","Consulta el resultado de cada fila.",resultado));
        else{req.getSession(false).setAttribute(RESULTADO,new ResultadoVisible(actor,resultado,reloj().instant().plus(java.time.Duration.ofMinutes(10))));res.setStatus(303);res.setHeader("Location",req.getContextPath()+req.getServletPath()+"?resultado=1");}
    }
    protected byte[] leerArchivo(HttpServletRequest req)throws IOException {
        String tipo=req.getContentType();
        if(tipo==null||!tipo.toLowerCase(java.util.Locale.ROOT).startsWith("multipart/form-data"))throw new AgendaException(415,"CARGA_MULTIPART_REQUERIDA","Carga el archivo mediante el formulario de importación.");
        java.util.List<Part> partes;
        try{partes=req.getParts().stream().filter(p->"archivo".equals(p.getName())).toList();}
        catch(IllegalStateException e){throw new AgendaException(413,"ARCHIVO_DEMASIADO_GRANDE","Selecciona un archivo XLSX de hasta 1 MB.");}
        catch(ServletException e){throw new AgendaException(422,"ARCHIVO_INVALIDO","No se pudo interpretar la carga. Selecciona nuevamente un archivo XLSX.");}
        if(partes.size()!=1)throw new AgendaException(422,"ARCHIVO_INVALIDO","Selecciona un único archivo XLSX.");
        var archivo=partes.get(0);
        try{
            String nombre=archivo.getSubmittedFileName();
            if(nombre==null||!nombre.toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")||archivo.getSize()==0||archivo.getSize()>ImportacionMotivosExcel.MAX_BYTES)throw new AgendaException(422,"ARCHIVO_INVALIDO","Selecciona un archivo XLSX de hasta 1 MB.");
            try(var entrada=archivo.getInputStream()){return entrada.readNBytes(ImportacionMotivosExcel.MAX_BYTES+1);}
        }finally{archivo.delete();}
    }
    private static void mostrar(HttpServletRequest req,HttpServletResponse res,List<FilaImportacionMotivo> filas,String token)throws IOException,ServletException {
        if(json(req)){
            if(Boolean.TRUE.equals(req.getAttribute("importacionTerminada")))ApiJson.write(res,200,new ApiResponse<>("IMPORTACION_RESULTADO","Resultado de la importación.",java.util.Map.of("filas",filas,"importados",req.getAttribute("importados"))));
            else ApiJson.write(res,200,new ApiResponse<>("IMPORTACION_REVISADA","Vista previa; todavía no se guardaron motivos.",java.util.Map.of("filas",filas,"confirmacion",token)));
            return;
        }
        req.setAttribute("filasImportacion",filas);req.setAttribute("confirmacionImportacion",token);req.getRequestDispatcher("/WEB-INF/views/catalogo/importar-motivos.jsp").forward(req,res);
    }
    private static Usuario usuario(HttpServletRequest req){
        var sesion=req.getSession(false);
        if(sesion==null||!(sesion.getAttribute("usuarioLogueado") instanceof Usuario usuario))throw noAutorizado();
        boolean sistema=req.getServletPath().startsWith("/sysadmin/");
        if(sistema&&!"ADMINISTRADOR".equals(usuario.getRol()))throw noAutorizado();
        if(!sistema&&!"PERSONAL_ADMINISTRATIVO".equals(usuario.getRol()))throw noAutorizado();
        return usuario;
    }
    private static AgendaException noAutorizado(){return new AgendaException(403,"IMPORTACION_NO_AUTORIZADA","Ingresa con un perfil autorizado para gestionar motivos.");}
    private static String parametro(HttpServletRequest req,String nombre){String[] valores=req.getParameterValues(nombre);if(valores==null)return "";if(valores.length!=1||valores[0]==null)throw new AgendaException(400,"PARAMETRO_INVALIDO","La solicitud contiene parámetros repetidos o inválidos.");return valores[0];}
    private static boolean json(HttpServletRequest req){String a=req.getHeader("Accept");return a!=null&&a.contains("application/json");}
    private static void error(HttpServletRequest req,HttpServletResponse res,AgendaException e){try{if(json(req))ApiJson.write(res,e.status(),new ApiResponse<>(e.code(),e.getMessage(),null));else{res.setStatus(e.status());req.setAttribute("mensajeCatalogo",e.getMessage());req.getRequestDispatcher("/WEB-INF/views/catalogo/error.jsp").forward(req,res);}}catch(IOException|ServletException ex){TransporteServlet.fallo(res);}}
}
