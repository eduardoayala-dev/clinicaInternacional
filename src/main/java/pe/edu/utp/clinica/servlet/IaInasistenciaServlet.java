package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.ia.EntradaInasistencia;
import pe.edu.utp.clinica.ia.ModeloInasistencia;
import pe.edu.utp.clinica.model.Usuario;
import java.io.IOException;
import java.util.logging.Logger;

@WebServlet(name="IaInasistenciaServlet",urlPatterns="/sysadmin/ia/inasistencia")
public final class IaInasistenciaServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(IaInasistenciaServlet.class.getName());
    private transient ModeloInasistencia modelo;
    @Override public void init() {
        modelo = null;
        try { modelo=ModeloInasistencia.demostracion(); }
        catch (IOException e) { LOGGER.warning("Modelo experimental no disponible; no se habilita el simulador."); }
    }

    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response) { responder(request,response,false); }
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response) { responder(request,response,true); }

    private void responder(HttpServletRequest request,HttpServletResponse response,boolean escritura) {
        try {
            if (escritura) simular(request,response); else mostrar(request,response);
        } catch (IOException | ServletException e) {
            LOGGER.warning("No se pudo entregar la respuesta del simulador de inasistencia.");
            if (!response.isCommitted()) response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void mostrar(HttpServletRequest request,HttpServletResponse response) throws IOException,ServletException {
        try {
            autorizar(request);
            if (json(request)) ApiJson.write(response,200,new ApiResponse<>("MODELO_EXPERIMENTAL_OBTENIDO","Modelo de demostración con datos sintéticos.",modelo.informacion()));
            else request.getRequestDispatcher("/WEB-INF/views/sysadmin/ia_inasistencia.jsp").forward(request,response);
        } catch (AgendaException e) { error(request,response,e); }
    }

    private void simular(HttpServletRequest request,HttpServletResponse response) throws IOException {
        try {
            autorizar(request);
            String confirmada=parametro(request,"confirmadaPorPaciente");
            if (!"true".equals(confirmada) && !"false".equals(confirmada)) throw new IllegalArgumentException("Selecciona si el paciente confirmó expresamente.");
            var entrada=new EntradaInasistencia(entero(request,"inasistenciasPrevias"),
                    entero(request,"atencionesPrevias"),entero(request,"anticipacionDias"),Boolean.parseBoolean(confirmada));
            ApiJson.write(response,200,new ApiResponse<>("PREDICCION_EXPERIMENTAL_OBTENIDA","Simulación; no se modificó ninguna cita.",modelo.predecir(entrada)));
        } catch (IllegalArgumentException e) {
            ApiJson.write(response,400,new ApiResponse<>("ENTRADA_IA_INVALIDA","Completa un historial válido (0–100), anticipación (0–180) y confirmación expresa.",null));
        } catch (AgendaException e) { error(request,response,e); }
    }

    private static String parametro(HttpServletRequest request,String nombre) {
        String[] valores=request.getParameterValues(nombre);
        if (valores==null || valores.length!=1) throw new IllegalArgumentException("Entrada ausente o repetida.");
        return valores[0];
    }
    private static int entero(HttpServletRequest request,String nombre) {
        String valor=parametro(request,nombre);
        if (valor==null || !valor.matches("[0-9]{1,3}")) throw new IllegalArgumentException("Entrada entera inválida.");
        return Integer.parseInt(valor);
    }

    private void autorizar(HttpServletRequest request) {
        var session=request.getSession(false);
        Usuario usuario=session==null?null:(Usuario)session.getAttribute("usuarioLogueado");
        if (usuario==null || !"ADMINISTRADOR".equals(usuario.getRol())) throw new AgendaException(403,"ACCESO_DENEGADO","Acceso exclusivo del administrador del sistema.");
        if (modelo==null) throw new AgendaException(503,"MODELO_NO_DISPONIBLE","El modelo experimental no está disponible.");
    }
    private static boolean json(HttpServletRequest request) { String accept=request.getHeader("Accept");return accept!=null && accept.contains("application/json"); }
    private static void error(HttpServletRequest request,HttpServletResponse response,AgendaException e) throws IOException {
        if (json(request) || "POST".equals(request.getMethod())) ApiJson.write(response,e.status(),new ApiResponse<>(e.code(),e.getMessage(),null));
        else response.sendError(e.status(),e.getMessage());
    }
}
