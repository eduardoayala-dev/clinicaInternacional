package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.*;
import java.time.*;
import java.util.*;

public final class InicioOperativoDAO {
    public record Resumen(int citas,int canceladas,int cupos,int ofertas,LocalDateTime actualizado){}
    private final AgendaCitaDAO.ConnectionSource conexiones;
    private final Clock reloj;
    public InicioOperativoDAO(){this(DatabaseConfig::getConnection,Clock.system(ZoneId.of("America/Lima")));}
    InicioOperativoDAO(AgendaCitaDAO.ConnectionSource conexiones,Clock reloj){this.conexiones=conexiones;this.reloj=reloj.withZone(ZoneId.of("America/Lima"));}
    public Resumen consultar(int actor){
        return TransaccionJdbc.ejecutar(conexiones,c->{
            int sede=AmbitoAdmisionDAO.sede(c,actor);var ahora=LocalDateTime.now(reloj);var fecha=ahora.toLocalDate();int citas=0,canceladas=0,ofertas=0;
            try(var st=c.prepareStatement("SELECT COUNT(*),COALESCE(SUM(CASE WHEN ci.estado='CANCELADA' THEN 1 ELSE 0 END),0) FROM cita ci JOIN medico m ON m.id_medico=ci.id_medico LEFT JOIN disponibilidad d ON d.id_disponibilidad=ci.id_disponibilidad WHERE COALESCE(d.id_sede,m.id_sede)=? AND ci.fecha_hora>=? AND ci.fecha_hora<?")){
                st.setInt(1,sede);st.setObject(2,fecha.atStartOfDay());st.setObject(3,fecha.plusDays(1).atStartOfDay());try(var r=st.executeQuery()){r.next();citas=r.getInt(1);canceladas=r.getInt(2);}
            }
            try(var st=c.prepareStatement("SELECT COUNT(*) FROM reasignacion_inteligente r JOIN disponibilidad d ON d.id_disponibilidad=r.id_disponibilidad JOIN cita ci ON ci.id_cita=r.id_cita_original WHERE d.id_sede=? AND r.estado='PENDIENTE' AND r.fecha_limite_respuesta>? AND ci.fecha_hora>? AND ci.estado='CANCELADA' AND d.estado='PUBLICADO'")){
                st.setInt(1,sede);st.setObject(2,ahora);st.setObject(3,ahora);try(var r=st.executeQuery()){r.next();ofertas=r.getInt(1);}
            }
            int cupos=new MedicoDAO(conexiones::open,reloj).contarCuposDisponibles(c,sede,fecha,fecha.plusDays(7));
            return new Resumen(citas,canceladas,cupos,ofertas,ahora);
        });
    }
    public List<Map<String,String>> operaciones(int actor){
        return TransaccionJdbc.ejecutar(conexiones,c->{
            int sede=AmbitoAdmisionDAO.sede(c,actor);var lista=new ArrayList<Map<String,String>>();
            try(var st=c.prepareStatement("SELECT n.fecha_envio,n.tipo,n.mensaje,ci.id_cita,ci.fecha_hora,CONCAT(u.nombres,' ',u.apellidos) FROM notificacion n JOIN cita ci ON ci.id_cita=n.id_cita JOIN paciente p ON p.id_paciente=ci.id_paciente JOIN usuario u ON u.id_usuario=p.id_usuario JOIN medico m ON m.id_medico=ci.id_medico LEFT JOIN disponibilidad d ON d.id_disponibilidad=ci.id_disponibilidad WHERE COALESCE(d.id_sede,m.id_sede)=? ORDER BY n.fecha_envio DESC,n.id_notificacion DESC LIMIT 5")){
                st.setInt(1,sede);try(var r=st.executeQuery()){while(r.next()){
                    var fecha=r.getObject(1,LocalDateTime.class);
                    String texto=resumenNotificacion(r.getString(2),r.getString(3))+" · Cita #"+r.getInt(4)+" · "+Objects.requireNonNullElse(r.getString(6),"Paciente")+" · "+pe.edu.utp.clinica.util.FormatoClinico.fechaHora(r.getObject(5,LocalDateTime.class));
                    lista.add(Map.of("hora",pe.edu.utp.clinica.util.FormatoClinico.fechaHora(fecha),"texto",texto));
                }}
            }
            return List.copyOf(lista);
        });
    }
    private static String resumenNotificacion(String tipo,String mensaje){
        String texto=(Objects.requireNonNullElse(tipo,"")+" "+Objects.requireNonNullElse(mensaje,"")).toLowerCase(Locale.ROOT);
        if(texto.contains("cancelad"))return "Aviso de cancelación registrado";
        if(texto.contains("reprogramad"))return "Aviso de reprogramación registrado";
        if(texto.contains("recordatorio"))return "Recordatorio de cita registrado";
        if(texto.contains("confirmad"))return "Aviso de confirmación de cita registrado";
        return "Notificación de cita registrada";
    }
}

