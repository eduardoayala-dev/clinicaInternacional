package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.dao.UsuarioDAO;
import pe.edu.utp.clinica.model.Usuario;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "AdminMedicosServlet", urlPatterns = {"/admin/medicos"})
public class AdminMedicosServlet extends HttpServlet {

    private UsuarioDAO usuarioDAO;
    private CitaDAO citaDAO;

    @Override
    public void init() throws ServletException {
        this.usuarioDAO = new UsuarioDAO();
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
        if (!"ADMINISTRADOR".equals(usuario.getRol()) && !"PERSONAL_ADMINISTRATIVO".equals(usuario.getRol())) {
            response.sendRedirect(request.getContextPath() + "/login/personal?error=rol_invalido");
            return;
        }

        List<Usuario> todosUsuarios = usuarioDAO.obtenerTodos();
        request.setAttribute("usuario", usuario);
        request.setAttribute("listaUsuarios", todosUsuarios);
        request.setAttribute("especialidades", citaDAO.obtenerEspecialidades());
        request.setAttribute("sedes", citaDAO.obtenerSedes());

        request.getRequestDispatcher("/WEB-INF/views/admin/medicos.jsp").forward(request, response);
    }
}
