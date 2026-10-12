package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.clinica.model.Usuario;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet del Dashboard del Paciente (HU003, HU008, HU012).
 * Proporciona el resumen integral del paciente integrando el Wireframe del sistema
 * (Alertas de cupo anticipado, Próxima cita, Estado en Lista de Espera)
 * con la estética visual y controles de la maqueta de la Clínica Internacional.
 */
import pe.edu.utp.clinica.dao.CitaDAO;

@WebServlet(name = "PacienteDashboardServlet", urlPatterns = {"/paciente/dashboard", "/paciente/citas", "/paciente/lista-espera"})
public class PacienteDashboardServlet extends HttpServlet {

    private CitaDAO citaDAO;

    @Override
    public void init() throws ServletException {
        this.citaDAO = new CitaDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Usuario usuario = (session != null) ? (Usuario) session.getAttribute("usuarioLogueado") : null;

        // Fallback de usuario paciente si la sesión fue iniciada
        String nombrePaciente = (usuario != null) ? usuario.getNombreCompleto() : "Juan Alberto Pérez";
        String emailPaciente = (usuario != null) ? usuario.getEmail() : "juan.perez@correo.com";

        String action = request.getParameter("action");
        String path = request.getServletPath();
        
        if ("nueva".equals(action) || "/paciente/citas".equals(path)) {
            request.setAttribute("listaEspecialidades", citaDAO.obtenerEspecialidades());
            request.setAttribute("listaSedes", citaDAO.obtenerSedes());
            request.setAttribute("nombrePaciente", nombrePaciente);
            String preMedId = request.getParameter("medicoId");
            String preEspId = request.getParameter("especialidadId");
            String preMedNombre = "";
            String preEspNombre = "";

            if (preMedId != null && !preMedId.trim().isEmpty()) {
                try {
                    int mId = Integer.parseInt(preMedId);
                    Map<String, Object> det = citaDAO.obtenerDetallesMedico(mId);
                    if (det != null && !det.isEmpty()) {
                        preMedNombre = (String) det.get("nombreMedico");
                        preEspNombre = (String) det.get("especialidad");
                        if (preEspId == null || preEspId.trim().isEmpty()) {
                            Object idEsp = det.get("idEspecialidad");
                            if (idEsp != null) preEspId = String.valueOf(idEsp);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore parse error
                }
            }

            request.setAttribute("preMedId", preMedId != null ? preMedId : "");
            request.setAttribute("preEspId", preEspId != null ? preEspId : "");
            request.setAttribute("preMedNombre", preMedNombre != null ? preMedNombre : "");
            request.setAttribute("preEspNombre", preEspNombre != null ? preEspNombre : "");
            request.getRequestDispatcher("/WEB-INF/views/paciente/reservar_cita.jsp").forward(request, response);
            return;
        }

        int usuarioId = usuario != null ? usuario.getIdUsuario() : -1;
        int idPaciente = citaDAO.obtenerIdPacientePorUsuarioId(usuarioId);

        List<Map<String, Object>> listaProximasCitas = idPaciente > 0 ? citaDAO.obtenerProximasCitasPaciente(idPaciente) : new ArrayList<>();
        List<Map<String, Object>> medicosAsignados = idPaciente > 0 ? citaDAO.obtenerMedicosDePaciente(idPaciente) : new ArrayList<>();
        List<Map<String, Object>> historialCitas = idPaciente > 0 ? citaDAO.obtenerHistorialPaciente(idPaciente) : new ArrayList<>();

        Map<String, Object> proximaCita = !listaProximasCitas.isEmpty() ? listaProximasCitas.get(0) : null;

        Map<String, Integer> stats = new HashMap<>();
        stats.put("proximasCitas", listaProximasCitas.size());
        stats.put("medicosAsignados", medicosAsignados.size());
        stats.put("estudiosPendientes", 0);
        stats.put("consultasTotales", historialCitas.size());

        String ultimoAcceso = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        String ipAcceso = request.getRemoteAddr();

        request.setAttribute("nombrePaciente", nombrePaciente);
        request.setAttribute("emailPaciente", emailPaciente);
        request.setAttribute("proximaCita", proximaCita);
        request.setAttribute("listaProximasCitas", listaProximasCitas);
        request.setAttribute("medicosAsignados", medicosAsignados);
        request.setAttribute("stats", stats);
        request.setAttribute("ultimoAcceso", ultimoAcceso);
        request.setAttribute("ipAcceso", ipAcceso);

        request.getRequestDispatcher("/WEB-INF/views/paciente/dashboard.jsp").forward(request, response);
    }
}
