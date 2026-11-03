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
    public record Instantanea(int cita, LocalDateTime evaluadaEn, LocalDateTime reservadaEn,
                             EntradaInasistencia entrada, boolean plazoVencido) {}
    private final SesionesJdbc.Conexion conexiones;
    private final Clock reloj;
    public SeguimientoIaDAO() { this(DatabaseConfig::getConnection, Clock.system(ZoneId.of("America/Lima"))); }
    public SeguimientoIaDAO(SesionesJdbc.Conexion conexiones, Clock reloj) {
        this.conexiones=conexiones;
        this.reloj=reloj.withZone(ZoneId.of("America/Lima"));
    }

    public Instantanea consultar(int actor, int cita) {
        if(actor<=0 || cita<=0) throw ausente();
        String sql="""
                SELECT c.id_cita,c.fecha_hora,c.estado,a.creada_en,a.vence_en,a.confirmada_en,
                (SELECT COUNT(*) FROM cita h JOIN inasistencia_cita i ON i.id_cita=h.id_cita
                 JOIN usuario responsable ON responsable.id_usuario=i.id_usuario_personal
                 WHERE h.id_paciente=c.id_paciente AND h.estado='NO_ASISTIO'
                 AND responsable.rol='PERSONAL_ADMINISTRATIVO'
                 AND h.fecha_hora<a.creada_en AND i.fecha_registro<a.creada_en) AS faltas,
                (SELECT COUNT(*) FROM cita h JOIN atencion_cita t ON t.id_cita=h.id_cita
                 WHERE h.id_paciente=c.id_paciente AND h.estado='ATENDIDA'
                 AND t.id_paciente=h.id_paciente AND t.id_medico=h.id_medico
                 AND t.fecha_inicio IS NOT NULL AND t.fecha_fin>=t.fecha_inicio
                 AND h.fecha_hora<a.creada_en AND t.fecha_fin<a.creada_en) AS atenciones
                FROM cita c JOIN paciente p ON p.id_paciente=c.id_paciente
                JOIN usuario paciente ON paciente.id_usuario=p.id_usuario AND paciente.activo=TRUE
                AND paciente.rol='PACIENTE'
                JOIN medico m ON m.id_medico=c.id_medico
                LEFT JOIN disponibilidad d ON d.id_disponibilidad=c.id_disponibilidad
                JOIN personal_administrativo pa ON pa.id_sede=COALESCE(d.id_sede,m.id_sede)
                JOIN usuario u ON u.id_usuario=pa.id_usuario AND u.activo=TRUE AND u.rol='PERSONAL_ADMINISTRATIVO'
                JOIN sede s ON s.id_sede=pa.id_sede AND s.estado='ACTIVO'
                LEFT JOIN confirmacion_asistencia a ON a.id_cita=c.id_cita
                WHERE c.id_cita=? AND u.id_usuario=?
                """;
        try(var c=conexiones.abrir(); var s=c.prepareStatement(sql)) {
            s.setInt(1,cita); s.setInt(2,actor);
            try(var r=s.executeQuery()) {
                if(!r.next()) throw ausente();
                var ahora=LocalDateTime.now(reloj);
                var fecha=r.getObject("fecha_hora",LocalDateTime.class);
                if(!fecha.isAfter(ahora) || !("SOLICITADA".equals(r.getString("estado")) || "CONFIRMADA".equals(r.getString("estado"))))
                    throw new AgendaException(409,"CITA_NO_EVALUABLE","Solo se evalúan citas futuras pendientes de atención.");
                var reserva=r.getObject("creada_en",LocalDateTime.class);
                var vence=r.getObject("vence_en",LocalDateTime.class);
                var confirmada=r.getObject("confirmada_en",LocalDateTime.class);
                if(reserva==null || vence==null || reserva.isAfter(ahora) || reserva.isAfter(fecha)
                        || (confirmada!=null && (confirmada.isAfter(ahora) || confirmada.isBefore(reserva)))) throw incompleta();
                long dias=ChronoUnit.DAYS.between(reserva.toLocalDate(),fecha.toLocalDate());
                if(dias>180 || r.getInt("faltas")>100 || r.getInt("atenciones")>100) throw incompleta();
                return new Instantanea(cita,ahora,reserva,new EntradaInasistencia(r.getInt("faltas"),r.getInt("atenciones"),
                        (int)dias,confirmada!=null),confirmada==null && !ahora.isBefore(vence));
            }
        } catch(SQLException e) {
            throw new AgendaException(503,"SEGUIMIENTO_IA_NO_DISPONIBLE","No se pudo reconstruir el historial de la cita.");
        }
    }
    private static AgendaException ausente() { return new AgendaException(404,"CITA_NO_ENCONTRADA","No se encontró una cita accesible en tu sede."); }
    private static AgendaException incompleta() { return new AgendaException(409,"DATOS_IA_INSUFICIENTES","La cita no tiene una instantánea válida dentro de los límites del modelo. Continúa el seguimiento habitual."); }
}
