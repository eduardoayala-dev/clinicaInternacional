package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.util.Html;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(name="MedicoExportServlet",urlPatterns={"/medico/export"})
public class MedicoExportServlet extends MedicoPortalServlet {
    private static final List<String> CAMPOS=List.of("idCita","fecha","hora","paciente","dni","estadoTexto","motivo");
    @Override protected void mostrar(HttpServletRequest request,HttpServletResponse response,Contexto contexto)throws IOException{
        String formato=request.getParameter("format");
        if(!"csv".equals(formato) && !"print".equals(formato)) throw new AgendaException(400,"FORMATO_INVALIDO","Elige CSV o la vista para imprimir.");
        var fecha=fecha(request);
        var agenda=citas.obtenerAgendaMedico(contexto.medico(),fecha);
        if("csv".equals(formato)){
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition","attachment; filename=\"agenda-medica-"+fecha+".csv\"");
            var out=response.getWriter();out.print('\ufeff');out.println("CITA;FECHA;HORA;PACIENTE;DOCUMENTO;ESTADO;MOTIVO");
            for(var cita:agenda) out.println(String.join(";",CAMPOS.stream().map(campo->csv(cita.get(campo))).toList()));
        }else imprimir(response,contexto.perfil(),agenda,fecha.toString());
    }
    static String csv(Object valor){
        String texto=java.util.Objects.toString(valor,"");
        String inicio=texto.stripLeading();
        if(!inicio.isEmpty() && "=+-@".indexOf(inicio.charAt(0))>=0) texto="'"+texto;
        if(texto.startsWith("\t") || texto.startsWith("\r") || texto.startsWith("\n")) texto="'"+texto;
        return "\""+texto.replace("\"","\"\"")+"\"";
    }
    private static void imprimir(HttpServletResponse response,Map<String,Object> perfil,List<Map<String,Object>> agenda,String fecha)throws IOException{
        response.setContentType("text/html;charset=UTF-8");
        var out=response.getWriter();
        out.print("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Agenda médica</title><style>body{font-family:Arial,sans-serif;padding:24px}table{border-collapse:collapse;width:100%}th,td{border:1px solid #ccc;padding:8px;text-align:left}@media print{button{display:none}}</style></head><body><button onclick=\"window.print()\">Imprimir / Guardar como PDF</button><h1>Agenda médica</h1><p>");
        out.print(Html.texto(perfil.get("nombreMedico"))+" · "+Html.texto(fecha));
        out.print("</p><table><thead><tr><th>Cita</th><th>Fecha</th><th>Hora</th><th>Paciente</th><th>Documento</th><th>Estado</th><th>Motivo</th></tr></thead><tbody>");
        for(var cita:agenda){out.print("<tr>");for(String campo:CAMPOS) out.print("<td>"+Html.texto(cita.get(campo))+"</td>");out.print("</tr>");}
        out.print("</tbody></table></body></html>");
    }
}
