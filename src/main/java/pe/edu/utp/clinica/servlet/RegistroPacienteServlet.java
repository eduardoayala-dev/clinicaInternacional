package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.model.Paciente;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.service.AuthService;
import pe.edu.utp.clinica.util.SecurityUtil;

import java.io.IOException;
import java.time.LocalDate;

@WebServlet(name = "RegistroPacienteServlet", urlPatterns = { "/registro" })
public class RegistroPacienteServlet extends HttpServlet {

    private AuthService authService;

    @Override
    public void init() throws ServletException {
        this.authService = new AuthService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/login?action=register");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String nombres = SecurityUtil.sanitizeInput(request.getParameter("nombres"));
        String apellidos = SecurityUtil.sanitizeInput(request.getParameter("apellidos"));
        String dni = SecurityUtil.sanitizeInput(request.getParameter("dni"));
        String fechaNacimiento = SecurityUtil.sanitizeInput(request.getParameter("fechaNacimiento"));
        String telefono = SecurityUtil.sanitizeInput(request.getParameter("telefono"));
        String email = SecurityUtil.sanitizeInput(request.getParameter("email"));
        String direccion = SecurityUtil.sanitizeInput(request.getParameter("direccion"));
        String password = request.getParameter("password");
        String terminos = request.getParameter("terminos");

        if (terminos == null) {
            request.getSession().setAttribute("registerError", "Debe aceptar la política de privacidad.");
            response.sendRedirect(request.getContextPath() + "/login?action=register");
            return;
        }

        if (password == null || password.length() < 8) {
            request.getSession().setAttribute("registerError", "La contraseña debe tener al menos 8 caracteres.");
            response.sendRedirect(request.getContextPath() + "/login?action=register");
            return;
        }

        if (dni != null && !dni.isEmpty() && password.contains(dni)) {
            request.getSession().setAttribute("registerError", "Por razones de seguridad, la contraseña no debe contener su número de documento.");
            response.sendRedirect(request.getContextPath() + "/login?action=register");
            return;
        }

        // Validate password complexity
        String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_\\-]).{8,}$";
        if (!password.matches(pattern)) {
            request.getSession().setAttribute("registerError", "La contraseña debe tener al menos una mayúscula, una minúscula, un número y un símbolo.");
            response.sendRedirect(request.getContextPath() + "/login?action=register");
            return;
        }

        if (authService.existeDocumentoOCorreo(dni, email)) {
            request.getSession().setAttribute("registerError", "El documento o correo electrónico ya se encuentra registrado.");
            response.sendRedirect(request.getContextPath() + "/login?action=register");
            return;
        }

        Usuario u = new Usuario();
        u.setNombres(nombres);
        u.setApellidos(apellidos);
        u.setEmail(email);
        u.setPasswordHash(password);

        Paciente p = new Paciente();
        p.setDni(dni);
        p.setFechaNacimiento(LocalDate.parse(fechaNacimiento));
        p.setTelefono(telefono);
        p.setDireccion(direccion);

        boolean exito = authService.registrarPaciente(u, p);

        if (exito) {
            response.sendRedirect(request.getContextPath() + "/login?msg=registro_exitoso");
        } else {
            request.getSession().setAttribute("registerError", "Ocurrió un error al crear la cuenta. Inténtelo de nuevo.");
            response.sendRedirect(request.getContextPath() + "/login?action=register");
        }
    }
}
