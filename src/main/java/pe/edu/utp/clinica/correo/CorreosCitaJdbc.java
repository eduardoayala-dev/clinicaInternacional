package pe.edu.utp.clinica.correo;

import pe.edu.utp.clinica.auth.SesionesJdbc;
import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.*;
import java.time.*;
import java.util.Optional;

public final class CorreosCitaJdbc extends CorreosPendientesJdbc {
    public CorreosCitaJdbc(){this(DatabaseConfig::getConnection);}
    public CorreosCitaJdbc(SesionesJdbc.Conexion conexiones){super(conexiones,CITAS);}

    public static void encolar(Connection conn,int cita,Instant ahora)throws SQLException {
        try(var st=conn.prepareStatement("INSERT INTO correo_cita(id_cita,estado,intentos,disponible_en) VALUES(?,'PENDIENTE',0,?)")) {
            st.setInt(1,cita);
            st.setObject(2,utc(ahora));
            st.executeUpdate();
        }
    }

    public Optional<ConfirmacionCita> datos(int cita,Instant ahora)throws SQLException {
        String sql="SELECT u.email,u.nombres,c.fecha_hora,um.nombres AS medico_nombres,um.apellidos AS medico_apellidos,e.nombre AS especialidad,s.nombre AS sede FROM cita c JOIN paciente p ON p.id_paciente=c.id_paciente JOIN usuario u ON u.id_usuario=p.id_usuario JOIN medico m ON m.id_medico=c.id_medico JOIN usuario um ON um.id_usuario=m.id_usuario JOIN especialidad e ON e.id_especialidad=m.id_especialidad JOIN sede s ON s.id_sede=m.id_sede WHERE c.id_cita=? AND c.estado='CONFIRMADA' AND u.activo=1 AND u.rol='PACIENTE'";
        try(var conn=conexiones.abrir();var st=conn.prepareStatement(sql)) {
            st.setInt(1,cita);
            try(var rs=st.executeQuery()) {
                if(!rs.next())return Optional.empty();
                LocalDateTime fecha=rs.getObject("fecha_hora",LocalDateTime.class);
                if(fecha==null || !fecha.isAfter(LocalDateTime.ofInstant(ahora,ZoneId.of("America/Lima"))))return Optional.empty();
                return Optional.of(new ConfirmacionCita(cita,rs.getString("email"),rs.getString("nombres"),
                        rs.getString("medico_nombres")+" "+rs.getString("medico_apellidos"),rs.getString("especialidad"),rs.getString("sede"),fecha));
            }
        }
    }

    @Override public Optional<Correo> mensaje(int id,Instant ahora)throws SQLException {
        return datos(id,ahora).map(ConfirmacionCita::correo);
    }
}
