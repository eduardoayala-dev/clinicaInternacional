package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MedicoPortalDAO {
    private static final String SIN_REGISTRAR = "Sin registrar";
    private static final String EN_ATENCION = "EN_ATENCION";
    private static final String NOMBRES = "nombres";
    private static final String APELLIDOS = "apellidos";
    private static final String ATENDIDA = "ATENDIDA";
    private static final String MOTIVO = "motivo";
    private static final ZoneId LIMA=ZoneId.of("America/Lima");
    private static final String PERFIL="SELECT m.id_medico,m.cmp,u.nombres,u.apellidos,u.email,e.nombre AS especialidad,s.nombre AS sedeNombre FROM medico m JOIN usuario u ON m.id_usuario=u.id_usuario JOIN especialidad e ON m.id_especialidad=e.id_especialidad JOIN sede s ON m.id_sede=s.id_sede WHERE m.id_usuario=? AND m.estado='ACTIVO' AND u.activo=TRUE AND u.rol='MEDICO' AND e.estado='ACTIVO' AND s.estado='ACTIVO'";
    private static final String DATOS="SELECT EXISTS(SELECT 1 FROM atencion_cita ac WHERE ac.medico_activo=c.id_medico OR ac.paciente_activo=c.id_paciente) AS consulta_ocupada,t.fecha_inicio,t.fecha_fin,t.motivo_no_atencion,a.fecha_llegada,a.llego_tarde,a.tolerancia_minutos,c.id_cita,c.id_paciente,c.fecha_hora,c.estado,c.nivel_urgencia,c.fecha_recomendada_proxima,c.fecha_creacion,p.dni,p.telefono,p.fecha_nacimiento,p.direccion,p.alergias,p.tipo_cobertura,p.canal_aviso,u.nombres,u.apellidos,u.email,mc.descripcion AS motivo,e.nombre AS especialidad,s.nombre AS sede,m.cmp,um.nombres AS medico_nombres,um.apellidos AS medico_apellidos,"
            + "(SELECT h.observacion FROM historial_asistencia h WHERE h.id_cita=c.id_cita ORDER BY h.id_historial DESC LIMIT 1) AS indicaciones,"
            + "EXISTS(SELECT 1 FROM reasignacion_inteligente r WHERE r.id_cita_asignada=c.id_cita AND r.estado='ACEPTADA') AS reasignada "
            + "FROM cita c JOIN paciente p ON c.id_paciente=p.id_paciente JOIN usuario u ON p.id_usuario=u.id_usuario JOIN medico m ON c.id_medico=m.id_medico JOIN usuario um ON m.id_usuario=um.id_usuario JOIN especialidad e ON m.id_especialidad=e.id_especialidad JOIN sede s ON m.id_sede=s.id_sede LEFT JOIN motivo_cita mc ON c.id_motivo=mc.id_motivo LEFT JOIN admision_cita a ON a.id_cita=c.id_cita LEFT JOIN atencion_cita t ON t.id_cita=c.id_cita ";
    private final AgendaCitaDAO.ConnectionSource connections;
    private final Clock clock;
    public MedicoPortalDAO(){this(DatabaseConfig::getConnection,Clock.system(LIMA));}
    MedicoPortalDAO(AgendaCitaDAO.ConnectionSource connections,Clock clock){this.connections=connections;this.clock=clock.withZone(LIMA);}

    public Map<String,Object> perfil(int usuario){
        try(Connection conn=connections.open();PreparedStatement ps=conn.prepareStatement(PERFIL)){
            ps.setInt(1,usuario);
            try(ResultSet rs=ps.executeQuery()){
                if(!rs.next()) throw new AgendaException(403,"MEDICO_NO_HABILITADO","El perfil médico no está habilitado.");
                Map<String,Object> data=new LinkedHashMap<>();
                data.put("idMedico",rs.getInt("id_medico"));
                data.put("nombreMedico",texto(rs,NOMBRES)+" "+texto(rs,APELLIDOS));
                for(String campo:List.of("cmp","email","especialidad","sedeNombre")) data.put(campo,texto(rs,campo));
                return data;
            }
        }catch(SQLException e){throw new CatalogoDatosException(e);}
    }

    public List<Map<String,Object>> agenda(int medico,LocalDate fecha){
        if(medico<=0 || fecha==null) throw new AgendaException(400,"DATOS_INVALIDOS","Selecciona una fecha válida.");
        String sql="SELECT EXISTS(SELECT 1 FROM atencion_cita ac WHERE ac.medico_activo=c.id_medico OR ac.paciente_activo=c.id_paciente) AS consulta_ocupada,t.fecha_inicio,t.fecha_fin,t.motivo_no_atencion,a.fecha_llegada,a.llego_tarde,a.tolerancia_minutos,c.id_cita,c.fecha_hora,c.estado,p.dni,u.nombres,u.apellidos,mc.descripcion AS motivo,EXISTS(SELECT 1 FROM reasignacion_inteligente r WHERE r.id_cita_asignada=c.id_cita AND r.estado='ACEPTADA') AS reasignada FROM cita c JOIN paciente p ON c.id_paciente=p.id_paciente JOIN usuario u ON p.id_usuario=u.id_usuario LEFT JOIN motivo_cita mc ON c.id_motivo=mc.id_motivo LEFT JOIN admision_cita a ON a.id_cita=c.id_cita LEFT JOIN atencion_cita t ON t.id_cita=c.id_cita WHERE c.id_medico=? AND c.fecha_hora>=? AND c.fecha_hora<? ORDER BY c.fecha_hora,c.id_cita";
        List<Map<String,Object>> rows=new ArrayList<>();
        try(Connection conn=connections.open();PreparedStatement ps=conn.prepareStatement(sql)){
            ps.setInt(1,medico);ps.setObject(2,fecha.atStartOfDay());ps.setObject(3,fecha.plusDays(1).atStartOfDay());
            try(ResultSet rs=ps.executeQuery()){
                while(rs.next()){
                    Map<String,Object> data=base(rs);
                    data.put("paciente",texto(rs,NOMBRES)+" "+texto(rs,APELLIDOS));data.put("dni",texto(rs,"dni"));
                    data.put(MOTIVO,texto(rs,MOTIVO));data.put("origen",rs.getBoolean("reasignada")?"Lista de espera":"Reserva");
                    rows.add(data);
                }
            }
            return rows;
        }catch(SQLException e){throw new CatalogoDatosException(e);}
    }

    public Map<String,Object> detalle(int medico,int cita){
        if(medico<=0 || cita<=0) return Map.of();
        try(Connection conn=connections.open();PreparedStatement ps=conn.prepareStatement(DATOS+"WHERE c.id_medico=? AND c.id_cita=?")){
            ps.setInt(1,medico);ps.setInt(2,cita);
            try(ResultSet rs=ps.executeQuery()){return rs.next()?presentar(rs):Map.of();}
        }catch(SQLException e){throw new CatalogoDatosException(e);}
    }

    public boolean guardar(int medico,int cita,String estado,String prioridad,String fecha,String notas){
        if (!ATENDIDA.equals(estado)) throw new AgendaException(403,"ACCION_NO_PERMITIDA","La inasistencia debe registrarse en admisión. Para un paciente que llegó, utiliza No atendida con un motivo.");
        return new AtencionCitaDAO(connections,clock).finalizar(medico,cita,prioridad,fecha,notas);
    }
    public boolean iniciar(int medico,int cita){return new AtencionCitaDAO(connections,clock).iniciar(medico,cita);}
    public boolean noAtender(int medico,int cita,String motivo){return new AtencionCitaDAO(connections,clock).noAtender(medico,cita,motivo);}
    public int activa(int medico){
        try(Connection conn=connections.open();PreparedStatement ps=conn.prepareStatement("SELECT id_cita FROM atencion_cita WHERE medico_activo=?")){
            ps.setInt(1,medico);try(ResultSet rs=ps.executeQuery()){return rs.next()?rs.getInt(1):-1;}
        }catch(SQLException e){throw new CatalogoDatosException(e);}
    }

    private Map<String,Object> base(ResultSet rs)throws SQLException{
        Map<String,Object> data=new LinkedHashMap<>();
        LocalDateTime inicio=rs.getObject("fecha_hora",LocalDateTime.class);
        String estado=texto(rs,"estado");
        data.put("idCita",rs.getInt("id_cita"));
        LocalDateTime llegada=rs.getObject("fecha_llegada",LocalDateTime.class);
        data.put("fechaLlegada",llegada==null?"":llegada.toString());
        data.put("horaLlegada",llegada==null?SIN_REGISTRAR:llegada.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        data.put("llegadaRegistrada",llegada!=null);
        presentarAtencion(rs,data,estado,inicio,llegada);
        data.put("llegoTarde",llegada!=null && rs.getBoolean("llego_tarde"));
        data.put("toleranciaMinutos",llegada==null?null:rs.getInt("tolerancia_minutos"));
        data.put("fecha",inicio.toLocalDate().toString());data.put("hora",inicio.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        data.put("fechaFormateada",inicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        data.put("fechaHora",inicio.toString());data.put("estado",estado);data.put("editable",EN_ATENCION.equals(estado) && rs.getObject("fecha_inicio")!=null && rs.getObject("fecha_fin")==null);
        data.put("estadoTexto",switch(estado){
            case EN_ATENCION->"En atención";case "NO_ATENDIDA"->"No atendida";case ATENDIDA->"Atendida";case "NO_ASISTIO"->"No asistió";case "EN_SALA"->"En sala";
            case "CANCELADA"->"Cancelada";case "REPROGRAMADA"->"Reprogramada";
            case "CONFIRMADA"->"Confirmada";case "SOLICITADA"->"Solicitada";default->estado;
        });
        return data;
    }
    private void presentarAtencion(ResultSet rs,Map<String,Object> data,String estado,LocalDateTime programada,LocalDateTime llegada)throws SQLException {
        LocalDateTime comienzo=rs.getObject("fecha_inicio",LocalDateTime.class);
        LocalDateTime fin=rs.getObject("fecha_fin",LocalDateTime.class);
        LocalDateTime ahora=LocalDateTime.now(clock).withNano(0);
        boolean enSalaHoy="EN_SALA".equals(estado) && llegada!=null && !llegada.isAfter(ahora) && programada.toLocalDate().equals(ahora.toLocalDate());
        boolean ocupada=rs.getBoolean("consulta_ocupada");
        data.put("puedeIniciar",enSalaHoy && !ocupada);
        data.put("puedeNoAtender",enSalaHoy || EN_ATENCION.equals(estado));
        data.put("consultaOcupada",ocupada);
        data.put("fechaInicio",comienzo==null?"":comienzo.toString());
        data.put("fechaFin",fin==null?"":fin.toString());
        DateTimeFormatter formato=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        data.put("inicioTexto",comienzo==null?SIN_REGISTRAR:comienzo.format(formato));
        data.put("finTexto",fin==null?SIN_REGISTRAR:fin.format(formato));
        data.put("motivoNoAtencion",texto(rs,"motivo_no_atencion"));
        data.put("esperaMinutos",minutosEntre(llegada,comienzo));
        data.put("duracionMinutos",minutosEntre(comienzo,fin));
    }
    private static Long minutosEntre(LocalDateTime desde,LocalDateTime hasta) {
        if(desde==null || hasta==null) return null;
        return java.time.Duration.between(desde.atZone(LIMA),hasta.atZone(LIMA)).toMinutes();
    }
    private Map<String,Object> presentar(ResultSet rs)throws SQLException{
        Map<String,Object> data=base(rs);
        data.put("idPaciente",rs.getInt("id_paciente"));
        String nombre=texto(rs,NOMBRES)+" "+texto(rs,APELLIDOS);
        data.put("paciente",nombre);data.put("pacienteNombre",nombre);
        for(String campo:List.of("dni","telefono","email","direccion","alergias","especialidad","sede","cmp",MOTIVO,"indicaciones")) data.put(campo,texto(rs,campo));
        data.put("urgencia",texto(rs,"nivel_urgencia"));data.put("tipoCobertura",texto(rs,"tipo_cobertura"));
        data.put("medicoNombre",texto(rs,"medico_nombres")+" "+texto(rs,"medico_apellidos"));
        data.put("sedeYConsultorio",texto(rs,"sede"));data.put("canalReserva",texto(rs,"canal_aviso"));
        data.put("origen",rs.getBoolean("reasignada")?"Lista de espera":"Reserva");
        LocalDate proxima=rs.getObject("fecha_recomendada_proxima",LocalDate.class);
        data.put("fechaRecomendadaProxima",proxima==null?"":proxima.toString());
        LocalDateTime registro=rs.getObject("fecha_creacion",LocalDateTime.class);
        data.put("fechaRegistro",registro==null?"":registro.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        LocalDate nacimiento=rs.getObject("fecha_nacimiento",LocalDate.class);
        data.put("edad",nacimiento==null?SIN_REGISTRAR:Period.between(nacimiento,LocalDate.now(clock)).getYears()+" años");
        return data;
    }
    private static String texto(ResultSet rs,String campo)throws SQLException{return java.util.Objects.toString(rs.getString(campo),"");}
}
