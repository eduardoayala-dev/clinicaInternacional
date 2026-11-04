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
    public record DiaCupos(String fecha,int manana,int tarde){}
    public List<DiaCupos> disponibilidadSemanal(int especialidad,Integer sede,Integer medico,LocalDate desde){
        var cantidades=new TreeMap<LocalDate,int[]>();for(int n=0;n<7;n++)cantidades.put(desde.plusDays(n),new int[2]);
        String sql="SELECT DISTINCT d.id_medico,d.fecha FROM disponibilidad d JOIN medico m ON m.id_medico=d.id_medico "+ACTIVE_JOINS+
            " WHERE m.id_especialidad=? AND (?=0 OR m.id_sede=?) AND (?=0 OR m.id_medico=?) AND d.fecha>=? AND d.fecha<? AND d.fecha>=? AND d.estado='PUBLICADO' AND d.id_sede=m.id_sede AND "+ACTIVE_FILTER+" ORDER BY d.fecha,d.id_medico";
        try(var c=connections.open();var st=c.prepareStatement(sql)){
            st.setInt(1,especialidad);st.setInt(2,sede==null?0:sede);st.setInt(3,sede==null?0:sede);st.setInt(4,medico==null?0:medico);st.setInt(5,medico==null?0:medico);st.setObject(6,desde);st.setObject(7,desde.plusDays(7));st.setObject(8,LocalDate.now(clock));
            var agendas=new ArrayList<Map.Entry<Integer,LocalDate>>();try(var r=st.executeQuery()){while(r.next())agendas.add(Map.entry(r.getInt(1),r.getObject(2,LocalDate.class)));}
            if(!agendas.isEmpty()){
                var seleccionadas=new java.util.HashSet<AgendaDia>();
                for(var agenda:agendas)seleccionadas.add(new AgendaDia(agenda.getKey(),agenda.getValue()));
                var disponibles=cuposPorAgenda(c,sede==null?0:sede,desde,desde.plusDays(7),seleccionadas);
                for(var agenda:agendas){
                    var dia=cantidades.get(agenda.getValue());
                    for(var hora:disponibles.getOrDefault(new AgendaDia(agenda.getKey(),agenda.getValue()),List.of()))dia[hora.getHour()<13?0:1]++;
                }
            }
        }catch(SQLException e){throw new CatalogoDatosException(e);}
        return cantidades.entrySet().stream().map(e->new DiaCupos(e.getKey().toString(),e.getValue()[0],e.getValue()[1])).toList();
    }
    private static final String ID_SEDE = "id_sede";
    private static final String ID_ESPECIALIDAD = "idEspecialidad";
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
                map.put("id", rs.getInt(ID_SEDE));
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
                    map.put(ID_ESPECIALIDAD, rs.getInt("id_especialidad"));
                    map.put("especialidad", rs.getString("especialidad") != null ? rs.getString("especialidad") : "Medicina General");
                    map.put("idSede", rs.getInt(ID_SEDE));
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
            return horariosDisponibles(conn,idMedico,date);
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }
    private List<String> horariosDisponibles(Connection conn,int idMedico,LocalDate date)throws SQLException {
            Map<LocalTime, Integer> publicados = publishedSlots(conn, idMedico, date);
            List<Ocupacion> ocupados = occupiedSlots(conn, idMedico, date);
            agregarRetenciones(conn,idMedico,date,ocupados);
            boolean today = date.equals(LocalDate.now(clock));
            LocalTime cutoff = LocalTime.now(clock);
            return publicados.entrySet().stream().filter(slot -> !today || slot.getKey().isAfter(cutoff))
                    .filter(slot -> noSeCruza(slot.getKey(), slot.getValue(), ocupados))
                    .map(slot -> slot.getKey().format(SLOT_FORMAT))
                    .toList();
    }
    int contarCuposDisponibles(Connection conn,int sede,LocalDate desde,LocalDate hasta)throws SQLException {
        return cuposPorAgenda(conn,sede,desde,hasta,null).values().stream().mapToInt(List::size).sum();
    }

    private Map<AgendaDia,List<LocalTime>> cuposPorAgenda(Connection conn,int sede,LocalDate desde,LocalDate hasta,java.util.Set<AgendaDia> seleccionadas)throws SQLException {
        var publicados=new HashMap<AgendaDia,Map<LocalTime,Integer>>();
        String sql="SELECT d.id_medico,d.fecha,d.hora_inicio,d.hora_fin,d.duracion_minutos FROM disponibilidad d JOIN medico m ON m.id_medico=d.id_medico "+ACTIVE_JOINS
                +" WHERE (?=0 OR d.id_sede=?) AND d.fecha>=? AND d.fecha<? AND d.estado='PUBLICADO' AND d.id_sede=m.id_sede AND "+ACTIVE_FILTER;
        try(var st=conn.prepareStatement(sql)){
            st.setInt(1,sede);st.setInt(2,sede);st.setObject(3,desde);st.setObject(4,hasta);
            try(var r=st.executeQuery()){
                while(r.next()){
                    var agenda=new AgendaDia(r.getInt("id_medico"),r.getObject("fecha",LocalDate.class));
                    if(seleccionadas==null||seleccionadas.contains(agenda))addSlots(publicados.computeIfAbsent(agenda,k->new TreeMap<>()),r);
                }
            }
        }
        if(publicados.isEmpty())return Map.of();
        var ocupados=ocupacionesDeSede(conn,sede,desde,hasta,publicados.keySet());
        var ahora=java.time.LocalDateTime.now(clock);
        var disponibles=new HashMap<AgendaDia,List<LocalTime>>();
        for(var agenda:publicados.entrySet()){
            var fecha=agenda.getKey().fecha();
            if(fecha.isBefore(ahora.toLocalDate()))continue;
            var intervalos=ocupados.getOrDefault(agenda.getKey(),List.of());
            var horas=new ArrayList<LocalTime>();
            for(var slot:agenda.getValue().entrySet()){
                if((!fecha.equals(ahora.toLocalDate())||slot.getKey().isAfter(ahora.toLocalTime()))
                        &&noSeCruza(slot.getKey(),slot.getValue(),intervalos))horas.add(slot.getKey());
            }
            disponibles.put(agenda.getKey(),List.copyOf(horas));
        }
        return disponibles;
    }

    private record AgendaDia(int medico,LocalDate fecha){}

    private Map<AgendaDia,List<Ocupacion>> ocupacionesDeSede(Connection conn,int sede,LocalDate desde,LocalDate hasta,java.util.Set<AgendaDia> seleccionadas)throws SQLException {
        var resultado=new HashMap<AgendaDia,List<Ocupacion>>();
        for(var agenda:seleccionadas)resultado.put(agenda,new ArrayList<>());
        String citas="SELECT c.id_medico,c.fecha_hora AS inicio,d.duracion_minutos FROM cita c JOIN disponibilidad d ON d.id_disponibilidad=c.id_disponibilidad JOIN medico m ON m.id_medico=c.id_medico WHERE (?=0 OR m.id_sede=?) AND c.fecha_hora>=? AND c.fecha_hora<? AND c.estado NOT IN ('CANCELADA','REPROGRAMADA')";
        leerOcupaciones(conn,citas,sede,desde,hasta,0,resultado);
        String ofertas="SELECT c.id_medico,c.fecha_hora AS inicio,d.duracion_minutos FROM reasignacion_inteligente r JOIN cita c ON c.id_cita=r.id_cita_original JOIN disponibilidad d ON d.id_disponibilidad=r.id_disponibilidad JOIN medico m ON m.id_medico=c.id_medico WHERE (?=0 OR m.id_sede=?) AND c.fecha_hora>=? AND c.fecha_hora<? AND r.estado='PENDIENTE' AND r.fecha_limite_respuesta>? AND c.fecha_hora>? AND c.estado='CANCELADA' AND d.estado='PUBLICADO' AND d.id_sede=m.id_sede AND EXISTS(SELECT 1 FROM lista_espera le JOIN paciente p ON p.id_paciente=le.id_paciente JOIN usuario u ON u.id_usuario=p.id_usuario WHERE le.id_lista_espera=r.id_lista_espera AND le.estado='OFERTADA' AND u.activo=TRUE AND u.rol='PACIENTE')";
        leerOcupaciones(conn,ofertas,sede,desde,hasta,2,resultado);
        String retenciones="SELECT r.id_medico,r.inicio,r.duracion_minutos FROM retencion_cupo r JOIN medico m ON m.id_medico=r.id_medico WHERE (?=0 OR m.id_sede=?) AND r.inicio>=? AND r.inicio<? AND r.vence>?";
        leerOcupaciones(conn,retenciones,sede,desde,hasta,1,resultado);
        return resultado;
    }

    private void leerOcupaciones(Connection conn,String sql,int sede,LocalDate desde,LocalDate hasta,
            int filtrosVigencia,Map<AgendaDia,List<Ocupacion>> destino)throws SQLException {
        try(var st=conn.prepareStatement(sql)){
            st.setInt(1,sede);st.setInt(2,sede);st.setObject(3,desde.atStartOfDay());st.setObject(4,hasta.atStartOfDay());
            for(int i=0;i<filtrosVigencia;i++)st.setObject(5+i,java.time.LocalDateTime.now(clock));
            try(var r=st.executeQuery()){
                while(r.next()){
                    var inicio=r.getObject("inicio",java.time.LocalDateTime.class);
                    var agenda=new AgendaDia(r.getInt("id_medico"),inicio.toLocalDate());
                    if(!destino.containsKey(agenda))continue;
                    int minutos=r.getInt("duracion_minutos");
                    if(minutos<=0||minutos>1440)throw new SQLException("La duración de la cita no es válida");
                    int segundo=inicio.toLocalTime().toSecondOfDay();
                    destino.get(agenda).add(new Ocupacion(segundo,segundo+minutos*60));
                }
            }
        }
    }

    private void agregarRetenciones(Connection conn,int medico,LocalDate fecha,List<Ocupacion> ocupados) throws SQLException {
        try(var ps=conn.prepareStatement("SELECT inicio,duracion_minutos FROM retencion_cupo WHERE id_medico=? AND inicio>=? AND inicio<? AND vence>?")) {
            ps.setInt(1,medico);ps.setObject(2,fecha.atStartOfDay());ps.setObject(3,fecha.plusDays(1).atStartOfDay());ps.setObject(4,java.time.LocalDateTime.now(clock));
            try(var rs=ps.executeQuery()) {
                while(rs.next()) {
                    int inicio=rs.getObject(1,java.time.LocalDateTime.class).toLocalTime().toSecondOfDay();
                    ocupados.add(new Ocupacion(inicio,inicio+rs.getInt(2)*60));
                }
            }
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

    private List<Ocupacion> occupiedSlots(Connection conn, int medico, LocalDate date) throws SQLException {
        List<Ocupacion> slots = new ArrayList<>();
        String sql = "SELECT c.fecha_hora,d.duracion_minutos FROM cita c JOIN disponibilidad d ON c.id_disponibilidad=d.id_disponibilidad "
                + "WHERE c.id_medico = ? AND c.fecha_hora >= ? AND c.fecha_hora < ? "
                + "AND c.estado NOT IN ('CANCELADA', 'REPROGRAMADA') "
                + "UNION ALL SELECT c.fecha_hora,d.duracion_minutos FROM reasignacion_inteligente r JOIN cita c ON r.id_cita_original=c.id_cita JOIN disponibilidad d ON r.id_disponibilidad=d.id_disponibilidad JOIN medico m ON c.id_medico=m.id_medico "
                + "WHERE c.id_medico=? AND c.fecha_hora>=? AND c.fecha_hora<? AND r.estado='PENDIENTE' AND r.fecha_limite_respuesta>? AND c.fecha_hora>? AND c.estado='CANCELADA' AND d.estado='PUBLICADO' AND d.id_sede=m.id_sede "
                + "AND EXISTS(SELECT 1 FROM lista_espera le JOIN paciente p ON le.id_paciente=p.id_paciente JOIN usuario u ON p.id_usuario=u.id_usuario WHERE le.id_lista_espera=r.id_lista_espera AND le.estado='OFERTADA' AND u.activo=TRUE AND u.rol='PACIENTE')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medico);
            ps.setObject(2, date.atStartOfDay());
            ps.setObject(3, date.plusDays(1).atStartOfDay());
            ps.setInt(4, medico);
            ps.setObject(5, date.atStartOfDay());
            ps.setObject(6, date.plusDays(1).atStartOfDay());
            ps.setObject(7, java.time.LocalDateTime.now(clock));
            ps.setObject(8, java.time.LocalDateTime.now(clock));
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

    public List<Map<String, Object>> obtenerMedicosParaEspera() {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT m.id_medico,m.id_especialidad,m.id_sede,u.nombres,u.apellidos FROM medico m"
                + ACTIVE_JOINS + "WHERE " + ACTIVE_FILTER + " ORDER BY u.apellidos,u.nombres,m.id_medico";
        try (Connection conn = connections.open(); PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> medico = new HashMap<>();
                medico.put("idMedico", rs.getInt("id_medico"));
                medico.put(ID_ESPECIALIDAD, rs.getInt("id_especialidad"));
                medico.put("idSede", rs.getInt(ID_SEDE));
                medico.put("nombre", NombrePersona.formatear(rs.getString("nombres") + " " + rs.getString("apellidos")));
                lista.add(medico);
            }
            return lista;
        } catch (SQLException e) { throw new CatalogoDatosException(e); }
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
                m.put(ID_ESPECIALIDAD, rs.getInt("id_especialidad"));
                
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

    public List<Map<String, Object>> obtenerMotivosPorEspecialidad(int especialidad) {
        return new MotivoCitaDAO(connections::open).listarEspecialidad(especialidad);
    }
}
