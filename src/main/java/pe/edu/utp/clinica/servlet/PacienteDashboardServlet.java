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

@WebServlet(name = "PacienteDashboardServlet", urlPatterns = {"/paciente/dashboard", "/paciente/citas", "/paciente/lista-espera", "/paciente/notificaciones", "/paciente/perfil"})
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
            request.getRequestDispatcher("/WEB-INF/views/paciente/reservar_cita.jsp").forward(request, response);
            return;
        }

        // 1. Datos de Alerta de Cupo Anticipado Recuperado (Wireframe - Banner superior)
        Map<String, String> ofertaAnticipada = new HashMap<>();
        ofertaAnticipada.put("titulo", "CUPO ANTICIPADO DISPONIBLE POR CANCELACION");
        ofertaAnticipada.put("especialidad", "Cardiología");
        ofertaAnticipada.put("medico", "Dr. Carlos Mendoza");
        ofertaAnticipada.put("fechaOfrecida", "Viernes 18 de Septiembre - 10:15 AM");
        ofertaAnticipada.put("segundosRestantes", "872"); // ~14m 32s para timer dinámico JS
        ofertaAnticipada.put("prioridad", "PRIORIDAD ALTA");

        // 2. Módulo 01: Próxima Cita Agendada Principal (Wireframe + Mockup)
        Map<String, String> proximaCita = new HashMap<>();
        proximaCita.put("id", "CIT-2026-9482");
        proximaCita.put("fechaHora", "Jueves 24 de Setiembre de 2026 - 09:30 AM");
        proximaCita.put("especialidad", "Cardiología");
        proximaCita.put("medico", "Dr. Carlos Mendoza");
        proximaCita.put("cmp", "CMP 45892");
        proximaCita.put("ubicacion", "Sede Norte - Edificio Consultas A - Consultorio 302");
        proximaCita.put("modalidad", "Presencial");
        proximaCita.put("estado", "CONFIRMADA");
        proximaCita.put("protocolo", "Presentarse 35 min antes con documento de identidad y orden médica");

        // 3. Segunda Cita Secundaría (Mockup)
        Map<String, String> citaSecundaria = new HashMap<>();
        citaSecundaria.put("id", "CIT-2026-9510");
        citaSecundaria.put("fechaHora", "Lunes 14 de Septiembre - 10:00 AM");
        citaSecundaria.put("especialidad", "Neurología");
        citaSecundaria.put("medico", "Dr. Marcus Okonkwo");
        citaSecundaria.put("cmp", "CMP 51204");
        citaSecundaria.put("ubicacion", "Telemedicina - Aula Virtual Sala 04");
        citaSecundaria.put("modalidad", "Video");
        citaSecundaria.put("estado", "PENDIENTE_CONFIRMACION");

        List<Map<String, String>> listaProximasCitas = new ArrayList<>();
        listaProximasCitas.add(proximaCita);
        listaProximasCitas.add(citaSecundaria);

        // 4. Módulo 02: Estado de Lista de Espera (Wireframe)
        Map<String, String> listaEspera = new HashMap<>();
        listaEspera.put("turnoActivo", "#03");
        listaEspera.put("especialidad", "Dermatología");
        listaEspera.put("sede", "Sede Central");
        listaEspera.put("rangoHorario", "08:00 a 13:00 hrs");
        listaEspera.put("canalNotificacion", "SMS + PORTAL PACIENTE");
        listaEspera.put("prioridadTriaje", "ESTANDAR / SEGUIMIENTO");
        listaEspera.put("estadoRegistro", "REGISTRO ACTIVO");

        // 5. Estadísticas Generales (Mockup Pills)
        Map<String, Integer> stats = new HashMap<>();
        stats.put("proximasCitas", 2);
        stats.put("medicosAsignados", 4);
        stats.put("estudiosPendientes", 1);
        stats.put("consultasTotales", 18);

        // 6. Auditoría y Privacidad del Paciente (HIPAA / Ley 29733 Protección Datos Personales)
        String ultimoAcceso = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        String ipAcceso = request.getRemoteAddr();

        // Enviar variables a la vista JSP
        request.setAttribute("nombrePaciente", nombrePaciente);
        request.setAttribute("emailPaciente", emailPaciente);
        request.setAttribute("ofertaAnticipada", ofertaAnticipada);
        request.setAttribute("proximaCita", proximaCita);
        request.setAttribute("listaProximasCitas", listaProximasCitas);
        request.setAttribute("listaEspera", listaEspera);
        request.setAttribute("stats", stats);
        request.setAttribute("ultimoAcceso", ultimoAcceso);
        request.setAttribute("ipAcceso", ipAcceso);

        request.getRequestDispatcher("/WEB-INF/views/paciente/dashboard.jsp").forward(request, response);
    }
}
