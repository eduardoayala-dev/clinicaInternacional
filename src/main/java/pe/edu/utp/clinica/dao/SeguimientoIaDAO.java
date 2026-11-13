package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.auth.SesionesJdbc;
import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.ia.EntradaInasistencia;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public final class SeguimientoIaDAO {
    public record Instantanea(
            int cita,
            LocalDateTime evaluadaEn,
            LocalDateTime reservadaEn,
            EntradaInasistencia entrada,
            boolean plazoVencido) {}

    private final SesionesJdbc.Conexion conexiones;
    private final Clock reloj;

    public SeguimientoIaDAO() {
        this(DatabaseConfig::getConnection, Clock.system(ZoneId.of("America/Lima")));
    }

    public SeguimientoIaDAO(SesionesJdbc.Conexion conexiones, Clock reloj) {
        this.conexiones = conexiones;
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    public Instantanea consultar(int actor, int cita) {
        if (actor <= 0 || cita <= 0) throw ausente();
        try (var c = conexiones.abrir()) {
            var datos =
                    new pe.edu.utp.clinica.repository.SeguimientoIaRepository(c)
                            .consultar(actor, cita);
            if (datos == null) throw ausente();
            var ahora = LocalDateTime.now(reloj);
            var fecha = datos.fecha();
            if (!fecha.isAfter(ahora)
                    || !("SOLICITADA".equals(datos.estado())
                            || "CONFIRMADA".equals(datos.estado())))
                throw new AgendaException(
                        409,
                        "CITA_NO_EVALUABLE",
                        "Solo se evalúan citas futuras pendientes de atención.");
            var reserva = datos.creada();
            var vence = datos.vence();
            var confirmada = datos.confirmada();
            if (reserva == null
                    || vence == null
                    || reserva.isAfter(ahora)
                    || reserva.isAfter(fecha)
                    || (confirmada != null
                            && (confirmada.isAfter(ahora) || confirmada.isBefore(reserva))))
                throw incompleta();
            long dias = ChronoUnit.DAYS.between(reserva.toLocalDate(), fecha.toLocalDate());
            if (dias > 180 || (int) datos.faltas() > 100 || (int) datos.atenciones() > 100)
                throw incompleta();
            return new Instantanea(
                    cita,
                    ahora,
                    reserva,
                    new EntradaInasistencia(
                            (int) datos.faltas(),
                            (int) datos.atenciones(),
                            (int) dias,
                            confirmada != null),
                    confirmada == null && !ahora.isBefore(vence));
        } catch (SQLException e) {
            throw new AgendaException(
                    503,
                    "SEGUIMIENTO_IA_NO_DISPONIBLE",
                    "No se pudo reconstruir el historial de la cita.");
        }
    }

    private static AgendaException ausente() {
        return new AgendaException(
                404, "CITA_NO_ENCONTRADA", "No se encontró una cita accesible en tu sede.");
    }

    private static AgendaException incompleta() {
        return new AgendaException(
                409,
                "DATOS_IA_INSUFICIENTES",
                "La cita no tiene una instantánea válida dentro de los límites del modelo. Continúa"
                    + " el seguimiento habitual.");
    }
}
