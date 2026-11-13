package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

public final class PadronMedicosDAO {
    public record Filtro(String buscar, String especialidad, String sede, String estado) {
        public Filtro {
            buscar = limpio(buscar);
            especialidad = limpio(especialidad);
            sede = limpio(sede);
            estado = limpio(estado);
            if ("Todos".equalsIgnoreCase(estado)) estado = "";
            if (buscar.length() > 100
                    || especialidad.length() > 100
                    || sede.length() > 100
                    || !Set.of("", "ACTIVO", "LICENCIA", "INACTIVO").contains(estado)) {
                throw new AgendaException(400, "FILTRO_INVALIDO", "Revisa los filtros del padrón.");
            }
        }

        private static String limpio(String valor) {
            return valor == null ? "" : valor.strip();
        }
    }

    public record Medico(
            int id,
            String nombre,
            String cmp,
            int especialidadId,
            String especialidad,
            String sede,
            String estado) {}

    public record Especialidad(int id, String nombre) {}

    public record Edicion(Medico medico, List<Especialidad> especialidades) {}

    private final AgendaCitaDAO.ConnectionSource conexiones;

    public PadronMedicosDAO() {
        this(DatabaseConfig::getConnection);
    }

    PadronMedicosDAO(AgendaCitaDAO.ConnectionSource conexiones) {
        this.conexiones = conexiones;
    }

    public List<Medico> listar(int actor, Filtro filtro) {
        return TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    int sede = AmbitoAdmisionDAO.sede(c, actor);
                    return List.copyOf(
                            new pe.edu.utp.clinica.repository.PadronMedicosRepository(c)
                                    .listar(sede, filtro, null));
                });
    }

    public Edicion consultar(int actor, int id) {
        return TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    var medico = bloquear(c, id, AmbitoAdmisionDAO.sede(c, actor));
                    return new Edicion(
                            medico,
                            List.copyOf(
                                    new pe.edu.utp.clinica.repository.PadronMedicosRepository(c)
                                            .especialidades(medico.especialidadId())));
                });
    }

    public void guardar(int actor, int id, int especialidad, String estado, String ip) {
        if (especialidad < 1
                || !Set.of("ACTIVO", "LICENCIA", "INACTIVO").contains(estado == null ? "" : estado))
            throw new AgendaException(
                    400, "DATOS_INVALIDOS", "Selecciona una especialidad y un estado válidos.");
        TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    int sede = AmbitoAdmisionDAO.sede(c, actor);
                    var anterior = bloquear(c, id, sede);
                    if (anterior.especialidadId() == especialidad
                            && anterior.estado().equals(estado)) return null;
                    if (!anterior.estado().equals(estado) && "ACTIVO".equals(estado))
                        exigirCuentaActiva(c, id);
                    if (anterior.especialidadId() != especialidad)
                        exigirEspecialidad(c, id, especialidad);
                    if (!"ACTIVO".equals(estado) || anterior.especialidadId() != especialidad)
                        exigirAgendaLibre(c, id);
                    new pe.edu.utp.clinica.repository.PadronMedicosRepository(c)
                            .guardar(id, sede, especialidad, estado);
                    new pe.edu.utp.clinica.repository.UsuarioRepository(c)
                            .auditar(
                                    actor,
                                    ip,
                                    "MEDICO_PROFESIONAL_EDITADO",
                                    "Médico #"
                                            + id
                                            + "; especialidad "
                                            + anterior.especialidadId()
                                            + " → "
                                            + especialidad
                                            + "; estado "
                                            + anterior.estado()
                                            + " → "
                                            + estado,
                                    false);
                    TransaccionJdbc.avisarSede(sede);
                    return null;
                });
    }

    private static Medico bloquear(Connection c, int id, int sede) throws SQLException {
        var medicos =
                new pe.edu.utp.clinica.repository.PadronMedicosRepository(c).listar(sede, null, id);
        if (medicos.isEmpty())
            throw new AgendaException(
                    404, "MEDICO_NO_ENCONTRADO", "No se encontró el médico en tu sede.");
        return medicos.get(0);
    }

    private static void exigirCuentaActiva(Connection c, int id) throws SQLException {
        if (!new pe.edu.utp.clinica.repository.PadronMedicosRepository(c).cuentaActiva(id))
            throw new AgendaException(
                    409,
                    "CUENTA_INACTIVA",
                    "El administrador debe habilitar la cuenta antes de activar el perfil médico.");
    }

    private static void exigirEspecialidad(Connection c, int id, int especialidad)
            throws SQLException {
        if (!new pe.edu.utp.clinica.repository.PerfilProfesionalRepository(c)
                .especialidadActiva(especialidad))
            throw new AgendaException(
                    409, "ESPECIALIDAD_INACTIVA", "Selecciona una especialidad activa.");
        if (new pe.edu.utp.clinica.repository.PadronMedicosRepository(c).tieneCitas(id))
            throw new AgendaException(
                    409,
                    "HISTORIAL_PROTEGIDO",
                    "No se cambia la especialidad de un médico con citas registradas. Coordina una"
                        + " nueva asignación con el administrador.");
    }

    private static void exigirAgendaLibre(Connection c, int id) throws SQLException {
        if (new pe.edu.utp.clinica.repository.PadronMedicosRepository(c)
                .agendaActiva(id, LocalDateTime.now(ZoneId.of("America/Lima"))))
            throw new AgendaException(
                    409,
                    "MEDICO_CON_AGENDA",
                    "Coordina las citas y los horarios publicados antes de realizar este cambio.");
    }
}
