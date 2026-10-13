package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.util.SecurityUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(name = "MedicoAtencionServlet", urlPatterns = {"/medico/atencion"})
public class MedicoAtencionServlet extends HttpServlet {

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
        String idCitaStr = request.getParameter("idCita");
        int idCita = -1;
        if (idCitaStr != null && !idCitaStr.trim().isEmpty()) {
            try {
                idCita = Integer.parseInt(idCitaStr);
            } catch (NumberFormatException ignored) {}
        }

        Map<String, Object> citaDetalle = null;
        if (idCita > 0) {
            citaDetalle = citaDAO.obtenerDetalleCitaMedico(idCita);
        }

        request.setAttribute("usuario", usuario);
        request.setAttribute("nombreMedico", usuario.getNombres() + " " + usuario.getApellidos());
        request.setAttribute("idCita", idCita);
        request.setAttribute("cita", citaDetalle);

        request.getRequestDispatcher("/WEB-INF/views/medico/atencion.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login/personal");
            return;
        }

        String idCitaStr = request.getParameter("idCita");
        String accion = SecurityUtil.sanitizeInput(request.getParameter("accion"));
        String prioridad = SecurityUtil.sanitizeInput(request.getParameter("prioridad"));
        String fechaProxima = SecurityUtil.sanitizeInput(request.getParameter("fechaProxima"));
        String indicaciones = SecurityUtil.sanitizeInput(request.getParameter("indicaciones"));

        int idCita = 1;
        if (idCitaStr != null && !idCitaStr.isEmpty()) {
            try {
                idCita = Integer.parseInt(idCitaStr);
            } catch (NumberFormatException ignored) {}
        }

        if ("no_asistio".equalsIgnoreCase(accion)) {
            citaDAO.actualizarAtencionMedica(idCita, "NO_ASISTIO", "BAJA", null, "Paciente no se presentó al llamado en consultorio.");
            request.setAttribute("mensajeExito", "Estado actualizado: Marcado como 'No se presentó al llamado'.");
        } else {
            String nuevoEstado = "ATENDIDA";
            citaDAO.actualizarAtencionMedica(idCita, nuevoEstado, prioridad != null ? prioridad : "MEDIA", fechaProxima, indicaciones);
            request.setAttribute("mensajeExito", "¡Registro de atención guardado con éxito! Cita finalizada y plan de seguimiento registrado.");
        }

        doGet(request, response);
    }
}
