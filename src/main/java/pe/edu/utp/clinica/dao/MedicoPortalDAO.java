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
import java.util.Set;

public class MedicoPortalDAO {
    private static final ZoneId LIMA=ZoneId.of("America/Lima");
    private static final Set<String> MODIFICABLES=Set.of("CONFIRMADA","EN_SALA");
    private static final String PERFIL="SELECT m.id_medico,m.cmp,u.nombres,u.apellidos,u.email,e.nombre AS especialidad,s.nombre AS sedeNombre FROM medico m JOIN usuario u ON m.id_usuario=u.id_usuario JOIN especialidad e ON m.id_especialidad=e.id_especialidad JOIN sede s ON m.id_sede=s.id_sede WHERE m.id_usuario=? AND m.estado='ACTIVO' AND u.activo=TRUE AND u.rol='MEDICO' AND e.estado='ACTIVO' AND s.estado='ACTIVO'";
    private static final String DATOS="SELECT c.id_cita,c.id_paciente,c.fecha_hora,c.estado,c.nivel_urgencia,c.fecha_recomendada_proxima,c.fecha_creacion,p.dni,p.telefono,p.fecha_nacimiento,p.direccion,p.alergias,p.tipo_cobertura,p.canal_aviso,u.nombres,u.apellidos,u.email,mc.descripcion AS motivo,e.nombre AS especialidad,s.nombre AS sede,m.cmp,um.nombres AS medico_nombres,um.apellidos AS medico_apellidos,"
            + "(SELECT h.observacion FROM historial_asistencia h WHERE h.id_cita=c.id_cita ORDER BY h.id_historial DESC LIMIT 1) AS indicaciones,"
            + "EXISTS(SELECT 1 FROM reasignacion_inteligente r WHERE r.id_cita_asignada=c.id_cita AND r.estado='ACEPTADA') AS reasignada "
            + "FROM cita c JOIN paciente p ON c.id_paciente=p.id_paciente JOIN usuario u ON p.id_usuario=u.id_usuario JOIN medico m ON c.id_medico=m.id_medico JOIN usuario um ON m.id_usuario=um.id_usuario JOIN especialidad e ON m.id_especialidad=e.id_especialidad JOIN sede s ON m.id_sede=s.id_sede LEFT JOIN motivo_cita mc ON c.id_motivo=mc.id_motivo ";
    private final AgendaCitaDAO.ConnectionSource connections;
    private final Clock clock;
    public MedicoPortalDAO(){this(DatabaseConfig::getConnection,Clock.system(LIMA));}
    MedicoPortalDAO(AgendaCitaDAO.ConnectionSource connections,Clock clock){this.connections=connections;this.clock=clock;}

    public Map<String,Object> perfil(int usuario){
        try(Connection conn=connections.open();PreparedStatement ps=conn.prepareStatement(PERFIL)){
            ps.setInt(1,usuario);
            try(ResultSet rs=ps.executeQuery()){
                if(!rs.next()) throw new AgendaException(403,"MEDICO_NO_HABILITADO","El perfil médico no está habilitado.");
                Map<String,Object> data=new LinkedHashMap<>();
                data.put("idMedico",rs.getInt("id_medico"));
                data.put("nombreMedico",texto(rs,"nombres")+" "+texto(rs,"apellidos"));
                for(String campo:List.of("cmp","email","especialidad","sedeNombre")) data.put(campo,texto(rs,campo));
                return data;
            }
        }catch(SQLException e){throw new CatalogoDatosException(e);}
    }

    public List<Map<String,Object>> agenda(int medico,LocalDate fecha){
        if(medico<=0 || fecha==null) throw new AgendaException(400,"DATOS_INVALIDOS","Selecciona una fecha válida.");
        String sql="SELECT c.id_cita,c.fecha_hora,c.estado,p.dni,u.nombres,u.apellidos,mc.descripcion AS motivo,EXISTS(SELECT 1 FROM reasignacion_inteligente r WHERE r.id_cita_asignada=c.id_cita AND r.estado='ACEPTADA') AS reasignada FROM cita c JOIN paciente p ON c.id_paciente=p.id_paciente JOIN usuario u ON p.id_usuario=u.id_usuario LEFT JOIN motivo_cita mc ON c.id_motivo=mc.id_motivo WHERE c.id_medico=? AND c.fecha_hora>=? AND c.fecha_hora<? ORDER BY c.fecha_hora,c.id_cita";
        List<Map<String,Object>> rows=new ArrayList<>();
        try(Connection conn=connections.open();PreparedStatement ps=conn.prepareStatement(sql)){
            ps.setInt(1,medico);ps.setObject(2,fecha.atStartOfDay());ps.setObject(3,fecha.plusDays(1).atStartOfDay());
            try(ResultSet rs=ps.executeQuery()){
                while(rs.next()){
                    Map<String,Object> data=base(rs);
                    data.put("paciente",texto(rs,"nombres")+" "+texto(rs,"apellidos"));data.put("dni",texto(rs,"dni"));
                    data.put("motivo",texto(rs,"motivo"));data.put("origen",rs.getBoolean("reasignada")?"Lista de espera":"Reserva");
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
        if(medico<=0 || cita<=0 || !Set.of("ATENDIDA","NO_ASISTIO").contains(estado==null?"":estado)) throw invalida();
        boolean asistio="ATENDIDA".equals(estado);
        LocalDate proxima=validarSeguimiento(asistio,prioridad,fecha,notas);
        return TransaccionJdbc.ejecutar(connections,conn->{
            int paciente=propietario(conn,medico,cita);
            try(PreparedStatement ps=conn.prepareStatement("SELECT id_paciente FROM paciente WHERE id_paciente=? FOR UPDATE")){
                ps.setInt(1,paciente);try(ResultSet rs=ps.executeQuery()){if(!rs.next()) throw inexistente();}
            }
            validarUsuario(conn,medico);
            AgendaCitaDAO.medico(conn,medico);
            String actual;LocalDateTime inicio;
            try(PreparedStatement ps=conn.prepareStatement("SELECT estado,fecha_hora FROM cita WHERE id_cita=? AND id_medico=? FOR UPDATE")){
                ps.setInt(1,cita);ps.setInt(2,medico);
                try(ResultSet rs=ps.executeQuery()){
                    if(!rs.next()) throw inexistente();actual=rs.getString(1);inicio=rs.getObject(2,LocalDateTime.class);
                }
            }
            if(!editable(actual,inicio)) throw new AgendaException(409,"ATENCION_NO_DISPONIBLE","Solo puedes cerrar una cita confirmada o en sala el día de su atención, a partir de su hora.");
            String update=asistio?"UPDATE cita SET estado='ATENDIDA',nivel_urgencia=?,fecha_recomendada_proxima=? WHERE id_cita=? AND id_medico=?"
                    :"UPDATE cita SET estado='NO_ASISTIO' WHERE id_cita=? AND id_medico=?";
            try(PreparedStatement ps=conn.prepareStatement(update)){
                int i=1;
                if(asistio){ps.setString(i++,prioridad);ps.setObject(i++,proxima);}
                ps.setInt(i++,cita);ps.setInt(i,medico);
                if(ps.executeUpdate()!=1) throw inexistente();
            }
            try(PreparedStatement ps=conn.prepareStatement("INSERT INTO historial_asistencia(id_paciente,id_cita,asistio,observacion,fecha_registro) VALUES(?,?,?,?,?)")){
                ps.setInt(1,paciente);ps.setInt(2,cita);ps.setBoolean(3,asistio);
                String observacion=java.util.Objects.toString(notas,"").trim();
                if(!asistio) observacion="Inasistencia registrada por el médico asignado.";
                ps.setString(4,observacion);
                ps.setObject(5,LocalDateTime.now(clock));
                if(ps.executeUpdate()!=1) throw new SQLException("No se guardó el registro de atención");
            }
            return true;
        });
    }

    private LocalDate validarSeguimiento(boolean asistio,String prioridad,String fecha,String notas){
        if(!asistio) return null;
        if(!Set.of("ALTA","MEDIA","BAJA").contains(prioridad==null?"":prioridad)) throw invalida();
        if(notas!=null && (notas.length()>200 || notas.chars().anyMatch(c->Character.isISOControl(c) && c!='\n' && c!='\r' && c!='\t'))) throw invalida();
        if(fecha==null || fecha.isBlank()) return null;
        try{
            if(!fecha.matches("\\d{4}-\\d{2}-\\d{2}")) throw invalida();
            LocalDate proxima=LocalDate.parse(fecha);
            if(!proxima.isAfter(LocalDate.now(clock))) throw invalida();
            return proxima;
        }catch(java.time.DateTimeException e){throw invalida();}
    }
    private static int propietario(Connection conn,int medico,int cita)throws SQLException{
        try(PreparedStatement ps=conn.prepareStatement("SELECT id_paciente FROM cita WHERE id_cita=? AND id_medico=?")){
            ps.setInt(1,cita);ps.setInt(2,medico);
            try(ResultSet rs=ps.executeQuery()){if(!rs.next()) throw inexistente();return rs.getInt(1);}
        }
    }
    private static void validarUsuario(Connection conn,int medico)throws SQLException{
        try(PreparedStatement ps=conn.prepareStatement("SELECT u.id_usuario FROM usuario u JOIN medico m ON u.id_usuario=m.id_usuario WHERE m.id_medico=? AND u.activo=TRUE AND u.rol='MEDICO'")){
            ps.setInt(1,medico);try(ResultSet rs=ps.executeQuery()){if(!rs.next()) throw new AgendaException(403,"MEDICO_NO_HABILITADO","El perfil médico no está habilitado.");}
        }
    }
    private Map<String,Object> base(ResultSet rs)throws SQLException{
        Map<String,Object> data=new LinkedHashMap<>();
        LocalDateTime inicio=rs.getObject("fecha_hora",LocalDateTime.class);
        String estado=texto(rs,"estado");
        data.put("idCita",rs.getInt("id_cita"));
        data.put("fecha",inicio.toLocalDate().toString());data.put("hora",inicio.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        data.put("fechaFormateada",inicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        data.put("fechaHora",inicio.toString());data.put("estado",estado);data.put("editable",editable(estado,inicio));
        data.put("estadoTexto",switch(estado){
            case "ATENDIDA"->"Atendida";case "NO_ASISTIO"->"No asistió";case "EN_SALA"->"En sala";
            case "CANCELADA"->"Cancelada";case "REPROGRAMADA"->"Reprogramada";
            case "CONFIRMADA"->"Confirmada";case "SOLICITADA"->"Solicitada";default->estado;
        });
        return data;
    }
    private Map<String,Object> presentar(ResultSet rs)throws SQLException{
        Map<String,Object> data=base(rs);
        data.put("idPaciente",rs.getInt("id_paciente"));
        String nombre=texto(rs,"nombres")+" "+texto(rs,"apellidos");
        data.put("paciente",nombre);data.put("pacienteNombre",nombre);
        for(String campo:List.of("dni","telefono","email","direccion","alergias","especialidad","sede","cmp","motivo","indicaciones")) data.put(campo,texto(rs,campo));
        data.put("urgencia",texto(rs,"nivel_urgencia"));data.put("tipoCobertura",texto(rs,"tipo_cobertura"));
        data.put("medicoNombre",texto(rs,"medico_nombres")+" "+texto(rs,"medico_apellidos"));
        data.put("sedeYConsultorio",texto(rs,"sede"));data.put("canalReserva",texto(rs,"canal_aviso"));
        data.put("origen",rs.getBoolean("reasignada")?"Lista de espera":"Reserva");
        LocalDate proxima=rs.getObject("fecha_recomendada_proxima",LocalDate.class);
        data.put("fechaRecomendadaProxima",proxima==null?"":proxima.toString());
        LocalDateTime registro=rs.getObject("fecha_creacion",LocalDateTime.class);
        data.put("fechaRegistro",registro==null?"":registro.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        LocalDate nacimiento=rs.getObject("fecha_nacimiento",LocalDate.class);
        data.put("edad",nacimiento==null?"Sin registrar":Period.between(nacimiento,LocalDate.now(clock)).getYears()+" años");
        return data;
    }
    private boolean editable(String estado,LocalDateTime inicio){
        LocalDateTime ahora=LocalDateTime.now(clock);
        return MODIFICABLES.contains(estado) && inicio!=null && inicio.toLocalDate().equals(ahora.toLocalDate()) && !inicio.isAfter(ahora);
    }
    private static String texto(ResultSet rs,String campo)throws SQLException{return java.util.Objects.toString(rs.getString(campo),"");}
    private static AgendaException invalida(){return new AgendaException(400,"DATOS_INVALIDOS","Revisa la prioridad, la fecha futura y las indicaciones de hasta 200 caracteres.");}
    private static AgendaException inexistente(){return new AgendaException(404,"CITA_NO_ENCONTRADA","No se encontró una cita asignada a este médico.");}
}
