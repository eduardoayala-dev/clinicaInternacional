package pe.edu.utp.clinica.correo;

import pe.edu.utp.clinica.auth.SesionesJdbc;
import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.util.Html;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class CorreosCambiosJdbc extends CorreosPendientesJdbc {
    public enum Tipo { CANCELADA, REPROGRAMADA, CONFIRMACION_VENCIDA }
    private static final ZoneId LIMA=ZoneId.of("America/Lima");
    private static final DateTimeFormatter FECHA=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public CorreosCambiosJdbc(){this(DatabaseConfig::getConnection);}
    public CorreosCambiosJdbc(SesionesJdbc.Conexion conexiones){super(conexiones,CAMBIOS);}
    public static void encolar(Connection c,int cita,Integer anterior,Tipo tipo,Instant ahora)throws SQLException {
        try(var s=c.prepareStatement("INSERT INTO correo_cambio_cita(id_cita,id_cita_anterior,tipo,disponible_en) VALUES(?,?,?,?)")){
            s.setInt(1,cita);s.setObject(2,anterior);s.setString(3,tipo.name());s.setObject(4,utc(ahora));s.executeUpdate();
        }
    }
    @Override public Optional<Correo> mensaje(int id,Instant ahora)throws SQLException {
        String sql="""
                SELECT a.tipo,c.id_cita,c.estado,c.fecha_hora,u.email,u.nombres,
                um.nombres AS medico,um.apellidos,e.nombre AS especialidad,s.nombre AS sede,
                anterior.id_cita AS origen,anterior.fecha_hora AS fecha_anterior,anterior.estado AS estado_anterior,
                asistencia.confirmada_en,asistencia.vence_en
                FROM correo_cambio_cita a JOIN cita c ON c.id_cita=a.id_cita
                JOIN paciente p ON p.id_paciente=c.id_paciente JOIN usuario u ON u.id_usuario=p.id_usuario
                JOIN medico m ON m.id_medico=c.id_medico JOIN usuario um ON um.id_usuario=m.id_usuario
                JOIN especialidad e ON e.id_especialidad=m.id_especialidad
                LEFT JOIN disponibilidad d ON d.id_disponibilidad=c.id_disponibilidad
                JOIN sede s ON s.id_sede=COALESCE(d.id_sede,m.id_sede)
                LEFT JOIN cita anterior ON anterior.id_cita=a.id_cita_anterior AND anterior.id_paciente=c.id_paciente
                LEFT JOIN confirmacion_asistencia asistencia ON asistencia.id_cita=c.id_cita
                WHERE a.id_aviso=? AND u.activo=TRUE AND u.rol='PACIENTE'
                """;
        try(var c=conexiones.abrir();var s=c.prepareStatement(sql)){
            s.setInt(1,id);
            try(var r=s.executeQuery()){
                if(!r.next())return Optional.empty();
                Tipo tipo=Tipo.valueOf(r.getString("tipo"));
                var fecha=r.getObject("fecha_hora",LocalDateTime.class);
                if(tipo==Tipo.CANCELADA&&!"CANCELADA".equals(r.getString("estado")))return Optional.empty();
                if(tipo==Tipo.REPROGRAMADA&&(!"CONFIRMADA".equals(r.getString("estado"))
                        || !fecha.isAfter(LocalDateTime.ofInstant(ahora,LIMA))
                        || r.getObject("origen")==null||!"REPROGRAMADA".equals(r.getString("estado_anterior"))))return Optional.empty();
                if(tipo==Tipo.CONFIRMACION_VENCIDA){
                    var limite=r.getObject("vence_en",LocalDateTime.class);
                    var local=LocalDateTime.ofInstant(ahora,LIMA);
                    if(!("CONFIRMADA".equals(r.getString("estado"))||"SOLICITADA".equals(r.getString("estado")))
                            ||!fecha.isAfter(local)||r.getObject("confirmada_en")!=null||limite==null||limite.isAfter(local))return Optional.empty();
                }
                String titulo=switch(tipo){
                    case CANCELADA->"Tu cita fue cancelada";
                    case REPROGRAMADA->"Tu cita fue reprogramada";
                    case CONFIRMACION_VENCIDA->"Coordina tu asistencia con admisión";
                };
                String detalle=switch(tipo){
                    case CANCELADA->"Esta cita ya no está reservada. Puedes buscar un nuevo horario en tu portal.";
                    case REPROGRAMADA->"La cita CIT-"+r.getInt("origen")+" del "+r.getObject("fecha_anterior",LocalDateTime.class).format(FECHA)+" fue reemplazada por esta nueva reserva. La asistencia de la nueva cita está pendiente de confirmar.";
                    case CONFIRMACION_VENCIDA->"El plazo para confirmar tu asistencia terminó. Tu cita sigue vigente: contacta con admisión para coordinarla. Este aviso no cancela tu reserva.";
                };
                String datos="CIT-"+r.getInt("id_cita")+" · "+fecha.format(FECHA)+" (hora de Perú)";
                String medico=r.getString("medico")+" "+r.getString("apellidos");
                String texto=titulo+"\nHola, "+r.getString("nombres")+".\n"+datos+"\n"+r.getString("especialidad")+" · "+medico+"\n"+r.getString("sede")+"\n\n"+detalle+"\nConsulta el estado actualizado en Mis citas, iniciando sesión en tu portal.";
                String contenido="<h1 style=\"color:#0d2b4e;font-family:Georgia,serif\">"+titulo+"</h1><p>Hola, "+Html.texto(r.getString("nombres"))+".</p><p><strong>"+Html.texto(datos)+"</strong></p><p>"+Html.texto(r.getString("especialidad"))+" · "+Html.texto(medico)+"<br>"+Html.texto(r.getString("sede"))+"</p><p>"+Html.texto(detalle)+"</p><p>Consulta el estado actualizado en <strong>Mis citas</strong>, iniciando sesión en tu portal.</p>";
                return Optional.of(new Correo(r.getString("email"),titulo+" · CIT-"+r.getInt("id_cita"),texto,PlantillasCorreo.marco(contenido,titulo)));
            }
        }
    }
}
