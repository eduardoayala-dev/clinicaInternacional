package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.dao.AdmisionCitaDAO;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.model.Usuario;
import java.io.IOException;

@WebServlet(name="AdmisionCitaServlet", urlPatterns="/admin/admision")
public class AdmisionCitaServlet extends HttpServlet {
    private transient AdmisionCitaDAO admision;
    protected AdmisionCitaDAO crearDAO() { return new AdmisionCitaDAO(); }
    @Override public void init() { admision = crearDAO(); }

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Allow", "POST");
        ApiJson.write(response, 405, new ApiResponse<>("METODO_NO_PERMITIDO", "Usa POST para registrar una llegada.", null));
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            var session = request.getSession(false);
            if (session == null || !(session.getAttribute("usuarioLogueado") instanceof Usuario usuario)
                    || !"PERSONAL_ADMINISTRATIVO".equals(usuario.getRol())) {
                throw new AgendaException(403, "ACCESO_DENEGADO", "Ingresa con una cuenta de admisión.");
            }
            int cita = identificador(request.getParameter("idCita"));
            String accion=request.getParameter("accion");
            Object resultado;
            String mensaje;
            String codigo;
            if("no_asistio".equals(accion)) {
                resultado=admision.registrarInasistencia(usuario.getIdUsuario(),cita,request.getParameter("motivo"));
                mensaje="Inasistencia verificada y registrada.";codigo="INASISTENCIA_REGISTRADA";
            } else if(accion==null || "llegada".equals(accion)) {
                var llegada=admision.registrar(usuario.getIdUsuario(),cita);
                resultado=llegada;
                mensaje=llegada.llegoTarde()?"Llegada registrada fuera de la tolerancia. El médico evaluará la atención.":"Llegada registrada. El paciente está en sala.";
                codigo="LLEGADA_REGISTRADA";
            } else throw new AgendaException(400,"ACCION_INVALIDA","La acción no es válida.");
            if (json(request)) ApiJson.write(response, 200, new ApiResponse<>(codigo, mensaje, resultado));
            else {
                session.setAttribute("flashMessage", mensaje);
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
            }
        } catch (AgendaException e) {
            ApiJson.write(response, e.status(), new ApiResponse<>(e.code(), e.getMessage(), null));
        }
    }

    private static int identificador(String valor) {
        try {
            if (valor == null || !valor.matches("[1-9]\\d{0,9}")) throw new NumberFormatException();
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new AgendaException(400, "DATOS_INVALIDOS", "Selecciona una cita válida.");
        }
    }
    private static boolean json(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }
}
