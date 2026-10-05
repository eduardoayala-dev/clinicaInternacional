package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.service.AuthService;
import pe.edu.utp.clinica.util.SecurityUtil;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet de Autenticación e Inicio de Sesión (HU001).
 * Implementa Controles de Seguridad Obligatorios:
 * - Protección CSRF mediante Token por sesión
 * - Invalidador de Sesión previniendo Session Fixation
 * - Cookie de Sesión segura (HttpOnly y SameSite)
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/login", "/logout"})
public class LoginServlet extends HttpServlet {

    private AuthService authService;

    @Override
    public void init() throws ServletException {
        this.authService = new AuthService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getServletPath();

        if ("/logout".equals(path)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/login?msg=sesion_cerrada");
            return;
        }

        // Generar Token CSRF para el formulario de Login
        HttpSession session = request.getSession(true);
        if (session.getAttribute("csrfToken") == null) {
            session.setAttribute("csrfToken", UUID.randomUUID().toString());
        }

        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        
        // 1. Validar Token CSRF (HU020 / RNF-04)
        String requestCsrfToken = request.getParameter("csrfToken");
        if (session == null || requestCsrfToken == null || !requestCsrfToken.equals(session.getAttribute("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Petición rechazada: Token CSRF inválido.");
            return;
        }

        // 2. Sanitizar entradas (HU018)
        String email = SecurityUtil.sanitizeInput(request.getParameter("email"));
        String password = request.getParameter("password");
        String ipOrigen = request.getRemoteAddr();

        // 3. Ejecutar Autenticación en Capa de Negocio
        Usuario usuario = authService.autenticar(email, password, ipOrigen);

        if (usuario != null) {
            // Regenerar ID de Sesión para evitar Session Fixation Attack
            session.invalidate();
            HttpSession newSession = request.getSession(true);
            newSession.setAttribute("usuarioLogueado", usuario);
            newSession.setAttribute("rol", usuario.getRol());
            newSession.setAttribute("nombreUsuario", usuario.getNombreCompleto());
            newSession.setMaxInactiveInterval(30 * 60); // Expira tras 30 min de inactividad

            // Redirección por Rol (CA-1)
            switch (usuario.getRol()) {
                case "PACIENTE":
                    response.sendRedirect(request.getContextPath() + "/paciente/dashboard");
                    break;
                case "MEDICO":
                    response.sendRedirect(request.getContextPath() + "/medico/agenda");
                    break;
                case "PERSONAL_ADMINISTRATIVO":
                    response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                    break;
                case "ADMINISTRADOR":
                    response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                    break;
                default:
                    response.sendRedirect(request.getContextPath() + "/login?error=rol_invalido");
            }
        } else {
            request.setAttribute("errorMessage", "Credenciales incorrectas o cuenta bloqueada por 15 minutos debido a intentos fallidos.");
            // Regenerar nuevo CSRF token tras intento fallido
            request.getSession(true).setAttribute("csrfToken", UUID.randomUUID().toString());
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        }
    }
}
