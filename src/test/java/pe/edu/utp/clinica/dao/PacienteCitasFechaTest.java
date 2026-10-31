package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class PacienteCitasFechaTest {
    private CitaAutorizacionTest fixture;
    private CitaDAO dao;
    @BeforeEach void preparar()throws Exception {
        fixture=new CitaAutorizacionTest();fixture.setUp();dao=fixture.dao;
    }
    private void sql(String query)throws SQLException {
        try(var conn=fixture.abrir();var st=conn.createStatement()){st.execute(query);}
    }
    @Test void unaConfirmadaDeAyerVaAPasadasSinInventarInasistencia()throws Exception {
        sql("UPDATE cita SET fecha_hora='2026-10-27 09:00:00'");
        assertTrue(dao.obtenerProximasCitasPaciente(1).isEmpty());
        var pasada=dao.obtenerHistorialPaciente(1).get(0);
        assertEquals("CONFIRMADA",pasada.get("estado"));assertEquals(true,pasada.get("pendienteCierre"));
        assertEquals("Pasada · asistencia por registrar",pasada.get("estadoTexto"));
        assertEquals(1,fixture.count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA'"));
        assertFalse(dao.actualizarEstadoCitaPaciente(7,1,"CANCELADA"));
    }
    @Test void hoyPermaneceActivaYUnaConsultaEnCursoConservaSuEstado()throws Exception {
        assertEquals(1,dao.obtenerProximasCitasPaciente(1).size());assertTrue(dao.obtenerHistorialPaciente(1).isEmpty());
        sql("UPDATE cita SET fecha_hora='2026-10-27 23:50:00',estado='EN_ATENCION'");
        assertEquals(1,dao.obtenerProximasCitasPaciente(1).size());assertTrue(dao.obtenerHistorialPaciente(1).isEmpty());
    }
    @Test void laClasificacionUsaElDiaDeLimaAunqueElRelojSeaUtc()throws Exception {
        var madrugada=new CitaDAO(){
            @Override protected Connection abrirConexion()throws SQLException{return fixture.abrir();}
            @Override protected Clock relojClinico(){return Clock.fixed(Instant.parse("2026-10-29T03:00:00Z"),ZoneOffset.UTC);}
        };
        assertEquals(1,madrugada.obtenerProximasCitasPaciente(1).size());
        assertTrue(madrugada.obtenerHistorialPaciente(1).isEmpty());
    }
    @Test void nuncaMezclaPacientesNiPresentaUnaCanceladaComoPendiente()throws Exception {
        sql("UPDATE cita SET fecha_hora='2026-10-27 09:00:00',estado='CANCELADA'");
        assertTrue(dao.obtenerProximasCitasPaciente(1).isEmpty());assertTrue(dao.obtenerHistorialPaciente(1).isEmpty());
        assertEquals(1,dao.obtenerCitasCanceladasPaciente(1).size());
        assertTrue(dao.obtenerHistorialPaciente(2).isEmpty());
    }
}
