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
