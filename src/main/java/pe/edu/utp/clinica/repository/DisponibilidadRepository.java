package pe.edu.utp.clinica.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public final class DisponibilidadRepository {
    public record Agenda(int medico, LocalDate fecha) {}
    public record Turno(int medico, LocalDate fecha, LocalTime inicio, LocalTime fin, int minutos) {}
    public record Ocupacion(int medico, LocalDateTime inicio, int minutos) {}

    private static final String AGENDAS = cargarConsulta("agendas-semana.sql");
    private static final String TURNOS = cargarConsulta("turnos-publicados.sql");
    private static final String CITAS = cargarConsulta("citas-ocupadas.sql");
    private static final String OFERTAS = cargarConsulta("ofertas-vigentes.sql");
    private static final String RETENCIONES = cargarConsulta("retenciones-vigentes.sql");

    private static final RowMapper<Ocupacion> OCUPACION = (fila, numero) -> new Ocupacion(
            fila.getInt("id_medico"), fila.getObject("inicio", LocalDateTime.class),
            fila.getInt("duracion_minutos"));

    private final NamedParameterJdbcTemplate jdbc;

    public DisponibilidadRepository(Connection conexion) {
        jdbc = new NamedParameterJdbcTemplate(new SingleConnectionDataSource(conexion, true));
    }

    public List<Agenda> agendasSemana(int especialidad, Integer sede, Integer medico,
            LocalDate desde, LocalDate hoy) throws SQLException {
        var parametros = rango(sede == null ? 0 : sede, desde, desde.plusDays(7))
                .addValue("especialidad", especialidad)
                .addValue("medico", medico == null ? 0 : medico)
                .addValue("hoy", hoy);
        return consultar(AGENDAS, parametros, (fila, numero) -> new Agenda(
                fila.getInt("id_medico"), fila.getObject("fecha", LocalDate.class)));
    }

    public List<Turno> turnosPublicados(int sede, LocalDate desde, LocalDate hasta) throws SQLException {
        return consultar(TURNOS, rango(sede, desde, hasta), (fila, numero) -> new Turno(
                fila.getInt("id_medico"), fila.getObject("fecha", LocalDate.class),
                fila.getObject("hora_inicio", LocalTime.class), fila.getObject("hora_fin", LocalTime.class),
                fila.getInt("duracion_minutos")));
    }

    public List<Ocupacion> citasOcupadas(int sede, LocalDate desde, LocalDate hasta) throws SQLException {
        return consultar(CITAS, rangoHoras(sede, desde, hasta), OCUPACION);
    }

    public List<Ocupacion> ofertasVigentes(int sede, LocalDate desde, LocalDate hasta,
            LocalDateTime ahora) throws SQLException {
        return consultar(OFERTAS, rangoHoras(sede, desde, hasta).addValue("ahora", ahora), OCUPACION);
    }

    public List<Ocupacion> retencionesVigentes(int sede, LocalDate desde, LocalDate hasta,
            LocalDateTime ahora) throws SQLException {
        return consultar(RETENCIONES, rangoHoras(sede, desde, hasta).addValue("ahora", ahora), OCUPACION);
    }

    private static MapSqlParameterSource rango(int sede, LocalDate desde, LocalDate hasta) {
        return new MapSqlParameterSource("sede", sede).addValue("desde", desde).addValue("hasta", hasta);
    }

    private static MapSqlParameterSource rangoHoras(int sede, LocalDate desde, LocalDate hasta) {
        return new MapSqlParameterSource("sede", sede)
                .addValue("desde", desde.atStartOfDay()).addValue("hasta", hasta.atStartOfDay());
    }

    private <T> List<T> consultar(String consulta, MapSqlParameterSource parametros,
            RowMapper<T> mapeador) throws SQLException {
        try {
            return jdbc.query(consulta, parametros, mapeador);
        } catch (DataAccessException error) {
            throw new SQLException("No fue posible consultar la disponibilidad.", error);
        }
    }

    private static String cargarConsulta(String archivo) {
        try {
            return new ClassPathResource("sql/disponibilidad/" + archivo)
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
    }
}
