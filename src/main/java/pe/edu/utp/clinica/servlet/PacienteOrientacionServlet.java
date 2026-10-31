package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.CatalogoOrientacionDAO;
import pe.edu.utp.clinica.ia.GeminiCatalogo;
import pe.edu.utp.clinica.ia.OrientacionCatalogo;
import pe.edu.utp.clinica.model.Usuario;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet(name="PacienteOrientacionServlet",urlPatterns="/paciente/orientacion")
public class PacienteOrientacionServlet extends HttpServlet {
    private transient OrientacionCatalogo servicio;
    private boolean gemini;
    @Override public void init() {servicio=crearServicio();}
    protected OrientacionCatalogo crearServicio() {
        gemini=Boolean.parseBoolean(System.getenv().getOrDefault("GEMINI_ORIENTATION_ENABLED","false"));
        if (!gemini) return new OrientacionCatalogo(null,false);
        try {return new OrientacionCatalogo(new GeminiCatalogo(System.getenv("GEMINI_API_KEY"),System.getenv("GEMINI_MODEL")),true);}
        catch (IllegalArgumentException e) {return null;}
    }
    protected List<OrientacionCatalogo.Especialidad> catalogo() throws SQLException {
        return new CatalogoOrientacionDAO().activas();
    }
    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response) throws IOException {
        response.setHeader("Allow","POST");
        ApiJson.write(response,405,new ApiResponse<>("METODO_NO_PERMITIDO","Utiliza POST para consultar un caso ficticio.",null));
    }
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response) throws IOException {
        try {
            var session=request.getSession(false);
            Usuario usuario=session==null?null:(Usuario)session.getAttribute("usuarioLogueado");
            if (usuario==null || !"PACIENTE".equals(usuario.getRol())) throw new AgendaException(403,"ACCESO_DENEGADO","Acceso exclusivo para pacientes.");
            if(servicio==null) throw new AgendaException(503,"ORIENTACION_NO_DISPONIBLE","El proveedor no está configurado. Puedes usar la búsqueda manual.");
            if (gemini) limitar(request);
            var resultado=servicio.orientar(request.getParameter("relato"),"true".equals(request.getParameter("casoFicticio")),catalogo());
            ApiJson.write(response,200,new ApiResponse<>("ORIENTACION_DEMOSTRATIVA_OBTENIDA","Consulta completada. No se cambió ninguna cita ni prioridad.",resultado));
        } catch(AgendaException e) {ApiJson.write(response,e.status(),new ApiResponse<>(e.code(),e.getMessage(),null));}
        catch(SQLException e) {ApiJson.write(response,503,new ApiResponse<>("CATALOGO_NO_DISPONIBLE","No se pudo consultar el catálogo. Inténtalo más tarde.",null));}
    }
    private static void limitar(HttpServletRequest request) {
        var session=request.getSession(false);
        synchronized(session) {
            long ahora=System.currentTimeMillis();Object ultimo=session.getAttribute("ultimaOrientacionGemini");
            if(ultimo instanceof Long fecha && ahora-fecha<15000) throw new AgendaException(429,"ORIENTACION_LIMITE","Espera unos segundos antes de volver a consultar.");
            session.setAttribute("ultimaOrientacionGemini",ahora);
        }
    }
}
