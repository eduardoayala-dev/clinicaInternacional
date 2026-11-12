package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.repository.AdmisionRepository;
import pe.edu.utp.clinica.repository.AdmisionRepository.Cita;
import pe.edu.utp.clinica.repository.ReservaCitaRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class AdmisionCitaDAO {
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");
    private static final String CONFIRMADA = "CONFIRMADA";

    private final AgendaCitaDAO.ConnectionSource connections;
    private final Clock clock;
    private final Integer tolerancia;

    public record Llegada(
            int idCita, String fechaLlegada, boolean llegoTarde, int toleranciaMinutos) {}

    public AdmisionCitaDAO() {
        this(DatabaseConfig::getConnection, Clock.system(ZONA_LIMA));
    }

    AdmisionCitaDAO(AgendaCitaDAO.ConnectionSource connections, Clock clock) {
        this.connections = connections;
        this.clock = clock.withZone(ZONA_LIMA);
        this.tolerancia = null;
    }

    AdmisionCitaDAO(AgendaCitaDAO.ConnectionSource connections, Clock clock, int tolerancia) {
        if (tolerancia < 0 || tolerancia > 120)
            throw new IllegalArgumentException("La tolerancia debe estar entre 0 y 120 minutos.");
        this.connections = connections;
        this.clock = clock.withZone(ZONA_LIMA);
        this.tolerancia = tolerancia;
    }

    public int toleranciaMinutos() {
        return tolerancia == null
                ? new ConfiguracionOfertasDAO(connections, Clock.systemUTC())
                        .leer()
                        .toleranciaLlegada()
                : tolerancia;
    }

    private int tolerancia(Connection conn) throws SQLException {
        return tolerancia == null
                ? ConfiguracionOfertasDAO.leer(conn).toleranciaLlegada()
                : tolerancia;
    }

    protected boolean correosActivos() {
        return Boolean.parseBoolean(
                System.getenv().getOrDefault("APPOINTMENT_EMAIL_ENABLED", "false"));
    }

    public void cancelar(int usuario, int cita, String motivo) {
        String validado = pe.edu.utp.clinica.util.MotivoCancelacion.validar(motivo);
        TransaccionJdbc.ejecutar(
                connections,
                conn -> {
                    cancelar(conn, usuario, cita, validado);
                    return null;
                });
    }

    void cancelar(Connection conn, int usuario, int cita, String motivo) throws SQLException {
        String validado = pe.edu.utp.clinica.util.MotivoCancelacion.validar(motivo);
        int sede = sedePersonal(conn, usuario);
        Cita actual = bloquear(conn, sede, cita);
        if ("CANCELADA".equals(actual.estado())) return;
        if ((!CONFIRMADA.equals(actual.estado()) && !"SOLICITADA".equals(actual.estado()))
                || actual.llegada()
                || !actual.programada().isAfter(LocalDateTime.now(clock))) {
            throw new AgendaException(
                    409,
                    "CANCELACION_NO_DISPONIBLE",
                    "Solo se cancelan citas futuras pendientes de llegada.");
        }
        new ReservaCitaRepository(conn).estado(cita, null, "CANCELADA", null, null);
        new pe.edu.utp.clinica.repository.UsuarioRepository(conn)
                .auditar(
                        usuario,
                        null,
                        "CITA_CANCELADA_ADMISION",
                        "Cita #" + cita + "; sede #" + sede + "; motivo: " + validado,
                        false);
        Integer destinatario = new AdmisionRepository(conn).usuarioPaciente(actual.paciente());
        if (destinatario != null)
            new pe.edu.utp.clinica.repository.NotificacionRepository(conn)
                    .crear(
                            destinatario,
                            cita,
                            "Cita Cancelada",
                            "Admisión canceló tu cita. Consulta el estado actualizado en Mis"
                                + " citas.",
                            LocalDateTime.now(clock));
        if (correosActivos())
            pe.edu.utp.clinica.correo.CorreosCambiosJdbc.encolar(
                    conn,
                    cita,
                    null,
                    pe.edu.utp.clinica.correo.CorreosCambiosJdbc.Tipo.CANCELADA,
                    clock.instant());
        TransaccionJdbc.avisarSede(sede);
    }

    public Llegada registrar(int usuario, int cita) {
        if (usuario <= 0 || cita <= 0)
            throw new AgendaException(400, "DATOS_INVALIDOS", "Selecciona una cita válida.");
        return TransaccionJdbc.ejecutar(
                connections,
                conn -> {
                    int sede = sedePersonal(conn, usuario);
                    TransaccionJdbc.avisarSede(sede);
                    Cita actual = bloquear(conn, sede, cita);
                    LocalDateTime ahora = LocalDateTime.now(clock).withNano(0);
                    if (!CONFIRMADA.equals(actual.estado())
                            || !actual.programada().toLocalDate().equals(ahora.toLocalDate())) {
                        throw new AgendaException(
                                409,
                                "LLEGADA_NO_DISPONIBLE",
                                "Solo puedes registrar la llegada de una cita confirmada para"
                                    + " hoy.");
                    }
                    LocalDateTime programada = actual.programada();
                    int toleranciaActual = tolerancia(conn);
                    boolean tarde = ahora.isAfter(programada.plusMinutes(toleranciaActual));
                    guardar(conn, usuario, cita, ahora, tarde, toleranciaActual);
                    return new Llegada(cita, ahora.toString(), tarde, toleranciaActual);
                });
    }

    private static int sedePersonal(Connection conn, int usuario) throws SQLException {
        var sede = new pe.edu.utp.clinica.repository.AmbitoAdmisionRepository(conn).sede(usuario);
        if (sede == null)
            throw new AgendaException(
                    403, "ADMISION_NO_HABILITADA", "Tu perfil de admisión no está habilitado.");
        return sede.id();
    }

    private static void bloquearPaciente(Connection conn, int paciente) throws SQLException {
        if (new ReservaCitaRepository(conn).paciente(paciente) == null) throw inexistente();
    }

    private static Cita bloquear(Connection conn, int sede, int cita) throws SQLException {
        var repo = new AdmisionRepository(conn);
        var identidad = repo.identidad(cita, sede);
        if (identidad == null) throw inexistente();
        bloquearPaciente(conn, identidad.paciente());
        if (AgendaCitaDAO.medico(conn, identidad.medico()).sede() != sede) throw inexistente();
        var actual = repo.bloquear(cita, identidad.medico(), identidad.paciente());
        if (actual == null) throw inexistente();
        return actual;
    }

    public record Inasistencia(int idCita, String fechaRegistro, String motivo) {}

    public Inasistencia registrarInasistencia(int usuario, int cita, String motivo) {
        String razon = AtencionCitaDAO.motivo(motivo);
        if (usuario <= 0 || cita <= 0)
            throw new AgendaException(400, "DATOS_INVALIDOS", "Selecciona una cita válida.");
        return TransaccionJdbc.ejecutar(
                connections,
                conn -> {
                    int sede = sedePersonal(conn, usuario);
                    TransaccionJdbc.avisarSede(sede);
                    Cita actual = bloquear(conn, sede, cita);
                    LocalDateTime ahora = LocalDateTime.now(clock).withNano(0);
                    int toleranciaActual = tolerancia(conn);
                    if (!CONFIRMADA.equals(actual.estado())
                            || actual.llegada()
                            || !actual.programada().toLocalDate().equals(ahora.toLocalDate())
                            || !ahora.isAfter(actual.programada().plusMinutes(toleranciaActual))) {
                        throw new AgendaException(
                                409,
                                "INASISTENCIA_NO_DISPONIBLE",
                                "Verifica la ausencia después de la hora de la cita y su"
                                    + " tolerancia. Un paciente que llegó no puede registrarse como"
                                    + " ausente.");
                    }
                    new AdmisionRepository(conn)
                            .inasistencia(
                                    cita,
                                    usuario,
                                    actual.paciente(),
                                    ahora,
                                    razon,
                                    toleranciaActual);
                    if (new ReservaCitaRepository(conn)
                                    .estado(
                                            cita,
                                            null,
                                            "NO_ASISTIO",
                                            java.util.List.of("CONFIRMADA"),
                                            null)
                            != 1)
                        throw new AgendaException(
                                409, "INASISTENCIA_NO_DISPONIBLE", "La cita cambió de estado.");
                    return new Inasistencia(cita, ahora.toString(), razon);
                });
    }

    private void guardar(
            Connection conn,
            int usuario,
            int cita,
            LocalDateTime ahora,
            boolean tarde,
            int toleranciaActual)
            throws SQLException {
        new AdmisionRepository(conn).llegada(cita, usuario, ahora, toleranciaActual, tarde);
        if (new ReservaCitaRepository(conn)
                        .estado(cita, null, "EN_SALA", java.util.List.of("CONFIRMADA"), null)
                != 1)
            throw new AgendaException(
                    409, "LLEGADA_NO_DISPONIBLE", "La cita cambió de estado. Actualiza la página.");
    }

    private static AgendaException inexistente() {
        return new AgendaException(
                404, "CITA_NO_ENCONTRADA", "No se encontró una cita en tu sede.");
    }
}
