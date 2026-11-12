package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.repository.OfertasRepository;
import pe.edu.utp.clinica.repository.OfertasRepository.Cupo;
import pe.edu.utp.clinica.repository.OfertasRepository.Oferta;
import pe.edu.utp.clinica.util.NombrePersona;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OfertasDAO {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final String PENDIENTE = "PENDIENTE";
    private static final String ACEPTADA = "ACEPTADA";
    private static final String RECHAZADA = "RECHAZADA";
    private static final String EXPIRADA = "EXPIRADA";
    private static final String CANCELADA = "CANCELADA";
    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale.forLanguageTag("es"));
    private final AgendaCitaDAO.ConnectionSource connections;
    private final Clock clock;
    private final AgendaCitaDAO agenda;
    private final boolean correosActivos;

    private record Resultado(int cita, AgendaException error) {}

    public OfertasDAO() {
        this(DatabaseConfig::getConnection, Clock.system(LIMA));
    }

    OfertasDAO(AgendaCitaDAO.ConnectionSource connections, Clock clock) {
        this(
                connections,
                clock,
                Boolean.parseBoolean(
                        System.getenv().getOrDefault("APPOINTMENT_EMAIL_ENABLED", "false")));
    }

    OfertasDAO(AgendaCitaDAO.ConnectionSource connections, Clock clock, boolean correosActivos) {
        this.connections = connections;
        this.clock = clock;
        this.correosActivos = correosActivos;
        agenda = new AgendaCitaDAO(connections, clock, correosActivos);
    }

    public int generar(int citaOriginal) {
        if (citaOriginal <= 0) throw invalida();
        return TransaccionJdbc.ejecutar(connections, conn -> generar(conn, citaOriginal));
    }

    private int generar(Connection conn, int original) throws SQLException {
        Cupo cupo = cupo(conn, original);
        if (cupo == null) return 0;
        AgendaCitaDAO.bloquearMedico(conn, cupo.medico());
        normalizar(conn, cupo.medico());
        if (!cupo.inicio().isAfter(ahora())) return 0;
        try {
            agenda.validarCupo(conn, cupo.medico(), cupo.inicio());
        } catch (AgendaException e) {
            if (e.status() == 400 || e.status() == 409) return 0;
            throw e;
        }
        var configuracion = ConfiguracionOfertasDAO.leer(conn);
        if (cupo.inicio().isBefore(ahora().plusMinutes(configuracion.anticipacionMinima())))
            return 0;
        int lista = candidato(conn, cupo);
        if (lista == 0) return 0;
        LocalDateTime vence = ahora().plusMinutes(configuracion.minutosRespuesta());
        if (vence.isAfter(cupo.inicio())) vence = cupo.inicio();
        int id =
                new OfertasRepository(conn)
                        .crear(original, lista, cupo.disponibilidad(), ahora(), vence);
        cambiarLista(conn, lista, "OFERTADA");
        avisoLista(
                conn,
                lista,
                "Tienes una oferta de cupo para el "
                        + cupo.inicio().format(FECHA)
                        + ". Revísala en tus notificaciones antes del vencimiento.");
        if (correosActivos)
            pe.edu.utp.clinica.correo.CorreosOfertaJdbc.encolar(conn, id, clock.instant());
        return id;
    }

    public int aceptar(int paciente, int oferta) {
        if (paciente <= 0 || oferta <= 0) throw invalida();
        Resultado result =
                TransaccionJdbc.ejecutar(connections, conn -> aceptar(conn, paciente, oferta));
        if (result.error() != null) throw result.error();
        return result.cita();
    }

    private Resultado aceptar(Connection conn, int paciente, int id) throws SQLException {
        AgendaCitaDAO.paciente(conn, paciente);
        Oferta oferta = obtener(conn, paciente, id, false);
        if (ACEPTADA.equals(oferta.estado()) && oferta.citaAsignada() != null)
            return new Resultado(oferta.citaAsignada(), null);
        AgendaCitaDAO.medico(conn, oferta.cupo().medico());
        normalizar(conn, oferta.cupo().medico());
        oferta = obtener(conn, paciente, id, true);
        if (EXPIRADA.equals(oferta.estado())) return new Resultado(0, vencida());
        if (!PENDIENTE.equals(oferta.estado())) throw noDisponible();
        if (!oferta.vence().isAfter(ahora())) return new Resultado(0, vencida());
        try {
            Cupo c = oferta.cupo();
            int cita =
                    agenda.reservarEnConexion(
                            conn,
                            paciente,
                            c.medico(),
                            c.inicio().toLocalDate().toString(),
                            c.inicio().toLocalTime().toString(),
                            "MEDIA",
                            id);
            actualizarOferta(conn, id, ACEPTADA, cita);
            cambiarLista(conn, oferta.lista(), "ATENDIDA");
            return new Resultado(cita, null);
        } catch (AgendaException e) {
            if (e.status() != 400 && e.status() != 409) throw e;
            actualizarOferta(conn, id, CANCELADA, null);
            cambiarLista(conn, oferta.lista(), "ACTIVA");
            return new Resultado(0, noDisponible());
        }
    }

    public boolean rechazar(int paciente, int id) {
        if (paciente <= 0 || id <= 0) throw invalida();
        return TransaccionJdbc.ejecutar(
                connections,
                conn -> {
                    AgendaCitaDAO.paciente(conn, paciente);
                    Oferta oferta = obtener(conn, paciente, id, false);
                    if (RECHAZADA.equals(oferta.estado())) return true;
                    AgendaCitaDAO.medico(conn, oferta.cupo().medico());
                    normalizar(conn, oferta.cupo().medico());
                    oferta = obtener(conn, paciente, id, true);
                    if (!PENDIENTE.equals(oferta.estado())) throw noDisponible();
                    actualizarOferta(conn, id, RECHAZADA, null);
                    cambiarLista(conn, oferta.lista(), "ACTIVA");
                    avisoLista(
                            conn,
                            oferta.lista(),
                            "Oferta rechazada. Conservas tu inscripción y antigüedad en la lista de"
                                + " espera.");
                    generar(conn, oferta.cupo().cita());
                    return true;
                });
    }

    public int procesarPendientes() {
        depurarVencidas();
        List<Integer> citas;
        try (var conn = connections.open()) {
            citas = new OfertasRepository(conn).canceladasFuturas(ahora());
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
        int nuevas = 0;
        for (int cita : citas) if (generar(cita) > 0) nuevas++;
        return nuevas;
    }

    public Map<String, Object> obtenerParaPaciente(int paciente) {
        if (paciente <= 0) return Map.of();
        try (var conn = connections.open()) {
            var oferta = new OfertasRepository(conn).vigente(paciente, ahora());
            return oferta == null ? Map.of() : presentar(oferta);
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }

    public boolean tieneOfertaVencida(int paciente) {
        try (var conn = connections.open()) {
            var ultima = new OfertasRepository(conn).ultima(paciente);
            return ultima != null
                    && (EXPIRADA.equals(ultima.estado())
                            || (PENDIENTE.equals(ultima.estado())
                                    && !ultima.vence().isAfter(ahora())));
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }

    private void depurarVencidas() {
        List<Integer> medicos;
        try (var conn = connections.open()) {
            medicos = new OfertasRepository(conn).medicosPendientes();
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
        for (int medico : medicos) {
            TransaccionJdbc.ejecutar(
                    connections,
                    conn -> {
                        AgendaCitaDAO.bloquearMedico(conn, medico);
                        normalizar(conn, medico);
                        return true;
                    });
        }
    }

    private Map<String, Object> presentar(OfertasRepository.Presentacion rs) {
        Map<String, Object> data = new LinkedHashMap<>();
        LocalDateTime vence = rs.vence();
        long segundos =
                Math.max(0, Duration.between(ahora().atZone(LIMA), vence.atZone(LIMA)).toSeconds());
        data.put("idOferta", rs.id());
        data.put("idListaEspera", rs.lista());
        data.put("especialidad", rs.especialidad());
        data.put("medico", NombrePersona.formatear(rs.nombres() + " " + rs.apellidos()));
        data.put("sede", rs.sede());
        data.put("fechaOfrecida", rs.inicio().format(FECHA));
        data.put("tiempoRestanteSegundos", segundos);
        data.put("tiempoRestante", String.format("%02d:%02d min", segundos / 60, segundos % 60));
        data.put("origen", "Cupo liberado por una cancelación.");
        return data;
    }

    private Cupo cupo(Connection conn, int cita) throws SQLException {
        return new OfertasRepository(conn).cupo(cita);
    }

    private Oferta obtener(Connection conn, int paciente, int id, boolean bloquear)
            throws SQLException {
        var oferta = new OfertasRepository(conn).propia(paciente, id, bloquear);
        if (oferta == null)
            throw new AgendaException(
                    404, "OFERTA_NO_ENCONTRADA", "No se encontró una oferta propia.");
        return oferta;
    }

    private int candidato(Connection conn, Cupo cupo) throws SQLException {
        return new OfertasRepository(conn).candidato(cupo, ahora());
    }

    private void normalizar(Connection conn, int medico) throws SQLException {
        new OfertasRepository(conn).normalizar(medico, ahora());
    }

    static void cambiarLista(Connection conn, int lista, String estado) throws SQLException {
        if (new OfertasRepository(conn).cambiarLista(lista, estado) != 1) throw noDisponible();
    }

    private static void actualizarOferta(Connection conn, int id, String estado, Integer cita)
            throws SQLException {
        if (new OfertasRepository(conn).actualizar(id, estado, cita) != 1) throw noDisponible();
    }

    static void avisoLista(Connection conn, int lista, String mensaje) throws SQLException {
        Integer usuario = new OfertasRepository(conn).usuarioLista(lista);
        if (usuario == null) throw new SQLException("No se pudo crear la notificación");
        new pe.edu.utp.clinica.repository.NotificacionRepository(conn)
                .crear(usuario, null, "Lista de espera", mensaje, LocalDateTime.now(LIMA));
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }

    private static AgendaException invalida() {
        return new AgendaException(
                400, "DATOS_INVALIDOS", "El identificador de la oferta no es válido.");
    }

    private static AgendaException noDisponible() {
        return new AgendaException(409, "OFERTA_NO_DISPONIBLE", "La oferta ya no está disponible.");
    }

    private static AgendaException vencida() {
        return new AgendaException(
                410,
                "OFERTA_VENCIDA",
                "La oferta venció. Conservas tu inscripción en la lista de espera.");
    }
}
