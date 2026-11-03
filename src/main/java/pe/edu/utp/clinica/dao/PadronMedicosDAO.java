package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class PadronMedicosDAO {
    public record Filtro(String buscar, String especialidad, String sede, String estado) {
        public Filtro {
            buscar = limpio(buscar); especialidad = limpio(especialidad); sede = limpio(sede); estado = limpio(estado);
            if ("Todos".equalsIgnoreCase(estado)) estado = "";
            if (buscar.length() > 100 || especialidad.length() > 100 || sede.length() > 100 ||
                    !Set.of("", "ACTIVO", "LICENCIA", "INACTIVO").contains(estado)) {
                throw new AgendaException(400,"FILTRO_INVALIDO","Revisa los filtros del padrón.");
            }
        }
        private static String limpio(String valor) { return valor == null ? "" : valor.strip(); }
    }
    public record Medico(int id, String nombre, String cmp, int especialidadId, String especialidad, String sede, String estado) {}
    public record Especialidad(int id, String nombre) {}
    public record Edicion(Medico medico, List<Especialidad> especialidades) {}
    private static final String SELECT_MEDICO = "SELECT m.id_medico,CONCAT(u.nombres,' ',u.apellidos),m.cmp,m.id_especialidad,e.nombre,s.nombre,CASE WHEN u.activo=FALSE THEN 'INACTIVO' ELSE m.estado END FROM medico m JOIN usuario u ON u.id_usuario=m.id_usuario JOIN especialidad e ON e.id_especialidad=m.id_especialidad JOIN sede s ON s.id_sede=m.id_sede ";
    private static final String LISTAR = SELECT_MEDICO + "WHERE m.id_sede=? AND (?='' OR LOWER(CONCAT(u.nombres,' ',u.apellidos)) LIKE LOWER(?) ESCAPE '!' OR LOWER(m.cmp) LIKE LOWER(?) ESCAPE '!') AND (?='' OR e.nombre=?) AND (?='' OR s.nombre=?) AND (?='' OR CASE WHEN u.activo=FALSE THEN 'INACTIVO' ELSE m.estado END=?) ORDER BY m.id_medico";
    private static final String CONSULTAR = SELECT_MEDICO + "WHERE m.id_medico=? AND m.id_sede=? FOR UPDATE";
    private final AgendaCitaDAO.ConnectionSource conexiones;
    public PadronMedicosDAO() { this(DatabaseConfig::getConnection); }
    PadronMedicosDAO(AgendaCitaDAO.ConnectionSource conexiones) { this.conexiones=conexiones; }

    public List<Medico> listar(int actor, Filtro filtro) {
        return TransaccionJdbc.ejecutar(conexiones,c->{
            int sede=AmbitoAdmisionDAO.sede(c,actor);
            var lista=new ArrayList<Medico>();
            try(var st=c.prepareStatement(LISTAR)) {
                String patron="%"+filtro.buscar().replace("!","!!").replace("%","!%").replace("_","!_")+"%";
                st.setInt(1,sede); st.setString(2,filtro.buscar()); st.setString(3,patron); st.setString(4,patron);
                st.setString(5,filtro.especialidad()); st.setString(6,filtro.especialidad());
                st.setString(7,filtro.sede()); st.setString(8,filtro.sede()); st.setString(9,filtro.estado()); st.setString(10,filtro.estado());
                try(var r=st.executeQuery()) { while(r.next()) lista.add(mapear(r)); }
            }
            return List.copyOf(lista);
        });
    }
    public Edicion consultar(int actor,int id) {
        return TransaccionJdbc.ejecutar(conexiones,c->{
            var medico=bloquear(c,id,AmbitoAdmisionDAO.sede(c,actor));
            var especialidades=new ArrayList<Especialidad>();
            try(var st=c.prepareStatement("SELECT id_especialidad,nombre FROM especialidad WHERE estado='ACTIVO' OR id_especialidad=? ORDER BY nombre")) {
                st.setInt(1,medico.especialidadId());
                try(var r=st.executeQuery()) { while(r.next()) especialidades.add(new Especialidad(r.getInt(1),r.getString(2))); }
            }
            return new Edicion(medico,List.copyOf(especialidades));
        });
    }
    public void guardar(int actor,int id,int especialidad,String estado,String ip) {
        if(especialidad<1 || !Set.of("ACTIVO","LICENCIA","INACTIVO").contains(estado==null?"":estado))
            throw new AgendaException(400,"DATOS_INVALIDOS","Selecciona una especialidad y un estado válidos.");
        TransaccionJdbc.ejecutar(conexiones,c->{
            int sede=AmbitoAdmisionDAO.sede(c,actor);
            var anterior=bloquear(c,id,sede);
            if(anterior.especialidadId()==especialidad && anterior.estado().equals(estado)) return null;
            if(!anterior.estado().equals(estado) && "ACTIVO".equals(estado)) exigirCuentaActiva(c,id);
            if(anterior.especialidadId()!=especialidad) exigirEspecialidad(c,id,especialidad);
            if(!"ACTIVO".equals(estado) || anterior.especialidadId()!=especialidad) exigirAgendaLibre(c,id);
            try(var st=c.prepareStatement("UPDATE medico SET id_especialidad=?,estado=? WHERE id_medico=? AND id_sede=?")) {
                st.setInt(1,especialidad); st.setString(2,estado); st.setInt(3,id); st.setInt(4,sede); st.executeUpdate();
            }
            try(var st=c.prepareStatement("INSERT INTO auditoria_seguridad(id_usuario,ip_origen,evento,descripcion,sospechoso) VALUES(?,?,'MEDICO_PROFESIONAL_EDITADO',?,FALSE)")) {
                st.setInt(1,actor); st.setString(2,ip); st.setString(3,"Médico #"+id+"; especialidad "+anterior.especialidadId()+" → "+especialidad+"; estado "+anterior.estado()+" → "+estado); st.executeUpdate();
            }
            TransaccionJdbc.avisarSede(sede);
            return null;
        });
    }
    private static Medico bloquear(Connection c,int id,int sede)throws SQLException {
        try(var st=c.prepareStatement(CONSULTAR)) {
            st.setInt(1,id);st.setInt(2,sede);
            try(var r=st.executeQuery()) {
                if(!r.next()) throw new AgendaException(404,"MEDICO_NO_ENCONTRADO","No se encontró el médico en tu sede.");
                return mapear(r);
            }
        }
    }
    private static Medico mapear(java.sql.ResultSet r)throws SQLException {
        return new Medico(r.getInt(1),r.getString(2),r.getString(3),r.getInt(4),r.getString(5),r.getString(6),r.getString(7));
    }
    private static void exigirCuentaActiva(Connection c,int id)throws SQLException {
        try(var st=c.prepareStatement("SELECT u.activo FROM usuario u JOIN medico m ON m.id_usuario=u.id_usuario WHERE m.id_medico=?")) {
            st.setInt(1,id);try(var r=st.executeQuery()) { if(!r.next() || !r.getBoolean(1)) throw new AgendaException(409,"CUENTA_INACTIVA","El administrador debe habilitar la cuenta antes de activar el perfil médico."); }
        }
    }
    private static void exigirEspecialidad(Connection c,int id,int especialidad)throws SQLException {
        try(var st=c.prepareStatement("SELECT id_especialidad FROM especialidad WHERE id_especialidad=? AND estado='ACTIVO' FOR UPDATE")) {
            st.setInt(1,especialidad);try(var r=st.executeQuery()) { if(!r.next()) throw new AgendaException(409,"ESPECIALIDAD_INACTIVA","Selecciona una especialidad activa."); }
        }
        try(var st=c.prepareStatement("SELECT id_cita FROM cita WHERE id_medico=? LIMIT 1")) {
            st.setInt(1,id);try(var r=st.executeQuery()) { if(r.next()) throw new AgendaException(409,"HISTORIAL_PROTEGIDO","No se cambia la especialidad de un médico con citas registradas. Coordina una nueva asignación con el administrador."); }
        }
    }
    private static void exigirAgendaLibre(Connection c,int id)throws SQLException {
        LocalDateTime ahora=LocalDateTime.now(ZoneId.of("America/Lima"));
        try(var st=c.prepareStatement("SELECT m.id_medico FROM medico m WHERE m.id_medico=? AND (EXISTS(SELECT 1 FROM disponibilidad d WHERE d.id_medico=m.id_medico AND d.estado='PUBLICADO' AND d.fecha>=?) OR EXISTS(SELECT 1 FROM cita ci WHERE ci.id_medico=m.id_medico AND (ci.estado IN ('EN_SALA','EN_ATENCION') OR (ci.fecha_hora>? AND ci.estado IN ('SOLICITADA','CONFIRMADA')))))")) {
            st.setInt(1,id);st.setObject(2,ahora.toLocalDate());st.setObject(3,ahora);
            try(var r=st.executeQuery()) { if(r.next()) throw new AgendaException(409,"MEDICO_CON_AGENDA","Coordina las citas y los horarios publicados antes de realizar este cambio."); }
        }
    }
}
