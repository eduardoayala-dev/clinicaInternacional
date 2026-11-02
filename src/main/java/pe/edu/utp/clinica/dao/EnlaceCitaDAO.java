package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.util.EnlaceCita;
import java.sql.*;
import java.time.*;

public class EnlaceCitaDAO {
    public record Resumen(int id,LocalDateTime fecha,String medico,String especialidad,String sede,String estado,boolean asistenciaConfirmada,boolean plazoVencido,boolean futura) {}
    private final AgendaCitaDAO.ConnectionSource conexiones;
    private final Clock reloj;
    public EnlaceCitaDAO(){this(DatabaseConfig::getConnection,Clock.system(ZoneId.of("America/Lima")));}
    EnlaceCitaDAO(AgendaCitaDAO.ConnectionSource conexiones,Clock reloj){this.conexiones=conexiones;this.reloj=reloj.withZone(ZoneId.of("America/Lima"));}
    public Resumen consultar(int usuario,int cita){
        String sql="""
                SELECT c.id_cita,c.fecha_hora,CONCAT(um.nombres,' ',um.apellidos),e.nombre,s.nombre,c.estado,a.confirmada_en,a.vence_en
                FROM cita c JOIN paciente p ON p.id_paciente=c.id_paciente JOIN usuario u ON u.id_usuario=p.id_usuario
                JOIN medico m ON m.id_medico=c.id_medico JOIN usuario um ON um.id_usuario=m.id_usuario
                JOIN especialidad e ON e.id_especialidad=m.id_especialidad
                LEFT JOIN disponibilidad d ON d.id_disponibilidad=c.id_disponibilidad JOIN sede s ON s.id_sede=COALESCE(d.id_sede,m.id_sede)
                JOIN confirmacion_asistencia a ON a.id_cita=c.id_cita
                WHERE c.id_cita=? AND u.id_usuario=? AND u.rol='PACIENTE' AND u.activo=TRUE
                """;
        try(var c=conexiones.open();var s=c.prepareStatement(sql)){
            s.setInt(1,cita);s.setInt(2,usuario);
            try(var r=s.executeQuery()){
                if(!r.next())throw new AgendaException(404,"CITA_NO_ENCONTRADA","No se encontró una cita propia para este enlace.");
                var ahora=LocalDateTime.now(reloj);var fecha=r.getObject(2,LocalDateTime.class);var vence=r.getObject(8,LocalDateTime.class);
                return new Resumen(r.getInt(1),fecha,r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getObject(7)!=null,vence==null||!ahora.isBefore(vence),fecha.isAfter(ahora));
            }
        }catch(SQLException e){throw new AgendaException(503,"CITA_NO_DISPONIBLE","No se pudo consultar tu cita. Inténtalo nuevamente.");}
    }
    public void ejecutar(int usuario,EnlaceCita enlace){
        Resumen cita=consultar(usuario,enlace.cita());
        if(!cita.futura()||!("CONFIRMADA".equals(cita.estado())||"SOLICITADA".equals(cita.estado())))throw new AgendaException(409,"CITA_NO_DISPONIBLE","La cita ya no está disponible para esta acción.");
        if(enlace.accion()==EnlaceCita.Accion.CONFIRMAR){new ConfirmacionAsistenciaDAO(conexiones,reloj).confirmarPaciente(usuario,enlace.cita());return;}
        var citas=new CitaDAO(){
            @Override protected Connection abrirConexion()throws SQLException{return conexiones.open();}
            @Override protected Clock relojClinico(){return reloj;}
        };
        int paciente=citas.obtenerIdPacientePorUsuarioId(usuario);
        if(!citas.actualizarEstadoCitaPaciente(enlace.cita(),paciente,"CANCELADA"))throw new AgendaException(409,"CITA_NO_DISPONIBLE","La cita cambió de estado o ya pasó su horario.");
    }
}
