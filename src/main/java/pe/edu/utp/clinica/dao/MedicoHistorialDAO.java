package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.model.medico.*;
import pe.edu.utp.clinica.persistence.FilaConsulta;
import pe.edu.utp.clinica.repository.HistorialClinicoRepository;

import java.sql.SQLException;
import java.time.*;
import java.util.*;

public final class MedicoHistorialDAO implements HistorialMedicoRepository {
    private static final int TAMANO = 20;
    private static final Set<String> CONTROLES =
            Set.of("SOLICITADA", "CONFIRMADA", "EN_SALA", "EN_ATENCION", "ATENDIDA");
    private final AgendaCitaDAO.ConnectionSource conexiones;
    private final Clock reloj;

    public MedicoHistorialDAO() {
        this(DatabaseConfig::getConnection, Clock.system(ZoneId.of("America/Lima")));
    }

    MedicoHistorialDAO(AgendaCitaDAO.ConnectionSource conexiones, Clock reloj) {
        this.conexiones = conexiones;
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    public Pagina<PacienteResumen> pacientes(int medico, String consulta, int pagina) {
        String filtro = filtro(consulta, pagina);
        try (var c = conexiones.open()) {
            return pagina(
                    new HistorialClinicoRepository(c)
                            .pacientes(medico, filtro, null, pagina).stream()
                                    .map(MedicoHistorialDAO::paciente)
                                    .toList(),
                    pagina);
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }

    private PacienteResumen paciente(int medico, int paciente) {
        try (var c = conexiones.open()) {
            var pacientes = new HistorialClinicoRepository(c).pacientes(medico, null, paciente, 1);
            if (pacientes.isEmpty()) throw inexistente();
            return paciente(pacientes.get(0));
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }

    public HistorialPaciente historial(int medico, int paciente, int pagina) {
        filtro("", pagina);
        var resumen = paciente(medico, paciente);
        try (var c = conexiones.open()) {
            return new HistorialPaciente(
                    resumen,
                    pagina(
                            new HistorialClinicoRepository(c).atenciones(medico, paciente, pagina),
                            pagina));
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }

    public Pagina<SeguimientoResumen> seguimientos(
            int medico, String consulta, String estado, int pagina) {
        String filtro = filtro(consulta, pagina);
        if (estado == null
                || !Set.of("", "PENDIENTE", "VENCIDO", "RESERVADO", "REALIZADO").contains(estado))
            throw invalida("Selecciona un estado de seguimiento válido.");
        try (var c = conexiones.open()) {
            return pagina(
                    new HistorialClinicoRepository(c)
                            .seguimientos(
                                    medico, filtro, estado, null, pagina, LocalDate.now(reloj)),
                    pagina);
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }

    public SeguimientoResumen seguimiento(int medico, int origen) {
        try (var c = conexiones.open()) {
            var filas =
                    new HistorialClinicoRepository(c)
                            .seguimientos(medico, null, null, origen, 1, LocalDate.now(reloj));
            if (filas.isEmpty()) throw inexistente();
            return filas.get(0);
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }

    public List<CitaControl> controles(int medico, int origen) {
        var seguimiento = seguimiento(medico, origen);
        try (var c = conexiones.open()) {
            return List.copyOf(
                    new HistorialClinicoRepository(c)
                            .controles(medico, seguimiento.idPaciente(), origen));
        } catch (SQLException e) {
            throw new CatalogoDatosException(e);
        }
    }

    public void vincular(int medico, int origen, int control) {
        TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    Integer paciente =
                            new pe.edu.utp.clinica.repository.AtencionRepository(c)
                                    .paciente(origen, medico);
                    if (paciente == null
                            || new pe.edu.utp.clinica.repository.ReservaCitaRepository(c)
                                            .paciente(paciente)
                                    == null) throw inexistente();
                    AgendaCitaDAO.medico(c, medico);
                    var repo = new HistorialClinicoRepository(c);
                    var datos = repo.bloquearVinculo(medico, paciente, origen, control);
                    if (datos == null) throw inexistente();
                    if (!CONTROLES.contains(datos.estadoControl()))
                        throw conflicto("La cita de control debe estar vigente o atendida.");
                    var previo = repo.existente(origen);
                    if (previo != null
                            && previo.control() != control
                            && CONTROLES.contains(Objects.toString(previo.estado(), "")))
                        throw conflicto(
                                "Ya existe una reserva vigente vinculada a este seguimiento.");
                    repo.vincular(
                            origen, control, datos.usuario(), LocalDateTime.now(reloj).withNano(0));
                    return null;
                });
    }

    private static <T> Pagina<T> pagina(List<T> filas, int pagina) {
        return new Pagina<>(
                filas.subList(0, Math.min(filas.size(), TAMANO)),
                pagina,
                TAMANO,
                filas.size() > TAMANO);
    }

    private static String filtro(String consulta, int pagina) {
        String texto = consulta == null ? "" : consulta.strip();
        if (pagina < 1
                || pagina > 10000
                || texto.length() > 100
                || texto.chars().anyMatch(Character::isISOControl))
            throw invalida("Revisa la búsqueda o la página solicitada.");
        return "%"
                + texto.toLowerCase(Locale.ROOT)
                        .replace("!", "!!")
                        .replace("%", "!%")
                        .replace("_", "!_")
                + "%";
    }

    private static PacienteResumen paciente(FilaConsulta f) {
        return new PacienteResumen(
                f.entero("id_paciente"),
                Objects.toString(f.texto("nombres"), "")
                        + " "
                        + Objects.toString(f.texto("apellidos"), ""),
                Objects.toString(f.texto("dni"), ""),
                Objects.toString(f.texto("alergias"), ""),
                f.valor("fecha_nacimiento", LocalDate.class),
                f.valor("ultima", LocalDateTime.class),
                f.entero("atenciones"));
    }

    private static AgendaException inexistente() {
        return new AgendaException(
                404,
                "REGISTRO_NO_ENCONTRADO",
                "No se encontró un registro autorizado para este médico.");
    }

    private static AgendaException invalida(String mensaje) {
        return new AgendaException(400, "FILTRO_INVALIDO", mensaje);
    }

    private static AgendaException conflicto(String mensaje) {
        return new AgendaException(409, "SEGUIMIENTO_NO_DISPONIBLE", mensaje);
    }
}
