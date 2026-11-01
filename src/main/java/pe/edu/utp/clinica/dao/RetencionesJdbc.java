package pe.edu.utp.clinica.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

final class RetencionesJdbc {
    private RetencionesJdbc() {}
    static void comprobarLibre(Connection conn,int medico,LocalDateTime inicio,int minutos,LocalDateTime ahora) throws SQLException {
        try(var ps=conn.prepareStatement("SELECT inicio,duracion_minutos FROM retencion_cupo WHERE id_medico=? AND vence>? AND inicio>=? AND inicio<?")) {
            ps.setInt(1,medico);ps.setObject(2,ahora);ps.setObject(3,inicio.toLocalDate().atStartOfDay());ps.setObject(4,inicio.toLocalDate().plusDays(1).atStartOfDay());
            try(var rs=ps.executeQuery()) {
                while(rs.next()) {
                    LocalDateTime otra=rs.getObject(1,LocalDateTime.class);
                    if(inicio.isBefore(otra.plusMinutes(rs.getInt(2))) && otra.isBefore(inicio.plusMinutes(minutos)))
                        throw new AgendaException(409,"HORARIO_RETENIDO","Otro paciente está confirmando ese horario. Selecciona otro cupo o inténtalo más tarde.");
                }
            }
        }
    }
}
