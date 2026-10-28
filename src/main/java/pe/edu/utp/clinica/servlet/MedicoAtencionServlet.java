package pe.edu.utp.clinica.servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.dao.AgendaException;
import java.io.IOException;
import java.util.Map;
import pe.edu.utp.clinica.model.medico.ConsultaClinica;

@WebServlet(name="MedicoAtencionServlet",urlPatterns={"/medico/atencion"})
public class MedicoAtencionServlet extends MedicoPortalServlet {
    private static final String ID_CITA = "idCita";
    @Override protected String metodosPermitidos(){return "GET, POST";}
    @Override protected void mostrar(HttpServletRequest request,HttpServletResponse response,Contexto contexto)throws IOException,ServletException{
        String valor=request.getParameter(ID_CITA);
        Map<String,Object> detalle=Map.of();
        int id=-1;
        id=valor==null?citas.obtenerAtencionActivaMedico(contexto.medico()):positivo(valor);
        if(id>0){detalle=citas.obtenerDetalleCitaMedico(id,contexto.medico());if(detalle.isEmpty()) throw inexistente();}
        if(json(request)){ApiJson.write(response,200,new ApiResponse<>("ATENCION_MEDICA_OBTENIDA","Consulta completada.",detalle.isEmpty()?null:detalle));return;}
        request.setAttribute(ID_CITA,id);request.setAttribute("cita",detalle);
        request.getRequestDispatcher("/WEB-INF/views/medico/atencion.jsp").forward(request,response);
    }
    @Override protected void ejecutar(HttpServletRequest request,HttpServletResponse response,Contexto contexto)throws IOException{
        int id=positivo(request.getParameter(ID_CITA));
        String accion=request.getParameter("accion");
        boolean ok=switch(accion==null?"":accion){
            case "iniciar" -> citas.iniciarAtencionMedica(id,contexto.medico());
            case "guardar" -> citas.finalizarConsultaClinica(id,contexto.medico(),request.getParameter("prioridad"),request.getParameter("fechaProxima"),request.getParameter("indicaciones"),new ConsultaClinica(request.getParameter("motivoClinico"),request.getParameter("antecedentes"),request.getParameter("hallazgos"),request.getParameter("evaluacion"),request.getParameter("diagnostico"),request.getParameter("plan")));
            case "no_atender" -> citas.registrarNoAtencionMedica(id,contexto.medico(),request.getParameter("motivo"));
            case "no_asistio" -> throw new AgendaException(403,"ACCION_NO_PERMITIDA","La inasistencia corresponde a admisión. Para un paciente que llegó, registra No atendida con un motivo.");
            default -> throw new AgendaException(400,"ACCION_INVALIDA","La acción no es válida.");
        };
        if(!ok) throw inexistente();
        if(json(request)) ApiJson.write(response,200,new ApiResponse<>("ATENCION_REGISTRADA","El registro se guardó correctamente.",Map.of(ID_CITA,id)));
        else response.sendRedirect(request.getContextPath()+"/medico/atencion?idCita="+id);
    }
    private static AgendaException inexistente(){return new AgendaException(404,"CITA_NO_ENCONTRADA","No se encontró una cita asignada a este médico.");}
}
