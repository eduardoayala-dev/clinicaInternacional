package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.dao.AgendaCitaDAO;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.CatalogoDatosException;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.util.MotivoCancelacion;

@WebServlet("/api/v1/citas/*")
public class ApiCitasServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(ApiCitasServlet.class.getName());
    protected CitaDAO citas() { return new CitaDAO(); }
    protected AgendaCitaDAO agenda() { return new AgendaCitaDAO(); }

    private record Referencia(int idCita) {}
    private record Resultado(int status, String code, String message, Object data) {}

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) {
        Resultado resultado;
        try {
            resultado = ejecutar(request, response);
        } catch (NumberFormatException e) {
            resultado = new Resultado(400, "DATOS_INVALIDOS", "Revisa los identificadores enviados.", null);
        } catch (AgendaException e) {
            resultado = new Resultado(e.status(), e.code(), e.getMessage(), null);
        } catch (CatalogoDatosException e) {
            resultado = new Resultado(503, "CITAS_NO_DISPONIBLES", "Inténtalo nuevamente.", null);
        } catch (RuntimeException e) {
            LOG.warning("Error inesperado en la API de citas.");
            resultado = new Resultado(500, "ERROR_INTERNO", "No se pudo completar la solicitud.", null);
        }
        try {
            ApiJson.write(response, resultado.status(), new ApiResponse<>(resultado.code(), resultado.message(), resultado.data()));
        } catch (IOException e) {
            LOG.warning("No se pudo entregar la respuesta de la API de citas.");
        }
    }

    private Resultado ejecutar(HttpServletRequest req, HttpServletResponse res) {
        String ruta = req.getPathInfo();
        if (ruta == null || "/".equals(ruta)) ruta = "";
        boolean coleccion = ruta.isEmpty();
        if (!coleccion && !"/retenciones".equals(ruta) && !ruta.matches("/[0-9]+/(reprogramacion|cancelacion)")) {
            return new Resultado(404, "RUTA_NO_ENCONTRADA", "La ruta no existe.", null);
        }
        String metodo = req.getMethod();
        if (!"POST".equals(metodo) && !(coleccion && "GET".equals(metodo))) {
            res.setHeader("Allow", coleccion ? "GET, POST" : "POST");
            return new Resultado(405, "METODO_NO_PERMITIDO", "Revisa el método de la solicitud.", null);
        }

        var sesion = req.getSession(false);
        Object identidad = sesion == null ? null : sesion.getAttribute("usuarioLogueado");
        if (!(identidad instanceof Usuario usuario)) {
            return new Resultado(401, "SESION_REQUERIDA", "Inicia sesión como paciente.", null);
        }
        if (!"PACIENTE".equals(usuario.getRol())) {
            return new Resultado(403, "ACCESO_DENEGADO", "Acceso exclusivo para pacientes.", null);
        }
        var dao = citas();
        int paciente = dao.obtenerIdPacientePorUsuarioId(usuario.getIdUsuario());
        if (paciente <= 0) return new Resultado(403, "ACCESO_DENEGADO", "Perfil de paciente requerido.", null);
        if ("GET".equals(metodo)) {
            return new Resultado(200, "CITAS_OBTENIDAS", "Próximas citas.", dao.obtenerProximasCitasPaciente(paciente));
        }
        return escribir(req, ruta, dao, paciente);
    }

    private Resultado escribir(HttpServletRequest req, String ruta, CitaDAO dao, int paciente) {
        if (ruta.endsWith("/cancelacion")) return cancelar(req, ruta, dao, paciente);

        int medico = positivo(req.getParameter("idMedico"));
        String fecha = req.getParameter("fecha");
        String hora = req.getParameter("hora");
        if ("/retenciones".equals(ruta)) {
            return new Resultado(201, "CUPO_RETENIDO", "Cupo retenido temporalmente.", agenda().retener(paciente, medico, fecha, hora));
        }
        int motivo = positivo(req.getParameter("idMotivo"));
        if (ruta.isEmpty()) {
            int cita = dao.reservarCitaRetenida(paciente, medico, fecha, hora, motivo, req.getParameter("retencion"));
            return new Resultado(201, "CITA_RESERVADA", "Cita reservada.", new Referencia(cita));
        }
        int nueva = dao.reprogramarCitaConId(idCita(ruta), paciente, medico, fecha, hora, motivo);
        return new Resultado(200, "CITA_REPROGRAMADA", "Cita reprogramada.", new Referencia(nueva));
    }

    private Resultado cancelar(HttpServletRequest req, String ruta, CitaDAO dao, int paciente) {
        int cita = idCita(ruta);
        String motivo = MotivoCancelacion.validar(req.getParameter("motivo"));
        if (dao.actualizarEstadoCitaPaciente(cita, paciente, "CANCELADA", motivo)) {
            try {
                dao.ejecutarReasignacionInteligente(cita);
            } catch (AgendaException | CatalogoDatosException e) {
                LOG.warning("El mantenimiento revisará la oferta del cupo cancelado.");
            }
        } else if (!dao.estaCanceladaPorPaciente(cita, paciente)) {
            return new Resultado(404, "CITA_NO_ENCONTRADA", "La cita no está disponible para cancelar.", null);
        }
        return new Resultado(200, "CITA_CANCELADA", "Cita cancelada.", new Referencia(cita));
    }

    private static int idCita(String ruta) { return positivo(ruta.substring(1, ruta.indexOf('/', 1))); }

    private static int positivo(String valor) {
        int id = Integer.parseInt(valor);
        if (id <= 0) throw new NumberFormatException();
        return id;
    }
}
