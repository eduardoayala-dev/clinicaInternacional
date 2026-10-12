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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "PacienteNotificacionesServlet", urlPatterns = {"/paciente/notificaciones"})
public class PacienteNotificacionesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login/paciente");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        int usuarioId = usuario != null ? usuario.getIdUsuario() : -1;

        CitaDAO citaDAO = new CitaDAO();
        int idPaciente = citaDAO.obtenerIdPacientePorUsuarioId(usuarioId);

        String action = request.getParameter("action");
        if ("marcarLeidas".equals(action)) {
            citaDAO.marcarNotificacionesComoLeidas(usuarioId);
            response.sendRedirect(request.getContextPath() + "/paciente/notificaciones");
            return;
        } else if ("marcarLeida".equals(action)) {
            String notifIdStr = request.getParameter("id");
            if (notifIdStr != null) {
                try {
                    int idNotif = Integer.parseInt(notifIdStr);
                    citaDAO.marcarNotificacionIndividualLeida(idNotif, usuarioId);
                } catch (NumberFormatException ignored) {}
            }
            response.sendRedirect(request.getContextPath() + "/paciente/notificaciones");
            return;
        } else if ("confirmar".equals(action)) {
            String notifIdStr = request.getParameter("id");
            if (notifIdStr != null) {
                try {
                    int idNotif = Integer.parseInt(notifIdStr);
                    citaDAO.confirmarRecordatorioCita(idNotif, usuarioId);
                    session.setAttribute("toastMsg", "Asistencia confirmada exitosamente. Tu cita ha quedado reconfirmada.");
                    session.setAttribute("toastTipo", "success");
                } catch (NumberFormatException ignored) {}
            }
            response.sendRedirect(request.getContextPath() + "/paciente/notificaciones");
            return;
        }

        String estadoParam = request.getParameter("estado");
        String simularParam = request.getParameter("simular");

        boolean ofertaExpirada = "expirada".equalsIgnoreCase(estadoParam) || "expirada".equalsIgnoreCase(simularParam);
        Map<String, Object> ofertaCupo = null;

        if (!ofertaExpirada) {
            if (idPaciente > 0) {
                ofertaCupo = citaDAO.obtenerOfertaCupoAnticipado(idPaciente);
            }
            if (ofertaCupo == null && ("oferta".equalsIgnoreCase(simularParam) || "oferta".equalsIgnoreCase(estadoParam))) {
                // Simulación para visualización de demostración
                ofertaCupo = new HashMap<>();
                ofertaCupo.put("idListaEspera", 999);
                ofertaCupo.put("especialidad", "Dermatología");
                ofertaCupo.put("medico", "Dr. Jorge Ríos");
                ofertaCupo.put("sede", "Sede San Borja · Consultorio 215");
                ofertaCupo.put("fechaOfrecida", "Mar 13 oct 2026 · 10:20");
                ofertaCupo.put("tiempoRestante", "14:32 min");
                ofertaCupo.put("tiempoRestanteSegundos", 872);
                ofertaCupo.put("origen", "Origen: cupo liberado por cancelación de otro paciente.");
            }
        }

        List<Map<String, Object>> listaNotificaciones = citaDAO.obtenerNotificacionesUsuario(usuarioId);
        int unreadCount = citaDAO.contarNotificacionesSinLeer(usuarioId);

        // Flash toast
        String toastMsg = (String) session.getAttribute("toastMsg");
        String toastTipo = (String) session.getAttribute("toastTipo");
        if (toastMsg != null) {
            request.setAttribute("toastMsg", toastMsg);
            request.setAttribute("toastTipo", toastTipo != null ? toastTipo : "info");
            session.removeAttribute("toastMsg");
            session.removeAttribute("toastTipo");
        }

        request.setAttribute("nombrePaciente", usuario != null ? usuario.getNombres() + " " + usuario.getApellidos() : "Paciente");
        request.setAttribute("listaNotificaciones", listaNotificaciones);
        request.setAttribute("totalNotificaciones", listaNotificaciones != null ? listaNotificaciones.size() : 0);
        request.setAttribute("unreadCount", unreadCount);
        request.setAttribute("tieneNotificacionesSinLeer", unreadCount > 0);
        request.setAttribute("ofertaCupo", ofertaCupo);
        request.setAttribute("ofertaExpirada", ofertaExpirada);

        request.getRequestDispatcher("/WEB-INF/views/paciente/notificaciones.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login/paciente");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        int usuarioId = usuario != null ? usuario.getIdUsuario() : -1;
        CitaDAO citaDAO = new CitaDAO();
        int idPaciente = citaDAO.obtenerIdPacientePorUsuarioId(usuarioId);

        String accion = request.getParameter("accion");
        int idListaEspera = 0;
        try {
            idListaEspera = Integer.parseInt(request.getParameter("idListaEspera"));
        } catch (Exception ignored) {}

        if ("aceptar".equalsIgnoreCase(accion)) {
            boolean ok = citaDAO.aceptarOfertaCupo(idPaciente, idListaEspera);
            if (ok) {
                session.setAttribute("toastMsg", "¡Oferta de cupo aceptada! Tu cupo ha sido reservado y tu cita quedó confirmada.");
                session.setAttribute("toastTipo", "success");
            }
            response.sendRedirect(request.getContextPath() + "/paciente/historial");
            return;
        } else if ("rechazar".equalsIgnoreCase(accion)) {
            citaDAO.rechazarOfertaCupo(idPaciente, idListaEspera);
            session.setAttribute("toastMsg", "Oferta rechazada. Mantienes tu posición en la lista de espera.");
            session.setAttribute("toastTipo", "info");
            response.sendRedirect(request.getContextPath() + "/paciente/notificaciones?estado=expirada");
            return;
        } else if ("confirmarCita".equalsIgnoreCase(accion)) {
            int idNotif = 0;
            try {
                idNotif = Integer.parseInt(request.getParameter("idNotificacion"));
            } catch (Exception ignored) {}
            citaDAO.confirmarRecordatorioCita(idNotif, usuarioId);
            session.setAttribute("toastMsg", "¡Asistencia confirmada exitosamente!");
            session.setAttribute("toastTipo", "success");
            response.sendRedirect(request.getContextPath() + "/paciente/notificaciones");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/paciente/notificaciones");
    }
}
