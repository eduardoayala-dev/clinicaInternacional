package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.model.medico.AtencionResumen;
import pe.edu.utp.clinica.model.medico.CitaControl;
import pe.edu.utp.clinica.model.medico.HistorialPaciente;
import pe.edu.utp.clinica.model.medico.PacienteResumen;
import pe.edu.utp.clinica.model.medico.Pagina;
import pe.edu.utp.clinica.model.medico.SeguimientoResumen;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class MedicoHistorialDAO implements HistorialMedicoRepository {
    private static final int TAMANO = 20;
    private static final String RELACION = "(t.fecha_inicio IS NOT NULL OR EXISTS(SELECT 1 FROM historial_asistencia h WHERE h.id_cita=c.id_cita AND h.asistio=TRUE))";
    private static final String BUSQUEDA = " AND (LOWER(CONCAT(u.nombres,' ',u.apellidos)) LIKE ? ESCAPE '!' OR p.dni LIKE ? ESCAPE '!')";
    private static final String PACIENTES = "SELECT p.id_paciente,p.dni,p.alergias,p.fecha_nacimiento,u.nombres,u.apellidos,MAX(COALESCE(t.fecha_inicio,c.fecha_hora)) AS ultima,COUNT(*) AS atenciones FROM paciente p JOIN usuario u ON u.id_usuario=p.id_usuario JOIN cita c ON c.id_paciente=p.id_paciente LEFT JOIN atencion_cita t ON t.id_cita=c.id_cita WHERE c.id_medico=? AND " + RELACION;
    private static final String AGRUPAR = " GROUP BY p.id_paciente,p.dni,p.alergias,p.fecha_nacimiento,u.nombres,u.apellidos";
    private static final String SEGUIMIENTOS = "SELECT c.id_cita,c.id_paciente,CONCAT(u.nombres,' ',u.apellidos) AS paciente,p.dni,c.fecha_recomendada_proxima,c.nivel_urgencia,s.id_cita_control,ct.fecha_hora AS fecha_control,CASE WHEN ct.estado='ATENDIDA' THEN 'REALIZADO' WHEN ct.estado IN ('SOLICITADA','CONFIRMADA','EN_SALA','EN_ATENCION') THEN 'RESERVADO' WHEN c.fecha_recomendada_proxima<? THEN 'VENCIDO' ELSE 'PENDIENTE' END AS estado_seguimiento FROM cita c JOIN paciente p ON p.id_paciente=c.id_paciente JOIN usuario u ON u.id_usuario=p.id_usuario LEFT JOIN atencion_cita t ON t.id_cita=c.id_cita LEFT JOIN seguimiento_cita s ON s.id_cita_origen=c.id_cita LEFT JOIN cita ct ON ct.id_cita=s.id_cita_control AND ct.id_medico=c.id_medico AND ct.id_paciente=c.id_paciente WHERE c.id_medico=? AND c.estado='ATENDIDA' AND c.fecha_recomendada_proxima IS NOT NULL AND " + RELACION;
    private static final Set<String> CONTROLES = Set.of("SOLICITADA", "CONFIRMADA", "EN_SALA", "EN_ATENCION", "ATENDIDA");
    private final AgendaCitaDAO.ConnectionSource conexiones;
    private final Clock reloj;

    public MedicoHistorialDAO() { this(DatabaseConfig::getConnection, Clock.system(ZoneId.of("America/Lima"))); }
    MedicoHistorialDAO(AgendaCitaDAO.ConnectionSource conexiones, Clock reloj) {
        this.conexiones = conexiones;
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    public Pagina<PacienteResumen> pacientes(int medico, String consulta, int pagina) {
        String filtro = filtro(consulta, pagina);
        return consultarPagina(PACIENTES + BUSQUEDA + AGRUPAR + " ORDER BY ultima DESC,p.id_paciente DESC LIMIT ? OFFSET ?", pagina, ps -> {
            ps.setInt(1, medico); ps.setString(2, filtro); ps.setString(3, filtro); limites(ps, 4, pagina);
        }, MedicoHistorialDAO::paciente);
    }

    private PacienteResumen paciente(int medico, int paciente) {
        try (Connection conn = conexiones.open(); PreparedStatement ps = conn.prepareStatement(PACIENTES + " AND p.id_paciente=?" + AGRUPAR)) {
            ps.setInt(1, medico); ps.setInt(2, paciente);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw inexistente();
                return paciente(rs);
            }
        } catch (SQLException e) { throw new CatalogoDatosException(e); }
    }

    public HistorialPaciente historial(int medico, int paciente, int pagina) {
        filtro("", pagina);
        PacienteResumen resumen = paciente(medico, paciente);
        String sql = "SELECT c.id_cita,c.fecha_hora,c.estado,c.nivel_urgencia,c.fecha_recomendada_proxima,cc.diagnostico,(SELECT h.observacion FROM historial_asistencia h WHERE h.id_cita=c.id_cita ORDER BY h.id_historial DESC LIMIT 1) AS indicaciones FROM cita c LEFT JOIN atencion_cita t ON t.id_cita=c.id_cita LEFT JOIN consulta_clinica cc ON cc.id_cita=c.id_cita WHERE c.id_medico=? AND c.id_paciente=? AND " + RELACION + " ORDER BY c.fecha_hora DESC,c.id_cita DESC LIMIT ? OFFSET ?";
        Pagina<AtencionResumen> atenciones = consultarPagina(sql, pagina, ps -> {
            ps.setInt(1, medico); ps.setInt(2, paciente); limites(ps, 3, pagina);
        }, rs -> new AtencionResumen(rs.getInt("id_cita"), rs.getObject("fecha_hora", LocalDateTime.class), rs.getString("estado"), rs.getString("nivel_urgencia"), texto(rs, "indicaciones"), texto(rs, "diagnostico"), rs.getObject("fecha_recomendada_proxima", LocalDate.class)));
        return new HistorialPaciente(resumen, atenciones);
    }

    public Pagina<SeguimientoResumen> seguimientos(int medico, String consulta, String estado, int pagina) {
        String filtro = filtro(consulta, pagina);
        if (estado == null) throw invalida("Selecciona un estado válido.");
        if (!Set.of("", "PENDIENTE", "VENCIDO", "RESERVADO", "REALIZADO").contains(estado)) throw invalida("Selecciona un estado de seguimiento válido.");
        String sql = "SELECT * FROM (" + SEGUIMIENTOS + BUSQUEDA + ") registros WHERE (?='' OR estado_seguimiento=?) ORDER BY fecha_recomendada_proxima,id_cita LIMIT ? OFFSET ?";
        return consultarPagina(sql, pagina, ps -> {
            ps.setObject(1, LocalDate.now(reloj)); ps.setInt(2, medico); ps.setString(3, filtro); ps.setString(4, filtro);
            ps.setString(5, estado); ps.setString(6, estado); limites(ps, 7, pagina);
        }, MedicoHistorialDAO::seguimiento);
    }

    public SeguimientoResumen seguimiento(int medico, int origen) {
        try (Connection conn = conexiones.open(); PreparedStatement ps = conn.prepareStatement(SEGUIMIENTOS + " AND c.id_cita=?")) {
            ps.setObject(1, LocalDate.now(reloj)); ps.setInt(2, medico); ps.setInt(3, origen);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw inexistente();
                return seguimiento(rs);
            }
        } catch (SQLException e) { throw new CatalogoDatosException(e); }
    }

    public List<CitaControl> controles(int medico, int origen) {
        SeguimientoResumen seguimiento = seguimiento(medico, origen);
        String sql = "SELECT ct.id_cita,ct.fecha_hora,ct.estado FROM cita ct JOIN cita co ON co.id_cita=? LEFT JOIN atencion_cita ao ON ao.id_cita=co.id_cita WHERE ct.id_medico=? AND ct.id_paciente=? AND ct.id_cita<>co.id_cita AND ct.fecha_hora>COALESCE(ao.fecha_fin,co.fecha_hora) AND ct.estado IN ('SOLICITADA','CONFIRMADA','EN_SALA','EN_ATENCION','ATENDIDA') ORDER BY ct.fecha_hora,ct.id_cita LIMIT 100";
        try (Connection conn = conexiones.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, origen); ps.setInt(2, medico); ps.setInt(3, seguimiento.idPaciente());
            try (ResultSet rs = ps.executeQuery()) {
                List<CitaControl> citas = new ArrayList<>();
                while (rs.next()) citas.add(new CitaControl(rs.getInt(1), rs.getObject(2, LocalDateTime.class), rs.getString(3)));
                return List.copyOf(citas);
            }
        } catch (SQLException e) { throw new CatalogoDatosException(e); }
    }

    public void vincular(int medico, int origen, int control) {
        TransaccionJdbc.ejecutar(conexiones, conn -> {
            SeguimientoVinculo datos = bloquearVinculo(conn, medico, origen, control);
            if (!CONTROLES.contains(datos.estadoControl())) throw conflicto("La cita de control debe estar vigente o atendida.");
            try (PreparedStatement ps = conn.prepareStatement("SELECT s.id_cita_control,c.estado FROM seguimiento_cita s JOIN cita co ON co.id_cita=s.id_cita_origen LEFT JOIN cita c ON c.id_cita=s.id_cita_control AND c.id_medico=co.id_medico AND c.id_paciente=co.id_paciente WHERE s.id_cita_origen=? FOR UPDATE")) {
                ps.setInt(1, origen);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) != control && CONTROLES.contains(Objects.toString(rs.getString(2), ""))) throw conflicto("Ya existe una reserva vigente vinculada a este seguimiento.");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO seguimiento_cita(id_cita_origen,id_cita_control,id_usuario_medico,fecha_vinculacion) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE id_cita_control=?,id_usuario_medico=?,fecha_vinculacion=?")) {
                LocalDateTime ahora = LocalDateTime.now(reloj).withNano(0);
                ps.setInt(1, origen); ps.setInt(2, control); ps.setInt(3, datos.usuario()); ps.setObject(4, ahora);
                ps.setInt(5, control); ps.setInt(6, datos.usuario()); ps.setObject(7, ahora); ps.executeUpdate();
            }
            return null;
        });
    }

    private SeguimientoVinculo bloquearVinculo(Connection conn, int medico, int origen, int control) throws SQLException {
        int paciente;
        try (PreparedStatement ps = conn.prepareStatement("SELECT id_paciente FROM cita WHERE id_cita=? AND id_medico=?")) {
            ps.setInt(1, origen); ps.setInt(2, medico);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { throw inexistente(); }
                paciente = rs.getInt(1);
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT id_paciente FROM paciente WHERE id_paciente=? FOR UPDATE")) {
            ps.setInt(1, paciente); try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) { throw inexistente(); } }
        }
        AgendaCitaDAO.medico(conn, medico);
        String sql = "SELECT m.id_usuario,ct.estado FROM cita c JOIN medico m ON m.id_medico=c.id_medico JOIN usuario u ON u.id_usuario=m.id_usuario JOIN cita ct ON ct.id_cita=? AND ct.id_medico=c.id_medico AND ct.id_paciente=c.id_paciente LEFT JOIN atencion_cita t ON t.id_cita=c.id_cita WHERE c.id_cita=? AND c.id_medico=? AND c.id_paciente=? AND c.estado='ATENDIDA' AND c.fecha_recomendada_proxima IS NOT NULL AND " + RELACION + " AND ct.id_cita<>c.id_cita AND ct.fecha_hora>COALESCE(t.fecha_fin,c.fecha_hora) AND u.activo=TRUE AND u.rol='MEDICO' FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, control); ps.setInt(2, origen); ps.setInt(3, medico); ps.setInt(4, paciente);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw inexistente();
                return new SeguimientoVinculo(rs.getInt(1), rs.getString(2));
            }
        }
    }

    private <T> Pagina<T> consultarPagina(String sql, int pagina, Parametros parametros, Fila<T> fila) {
        try (Connection conn = conexiones.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            parametros.configurar(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> datos = new ArrayList<>();
                while (rs.next()) datos.add(fila.leer(rs));
                return new Pagina<>(datos.subList(0, Math.min(datos.size(), TAMANO)), pagina, TAMANO, datos.size() > TAMANO);
            }
        } catch (SQLException e) { throw new CatalogoDatosException(e); }
    }

    private static void limites(PreparedStatement ps, int indice, int pagina) throws SQLException {
        ps.setInt(indice, TAMANO + 1); ps.setInt(indice + 1, (pagina - 1) * TAMANO);
    }
    private static String filtro(String consulta, int pagina) {
        String texto = consulta == null ? "" : consulta.strip();
        if (pagina < 1 || pagina > 10000 || texto.length() > 100 || texto.chars().anyMatch(Character::isISOControl)) throw invalida("Revisa la búsqueda o la página solicitada.");
        return "%" + texto.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
    private static PacienteResumen paciente(ResultSet rs) throws SQLException {
        return new PacienteResumen(rs.getInt("id_paciente"), texto(rs, "nombres") + " " + texto(rs, "apellidos"), texto(rs, "dni"), texto(rs, "alergias"), rs.getObject("fecha_nacimiento", LocalDate.class), rs.getObject("ultima", LocalDateTime.class), rs.getInt("atenciones"));
    }
    private static SeguimientoResumen seguimiento(ResultSet rs) throws SQLException {
        return new SeguimientoResumen(rs.getInt("id_cita"), rs.getInt("id_paciente"), rs.getString("paciente"), rs.getString("dni"), rs.getObject("fecha_recomendada_proxima", LocalDate.class), rs.getString("nivel_urgencia"), rs.getString("estado_seguimiento"), rs.getObject("id_cita_control", Integer.class), rs.getObject("fecha_control", LocalDateTime.class));
    }
    private static String texto(ResultSet rs, String columna) throws SQLException { return Objects.toString(rs.getString(columna), ""); }
    private static AgendaException inexistente() { return new AgendaException(404, "REGISTRO_NO_ENCONTRADO", "No se encontró un registro autorizado para este médico."); }
    private static AgendaException invalida(String mensaje) { return new AgendaException(400, "FILTRO_INVALIDO", mensaje); }
    private static AgendaException conflicto(String mensaje) { return new AgendaException(409, "SEGUIMIENTO_NO_DISPONIBLE", mensaje); }
    private record SeguimientoVinculo(int usuario, String estadoControl) {}
    @FunctionalInterface private interface Parametros { void configurar(PreparedStatement ps) throws SQLException; }
    @FunctionalInterface private interface Fila<T> { T leer(ResultSet rs) throws SQLException; }
}
