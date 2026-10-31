package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.dao.OfertasDAO;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.model.Usuario;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet(name="PacienteNotificacionesServlet",urlPatterns={"/paciente/notificaciones"})
public class PacienteNotificacionesServlet extends PacientePortalServlet {
    private static final String ID_OFERTA = "idOferta";
    private transient CitaDAO citas;
    private transient OfertasDAO ofertas;
    protected CitaDAO crearCitaDAO() { return new CitaDAO(); }
    protected OfertasDAO crearOfertasDAO() { return new OfertasDAO(); }
    @Override public void init() { citas=crearCitaDAO(); ofertas=crearOfertasDAO(); }
    @Override protected String ruta() { return "/paciente/notificaciones"; }

    @Override protected void mostrar(HttpServletRequest request,HttpServletResponse response,Usuario usuario) throws IOException,ServletException {
        if(request.getParameter("action")!=null || request.getParameter("accion")!=null) throw new AgendaException(405,"METODO_NO_PERMITIDO","Utiliza POST para modificar notificaciones y ofertas.");
        int paciente=identidad(usuario);
        var oferta=ofertas.obtenerParaPaciente(paciente);
        var notificaciones=citas.obtenerNotificacionesUsuario(usuario.getIdUsuario());
        int sinLeer=citas.contarNotificacionesSinLeer(usuario.getIdUsuario());
        boolean vencida=oferta.isEmpty() && ofertas.tieneOfertaVencida(paciente);
        if(json(request)) {
            Map<String,Object> data=new LinkedHashMap<>();
            data.put("notificaciones",notificaciones); data.put("sinLeer",sinLeer); data.put("oferta",oferta.isEmpty()?null:oferta); data.put("ofertaVencida",vencida);
            ApiJson.write(response,200,new ApiResponse<>("NOTIFICACIONES_OBTENIDAS","Consulta completada.",data));
            return;
        }
        request.setAttribute("nombrePaciente",usuario.getNombreCompleto());
        request.setAttribute("listaNotificaciones",notificaciones);
        request.setAttribute("totalNotificaciones",notificaciones.size());
        request.setAttribute("unreadCount",sinLeer);
        request.setAttribute("tieneNotificacionesSinLeer",sinLeer>0);
        request.setAttribute("ofertaCupo",oferta);
        request.setAttribute("ofertaExpirada",vencida);
        flash(request);
        request.getRequestDispatcher("/WEB-INF/views/paciente/notificaciones.jsp").forward(request,response);
    }

    @Override protected void ejecutar(HttpServletRequest request,HttpServletResponse response,Usuario usuario) throws IOException {
        int paciente=identidad(usuario);
        String accion=request.getParameter("accion");
        String action=request.getParameter("action");
        if(accion!=null && action!=null) throw new AgendaException(400,"ACCION_INVALIDA","Envía una sola acción.");
        if(accion==null) {
            notificacion(request,response,usuario,action);
            return;
        }
        if("aceptar".equals(accion)) {
            int id=positivo(request.getParameter(ID_OFERTA));
            int cita=ofertas.aceptar(paciente,id);
            resultado(request,response,200,new ApiResponse<>("OFERTA_ACEPTADA","La oferta se procesó. Revisa la cita asociada en Mis citas.",Map.of(ID_OFERTA,id,"idCita",cita)),"/paciente/historial");
        } else if("rechazar".equals(accion)) {
            int id=positivo(request.getParameter(ID_OFERTA));
            ofertas.rechazar(paciente,id);
            resultado(request,response,200,new ApiResponse<>("OFERTA_RECHAZADA","Oferta rechazada. Conservas tu inscripción y antigüedad.",Map.of(ID_OFERTA,id)),ruta());
        } else if("confirmarCita".equals(accion)) {
            marcar(request,response,usuario,positivo(request.getParameter("idNotificacion")),true);
        } else throw new AgendaException(400,"ACCION_INVALIDA","La acción no es válida.");
    }

    private void notificacion(HttpServletRequest request,HttpServletResponse response,Usuario usuario,String action) throws IOException {
        if("marcarLeidas".equals(action)) {
            if(!citas.marcarNotificacionesComoLeidas(usuario.getIdUsuario())) throw inexistente();
            resultado(request,response,200,new ApiResponse<>("NOTIFICACIONES_LEIDAS","Notificaciones marcadas como leídas.",null),ruta());
        } else if("marcarLeida".equals(action) || "confirmar".equals(action)) {
            marcar(request,response,usuario,positivo(request.getParameter("id")),"confirmar".equals(action));
        } else throw new AgendaException(400,"ACCION_INVALIDA","La acción no es válida.");
    }

    private void marcar(HttpServletRequest request,HttpServletResponse response,Usuario usuario,int id,boolean confirmar) throws IOException {
        boolean ok=confirmar?citas.confirmarAsistenciaDesdeNotificacion(id,usuario.getIdUsuario()):citas.marcarNotificacionIndividualLeida(id,usuario.getIdUsuario());
        if(!ok) throw inexistente();
        resultado(request,response,200,new ApiResponse<>(confirmar?"ASISTENCIA_CONFIRMADA":"NOTIFICACION_LEIDA",confirmar?"Asistencia confirmada. Tu cita continúa programada.":"Notificación marcada como leída.",Map.of("idNotificacion",id)),ruta());
    }
    private int identidad(Usuario usuario) {
        int paciente=citas.obtenerIdPacientePorUsuarioId(usuario.getIdUsuario());
        if(paciente<=0) throw new AgendaException(403,"PACIENTE_NO_HABILITADO","No se encontró un paciente habilitado para esta sesión.");
        return paciente;
    }
    private static AgendaException inexistente() { return new AgendaException(404,"NOTIFICACION_NO_ENCONTRADA","No se encontró una notificación propia que permita esa acción."); }
}
