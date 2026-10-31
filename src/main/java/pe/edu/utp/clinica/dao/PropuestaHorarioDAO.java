package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.model.medico.PropuestaHorario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public final class PropuestaHorarioDAO {
    private final AgendaCitaDAO.ConnectionSource connections;
    private final Clock clock;
    private final AgendaCitaDAO agenda;
    private static final String SELECT = "SELECT p.*,CONCAT(u.nombres,' ',u.apellidos) nombre_medico,s.nombre nombre_sede FROM propuesta_horario p JOIN medico m ON p.id_medico=m.id_medico JOIN usuario u ON m.id_usuario=u.id_usuario JOIN sede s ON p.id_sede=s.id_sede ";

    public PropuestaHorarioDAO() { this(DatabaseConfig::getConnection, Clock.system(ZoneId.of("America/Lima"))); }
    PropuestaHorarioDAO(AgendaCitaDAO.ConnectionSource connections, Clock clock) {
        this.connections = connections;
        this.clock = clock.withZone(ZoneId.of("America/Lima"));
        agenda = new AgendaCitaDAO(connections, this.clock);
    }

    public int proponer(int usuario, int dias, LocalDate desde, LocalDate hasta,
                        LocalTime inicio, LocalTime fin, int minutos) {
        validar(dias, desde, hasta, inicio, fin, minutos);
        return TransaccionJdbc.ejecutar(connections, conn -> {
            int medico = medicoUsuario(conn, usuario);
            int sede = AgendaCitaDAO.medico(conn, medico).sede();
            String sql = "INSERT INTO propuesta_horario(id_medico,id_sede,dias_semana,vigente_desde,vigente_hasta,hora_inicio,hora_fin,duracion_minutos,estado,creada_en) VALUES(?,?,?,?,?,?,?,?,'PENDIENTE',?)";
            try (var ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, medico); ps.setInt(2, sede); ps.setInt(3, dias);
                ps.setObject(4, desde); ps.setObject(5, hasta); ps.setObject(6, inicio); ps.setObject(7, fin);
                ps.setInt(8, minutos); ps.setObject(9, ahoraUtc());
                ps.executeUpdate();
                try (var rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) throw new SQLException("Propuesta sin identificador");
                    return rs.getInt(1);
                }
            }
        });
    }

    public List<PropuestaHorario> delMedico(int usuario) {
        return TransaccionJdbc.ejecutar(connections, conn -> listar(conn, "WHERE p.id_medico=? ORDER BY p.id_propuesta DESC LIMIT 100", medicoUsuario(conn, usuario)));
    }

    public List<PropuestaHorario> paraAdmision(int usuario) {
        return TransaccionJdbc.ejecutar(connections, conn -> listar(conn, "WHERE p.id_sede=? ORDER BY CASE WHEN p.estado='PENDIENTE' THEN 0 ELSE 1 END,p.id_propuesta DESC LIMIT 100", sedePersonal(conn, usuario)));
    }

    public int revisar(int usuario, int id, boolean aprobar, String motivo) {
        if (id <= 0) throw invalida("Selecciona una propuesta válida.");
        String razon = motivo == null ? "" : motivo.strip();
        if (razon.length() > 500 || (!aprobar && razon.length() < 5)) throw invalida("Para rechazar, explica el motivo (entre 5 y 500 caracteres).");
        return TransaccionJdbc.ejecutar(connections, conn -> {
            int sede = sedePersonal(conn, usuario);
            PropuestaHorario propuesta = bloquear(conn, id, sede);
            if (!"PENDIENTE".equals(propuesta.estado())) throw new AgendaException(409, "PROPUESTA_REVISADA", "Esta propuesta ya fue revisada.");
            int bloques = aprobar ? publicar(conn, propuesta) : 0;
            try (var ps = conn.prepareStatement("UPDATE propuesta_horario SET estado=?,revisada_en=?,id_revisor=?,motivo_revision=? WHERE id_propuesta=?")) {
                ps.setString(1, aprobar ? "PUBLICADO" : "RECHAZADO"); ps.setObject(2, ahoraUtc());
                ps.setInt(3, usuario); ps.setString(4, razon); ps.setInt(5, id); ps.executeUpdate();
            }
            return bloques;
        });
    }

    private int publicar(Connection conn, PropuestaHorario p) throws SQLException {
        if (AgendaCitaDAO.medico(conn, p.medico()).sede() != p.sede()) throw invalida("La sede del médico cambió. Solicita una nueva propuesta.");
        int publicados = 0;
        for (LocalDate fecha = p.desde(); !fecha.isAfter(p.hasta()); fecha = fecha.plusDays(1)) {
            if (p.incluye(fecha) && LocalDateTime.of(fecha, p.inicio()).isAfter(LocalDateTime.now(clock))) {
                int bloque = agenda.publicarEnConexion(conn, p.medico(), p.sede(), LocalDateTime.of(fecha, p.inicio()), p.fin(), p.minutos());
                try (var ps = conn.prepareStatement("UPDATE disponibilidad SET id_propuesta=? WHERE id_disponibilidad=?")) {
                    ps.setInt(1, p.id()); ps.setInt(2, bloque); ps.executeUpdate();
                }
                publicados++;
            }
        }
        if (publicados == 0) throw invalida("La propuesta ya no tiene turnos futuros. Solicita una nueva vigencia.");
        return publicados;
    }

    private void validar(int dias, LocalDate desde, LocalDate hasta, LocalTime inicio, LocalTime fin, int minutos) {
        if (dias < 1 || dias > 127 || desde == null || hasta == null || inicio == null || fin == null) throw invalida("Completa los días, las fechas y las horas.");
        validarFechas(desde, hasta);
        validarHoras(inicio, fin, minutos);
        boolean futuro = desde.datesUntil(hasta.plusDays(1)).anyMatch(f -> (dias & (1 << (f.getDayOfWeek().getValue()-1))) != 0 && LocalDateTime.of(f, inicio).isAfter(LocalDateTime.now(clock)));
        if (!futuro) throw invalida("No hay un día y hora futuros dentro de la vigencia seleccionada.");
    }

    private void validarFechas(LocalDate desde, LocalDate hasta) {
        if (desde.getYear()>9999 || hasta.getYear()>9999 || desde.isBefore(LocalDate.now(clock)) || hasta.isBefore(desde) || hasta.isAfter(desde.plusDays(89))) throw invalida("La vigencia debe comenzar desde hoy y abarcar como máximo 90 días, incluyendo ambas fechas.");
    }

    private static void validarHoras(LocalTime inicio, LocalTime fin, int minutos) {
        long segundos = fin.toSecondOfDay() - (long) inicio.toSecondOfDay();
        if (inicio.getSecond() != 0 || fin.getSecond() != 0 || inicio.getNano() != 0 || fin.getNano() != 0 || minutos < 5 || minutos > 240 || segundos < minutos * 60L || segundos % (minutos * 60L) != 0) throw invalida("El turno debe contener citas completas de entre 5 y 240 minutos y terminar el mismo día.");
    }

    private static int medicoUsuario(Connection conn, int usuario) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT m.id_medico FROM medico m JOIN usuario u ON m.id_usuario=u.id_usuario WHERE u.id_usuario=? AND u.activo=TRUE AND u.rol='MEDICO' AND m.estado='ACTIVO'")) {
            ps.setInt(1, usuario);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) throw new AgendaException(403, "MEDICO_NO_HABILITADO", "Tu perfil médico no está habilitado.");
                return rs.getInt(1);
            }
        }
    }

    private static int sedePersonal(Connection conn, int usuario) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT p.id_sede FROM personal_administrativo p JOIN usuario u ON p.id_usuario=u.id_usuario JOIN sede s ON p.id_sede=s.id_sede WHERE u.id_usuario=? AND u.activo=TRUE AND u.rol='PERSONAL_ADMINISTRATIVO' AND s.estado='ACTIVO'")) {
            ps.setInt(1, usuario);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) throw new AgendaException(403, "ADMISION_NO_HABILITADA", "Tu perfil de admisión no está habilitado.");
                return rs.getInt(1);
            }
        }
    }

    private static PropuestaHorario bloquear(Connection conn, int id, int sede) throws SQLException {
        try (var ps = conn.prepareStatement(SELECT + "WHERE p.id_propuesta=? AND p.id_sede=? FOR UPDATE")) {
            ps.setInt(1, id); ps.setInt(2, sede);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) throw new AgendaException(404, "PROPUESTA_NO_ENCONTRADA", "La propuesta no está disponible para tu sede.");
                return leer(rs);
            }
        }
    }

    private static List<PropuestaHorario> listar(Connection conn, String condicion, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + condicion)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<PropuestaHorario> lista = new ArrayList<>();
                while (rs.next()) lista.add(leer(rs));
                return List.copyOf(lista);
            }
        }
    }

    private static PropuestaHorario leer(ResultSet rs) throws SQLException {
        return new PropuestaHorario(rs.getInt("id_propuesta"), rs.getInt("id_medico"), rs.getInt("id_sede"), rs.getString("nombre_medico"), rs.getString("nombre_sede"),
                rs.getInt("dias_semana"), rs.getDate("vigente_desde").toLocalDate(), rs.getDate("vigente_hasta").toLocalDate(), rs.getTime("hora_inicio").toLocalTime(), rs.getTime("hora_fin").toLocalTime(), rs.getInt("duracion_minutos"), rs.getString("estado"), rs.getString("motivo_revision"));
    }

    private LocalDateTime ahoraUtc() { return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC); }
    private static AgendaException invalida(String mensaje) { return new AgendaException(400, "PROPUESTA_INVALIDA", mensaje); }
}
