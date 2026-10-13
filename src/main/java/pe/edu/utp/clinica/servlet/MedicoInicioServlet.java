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
import java.util.List;
import java.util.Map;

@WebServlet(name = "MedicoInicioServlet", urlPatterns = {"/medico/inicio", "/medico/dashboard"})
public class MedicoInicioServlet extends HttpServlet {

    private CitaDAO citaDAO;

    @Override
    public void init() throws ServletException {
        this.citaDAO = new CitaDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login/personal");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (!"MEDICO".equals(usuario.getRol())) {
            response.sendRedirect(request.getContextPath() + "/login/personal?error=rol_invalido");
            return;
        }

        int idMedico = citaDAO.obtenerIdMedicoPorUsuarioId(usuario.getIdUsuario());
        List<Map<String, Object>> agenda = citaDAO.obtenerAgendaMedico(idMedico);
        Map<String, Object> detallesMedico = citaDAO.obtenerDetallesMedico(idMedico);

        // Métricas calculadas directamente desde los datos reales de la BD
        int totalCitas = agenda.size();
        int atendidos = 0;
        int enEspera = 0;
        int pendientes = 0;
        Map<String, Object> pacienteEnSala = null;

        for (Map<String, Object> c : agenda) {
            String est = (String) c.get("estado");
            if ("ATENDIDA".equalsIgnoreCase(est) || "ATENDIDO".equalsIgnoreCase(est)) {
                atendidos++;
            } else if ("EN_ESPERA".equalsIgnoreCase(est) || "CONFIRMADA".equalsIgnoreCase(est) || "SOLICITADA".equalsIgnoreCase(est)) {
                pendientes++;
                if (pacienteEnSala == null && ("EN_ESPERA".equalsIgnoreCase(est) || "CONFIRMADA".equalsIgnoreCase(est))) {
                    pacienteEnSala = c;
                    enEspera++;
                }
            } else {
                pendientes++;
            }
        }

        // Si enEspera es 0 pero hay pendientes, considerar al primer pendiente como paciente en sala de espera para atención
        if (enEspera == 0 && !agenda.isEmpty()) {
            for (Map<String, Object> c : agenda) {
                String est = (String) c.get("estado");
                if (!"ATENDIDA".equalsIgnoreCase(est) && !"ATENDIDO".equalsIgnoreCase(est)) {
                    pacienteEnSala = c;
                    enEspera = 1;
                    break;
                }
            }
        }

        int pendientesRestantes = Math.max(0, totalCitas - atendidos);

        request.setAttribute("usuario", usuario);
        request.setAttribute("nombreMedico", detallesMedico.get("nombreMedico") != null ? detallesMedico.get("nombreMedico") : ("Dr. " + usuario.getNombres() + " " + usuario.getApellidos()));
        request.setAttribute("especialidadMedico", detallesMedico.get("especialidad") != null ? detallesMedico.get("especialidad") : "Medicina General");
        request.setAttribute("sedeMedico", detallesMedico.get("sedeNombre") != null ? detallesMedico.get("sedeNombre") : "Sede San Borja");
        request.setAttribute("agenda", agenda);
        request.setAttribute("totalCitas", totalCitas);
        request.setAttribute("atendidos", atendidos);
        request.setAttribute("enEspera", enEspera);
        request.setAttribute("pendientes", pendientes);
        request.setAttribute("pendientesRestantes", pendientesRestantes);
        request.setAttribute("pacienteEnSala", pacienteEnSala);

        // Fecha actual formateada en español
        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("EEE dd MMM yyyy", java.util.Locale.forLanguageTag("es"));
        String fechaActual = hoy.format(dtf);
        if (!fechaActual.isEmpty()) {
            fechaActual = Character.toUpperCase(fechaActual.charAt(0)) + fechaActual.substring(1);
        }
        request.setAttribute("fechaActual", fechaActual);

        request.getRequestDispatcher("/WEB-INF/views/medico/inicio.jsp").forward(request, response);
    }
}
