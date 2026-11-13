package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.persistence.FilaConsulta;
import pe.edu.utp.clinica.repository.ConsultaMedicaRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MedicoPortalDAO {
    private static final String SIN_REGISTRAR = "Sin registrar";
    private static final String EN_ATENCION = "EN_ATENCION";
    private static final String NOMBRES = "nombres";
    private static final String APELLIDOS = "apellidos";
    private static final String ATENDIDA = "ATENDIDA";
    private static final String MOTIVO = "motivo";
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final AgendaCitaDAO.ConnectionSource connections;
    private final Clock clock;

    public MedicoPortalDAO() {
        this(DatabaseConfig::getConnection, Clock.system(LIMA));
    }

    MedicoPortalDAO(AgendaCitaDAO.ConnectionSource connections, Clock clock) {
        this.connections = connections;
        this.clock = clock.withZone(LIMA);
    }

    public Map<String, Object> perfil(int usuario) {
        try (Connection conn = connections.open()) {
            var repo = new ConsultaMedicaRepository(conn);
            var rs = repo.perfil(usuario);
            if (rs == null) {
                if (repo.perfilPendiente(usuario))
                    throw new AgendaException(
                            409,
                            "PERFIL_MEDICO_PENDIENTE",
                            "Admisión debe completar tu perfil profesional antes de habilitar la"
                                + " agenda.");
                throw new AgendaException(
                        403, "MEDICO_NO_HABILITADO", "El perfil médico no está habilitado.");
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("idMedico", rs.entero("id_medico"));
            data.put("nombreMedico", texto(rs, NOMBRES) + " " + texto(rs, APELLIDOS));
            for (String campo : List.of("cmp", "email", "especialidad", "sedeNombre"))
                data.put(campo, texto(rs, campo));
            return data;
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }

    public List<Map<String, Object>> agenda(int medico, LocalDate fecha) {
        return agenda(medico, fecha, fecha == null ? null : fecha.plusDays(1));
    }

    public List<Map<String, Object>> agenda(int medico, LocalDate fecha, LocalDate hasta) {
        if (medico <= 0
                || fecha == null
                || hasta == null
                || !hasta.isAfter(fecha)
                || java.time.temporal.ChronoUnit.DAYS.between(fecha, hasta) > 31)
            throw new AgendaException(400, "DATOS_INVALIDOS", "Selecciona una fecha válida.");
        try (Connection conn = connections.open()) {
            var rows = new ArrayList<Map<String, Object>>();
            for (var rs : new ConsultaMedicaRepository(conn).citas(medico, fecha, hasta, null)) {
                var data = base(rs);
                data.put("paciente", texto(rs, NOMBRES) + " " + texto(rs, APELLIDOS));
                data.put("dni", texto(rs, "dni"));
                data.put(MOTIVO, texto(rs, MOTIVO));
                data.put("origen", rs.booleano("reasignada") ? "Lista de espera" : "Reserva");
                rows.add(data);
            }
            return rows;
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }

    public Map<String, Object> detalle(int medico, int cita) {
        if (medico <= 0 || cita <= 0) return Map.of();
        try (Connection conn = connections.open()) {
            var filas = new ConsultaMedicaRepository(conn).citas(medico, null, null, cita);
            return filas.isEmpty() ? Map.of() : presentar(filas.get(0));
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }

    public boolean guardar(
            int medico, int cita, String estado, String prioridad, String fecha, String notas) {
        if (!ATENDIDA.equals(estado))
            throw new AgendaException(
                    403,
                    "ACCION_NO_PERMITIDA",
                    "La inasistencia debe registrarse en admisión. Para un paciente que llegó,"
                        + " utiliza No atendida con un motivo.");
        return new AtencionCitaDAO(connections, clock)
                .finalizar(medico, cita, prioridad, fecha, notas);
    }

    public boolean iniciar(int medico, int cita) {
        return new AtencionCitaDAO(connections, clock).iniciar(medico, cita);
    }

    public boolean noAtender(int medico, int cita, String motivo) {
        return new AtencionCitaDAO(connections, clock).noAtender(medico, cita, motivo);
    }

    public int activa(int medico) {
        try (Connection conn = connections.open()) {
            return new ConsultaMedicaRepository(conn).activa(medico);
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }

    private Map<String, Object> base(FilaConsulta rs) throws SQLException {
        Map<String, Object> data = new LinkedHashMap<>();
        LocalDateTime inicio = rs.valor("fecha_hora", LocalDateTime.class);
        String estado = texto(rs, "estado");
        data.put("idCita", rs.entero("id_cita"));
        LocalDateTime llegada = rs.valor("fecha_llegada", LocalDateTime.class);
        data.put("fechaLlegada", llegada == null ? "" : llegada.toString());
        data.put(
                "horaLlegada",
                llegada == null
                        ? SIN_REGISTRAR
                        : llegada.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        data.put("llegadaRegistrada", llegada != null);
        presentarAtencion(rs, data, estado, inicio, llegada);
        data.put("llegoTarde", llegada != null && rs.booleano("llego_tarde"));
        data.put("toleranciaMinutos", llegada == null ? null : rs.entero("tolerancia_minutos"));
        data.put("fecha", inicio.toLocalDate().toString());
        data.put("hora", inicio.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        data.put("fechaFormateada", inicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        data.put("fechaHora", inicio.toString());
        data.put("estado", estado);
        data.put(
                "editable",
                EN_ATENCION.equals(estado)
                        && rs.valor("fecha_inicio") != null
                        && rs.valor("fecha_fin") == null);
        data.put(
                "estadoTexto",
                switch (estado) {
                    case EN_ATENCION -> "En atención";
                    case "NO_ATENDIDA" -> "No atendida";
                    case ATENDIDA -> "Atendida";
                    case "NO_ASISTIO" -> "No asistió";
                    case "EN_SALA" -> "En sala";
                    case "CANCELADA" -> "Cancelada";
                    case "REPROGRAMADA" -> "Reprogramada";
                    case "CONFIRMADA" -> "Confirmada";
                    case "SOLICITADA" -> "Solicitada";
                    default -> estado;
                });
        return data;
    }

    private void presentarAtencion(
            FilaConsulta rs,
            Map<String, Object> data,
            String estado,
            LocalDateTime programada,
            LocalDateTime llegada)
            throws SQLException {
        LocalDateTime comienzo = rs.valor("fecha_inicio", LocalDateTime.class);
        LocalDateTime fin = rs.valor("fecha_fin", LocalDateTime.class);
        LocalDateTime ahora = LocalDateTime.now(clock).withNano(0);
        boolean enSalaHoy =
                "EN_SALA".equals(estado)
                        && llegada != null
                        && !llegada.isAfter(ahora)
                        && programada.toLocalDate().equals(ahora.toLocalDate())
                        && llegada.toLocalDate().equals(ahora.toLocalDate());
        boolean ocupada = rs.booleano("consulta_ocupada");
        data.put("puedeIniciar", enSalaHoy && !ocupada);
        data.put("puedeNoAtender", enSalaHoy || EN_ATENCION.equals(estado));
        data.put("consultaOcupada", ocupada);
        data.put("fechaInicio", comienzo == null ? "" : comienzo.toString());
        data.put("fechaFin", fin == null ? "" : fin.toString());
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        data.put("inicioTexto", comienzo == null ? SIN_REGISTRAR : comienzo.format(formato));
        data.put("finTexto", fin == null ? SIN_REGISTRAR : fin.format(formato));
        data.put("motivoNoAtencion", texto(rs, "motivo_no_atencion"));
        data.put("esperaMinutos", minutosEntre(llegada, comienzo));
        data.put("duracionMinutos", minutosEntre(comienzo, fin));
    }

    private static Long minutosEntre(LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null || hasta == null) return null;
        return java.time.Duration.between(desde.atZone(LIMA), hasta.atZone(LIMA)).toMinutes();
    }

    private Map<String, Object> presentar(FilaConsulta rs) throws SQLException {
        Map<String, Object> data = base(rs);
        data.put(
                "consultaClinica",
                new pe.edu.utp.clinica.model.medico.ConsultaClinica(
                        texto(rs, "motivo_clinico"),
                        texto(rs, "antecedentes"),
                        texto(rs, "hallazgos"),
                        texto(rs, "evaluacion"),
                        texto(rs, "diagnostico"),
                        texto(rs, "plan")));
        data.put("idPaciente", rs.entero("id_paciente"));
        String nombre = texto(rs, NOMBRES) + " " + texto(rs, APELLIDOS);
        data.put("paciente", nombre);
        data.put("pacienteNombre", nombre);
        for (String campo :
                List.of(
                        "dni",
                        "telefono",
                        "email",
                        "direccion",
                        "alergias",
                        "especialidad",
                        "sede",
                        "cmp",
                        MOTIVO,
                        "indicaciones")) data.put(campo, texto(rs, campo));
        data.put("urgencia", texto(rs, "nivel_urgencia"));
        data.put("tipoCobertura", texto(rs, "tipo_cobertura"));
        data.put("medicoNombre", texto(rs, "medico_nombres") + " " + texto(rs, "medico_apellidos"));
        data.put("sedeYConsultorio", texto(rs, "sede"));
        data.put("canalReserva", texto(rs, "canal_aviso"));
        data.put("origen", rs.booleano("reasignada") ? "Lista de espera" : "Reserva");
        LocalDate proxima = rs.valor("fecha_recomendada_proxima", LocalDate.class);
        data.put("fechaRecomendadaProxima", proxima == null ? "" : proxima.toString());
        LocalDateTime registro = rs.valor("fecha_creacion", LocalDateTime.class);
        data.put(
                "fechaRegistro",
                registro == null
                        ? ""
                        : registro.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        LocalDate nacimiento = rs.valor("fecha_nacimiento", LocalDate.class);
        data.put(
                "edad",
                nacimiento == null
                        ? SIN_REGISTRAR
                        : Period.between(nacimiento, LocalDate.now(clock)).getYears() + " años");
        return data;
    }

    private static String texto(FilaConsulta rs, String campo) throws SQLException {
        return java.util.Objects.toString(rs.texto(campo), "");
    }
}
