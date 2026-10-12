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

@WebServlet(name = "PacienteListaEsperaServlet", urlPatterns = {"/paciente/lista-espera"})
public class PacienteListaEsperaServlet extends HttpServlet {

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
        int usuarioId = usuario != null ? usuario.getIdUsuario() : -1;
        int idPaciente = citaDAO.obtenerIdPacientePorUsuarioId(usuarioId);

        List<Map<String, Object>> especialidades = citaDAO.obtenerEspecialidades();
        List<Map<String, Object>> sedes = citaDAO.obtenerSedes();
        List<Map<String, Object>> medicos = citaDAO.obtenerTodosMedicosDetalle();
        Map<String, Object> estadoListaEspera = idPaciente > 0 ? citaDAO.obtenerEstadoListaEspera(idPaciente) : null;
        int numNotifSinLeer = usuario != null ? citaDAO.contarNotificacionesSinLeer(usuario.getIdUsuario()) : 0;
        String nombrePaciente = usuario != null ? usuario.getNombreCompleto() : "Paciente";

        request.setAttribute("listaEspecialidades", especialidades);
        request.setAttribute("listaSedes", sedes);
        request.setAttribute("listaMedicos", medicos);
        request.setAttribute("estadoListaEspera", estadoListaEspera);
        request.setAttribute("numNotifSinLeer", numNotifSinLeer);
        request.setAttribute("nombrePaciente", nombrePaciente);

        request.getRequestDispatcher("/WEB-INF/views/paciente/lista_espera.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login?error=sesion_requerida");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        int usuarioId = usuario != null ? usuario.getIdUsuario() : -1;
        int idPaciente = citaDAO.obtenerIdPacientePorUsuarioId(usuarioId);

        if (idPaciente <= 0) {
            response.sendRedirect(request.getContextPath() + "/paciente/lista-espera?error=paciente_no_encontrado");
            return;
        }

        String action = request.getParameter("action");

        if ("retirar".equalsIgnoreCase(action)) {
            String idListaStr = request.getParameter("idListaEspera");
            if (idListaStr != null && !idListaStr.trim().isEmpty()) {
                try {
                    int idLista = Integer.parseInt(idListaStr);
                    citaDAO.retirarListaEspera(idLista, idPaciente);
                    citaDAO.crearNotificacion(usuarioId, null, "Lista de Espera", "Te has retirado de la lista de espera correctamente.");
                } catch (NumberFormatException ignored) {}
            }
            response.sendRedirect(request.getContextPath() + "/paciente/lista-espera?retirado=ok");
            return;
        }

        // Caso por defecto: Registrarse en lista de espera
        try {
            int idEspecialidad = Integer.parseInt(request.getParameter("especialidadId"));
            
            String sedeParam = request.getParameter("sede");
            Integer idSede = 1; // Sede San Borja por defecto
            if ("lima".equalsIgnoreCase(sedeParam) || "2".equals(sedeParam)) {
                idSede = 2;
            } else if ("san_borja".equalsIgnoreCase(sedeParam) || "1".equals(sedeParam)) {
                idSede = 1;
            } else if ("cualquiera".equalsIgnoreCase(sedeParam)) {
                idSede = 1;
            } else if (sedeParam != null) {
                try {
                    idSede = Integer.parseInt(sedeParam);
                } catch (NumberFormatException ignored) {}
            }

            String[] horarios = request.getParameterValues("horarios");
            String rangoHorario = "MANANA";
            if (horarios != null && horarios.length > 0) {
                boolean tieneManana = false, tieneTarde = false;
                for (String h : horarios) {
                    if ("manana".equalsIgnoreCase(h) || "mañana".equalsIgnoreCase(h)) tieneManana = true;
                    if ("tarde".equalsIgnoreCase(h)) tieneTarde = true;
                }
                if (tieneManana && tieneTarde) rangoHorario = "INDIFERENTE";
                else if (tieneTarde) rangoHorario = "TARDE";
                else rangoHorario = "MANANA";
            }

            String disponibilidad = request.getParameter("disponibilidad");
            String nivelUrgencia = "flexible".equalsIgnoreCase(disponibilidad) || "3h".equalsIgnoreCase(disponibilidad) ? "ALTA" : "MEDIA";

            String[] canales = request.getParameterValues("canales");
            String canalNotif = "WHATSAPP";
            if (canales != null && canales.length > 0) {
                boolean tieneWhatsapp = false, tieneCorreo = false;
                for (String c : canales) {
                    if ("sms".equalsIgnoreCase(c) || "whatsapp".equalsIgnoreCase(c)) tieneWhatsapp = true;
                    if ("email".equalsIgnoreCase(c) || "correo".equalsIgnoreCase(c)) tieneCorreo = true;
                }
                if (tieneWhatsapp && tieneCorreo) canalNotif = "WHATSAPP,CORREO";
                else if (tieneCorreo) canalNotif = "CORREO";
                else canalNotif = "WHATSAPP";
            }

            boolean ok = citaDAO.inscribirListaEspera(idPaciente, idEspecialidad, idSede, rangoHorario, nivelUrgencia, canalNotif);
            if (ok) {
                citaDAO.crearNotificacion(usuarioId, null, "Lista de Espera", "¡Inscripción confirmada! Te avisaremos por " + canalNotif + " apenas un cupo esté disponible.");
                response.sendRedirect(request.getContextPath() + "/paciente/lista-espera?inscrito=ok");
            } else {
                response.sendRedirect(request.getContextPath() + "/paciente/lista-espera?error=fallo_registro");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/paciente/lista-espera?error=parametros_invalidos");
        }
    }
}
