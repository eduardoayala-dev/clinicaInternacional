package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.repository.ReservaCitaRepository;
import pe.edu.utp.clinica.repository.DisponibilidadRepository;
import pe.edu.utp.clinica.persistence.entity.DisponibilidadEntidad;
import java.sql.Connection;
import java.sql.SQLException;
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
    private final boolean correosActivos;
    private final Integer horasAnticipacionConfirmacion;

    @FunctionalInterface
    interface ConnectionSource { Connection open() throws SQLException; }
    record Medico(int sede, int especialidad) {}
    private record Reserva(int paciente,int medico,String fecha,String hora,String urgencia,Integer motivo) {}
    private record Bloque(int id, LocalTime inicio, LocalTime fin, int minutos) {}

    public AgendaCitaDAO() { this(DatabaseConfig::getConnection); }
    AgendaCitaDAO(ConnectionSource connections) {
        this(connections, Clock.system(ZoneId.of("America/Lima")));
    }
    AgendaCitaDAO(ConnectionSource connections, Clock clock) {
        this(connections,clock,Boolean.parseBoolean(System.getenv().getOrDefault("APPOINTMENT_EMAIL_ENABLED","false")));
    }
    AgendaCitaDAO(ConnectionSource connections, Clock clock,boolean correosActivos) {
        this.connections=connections;this.clock=clock;this.correosActivos=correosActivos;this.horasAnticipacionConfirmacion=null;
    }
    AgendaCitaDAO(ConnectionSource connections, Clock clock, boolean correosActivos, int horasAnticipacionConfirmacion) {
        if (horasAnticipacionConfirmacion < 1 || horasAnticipacionConfirmacion > 168) throw new IllegalArgumentException("El plazo debe estar entre 1 y 168 horas.");
        this.connections = connections;
        this.clock = clock;
        this.correosActivos=correosActivos;
        this.horasAnticipacionConfirmacion=horasAnticipacionConfirmacion;
    }

    public int reservar(int paciente, int medico, String fecha, String hora, String urgencia) {
        return reservar(paciente,medico,fecha,hora,urgencia,null);
    }
    public int reservar(int paciente,int medico,String fecha,String hora,String urgencia,Integer motivo) {
        return transaccion(conn -> reservarEnConexion(conn,new Reserva(paciente,medico,fecha,hora,urgencia,motivo),0));
    }

    public record Retencion(String identificador, long segundosRestantes) {}

    public Retencion retener(int paciente, int medico, String fecha, String hora) {
        LocalDateTime inicio = futura(fecha, hora);
        return transaccion(conn -> {
            paciente(conn, paciente);
            Medico perfil = medico(conn, medico);
            Bloque bloque = bloqueReservable(conn, medico, perfil.sede(), inicio);
            validarFutura(inicio);
            LocalDateTime ahora = LocalDateTime.now(clock).withNano(0);
            var repositorio = new ReservaCitaRepository(conn);
            var existente = repositorio.retencion(paciente, medico, inicio, ahora);
            if (existente != null) return new Retencion(existente.identificador(), segundos(ahora, existente.vence()));
            repositorio.limpiarRetenciones(paciente, medico, ahora);
            libre(conn, medico, inicio, bloque.minutos(), 0);
            String id = java.util.UUID.randomUUID().toString();
            LocalDateTime vence = ahora.plusMinutes(5);
            if (vence.isAfter(inicio)) vence = inicio;
            repositorio.retener(paciente, medico, inicio, bloque.minutos(), id, vence);
            return new Retencion(id, segundos(ahora, vence));
        });
    }

    private static long segundos(LocalDateTime desde, LocalDateTime hasta) {
        var zona = ZoneId.of("America/Lima");
        return java.time.Duration.between(desde.atZone(zona), hasta.atZone(zona)).getSeconds();
    }

    public boolean liberarRetencion(int paciente, String identificador) {
        return transaccion(conn -> {
            paciente(conn, paciente);
            new ReservaCitaRepository(conn).liberarRetencion(paciente, identificador, null, null, null);
            return true;
        });
    }

    public int reservarRetenida(int paciente,int medico,String fecha,String hora,Integer motivo,String identificador) {
        LocalDateTime inicio=futura(fecha,hora);
        return transaccion(conn -> {
            paciente(conn,paciente);medico(conn,medico);
            if (new ReservaCitaRepository(conn).liberarRetencion(paciente, identificador, medico, inicio, LocalDateTime.now(clock)) != 1) {
                throw new AgendaException(409,"RETENCION_VENCIDA","El cupo ya no está retenido para este formulario. Selecciona el horario nuevamente.");
            }
            return reservarEnConexion(conn,new Reserva(paciente,medico,fecha,hora,"MEDIA",motivo),0);
        });
    }

    int reservarEnConexion(Connection conn, int paciente, int medico, String fecha, String hora, String urgencia, int oferta) throws SQLException {
        return reservarEnConexion(conn,new Reserva(paciente,medico,fecha,hora,urgencia,null),oferta);
    }
    private int reservarEnConexion(Connection conn,Reserva reserva,int oferta) throws SQLException {
        LocalDateTime inicio = futura(reserva.fecha(), reserva.hora());
        validarPrioridad(reserva.urgencia());
        int usuario = paciente(conn, reserva.paciente());
        Medico perfil = medico(conn, reserva.medico());
        String prioridad=new ReservaCitaRepository(conn).prioridad(reserva.paciente(),perfil.especialidad(),inicio.toLocalDate());
        verificarMotivo(conn,reserva.motivo(),perfil.especialidad());
        Bloque bloque = bloqueReservable(conn, reserva.medico(), perfil.sede(), inicio);
        libre(conn, reserva.medico(), inicio, bloque.minutos(), oferta);
        validarFutura(inicio);
        int id = insertarCita(conn, reserva.paciente(), reserva.medico(), bloque.id(), inicio, reserva.motivo(), prioridad);
        TransaccionJdbc.avisarSede(perfil.sede());
        notificar(conn, usuario, id, "Cita confirmada para el " + inicio.toLocalDate() + " a las " + inicio.toLocalTime() + ".",true);
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
        return reprogramarConId(anterior, paciente, medico, fecha, hora, motivo, urgencia) > 0;
    }

    public int reprogramarConId(int anterior, int paciente, int medico, String fecha, String hora,
                               Integer motivo, String urgencia) {
        if (anterior <= 0) throw invalida("La cita no es válida.");
        LocalDateTime inicio = futura(fecha, hora);
        validarPrioridad(urgencia);
        return transaccion(conn -> {
            int usuario = paciente(conn, paciente);
            verificarAnterior(conn, anterior, paciente);
            Medico perfil = medico(conn, medico);
            String prioridad=new ReservaCitaRepository(conn).prioridad(paciente,perfil.especialidad(),inicio.toLocalDate());
            verificarMotivo(conn, motivo, perfil.especialidad());
            Bloque bloque = bloqueReservable(conn, medico, perfil.sede(), inicio);
            libre(conn, medico, inicio, bloque.minutos(), 0);
            validarFutura(inicio);
            if (new ReservaCitaRepository(conn).estado(anterior, paciente, "REPROGRAMADA", null, null) != 1) throw noDisponible();
            int nueva = insertarCita(conn, paciente, medico, bloque.id(), inicio, motivo, prioridad);
            TransaccionJdbc.avisarSede(perfil.sede());
            if(correosActivos)new ReservaCitaRepository(conn).correoCambio(nueva,anterior,"REPROGRAMADA",clock.instant());
            notificar(conn, usuario, nueva, "Cita reprogramada para el " + inicio.toLocalDate() + " a las " + inicio.toLocalTime() + ".",false);
            return nueva;
        });
    }

    public int publicar(int medico, int sede, String fecha, String inicio, String fin, int minutos) {
        LocalDateTime desde = futura(fecha, inicio);
        LocalTime hasta = hora(fin);
        validarIntervalo(desde.toLocalTime(), hasta, minutos);
        return transaccion(conn -> publicarEnConexion(conn, medico, sede, desde, hasta, minutos));
    }

    int publicarEnConexion(Connection conn, int medico, int sede, LocalDateTime desde, LocalTime hasta, int minutos) throws SQLException {
            validarIntervalo(desde.toLocalTime(), hasta, minutos);
            Medico perfil = medico(conn, medico);
            validarFutura(desde);
            if (sede != perfil.sede()) throw invalida("La sede debe ser la asignada al médico.");
            sinSolapamiento(conn, medico, desde.toLocalDate(), desde.toLocalTime(), hasta, 0);
            return new ReservaCitaRepository(conn).publicar(medico, sede, desde.toLocalDate(), desde.toLocalTime(), hasta, minutos);
    }

    public void cambiarEstado(int disponibilidad, String estado) {
        if (disponibilidad <= 0 || !Set.of(PUBLICADO, "CANCELADO", "PENDIENTE", "RECHAZADO").contains(estado == null ? "" : estado)) {
            throw invalida("El turno o su estado no son válidos.");
        }
        transaccion(conn -> {
            int medico = medicoDeBloque(conn, disponibilidad);
            Medico perfil = medico(conn, medico);
            var bloque = new ReservaCitaRepository(conn).bloque(disponibilidad);
            if (bloque == null) throw noDisponible();
            validarCambio(conn, disponibilidad, medico, perfil, bloque, estado);
            bloque.cambiarEstado(estado);
            return true;
        });
    }

    private void validarCambio(Connection conn, int id, int medico, Medico perfil, DisponibilidadEntidad bloque, String estado) throws SQLException {
        LocalDate fecha = bloque.fecha();
        LocalTime inicio = bloque.inicio();
        LocalTime fin = bloque.fin();
        if (PUBLICADO.equals(estado)) {
            if (perfil.sede() != bloque.sede()) throw invalida("La sede del turno no coincide con la del médico.");
            validarFutura(LocalDateTime.of(fecha, inicio));
            validarIntervalo(inicio, fin, bloque.minutos());
            sinSolapamiento(conn, medico, fecha, inicio, fin, id);
        } else if (new ReservaCitaRepository(conn).bloqueConCitas(id, LocalDateTime.now(clock))) {
            throw new AgendaException(409, "TURNO_CON_CITAS", "El turno tiene citas activas. Reprograma o cancela esas citas antes de retirarlo.");
        }
    }

    private <T> T transaccion(TransaccionJdbc.Operacion<T> operacion) {
        return TransaccionJdbc.ejecutar(connections, operacion);
    }

    static int paciente(Connection conn, int id) throws SQLException {
        if (id <= 0) throw invalida("El paciente no es válido.");
        var repositorio = new ReservaCitaRepository(conn);
        var perfil = repositorio.paciente(id);
        if (perfil == null || perfil.usuario() == null || !repositorio.usuarioActivo(perfil.usuario(), "PACIENTE")) {
            throw invalida("El paciente no está habilitado.");
        }
        return perfil.usuario();
    }

    static void bloquearMedico(Connection conn, int id) throws SQLException {
        if (new ReservaCitaRepository(conn).medico(id) == null) throw noDisponible();
    }

    static Medico medico(Connection conn, int id) throws SQLException {
        if (id <= 0) throw invalida("El médico no es válido.");
        var repositorio = new ReservaCitaRepository(conn);
        var perfil = repositorio.medico(id);
        if (perfil == null || !"ACTIVO".equals(perfil.estado()) || perfil.sede() == null
                || perfil.especialidad() == null || !repositorio.medicoActivo(id)) throw noDisponible();
        return new Medico(perfil.sede(), perfil.especialidad());
    }

    private static Bloque bloqueReservable(Connection conn, int medico, int sede, LocalDateTime inicio) throws SQLException {
        for (var turno : new ReservaCitaRepository(conn).bloques(medico, sede, inicio.toLocalDate())) {
            var bloque = new Bloque(turno.id(), turno.inicio(), turno.fin(), turno.minutos());
            if (contiene(bloque, inicio.toLocalTime())) return bloque;
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
        var repositorio = new DisponibilidadRepository(conn);
        var ahora = LocalDateTime.now(clock);
        for (var retencion : repositorio.retencionesMedico(medico, inicio.toLocalDate(), ahora)) {
            if (inicio.isBefore(retencion.inicio().plusMinutes(retencion.minutos()))
                    && retencion.inicio().isBefore(inicio.plusMinutes(minutos))) {
                throw new AgendaException(409,"HORARIO_RETENIDO","Otro paciente está confirmando ese horario. Selecciona otro cupo o inténtalo más tarde.");
            }
        }
        for (var ocupada : repositorio.ocupacionesReserva(medico, inicio.toLocalDate(), ahora, oferta)) {
            int duracion = ocupada.minutos();
            if (duracion <= 0 || duracion > 1440) throw noDisponible();
            if (inicio.isBefore(ocupada.inicio().plusMinutes(duracion)) && ocupada.inicio().isBefore(inicio.plusMinutes(minutos))) throw noDisponible();
        }
    }

    private void verificarAnterior(Connection conn, int cita, int paciente) throws SQLException {
        var anterior = new ReservaCitaRepository(conn).anterior(cita, paciente);
        if (anterior == null) throw new AgendaException(404, "CITA_NO_ENCONTRADA", "No se encontró una cita propia para reprogramar.");
        if (!anterior.inicio().isAfter(LocalDateTime.now(clock))) throw invalida("No se puede reprogramar una cita pasada.");
        if (!REPROGRAMABLES.contains(anterior.estado())) throw new AgendaException(409, "CITA_NO_REPROGRAMABLE", "El estado de la cita no permite reprogramarla.");
    }

    private static void verificarMotivo(Connection conn, Integer motivo, int especialidad) throws SQLException {
        if (motivo == null) return;
        if (motivo <= 0) throw invalida("El motivo no es válido.");
        if (!new ReservaCitaRepository(conn).motivoValido(motivo, especialidad)) {
            throw invalida("El motivo no pertenece a la especialidad del médico.");
        }
    }

    private int insertarCita(Connection conn, int paciente, int medico, int bloque, LocalDateTime inicio, Integer motivo, String urgencia) throws SQLException {
        var repositorio = new ReservaCitaRepository(conn);
        int id = repositorio.crearCita(paciente, medico, bloque, inicio, motivo, urgencia, riesgo(conn, paciente));
        LocalDateTime ahora = LocalDateTime.now(clock).withNano(0);
        Integer horas = horasAnticipacionConfirmacion == null ? repositorio.horasConfirmacion() : horasAnticipacionConfirmacion;
        if (horas == null) throw new AgendaException(503,"CONFIGURACION_NO_DISPONIBLE","No se pudo obtener la configuración de ofertas.");
        LocalDateTime vence = inicio.minusHours(horas);
        if (!vence.isAfter(ahora)) vence = inicio;
        repositorio.confirmar(id, ahora, vence);
        return id;
    }

    private static String riesgo(Connection conn, int paciente) throws SQLException {
        long canceladas = new ReservaCitaRepository(conn).canceladas(paciente);
        return canceladas >= 3 ? "ALTO" : canceladas > 0 ? "MEDIO" : "BAJO";
    }

    private void notificar(Connection conn, int usuario, int cita, String mensaje, boolean reservaInicial) throws SQLException {
        var repositorio = new ReservaCitaRepository(conn);
        if (correosActivos && reservaInicial) repositorio.correoReserva(cita, clock.instant());
        repositorio.notificar(usuario, cita, mensaje, LocalDateTime.now(clock));
    }

    private static int medicoDeBloque(Connection conn, int id) throws SQLException {
        Integer medico = new ReservaCitaRepository(conn).medicoDeBloque(id);
        if (medico == null) throw noDisponible();
        return medico;
    }

    private static void sinSolapamiento(Connection conn, int medico, LocalDate fecha, LocalTime inicio, LocalTime fin, int excluir) throws SQLException {
        if (new ReservaCitaRepository(conn).solapamiento(medico, fecha, inicio, fin, excluir)) {
            throw new AgendaException(409, "TURNOS_SUPERPUESTOS", "Ya existe un turno publicado que se cruza con ese horario.");
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

    private static void validarPrioridad(String value) {
        if (value == null || value.isBlank()) return;
        if (!Set.of("ALTA", "MEDIA", "BAJA").contains(value)) throw invalida("La urgencia no es válida.");
    }

    private static AgendaException invalida(String message) { return new AgendaException(400, "DATOS_INVALIDOS", message); }
    private static AgendaException noDisponible() { return new AgendaException(409, OCUPADO, "El horario no está publicado o ya no está disponible. Selecciona otro horario."); }
}
