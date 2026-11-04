package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="AGENDA_MYSQL_URL",matches=".+")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CicloVidaCitaMySqlTest {
    @Test @Order(7) void migracionSedeConservaInvitacionYUsaSoloAsignacionExistente()throws Exception{
        var invitaciones=new InvitacionPersonalDAO(MySqlPruebas::abrir,hora("2026-11-04T15:00:00Z"));
        int id=invitaciones.crear(1,new InvitacionPersonalDAO.Solicitud("Médico","Migración","medico.migracion@example.test","MEDICO","730001",1,1,null),pe.edu.utp.clinica.auth.SecretosSesion.hash("solo-prueba")).usuario();
        try(var c=abrir();var st=c.createStatement()){
            st.execute("ALTER TABLE invitacion_personal DROP COLUMN id_sede_destino");
            String migracion=Files.readString(Path.of("sql/migrations/V022__sede_destino_invitacion_medica.sql"));
            for(int repeticion=0;repeticion<2;repeticion++)for(String sentencia:migracion.split(";"))if(!sentencia.isBlank())st.execute(sentencia);
        }
        assertEquals(1,cantidad("SELECT COUNT(*) FROM invitacion_personal WHERE id_usuario="+id+" AND id_sede_destino=1 AND utilizada_en IS NULL"));
        var semana=new MedicoDAO(MySqlPruebas::abrir,hora("2026-11-04T13:00:00Z")).disponibilidadSemanal(1,1,1,java.time.LocalDate.of(2026,11,2));
        assertEquals(7,semana.size());assertEquals(5,semana.stream().filter(d->d.fecha().equals("2026-11-05")).findFirst().orElseThrow().manana());
    }
    @Test @Order(6) void codigoPorInicialesNoColisionaEntreAdministradoresEnMySql()throws Exception{
        int otro;
        try(var c=abrir();var s=c.createStatement()){
            s.executeUpdate("INSERT INTO usuario(nombres,apellidos,email,password_hash,rol,activo) VALUES('Admin','Código','codigo.prueba@example.test','sin-acceso-compartido','ADMINISTRADOR',TRUE)",java.sql.Statement.RETURN_GENERATED_KEYS);
            try(var r=s.getGeneratedKeys()){assertTrue(r.next());otro=r.getInt(1);}
            s.execute("INSERT INTO administrador(id_usuario,estado) VALUES("+otro+",'ACTIVO')");
        }
        int antes=cantidad("SELECT COUNT(*) FROM sede");var dao=new CatalogoSedesDAO(MySqlPruebas::abrir);
        var ejecutor=java.util.concurrent.Executors.newFixedThreadPool(2);var inicio=new java.util.concurrent.CountDownLatch(1);
        try{
            var una=ejecutor.submit(()->{inicio.await();dao.guardar(1,new CatalogoSedesDAO.Sede(0,null,"San Boreal","Avenida de prueba 123","016102222","ACTIVO"),null);return true;});
            var dos=ejecutor.submit(()->{inicio.await();dao.guardar(otro,new CatalogoSedesDAO.Sede(0,null,"San Bonifacio","Avenida de prueba 123","016102222","ACTIVO"),null);return true;});
            inicio.countDown();assertTrue(una.get(30,java.util.concurrent.TimeUnit.SECONDS));assertTrue(dos.get(30,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(antes+2,cantidad("SELECT COUNT(*) FROM sede"));assertEquals(0,cantidad("SELECT COUNT(*) FROM (SELECT codigo FROM sede GROUP BY codigo HAVING COUNT(*)>1) duplicados"));
            assertTrue(dao.listar(1).stream().filter(s->s.nombre().equals("San Boreal")||s.nombre().equals("San Bonifacio")).allMatch(s->s.codigo().startsWith("SED-SB-")));
        }finally{ejecutor.shutdownNow();}
    }
    @Test @Order(5) void panelGlobalUsaElEsquemaRealYLosEventosDelCiclo()throws Exception {
        var panel=new PanelSistemaDAO(MySqlPruebas::abrir,hora("2026-11-08T15:00:00Z")).consultar(1,new PanelSistemaDAO.Filtro(java.time.LocalDate.of(2026,11,1),java.time.LocalDate.of(2026,11,30),0,0));
        assertEquals(cantidad("SELECT COUNT(*) FROM cita WHERE fecha_hora>='2026-11-01' AND fecha_hora<'2026-12-01'"),panel.datos().citas());
        assertTrue(panel.datos().citas()>0);assertTrue(panel.datos().atendidas()>0);assertTrue(panel.datos().inasistencias()>0);assertTrue(panel.datos().reasignadas()>0);
        assertEquals(cantidad("SELECT COUNT(*) FROM sede"),panel.sedes().size());assertFalse(panel.auditoria().isEmpty());
        assertEquals(panel.datos().citas(),panel.semanas().stream().mapToLong(PanelSistemaDAO.Semana::citas).sum());
    }
    private String fecha="2026-11-05";
    private Clock reloj=hora("2026-11-04T13:00:00Z");
    private static Connection abrir()throws Exception { return MySqlPruebas.abrir(); }
    private static Clock hora(String instante) { return Clock.fixed(Instant.parse(instante),ZoneId.of("America/Lima")); }
    private int cantidad(String sql)throws Exception {
        try(var c=abrir();var s=c.createStatement();var r=s.executeQuery(sql)) { r.next();return r.getInt(1); }
    }
    @BeforeAll static void prepararEsquemaRealAislado()throws Exception {
        MySqlPruebas.validarUrl(System.getenv("AGENDA_MYSQL_URL"));
        try(var c=abrir();var s=c.createStatement()) {
            assertTrue(c.getCatalog().startsWith("clinica_agendas_test_"));
            s.execute("SET FOREIGN_KEY_CHECKS=0");
            EsquemaPruebas.limpiar(c);
            String esquema=Files.readString(Path.of("sql/schema.sql")).replaceAll("(?m)^\\s*--[^\\r\\n]*","");
            for(String sentencia:esquema.split(";")) {
                String sql=sentencia.strip();
                if(sql.isEmpty() || sql.startsWith("CREATE DATABASE") || sql.startsWith("USE ")) continue;
                s.execute(sql);
            }
        }
    }
    private AgendaCitaDAO agenda() { return new AgendaCitaDAO(MySqlPruebas::abrir,reloj,false); }
    @Test @Order(1) void migracionConservaLasIpRegistradasYAdmiteEventosInternos()throws Exception {
        try(var c=abrir();var s=c.createStatement()) {
            s.execute("ALTER TABLE auditoria_seguridad MODIFY COLUMN ip_origen VARCHAR(45) NOT NULL");
            s.execute("INSERT INTO auditoria_seguridad(id_usuario,ip_origen,evento) VALUES(1,'2001:db8::1','PRUEBA_CON_IP')");
            s.execute(Files.readString(Path.of("sql/migrations/V021__auditoria_ip_opcional.sql")));
            s.execute("INSERT INTO auditoria_seguridad(id_usuario,evento) VALUES(1,'PRUEBA_SIN_IP')");
        }
        assertEquals(1,cantidad("SELECT COUNT(*) FROM auditoria_seguridad WHERE ip_origen='2001:db8::1'"));
        assertEquals(1,cantidad("SELECT COUNT(*) FROM auditoria_seguridad WHERE evento='PRUEBA_SIN_IP' AND ip_origen IS NULL"));
    }
    private CitaDAO paciente() {
        return new CitaDAO() {
            @Override protected Connection abrirConexion()throws java.sql.SQLException { return MySqlPruebas.abrir(); }
            @Override protected Clock relojClinico() { return reloj; }
            @Override protected boolean correosActivos() { return false; }
        };
    }
    private int reservar(String inicio) {
        agenda().publicar(1,1,fecha,"09:00","12:00",30);
        var retencion=agenda().retener(1,1,fecha,inicio);
        return agenda().reservarRetenida(1,1,fecha,inicio,null,retencion.identificador());
    }
    @Test @Order(2) void reservaConfirmaLlegaSeAtiendeYCierraSinPermitirCancelarDespues()throws Exception {
        int cita=reservar("09:00");
        assertEquals(0,cantidad("SELECT COUNT(*) FROM retencion_cupo"));
        new ConfirmacionAsistenciaDAO(MySqlPruebas::abrir,reloj).confirmarPaciente(3,cita);
        assertEquals(1,cantidad("SELECT COUNT(*) FROM confirmacion_asistencia WHERE id_cita="+cita+" AND confirmada_en IS NOT NULL"));
        reloj=hora("2026-11-05T13:55:00Z");
        new AdmisionCitaDAO(MySqlPruebas::abrir,reloj,10).registrar(2,cita);
        reloj=hora("2026-11-05T14:00:00Z");
        assertTrue(new AtencionCitaDAO(MySqlPruebas::abrir,reloj).iniciar(1,cita));
        reloj=hora("2026-11-05T14:20:00Z");
        assertTrue(new AtencionCitaDAO(MySqlPruebas::abrir,reloj).finalizar(1,cita,"MEDIA","","Consulta de prueba completada"));
        assertEquals(1,cantidad("SELECT COUNT(*) FROM cita WHERE id_cita="+cita+" AND estado='ATENDIDA'"));
        assertEquals(1,cantidad("SELECT COUNT(*) FROM historial_asistencia WHERE id_cita="+cita+" AND asistio=TRUE"));
        assertFalse(paciente().actualizarEstadoCitaPaciente(cita,1,"CANCELADA","Imprevisto personal"));
    }
    @Test @Order(3) void reprogramaCancelaConMotivoYReasignaElCupoAListaDeEspera()throws Exception {
        fecha="2026-11-06";
        int anterior=reservar("09:00");
        assertTrue(agenda().reprogramar(anterior,1,1,fecha,"09:30",null,"MEDIA"));
        int nueva=cantidad("SELECT MAX(id_cita) FROM cita");
        assertEquals(1,cantidad("SELECT COUNT(*) FROM cita WHERE id_cita="+anterior+" AND estado='REPROGRAMADA'"));
        new ListaEsperaDAO(MySqlPruebas::abrir,reloj).registrar(2,1,1,"MANANA","MEDIA","CORREO",180);
        assertTrue(paciente().actualizarEstadoCitaPaciente(nueva,1,"CANCELADA","Imprevisto personal"));
        assertEquals(1,cantidad("SELECT COUNT(*) FROM auditoria_seguridad WHERE evento='CITA_CANCELADA_PACIENTE' AND descripcion LIKE '%Imprevisto personal%'"));
        var ofertas=new OfertasDAO(MySqlPruebas::abrir,reloj,false);
        int oferta=ofertas.generar(nueva);assertTrue(oferta>0);
        int asignada=ofertas.aceptar(2,oferta);
        assertEquals(1,cantidad("SELECT COUNT(*) FROM cita WHERE id_cita="+asignada+" AND id_paciente=2 AND estado='CONFIRMADA'"));
        assertEquals(1,cantidad("SELECT COUNT(*) FROM reasignacion_inteligente WHERE id_reasignacion="+oferta+" AND estado='ACEPTADA'"));
    }
    @Test @Order(4) void inasistenciaRequiereVerificacionTrasLaTolerancia()throws Exception {
        fecha="2026-11-07";
        int cita=reservar("10:00");
        reloj=hora("2026-11-07T15:20:00Z");
        new AdmisionCitaDAO(MySqlPruebas::abrir,reloj,10).registrarInasistencia(2,cita,"Se verificó que el paciente no llegó");
        assertEquals(1,cantidad("SELECT COUNT(*) FROM cita WHERE id_cita="+cita+" AND estado='NO_ASISTIO'"));
        assertEquals(0,cantidad("SELECT COUNT(*) FROM atencion_cita WHERE id_cita="+cita));
    }
}
