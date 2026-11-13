package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.repository.ConfirmacionRepository;
import pe.edu.utp.clinica.repository.ConfirmacionRepository.Cita;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ConfirmacionAsistenciaDAO {
    private static final Set<String> ESTADOS_ACTIVOS = Set.of("SOLICITADA", "CONFIRMADA");
    private final AgendaCitaDAO.ConnectionSource connections;
    private final Clock clock;

    public record Seguimiento(
            int idCita,
            String paciente,
            String documento,
            String correo,
            String telefono,
            String fechaHora,
            String estado,
            int intentos,
            String ultimoContacto,
            String ultimoResultado,
            boolean vencida) {}

    public record ResultadoContacto(boolean asistenciaConfirmada) {}

    public record EstadoAsistencia(boolean confirmada, boolean vencida, String venceEn) {}

    public ConfirmacionAsistenciaDAO() {
        this(DatabaseConfig::getConnection, Clock.system(ZoneId.of("America/Lima")));
    }

    ConfirmacionAsistenciaDAO(AgendaCitaDAO.ConnectionSource connections, Clock clock) {
        this.connections = connections;
        this.clock = clock.withZone(ZoneId.of("America/Lima"));
    }

    public Map<Integer, EstadoAsistencia> estadoDelPaciente(int idUsuario, List<Integer> citas) {
        if (idUsuario <= 0 || citas == null || citas.isEmpty()) return Map.of();
        Set<Integer> solicitadas = Set.copyOf(citas);
        try (var conn = connections.open()) {
            return new ConfirmacionRepository(conn)
                    .estados(idUsuario, solicitadas, LocalDateTime.now(clock));
        } catch (SQLException e) {
            throw new AgendaException(
                    503,
                    "CONFIRMACION_NO_DISPONIBLE",
                    "No se pudo consultar el estado de asistencia.");
        }
    }

    public void confirmarPaciente(int idUsuario, int idCita) {
        if (idUsuario <= 0 || idCita <= 0) throw invalida();
        TransaccionJdbc.ejecutar(
                connections,
                conn -> {
                    confirmarPaciente(conn, idUsuario, idCita);
                    return null;
                });
    }

    void confirmarPaciente(Connection conn, int idUsuario, int idCita) throws SQLException {
        int paciente = pacientePropio(conn, idUsuario);
        bloquearPaciente(conn, paciente);
        Cita cita = citaPaciente(conn, idCita, paciente);
        LocalDateTime ahora = LocalDateTime.now(clock).withNano(0);
        if (!ESTADOS_ACTIVOS.contains(cita.estado()) || !cita.fechaHora().isAfter(ahora))
            throw noConfirmable();
        if (cita.confirmada()) return;
        if (!ahora.isBefore(cita.venceEn()))
            throw new AgendaException(
                    409,
                    "PLAZO_CONFIRMACION_VENCIDO",
                    "El plazo de confirmación terminó. Contacta con admisión para coordinar tu"
                        + " cita.");
        if (new ConfirmacionRepository(conn).confirmar(idCita, idUsuario, "WEB", ahora) != 1)
            throw noConfirmable();
        TransaccionJdbc.avisarSede(cita.sede());
    }

    public int sedeDelPersonal(int usuario) {
        try (Connection conn = connections.open()) {
            return sedePersonal(conn, usuario);
        } catch (SQLException e) {
            throw new AgendaException(
                    503, "SEGUIMIENTO_NO_DISPONIBLE", "No se pudo comprobar la sede.");
        }
    }

    public static final int FILAS_POR_PAGINA = 5;

    public record PaginaSeguimiento(
            List<Seguimiento> registros, long total, long vencidas, int pagina, int paginas) {}

    public PaginaSeguimiento consultarPagina(int idUsuario, int solicitada) {
        if (solicitada < 1 || solicitada > 100000)
            throw new AgendaException(400, "PAGINA_INVALIDA", "Selecciona una página válida.");
        return TransaccionJdbc.ejecutar(
                connections,
                conn -> {
                    conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                    int sede = sedePersonal(conn, idUsuario);
                    LocalDateTime ahora = LocalDateTime.now(clock).withNano(0);
                    var conteo = new ConfirmacionRepository(conn).contar(sede, ahora);
                    long total = conteo.total(), vencidas = conteo.vencidas();
                    int paginas =
                            Math.max(1, (int) ((total + FILAS_POR_PAGINA - 1) / FILAS_POR_PAGINA));
                    int pagina = Math.min(solicitada, paginas);
                    return new PaginaSeguimiento(
                            pendientes(
                                    conn,
                                    sede,
                                    ahora,
                                    FILAS_POR_PAGINA,
                                    (pagina - 1) * FILAS_POR_PAGINA),
                            total,
                            vencidas,
                            pagina,
                            paginas);
                });
    }

    public List<Seguimiento> listarPendientes(int idUsuario) {
        try (Connection conn = connections.open()) {
            return pendientes(
                    conn,
                    sedePersonal(conn, idUsuario),
                    LocalDateTime.now(clock).withNano(0),
                    Integer.MAX_VALUE,
                    0);
        } catch (SQLException e) {
            throw new AgendaException(
                    503,
                    "SEGUIMIENTO_NO_DISPONIBLE",
                    "No se pudo consultar el seguimiento de confirmaciones.");
        }
    }

    private static List<Seguimiento> pendientes(
            Connection conn, int sede, LocalDateTime ahora, int limite, int offset)
            throws SQLException {
        return new ConfirmacionRepository(conn).pendientes(sede, ahora, limite, offset);
    }

    public ResultadoContacto registrarContacto(
            int idUsuario, int idCita, String canal, String resultado) {
        if (idUsuario <= 0 || idCita <= 0) throw invalida();
        if (canal == null
                || resultado == null
                || !Set.of("LLAMADA", "CORREO").contains(canal)
                || !Set.of("SIN_RESPUESTA", "CONFIRMA", "NO_CONFIRMA").contains(resultado))
            throw invalida();
        return TransaccionJdbc.ejecutar(
                connections,
                conn -> {
                    int sede = sedePersonal(conn, idUsuario);
                    Cita cita = bloquearCitaAdmision(conn, idCita, sede);
                    LocalDateTime ahora = LocalDateTime.now(clock).withNano(0);
                    if (!ESTADOS_ACTIVOS.contains(cita.estado())
                            || !cita.fechaHora().isAfter(ahora)) throw noConfirmable();
                    if (cita.confirmada())
                        throw new AgendaException(
                                409,
                                "ASISTENCIA_YA_CONFIRMADA",
                                "La asistencia ya está confirmada.");
                    var repo = new ConfirmacionRepository(conn);
                    repo.contacto(idCita, idUsuario, canal, resultado, ahora);
                    boolean confirmada = "CONFIRMA".equals(resultado);
                    if (confirmada && repo.confirmar(idCita, idUsuario, "ADMISION", ahora) != 1)
                        throw noConfirmable();
                    TransaccionJdbc.avisarSede(sede);
                    return new ResultadoContacto(confirmada);
                });
    }

    public int intentosMinimos(int usuario) {
        try (var conn = connections.open()) {
            sedePersonal(conn, usuario);
            return ConfiguracionOfertasDAO.leer(conn).intentosConfirmacion();
        } catch (SQLException e) {
            throw new AgendaException(
                    503,
                    "SEGUIMIENTO_NO_DISPONIBLE",
                    "No se pudo consultar la política de contacto.");
        }
    }

    public void cancelarSinConfirmacion(int usuario, int idCita) {
        if (usuario <= 0 || idCita <= 0) throw invalida();
        TransaccionJdbc.ejecutar(
                connections,
                conn -> {
                    int sede = sedePersonal(conn, usuario);
                    Integer paciente =
                            new ConfirmacionRepository(conn).pacienteEnSede(idCita, sede);
                    if (paciente == null) throw noConfirmable();
                    bloquearPaciente(conn, paciente);
                    Cita cita = bloquearCitaAdmision(conn, idCita, sede);
                    var ahora = LocalDateTime.now(clock).withNano(0);
                    if (!ESTADOS_ACTIVOS.contains(cita.estado())
                            || !cita.fechaHora().isAfter(ahora)
                            || cita.confirmada()) throw noConfirmable();
                    if (cita.venceEn() == null || ahora.isBefore(cita.venceEn()))
                        throw new AgendaException(
                                409,
                                "PLAZO_CONFIRMACION_VIGENTE",
                                "Todavía no vence el plazo de confirmación.");
                    var parametros = ConfiguracionOfertasDAO.leer(conn);
                    int intentos = new ConfirmacionRepository(conn).intentos(idCita);
                    if (intentos < parametros.intentosConfirmacion())
                        throw new AgendaException(
                                409,
                                "CONTACTOS_INSUFICIENTES",
                                "Registra al menos "
                                        + parametros.intentosConfirmacion()
                                        + " contactos realizados antes de cancelar por falta de"
                                        + " confirmación.");
                    new AdmisionCitaDAO(connections, clock)
                            .cancelar(
                                    conn,
                                    usuario,
                                    idCita,
                                    "Falta de confirmación tras vencer el plazo y completar los"
                                        + " contactos requeridos");
                    new pe.edu.utp.clinica.repository.UsuarioRepository(conn)
                            .auditar(
                                    usuario,
                                    null,
                                    "CITA_CANCELADA_SIN_CONFIRMAR",
                                    "Cita #"
                                            + idCita
                                            + "; contactos "
                                            + intentos
                                            + "; mínimo "
                                            + parametros.intentosConfirmacion()
                                            + "; política versión "
                                            + parametros.version(),
                                    false);
                    return null;
                });
    }

    private int pacientePropio(Connection conn, int usuario) throws SQLException {
        Integer paciente = new ConfirmacionRepository(conn).pacientePropio(usuario);
        if (paciente == null)
            throw new AgendaException(
                    403, "PACIENTE_NO_HABILITADO", "El perfil de paciente no está habilitado.");
        return paciente;
    }

    private static void bloquearPaciente(Connection conn, int paciente) throws SQLException {
        if (new pe.edu.utp.clinica.repository.ReservaCitaRepository(conn).paciente(paciente)
                == null) throw noConfirmable();
    }

    private static Cita citaPaciente(Connection conn, int cita, int paciente) throws SQLException {
        Cita encontrada = new ConfirmacionRepository(conn).bloquear(cita, paciente, null);
        if (encontrada == null) throw noConfirmable();
        return encontrada;
    }

    private static int sedePersonal(Connection conn, int usuario) throws SQLException {
        var sede = new pe.edu.utp.clinica.repository.AmbitoAdmisionRepository(conn).sede(usuario);
        if (sede == null)
            throw new AgendaException(
                    403, "ADMISION_NO_HABILITADA", "Tu perfil de admisión no está habilitado.");
        return sede.id();
    }

    private static Cita bloquearCitaAdmision(Connection conn, int cita, int sede)
            throws SQLException {
        Cita encontrada = new ConfirmacionRepository(conn).bloquear(cita, null, sede);
        if (encontrada == null) throw noConfirmable();
        return encontrada;
    }

    private static AgendaException invalida() {
        return new AgendaException(400, "DATOS_INVALIDOS", "Revisa los datos de la solicitud.");
    }

    private static AgendaException noConfirmable() {
        return new AgendaException(
                409,
                "CITA_NO_CONFIRMABLE",
                "La cita no está disponible para confirmar asistencia.");
    }
}
