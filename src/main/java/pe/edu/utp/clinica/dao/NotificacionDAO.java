package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class NotificacionDAO {
    private static final String RECORDATORIO = "Recordatorio de cita";
    private static final Set<String> TIPOS_ASISTENCIA =
            Set.of(RECORDATORIO, "Recordatorio", "Citas", "Reserva registrada");
    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("es"));
    private final AgendaCitaDAO.ConnectionSource connections;
    private final Clock clock;

    public NotificacionDAO() {
        this(DatabaseConfig::getConnection, Clock.system(ZoneId.of("America/Lima")));
    }

    NotificacionDAO(AgendaCitaDAO.ConnectionSource connections, Clock clock) {
        this.connections = connections;
        this.clock = clock;
    }

    public boolean crearNotificacion(int usuario, Integer cita, String tipo, String mensaje) {
        if (usuario <= 0 || mensaje == null || mensaje.isBlank()) return false;
        try (var conn = connections.open()) {
            return pe.edu.utp.clinica.persistence.PersistenciaJpa.transaccion(
                    conn,
                    () -> {
                        new pe.edu.utp.clinica.repository.NotificacionRepository(conn)
                                .crear(
                                        usuario,
                                        cita,
                                        tipo == null ? "Aviso del Sistema" : tipo,
                                        mensaje,
                                        LocalDateTime.now(clock));
                        return true;
                    });
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }

    public List<Map<String, Object>> obtenerNotificacionesUsuario(int usuario) {
        if (usuario <= 0) return List.of();
        try (var conn = connections.open()) {
            var resultado = new ArrayList<Map<String, Object>>();
            var ahora = LocalDateTime.now(clock);
            for (var aviso :
                    new pe.edu.utp.clinica.repository.NotificacionRepository(conn).listar(usuario))
                resultado.add(presentar(aviso, ahora));
            return resultado;
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }

    private static Map<String, Object> presentar(
            pe.edu.utp.clinica.repository.NotificacionRepository.Aviso rs, LocalDateTime ahora) {
        Map<String, Object> data = new LinkedHashMap<>();
        String tipo = rs.tipo();
        String mensaje = rs.mensaje();
        LocalDateTime fecha = rs.enviada();
        boolean leida = rs.leida();
        String categoria = categoria(tipo, mensaje);
        String dia = fecha == null ? "Sin fecha" : fecha.format(FECHA);
        String hora =
                fecha == null
                        ? "—"
                        : fecha.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        data.put("idNotificacion", rs.id());
        data.put("idCita", rs.cita());
        data.put("mensaje", mensaje);
        data.put("detalle", mensaje);
        data.put("fecha", dia);
        data.put("hora", hora);
        data.put("fechaEnvio", dia + " · " + hora);
        data.put("leida", leida);
        data.put("estado", leida ? "Leído" : "Pendiente");
        data.put("tipo", tipo);
        data.put("categoria", categoria);
        asistencia(rs, data, tipo, ahora);
        return data;
    }

    private static void asistencia(
            pe.edu.utp.clinica.repository.NotificacionRepository.Aviso rs,
            Map<String, Object> data,
            String tipo,
            LocalDateTime ahora) {
        LocalDateTime fecha = rs.fechaCita();
        LocalDateTime vence = rs.vence();
        boolean confirmada = rs.confirmada() != null;
        String estadoCita = rs.estadoCita();
        boolean vigente =
                fecha != null
                        && fecha.isAfter(ahora)
                        && ("SOLICITADA".equals(estadoCita) || "CONFIRMADA".equals(estadoCita));
        boolean vencida = vence != null && !ahora.isBefore(vence);
        boolean aplicable = tipo != null && TIPOS_ASISTENCIA.contains(tipo);
        boolean confirmable = aplicable && vigente && vence != null && !vencida && !confirmada;
        data.put("accion", confirmable ? "Confirmar" : "Ver");
        data.put("asistenciaConfirmada", aplicable && confirmada);
        data.put(
                "estadoAsistencia",
                aplicable ? estadoAsistencia(vigente, confirmada, vencida) : null);
        data.put(
                "venceConfirmacion",
                confirmable ? pe.edu.utp.clinica.util.FormatoClinico.fechaHora(vence) : null);
    }

    private static String estadoAsistencia(boolean vigente, boolean confirmada, boolean vencida) {
        if (!vigente) return "Cita no disponible para confirmar";
        if (confirmada) return "Asistencia confirmada";
        return vencida ? "Plazo de confirmación vencido" : "Asistencia pendiente";
    }

    private static String categoria(String tipo, String mensaje) {
        String clase = tipo == null ? "" : tipo.toUpperCase(Locale.ROOT);
        String texto = mensaje == null ? "" : mensaje.toUpperCase(Locale.ROOT);
        if (clase.startsWith("RECORDATORIO")) return RECORDATORIO;
        if (clase.contains("ESPERA")) return "Lista de espera";
        if (clase.contains("RESERVA")
                || texto.startsWith("CITA CONFIRMADA")
                || texto.startsWith("CITA AGENDADA")) return "Reserva registrada";
        return "Aviso del Sistema";
    }

    public int contarNotificacionesSinLeer(int usuario) {
        if (usuario <= 0) return 0;
        try (var conn = connections.open()) {
            return new pe.edu.utp.clinica.repository.NotificacionRepository(conn)
                    .pendientes(usuario);
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }

    public boolean marcarNotificacionesComoLeidas(int usuario) {
        if (usuario <= 0) return false;
        try (var conn = connections.open()) {
            pe.edu.utp.clinica.persistence.PersistenciaJpa.transaccion(
                    conn,
                    () ->
                            new pe.edu.utp.clinica.repository.NotificacionRepository(conn)
                                    .marcar(null, usuario));
            return true;
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }

    public boolean marcarNotificacionIndividualLeida(int id, int usuario) {
        if (id <= 0 || usuario <= 0) return false;
        try (var conn = connections.open()) {
            return pe.edu.utp.clinica.persistence.PersistenciaJpa.transaccion(
                            conn,
                            () ->
                                    new pe.edu.utp.clinica.repository.NotificacionRepository(conn)
                                            .marcar(id, usuario))
                    == 1;
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }

    public boolean confirmarAsistenciaDesdeNotificacion(int id, int usuario) {
        if (id <= 0 || usuario <= 0) return false;
        return TransaccionJdbc.ejecutar(
                connections,
                conn -> {
                    var repo = new pe.edu.utp.clinica.repository.NotificacionRepository(conn);
                    var propia = repo.propia(id, usuario);
                    if (propia == null
                            || propia.tipo() == null
                            || !TIPOS_ASISTENCIA.contains(propia.tipo())) return false;
                    new ConfirmacionAsistenciaDAO(connections, clock)
                            .confirmarPaciente(conn, usuario, propia.cita());
                    if (repo.marcar(id, usuario) != 1)
                        throw new AgendaException(
                                404,
                                "NOTIFICACION_NO_ENCONTRADA",
                                "No se encontró una notificación propia.");
                    return true;
                });
    }
}
