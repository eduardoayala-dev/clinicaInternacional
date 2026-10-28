package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.util.NombrePersona;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale.forLanguageTag("es"));
    private static final String DATOS_OFERTA = "SELECT r.id_reasignacion,r.id_lista_espera,r.estado,r.fecha_limite_respuesta,r.id_cita_asignada,c.id_medico,c.fecha_hora,m.id_especialidad,m.id_sede,c.id_disponibilidad,c.id_cita "
            + "FROM reasignacion_inteligente r JOIN lista_espera le ON r.id_lista_espera=le.id_lista_espera JOIN cita c ON r.id_cita_original=c.id_cita JOIN medico m ON c.id_medico=m.id_medico ";
    private final AgendaCitaDAO.ConnectionSource connections;
    private final Clock clock;
    private final AgendaCitaDAO agenda;
    private record Cupo(int cita, int medico, int disponibilidad, int especialidad, int sede, int paciente, LocalDateTime inicio) {}
    private record Oferta(int id, int lista, String estado, LocalDateTime vence, Integer citaAsignada, Cupo cupo) {}
    private record Resultado(int cita, AgendaException error) {}

    public OfertasDAO() { this(DatabaseConfig::getConnection, Clock.system(LIMA)); }
    OfertasDAO(AgendaCitaDAO.ConnectionSource connections, Clock clock) {
        this.connections = connections;
        this.clock = clock;
        agenda = new AgendaCitaDAO(connections, clock);
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
        int lista = candidato(conn, cupo);
        if (lista == 0) return 0;
        LocalDateTime vence = ahora().plusMinutes(15);
        if (vence.isAfter(cupo.inicio())) vence = cupo.inicio();
        String insert = "INSERT INTO reasignacion_inteligente(id_cita_original,id_lista_espera,id_disponibilidad,fecha_oferta,fecha_limite_respuesta,estado) VALUES(?,?,?,?,?,'PENDIENTE')";
        int id;
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, original);
            ps.setInt(2, lista);
            ps.setInt(3, cupo.disponibilidad());
            ps.setObject(4, ahora());
            ps.setObject(5, vence);
            ps.executeUpdate();
            id = clave(ps);
        }
        cambiarLista(conn, lista, "OFERTADA");
        avisoLista(conn, lista, "Tienes una oferta de cupo para el " + cupo.inicio().format(FECHA) + ". Revísala en tus notificaciones antes del vencimiento.");
        return id;
    }

    public int aceptar(int paciente, int oferta) {
        if (paciente <= 0 || oferta <= 0) throw invalida();
        Resultado result = TransaccionJdbc.ejecutar(connections, conn -> aceptar(conn, paciente, oferta));
        if (result.error() != null) throw result.error();
        return result.cita();
    }

    private Resultado aceptar(Connection conn, int paciente, int id) throws SQLException {
        AgendaCitaDAO.paciente(conn, paciente);
        Oferta oferta = obtener(conn, paciente, id, false);
        if (ACEPTADA.equals(oferta.estado()) && oferta.citaAsignada() != null) return new Resultado(oferta.citaAsignada(), null);
        AgendaCitaDAO.medico(conn, oferta.cupo().medico());
        normalizar(conn, oferta.cupo().medico());
        oferta = obtener(conn, paciente, id, true);
        if (EXPIRADA.equals(oferta.estado())) return new Resultado(0, vencida());
        if (!PENDIENTE.equals(oferta.estado())) throw noDisponible();
        if (!oferta.vence().isAfter(ahora())) return new Resultado(0, vencida());
        try {
            Cupo c = oferta.cupo();
            int cita = agenda.reservarEnConexion(conn, paciente, c.medico(), c.inicio().toLocalDate().toString(),
                    c.inicio().toLocalTime().toString(), "MEDIA", id);
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
        return TransaccionJdbc.ejecutar(connections, conn -> {
            AgendaCitaDAO.paciente(conn, paciente);
            Oferta oferta = obtener(conn, paciente, id, false);
            if (RECHAZADA.equals(oferta.estado())) return true;
            AgendaCitaDAO.medico(conn, oferta.cupo().medico());
            normalizar(conn, oferta.cupo().medico());
            oferta = obtener(conn, paciente, id, true);
            if (!PENDIENTE.equals(oferta.estado())) throw noDisponible();
            actualizarOferta(conn, id, RECHAZADA, null);
            cambiarLista(conn, oferta.lista(), "ACTIVA");
            avisoLista(conn, oferta.lista(), "Oferta rechazada. Conservas tu inscripción y antigüedad en la lista de espera.");
            generar(conn, oferta.cupo().cita());
            return true;
        });
    }

    public int procesarPendientes() {
        depurarVencidas();
        List<Integer> citas = new ArrayList<>();
        String sql = "SELECT c.id_cita FROM cita c JOIN disponibilidad d ON c.id_disponibilidad=d.id_disponibilidad WHERE c.estado='CANCELADA' AND c.fecha_hora>? AND d.estado='PUBLICADO' ORDER BY c.fecha_hora,c.id_cita";
        try (Connection conn = connections.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, ahora());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) citas.add(rs.getInt(1)); }
        } catch (SQLException e) { throw new CatalogoDatosException(e); }
        int nuevas = 0;
        for (int cita : citas) if (generar(cita) > 0) nuevas++;
        return nuevas;
    }

    public Map<String, Object> obtenerParaPaciente(int paciente) {
        if (paciente <= 0) return Map.of();
        String sql = "SELECT r.id_reasignacion,r.id_lista_espera,r.fecha_limite_respuesta,c.fecha_hora,u.nombres,u.apellidos,e.nombre AS especialidad,s.nombre AS sede "
                + "FROM reasignacion_inteligente r JOIN lista_espera le ON r.id_lista_espera=le.id_lista_espera JOIN cita c ON r.id_cita_original=c.id_cita "
                + "JOIN disponibilidad d ON r.id_disponibilidad=d.id_disponibilidad JOIN medico m ON c.id_medico=m.id_medico JOIN usuario u ON m.id_usuario=u.id_usuario JOIN especialidad e ON m.id_especialidad=e.id_especialidad JOIN sede s ON m.id_sede=s.id_sede "
                + "WHERE le.id_paciente=? AND le.estado='OFERTADA' AND r.estado='PENDIENTE' AND r.fecha_limite_respuesta>? AND c.fecha_hora>? AND c.estado='CANCELADA' AND d.estado='PUBLICADO' AND d.id_sede=m.id_sede AND m.estado='ACTIVO' AND u.activo=TRUE AND e.estado='ACTIVO' AND s.estado='ACTIVO' ORDER BY r.id_reasignacion DESC LIMIT 1";
        try (Connection conn = connections.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, paciente);
            ps.setObject(2, ahora());
            ps.setObject(3, ahora());
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? presentar(rs) : Map.of(); }
        } catch (SQLException e) { throw new CatalogoDatosException(e); }
    }

    public boolean tieneOfertaVencida(int paciente) {
        String sql = "SELECT r.estado,r.fecha_limite_respuesta FROM reasignacion_inteligente r JOIN lista_espera le ON r.id_lista_espera=le.id_lista_espera WHERE le.id_paciente=? ORDER BY r.id_reasignacion DESC LIMIT 1";
        try (Connection conn = connections.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, paciente);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                return EXPIRADA.equals(rs.getString(1)) || (PENDIENTE.equals(rs.getString(1)) && !rs.getObject(2, LocalDateTime.class).isAfter(ahora()));
            }
        } catch (SQLException e) { throw new CatalogoDatosException(e); }
    }

    private void depurarVencidas() {
        List<Integer> medicos = new ArrayList<>();
        String sql = "SELECT DISTINCT c.id_medico FROM reasignacion_inteligente r JOIN cita c ON r.id_cita_original=c.id_cita WHERE r.estado='PENDIENTE' ORDER BY c.id_medico";
        try (Connection conn = connections.open(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) medicos.add(rs.getInt(1));
        } catch (SQLException e) { throw new CatalogoDatosException(e); }
        for (int medico : medicos) {
            TransaccionJdbc.ejecutar(connections, conn -> {
                AgendaCitaDAO.bloquearMedico(conn, medico);
                normalizar(conn, medico);
                return true;
            });
        }
    }

    private Map<String, Object> presentar(ResultSet rs) throws SQLException {
        Map<String, Object> data = new LinkedHashMap<>();
        LocalDateTime vence = rs.getObject("fecha_limite_respuesta", LocalDateTime.class);
        long segundos = Math.max(0, Duration.between(ahora().atZone(LIMA), vence.atZone(LIMA)).toSeconds());
        data.put("idOferta", rs.getInt("id_reasignacion"));
        data.put("idListaEspera", rs.getInt("id_lista_espera"));
        data.put("especialidad", rs.getString("especialidad"));
        data.put("medico", NombrePersona.formatear(rs.getString("nombres") + " " + rs.getString("apellidos")));
        data.put("sede", rs.getString("sede"));
        data.put("fechaOfrecida", rs.getObject("fecha_hora", LocalDateTime.class).format(FECHA));
        data.put("tiempoRestanteSegundos", segundos);
        data.put("tiempoRestante", String.format("%02d:%02d min", segundos / 60, segundos % 60));
        data.put("origen", "Cupo liberado por una cancelación.");
        return data;
    }

    private Cupo cupo(Connection conn, int cita) throws SQLException {
        String sql = "SELECT c.id_cita,c.id_medico,c.id_disponibilidad,m.id_especialidad,m.id_sede,c.id_paciente,c.fecha_hora FROM cita c JOIN medico m ON c.id_medico=m.id_medico JOIN disponibilidad d ON c.id_disponibilidad=d.id_disponibilidad WHERE c.id_cita=? AND c.estado='CANCELADA' AND d.estado='PUBLICADO' AND d.id_sede=m.id_sede";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cita);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Cupo(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.getObject(7, LocalDateTime.class));
            }
        }
    }

    private Oferta obtener(Connection conn, int paciente, int id, boolean bloquear) throws SQLException {
        String sql = bloquear ? DATOS_OFERTA + "WHERE le.id_paciente=? AND r.id_reasignacion=? FOR UPDATE"
                : DATOS_OFERTA + "WHERE le.id_paciente=? AND r.id_reasignacion=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, paciente);
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new AgendaException(404, "OFERTA_NO_ENCONTRADA", "No se encontró una oferta propia.");
                Cupo cupo = new Cupo(rs.getInt(11), rs.getInt(6), rs.getInt(10), rs.getInt(8), rs.getInt(9), paciente, rs.getObject(7, LocalDateTime.class));
                return new Oferta(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getObject(4, LocalDateTime.class), (Integer) rs.getObject(5), cupo);
            }
        }
    }

    private int candidato(Connection conn, Cupo cupo) throws SQLException {
        String rango = "NOCHE";
        if(cupo.inicio().getHour()<13) rango="MANANA";
        else if(cupo.inicio().getHour()<19) rango="TARDE";
        String sql = "SELECT le.id_lista_espera FROM lista_espera le "
                + "WHERE le.estado='ACTIVA' AND le.id_paciente IN (SELECT p.id_paciente FROM paciente p JOIN usuario u ON p.id_usuario=u.id_usuario WHERE u.activo=TRUE AND u.rol='PACIENTE') AND le.id_especialidad=? AND (le.id_sede=? OR le.id_sede IS NULL) AND le.rango_horario IN (?, 'INDIFERENTE') AND le.id_paciente<>? "
                + "AND NOT EXISTS (SELECT 1 FROM reasignacion_inteligente r WHERE r.id_cita_original=? AND r.id_lista_espera=le.id_lista_espera AND r.estado IN ('RECHAZADA','EXPIRADA','ACEPTADA')) "
                + "AND NOT EXISTS (SELECT 1 FROM cita c WHERE c.id_paciente=le.id_paciente AND c.fecha_hora=? AND c.estado NOT IN ('CANCELADA','REPROGRAMADA')) "
                + "AND le.anticipacion_minutos<=? AND (le.id_medico_preferido IS NULL OR le.id_medico_preferido=?) ORDER BY CASE le.nivel_urgencia WHEN 'ALTA' THEN 1 WHEN 'MEDIA' THEN 2 ELSE 3 END,le.fecha_registro,le.id_lista_espera LIMIT 1 FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cupo.especialidad());
            ps.setInt(2, cupo.sede());
            ps.setString(3, rango);
            ps.setInt(4, cupo.paciente());
            ps.setInt(5, cupo.cita());
            ps.setObject(6, cupo.inicio());
            ps.setLong(7, Duration.between(ahora().atZone(LIMA), cupo.inicio().atZone(LIMA)).toMinutes());
            ps.setInt(8,cupo.medico());
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    private void normalizar(Connection conn, int medico) throws SQLException {
        String expirar = "UPDATE reasignacion_inteligente SET estado='EXPIRADA' WHERE estado='PENDIENTE' AND id_cita_original IN (SELECT id_cita FROM cita WHERE id_medico=?) AND (fecha_limite_respuesta<=? OR id_cita_original IN (SELECT id_cita FROM cita WHERE fecha_hora<=?))";
        try (PreparedStatement ps = conn.prepareStatement(expirar)) {
            ps.setInt(1, medico);
            ps.setObject(2, ahora());
            ps.setObject(3, ahora());
            ps.executeUpdate();
        }
        String cancelar = "UPDATE reasignacion_inteligente SET estado='CANCELADA' WHERE estado='PENDIENTE' AND id_cita_original IN (SELECT id_cita FROM cita WHERE id_medico=?) AND (id_cita_original IN (SELECT id_cita FROM cita WHERE estado<>'CANCELADA') "
                + "OR id_disponibilidad IN (SELECT id_disponibilidad FROM disponibilidad WHERE estado<>'PUBLICADO') "
                + "OR id_lista_espera IN (SELECT le.id_lista_espera FROM lista_espera le JOIN paciente p ON le.id_paciente=p.id_paciente JOIN usuario u ON p.id_usuario=u.id_usuario WHERE le.estado<>'OFERTADA' OR u.activo=FALSE) "
                + "OR id_cita_original IN (SELECT c.id_cita FROM cita c JOIN medico m ON c.id_medico=m.id_medico JOIN usuario u ON m.id_usuario=u.id_usuario JOIN sede s ON m.id_sede=s.id_sede JOIN especialidad e ON m.id_especialidad=e.id_especialidad JOIN disponibilidad d ON c.id_disponibilidad=d.id_disponibilidad WHERE m.estado<>'ACTIVO' OR u.activo=FALSE OR s.estado<>'ACTIVO' OR e.estado<>'ACTIVO' OR d.id_sede<>m.id_sede))";
        try (PreparedStatement ps = conn.prepareStatement(cancelar)) {
            ps.setInt(1, medico);
            ps.executeUpdate();
        }
        String reactivar = "UPDATE lista_espera SET estado='ACTIVA' WHERE estado='OFERTADA' AND id_lista_espera IN (SELECT r.id_lista_espera FROM reasignacion_inteligente r JOIN cita c ON r.id_cita_original=c.id_cita WHERE c.id_medico=?) AND NOT EXISTS (SELECT 1 FROM reasignacion_inteligente r WHERE r.id_lista_espera=lista_espera.id_lista_espera AND r.estado='PENDIENTE' AND r.fecha_limite_respuesta>?)";
        try (PreparedStatement ps = conn.prepareStatement(reactivar)) {
            ps.setInt(1, medico);
            ps.setObject(2, ahora());
            ps.executeUpdate();
        }
    }

    static void cambiarLista(Connection conn, int lista, String estado) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE lista_espera SET estado=? WHERE id_lista_espera=?")) {
            ps.setString(1, estado);
            ps.setInt(2, lista);
            if (ps.executeUpdate() != 1) throw noDisponible();
        }
    }

    private static void actualizarOferta(Connection conn, int id, String estado, Integer cita) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE reasignacion_inteligente SET estado=?,id_cita_asignada=? WHERE id_reasignacion=? AND estado='PENDIENTE'")) {
            ps.setString(1, estado);
            if (cita == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, cita);
            ps.setInt(3, id);
            if (ps.executeUpdate() != 1) throw noDisponible();
        }
    }

    static void avisoLista(Connection conn, int lista, String mensaje) throws SQLException {
        String sql = "INSERT INTO notificacion(id_usuario,tipo,canal,mensaje,estado_envio,leida,fecha_envio) SELECT p.id_usuario,'Lista de espera','PLATAFORMA',?,'ENVIADA',0,? FROM lista_espera le JOIN paciente p ON le.id_paciente=p.id_paciente WHERE le.id_lista_espera=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mensaje);
            ps.setObject(2, LocalDateTime.now(LIMA));
            ps.setInt(3, lista);
            if (ps.executeUpdate() != 1) throw new SQLException("No se pudo crear la notificación");
        }
    }

    static int clave(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (!rs.next()) throw new SQLException("No se obtuvo la clave generada");
            return rs.getInt(1);
        }
    }
    private LocalDateTime ahora() { return LocalDateTime.now(clock); }
    private static AgendaException invalida() { return new AgendaException(400, "DATOS_INVALIDOS", "El identificador de la oferta no es válido."); }
    private static AgendaException noDisponible() { return new AgendaException(409, "OFERTA_NO_DISPONIBLE", "La oferta ya no está disponible."); }
    private static AgendaException vencida() { return new AgendaException(410, "OFERTA_VENCIDA", "La oferta venció. Conservas tu inscripción en la lista de espera."); }
}
