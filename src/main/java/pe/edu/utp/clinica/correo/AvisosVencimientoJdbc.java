package pe.edu.utp.clinica.correo;

import pe.edu.utp.clinica.auth.SesionesJdbc;
import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.SQLException;
import java.time.*;

public final class AvisosVencimientoJdbc {
    private final SesionesJdbc.Conexion conexiones;
    public AvisosVencimientoJdbc(){this(DatabaseConfig::getConnection);}
    public AvisosVencimientoJdbc(SesionesJdbc.Conexion conexiones){this.conexiones=conexiones;}
    public int programar(Instant ahora)throws SQLException {
        String sql="""
                INSERT INTO correo_cambio_cita(id_cita,tipo,disponible_en)
                SELECT c.id_cita,'CONFIRMACION_VENCIDA',? FROM cita c
                JOIN confirmacion_asistencia a ON a.id_cita=c.id_cita
                JOIN paciente p ON p.id_paciente=c.id_paciente JOIN usuario u ON u.id_usuario=p.id_usuario
                WHERE c.estado IN ('SOLICITADA','CONFIRMADA') AND c.fecha_hora>?
                AND a.confirmada_en IS NULL AND a.vence_en<=? AND u.activo=TRUE AND u.rol='PACIENTE'
                AND NOT EXISTS(SELECT 1 FROM correo_cambio_cita n WHERE n.id_cita=c.id_cita AND n.tipo='CONFIRMACION_VENCIDA')
                ORDER BY a.vence_en,c.id_cita LIMIT 100
                ON DUPLICATE KEY UPDATE id_aviso=correo_cambio_cita.id_aviso
                """;
        try(var c=conexiones.abrir();var s=c.prepareStatement(sql)){
            var local=LocalDateTime.ofInstant(ahora,ZoneId.of("America/Lima"));
            s.setObject(1,LocalDateTime.ofInstant(ahora,ZoneOffset.UTC));s.setObject(2,local);s.setObject(3,local);
            return s.executeUpdate();
        }
    }
}
