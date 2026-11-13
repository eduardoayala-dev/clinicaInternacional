package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.edu.utp.clinica.api.*;
import pe.edu.utp.clinica.dao.*;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.reporte.*;
import java.io.IOException;
import java.time.*;
import java.util.Set;

@WebServlet(name="ExportServlet",urlPatterns="/admin/export")
public class ExportServlet extends HttpServlet {
    protected ReporteAdmisionDAO reportes(){return new ReporteAdmisionDAO();}
    protected Clock reloj(){return Clock.system(ZoneId.of("America/Lima"));}
    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response) {
        try{exportar(request,response);}
        catch(IOException e){java.util.logging.Logger.getLogger(ExportServlet.class.getName()).log(java.util.logging.Level.WARNING,"No se pudo entregar el reporte.",e);if(!response.isCommitted())response.setStatus(500);}
    }
    private void exportar(HttpServletRequest request,HttpServletResponse response)throws IOException {
        var sesion=request.getSession(false);
        Usuario usuario=sesion==null?null:(Usuario)sesion.getAttribute("usuarioLogueado");
        if(usuario==null || !"PERSONAL_ADMINISTRATIVO".equals(usuario.getRol())){ApiJson.write(response,403,new ApiResponse<>("ACCESO_DENEGADO","Ingresa con una cuenta de admisión.",null));return;}
        try {
            String formato=request.getParameter("format");if("excel".equals(formato))formato="xlsx";
            if(formato==null || !Set.of("csv","pdf","xlsx").contains(formato))throw new AgendaException(400,"FORMATO_INVALIDO","Elige CSV, Excel o PDF.");
            var filtro=FiltroReporte.leer(request.getParameter("desde"),request.getParameter("hasta"),request.getParameter("estado"),request.getParameter("type"),reloj());
            var reporte=reportes().consultar(usuario.getIdUsuario(),filtro,1,true);
            byte[] archivo=switch(formato){case "csv" -> ExportacionAdmision.csv(reporte);case "xlsx" -> ExportacionAdmision.excel(reporte);default -> ExportacionAdmision.pdf(reporte);};
            response.setContentType(switch(formato){case "csv" -> "text/csv;charset=UTF-8";case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";default -> "application/pdf";});
            response.setHeader("Content-Disposition","attachment; filename=\"admision-"+filtro.tipo()+"-"+filtro.desde()+"-"+filtro.hasta()+"."+formato+"\"");
            response.setHeader("Cache-Control","no-store");response.setHeader("X-Content-Type-Options","nosniff");response.setContentLength(archivo.length);response.getOutputStream().write(archivo);
        } catch(AgendaException e){ApiJson.write(response,e.status(),new ApiResponse<>(e.code(),e.getMessage(),null));}
    }
}
