package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.util.EnlaceCita;
import static org.junit.jupiter.api.Assertions.*;

class EnlaceCitaDAOTest {
    private CitaAutorizacionTest esquema;
    private EnlaceCitaDAO enlaces;
    protected CitaAutorizacionTest crearEsquema(){return new CitaAutorizacionTest();}
    private void ejecutar(String sql)throws Exception{try(var c=esquema.abrir();var s=c.createStatement()){s.execute(sql);}}
    @BeforeEach void preparar()throws Exception {
        esquema=crearEsquema();esquema.setUp();
        ejecutar("UPDATE cita SET fecha_hora='2026-10-30 09:00:00' WHERE id_cita=7");
        enlaces=new EnlaceCitaDAO(esquema::abrir,esquema.reloj());
    }
    @Test void consultarNoConfirmaNiCancelaYSoloMuestraUnaCitaPropia()throws Exception {
        var cita=enlaces.consultar(10,7);assertEquals(7,cita.id());assertTrue(cita.futura());assertFalse(cita.asistenciaConfirmada());
        assertEquals(1,esquema.count("SELECT COUNT(*) FROM cita WHERE id_cita=7 AND estado='CONFIRMADA'"));
        assertEquals(0,esquema.count("SELECT COUNT(*) FROM confirmacion_asistencia WHERE confirmada_en IS NOT NULL"));
        assertEquals(404,assertThrows(AgendaException.class,()->enlaces.consultar(99,7)).status());
    }
    @Test void confirmarUsaLaCuentaAutenticadaYEsIdempotente()throws Exception {
        var confirmar=new EnlaceCita(7,EnlaceCita.Accion.CONFIRMAR);
        enlaces.ejecutar(10,confirmar);enlaces.ejecutar(10,confirmar);
        assertEquals(1,esquema.count("SELECT COUNT(*) FROM confirmacion_asistencia WHERE id_cita=7 AND id_usuario_confirmo=10 AND canal='WEB'"));
        assertEquals(1,esquema.count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA'"));
    }
    @Test void cancelarExigePropiedadYNoPermiteRepetirLaAccion()throws Exception {
        var cancelar=new EnlaceCita(7,EnlaceCita.Accion.CANCELAR);
        assertEquals(404,assertThrows(AgendaException.class,()->enlaces.ejecutar(99,cancelar)).status());
        enlaces.ejecutar(10,cancelar);
        assertEquals(1,esquema.count("SELECT COUNT(*) FROM cita WHERE estado='CANCELADA'"));
        assertEquals(409,assertThrows(AgendaException.class,()->enlaces.ejecutar(10,cancelar)).status());
    }
    @Test void citaYaIniciadaYPlazoVencidoSeRevalidanAlEjecutar()throws Exception {
        var confirmar=new EnlaceCita(7,EnlaceCita.Accion.CONFIRMAR);
        ejecutar("UPDATE confirmacion_asistencia SET vence_en='2026-10-28 10:00:00' WHERE id_cita=7");
        assertEquals("PLAZO_CONFIRMACION_VENCIDO",assertThrows(AgendaException.class,()->enlaces.ejecutar(10,confirmar)).code());
        ejecutar("UPDATE cita SET fecha_hora='2026-10-28 10:00:00' WHERE id_cita=7");
        var cancelar=new EnlaceCita(7,EnlaceCita.Accion.CANCELAR);
        assertEquals(409,assertThrows(AgendaException.class,()->enlaces.ejecutar(10,cancelar)).status());
    }
    @Test void cuentaInactivaNoPuedeUsarUnEnlaceAnterior()throws Exception {
        ejecutar("UPDATE usuario SET activo=FALSE WHERE id_usuario=10");
        assertEquals(404,assertThrows(AgendaException.class,()->enlaces.consultar(10,7)).status());
    }
}
