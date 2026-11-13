package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.model.medico.ConsultaClinica;
import pe.edu.utp.clinica.repository.AtencionRepository;
import pe.edu.utp.clinica.repository.AtencionRepository.Cita;
import pe.edu.utp.clinica.service.ConsultaClinicaService;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

final class AtencionCitaDAO {
    private static final String EN_ATENCION = "EN_ATENCION";
    private final AgendaCitaDAO.ConnectionSource connections;
    private final Clock clock;

    AtencionCitaDAO(AgendaCitaDAO.ConnectionSource connections, Clock clock) {
        this.connections = connections;
        this.clock = clock.withZone(ZoneId.of("America/Lima"));
    }

    boolean iniciar(int medico, int cita) {
        return TransaccionJdbc.ejecutar(
                connections,
                conn -> {
                    Cita actual = bloquear(conn, medico, cita);
                    LocalDateTime ahora = ahora();
                    validarSala(actual, ahora);
                    var repo = new AtencionRepository(conn);
                    if (repo.ocupados(medico, actual.paciente()))
                        throw conflicto(
                                "El médico o el paciente ya tiene una consulta en curso. Finalízala"
                                    + " antes de iniciar otra.");
                    repo.registrar(cita, medico, actual.paciente(), ahora, null, null);
                    estado(conn, cita, medico, EN_ATENCION);
                    return true;
                });
    }

    boolean finalizar(int medico, int cita, String prioridad, String fecha, String notas) {
        return finalizar(medico, cita, prioridad, fecha, notas, null);
    }

    boolean finalizar(
            int medico,
            int cita,
            String prioridad,
            String fecha,
            String notas,
            ConsultaClinica consulta) {
        ConsultaClinica validada =
                consulta == null ? null : ConsultaClinicaService.validar(consulta);
        return TransaccionJdbc.ejecutar(
                connections,
                conn -> {
                    Cita actual = bloquear(conn, medico, cita);
                    LocalDateTime ahora = ahora();
                    if (!EN_ATENCION.equals(actual.estado()))
                        throw conflicto("Debes iniciar la atención antes de finalizarla.");
                    LocalDate seguimiento = validarSeguimiento(prioridad, fecha, notas);
                    cerrarRegistro(conn, medico, cita, ahora, null);
                    if (validada != null) guardarConsulta(conn, cita, validada);
                    if (new AtencionRepository(conn)
                                    .estado(cita, medico, "ATENDIDA", prioridad, seguimiento, true)
                            != 1) throw inexistente();
                    PrioridadClinicaJdbc.auditar(conn, medico, cita, prioridad);
                    new AtencionRepository(conn)
                            .historial(actual.paciente(), cita, notas.trim(), ahora);
                    return true;
                });
    }

    private static void guardarConsulta(Connection conn, int cita, ConsultaClinica consulta)
            throws SQLException {
        new AtencionRepository(conn).consulta(cita, consulta);
    }

    boolean noAtender(int medico, int cita, String motivo) {
        String razon = motivo(motivo);
        return TransaccionJdbc.ejecutar(
                connections,
                conn -> {
                    Cita actual = bloquear(conn, medico, cita);
                    LocalDateTime ahora = ahora();
                    if (EN_ATENCION.equals(actual.estado()))
                        cerrarRegistro(conn, medico, cita, ahora, razon);
                    else {
                        validarSala(actual, ahora);
                        new AtencionRepository(conn)
                                .registrar(cita, medico, actual.paciente(), null, ahora, razon);
                    }
                    estado(conn, cita, medico, "NO_ATENDIDA");
                    return true;
                });
    }

    private Cita bloquear(Connection conn, int medico, int cita) throws SQLException {
        if (medico <= 0 || cita <= 0) throw inexistente();
        var repo = new AtencionRepository(conn);
        Integer paciente = repo.paciente(cita, medico);
        if (paciente == null
                || new pe.edu.utp.clinica.repository.ReservaCitaRepository(conn).paciente(paciente)
                        == null) throw inexistente();
        if (!repo.medicoHabilitado(medico))
            throw new AgendaException(
                    403, "MEDICO_NO_HABILITADO", "El perfil médico no está habilitado.");
        AgendaCitaDAO.medico(conn, medico);
        var actual = repo.bloquear(cita, medico, paciente);
        if (actual == null) throw inexistente();
        return actual;
    }

    private static void validarSala(Cita cita, LocalDateTime ahora) {
        if (!"EN_SALA".equals(cita.estado())
                || cita.programada() == null
                || !cita.programada().toLocalDate().equals(ahora.toLocalDate())
                || cita.llegada() == null
                || cita.llegada().isAfter(ahora)
                || !cita.llegada().toLocalDate().equals(ahora.toLocalDate())) {
            throw conflicto("La cita debe ser de hoy y tener una llegada registrada por admisión.");
        }
    }

    private static void cerrarRegistro(
            Connection conn, int medico, int cita, LocalDateTime ahora, String motivo)
            throws SQLException {
        if (new AtencionRepository(conn).cerrar(cita, medico, ahora, motivo) != 1)
            throw conflicto("No se encontró una consulta activa para cerrar.");
    }

    private static void estado(Connection conn, int cita, int medico, String estado)
            throws SQLException {
        if (new AtencionRepository(conn).estado(cita, medico, estado, null, null, false) != 1)
            throw inexistente();
    }

    private LocalDate validarSeguimiento(String prioridad, String fecha, String notas) {
        if (!Set.of("ALTA", "MEDIA", "BAJA").contains(prioridad == null ? "" : prioridad)
                || notas == null
                || notas.isBlank()
                || notas.length() > 200
                || controles(notas)) {
            throw invalida(
                    "Selecciona la prioridad y registra una indicación de atención de hasta 200"
                        + " caracteres.");
        }
        if (fecha == null || fecha.isBlank()) return null;
        try {
            if (!fecha.matches("\\d{4}-\\d{2}-\\d{2}"))
                throw invalida("Selecciona una fecha futura válida para el seguimiento.");
            LocalDate proxima = LocalDate.parse(fecha);
            if (!proxima.isAfter(LocalDate.now(clock)))
                throw invalida("El seguimiento debe ser posterior a hoy.");
            return proxima;
        } catch (java.time.DateTimeException e) {
            throw invalida("Selecciona una fecha futura válida para el seguimiento.");
        }
    }

    static String motivo(String valor) {
        if (valor == null
                || valor.trim().length() < 3
                || valor.length() > 200
                || controles(valor)) {
            throw invalida("Registra un motivo de entre 3 y 200 caracteres.");
        }
        return valor.trim();
    }

    private static boolean controles(String valor) {
        return valor.chars()
                .anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t');
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock).withNano(0);
    }

    private static AgendaException conflicto(String mensaje) {
        return new AgendaException(409, "ATENCION_NO_DISPONIBLE", mensaje);
    }

    private static AgendaException invalida(String mensaje) {
        return new AgendaException(400, "DATOS_INVALIDOS", mensaje);
    }

    private static AgendaException inexistente() {
        return new AgendaException(
                404, "CITA_NO_ENCONTRADA", "No se encontró una cita asignada a este médico.");
    }
}
