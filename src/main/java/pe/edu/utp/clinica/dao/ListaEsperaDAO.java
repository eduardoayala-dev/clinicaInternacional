package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
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
    private record Preferencias(int especialidad, Integer sede, String horario, String urgencia, String canales, int anticipacion, Integer medico) {}

    public ListaEsperaDAO() { this(DatabaseConfig::getConnection, Clock.system(ZoneId.of("America/Lima"))); }
    ListaEsperaDAO(AgendaCitaDAO.ConnectionSource connections, Clock clock) {
        this.connections = connections;
        this.clock = clock;
        ofertas = new OfertasDAO(connections, clock);
    }

    public boolean inscribirListaEspera(int paciente, int especialidad, Integer sede, String horario, String urgencia, String canales) {
        registrar(paciente, especialidad, sede, horario, urgencia, canales);
        return true;
    }

    public int registrar(int paciente, int especialidad, Integer sede, String horario, String urgencia, String canales) {
        return registrar(paciente, especialidad, sede, horario, urgencia, canales, 180);
    }

    public int registrar(int paciente, int especialidad, Integer sede, String horario, String urgencia, String canales, int anticipacion) {
        return registrar(paciente,especialidad,sede,horario,urgencia,canales,anticipacion,null);
    }

    public int registrar(int paciente,int especialidad,Integer sede,String horario,String urgencia,String canales,int anticipacion,Integer medico) {
        Preferencias prefs = preferencias(especialidad, sede, horario, urgencia, canales, anticipacion, medico);
        return TransaccionJdbc.ejecutar(connections, conn -> {
            AgendaCitaDAO.paciente(conn, paciente);
            validarCatalogos(conn, prefs);
            int existente = actualIgual(conn, paciente, prefs);
            if (existente > 0) return existente;
            cancelarInscripciones(conn, paciente);
            String sql = "INSERT INTO lista_espera(id_paciente,id_especialidad,id_sede,nivel_urgencia,rango_horario,canal_notificacion,fecha_registro,anticipacion_minutos,id_medico_preferido,estado,posicion) VALUES(?,?,?,?,?,?,?,?,?,'ACTIVA',0)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, paciente);
                ps.setInt(2, prefs.especialidad());
                if (prefs.sede() == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, prefs.sede());
                ps.setString(4, prefs.urgencia());
                ps.setString(5, prefs.horario());
                ps.setString(6, prefs.canales());
                ps.setObject(7, LocalDateTime.now(clock));
                ps.setInt(8, prefs.anticipacion());
                if(prefs.medico()==null) ps.setNull(9,Types.INTEGER); else ps.setInt(9,prefs.medico());
                ps.executeUpdate();
                int id = OfertasDAO.clave(ps);
                OfertasDAO.avisoLista(conn, id, "Inscripción confirmada. Consulta en este portal las ofertas compatibles con tus preferencias.");
                return id;
            }
        });
    }

    public boolean retirarListaEspera(int lista, int paciente) {
        if (lista <= 0 || paciente <= 0) throw invalida("La inscripción no es válida.");
        return TransaccionJdbc.ejecutar(connections, conn -> {
            AgendaCitaDAO.paciente(conn, paciente);
            try (PreparedStatement ps = conn.prepareStatement("SELECT estado FROM lista_espera WHERE id_lista_espera=? AND id_paciente=? FOR UPDATE")) {
                ps.setInt(1, lista);
                ps.setInt(2, paciente);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new AgendaException(404, "INSCRIPCION_NO_ENCONTRADA", "No se encontró una inscripción propia.");
                    if ("CANCELADA".equals(rs.getString(1))) return true;
                    if ("ATENDIDA".equals(rs.getString(1))) throw new AgendaException(409, "INSCRIPCION_ATENDIDA", "Esta inscripción ya fue atendida.");
                }
            }
            cancelarOfertas(conn, lista);
            OfertasDAO.cambiarLista(conn, lista, "CANCELADA");
            OfertasDAO.avisoLista(conn, lista, "Te retiraste de la lista de espera. No se canceló ninguna cita confirmada.");
            return true;
        });
    }

    public Map<String, Object> obtenerEstadoListaEspera(int paciente) {
        if (paciente <= 0) return null;
        String sql = "SELECT le.id_lista_espera,le.rango_horario,le.canal_notificacion,le.anticipacion_minutos,e.nombre AS especialidad,s.nombre AS sede, "
                + "CASE WHEN EXISTS(SELECT 1 FROM reasignacion_inteligente r WHERE r.id_lista_espera=le.id_lista_espera AND r.estado='PENDIENTE' AND r.fecha_limite_respuesta>?) THEN 'OFERTADA' ELSE 'ACTIVA' END AS estado_real, "
                + "(SELECT COUNT(*) FROM lista_espera otra WHERE otra.id_especialidad=le.id_especialidad AND (otra.id_sede=le.id_sede OR otra.id_sede IS NULL OR le.id_sede IS NULL) AND otra.estado IN ('ACTIVA','OFERTADA') AND otra.id_lista_espera<=le.id_lista_espera) AS posicion_real "
                + "FROM lista_espera le JOIN especialidad e ON le.id_especialidad=e.id_especialidad LEFT JOIN sede s ON le.id_sede=s.id_sede WHERE le.id_paciente=? AND le.estado IN ('ACTIVA','OFERTADA') ORDER BY le.id_lista_espera DESC LIMIT 1";
        try (Connection conn = connections.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, LocalDateTime.now(clock));
            ps.setInt(2, paciente);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? presentar(rs) : null; }
        } catch (SQLException e) { throw new CatalogoDatosException(e); }
    }

    public Map<String, Object> obtenerOfertaCupoAnticipado(int paciente) { return ofertas.obtenerParaPaciente(paciente); }
    public boolean aceptarOfertaCupo(int paciente, int oferta, int usuario) {
        ofertas.aceptar(paciente, oferta);
        return true;
    }
    public boolean rechazarOfertaCupo(int paciente, int oferta, int usuario) { return ofertas.rechazar(paciente, oferta); }

    private static Map<String, Object> presentar(ResultSet rs) throws SQLException {
        Map<String, Object> data = new LinkedHashMap<>();
        String estado = rs.getString("estado_real");
        data.put("idListaEspera", rs.getInt("id_lista_espera"));
        data.put("estadoCodigo", estado);
        data.put("estado", "OFERTADA".equals(estado) ? "Oferta pendiente de respuesta" : "En fila activa");
        data.put("especialidad", rs.getString("especialidad"));
        data.put("sede", rs.getString("sede") == null ? "Cualquier sede" : rs.getString("sede"));
        data.put("posicion", rs.getInt("posicion_real"));
        String horario = switch (rs.getString("rango_horario")) {
            case "MANANA" -> "Mañana";
            case "TARDE" -> "Tarde";
            case "NOCHE" -> "Noche";
            default -> "Cualquier horario";
        };
        data.put("preferencias", horario + " · aviso con " + rs.getInt("anticipacion_minutos") / 60 + " h · " + rs.getString("canal_notificacion"));
        return data;
    }

    private static Preferencias preferencias(int especialidad, Integer sede, String horario, String urgencia, String canales, int anticipacion, Integer medico) {
        if (medico!=null && medico<=0) throw invalida("El médico preferido no es válido.");
        if (!Set.of(180, 720).contains(anticipacion)) throw invalida("Selecciona al menos 3 o 12 horas de anticipación.");
        if (especialidad <= 0 || (sede != null && sede <= 0)) throw invalida("La especialidad y la sede deben ser válidas.");
        if (!Set.of("MANANA", "TARDE", "NOCHE", "INDIFERENTE").contains(horario == null ? "" : horario)) throw invalida("Selecciona un horario válido.");
        if (!Set.of("ALTA", "MEDIA", "BAJA").contains(urgencia == null ? "" : urgencia)) throw invalida("La prioridad no es válida.");
        if (canales == null || canales.isBlank()) throw invalida("Selecciona una preferencia de contacto.");
        Set<String> elegidos = new TreeSet<>(Arrays.asList(canales.split(",", -1)));
        if (!Set.of("WHATSAPP", "CORREO", "SMS").containsAll(elegidos)) throw invalida("La preferencia de contacto no es válida.");
        return new Preferencias(especialidad, sede, horario, urgencia, String.join(",", elegidos), anticipacion, medico);
    }

    private static void validarCatalogos(Connection conn, Preferencias prefs) throws SQLException {
        if(prefs.medico()!=null) {
            AgendaCitaDAO.Medico perfil=AgendaCitaDAO.medico(conn,prefs.medico());
            if(perfil.especialidad()!=prefs.especialidad() || (prefs.sede()!=null && perfil.sede()!=prefs.sede())) throw invalida("El médico preferido debe pertenecer a la especialidad y sede elegidas.");
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT id_especialidad FROM especialidad WHERE id_especialidad=? AND estado='ACTIVO'")) {
            ps.setInt(1, prefs.especialidad());
            try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw invalida("La especialidad no está habilitada."); }
        }
        if (prefs.sede() == null) return;
        try (PreparedStatement ps = conn.prepareStatement("SELECT id_sede FROM sede WHERE id_sede=? AND estado='ACTIVO'")) {
            ps.setInt(1, prefs.sede());
            try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw invalida("La sede no está habilitada."); }
        }
    }

    private static int actualIgual(Connection conn, int paciente, Preferencias prefs) throws SQLException {
        String sql = "SELECT id_lista_espera,id_especialidad,id_sede,rango_horario,nivel_urgencia,canal_notificacion,anticipacion_minutos,id_medico_preferido FROM lista_espera WHERE id_paciente=? AND estado IN ('ACTIVA','OFERTADA') FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, paciente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    boolean mismaSede = java.util.Objects.equals((Integer) rs.getObject(3), prefs.sede());
                    if (rs.getInt(2) == prefs.especialidad() && mismaSede && prefs.horario().equals(rs.getString(4))
                            && prefs.urgencia().equals(rs.getString(5)) && prefs.canales().equals(rs.getString(6)) && prefs.anticipacion() == rs.getInt(7) && java.util.Objects.equals((Integer)rs.getObject(8),prefs.medico())) return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private static void cancelarInscripciones(Connection conn, int paciente) throws SQLException {
        String ofertas = "UPDATE reasignacion_inteligente SET estado='CANCELADA' WHERE estado='PENDIENTE' AND id_lista_espera IN (SELECT id_lista_espera FROM lista_espera WHERE id_paciente=?)";
        try (PreparedStatement ps = conn.prepareStatement(ofertas)) { ps.setInt(1, paciente); ps.executeUpdate(); }
        try (PreparedStatement ps = conn.prepareStatement("UPDATE lista_espera SET estado='CANCELADA' WHERE id_paciente=? AND estado IN ('ACTIVA','OFERTADA')")) {
            ps.setInt(1, paciente);
            ps.executeUpdate();
        }
    }

    private static void cancelarOfertas(Connection conn, int lista) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE reasignacion_inteligente SET estado='CANCELADA' WHERE id_lista_espera=? AND estado='PENDIENTE'")) {
            ps.setInt(1, lista);
            ps.executeUpdate();
        }
    }
    private static AgendaException invalida(String message) { return new AgendaException(400, "DATOS_INVALIDOS", message); }
}
