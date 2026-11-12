package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.reporte.FiltroReporte;

import java.sql.*;
import java.time.*;
import java.util.*;

public class ReporteAdmisionDAO {
    private static final String INDICADORES = "indicadores";
    private final AgendaCitaDAO.ConnectionSource conexiones;
    private final Clock clock;

    public record Cita(
            int id,
            LocalDateTime fecha,
            String paciente,
            String documento,
            String correo,
            String telefono,
            String medico,
            String especialidad,
            String estado,
            String riesgo,
            String confirmacion) {}

    public record Resumen(
            long total,
            long atendidas,
            long enAtencion,
            long pendientes,
            long canceladas,
            long bajo,
            long medio,
            long alto,
            long sinRiesgo,
            Double esperaMedia,
            long medicos,
            long ofertas,
            long aceptadas) {}

    public record Ocupacion(long capacidad, long ocupados, long ofertasPendientes) {}

    public record Reporte(
            String sede,
            FiltroReporte filtro,
            Resumen resumen,
            List<Cita> citas,
            int pagina,
            int paginas,
            LocalDateTime actualizado,
            Ocupacion ocupacion) {
        public Reporte(
                String sede,
                FiltroReporte filtro,
                Resumen resumen,
                List<Cita> citas,
                int pagina,
                int paginas,
                LocalDateTime actualizado) {
            this(
                    sede,
                    filtro,
                    resumen,
                    citas,
                    pagina,
                    paginas,
                    actualizado,
                    new Ocupacion(0, 0, 0));
        }
    }

    public ReporteAdmisionDAO() {
        this(DatabaseConfig::getConnection, Clock.system(ZoneId.of("America/Lima")));
    }

    ReporteAdmisionDAO(AgendaCitaDAO.ConnectionSource conexiones, Clock clock) {
        this.conexiones = conexiones;
        this.clock = clock.withZone(ZoneId.of("America/Lima"));
    }

    public Reporte consultar(int usuario, FiltroReporte filtro, int pagina, boolean exportar) {
        if (pagina < 1 || pagina > 100000)
            throw new AgendaException(400, "PAGINA_INVALIDA", "Selecciona una página válida.");
        try (Connection c = conexiones.open()) {
            return consultarEnTransaccion(c, usuario, filtro, pagina, exportar);
        } catch (SQLException e) {
            throw new AgendaException(
                    503,
                    "REPORTE_NO_DISPONIBLE",
                    "No se pudo consultar el reporte. Inténtalo nuevamente.");
        }
    }

    private Reporte consultarEnTransaccion(
            Connection c, int usuario, FiltroReporte filtro, int pagina, boolean exportar)
            throws SQLException {
        c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        return pe.edu.utp.clinica.persistence.PersistenciaJpa.transaccion(
                c,
                () -> {
                    int sede = AmbitoAdmisionDAO.sede(c, usuario);
                    var repo = new pe.edu.utp.clinica.repository.ReporteAdmisionRepository(c);
                    LocalDateTime ahora = LocalDateTime.now(clock).withNano(0);
                    Resumen resumen = repo.resumen(sede, filtro, ahora);
                    if (exportar && !INDICADORES.equals(filtro.tipo()) && resumen.total() > 2000)
                        throw new AgendaException(
                                422,
                                "REPORTE_DEMASIADO_GRANDE",
                                "El reporte supera 2000 citas. Reduce el período o filtra por"
                                    + " estado.");
                    int paginas = Math.max(1, (int) ((resumen.total() + 19) / 20));
                    int actual = Math.min(pagina, paginas);
                    List<Cita> citas =
                            INDICADORES.equals(filtro.tipo())
                                    ? List.of()
                                    : repo.citas(
                                            sede,
                                            filtro,
                                            ahora,
                                            exportar ? 2000 : 20,
                                            exportar ? 0 : (actual - 1) * 20);
                    return new Reporte(
                            repo.nombreSede(sede),
                            filtro,
                            resumen,
                            List.copyOf(citas),
                            actual,
                            paginas,
                            ahora,
                            repo.ocupacion(sede, filtro, ahora));
                });
    }
}
