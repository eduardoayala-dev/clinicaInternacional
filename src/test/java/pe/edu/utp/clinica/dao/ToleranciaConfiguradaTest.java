package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.*;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

class ToleranciaConfiguradaTest {
    private CitaAutorizacionTest esquema;
    private AdmisionCitaDAO admision;
    protected CitaAutorizacionTest crearEsquema(){return new CitaAutorizacionTest();}
    private Connection abrir()throws SQLException{return esquema.abrir();}
    private void ejecutar(String sql)throws SQLException{try(var c=abrir();var s=c.createStatement()){s.execute(sql);}}
    @BeforeEach void preparar()throws Exception {
        esquema=crearEsquema();esquema.setUp();admision=new AdmisionCitaDAO(this::abrir,esquema.reloj());
        ejecutar("INSERT INTO usuario(id_usuario,nombres,rol) VALUES(40,'Admisión','PERSONAL_ADMINISTRATIVO')");
        ejecutar("INSERT INTO personal_administrativo(id_personal,id_usuario,id_sede) VALUES(1,40,1)");
    }
    @Test void unaNuevaLlegadaUsaLaToleranciaActualSinReescribirLaAnterior()throws Exception {
        ejecutar("UPDATE configuracion_ofertas SET tolerancia_llegada=120");
        var anterior=admision.registrar(40,7);assertFalse(anterior.llegoTarde());assertEquals(120,anterior.toleranciaMinutos());
        ejecutar("UPDATE configuracion_ofertas SET tolerancia_llegada=0");
        ejecutar("INSERT INTO cita(id_cita,id_paciente,id_medico,fecha_hora,estado) VALUES(8,1,1,'2026-10-28 09:55:00','CONFIRMADA')");
        assertTrue(admision.registrar(40,8).llegoTarde());
        try(var c=abrir();var s=c.createStatement();var rs=s.executeQuery("SELECT tolerancia_minutos FROM admision_cita WHERE id_cita=7")){assertTrue(rs.next());assertEquals(120,rs.getInt(1));}
    }
    @Test void ausenciaRespetaElParametroActualYPersisteLaReglaUsada()throws Exception {
        ejecutar("UPDATE configuracion_ofertas SET tolerancia_llegada=120");
        assertEquals(409,assertThrows(AgendaException.class,()->admision.registrarInasistencia(40,7,"Ausencia comprobada")).status());
        ejecutar("UPDATE configuracion_ofertas SET tolerancia_llegada=5");admision.registrarInasistencia(40,7,"Ausencia comprobada");
        try(var c=abrir();var s=c.createStatement();var rs=s.executeQuery("SELECT tolerancia_minutos FROM inasistencia_cita WHERE id_cita=7")){assertTrue(rs.next());assertEquals(5,rs.getInt(1));}
    }
}
