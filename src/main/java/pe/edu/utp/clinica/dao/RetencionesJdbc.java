package pe.edu.utp.clinica.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

final class RetencionesJdbc {
    private RetencionesJdbc() {}

    static void comprobarLibre(
            Connection conn, int medico, LocalDateTime inicio, int minutos, LocalDateTime ahora)
            throws SQLException {
        for (var otra :
                new pe.edu.utp.clinica.repository.DisponibilidadRepository(conn)
                        .retencionesMedico(medico, inicio.toLocalDate(), ahora)) {
            if (inicio.isBefore(otra.inicio().plusMinutes(otra.minutos()))
                    && otra.inicio().isBefore(inicio.plusMinutes(minutos)))
                throw new AgendaException(
                        409,
                        "HORARIO_RETENIDO",
                        "Otro paciente está confirmando ese horario. Selecciona otro cupo o"
                            + " inténtalo más tarde.");
        }
    }
}
