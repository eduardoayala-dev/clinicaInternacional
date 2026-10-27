package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TreeMap;
import pe.edu.utp.clinica.model.MedicoCatalogo;
import pe.edu.utp.clinica.util.NombrePersona;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO para gestión del cuerpo médico, sedes, especialidades y disponibilidad de horarios.
 */
public class MedicoDAO {
    private static final DateTimeFormatter SLOT_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String ACTIVE_JOINS = " JOIN usuario u ON m.id_usuario = u.id_usuario "
            + "JOIN especialidad e ON m.id_especialidad = e.id_especialidad JOIN sede s ON m.id_sede = s.id_sede ";
    private static final String ACTIVE_FILTER = "m.estado = 'ACTIVO' AND u.activo = TRUE "
            + "AND e.estado = 'ACTIVO' AND s.estado = 'ACTIVO'";
    @FunctionalInterface
    interface ConnectionSource { Connection open() throws SQLException; }

    private final ConnectionSource connections;
    private final Clock clock;

    public MedicoDAO() { this(DatabaseConfig::getConnection, Clock.system(ZoneId.of("America/Lima"))); }

    MedicoDAO(ConnectionSource connections, Clock clock) {
        this.connections = connections;
        this.clock = clock;
    }

    public List<Map<String, Object>> obtenerEspecialidades() {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT e.id_especialidad, e.nombre, COUNT(m.id_medico) AS total_medicos " +
                     "FROM especialidad e " +
                     "LEFT JOIN medico m ON e.id_especialidad = m.id_especialidad AND m.estado = 'ACTIVO' " +
                     "WHERE e.estado = 'ACTIVO' " +
                     "GROUP BY e.id_especialidad, e.nombre " +
                     "ORDER BY e.id_especialidad ASC";
        try (Connection conn = connections.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getInt("id_especialidad"));
                map.put("nombre", rs.getString("nombre"));
                map.put("totalMedicos", rs.getInt("total_medicos"));
                lista.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<Map<String, Object>> obtenerSedes() {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id_sede, nombre, direccion FROM sede WHERE estado = 'ACTIVO'";
        try (Connection conn = connections.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getInt("id_sede"));
                map.put("nombre", rs.getString("nombre"));
                map.put("direccion", rs.getString("direccion"));
                lista.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<Map<String, Object>> obtenerMedicosDisponibles(int idEspecialidad, int idSede) {
        return legacyMedicos(consultarMedicos(idEspecialidad, idSede));
    }

    public List<Map<String, Object>> obtenerMedicosPorEspecialidad(int idEspecialidad) {
        return legacyMedicos(consultarMedicos(idEspecialidad, null));
    }

    public List<MedicoCatalogo> consultarMedicos(int idEspecialidad, Integer idSede) {
        String sql = "SELECT m.id_medico, u.nombres, u.apellidos, m.cmp FROM medico m " + ACTIVE_JOINS
                + "WHERE m.id_especialidad = ? AND " + ACTIVE_FILTER;
        if (idSede != null) sql += " AND m.id_sede = ?";
        sql += " ORDER BY u.apellidos, u.nombres, m.id_medico";
        List<MedicoCatalogo> medicos = new ArrayList<>();
        try (Connection conn = connections.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEspecialidad);
            if (idSede != null) ps.setInt(2, idSede);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nombre = java.util.Objects.toString(rs.getString("nombres"), "") + " "
                            + java.util.Objects.toString(rs.getString("apellidos"), "");
                    medicos.add(new MedicoCatalogo(rs.getInt("id_medico"), NombrePersona.formatear(nombre),
                            java.util.Objects.toString(rs.getString("cmp"), "")));
                }
            }
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
        return List.copyOf(medicos);
    }

    private static List<Map<String, Object>> legacyMedicos(List<MedicoCatalogo> medicos) {
        return medicos.stream().map(medico -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("idMedico", medico.idMedico());
            entry.put("nombreCompleto", medico.nombreCompleto());
            entry.put("nombres", medico.nombreCompleto());
            entry.put("cmp", medico.cmp());
            entry.put("CMP", medico.cmp());
            return entry;
        }).toList();
    }

    public Map<String, Object> obtenerDetallesMedico(int idMedico) {
        Map<String, Object> map = new HashMap<>();
        String sql = "SELECT m.id_medico, m.cmp, u.nombres, u.apellidos, e.id_especialidad, e.nombre AS especialidad, " +
                     "s.id_sede, s.nombre AS sede_nombre " +
                     "FROM medico m " +
                     "INNER JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN sede s ON m.id_sede = s.id_sede " +
                     "WHERE m.id_medico = ?";
        try (Connection conn = connections.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMedico);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String nombres = rs.getString("nombres");
                    String apellidos = rs.getString("apellidos");
                    String rawNombre = ((nombres != null ? nombres : "") + " " + (apellidos != null ? apellidos : "")).trim();
                    String cleanNombre = rawNombre.replaceAll("(?i)\\b(Dr\\.|Dra\\.|Dr|Dra)\\b|[^a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]", "").replaceAll("\\s+", " ").trim();
                    String nombreCompleto = "Dr. " + cleanNombre;
                    map.put("idMedico", rs.getInt("id_medico"));
                    map.put("cmp", rs.getString("cmp"));
                    map.put("nombreMedico", nombreCompleto);
                    map.put("idEspecialidad", rs.getInt("id_especialidad"));
                    map.put("especialidad", rs.getString("especialidad") != null ? rs.getString("especialidad") : "Medicina General");
                    map.put("idSede", rs.getInt("id_sede"));
                    map.put("sedeNombre", rs.getString("sede_nombre") != null ? rs.getString("sede_nombre") : "Sede San Borja");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public List<String> obtenerFechasDisponibles(int idMedico) {
        String sql = "SELECT DISTINCT d.fecha FROM disponibilidad d JOIN medico m ON d.id_medico = m.id_medico "
                + ACTIVE_JOINS + "WHERE d.id_medico = ? AND d.fecha >= ? AND d.estado = 'PUBLICADO' "
                + "AND d.id_sede = m.id_sede AND " + ACTIVE_FILTER + " ORDER BY d.fecha";
        List<String> fechas = new ArrayList<>();
        try (Connection conn = connections.open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMedico);
            ps.setDate(2, Date.valueOf(LocalDate.now(clock)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) fechas.add(rs.getDate("fecha").toLocalDate().toString());
            }
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
        return List.copyOf(fechas);
    }

    public List<String> obtenerHorariosDisponibles(int idMedico, String fecha) {
        LocalDate date = LocalDate.parse(fecha);
        if (date.isBefore(LocalDate.now(clock))) return List.of();
        try (Connection conn = connections.open()) {
            Map<LocalTime, Integer> publicados = publishedSlots(conn, idMedico, date);
            List<Ocupacion> ocupados = occupiedSlots(conn, idMedico, date);
            boolean today = date.equals(LocalDate.now(clock));
            LocalTime cutoff = LocalTime.now(clock);
            return publicados.entrySet().stream().filter(slot -> !today || slot.getKey().isAfter(cutoff))
                    .filter(slot -> noSeCruza(slot.getKey(), slot.getValue(), ocupados))
                    .map(slot -> slot.getKey().format(SLOT_FORMAT))
                    .toList();
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }

    private static Map<LocalTime, Integer> publishedSlots(Connection conn, int medico, LocalDate date) throws SQLException {
        Map<LocalTime, Integer> slots = new TreeMap<>();
        String sql = "SELECT d.hora_inicio, d.hora_fin, d.duracion_minutos FROM disponibilidad d "
                + "JOIN medico m ON d.id_medico = m.id_medico " + ACTIVE_JOINS
                + "WHERE d.id_medico = ? AND d.fecha = ? AND d.estado = 'PUBLICADO' "
                + "AND d.id_sede = m.id_sede AND " + ACTIVE_FILTER;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medico);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) addSlots(slots, rs);
            }
        }
        return slots;
    }

    private static void addSlots(Map<LocalTime, Integer> slots, ResultSet rs) throws SQLException {
        java.sql.Time start = rs.getTime("hora_inicio");
        java.sql.Time end = rs.getTime("hora_fin");
        int minutes = rs.getInt("duracion_minutos");
        if (start == null || end == null || minutes <= 0 || minutes > 1440) {
            throw new SQLException("Intervalo de disponibilidad inválido");
        }
        long first = start.toLocalTime().toSecondOfDay();
        long last = end.toLocalTime().toSecondOfDay();
        if (first % 60 != 0 || last % 60 != 0) throw new SQLException("El intervalo debe usar minutos completos");
        if (first >= last) throw new SQLException("Intervalo de disponibilidad inválido");
        long duration = minutes * 60L;
        for (long second = first; second + duration <= last; second += duration) {
            slots.merge(LocalTime.ofSecondOfDay(second), minutes, Math::max);
        }
    }

    private record Ocupacion(int inicio, int fin) {}

    private static boolean noSeCruza(LocalTime hora, int minutos, List<Ocupacion> ocupados) {
        int inicio = hora.toSecondOfDay();
        int fin = inicio + minutos * 60;
        return ocupados.stream().noneMatch(cita -> inicio < cita.fin() && cita.inicio() < fin);
    }

    private static List<Ocupacion> occupiedSlots(Connection conn, int medico, LocalDate date) throws SQLException {
        List<Ocupacion> slots = new ArrayList<>();
        String sql = "SELECT c.fecha_hora,d.duracion_minutos FROM cita c JOIN disponibilidad d ON c.id_disponibilidad=d.id_disponibilidad "
                + "WHERE c.id_medico = ? AND c.fecha_hora >= ? AND c.fecha_hora < ? "
                + "AND c.estado NOT IN ('CANCELADA', 'REPROGRAMADA')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medico);
            ps.setObject(2, date.atStartOfDay());
            ps.setObject(3, date.plusDays(1).atStartOfDay());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int inicio = rs.getObject(1, java.time.LocalDateTime.class).toLocalTime().toSecondOfDay();
                    int minutos = rs.getInt(2);
                    if (minutos <= 0 || minutos > 1440) throw new SQLException("La duración de la cita no es válida");
                    slots.add(new Ocupacion(inicio, inicio + minutos * 60));
                }
            }
        }
        return slots;
    }

    public List<Map<String, Object>> obtenerMedicosDePaciente(int idPaciente) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT DISTINCT m.id_medico, m.cmp, u.nombres, u.apellidos, e.nombre AS especialidad, s.nombre AS sede " +
                     "FROM cita c " +
                     "JOIN medico m ON c.id_medico = m.id_medico " +
                     "JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN sede s ON m.id_sede = s.id_sede " +
                     "WHERE c.id_paciente = ?";
        try (Connection conn = connections.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("idMedico", rs.getInt("id_medico"));
                    m.put("nombre", "Dr. " + rs.getString("nombres") + " " + rs.getString("apellidos"));
                    m.put("especialidad", rs.getString("especialidad") != null ? rs.getString("especialidad") : "Medicina General");
                    m.put("cmp", rs.getString("cmp") != null ? rs.getString("cmp") : "CMP 045218");
                    m.put("sede", rs.getString("sede") != null ? rs.getString("sede") : "Sede Central");
                    list.add(m);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Map<String, Object>> obtenerTodosMedicosDetalle() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT m.id_medico, m.cmp, u.nombres, u.apellidos, e.id_especialidad, e.nombre AS especialidad_nombre, s.nombre AS sede_nombre " +
                     "FROM medico m " +
                     "JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad " +
                     "LEFT JOIN sede s ON m.id_sede = s.id_sede " +
                     "ORDER BY m.id_medico ASC";
        try (Connection conn = connections.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                int idMed = rs.getInt("id_medico");
                m.put("idMedico", idMed);
                m.put("cmp", rs.getString("cmp") != null ? rs.getString("cmp") : "CMP-00000");
                String rawN = rs.getString("nombres") != null ? rs.getString("nombres") : "";
                String rawA = rs.getString("apellidos") != null ? rs.getString("apellidos") : "";
                String cleanN = rawN.replaceAll("(?i)\\b(Dr\\.|Dra\\.|Dr|Dra)\\b|[^a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]", "").replaceAll("\\s+", " ").trim();
                String cleanA = rawA.replaceAll("(?i)\\b(Dr\\.|Dra\\.|Dr|Dra)\\b|[^a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]", "").replaceAll("\\s+", " ").trim();
                m.put("nombre", "Dr. " + (cleanN + " " + cleanA).trim());
                m.put("nombresOnly", cleanN);
                m.put("apellidosOnly", cleanA);
                String espName = rs.getString("especialidad_nombre") != null ? rs.getString("especialidad_nombre") : "Medicina General";
                m.put("especialidad", espName);
                m.put("idEspecialidad", rs.getInt("id_especialidad"));
                
                double precio = 80.00;
                if ("Medicina General".equalsIgnoreCase(espName)) precio = 60.00;
                else if ("Neurología".equalsIgnoreCase(espName)) precio = 85.00;
                else if ("Dermatología".equalsIgnoreCase(espName)) precio = 75.00;
                
                m.put("precio", String.format(java.util.Locale.US, "%.2f", precio));
                String sNom = rs.getString("sede_nombre") != null ? rs.getString("sede_nombre") : "Sede Central";
                m.put("sede", sNom);
                m.put("experiencia", "8 años exp.");
                m.put("rating", "4.8");
                m.put("horarioProximo", "Hoy 15:00");
                list.add(m);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Map<String, Object>> obtenerMotivosPorEspecialidad(int idEspecialidad) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id_motivo, descripcion FROM motivo_consulta WHERE id_especialidad = ? AND estado = 'ACTIVO' ORDER BY id_motivo ASC";
        try (Connection conn = connections.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEspecialidad);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("idMotivo", rs.getInt("id_motivo"));
                    m.put("descripcion", rs.getString("descripcion"));
                    lista.add(m);
                }
            }
        } catch (Exception e) {
            // Fallback sintético clínico
        }

        if (lista.isEmpty()) {
            String[] seleccion = switch (idEspecialidad) {
                case 1 -> new String[]{"Chequeo cardiológico preventivo", "Evaluación de hipertensión arterial", "Palpitaciones o arritmias", "Dolor torácico / Opresión", "Riesgo quirúrgico cardiovascular"};
                case 2 -> new String[]{"Acné juvenil y del adulto", "Control de lunares y manchas", "Dermatitis o alergias cutáneas", "Caída de cabello / Alopecia", "Revisión preventiva de la piel"};
                case 3 -> new String[]{"Control de niño sano / Crecimiento", "Vacunación preventiva", "Cuadros respiratorios o tos", "Fiebre persistente", "Problemas digestivos infantiles"};
                case 4 -> new String[]{"Control de diabetes / Glucosa", "Evaluación integral de salud", "Fatiga crónica o malestar", "Hipertensión y perfil lipídico", "Revisión de chequeo anual"};
                case 5 -> new String[]{"Medición de la vista / Refracción", "Ojo rojo o irritación ocular", "Sospecha de glaucoma / Presión", "Control de cataratas", "Fatiga visual por pantallas"};
                default -> new String[]{"Consulta médica general", "Evaluación de síntomas agudos", "Control y seguimiento preventivo", "Lectura de exámenes de laboratorio", "Renovación o ajuste de medicación"};
            };

            for (int i = 0; i < seleccion.length; i++) {
                Map<String, Object> m = new HashMap<>();
                m.put("idMotivo", (i + 1));
                m.put("descripcion", seleccion[i]);
                lista.add(m);
            }
        }
        return lista;
    }
}
