package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Types;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

public final class AgendaCitaDAO {
    private static final Set<String> REPROGRAMABLES = Set.of("SOLICITADA", "CONFIRMADA");
    private static final String PUBLICADO = "PUBLICADO";
    private static final String OCUPADO = "HORARIO_NO_DISPONIBLE";
    private final ConnectionSource connections;
    private final Clock clock;

    @FunctionalInterface
    interface ConnectionSource { Connection open() throws SQLException; }
    record Medico(int sede, int especialidad) {}
    private record Bloque(int id, LocalTime inicio, LocalTime fin, int minutos) {}

    public AgendaCitaDAO() { this(DatabaseConfig::getConnection); }
    AgendaCitaDAO(ConnectionSource connections) {
        this(connections, Clock.system(ZoneId.of("America/Lima")));
    }
    AgendaCitaDAO(ConnectionSource connections, Clock clock) {
        this.connections = connections;
        this.clock = clock;
    }

    public int reservar(int paciente, int medico, String fecha, String hora, String urgencia) {
        return transaccion(conn -> reservarEnConexion(conn, paciente, medico, fecha, hora, urgencia, 0));
    }

    int reservarEnConexion(Connection conn, int paciente, int medico, String fecha, String hora, String urgencia, int oferta) throws SQLException {
        LocalDateTime inicio = futura(fecha, hora);
        String prioridad = prioridad(urgencia);
        int usuario = paciente(conn, paciente);
        Medico perfil = medico(conn, medico);
        Bloque bloque = bloqueReservable(conn, medico, perfil.sede(), inicio);
        libre(conn, medico, inicio, bloque.minutos(), oferta);
        validarFutura(inicio);
        int id = insertarCita(conn, paciente, medico, bloque.id(), inicio, null, prioridad);
        notificar(conn, usuario, id, "Cita confirmada para el " + inicio.toLocalDate() + " a las " + inicio.toLocalTime() + ".");
        return id;
    }

    void validarCupo(Connection conn, int medico, LocalDateTime inicio) throws SQLException {
        Medico perfil = medico(conn, medico);
        validarFutura(inicio);
        Bloque bloque = bloqueReservable(conn, medico, perfil.sede(), inicio);
        libre(conn, medico, inicio, bloque.minutos(), 0);
    }

    public boolean reprogramar(int anterior, int paciente, int medico, String fecha, String hora,
                               Integer motivo, String urgencia) {
        if (anterior <= 0) throw invalida("La cita no es válida.");
        LocalDateTime inicio = futura(fecha, hora);
        String prioridad = prioridad(urgencia);
        return transaccion(conn -> {
            int usuario = paciente(conn, paciente);
            verificarAnterior(conn, anterior, paciente);
            Medico perfil = medico(conn, medico);
            verificarMotivo(conn, motivo, perfil.especialidad());
            Bloque bloque = bloqueReservable(conn, medico, perfil.sede(), inicio);
            libre(conn, medico, inicio, bloque.minutos(), 0);
            validarFutura(inicio);
            try (PreparedStatement ps = conn.prepareStatement("UPDATE cita SET estado = 'REPROGRAMADA' WHERE id_cita = ? AND id_paciente = ?")) {
                ps.setInt(1, anterior);
                ps.setInt(2, paciente);
                if (ps.executeUpdate() != 1) throw noDisponible();
            }
            int nueva = insertarCita(conn, paciente, medico, bloque.id(), inicio, motivo, prioridad);
            notificar(conn, usuario, nueva, "Cita reprogramada para el " + inicio.toLocalDate() + " a las " + inicio.toLocalTime() + ".");
            return true;
        });
    }

    public int publicar(int medico, int sede, String fecha, String inicio, String fin, int minutos) {
        LocalDateTime desde = futura(fecha, inicio);
        LocalTime hasta = hora(fin);
        validarIntervalo(desde.toLocalTime(), hasta, minutos);
        return transaccion(conn -> {
            Medico perfil = medico(conn, medico);
            validarFutura(desde);
            if (sede != perfil.sede()) throw invalida("La sede debe ser la asignada al médico.");
            sinSolapamiento(conn, medico, desde.toLocalDate(), desde.toLocalTime(), hasta, 0);
            String sql = "INSERT INTO disponibilidad (id_medico,id_sede,fecha,hora_inicio,hora_fin,duracion_minutos,estado) VALUES (?,?,?,?,?,?,'PUBLICADO')";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, medico);
                ps.setInt(2, sede);
                ps.setDate(3, Date.valueOf(desde.toLocalDate()));
                ps.setTime(4, Time.valueOf(desde.toLocalTime()));
                ps.setTime(5, Time.valueOf(hasta));
                ps.setInt(6, minutos);
                ps.executeUpdate();
                return clave(ps);
            }
        });
    }

    public void cambiarEstado(int disponibilidad, String estado) {
        if (disponibilidad <= 0 || !Set.of(PUBLICADO, "CANCELADO", "PENDIENTE", "RECHAZADO").contains(estado == null ? "" : estado)) {
            throw invalida("El turno o su estado no son válidos.");
        }
        transaccion(conn -> {
            int medico = medicoDeBloque(conn, disponibilidad);
            Medico perfil = medico(conn, medico);
            String sql = "SELECT id_sede,fecha,hora_inicio,hora_fin,duracion_minutos FROM disponibilidad WHERE id_disponibilidad = ? FOR UPDATE";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, disponibilidad);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw noDisponible();
                    validarCambio(conn, disponibilidad, medico, perfil, rs, estado);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE disponibilidad SET estado = ? WHERE id_disponibilidad = ?")) {
                ps.setString(1, estado);
                ps.setInt(2, disponibilidad);
                if (ps.executeUpdate() != 1) throw noDisponible();
            }
            return true;
        });
    }

    private void validarCambio(Connection conn, int id, int medico, Medico perfil, ResultSet rs, String estado) throws SQLException {
        LocalDate fecha = rs.getDate("fecha").toLocalDate();
        LocalTime inicio = rs.getTime("hora_inicio").toLocalTime();
        LocalTime fin = rs.getTime("hora_fin").toLocalTime();
        if (PUBLICADO.equals(estado)) {
            if (perfil.sede() != rs.getInt("id_sede")) throw invalida("La sede del turno no coincide con la del médico.");
            validarFutura(LocalDateTime.of(fecha, inicio));
            validarIntervalo(inicio, fin, rs.getInt("duracion_minutos"));
            sinSolapamiento(conn, medico, fecha, inicio, fin, id);
        } else {
            String sql = "SELECT id_cita FROM cita WHERE id_disponibilidad = ? AND fecha_hora > ? AND estado NOT IN ('CANCELADA','REPROGRAMADA') LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.setObject(2, LocalDateTime.now(clock));
                try (ResultSet citas = ps.executeQuery()) {
                    if (citas.next()) throw new AgendaException(409, "TURNO_CON_CITAS", "El turno tiene citas activas. Reprograma o cancela esas citas antes de retirarlo.");
                }
            }
        }
    }

    private <T> T transaccion(TransaccionJdbc.Operacion<T> operacion) {
        return TransaccionJdbc.ejecutar(connections, operacion);
    }

    static int paciente(Connection conn, int id) throws SQLException {
        if (id <= 0) throw invalida("El paciente no es válido.");
        int usuario;
        try (PreparedStatement ps = conn.prepareStatement("SELECT id_usuario FROM paciente WHERE id_paciente=? FOR UPDATE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw invalida("El paciente no está habilitado.");
                usuario = rs.getInt(1);
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT id_usuario FROM usuario WHERE id_usuario=? AND activo=TRUE AND rol='PACIENTE'")) {
            ps.setInt(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw invalida("El paciente no está habilitado.");
            }
        }
        return usuario;
    }

    static void bloquearMedico(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id_medico FROM medico WHERE id_medico=? FOR UPDATE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw noDisponible(); }
        }
    }

    static Medico medico(Connection conn, int id) throws SQLException {
        if (id <= 0) throw invalida("El médico no es válido.");
        String sql = "SELECT id_sede,id_especialidad FROM medico WHERE id_medico=? AND estado='ACTIVO' FOR UPDATE";
        int sede;
        int especialidad;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw noDisponible();
                sede = rs.getInt(1);
                especialidad = rs.getInt(2);
            }
        }
        String activo = "SELECT m.id_medico FROM medico m JOIN usuario u ON m.id_usuario=u.id_usuario JOIN sede s ON m.id_sede=s.id_sede JOIN especialidad e ON m.id_especialidad=e.id_especialidad WHERE m.id_medico=? AND u.activo=TRUE AND u.rol='MEDICO' AND s.estado='ACTIVO' AND e.estado='ACTIVO'";
        try (PreparedStatement ps = conn.prepareStatement(activo)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw noDisponible();
            }
        }
        return new Medico(sede, especialidad);
    }

    private static Bloque bloqueReservable(Connection conn, int medico, int sede, LocalDateTime inicio) throws SQLException {
        String sql = "SELECT id_disponibilidad,hora_inicio,hora_fin,duracion_minutos FROM disponibilidad WHERE id_medico=? AND id_sede=? AND fecha=? AND estado='PUBLICADO' ORDER BY id_disponibilidad FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medico);
            ps.setInt(2, sede);
            ps.setDate(3, Date.valueOf(inicio.toLocalDate()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Bloque bloque = new Bloque(rs.getInt(1), rs.getTime(2).toLocalTime(), rs.getTime(3).toLocalTime(), rs.getInt(4));
                    if (contiene(bloque, inicio.toLocalTime())) return bloque;
                }
            }
        }
        throw noDisponible();
    }

    private static boolean contiene(Bloque bloque, LocalTime hora) {
        long start = bloque.inicio().toSecondOfDay();
        long end = bloque.fin().toSecondOfDay();
        long requested = hora.toSecondOfDay();
        long duration = bloque.minutos() * 60L;
        return duration > 0 && duration <= 86400 && start % 60 == 0 && end % 60 == 0
                && requested >= start && requested + duration <= end && (requested - start) % duration == 0;
    }

    private void libre(Connection conn, int medico, LocalDateTime inicio, int minutos, int oferta) throws SQLException {
        String sql = "SELECT c.fecha_hora,d.duracion_minutos FROM cita c JOIN disponibilidad d ON c.id_disponibilidad=d.id_disponibilidad WHERE c.id_medico=? AND c.fecha_hora>=? AND c.fecha_hora<? AND c.estado NOT IN ('CANCELADA','REPROGRAMADA') "
                + "UNION ALL SELECT c.fecha_hora,d.duracion_minutos FROM reasignacion_inteligente r JOIN cita c ON r.id_cita_original=c.id_cita JOIN disponibilidad d ON r.id_disponibilidad=d.id_disponibilidad JOIN medico m ON c.id_medico=m.id_medico "
                + "WHERE c.id_medico=? AND r.estado='PENDIENTE' AND r.fecha_limite_respuesta>? AND c.fecha_hora>? AND c.estado='CANCELADA' AND d.estado='PUBLICADO' AND d.id_sede=m.id_sede AND r.id_reasignacion<>? "
                + "AND EXISTS(SELECT 1 FROM lista_espera le JOIN paciente p ON le.id_paciente=p.id_paciente JOIN usuario u ON p.id_usuario=u.id_usuario WHERE le.id_lista_espera=r.id_lista_espera AND le.estado='OFERTADA' AND u.activo=TRUE AND u.rol='PACIENTE')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medico);
            ps.setObject(2, inicio.toLocalDate().atStartOfDay());
            ps.setObject(3, inicio.toLocalDate().plusDays(1).atStartOfDay());
            ps.setInt(4, medico);
            ps.setObject(5, LocalDateTime.now(clock));
            ps.setObject(6, LocalDateTime.now(clock));
            ps.setInt(7, oferta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime ocupada = rs.getObject(1, LocalDateTime.class);
                    int duracion = rs.getInt(2);
                    if (duracion <= 0 || duracion > 1440) throw noDisponible();
                    if (inicio.isBefore(ocupada.plusMinutes(duracion)) && ocupada.isBefore(inicio.plusMinutes(minutos))) throw noDisponible();
                }
            }
        }
    }

    private void verificarAnterior(Connection conn, int cita, int paciente) throws SQLException {
        String sql = "SELECT estado,fecha_hora FROM cita WHERE id_cita=? AND id_paciente=? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cita);
            ps.setInt(2, paciente);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new AgendaException(404, "CITA_NO_ENCONTRADA", "No se encontró una cita propia para reprogramar.");
                if (!rs.getObject(2, LocalDateTime.class).isAfter(LocalDateTime.now(clock))) throw invalida("No se puede reprogramar una cita pasada.");
                if (!REPROGRAMABLES.contains(rs.getString(1))) throw new AgendaException(409, "CITA_NO_REPROGRAMABLE", "El estado de la cita no permite reprogramarla.");
            }
        }
    }

    private static void verificarMotivo(Connection conn, Integer motivo, int especialidad) throws SQLException {
        if (motivo == null) return;
        if (motivo <= 0) throw invalida("El motivo no es válido.");
        try (PreparedStatement ps = conn.prepareStatement("SELECT id_motivo FROM motivo_cita WHERE id_motivo=? AND id_especialidad=? AND estado='ACTIVO'")) {
            ps.setInt(1, motivo);
            ps.setInt(2, especialidad);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw invalida("El motivo no pertenece a la especialidad del médico.");
            }
        }
    }

    private static int insertarCita(Connection conn, int paciente, int medico, int bloque, LocalDateTime inicio, Integer motivo, String urgencia) throws SQLException {
        String sql = "INSERT INTO cita (id_paciente,id_medico,id_disponibilidad,fecha_hora,id_motivo,estado,nivel_urgencia,riesgo_cancelacion) VALUES (?,?,?,?,?,'CONFIRMADA',?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, paciente);
            ps.setInt(2, medico);
            ps.setInt(3, bloque);
            ps.setObject(4, inicio);
            if (motivo == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, motivo);
            ps.setString(6, urgencia);
            ps.setString(7, riesgo(conn, paciente));
            ps.executeUpdate();
            return clave(ps);
        }
    }

    private static String riesgo(Connection conn, int paciente) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM cita WHERE id_paciente=? AND estado='CANCELADA'")) {
            ps.setInt(1, paciente);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int canceladas = rs.getInt(1);
                if (canceladas >= 3) return "ALTO";
                return canceladas > 0 ? "MEDIO" : "BAJO";
            }
        }
    }

    private void notificar(Connection conn, int usuario, int cita, String mensaje) throws SQLException {
        String sql = "INSERT INTO notificacion (id_usuario,id_cita,tipo,canal,mensaje,estado_envio,leida,fecha_envio) VALUES (?,?,'Citas','PLATAFORMA',?,'ENVIADA',0,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuario);
            ps.setInt(2, cita);
            ps.setString(3, mensaje);
            ps.setObject(4, LocalDateTime.now(clock));
            ps.executeUpdate();
        }
    }

    private static int clave(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (!rs.next()) throw new SQLException("No se obtuvo la clave generada");
            return rs.getInt(1);
        }
    }

    private static int medicoDeBloque(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id_medico FROM disponibilidad WHERE id_disponibilidad=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw noDisponible();
                return rs.getInt(1);
            }
        }
    }

    private static void sinSolapamiento(Connection conn, int medico, LocalDate fecha, LocalTime inicio, LocalTime fin, int excluir) throws SQLException {
        String sql = "SELECT id_disponibilidad FROM disponibilidad WHERE id_medico=? AND fecha=? AND estado='PUBLICADO' AND hora_inicio<? AND hora_fin>? AND id_disponibilidad<>? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medico);
            ps.setDate(2, Date.valueOf(fecha));
            ps.setTime(3, Time.valueOf(fin));
            ps.setTime(4, Time.valueOf(inicio));
            ps.setInt(5, excluir);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) throw new AgendaException(409, "TURNOS_SUPERPUESTOS", "Ya existe un turno publicado que se cruza con ese horario.");
            }
        }
    }

    private LocalDateTime futura(String fecha, String hora) {
        try {
            if (fecha == null || !fecha.matches("\\d{4}-\\d{2}-\\d{2}")) throw invalida("La fecha no es válida.");
            LocalDateTime inicio = LocalDateTime.of(LocalDate.parse(fecha), hora(hora));
            validarFutura(inicio);
            return inicio;
        } catch (java.time.DateTimeException e) {
            throw invalida("La fecha no es válida.");
        }
    }

    private void validarFutura(LocalDateTime inicio) {
        if (!inicio.isAfter(LocalDateTime.now(clock))) throw invalida("Selecciona una fecha y hora futuras.");
    }

    private static LocalTime hora(String value) {
        try {
            if (value == null || !value.matches("\\d{2}:\\d{2}(:00)?")) throw invalida("La hora no es válida.");
            return LocalTime.parse(value);
        } catch (java.time.DateTimeException e) {
            throw invalida("La hora no es válida.");
        }
    }

    private static void validarIntervalo(LocalTime inicio, LocalTime fin, int minutos) {
        if (minutos <= 0 || minutos > 1440 || fin.toSecondOfDay() - inicio.toSecondOfDay() < minutos * 60L) {
            throw invalida("El intervalo debe permitir al menos una consulta completa con una duración positiva.");
        }
    }

    private static String prioridad(String value) {
        if (value == null || value.isBlank()) return "BAJA";
        if (!Set.of("ALTA", "MEDIA", "BAJA").contains(value)) throw invalida("La urgencia no es válida.");
        return value;
    }

    private static AgendaException invalida(String message) { return new AgendaException(400, "DATOS_INVALIDOS", message); }
    private static AgendaException noDisponible() { return new AgendaException(409, OCUPADO, "El horario no está publicado o ya no está disponible. Selecciona otro horario."); }
}
