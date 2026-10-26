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

    protected CitaDAO crearCitaDAO() {
        return new CitaDAO();
    }

    @Override
    public void init() throws ServletException {
        this.citaDAO = crearCitaDAO();
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
                if (idCita <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }

        Map<String, Object> citaDetalle = null;
        if (idCita > 0) {
            citaDetalle = citaDAO.obtenerDetalleCitaMedico(idCita, idMedico);
            if (citaDetalle == null || citaDetalle.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
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

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (!"MEDICO".equals(usuario.getRol())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        int idCita;
        try {
            idCita = Integer.parseInt(request.getParameter("idCita"));
            if (idCita <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String accion = request.getParameter("accion");
        if (!"no_asistio".equals(accion) && !"guardar".equals(accion)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String prioridad = request.getParameter("prioridad");
        if ("no_asistio".equals(accion)) prioridad = "BAJA";
        String fechaProxima = request.getParameter("fechaProxima");
        int idMedico = citaDAO.obtenerIdMedicoPorUsuarioId(usuario.getIdUsuario());
        boolean ok = citaDAO.actualizarAtencionMedica(idCita, idMedico,
                "no_asistio".equals(accion) ? "NO_ASISTIO" : "ATENDIDA", prioridad,
                fechaProxima, SecurityUtil.sanitizeInput(request.getParameter("indicaciones")));
        if (!ok) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/medico/atencion?idCita=" + idCita);
    }
}
