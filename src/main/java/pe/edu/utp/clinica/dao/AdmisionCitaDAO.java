package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class AdmisionCitaDAO {
    private final AgendaCitaDAO.ConnectionSource connections;
    private final Clock clock;
    private final int tolerancia;

    public record Llegada(int idCita, String fechaLlegada, boolean llegoTarde, int toleranciaMinutos) {}

    public AdmisionCitaDAO() {
        this(DatabaseConfig::getConnection, Clock.system(ZoneId.of("America/Lima")),
                Integer.parseInt(System.getenv().getOrDefault("ADMISION_TOLERANCIA_MINUTOS", "10")));
    }

    AdmisionCitaDAO(AgendaCitaDAO.ConnectionSource connections, Clock clock, int tolerancia) {
        if (tolerancia < 0 || tolerancia > 120) throw new IllegalArgumentException("La tolerancia debe estar entre 0 y 120 minutos.");
        this.connections = connections;
        this.clock = clock.withZone(ZoneId.of("America/Lima"));
        this.tolerancia = tolerancia;
    }

    public int toleranciaMinutos() { return tolerancia; }

    public Llegada registrar(int usuario, int cita) {
        if (usuario <= 0 || cita <= 0) throw new AgendaException(400, "DATOS_INVALIDOS", "Selecciona una cita válida.");
        return TransaccionJdbc.ejecutar(connections, conn -> {
            int sede = sedePersonal(conn, usuario);
            TransaccionJdbc.avisarSede(sede);
            CitaAdmision actual = bloquear(conn,sede,cita);
            LocalDateTime ahora = LocalDateTime.now(clock).withNano(0);
            if (!"CONFIRMADA".equals(actual.estado()) || !actual.programada().toLocalDate().equals(ahora.toLocalDate())) {
                throw new AgendaException(409,"LLEGADA_NO_DISPONIBLE","Solo puedes registrar la llegada de una cita confirmada para hoy.");
            }
            LocalDateTime programada = actual.programada();
            boolean tarde = ahora.isAfter(programada.plusMinutes(tolerancia));
            guardar(conn, usuario, cita, ahora, tarde);
            return new Llegada(cita, ahora.toString(), tarde, tolerancia);
        });
    }

    private static int sedePersonal(Connection conn, int usuario) throws SQLException {
        String sql = "SELECT p.id_sede FROM personal_administrativo p JOIN usuario u ON p.id_usuario=u.id_usuario JOIN sede s ON p.id_sede=s.id_sede WHERE u.id_usuario=? AND u.activo=TRUE AND u.rol='PERSONAL_ADMINISTRATIVO' AND s.estado='ACTIVO' FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new AgendaException(403, "ADMISION_NO_HABILITADA", "Tu perfil de admisión no está habilitado.");
                return rs.getInt(1);
            }
        }
    }

    private static void bloquearPaciente(Connection conn, int paciente) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id_paciente FROM paciente WHERE id_paciente=? FOR UPDATE")) {
            ps.setInt(1, paciente);
            try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw inexistente(); }
        }
    }

    private record CitaAdmision(int paciente, String estado, LocalDateTime programada, boolean llegada) {}
    private static CitaAdmision bloquear(Connection conn,int sede,int cita)throws SQLException {
        int paciente;
        int medico;
        try (PreparedStatement ps = conn.prepareStatement("SELECT c.id_paciente,c.id_medico FROM cita c JOIN medico m ON c.id_medico=m.id_medico WHERE c.id_cita=? AND m.id_sede=?")) {
            ps.setInt(1, cita); ps.setInt(2, sede);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw inexistente();
                paciente = rs.getInt(1); medico = rs.getInt(2);
            }
        }
        bloquearPaciente(conn, paciente);
        if (AgendaCitaDAO.medico(conn, medico).sede() != sede) throw inexistente();
        try (PreparedStatement ps = conn.prepareStatement("SELECT c.estado,c.fecha_hora,a.id_cita FROM cita c LEFT JOIN admision_cita a ON a.id_cita=c.id_cita WHERE c.id_cita=? AND c.id_medico=? AND c.id_paciente=? FOR UPDATE")) {
            ps.setInt(1,cita); ps.setInt(2,medico); ps.setInt(3,paciente);
            try(ResultSet rs=ps.executeQuery()) {
                if(!rs.next()) throw inexistente();
                return new CitaAdmision(paciente,rs.getString(1),rs.getObject(2,LocalDateTime.class),rs.getObject(3)!=null);
            }
        }
    }

    public record Inasistencia(int idCita,String fechaRegistro,String motivo) {}
    public Inasistencia registrarInasistencia(int usuario,int cita,String motivo) {
        String razon=AtencionCitaDAO.motivo(motivo);
        if(usuario<=0 || cita<=0) throw new AgendaException(400,"DATOS_INVALIDOS","Selecciona una cita válida.");
        return TransaccionJdbc.ejecutar(connections,conn->{
            int sede=sedePersonal(conn,usuario);
            TransaccionJdbc.avisarSede(sede);
            CitaAdmision actual=bloquear(conn,sede,cita);
            LocalDateTime ahora=LocalDateTime.now(clock).withNano(0);
            if(!"CONFIRMADA".equals(actual.estado()) || actual.llegada() || !actual.programada().toLocalDate().equals(ahora.toLocalDate())
                    || !ahora.isAfter(actual.programada().plusMinutes(tolerancia))) {
                throw new AgendaException(409,"INASISTENCIA_NO_DISPONIBLE","Verifica la ausencia después de la hora de la cita y su tolerancia. Un paciente que llegó no puede registrarse como ausente.");
            }
            try(PreparedStatement ps=conn.prepareStatement("INSERT INTO inasistencia_cita(id_cita,id_usuario_personal,fecha_registro,motivo,tolerancia_minutos) VALUES(?,?,?,?,?)")) {
                ps.setInt(1,cita);ps.setInt(2,usuario);ps.setObject(3,ahora);ps.setString(4,razon);ps.setInt(5,tolerancia);ps.executeUpdate();
            }
            try(PreparedStatement ps=conn.prepareStatement("UPDATE cita SET estado='NO_ASISTIO' WHERE id_cita=? AND estado='CONFIRMADA'")) {
                ps.setInt(1,cita);if(ps.executeUpdate()!=1) throw new AgendaException(409,"INASISTENCIA_NO_DISPONIBLE","La cita cambió de estado.");
            }
            try(PreparedStatement ps=conn.prepareStatement("INSERT INTO historial_asistencia(id_paciente,id_cita,asistio,observacion,fecha_registro) VALUES(?,?,FALSE,?,?)")) {
                ps.setInt(1,actual.paciente());ps.setInt(2,cita);ps.setString(3,razon);ps.setObject(4,ahora);ps.executeUpdate();
            }
            return new Inasistencia(cita,ahora.toString(),razon);
        });
    }

    private void guardar(Connection conn, int usuario, int cita, LocalDateTime ahora, boolean tarde) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO admision_cita(id_cita,id_usuario_personal,fecha_llegada,tolerancia_minutos,llego_tarde) VALUES(?,?,?,?,?)")) {
            ps.setInt(1, cita); ps.setInt(2, usuario); ps.setObject(3, ahora);
            ps.setInt(4, tolerancia); ps.setBoolean(5, tarde);
            if (ps.executeUpdate() != 1) throw new SQLException("No se registró la llegada.");
        }
        try (PreparedStatement ps = conn.prepareStatement("UPDATE cita SET estado='EN_SALA' WHERE id_cita=? AND estado='CONFIRMADA'")) {
            ps.setInt(1, cita);
            if (ps.executeUpdate() != 1) throw new AgendaException(409, "LLEGADA_NO_DISPONIBLE", "La cita cambió de estado. Actualiza la página.");
        }
    }

    private static AgendaException inexistente() {
        return new AgendaException(404, "CITA_NO_ENCONTRADA", "No se encontró una cita en tu sede.");
    }
}
