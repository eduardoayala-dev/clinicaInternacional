package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.model.Usuario;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@WebServlet(name = "PacienteReprogramarServlet", urlPatterns = {"/paciente/reprogramar"})
public class PacienteReprogramarServlet extends HttpServlet {

    private CitaDAO citaDAO;

    @Override
    public void init() throws ServletException {
        this.citaDAO = new CitaDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login?error=sesion_requerida");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (!"PACIENTE".equalsIgnoreCase(usuario.getRol())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso restringido para pacientes.");
            return;
        }

        String idCitaStr = request.getParameter("idCita");
        if (idCitaStr == null || idCitaStr.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/paciente/historial");
            return;
        }

        try {
            int idCita = Integer.parseInt(idCitaStr.trim());
            int idPaciente = citaDAO.obtenerIdPacientePorUsuarioId(usuario.getIdUsuario());

            Map<String, Object> cita = citaDAO.obtenerCitaPorId(idCita);
            if (cita == null || cita.isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/paciente/historial?error=cita_no_encontrada");
                return;
            }

            // Validar que la cita pertenezca al paciente en sesión (Seguridad médica)
            int citaPacienteId = (Integer) cita.get("idPaciente");
            if (citaPacienteId != idPaciente) {
                response.sendRedirect(request.getContextPath() + "/paciente/historial?error=no_autorizado");
                return;
            }

            // Validar estado no reprogramable
            String estado = (String) cita.get("estado");
            if ("CANCELADA".equalsIgnoreCase(estado) || "ATENDIDA".equalsIgnoreCase(estado) || "REPROGRAMADA".equalsIgnoreCase(estado)) {
                response.sendRedirect(request.getContextPath() + "/paciente/historial?error=estado_invalido");
                return;
            }

            int idEspecialidad = (Integer) cita.get("idEspecialidad");
            int idSede = (Integer) cita.get("idSede");

            List<Map<String, Object>> listaSedes = citaDAO.obtenerSedes();
            List<Map<String, Object>> listaEspecialidades = citaDAO.obtenerEspecialidades();
            List<Map<String, Object>> listaMedicos = citaDAO.obtenerMedicosDisponibles(idEspecialidad, idSede);
            if (listaMedicos.isEmpty()) {
                listaMedicos = citaDAO.obtenerMedicosPorEspecialidad(idEspecialidad);
            }
            List<Map<String, Object>> listaMotivos = citaDAO.obtenerMotivosPorEspecialidad(idEspecialidad);
            int numNotifSinLeer = citaDAO.contarNotificacionesSinLeer(usuario.getIdUsuario());

            String nombrePaciente = usuario.getNombres() + " " + usuario.getApellidos();

            request.setAttribute("nombrePaciente", nombrePaciente);
            request.setAttribute("cita", cita);
            request.setAttribute("listaSedes", listaSedes);
            request.setAttribute("listaEspecialidades", listaEspecialidades);
            request.setAttribute("listaMedicos", listaMedicos);
            request.setAttribute("listaMotivos", listaMotivos);
            request.setAttribute("numNotifSinLeer", numNotifSinLeer);

            request.getRequestDispatcher("/WEB-INF/views/paciente/reprogramar_cita.jsp").forward(request, response);

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/paciente/historial");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login?error=sesion_requerida");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (!"PACIENTE".equalsIgnoreCase(usuario.getRol())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso restringido para pacientes.");
            return;
        }

        try {
            int idCita = Integer.parseInt(request.getParameter("idCita"));
            int idMedico = Integer.parseInt(request.getParameter("idMedico"));
            String fecha = request.getParameter("fecha"); // yyyy-MM-dd
            String hora = request.getParameter("hora");   // HH:mm
            String idMotivoStr = request.getParameter("idMotivo");
            Integer idMotivo = (idMotivoStr != null && !idMotivoStr.trim().isEmpty()) ? Integer.parseInt(idMotivoStr) : null;
            String nivelUrgencia = request.getParameter("urgencia");

            int idPaciente = citaDAO.obtenerIdPacientePorUsuarioId(usuario.getIdUsuario());

            // Validación de fecha y hora futura obligatoria en el flujo clínico
            LocalDate fechaParsed = LocalDate.parse(fecha);
            LocalTime horaParsed = LocalTime.parse(hora);
            LocalDateTime nuevaFechaHora = LocalDateTime.of(fechaParsed, horaParsed);

            if (nuevaFechaHora.isBefore(LocalDateTime.now())) {
                response.sendRedirect(request.getContextPath() + "/paciente/reprogramar?idCita=" + idCita + "&error=fecha_pasada");
                return;
            }

            boolean exito = citaDAO.reprogramarCita(idCita, idPaciente, idMedico, fecha, hora, idMotivo, nivelUrgencia);

            if (exito) {
                response.sendRedirect(request.getContextPath() + "/paciente/historial?reprogramada=ok");
            } else {
                response.sendRedirect(request.getContextPath() + "/paciente/reprogramar?idCita=" + idCita + "&error=fallo_transaccion");
            }

        } catch (Exception e) {
            e.printStackTrace();
            String idCitaParam = request.getParameter("idCita");
            if (idCitaParam != null) {
                response.sendRedirect(request.getContextPath() + "/paciente/reprogramar?idCita=" + idCitaParam + "&error=datos_invalidos");
            } else {
                response.sendRedirect(request.getContextPath() + "/paciente/historial?error=error_inesperado");
            }
        }
    }
}
