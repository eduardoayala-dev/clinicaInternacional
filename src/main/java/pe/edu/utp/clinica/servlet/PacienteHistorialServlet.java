package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.dao.ConfirmacionAsistenciaDAO;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.CatalogoDatosException;
import pe.edu.utp.clinica.model.Usuario;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet(name = "PacienteHistorialServlet", urlPatterns = {"/paciente/historial"})
public class PacienteHistorialServlet extends HttpServlet {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(PacienteHistorialServlet.class.getName());

    private static final String ID_CITA = "idCita";
    private static final String FLASH_MESSAGE = "flashMessage";

    protected CitaDAO crearCitaDAO() {
        return new CitaDAO();
    }
    protected ConfirmacionAsistenciaDAO crearConfirmacionDAO() { return new ConfirmacionAsistenciaDAO(); }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            pe.edu.utp.clinica.util.TransporteHttp.redirigir(response,request.getContextPath() + "/login?error=sesion_requerida");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        int usuarioId = usuario != null ? usuario.getIdUsuario() : -1;

        CitaDAO citaDAO = crearCitaDAO();
        int idPaciente = citaDAO.obtenerIdPacientePorUsuarioId(usuarioId);

        List<Map<String, Object>> citasActivas = idPaciente > 0 ? citaDAO.obtenerProximasCitasPaciente(idPaciente) : new ArrayList<>();
        List<Map<String, Object>> historialPasadas = idPaciente > 0 ? citaDAO.obtenerHistorialPaciente(idPaciente) : new ArrayList<>();
        List<Map<String, Object>> citasCanceladas = idPaciente > 0 ? citaDAO.obtenerCitasCanceladasPaciente(idPaciente) : new ArrayList<>();
        List<Integer> idsCitasActivas = citasActivas.stream().map(c -> (Integer) c.get(ID_CITA)).filter(id -> id != null && id > 0).toList();
        Map<Integer, ConfirmacionAsistenciaDAO.EstadoAsistencia> estadosAsistencia = usuario != null
                ? crearConfirmacionDAO().estadoDelPaciente(usuario.getIdUsuario(), idsCitasActivas) : Map.of();
        for (Map<String, Object> cita : citasActivas) {
            ConfirmacionAsistenciaDAO.EstadoAsistencia estado = estadosAsistencia.get(cita.get(ID_CITA));
            cita.put("asistenciaConfirmada", estado != null && estado.confirmada());
            cita.put("confirmacionVencida", estado != null && estado.vencida());
            cita.put("venceConfirmacion", estado == null ? null : estado.venceEn());
        }
        int numNotifSinLeer = usuario != null ? citaDAO.contarNotificacionesSinLeer(usuario.getIdUsuario()) : 0;

        String nombrePaciente = usuario != null ? usuario.getNombres() + " " + usuario.getApellidos() : "Paciente";

        request.setAttribute("nombrePaciente", nombrePaciente);
        request.setAttribute("citasActivas", citasActivas);
        request.setAttribute("historialPasadas", historialPasadas);
        request.setAttribute("citasCanceladas", citasCanceladas);
        request.setAttribute("numNotifSinLeer", numNotifSinLeer);

        request.getRequestDispatcher("/WEB-INF/views/paciente/historial.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            pe.edu.utp.clinica.util.TransporteHttp.redirigir(response,request.getContextPath() + "/login?error=sesion_requerida");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (!"PACIENTE".equals(usuario.getRol())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String action = request.getParameter("action");
        if ("confirmar_asistencia".equals(action)) {
            confirmarAsistencia(request, session, usuario);
            pe.edu.utp.clinica.util.TransporteHttp.redirigir(response,request.getContextPath() + "/paciente/historial");
            return;
        }
        String estado;
        if ("confirmar".equals(action)) estado = "CONFIRMADA";
        else if ("cancelar".equals(action)) estado = "CANCELADA";
        else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        int idCita;
        try {
            idCita = Integer.parseInt(request.getParameter(ID_CITA));
            if (idCita <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        CitaDAO citaDAO = crearCitaDAO();
        int idPaciente = citaDAO.obtenerIdPacientePorUsuarioId(usuario.getIdUsuario());
        if (!citaDAO.actualizarEstadoCitaPaciente(idCita, idPaciente, estado)) {
            // Misma respuesta para cita ajena, inexistente o no modificable.
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if ("CANCELADA".equals(estado)) {
            try {
                citaDAO.ejecutarReasignacionInteligente(idCita);
                citaDAO.crearNotificacion(usuario.getIdUsuario(), idCita, "Cita Cancelada",
                        "Tu cita médica ha sido cancelada correctamente.");
            } catch (AgendaException | CatalogoDatosException e) {
                LOGGER.warning("La cita se canceló; el procesamiento posterior se reintentará para la lista de espera.");
            }
        }
        pe.edu.utp.clinica.util.TransporteHttp.redirigir(response,request.getContextPath() + "/paciente/historial");
    }

    private void confirmarAsistencia(HttpServletRequest request, HttpSession session, Usuario usuario) {
            int idCita;
            try {
                idCita = Integer.parseInt(request.getParameter(ID_CITA));
                if (idCita <= 0) throw new NumberFormatException();
                crearConfirmacionDAO().confirmarPaciente(usuario.getIdUsuario(), idCita);
                session.setAttribute(FLASH_MESSAGE, "Asistencia confirmada. Tu cita continúa programada.");
            } catch (NumberFormatException e) {
                session.setAttribute(FLASH_MESSAGE, "Selecciona una cita válida.");
            } catch (AgendaException e) {
                session.setAttribute(FLASH_MESSAGE, e.getMessage());
            }
    }
}
