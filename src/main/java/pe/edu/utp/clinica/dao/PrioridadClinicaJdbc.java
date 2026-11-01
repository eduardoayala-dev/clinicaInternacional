package pe.edu.utp.clinica.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

final class PrioridadClinicaJdbc {
    private static final String FUENTE=" FROM cita pc JOIN atencion_cita pa ON pa.id_cita=pc.id_cita AND pa.id_medico=pc.id_medico WHERE pc.estado='ATENDIDA' AND pa.fecha_fin IS NOT NULL AND pc.nivel_urgencia IN ('ALTA','MEDIA','BAJA') ";
    private static final String ULTIMA=" ORDER BY pa.fecha_fin DESC,pc.id_cita DESC LIMIT 1";
    private static final String NIVEL="CASE WHEN pc.fecha_recomendada_proxima>=? AND NOT EXISTS (SELECT 1 FROM seguimiento_cita sc JOIN cita control ON control.id_cita=sc.id_cita_control WHERE sc.id_cita_origen=pc.id_cita AND control.id_paciente=pc.id_paciente AND control.estado='ATENDIDA') THEN pc.nivel_urgencia ELSE NULL END";
    static final String EVALUACION_LISTA="(SELECT "+NIVEL+FUENTE+"AND pc.id_paciente=le.id_paciente AND pa.id_especialidad_prioridad=le.id_especialidad"+ULTIMA+")";
    static final String PARA_LISTA="COALESCE("+EVALUACION_LISTA+",'MEDIA')";
    private PrioridadClinicaJdbc() {}
    static String resolver(Connection conexion,int paciente,int especialidad,LocalDate hoy)throws SQLException {
        try(var consulta=conexion.prepareStatement("SELECT "+NIVEL+FUENTE+"AND pc.id_paciente=? AND pa.id_especialidad_prioridad=?"+ULTIMA)) {
            consulta.setObject(1,hoy);consulta.setInt(2,paciente);consulta.setInt(3,especialidad);
            try(var filas=consulta.executeQuery()){return filas.next() && filas.getString(1)!=null?filas.getString(1):"MEDIA";}
        }
    }
    static void auditar(Connection conexion,int medico,int cita,String prioridad)throws SQLException {
        try(var referencia=conexion.prepareStatement("UPDATE atencion_cita SET id_especialidad_prioridad=(SELECT id_especialidad FROM medico WHERE id_medico=?) WHERE id_cita=? AND id_medico=? AND fecha_fin IS NOT NULL")) {
            referencia.setInt(1,medico);referencia.setInt(2,cita);referencia.setInt(3,medico);
            if(referencia.executeUpdate()!=1)throw new SQLException("No se pudo vincular la especialidad de la evaluación");
        }
        try(var registro=conexion.prepareStatement("INSERT INTO auditoria_seguridad(id_usuario,evento,descripcion,sospechoso) SELECT id_usuario,'PRIORIDAD_CLINICA_REGISTRADA',?,FALSE FROM medico WHERE id_medico=?")) {
            registro.setString(1,"Cita #"+cita+"; prioridad "+prioridad);registro.setInt(2,medico);
            if(registro.executeUpdate()!=1)throw new SQLException("No se pudo auditar la prioridad clínica");
        }
    }
}
