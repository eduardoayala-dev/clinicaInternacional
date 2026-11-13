package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.repository.ListaEsperaRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ListaEsperaDAO {
    private final AgendaCitaDAO.ConnectionSource connections;
    private final Clock clock;
    private final OfertasDAO ofertas;

    public record Preferencias(
            int especialidad,
            Integer sede,
            String horario,
            String urgencia,
            String canales,
            int anticipacion,
            Integer medico) {}

    public ListaEsperaDAO() {
        this(DatabaseConfig::getConnection, Clock.system(ZoneId.of("America/Lima")));
    }

    ListaEsperaDAO(AgendaCitaDAO.ConnectionSource connections, Clock clock) {
        this.connections = connections;
        this.clock = clock;
        ofertas = new OfertasDAO(connections, clock);
    }

    public boolean inscribirListaEspera(
            int paciente,
            int especialidad,
            Integer sede,
            String horario,
            String urgencia,
            String canales) {
        registrar(paciente, especialidad, sede, horario, urgencia, canales);
        return true;
    }

    public int registrar(
            int paciente,
            int especialidad,
            Integer sede,
            String horario,
            String urgencia,
            String canales) {
        return registrar(paciente, especialidad, sede, horario, urgencia, canales, 180);
    }

    public int registrar(
            int paciente,
            int especialidad,
            Integer sede,
            String horario,
            String urgencia,
            String canales,
            int anticipacion) {
        return registrar(
                paciente,
                new Preferencias(
                        especialidad, sede, horario, urgencia, canales, anticipacion, null));
    }

    public int registrar(int paciente, Preferencias solicitud) {
        if (solicitud == null) throw invalida("Indica las preferencias de la inscripción.");
        Preferencias recibidas =
                preferencias(
                        solicitud.especialidad(),
                        solicitud.sede(),
                        solicitud.horario(),
                        solicitud.urgencia(),
                        solicitud.canales(),
                        solicitud.anticipacion(),
                        solicitud.medico());
        return TransaccionJdbc.ejecutar(
                connections,
                conn -> {
                    AgendaCitaDAO.paciente(conn, paciente);
                    var prefs =
                            new Preferencias(
                                    recibidas.especialidad(),
                                    recibidas.sede(),
                                    recibidas.horario(),
                                    PrioridadClinicaJdbc.resolver(
                                            conn,
                                            paciente,
                                            recibidas.especialidad(),
                                            java.time.LocalDate.now(clock)),
                                    recibidas.canales(),
                                    recibidas.anticipacion(),
                                    recibidas.medico());
                    validarCatalogos(conn, prefs);
                    int existente = actualIgual(conn, paciente, prefs);
                    if (existente > 0) return existente;
                    cancelarInscripciones(conn, paciente);
                    int id =
                            new ListaEsperaRepository(conn)
                                    .crear(paciente, prefs, LocalDateTime.now(clock));
                    OfertasDAO.avisoLista(
                            conn,
                            id,
                            "Inscripción confirmada. Consulta en este portal las ofertas"
                                + " compatibles con tus preferencias.");
                    return id;
                });
    }

    public boolean retirarListaEspera(int lista, int paciente) {
        if (lista <= 0 || paciente <= 0) throw invalida("La inscripción no es válida.");
        return TransaccionJdbc.ejecutar(
                connections,
                conn -> {
                    AgendaCitaDAO.paciente(conn, paciente);
                    String estado = new ListaEsperaRepository(conn).bloquearPropia(lista, paciente);
                    if (estado == null)
                        throw new AgendaException(
                                404,
                                "INSCRIPCION_NO_ENCONTRADA",
                                "No se encontró una inscripción propia.");
                    if ("CANCELADA".equals(estado)) return true;
                    if ("ATENDIDA".equals(estado))
                        throw new AgendaException(
                                409, "INSCRIPCION_ATENDIDA", "Esta inscripción ya fue atendida.");
                    cancelarOfertas(conn, lista);
                    OfertasDAO.cambiarLista(conn, lista, "CANCELADA");
                    OfertasDAO.avisoLista(
                            conn,
                            lista,
                            "Te retiraste de la lista de espera. No se canceló ninguna cita"
                                + " confirmada.");
                    return true;
                });
    }

    public Map<String, Object> obtenerEstadoListaEspera(int paciente) {
        if (paciente <= 0) return Map.of();
        try (var conn = connections.open()) {
            var estado = new ListaEsperaRepository(conn).estado(paciente, LocalDateTime.now(clock));
            if (estado == null) return Map.of();
            String prioridad =
                    new pe.edu.utp.clinica.repository.ReservaCitaRepository(conn)
                            .prioridadEvaluada(
                                    paciente,
                                    estado.especialidadId(),
                                    java.time.LocalDate.now(clock));
            return presentar(estado, prioridad);
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }

    public Map<String, Object> obtenerOfertaCupoAnticipado(int paciente) {
        return ofertas.obtenerParaPaciente(paciente);
    }

    public boolean aceptarOfertaCupo(int paciente, int oferta) {
        ofertas.aceptar(paciente, oferta);
        return true;
    }

    public boolean rechazarOfertaCupo(int paciente, int oferta) {
        return ofertas.rechazar(paciente, oferta);
    }

    private static Map<String, Object> presentar(
            ListaEsperaRepository.Estado rs, String prioridad) {
        Map<String, Object> data = new LinkedHashMap<>();
        String estado = rs.estado();
        data.put("idListaEspera", rs.id());
        data.put("estadoCodigo", estado);
        data.put(
                "estado",
                "OFERTADA".equals(estado) ? "Oferta pendiente de respuesta" : "En fila activa");
        data.put("especialidad", rs.especialidad());
        data.put("sede", rs.sede() == null ? "Cualquier sede" : rs.sede());
        data.put("posicion", (int) rs.posicion());
        data.put(
                "prioridad",
                prioridad == null ? "Sin prioridad de seguimiento vigente" : prioridad);
        String horario =
                switch (rs.horario()) {
                    case "MANANA" -> "Mañana";
                    case "TARDE" -> "Tarde";
                    case "NOCHE" -> "Noche";
                    default -> "Cualquier horario";
                };
        data.put(
                "preferencias",
                horario + " · aviso con " + rs.anticipacion() / 60 + " h · " + rs.canales());
        return data;
    }

    private static Preferencias preferencias(
            int especialidad,
            Integer sede,
            String horario,
            String urgencia,
            String canales,
            int anticipacion,
            Integer medico) {
        if (medico != null && medico <= 0) throw invalida("El médico preferido no es válido.");
        if (!Set.of(180, 720).contains(anticipacion))
            throw invalida("Selecciona al menos 3 o 12 horas de anticipación.");
        if (especialidad <= 0 || (sede != null && sede <= 0))
            throw invalida("La especialidad y la sede deben ser válidas.");
        if (!Set.of("MANANA", "TARDE", "NOCHE", "INDIFERENTE")
                .contains(horario == null ? "" : horario))
            throw invalida("Selecciona un horario válido.");
        if (!Set.of("ALTA", "MEDIA", "BAJA").contains(urgencia == null ? "" : urgencia))
            throw invalida("La prioridad no es válida.");
        if (canales == null || canales.isBlank())
            throw invalida("Selecciona una preferencia de contacto.");
        Set<String> elegidos = new TreeSet<>(Arrays.asList(canales.split(",", -1)));
        if (!Set.of("WHATSAPP", "CORREO", "SMS").containsAll(elegidos))
            throw invalida("La preferencia de contacto no es válida.");
        return new Preferencias(
                especialidad,
                sede,
                horario,
                urgencia,
                String.join(",", elegidos),
                anticipacion,
                medico);
    }

    private static void validarCatalogos(Connection conn, Preferencias prefs) throws SQLException {
        if (prefs.medico() != null) {
            AgendaCitaDAO.Medico perfil = AgendaCitaDAO.medico(conn, prefs.medico());
            if (perfil.especialidad() != prefs.especialidad()
                    || (prefs.sede() != null && perfil.sede() != prefs.sede()))
                throw invalida(
                        "El médico preferido debe pertenecer a la especialidad y sede elegidas.");
        }
        var repo = new ListaEsperaRepository(conn);
        if (!repo.catalogoActivo(prefs.especialidad(), false))
            throw invalida("La especialidad no está habilitada.");
        if (prefs.sede() != null && !repo.catalogoActivo(prefs.sede(), true))
            throw invalida("La sede no está habilitada.");
    }

    private static int actualIgual(Connection conn, int paciente, Preferencias prefs)
            throws SQLException {
        return new ListaEsperaRepository(conn).actualIgual(paciente, prefs);
    }

    private static void cancelarInscripciones(Connection conn, int paciente) throws SQLException {
        new ListaEsperaRepository(conn).cancelarInscripciones(paciente);
    }

    private static void cancelarOfertas(Connection conn, int lista) throws SQLException {
        new ListaEsperaRepository(conn).cancelarOfertas(lista);
    }

    private static AgendaException invalida(String mensaje) {
        return new AgendaException(400, "DATOS_INVALIDOS", mensaje);
    }
}
