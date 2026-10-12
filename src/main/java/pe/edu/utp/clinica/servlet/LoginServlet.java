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

@WebServlet(name = "LoginServlet", urlPatterns = {"/login", "/login/paciente", "/login/personal", "/logout"})
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

        HttpSession existingSession = request.getSession(false);
        if (existingSession != null && existingSession.getAttribute("usuarioLogueado") != null) {
            String rol = (String) existingSession.getAttribute("rol");
            if ("ADMINISTRADOR".equals(rol)) {
                response.sendRedirect(request.getContextPath() + "/sysadmin/dashboard");
                return;
            } else if ("PERSONAL_ADMINISTRATIVO".equals(rol)) {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                return;
            } else if ("MEDICO".equals(rol)) {
                response.sendRedirect(request.getContextPath() + "/medico/agenda");
                return;
            } else if ("PACIENTE".equals(rol)) {
                response.sendRedirect(request.getContextPath() + "/paciente/dashboard");
                return;
            }
        }

        HttpSession session = request.getSession(true);
        if (session.getAttribute("csrfToken") == null) {
            session.setAttribute("csrfToken", UUID.randomUUID().toString());
        }

        if ("/login/personal".equals(path)) {
            request.setAttribute("tipoPortal", "PERSONAL");
            request.getRequestDispatcher("/WEB-INF/views/login-personal.jsp").forward(request, response);
        } else {
            request.setAttribute("tipoPortal", "PACIENTE");
            request.getRequestDispatcher("/WEB-INF/views/login-paciente.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        String path = request.getServletPath();

        String requestCsrfToken = request.getParameter("csrfToken");
        String sessionCsrfToken = (session != null) ? (String) session.getAttribute("csrfToken") : null;

        if (session == null || requestCsrfToken == null || !requestCsrfToken.equals(sessionCsrfToken)) {
            HttpSession freshSession = request.getSession(true);
            freshSession.setAttribute("csrfToken", UUID.randomUUID().toString());
            request.setAttribute("errorMessage", "Tu sesión previa expiró por inactividad. Por favor, ingresa tus credenciales nuevamente.");

            if ("/login/personal".equals(path)) {
                request.setAttribute("tipoPortal", "PERSONAL");
                request.getRequestDispatcher("/WEB-INF/views/login-personal.jsp").forward(request, response);
            } else {
                request.setAttribute("tipoPortal", "PACIENTE");
                request.getRequestDispatcher("/WEB-INF/views/login-paciente.jsp").forward(request, response);
            }
            return;
        }

        String rawIdentificador = request.getParameter("identificador");
        String identificador = (rawIdentificador != null) ? SecurityUtil.sanitizeInput(rawIdentificador).trim() : "";
        String password = request.getParameter("password");
        String ipOrigen = request.getRemoteAddr();

        Usuario usuario = authService.autenticar(identificador, password, ipOrigen);

        if (usuario != null) {
            boolean esPortalPersonal = "/login/personal".equals(path);
            boolean esRolPersonal = "MEDICO".equals(usuario.getRol()) || "PERSONAL_ADMINISTRATIVO".equals(usuario.getRol()) || "ADMINISTRADOR".equals(usuario.getRol());

            if (esPortalPersonal && !esRolPersonal) {
                request.setAttribute("errorMessage", "Acceso denegado: Esta cuenta de Paciente debe ingresar por el Portal de Pacientes.");
                request.setAttribute("tipoPortal", "PERSONAL");
                request.getRequestDispatcher("/WEB-INF/views/login-personal.jsp").forward(request, response);
                return;
            }

            if (!esPortalPersonal && esRolPersonal) {
                request.setAttribute("errorMessage", "Acceso denegado: El personal médico y administrativo debe ingresar por el Portal Institucional.");
                request.setAttribute("tipoPortal", "PACIENTE");
                request.getRequestDispatcher("/WEB-INF/views/login-paciente.jsp").forward(request, response);
                return;
            }

            if (session != null) {
                session.invalidate();
            }
            HttpSession newSession = request.getSession(true);
            newSession.setAttribute("usuarioLogueado", usuario);
            newSession.setAttribute("rol", usuario.getRol());
            newSession.setAttribute("nombreUsuario", usuario.getNombreCompleto());
            newSession.setMaxInactiveInterval(30 * 60);

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
                    response.sendRedirect(request.getContextPath() + "/sysadmin/dashboard");
                    break;
                default:
                    response.sendRedirect(request.getContextPath() + "/login?error=rol_invalido");
            }
        } else {
            String errorMsg = authService.getLockoutMessage(identificador);
            request.setAttribute("errorMessage", errorMsg);
            request.getSession(true).setAttribute("csrfToken", UUID.randomUUID().toString());
            
            if ("/login/personal".equals(path)) {
                request.setAttribute("tipoPortal", "PERSONAL");
                request.getRequestDispatcher("/WEB-INF/views/login-personal.jsp").forward(request, response);
            } else {
                request.setAttribute("tipoPortal", "PACIENTE");
                request.getRequestDispatcher("/WEB-INF/views/login-paciente.jsp").forward(request, response);
            }
        }
    }
}
