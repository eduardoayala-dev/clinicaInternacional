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

@WebServlet(name = "MedicoAgendaServlet", urlPatterns = {"/medico/agenda"})
public class MedicoAgendaServlet extends HttpServlet {

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
        List<Map<String, Object>> sedes = citaDAO.obtenerSedes();

        int totalAsignados = agenda.size();
        int atendidos = 0;
        int enEspera = 0;
        int pendientes = 0;

        for (Map<String, Object> c : agenda) {
            String est = (String) c.get("estado");
            if ("ATENDIDA".equalsIgnoreCase(est) || "ATENDIDO".equalsIgnoreCase(est)) {
                atendidos++;
            } else if ("EN_ESPERA".equalsIgnoreCase(est) || "EN SALA".equalsIgnoreCase(est)) {
                enEspera++;
            } else {
                pendientes++;
            }
        }

        if (enEspera == 0 && pendientes > 0 && !agenda.isEmpty()) {
            enEspera = 1;
            pendientes = Math.max(0, pendientes - 1);
        }

        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("EEE dd/MM/yyyy", java.util.Locale.forLanguageTag("es"));
        String fechaHoyStr = hoy.format(dtf);
        if (!fechaHoyStr.isEmpty()) {
            fechaHoyStr = Character.toUpperCase(fechaHoyStr.charAt(0)) + fechaHoyStr.substring(1);
        }

        request.setAttribute("usuario", usuario);
        request.setAttribute("nombreMedico", detallesMedico.get("nombreMedico") != null ? detallesMedico.get("nombreMedico") : ("Dr. " + usuario.getNombres() + " " + usuario.getApellidos()));
        request.setAttribute("especialidadMedico", detallesMedico.get("especialidad") != null ? detallesMedico.get("especialidad") : "Medicina General");
        request.setAttribute("sedeMedico", detallesMedico.get("sedeNombre") != null ? detallesMedico.get("sedeNombre") : "Sede San Borja");
        request.setAttribute("agenda", agenda);
        request.setAttribute("sedes", sedes);
        request.setAttribute("fechaHoyStr", fechaHoyStr);
        request.setAttribute("totalAsignados", totalAsignados);
        request.setAttribute("atendidos", atendidos);
        request.setAttribute("enEspera", enEspera);
        request.setAttribute("pendientes", pendientes);

        request.getRequestDispatcher("/WEB-INF/views/medico/agenda.jsp").forward(request, response);
    }
}
