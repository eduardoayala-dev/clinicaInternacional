package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.model.medico.PropuestaHorario;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

public final class PropuestaHorarioDAO {
    private final AgendaCitaDAO.ConnectionSource connections;
    private final Clock clock;
    private final AgendaCitaDAO agenda;

    public PropuestaHorarioDAO() {
        this(DatabaseConfig::getConnection, Clock.system(ZoneId.of("America/Lima")));
    }

    PropuestaHorarioDAO(AgendaCitaDAO.ConnectionSource connections, Clock clock) {
        this.connections = connections;
        this.clock = clock.withZone(ZoneId.of("America/Lima"));
        agenda = new AgendaCitaDAO(connections, this.clock);
    }

    public int proponer(
            int usuario,
            int dias,
            LocalDate desde,
            LocalDate hasta,
            LocalTime inicio,
            LocalTime fin,
            int minutos) {
        validar(dias, desde, hasta, inicio, fin, minutos);
        return TransaccionJdbc.ejecutar(
                connections,
                conn -> {
                    int medico = medicoUsuario(conn, usuario);
                    int sede = AgendaCitaDAO.medico(conn, medico).sede();
                    return new pe.edu.utp.clinica.repository.PropuestaHorarioRepository(conn)
                            .crear(
                                    medico,
                                    sede,
                                    dias,
                                    desde,
                                    hasta,
                                    inicio,
                                    fin,
                                    minutos,
                                    ahoraUtc());
                });
    }

    public List<PropuestaHorario> delMedico(int usuario) {
        return TransaccionJdbc.ejecutar(
                connections,
                conn ->
                        List.copyOf(
                                new pe.edu.utp.clinica.repository.PropuestaHorarioRepository(conn)
                                        .listar(medicoUsuario(conn, usuario), null, null)));
    }

    public List<PropuestaHorario> paraAdmision(int usuario) {
        return TransaccionJdbc.ejecutar(
                connections,
                conn ->
                        List.copyOf(
                                new pe.edu.utp.clinica.repository.PropuestaHorarioRepository(conn)
                                        .listar(null, sedePersonal(conn, usuario), null)));
    }

    public int revisar(int usuario, int id, boolean aprobar, String motivo) {
        if (id <= 0) throw invalida("Selecciona una propuesta válida.");
        String razon = motivo == null ? "" : motivo.strip();
        if (razon.length() > 500 || (!aprobar && razon.length() < 5))
            throw invalida("Para rechazar, explica el motivo (entre 5 y 500 caracteres).");
        return TransaccionJdbc.ejecutar(
                connections,
                conn -> {
                    int sede = sedePersonal(conn, usuario);
                    PropuestaHorario propuesta = bloquear(conn, id, sede);
                    if (!"PENDIENTE".equals(propuesta.estado()))
                        throw new AgendaException(
                                409, "PROPUESTA_REVISADA", "Esta propuesta ya fue revisada.");
                    int bloques = aprobar ? publicar(conn, propuesta) : 0;
                    new pe.edu.utp.clinica.repository.PropuestaHorarioRepository(conn)
                            .revisar(id, usuario, aprobar, razon, ahoraUtc());
                    return bloques;
                });
    }

    private int publicar(Connection conn, PropuestaHorario p) throws SQLException {
        if (AgendaCitaDAO.medico(conn, p.medico()).sede() != p.sede())
            throw invalida("La sede del médico cambió. Solicita una nueva propuesta.");
        int publicados = 0;
        for (LocalDate fecha = p.desde(); !fecha.isAfter(p.hasta()); fecha = fecha.plusDays(1)) {
            if (p.incluye(fecha)
                    && LocalDateTime.of(fecha, p.inicio()).isAfter(LocalDateTime.now(clock))) {
                int bloque =
                        agenda.publicarEnConexion(
                                conn,
                                p.medico(),
                                p.sede(),
                                LocalDateTime.of(fecha, p.inicio()),
                                p.fin(),
                                p.minutos());
                new pe.edu.utp.clinica.repository.PropuestaHorarioRepository(conn)
                        .vincular(bloque, p.id());
                publicados++;
            }
        }
        if (publicados == 0)
            throw invalida("La propuesta ya no tiene turnos futuros. Solicita una nueva vigencia.");
        return publicados;
    }

    private void validar(
            int dias,
            LocalDate desde,
            LocalDate hasta,
            LocalTime inicio,
            LocalTime fin,
            int minutos) {
        if (dias < 1
                || dias > 127
                || desde == null
                || hasta == null
                || inicio == null
                || fin == null) throw invalida("Completa los días, las fechas y las horas.");
        validarFechas(desde, hasta);
        validarHoras(inicio, fin, minutos);
        boolean futuro =
                desde.datesUntil(hasta.plusDays(1))
                        .anyMatch(
                                f ->
                                        (dias & (1 << (f.getDayOfWeek().getValue() - 1))) != 0
                                                && LocalDateTime.of(f, inicio)
                                                        .isAfter(LocalDateTime.now(clock)));
        if (!futuro)
            throw invalida("No hay un día y hora futuros dentro de la vigencia seleccionada.");
    }

    private void validarFechas(LocalDate desde, LocalDate hasta) {
        if (desde.getYear() > 9999
                || hasta.getYear() > 9999
                || desde.isBefore(LocalDate.now(clock))
                || hasta.isBefore(desde)
                || hasta.isAfter(desde.plusDays(89)))
            throw invalida(
                    "La vigencia debe comenzar desde hoy y abarcar como máximo 90 días, incluyendo"
                        + " ambas fechas.");
    }

    private static void validarHoras(LocalTime inicio, LocalTime fin, int minutos) {
        long segundos = fin.toSecondOfDay() - (long) inicio.toSecondOfDay();
        if (inicio.getSecond() != 0
                || fin.getSecond() != 0
                || inicio.getNano() != 0
                || fin.getNano() != 0
                || minutos < 5
                || minutos > 240
                || segundos < minutos * 60L
                || segundos % (minutos * 60L) != 0)
            throw invalida(
                    "El turno debe contener citas completas de entre 5 y 240 minutos y terminar el"
                        + " mismo día.");
    }

    private static int medicoUsuario(Connection conn, int usuario) throws SQLException {
        Integer medico =
                new pe.edu.utp.clinica.repository.PropuestaHorarioRepository(conn)
                        .medicoUsuario(usuario);
        if (medico == null)
            throw new AgendaException(
                    403, "MEDICO_NO_HABILITADO", "Tu perfil médico no está habilitado.");
        return medico;
    }

    private static int sedePersonal(Connection conn, int usuario) throws SQLException {
        var sede = new pe.edu.utp.clinica.repository.AmbitoAdmisionRepository(conn).sede(usuario);
        if (sede == null)
            throw new AgendaException(
                    403, "ADMISION_NO_HABILITADA", "Tu perfil de admisión no está habilitado.");
        return sede.id();
    }

    private static PropuestaHorario bloquear(Connection conn, int id, int sede)
            throws SQLException {
        var propuestas =
                new pe.edu.utp.clinica.repository.PropuestaHorarioRepository(conn)
                        .listar(null, sede, id);
        if (propuestas.isEmpty())
            throw new AgendaException(
                    404,
                    "PROPUESTA_NO_ENCONTRADA",
                    "La propuesta no está disponible para tu sede.");
        return propuestas.get(0);
    }

    private LocalDateTime ahoraUtc() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private static AgendaException invalida(String mensaje) {
        return new AgendaException(400, "PROPUESTA_INVALIDA", mensaje);
    }
}
