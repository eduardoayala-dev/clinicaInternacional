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

        String idCitaStr = request.getParameter("idCita");
        int idCita = 1;
        if (idCitaStr != null && !idCitaStr.isEmpty()) {
            try {
                idCita = Integer.parseInt(idCitaStr);
            } catch (NumberFormatException ignored) {}
        }

        request.setAttribute("usuario", usuario);
        request.setAttribute("nombreMedico", usuario.getNombres() + " " + usuario.getApellidos());
        request.setAttribute("idCita", idCita);

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
        String estadoAsistencia = SecurityUtil.sanitizeInput(request.getParameter("asistencia"));
        String prioridad = SecurityUtil.sanitizeInput(request.getParameter("prioridad"));
        String indicaciones = SecurityUtil.sanitizeInput(request.getParameter("indicaciones"));

        int idCita = 1;
        if (idCitaStr != null && !idCitaStr.isEmpty()) {
            try {
                idCita = Integer.parseInt(idCitaStr);
            } catch (NumberFormatException ignored) {}
        }

        String nuevoEstado = "NO_ASISTIO".equals(estadoAsistencia) ? "NO_ASISTIO" : "ATENDIDO";
        citaDAO.actualizarAtencionMedica(idCita, nuevoEstado, prioridad, indicaciones);

        request.setAttribute("mensajeExito", "¡Registro de atención médica guardado correctamente!");
        doGet(request, response);
    }
}
