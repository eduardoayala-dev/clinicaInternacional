package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.*;

public class AmbitoAdmisionDAO {
    public enum Recurso {
        CITA,
        MEDICO,
        HORARIO,
        PACIENTE
    }

    private final AgendaCitaDAO.ConnectionSource conexiones;

    public AmbitoAdmisionDAO() {
        this(DatabaseConfig::getConnection);
    }

    AmbitoAdmisionDAO(AgendaCitaDAO.ConnectionSource conexiones) {
        this.conexiones = conexiones;
    }

    public int sede(int usuario) {
        try (var c = conexiones.open()) {
            return sede(c, usuario);
        } catch (SQLException e) {
            throw new AgendaException(
                    503, "AMBITO_NO_DISPONIBLE", "No se pudo comprobar la sede del personal.");
        }
    }

    public String nombreSede(int usuario) {
        try (var c = conexiones.open()) {
            var perfil =
                    new pe.edu.utp.clinica.repository.AmbitoAdmisionRepository(c)
                            .sedeConNombre(usuario);
            if (perfil == null)
                throw new AgendaException(
                        403, "ADMISION_NO_HABILITADA", "El perfil de admisión no está habilitado.");
            return perfil.nombre();
        } catch (SQLException e) {
            throw new AgendaException(503, "AMBITO_NO_DISPONIBLE", "No se pudo comprobar la sede.");
        }
    }

    static int sede(Connection c, int usuario) throws SQLException {
        var perfil = new pe.edu.utp.clinica.repository.AmbitoAdmisionRepository(c).sede(usuario);
        if (perfil == null)
            throw new AgendaException(
                    403, "ADMISION_NO_HABILITADA", "El perfil de admisión no está habilitado.");
        return perfil.id();
    }

    public void exigir(int usuario, Recurso recurso, int id) {
        TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    exigir(c, sede(c, usuario), recurso, id);
                    return null;
                });
    }

    public record EdicionPaciente(
            String nombres,
            String apellidos,
            String telefono,
            String direccion,
            String cobertura) {}

    public void editarPaciente(int usuario, int paciente, EdicionPaciente datos) {
        if (datos == null
                || !texto(datos.nombres(), 100)
                || !texto(datos.apellidos(), 100)
                || !texto(datos.telefono(), 20)
                || !texto(datos.direccion(), 255)
                || !texto(datos.cobertura(), 50))
            throw new AgendaException(
                    400,
                    "DATOS_INVALIDOS",
                    "Completa los datos del paciente sin superar sus límites.");
        TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    int sede = sede(c, usuario);
                    if (new pe.edu.utp.clinica.repository.ReservaCitaRepository(c)
                                    .paciente(paciente)
                            == null)
                        throw new AgendaException(
                                404, "PACIENTE_NO_ENCONTRADO", "No se encontró el paciente.");
                    exigir(c, sede, Recurso.PACIENTE, paciente);
                    new pe.edu.utp.clinica.repository.AmbitoAdmisionRepository(c)
                            .editarPaciente(paciente, datos);
                    new pe.edu.utp.clinica.repository.UsuarioRepository(c)
                            .auditar(
                                    usuario,
                                    null,
                                    "PACIENTE_EDITADO",
                                    "Paciente #" + paciente + "; sede #" + sede,
                                    false);
                    return null;
                });
    }

    private static boolean texto(String s, int max) {
        return s != null
                && !s.isBlank()
                && s.length() <= max
                && s.chars().noneMatch(Character::isISOControl);
    }

    static void exigir(Connection c, int sede, Recurso recurso, int id) throws SQLException {
        if (id <= 0)
            throw new AgendaException(400, "DATOS_INVALIDOS", "El identificador no es válido.");
        if (!new pe.edu.utp.clinica.repository.AmbitoAdmisionRepository(c)
                .autorizado(sede, recurso, id))
            throw new AgendaException(
                    404,
                    "RECURSO_NO_ENCONTRADO",
                    "No se encontró un recurso autorizado en tu sede.");
    }
}
