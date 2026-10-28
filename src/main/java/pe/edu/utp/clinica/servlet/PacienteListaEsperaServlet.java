package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.dao.ListaEsperaDAO;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.model.Usuario;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;

@WebServlet(name="PacienteListaEsperaServlet",urlPatterns={"/paciente/lista-espera"})
public class PacienteListaEsperaServlet extends PacientePortalServlet {
    private transient CitaDAO citas;
    private transient ListaEsperaDAO lista;
    protected CitaDAO crearCitaDAO() { return new CitaDAO(); }
    protected ListaEsperaDAO crearListaDAO() { return new ListaEsperaDAO(); }
    @Override public void init() { citas=crearCitaDAO(); lista=crearListaDAO(); }
    @Override protected String ruta() { return "/paciente/lista-espera"; }

    @Override protected void mostrar(HttpServletRequest request,HttpServletResponse response,Usuario usuario) throws IOException,ServletException {
        if(request.getParameter("action")!=null) throw new AgendaException(405,"METODO_NO_PERMITIDO","Utiliza POST para modificar la lista de espera.");
        int paciente=identidad(usuario);
        var estado=lista.obtenerEstadoListaEspera(paciente);
        if(json(request)) {
            ApiJson.write(response,200,new ApiResponse<>("LISTA_ESPERA_OBTENIDA","Consulta completada.",estado));
            return;
        }
        request.setAttribute("listaEspecialidades",citas.obtenerEspecialidades());
        request.setAttribute("listaSedes",citas.obtenerSedes());
        request.setAttribute("listaMedicos",citas.obtenerMedicosParaEspera());
        request.setAttribute("estadoListaEspera",estado);
        request.setAttribute("numNotifSinLeer",citas.contarNotificacionesSinLeer(usuario.getIdUsuario()));
        request.setAttribute("nombrePaciente",usuario.getNombreCompleto());
        flash(request);
        request.getRequestDispatcher("/WEB-INF/views/paciente/lista_espera.jsp").forward(request,response);
    }

    @Override protected void ejecutar(HttpServletRequest request,HttpServletResponse response,Usuario usuario) throws IOException {
        int paciente=identidad(usuario);
        String accion=request.getParameter("action");
        if("retirar".equals(accion)) {
            int id=positivo(request.getParameter("idListaEspera"));
            lista.retirarListaEspera(id,paciente);
            resultado(request,response,200,new ApiResponse<>("INSCRIPCION_RETIRADA","Te retiraste de la lista de espera.",Map.of("idListaEspera",id)),ruta());
            return;
        }
        if(accion!=null && !"inscribir".equals(accion)) throw new AgendaException(400,"ACCION_INVALIDA","La acción no es válida.");
        int especialidad=positivo(request.getParameter("especialidadId"));
        String sede=request.getParameter("sede");
        Integer sedeId="cualquiera".equals(sede)?null:positivo(sede);
        Set<String> horarios=seleccion(request,"horarios",Set.of("manana","tarde"));
        String rango=horarios.size()==2?"INDIFERENTE":horarios.contains("manana")?"MANANA":"TARDE";
        Set<String> canales=seleccion(request,"canales",Set.of("whatsapp","email","sms"));
        String contacto=String.join(",",canales.stream().map(c->switch(c){case "email"->"CORREO";case "sms"->"SMS";default->"WHATSAPP";}).toList());
        String aviso=request.getParameter("disponibilidad");
        if(!Set.of("3h","12h").contains(aviso==null?"":aviso)) throw new AgendaException(400,"DATOS_INVALIDOS","Selecciona la anticipación del aviso.");
        String medico=request.getParameter("medicoId");
        Integer medicoId=medico==null || medico.isBlank()?null:positivo(medico);
        int id=lista.registrar(paciente,especialidad,sedeId,rango,"MEDIA",contacto,"12h".equals(aviso)?720:180,medicoId);
        resultado(request,response,201,new ApiResponse<>("INSCRIPCION_REGISTRADA","Inscripción registrada. Revisa tus ofertas en este portal.",Map.of("idListaEspera",id)),ruta());
    }

    private int identidad(Usuario usuario) {
        int paciente=citas.obtenerIdPacientePorUsuarioId(usuario.getIdUsuario());
        if(paciente<=0) throw new AgendaException(403,"PACIENTE_NO_HABILITADO","No se encontró un paciente habilitado para esta sesión.");
        return paciente;
    }
    private static Set<String> seleccion(HttpServletRequest request,String campo,Set<String> permitidos) {
        String[] values=request.getParameterValues(campo);
        if(values==null || values.length==0) throw new AgendaException(400,"DATOS_INVALIDOS","Selecciona una preferencia de horario y contacto.");
        Set<String> data=new TreeSet<>(Arrays.asList(values));
        if(!permitidos.containsAll(data)) throw new AgendaException(400,"DATOS_INVALIDOS","Las preferencias no son válidas.");
        return data;
    }
}
