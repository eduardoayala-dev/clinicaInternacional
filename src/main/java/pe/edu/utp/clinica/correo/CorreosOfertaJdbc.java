package pe.edu.utp.clinica.correo;

import pe.edu.utp.clinica.auth.SesionesJdbc;
import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

public final class CorreosOfertaJdbc extends CorreosPendientesJdbc {
    public CorreosOfertaJdbc() { this(DatabaseConfig::getConnection); }
    public CorreosOfertaJdbc(SesionesJdbc.Conexion conexiones) { super(conexiones,OFERTAS); }

    public static void encolar(Connection conn,int oferta,Instant ahora)throws SQLException {
        try(var st=conn.prepareStatement("INSERT INTO correo_oferta(id_oferta,estado,intentos,disponible_en) VALUES(?,'PENDIENTE',0,?)")) {
            st.setInt(1,oferta);
            st.setObject(2,utc(ahora));
            st.executeUpdate();
        }
    }

    @Override public Optional<Correo> mensaje(int oferta,Instant ahora)throws SQLException {
        String sql="SELECT u.email,u.nombres,c.fecha_hora,r.fecha_limite_respuesta,um.nombres AS medico_nombres,um.apellidos AS medico_apellidos,e.nombre AS especialidad,s.nombre AS sede "
                +"FROM reasignacion_inteligente r JOIN lista_espera le ON le.id_lista_espera=r.id_lista_espera JOIN paciente p ON p.id_paciente=le.id_paciente JOIN usuario u ON u.id_usuario=p.id_usuario "
                +"JOIN cita c ON c.id_cita=r.id_cita_original JOIN disponibilidad d ON d.id_disponibilidad=r.id_disponibilidad JOIN medico m ON m.id_medico=c.id_medico JOIN usuario um ON um.id_usuario=m.id_usuario "
                +"JOIN especialidad e ON e.id_especialidad=m.id_especialidad JOIN sede s ON s.id_sede=m.id_sede "
                +"WHERE r.id_reasignacion=? AND r.estado='PENDIENTE' AND le.estado='OFERTADA' AND le.canal_notificacion LIKE '%CORREO%' AND r.fecha_limite_respuesta>? AND c.fecha_hora>? "
                +"AND c.estado='CANCELADA' AND d.estado='PUBLICADO' AND d.id_sede=m.id_sede AND m.estado='ACTIVO' AND um.activo=TRUE AND e.estado='ACTIVO' AND s.estado='ACTIVO' AND u.activo=TRUE AND u.rol='PACIENTE' "
                +"AND NOT EXISTS(SELECT 1 FROM cita ocupada WHERE ocupada.id_medico=c.id_medico AND ocupada.fecha_hora=c.fecha_hora AND ocupada.estado NOT IN ('CANCELADA','REPROGRAMADA'))";
        var lima=LocalDateTime.ofInstant(ahora,ZoneId.of("America/Lima"));
        try(var conn=conexiones.abrir();var st=conn.prepareStatement(sql)) {
            st.setInt(1,oferta); st.setObject(2,lima); st.setObject(3,lima);
            try(var rs=st.executeQuery()) {
                if(!rs.next())return Optional.empty();
                return Optional.of(new OfertaCupo(oferta,rs.getString("email"),rs.getString("nombres"),
                        rs.getString("medico_nombres")+" "+rs.getString("medico_apellidos"),rs.getString("especialidad"),rs.getString("sede"),
                        rs.getObject("fecha_hora",LocalDateTime.class),rs.getObject("fecha_limite_respuesta",LocalDateTime.class)).correo());
            }
        }
    }
}
