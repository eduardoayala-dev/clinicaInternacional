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
import pe.edu.utp.clinica.util.CsrfToken;

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
            response.setHeader("Allow", "POST");
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
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
        CsrfToken.getOrCreate(session);
        if ("acceso_temporal".equals(request.getParameter("error"))) {
            request.setAttribute("errorMessage","Tu cuenta se creó. Inicia sesión para continuar.");
        }
        if ("sesion_formulario".equals(request.getParameter("error"))) {
            request.setAttribute("errorMessage",
                    "La sesión del formulario venció o cambió. Por seguridad, vuelve a ingresar tus datos.");
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

        // El filtro valida CSRF también para login, registro y cierre de sesión.
        if ("/logout".equals(path)) {
            try {
                var gestor=pe.edu.utp.clinica.auth.GestorAutenticacion.obtener(request);
                if (gestor!=null) gestor.cerrar(request,response);
                else if (session!=null) session.invalidate();
            } catch (java.sql.SQLException e) {
                response.setHeader("Retry-After","3"); response.sendError(503); return;
            }
            response.sendRedirect(request.getContextPath() + "/login?msg=sesion_cerrada");
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

            try {
                pe.edu.utp.clinica.auth.InicioSesion.iniciar(request,response,usuario);
            } catch (java.sql.SQLException e) {
                response.setHeader("Retry-After","3"); response.sendError(503); return;
            }

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
            CsrfToken.getOrCreate(request.getSession(true));
            
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
