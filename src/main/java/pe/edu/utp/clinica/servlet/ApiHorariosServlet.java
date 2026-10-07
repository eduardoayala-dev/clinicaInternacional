package pe.edu.utp.clinica.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.dao.CitaDAO;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet(name = "ApiHorariosServlet", urlPatterns = {"/api/horarios"})
public class ApiHorariosServlet extends HttpServlet {

    private CitaDAO citaDAO;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        this.citaDAO = new CitaDAO();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            int idMedico = Integer.parseInt(request.getParameter("idMedico"));
            String fecha = request.getParameter("fecha");
            
            String jsonResponse;
            if (fecha != null && !fecha.trim().isEmpty()) {
                // Obtener horas disponibles para esa fecha
                List<String> horas = citaDAO.obtenerHorariosDisponibles(idMedico, fecha);
                jsonResponse = objectMapper.writeValueAsString(horas);
            } else {
                // Obtener lista de fechas disponibles
                List<String> fechas = citaDAO.obtenerFechasDisponibles(idMedico);
                jsonResponse = objectMapper.writeValueAsString(fechas);
            }

            try (PrintWriter out = response.getWriter()) {
                out.print(jsonResponse);
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("[]");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("[]");
        }
    }
}
