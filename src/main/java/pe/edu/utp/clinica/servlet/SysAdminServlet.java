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
import pe.edu.utp.clinica.config.DatabaseConfig;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "SysAdminServlet", urlPatterns = {
    "/sysadmin/dashboard",
    "/sysadmin/inicio",
    "/sysadmin/usuarios",
    "/sysadmin/sedes",
    "/sysadmin/priorizacion",
    "/sysadmin/analitica"
})
public class SysAdminServlet extends HttpServlet {

    private UsuarioDAO usuarioDAO;
    private CitaDAO citaDAO;
    private pe.edu.utp.clinica.dao.SedeDAO sedeDAO;

    @Override
    public void init() throws ServletException {
        this.usuarioDAO = new UsuarioDAO();
        this.citaDAO = new CitaDAO();
        this.sedeDAO = new pe.edu.utp.clinica.dao.SedeDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login/personal?error=sesion_requerida");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (!"ADMINISTRADOR".equals(usuario.getRol())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso no autorizado al módulo de Administrador del Sistema.");
            return;
        }

        String path = request.getServletPath();
        request.setAttribute("nombreAdmin", usuario.getNombreCompleto());

        switch (path) {
            case "/sysadmin/usuarios":
                List<Usuario> todos = usuarioDAO.obtenerTodos();
                List<Usuario> personal = new ArrayList<>();
                for (Usuario u : todos) {
                    if (!"PACIENTE".equals(u.getRol())) {
                        personal.add(u);
                    }
                }
                request.setAttribute("listaUsuarios", personal);
                cargarSedesYEspecialidades(request);
                request.getRequestDispatcher("/WEB-INF/views/sysadmin/usuarios.jsp").forward(request, response);
                break;

            case "/sysadmin/sedes":
                List<pe.edu.utp.clinica.model.Sede> listaSedes = sedeDAO.listarTodas();
                Map<String, Object> metricasSedes = sedeDAO.obtenerMetricasSedes();
                request.setAttribute("listaSedes", listaSedes);
                request.setAttribute("metricasSedes", metricasSedes);
                request.getRequestDispatcher("/WEB-INF/views/sysadmin/sedes.jsp").forward(request, response);
                break;

            case "/sysadmin/priorizacion":
                request.getRequestDispatcher("/WEB-INF/views/sysadmin/priorizacion.jsp").forward(request, response);
                break;

            case "/sysadmin/analitica":
                request.getRequestDispatcher("/WEB-INF/views/sysadmin/analitica.jsp").forward(request, response);
                break;

            case "/sysadmin/dashboard":
            case "/sysadmin/inicio":
            default:
                cargarMetricsSysAdmin(request);
                request.getRequestDispatcher("/WEB-INF/views/sysadmin/dashboard.jsp").forward(request, response);
                break;
        }
    }

    private void cargarMetricsSysAdmin(HttpServletRequest request) {
        int totalUsuarios = 0;
        int countPacientes = 0;
        int countMedicos = 0;
        int countPersonal = 0;
        int countAdmins = 0;
        int countSedes = 0;
        int countCitas = 0;

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Count medicos directly from medico table
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM medico");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) countMedicos = rs.getInt(1);
            }

            // Count pacientes directly from paciente table
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM paciente");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) countPacientes = rs.getInt(1);
            }

            // Count personal directly from personal_administrativo table
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM personal_administrativo");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) countPersonal = rs.getInt(1);
            }

            // Count admins directly from administrador table
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM administrador");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) countAdmins = rs.getInt(1);
            }

            totalUsuarios = countMedicos + countPacientes + countPersonal + countAdmins;

            // Count sedes
            String sqlSedes = "SELECT COUNT(*) FROM sede WHERE estado = 'ACTIVO'";
            try (PreparedStatement ps = conn.prepareStatement(sqlSedes);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) countSedes = rs.getInt(1);
            }

            // Count citas
            String sqlCitas = "SELECT COUNT(*) FROM cita";
            try (PreparedStatement ps = conn.prepareStatement(sqlCitas);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) countCitas = rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        request.setAttribute("totalUsuarios", totalUsuarios);
        request.setAttribute("countPacientes", countPacientes);
        request.setAttribute("countMedicos", countMedicos);
        request.setAttribute("countPersonal", countPersonal);
        request.setAttribute("countAdmins", countAdmins);
        request.setAttribute("countSedes", countSedes);
        request.setAttribute("countCitas", countCitas);

        // Fetch radar citas
        List<Map<String, Object>> radarCitas = citaDAO.obtenerCitasConRiesgo();
        request.setAttribute("radarCitas", radarCitas);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login/personal");
            return;
        }

        String action = request.getParameter("action");

        if ("cambiarEstado".equals(action)) {
            try {
                int idUsuario = Integer.parseInt(request.getParameter("idUsuario"));
                boolean nuevoEstado = Boolean.parseBoolean(request.getParameter("estado"));
                boolean exito = usuarioDAO.cambiarEstadoUsuario(idUsuario, nuevoEstado);
                if (exito) {
                    session.setAttribute("flashMessage", "Estado del usuario #" + idUsuario + " actualizado a " + (nuevoEstado ? "ACTIVO" : "INACTIVO") + " en la Base de Datos.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            response.sendRedirect(request.getContextPath() + "/sysadmin/usuarios");
            return;
        }

        if ("registrar".equals(action) || "editar".equals(action)) {
            String nombres = request.getParameter("nombres");
            String apellidos = request.getParameter("apellidos");
            String email = request.getParameter("email");
            String rol = request.getParameter("rol");

            Usuario u = new Usuario();
            u.setNombres(nombres);
            u.setApellidos(apellidos);
            u.setEmail(email);
            u.setRol(rol);

            String cmp = null;
            Integer idEspecialidad = null;
            Integer idSede = null;
            String cargo = null;

            if ("MEDICO".equals(rol)) {
                cmp = request.getParameter("cmp");
                String espStr = request.getParameter("id_especialidad");
                if (espStr != null && !espStr.isEmpty()) idEspecialidad = Integer.parseInt(espStr);
                String sedeStr = request.getParameter("id_sede_medico");
                if (sedeStr != null && !sedeStr.isEmpty()) idSede = Integer.parseInt(sedeStr);
            } else if ("PERSONAL_ADMINISTRATIVO".equals(rol)) {
                cargo = request.getParameter("cargo");
                String sedeStr = request.getParameter("id_sede_personal");
                if (sedeStr != null && !sedeStr.isEmpty()) idSede = Integer.parseInt(sedeStr);
            }

            if ("editar".equals(action)) {
                u.setIdUsuario(Integer.parseInt(request.getParameter("idUsuario")));
                usuarioDAO.actualizarPersonal(u, cmp, idEspecialidad, idSede, cargo);
                session.setAttribute("flashMessage", "Usuario actualizado correctamente en la Base de Datos.");
            } else {
                String passwordRaw = request.getParameter("password");
                if (passwordRaw == null || passwordRaw.trim().isEmpty()) passwordRaw = "Password123!";
                u.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw(passwordRaw, org.mindrot.jbcrypt.BCrypt.gensalt()));
                u.setActivo(true);
                usuarioDAO.registrarPersonal(u, cmp, idEspecialidad, idSede, cargo);
                session.setAttribute("flashMessage", "Nuevo usuario (" + nombres + " " + apellidos + ") guardado en la Base de Datos.");
            }

            response.sendRedirect(request.getContextPath() + "/sysadmin/usuarios");
            return;
        }

        if ("guardarPriorizacion".equals(action)) {
            session.setAttribute("flashMessage", "Parámetros de priorización y tiempo límite de respuesta actualizados correctamente.");
            response.sendRedirect(request.getContextPath() + "/sysadmin/priorizacion");
            return;
        }

        if ("crearSede".equals(action)) {
            String codigo = request.getParameter("codigo");
            String nombre = request.getParameter("nombre");
            String direccion = request.getParameter("direccion");
            String telefono = request.getParameter("telefono");
            String estado = request.getParameter("estado");

            pe.edu.utp.clinica.model.Sede s = new pe.edu.utp.clinica.model.Sede(
                0,
                codigo != null ? codigo.trim() : "",
                nombre != null ? nombre.trim() : "",
                direccion != null ? direccion.trim() : "",
                telefono != null ? telefono.trim() : "",
                null,
                estado != null ? estado.trim() : "ACTIVO"
            );

            boolean ok = sedeDAO.crearSede(s);
            if (ok) {
                session.setAttribute("flashMessage", "¡Sede " + s.getNombre() + " (" + s.getCodigo() + ") guardada exitosamente en la Base de Datos!");
            } else {
                session.setAttribute("flashError", "Error al crear la sede. Verifica que el código no esté duplicado.");
            }
            response.sendRedirect(request.getContextPath() + "/sysadmin/sedes");
            return;
        }

        if ("editarSede".equals(action)) {
            try {
                int idSede = Integer.parseInt(request.getParameter("idSede"));
                String codigo = request.getParameter("codigo");
                String nombre = request.getParameter("nombre");
                String direccion = request.getParameter("direccion");
                String telefono = request.getParameter("telefono");
                String estado = request.getParameter("estado");

                pe.edu.utp.clinica.model.Sede s = new pe.edu.utp.clinica.model.Sede(
                    idSede,
                    codigo != null ? codigo.trim() : "",
                    nombre != null ? nombre.trim() : "",
                    direccion != null ? direccion.trim() : "",
                    telefono != null ? telefono.trim() : "",
                    null,
                    estado != null ? estado.trim() : "ACTIVO"
                );

                boolean ok = sedeDAO.actualizarSede(s);
                if (ok) {
                    session.setAttribute("flashMessage", "Sede " + s.getNombre() + " actualizada correctamente.");
                } else {
                    session.setAttribute("flashError", "No se pudo actualizar la sede.");
                }
            } catch (Exception e) {
                session.setAttribute("flashError", "Datos inválidos para actualizar la sede.");
            }
            response.sendRedirect(request.getContextPath() + "/sysadmin/sedes");
            return;
        }

        if ("cambiarEstadoSede".equals(action)) {
            try {
                int idSede = Integer.parseInt(request.getParameter("idSede"));
                String nuevoEstado = request.getParameter("estado");
                boolean ok = sedeDAO.cambiarEstado(idSede, nuevoEstado);
                if (ok) {
                    session.setAttribute("flashMessage", "Estado de la sede #" + idSede + " cambiado a " + nuevoEstado + ".");
                } else {
                    session.setAttribute("flashError", "No se pudo cambiar el estado de la sede.");
                }
            } catch (Exception e) {
                session.setAttribute("flashError", "Error al procesar cambio de estado de sede.");
            }
            response.sendRedirect(request.getContextPath() + "/sysadmin/sedes");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/sysadmin/dashboard");
    }

    private void cargarSedesYEspecialidades(HttpServletRequest request) {
        List<Map<String, Object>> sedes = new ArrayList<>();
        List<Map<String, Object>> especialidades = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT id_sede, nombre FROM sede WHERE estado='ACTIVO'");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", rs.getInt("id_sede"));
                    m.put("nombre", rs.getString("nombre"));
                    sedes.add(m);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT id_especialidad, nombre FROM especialidad");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", rs.getInt("id_especialidad"));
                    m.put("nombre", rs.getString("nombre"));
                    especialidades.add(m);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        request.setAttribute("sedes", sedes);
        request.setAttribute("especialidades", especialidades);
    }
}
