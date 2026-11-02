package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class PoliticaContactoTest {
    private CitaAutorizacionTest esquema;
    protected CitaAutorizacionTest crearEsquema(){return new CitaAutorizacionTest();}
    protected java.sql.Connection abrir()throws java.sql.SQLException{return esquema.abrir();}
    private java.time.Clock reloj(){return esquema.reloj();}
    private int count(String sql)throws Exception{return esquema.count(sql);}
    private ConfirmacionAsistenciaDAO seguimiento;
    private void ejecutar(String sql)throws Exception {try(var c=abrir();var s=c.createStatement()){s.execute(sql);}}
    @BeforeEach void prepararPolitica()throws Exception {
        esquema=crearEsquema();esquema.setUp();
        ejecutar("CREATE TABLE notificacion(id_usuario INT,id_cita INT,tipo VARCHAR(40),canal VARCHAR(30),mensaje VARCHAR(300),estado_envio VARCHAR(30),leida INT,fecha_envio DATETIME)");
        seguimiento=new ConfirmacionAsistenciaDAO(this::abrir,reloj());
        ejecutar("INSERT INTO usuario(id_usuario,nombres,rol) VALUES(60,'Admisión','PERSONAL_ADMINISTRATIVO')");
        ejecutar("INSERT INTO personal_administrativo(id_personal,id_usuario,id_sede) VALUES(60,60,1)");
        ejecutar("UPDATE cita SET fecha_hora='2026-10-30 09:00:00' WHERE id_cita=7");
        ejecutar("UPDATE confirmacion_asistencia SET vence_en='2026-10-28 10:00:00' WHERE id_cita=7");
    }
    private void contactos(){seguimiento.registrarContacto(60,7,"LLAMADA","SIN_RESPUESTA");seguimiento.registrarContacto(60,7,"CORREO","NO_CONFIRMA");}
    @Test void noCancelaSinLosContactosMinimosYUsaElValorVigente()throws Exception {
        assertEquals("CONTACTOS_INSUFICIENTES",assertThrows(AgendaException.class,()->seguimiento.cancelarSinConfirmacion(60,7)).code());
        contactos();ejecutar("UPDATE configuracion_ofertas SET intentos_confirmacion=3");
        assertEquals("CONTACTOS_INSUFICIENTES",assertThrows(AgendaException.class,()->seguimiento.cancelarSinConfirmacion(60,7)).code());
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE id_cita=7 AND estado='CONFIRMADA'"));
    }
    @Test void permiteCancelacionManualAlVencerConContactosYLaAudita()throws Exception {
        contactos();seguimiento.cancelarSinConfirmacion(60,7);
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE id_cita=7 AND estado='CANCELADA'"));
        assertEquals(1,count("SELECT COUNT(*) FROM auditoria_seguridad WHERE evento='CITA_CANCELADA_SIN_CONFIRMAR'"));
        assertEquals(409,assertThrows(AgendaException.class,()->seguimiento.cancelarSinConfirmacion(60,7)).status());
    }
    @Test void losContactosNoPermitenCancelarAntesDelPlazo()throws Exception {
        contactos();ejecutar("UPDATE confirmacion_asistencia SET vence_en='2026-10-29 10:00:00' WHERE id_cita=7");
        assertEquals("PLAZO_CONFIRMACION_VIGENTE",assertThrows(AgendaException.class,()->seguimiento.cancelarSinConfirmacion(60,7)).code());
    }
    @Test void asistenciaConfirmadaYCuentaAjenaNoSeCancelan() {
        contactos();seguimiento.registrarContacto(60,7,"LLAMADA","CONFIRMA");
        assertEquals(409,assertThrows(AgendaException.class,()->seguimiento.cancelarSinConfirmacion(60,7)).status());
        assertEquals(403,assertThrows(AgendaException.class,()->seguimiento.cancelarSinConfirmacion(10,7)).status());
    }
    @Test void otraSedeNoPuedeCancelarPorFaltaDeConfirmacion()throws Exception {
        contactos();ejecutar("INSERT INTO sede(id_sede,nombre,estado) VALUES(2,'Otra','ACTIVO')");
        ejecutar("INSERT INTO usuario(id_usuario,nombres,rol) VALUES(70,'Otra admisión','PERSONAL_ADMINISTRATIVO')");
        ejecutar("INSERT INTO personal_administrativo VALUES(70,70,2)");
        assertEquals(409,assertThrows(AgendaException.class,()->seguimiento.cancelarSinConfirmacion(70,7)).status());
    }
    @Test void fallaDeAuditoriaRevierteLaCancelacion()throws Exception {
        contactos();ejecutar("DROP TABLE auditoria_seguridad");
        assertEquals(503,assertThrows(AgendaException.class,()->seguimiento.cancelarSinConfirmacion(60,7)).status());
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE id_cita=7 AND estado='CONFIRMADA'"));
    }
    @Test void confirmacionSimultaneaNoDejaUnaCitaConfirmadaYCancelada()throws Exception {
        contactos();var tareas=Executors.newFixedThreadPool(2);
        try {
            var a=tareas.submit(()->intentar(()->seguimiento.cancelarSinConfirmacion(60,7)));
            var b=tareas.submit(()->intentar(()->seguimiento.registrarContacto(60,7,"LLAMADA","CONFIRMA")));
            assertEquals(1,a.get(10,TimeUnit.SECONDS)+b.get(10,TimeUnit.SECONDS));
            assertEquals(1,count("SELECT COUNT(*) FROM cita c JOIN confirmacion_asistencia a ON a.id_cita=c.id_cita WHERE c.id_cita=7 AND ((c.estado='CANCELADA' AND a.confirmada_en IS NULL) OR (c.estado='CONFIRMADA' AND a.confirmada_en IS NOT NULL))"));
        } finally {tareas.shutdownNow();}
    }
    private int intentar(Runnable accion){try{accion.run();return 1;}catch(AgendaException e){assertEquals(409,e.status());return 0;}}
}


