package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.PadronMedicosDAO;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.util.TransporteServlet;
import java.io.IOException;
import java.util.List;

@WebServlet({"/admin/medicos/exportar","/admin/medicos/editar"})
public class PadronMedicosServlet extends HttpServlet {
    protected List<PadronMedicosDAO.Medico> listar(int actor,PadronMedicosDAO.Filtro filtro) { return new PadronMedicosDAO().listar(actor,filtro); }
    protected PadronMedicosDAO.Edicion consultar(int actor,int id) { return new PadronMedicosDAO().consultar(actor,id); }
    protected void guardar(int actor,int id,int especialidad,String estado,String ip) { new PadronMedicosDAO().guardar(actor,id,especialidad,estado,ip); }
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res) {
        res.setHeader("Cache-Control","no-store");
        try {
            int actor=actor(req);
            if(req.getServletPath().endsWith("/exportar")) {
                var filtro=new PadronMedicosDAO.Filtro(parametro(req,"query"),parametro(req,"especialidad"),parametro(req,"sede"),parametro(req,"estado"));
                var medicos=listar(actor,filtro);
                try(var libro=excel(medicos)) {
                    res.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    res.setHeader("Content-Disposition","attachment; filename=medicos.xlsx");
                    libro.write(res.getOutputStream());
                }
            } else {
                mostrar(req,res,consultar(actor,numero(req,"id")));
            }
        } catch(AgendaException e) { error(req,res,e); }
        catch(IOException|ServletException e) { TransporteServlet.fallo(res); }
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res) {
        res.setHeader("Cache-Control","no-store");
        try {
            int actor=actor(req);
            if(!req.getServletPath().endsWith("/editar")) throw new AgendaException(405,"METODO_NO_PERMITIDO","La exportación solo admite consultas.");
            int id=numero(req,"id");
            guardar(actor,id,numero(req,"especialidad"),parametro(req,"estado"),req.getRemoteAddr());
            if(json(req)) ApiJson.write(res,200,new ApiResponse<>("MEDICO_ACTUALIZADO","Datos profesionales actualizados.",java.util.Map.of("idMedico",id)));
            else {
                req.getSession(false).setAttribute("flashMessage","Datos profesionales actualizados.");
                res.setStatus(303);res.setHeader("Location",req.getContextPath()+"/admin/medicos");
            }
        } catch(AgendaException e) { error(req,res,e); }
        catch(IOException e) { TransporteServlet.fallo(res); }
    }
    static XSSFWorkbook excel(List<PadronMedicosDAO.Medico> medicos) {
        var libro=new XSSFWorkbook();var hoja=libro.createSheet("Médicos");
        String[] columnas={"CMP","Médico","Especialidad","Sede","Estado"};
        var cabecera=hoja.createRow(0);var estilo=libro.createCellStyle();var fuente=libro.createFont();fuente.setBold(true);estilo.setFont(fuente);
        for(int i=0;i<columnas.length;i++) { var celda=cabecera.createCell(i);celda.setCellValue(columnas[i]);celda.setCellStyle(estilo); }
        int indice=1;
        for(var medico:medicos) {
            var fila=hoja.createRow(indice++);
            String[] valores={medico.cmp(),medico.nombre(),medico.especialidad(),medico.sede(),medico.estado()};
            for(int i=0;i<valores.length;i++) fila.createCell(i).setCellValue(valores[i]);
        }
        hoja.createFreezePane(0,1);hoja.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,Math.max(0,indice-1),0,4));
        int[] anchos={20,38,30,30,18};for(int i=0;i<anchos.length;i++) hoja.setColumnWidth(i,anchos[i]*256);
        return libro;
    }
    private static int actor(HttpServletRequest req) {
        var sesion=req.getSession(false);
        if(sesion==null || !(sesion.getAttribute("usuarioLogueado") instanceof Usuario u) || !"PERSONAL_ADMINISTRATIVO".equals(u.getRol()))
            throw new AgendaException(403,"ADMISION_REQUERIDA","Inicia sesión con una cuenta de admisión.");
        return u.getIdUsuario();
    }
    private static int numero(HttpServletRequest req,String nombre) {
        String valor=parametro(req,nombre);
        if(!valor.matches("\\d{1,8}") || Integer.parseInt(valor)<1) throw new AgendaException(400,"ID_INVALIDO","Selecciona un registro válido.");
        return Integer.parseInt(valor);
    }
    private static String parametro(HttpServletRequest req,String nombre) {
        String[] valores=req.getParameterValues(nombre);
        if(valores==null) return "";
        if(valores.length!=1 || valores[0]==null) throw new AgendaException(400,"PARAMETRO_INVALIDO","La solicitud contiene parámetros repetidos.");
        return valores[0];
    }
    private static void mostrar(HttpServletRequest req,HttpServletResponse res,PadronMedicosDAO.Edicion edicion)throws IOException,ServletException {
        if(json(req)) { ApiJson.write(res,200,new ApiResponse<>("MEDICO_OBTENIDO","Datos profesionales consultados.",edicion));return; }
        req.setAttribute("edicionMedico",edicion);req.getRequestDispatcher("/WEB-INF/views/admin/editar-medico.jsp").forward(req,res);
    }
    private static boolean json(HttpServletRequest req) { String aceptar=req.getHeader("Accept");return aceptar!=null && aceptar.contains("application/json"); }
    private static void error(HttpServletRequest req,HttpServletResponse res,AgendaException e) {
        try {
            if(json(req)) ApiJson.write(res,e.status(),new ApiResponse<>(e.code(),e.getMessage(),null));
            else { res.setStatus(e.status());req.setAttribute("errorMedico",e.getMessage());req.getRequestDispatcher("/WEB-INF/views/admin/editar-medico.jsp").forward(req,res); }
        } catch(IOException|ServletException ex) { TransporteServlet.fallo(res); }
    }
}
