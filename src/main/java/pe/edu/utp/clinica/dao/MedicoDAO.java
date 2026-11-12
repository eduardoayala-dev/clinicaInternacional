package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TreeMap;
import pe.edu.utp.clinica.model.MedicoCatalogo;
import pe.edu.utp.clinica.repository.DisponibilidadRepository;
import pe.edu.utp.clinica.repository.CatalogoMedicoRepository;
import pe.edu.utp.clinica.util.NombrePersona;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO para gestión del cuerpo médico, sedes, especialidades y disponibilidad de horarios.
 */
public class MedicoDAO {
    public record DiaCupos(String fecha, int manana, int tarde) {}

    public List<DiaCupos> disponibilidadSemanal(int especialidad, Integer sede, Integer medico, LocalDate desde) {
        var cantidades = new TreeMap<LocalDate, int[]>();
        for (int dia = 0; dia < 7; dia++) {
            cantidades.put(desde.plusDays(dia), new int[2]);
        }
        try (var conexion = connections.open()) {
            var repositorio = new DisponibilidadRepository(conexion);
            var agendas = repositorio.agendasSemana(especialidad, sede, medico, desde, LocalDate.now(clock));
            if (!agendas.isEmpty()) {
                var seleccionadas = new java.util.HashSet<AgendaDia>();
                for (var agenda : agendas) {
                    seleccionadas.add(new AgendaDia(agenda.medico(), agenda.fecha()));
                }
                var disponibles = cuposPorAgenda(conexion, sede == null ? 0 : sede,
                        desde, desde.plusDays(7), seleccionadas);
                for (var agenda : agendas) {
                    var cantidadDia = cantidades.get(agenda.fecha());
                    var horas = disponibles.getOrDefault(new AgendaDia(agenda.medico(), agenda.fecha()), List.of());
                    for (var hora : horas) {
                        cantidadDia[hora.getHour() < 13 ? 0 : 1]++;
                    }
                }
            }
        } catch (SQLException error) {
            throw new CatalogoDatosException(error);
        }
        return cantidades.entrySet().stream()
                .map(dia -> new DiaCupos(dia.getKey().toString(), dia.getValue()[0], dia.getValue()[1]))
                .toList();
    }
    private static final String ID_ESPECIALIDAD = "idEspecialidad";
    private static final DateTimeFormatter SLOT_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
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
        try (var conexion = connections.open()) {
            return new CatalogoMedicoRepository(conexion).especialidades().stream().map(especialidad -> {
                Map<String, Object> datos = new HashMap<>();
                datos.put("id", especialidad.id());
                datos.put("nombre", especialidad.nombre());
                datos.put("totalMedicos", Math.toIntExact(especialidad.medicos()));
                return datos;
            }).toList();
        } catch (SQLException error) { throw new CatalogoDatosException(error); }
    }

    public List<Map<String, Object>> obtenerSedes() {
        try (var conexion = connections.open()) {
            return new CatalogoMedicoRepository(conexion).sedes().stream().map(sede -> {
                Map<String, Object> datos = new HashMap<>();
                datos.put("id", sede.id());
                datos.put("nombre", sede.nombre());
                datos.put("direccion", sede.direccion());
                return datos;
            }).toList();
        } catch (SQLException error) { throw new CatalogoDatosException(error); }
    }

    public List<Map<String, Object>> obtenerMedicosDisponibles(int idEspecialidad, int idSede) {
        return legacyMedicos(consultarMedicos(idEspecialidad, idSede));
    }

    public List<Map<String, Object>> obtenerMedicosPorEspecialidad(int idEspecialidad) {
        return legacyMedicos(consultarMedicos(idEspecialidad, null));
    }

    public List<MedicoCatalogo> consultarMedicos(int idEspecialidad, Integer idSede) {
        try (var conexion = connections.open()) {
            return new CatalogoMedicoRepository(conexion).disponibles(idEspecialidad, idSede).stream()
                    .map(medico -> new MedicoCatalogo(medico.id(), NombrePersona.formatear(
                            texto(medico.nombres()) + " " + texto(medico.apellidos())), texto(medico.cmp())))
                    .toList();
        } catch (SQLException error) { throw new CatalogoDatosException(error); }
    }

    private static String texto(String valor) { return java.util.Objects.toString(valor, ""); }

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
        try (var conexion = connections.open()) {
            var medicos = new CatalogoMedicoRepository(conexion).detalle(idMedico);
            if (medicos.isEmpty()) return Map.of();
            var medico = medicos.get(0);
            Map<String, Object> datos = new HashMap<>();
            datos.put("idMedico", medico.id());
            datos.put("cmp", medico.cmp());
            datos.put("nombreMedico", "Dr. " + limpiarNombre(texto(medico.nombres()) + " " + texto(medico.apellidos())));
            datos.put(ID_ESPECIALIDAD, medico.especialidad() == null ? 0 : medico.especialidad());
            datos.put("especialidad", java.util.Objects.requireNonNullElse(medico.especialidadNombre(), "Medicina General"));
            datos.put("idSede", medico.sede() == null ? 0 : medico.sede());
            datos.put("sedeNombre", java.util.Objects.requireNonNullElse(medico.sedeNombre(), "Sede San Borja"));
            return datos;
        } catch (SQLException error) { throw new CatalogoDatosException(error); }
    }

    public List<String> obtenerFechasDisponibles(int idMedico) {
        try (var conexion = connections.open()) {
            return new DisponibilidadRepository(conexion).fechasMedico(idMedico, LocalDate.now(clock))
                    .stream().map(LocalDate::toString).toList();
        } catch (SQLException error) {
            throw new CatalogoDatosException(error);
        }
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
        var repositorio = new DisponibilidadRepository(conn);
        Map<LocalTime, Integer> publicados = new TreeMap<>();
        for (var turno : repositorio.turnosMedico(idMedico, date)) {
            addSlots(publicados, turno.inicio(), turno.fin(), turno.minutos());
        }
        var ocupados = new ArrayList<Ocupacion>();
        for (var ocupacion : repositorio.ocupacionesMedico(idMedico, date, java.time.LocalDateTime.now(clock))) {
            ocupados.add(intervalo(ocupacion));
        }
        boolean today = date.equals(LocalDate.now(clock));
        LocalTime cutoff = LocalTime.now(clock);
        return publicados.entrySet().stream().filter(slot -> !today || slot.getKey().isAfter(cutoff))
                .filter(slot -> noSeCruza(slot.getKey(), slot.getValue(), ocupados))
                .map(slot -> slot.getKey().format(SLOT_FORMAT)).toList();
    }
    int contarCuposDisponibles(Connection conexion, int sede, LocalDate desde, LocalDate hasta) throws SQLException {
        return cuposPorAgenda(conexion, sede, desde, hasta, null).values().stream().mapToInt(List::size).sum();
    }

    private Map<AgendaDia, List<LocalTime>> cuposPorAgenda(Connection conexion, int sede,
            LocalDate desde, LocalDate hasta, java.util.Set<AgendaDia> seleccionadas) throws SQLException {
        var publicados = new HashMap<AgendaDia, Map<LocalTime, Integer>>();
        var repositorio = new DisponibilidadRepository(conexion);
        for (var turno : repositorio.turnosPublicados(sede, desde, hasta)) {
            var agenda = new AgendaDia(turno.medico(), turno.fecha());
            if (seleccionadas == null || seleccionadas.contains(agenda)) {
                addSlots(publicados.computeIfAbsent(agenda, clave -> new TreeMap<>()),
                        turno.inicio(), turno.fin(), turno.minutos());
            }
        }
        if (publicados.isEmpty()) {
            return Map.of();
        }
        var ocupados = ocupacionesDeSede(conexion, sede, desde, hasta, publicados.keySet());
        var ahora = java.time.LocalDateTime.now(clock);
        var disponibles = new HashMap<AgendaDia, List<LocalTime>>();
        for (var agenda : publicados.entrySet()) {
            var fecha = agenda.getKey().fecha();
            if (fecha.isBefore(ahora.toLocalDate())) {
                continue;
            }
            var intervalos = ocupados.getOrDefault(agenda.getKey(), List.of());
            var horas = new ArrayList<LocalTime>();
            for (var horario : agenda.getValue().entrySet()) {
                boolean horarioFuturo = !fecha.equals(ahora.toLocalDate())
                        || horario.getKey().isAfter(ahora.toLocalTime());
                if (horarioFuturo && noSeCruza(horario.getKey(), horario.getValue(), intervalos)) {
                    horas.add(horario.getKey());
                }
            }
            disponibles.put(agenda.getKey(), List.copyOf(horas));
        }
        return disponibles;
    }

    private record AgendaDia(int medico, LocalDate fecha) {}

    private Map<AgendaDia, List<Ocupacion>> ocupacionesDeSede(Connection conexion, int sede,
            LocalDate desde, LocalDate hasta, java.util.Set<AgendaDia> seleccionadas) throws SQLException {
        var resultado = new HashMap<AgendaDia, List<Ocupacion>>();
        for (var agenda : seleccionadas) {
            resultado.put(agenda, new ArrayList<>());
        }
        var repositorio = new DisponibilidadRepository(conexion);
        var ahora = java.time.LocalDateTime.now(clock);
        agregarOcupaciones(repositorio.citasOcupadas(sede, desde, hasta), resultado);
        agregarOcupaciones(repositorio.ofertasVigentes(sede, desde, hasta, ahora), resultado);
        agregarOcupaciones(repositorio.retencionesVigentes(sede, desde, hasta, ahora), resultado);
        return resultado;
    }

    private static void agregarOcupaciones(List<DisponibilidadRepository.Ocupacion> ocupaciones,
            Map<AgendaDia, List<Ocupacion>> destino) throws SQLException {
        for (var ocupacion : ocupaciones) {
            var agenda = new AgendaDia(ocupacion.medico(), ocupacion.inicio().toLocalDate());
            if (destino.containsKey(agenda)) {
                destino.get(agenda).add(intervalo(ocupacion));
            }
        }
    }

    private static Ocupacion intervalo(DisponibilidadRepository.Ocupacion ocupacion) throws SQLException {
        if (ocupacion.minutos() <= 0 || ocupacion.minutos() > 1440) {
            throw new SQLException("La duración de la cita no es válida");
        }
        int inicio = ocupacion.inicio().toLocalTime().toSecondOfDay();
        return new Ocupacion(inicio, inicio + ocupacion.minutos() * 60);
    }

    private static void addSlots(Map<LocalTime, Integer> slots, LocalTime start, LocalTime end,
            int minutes) throws SQLException {
        if (start == null || end == null || minutes <= 0 || minutes > 1440) {
            throw new SQLException("Intervalo de disponibilidad inválido");
        }
        long first = start.toSecondOfDay();
        long last = end.toSecondOfDay();
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

    public List<Map<String, Object>> obtenerMedicosDePaciente(int idPaciente) {
        try (var conexion = connections.open()) {
            return new CatalogoMedicoRepository(conexion).dePaciente(idPaciente).stream().map(medico -> {
                Map<String, Object> datos = new HashMap<>();
                datos.put("idMedico", medico.id());
                datos.put("nombre", "Dr. " + medico.nombres() + " " + medico.apellidos());
                datos.put("especialidad", java.util.Objects.requireNonNullElse(medico.especialidadNombre(), "Medicina General"));
                datos.put("cmp", java.util.Objects.requireNonNullElse(medico.cmp(), "CMP 045218"));
                datos.put("sede", java.util.Objects.requireNonNullElse(medico.sedeNombre(), "Sede Central"));
                return datos;
            }).toList();
        } catch (SQLException error) { throw new CatalogoDatosException(error); }
    }

    public List<Map<String, Object>> obtenerMedicosParaEspera() {
        try (var conexion = connections.open()) {
            return new CatalogoMedicoRepository(conexion).disponibles(null, null).stream().map(medico -> {
                Map<String, Object> datos = new HashMap<>();
                datos.put("idMedico", medico.id());
                datos.put(ID_ESPECIALIDAD, medico.especialidad());
                datos.put("idSede", medico.sede());
                datos.put("nombre", NombrePersona.formatear(medico.nombres() + " " + medico.apellidos()));
                return datos;
            }).toList();
        } catch (SQLException error) { throw new CatalogoDatosException(error); }
    }

    public List<Map<String, Object>> obtenerTodosMedicosDetalle() {
        List<Map<String, Object>> list = new ArrayList<>();
        try (var conexion = connections.open()) {
            for (var medico : new CatalogoMedicoRepository(conexion).todos()) {
                Map<String, Object> m = new HashMap<>();
                int idMed = medico.id();
                m.put("idMedico", idMed);
                m.put("cmp", medico.cmp() != null ? medico.cmp() : "CMP-00000");
                String rawN = medico.nombres() != null ? medico.nombres() : "";
                String rawA = medico.apellidos() != null ? medico.apellidos() : "";
                String cleanN = limpiarNombre(rawN);
                String cleanA = limpiarNombre(rawA);
                m.put("nombre", "Dr. " + (cleanN + " " + cleanA).trim());
                m.put("nombresOnly", cleanN);
                m.put("apellidosOnly", cleanA);
                String espName = medico.especialidadNombre() != null ? medico.especialidadNombre() : "Medicina General";
                m.put("especialidad", espName);
                m.put(ID_ESPECIALIDAD, medico.especialidad() == null ? 0 : medico.especialidad());
                
                double precio = 80.00;
                if ("Medicina General".equalsIgnoreCase(espName)) precio = 60.00;
                else if ("Neurología".equalsIgnoreCase(espName)) precio = 85.00;
                else if ("Dermatología".equalsIgnoreCase(espName)) precio = 75.00;
                
                m.put("precio", String.format(java.util.Locale.US, "%.2f", precio));
                String sNom = medico.sedeNombre() != null ? medico.sedeNombre() : "Sede Central";
                m.put("sede", sNom);
                m.put("experiencia", "8 años exp.");
                m.put("rating", "4.8");
                m.put("horarioProximo", "Hoy 15:00");
                list.add(m);
            }
        } catch (SQLException error) { throw new CatalogoDatosException(error); }
        return list;
    }

    private static String limpiarNombre(String nombre) {
        return nombre.replaceAll("(?iu)\\b(Dr\\.|Dra\\.|Dr|Dra)\\b|[^a-záéíóúñ\\s]", "")
                .replaceAll("\\s+", " ").trim();
    }

    public List<Map<String, Object>> obtenerMotivosPorEspecialidad(int especialidad) {
        return new MotivoCitaDAO(connections::open).listarEspecialidad(especialidad);
    }
}
