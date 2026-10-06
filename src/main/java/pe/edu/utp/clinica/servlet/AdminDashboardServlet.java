package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.clinica.model.Usuario;

import java.io.IOException;

@WebServlet(name = "AdminDashboardServlet", urlPatterns = {"/admin/dashboard"})
public class AdminDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login/personal?error=acceso_denegado");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (!"ADMINISTRADOR".equals(usuario.getRol()) && !"PERSONAL_ADMINISTRATIVO".equals(usuario.getRol())) {
            response.sendRedirect(request.getContextPath() + "/login/personal?error=rol_invalido");
            return;
        }
        
        // Pass dummy data for the dashboard to match the wireframe requirements for now
        // Later we will connect this to a DAO to get real metrics
        
        request.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(request, response);
    }
}
