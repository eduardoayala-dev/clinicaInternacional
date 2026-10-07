package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.clinica.model.Usuario;

import java.io.IOException;

@WebServlet(name = "ReservarCitaServlet", urlPatterns = {"/paciente/reservar"})
public class ReservarCitaServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Usuario usuario = (session != null) ? (Usuario) session.getAttribute("usuarioLogueado") : null;
        if (usuario == null || !"PACIENTE".equals(usuario.getRol())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso denegado");
            return;
        }

        try {
            int idSede = Integer.parseInt(request.getParameter("idSede"));
            int idEspecialidad = Integer.parseInt(request.getParameter("idEspecialidad"));
            int idMedico = Integer.parseInt(request.getParameter("idMedico"));
            String fechaCita = request.getParameter("fechaCita"); // Format: 2026-10-15T10:30

            // Here we would call CitaDAO.registrarCita(...)
            // Since we haven't implemented the DB insert yet, we just redirect to dashboard with success
            System.out.println("Cita Reservada: " + fechaCita + " Sede: " + idSede + " Esp: " + idEspecialidad + " Medico: " + idMedico);
            
            response.sendRedirect(request.getContextPath() + "/paciente/dashboard?reserva=success");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/paciente/citas?action=nueva&error=true");
        }
    }
}
