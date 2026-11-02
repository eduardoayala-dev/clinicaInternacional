package pe.edu.utp.clinica.correo;

import pe.edu.utp.clinica.auth.SesionesJdbc;
import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.util.Html;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class CorreosRecordatorioJdbc extends CorreosPendientesJdbc {
    private static final ZoneId LIMA=ZoneId.of("America/Lima");
    private final String portal;
    private final String base;
    public CorreosRecordatorioJdbc(String base){this(DatabaseConfig::getConnection,base);}
    public CorreosRecordatorioJdbc(SesionesJdbc.Conexion conexiones,String base){
        super(conexiones,RECORDATORIOS);this.base=base;this.portal=base+"/paciente/historial";
    }

    public int programar(Instant ahora,int horas)throws SQLException {
        if(horas<1||horas>168)throw new IllegalArgumentException("La anticipación debe ser de 1 a 168 horas.");
        String sql="""
                INSERT INTO correo_recordatorio(id_cita,estado,intentos,disponible_en)
                SELECT c.id_cita,'PENDIENTE',0,? FROM cita c
                JOIN confirmacion_asistencia a ON a.id_cita=c.id_cita
                JOIN paciente p ON p.id_paciente=c.id_paciente
                JOIN usuario u ON u.id_usuario=p.id_usuario
                WHERE c.estado='CONFIRMADA' AND a.confirmada_en IS NULL
                AND u.activo=TRUE AND u.rol='PACIENTE' AND c.fecha_hora>? AND c.fecha_hora<=? AND a.vence_en>?
                AND NOT EXISTS(SELECT 1 FROM correo_recordatorio r WHERE r.id_cita=c.id_cita)
                ORDER BY c.fecha_hora,c.id_cita LIMIT 100
                ON DUPLICATE KEY UPDATE id_cita=correo_recordatorio.id_cita
                """;
        try(var c=conexiones.abrir();var s=c.prepareStatement(sql)){
            var local=LocalDateTime.ofInstant(ahora,LIMA);
            s.setObject(1,utc(ahora));s.setObject(2,local);s.setObject(3,local.plusHours(horas));s.setObject(4,local);
            return s.executeUpdate();
        }
    }

    @Override public Optional<Correo> mensaje(int id,Instant ahora)throws SQLException {
        try(var c=conexiones.abrir();var s=c.prepareStatement("SELECT confirmada_en,vence_en FROM confirmacion_asistencia WHERE id_cita=?")){
            s.setInt(1,id);
            try(var r=s.executeQuery()){
                if(!r.next()||r.getObject(1)!=null)return Optional.empty();
                var limite=r.getObject(2,LocalDateTime.class);
                if(limite==null||!limite.isAfter(LocalDateTime.ofInstant(ahora,LIMA)))return Optional.empty();
            }
        }
        return new CorreosCitaJdbc(conexiones).datos(id,ahora).map(this::crearCorreo);
    }

    private Correo crearCorreo(ConfirmacionCita cita){
        String fecha=cita.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String texto="Hola, "+cita.paciente()+". Recuerda tu cita CIT-"+cita.id()+" el "+fecha+" (hora de Perú)."
                +"\nEspecialidad: "+cita.especialidad()+"\nMédico: "+cita.medico()+"\nSede: "+cita.sede()
                +"\nTu asistencia está pendiente de confirmar. Ingresa al portal para confirmar tu asistencia o gestionar tu cita: "+portal
                +"\nSi ya venció el plazo de confirmación, contacta a admisión. Este mensaje no cancela tu cita."+EnlacesCorreoCita.texto(base,cita.id());
        String html="""
                <h1 style="color:#0d2b4e;font-family:Georgia,serif">Recuerda tu próxima cita</h1>
                <p>Hola, %s.</p><p><strong>CIT-%d · %s (hora de Perú)</strong></p>
                <p>%s · %s<br>%s</p>
                <p>Tu asistencia está pendiente de confirmar. Ingresa al portal para confirmar o gestionar tu cita.</p>
                <p><a href="%s" style="display:inline-block;padding:12px 20px;background:#1668b8;color:#fff;border-radius:8px;text-decoration:none">Ver mis citas</a></p>
                <p>Si ya venció el plazo de confirmación, contacta a admisión. Este mensaje no cancela tu cita.</p>
                """.formatted(Html.texto(cita.paciente()),cita.id(),fecha,Html.texto(cita.especialidad()),Html.texto(cita.medico()),Html.texto(cita.sede()),Html.texto(portal));
        return new Correo(cita.destinatario(),"Recordatorio de cita CIT-"+cita.id(),texto,PlantillasCorreo.marco(html+EnlacesCorreoCita.html(base,cita.id()),"Recordatorio de cita"));
    }
}
