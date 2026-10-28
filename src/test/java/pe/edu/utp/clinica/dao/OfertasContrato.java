package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

abstract class OfertasContrato extends AgendaCitaContrato {
    protected OfertasDAO ofertas;
    protected ListaEsperaDAO lista;
    protected NotificacionDAO notificaciones;
    private Reloj reloj;
    private static final String FECHA = "2026-12-01";

    private static final class Reloj extends Clock {
        private final AtomicReference<Instant> instante = new AtomicReference<>(Instant.parse("2026-10-27T15:10:00Z"));
        @Override public ZoneId getZone() { return ZoneId.of("America/Lima"); }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(),zone); }
        @Override public Instant instant() { return instante.get(); }
        void avanzar(Duration tiempo) { instante.updateAndGet(actual -> actual.plus(tiempo)); }
    }

    @BeforeEach void prepararOfertas() throws Exception {
        reloj=new Reloj();
        dao=new AgendaCitaDAO(this::abrir,reloj);
        lista=new ListaEsperaDAO(this::abrir,reloj);
        ofertas=new OfertasDAO(this::abrir,reloj);
        notificaciones=new NotificacionDAO(this::abrir,reloj);
        sql("ALTER TABLE especialidad ADD COLUMN nombre VARCHAR(100)");
        sql("ALTER TABLE sede ADD COLUMN nombre VARCHAR(100)");
        sql("UPDATE especialidad SET nombre='Cardiología'");
        sql("UPDATE sede SET nombre='Sede de prueba'");
        sql("INSERT INTO usuario VALUES(5,'Tercero','Pérez','PACIENTE',TRUE),(6,'Cuarto','Núñez','PACIENTE',TRUE)");
        sql("INSERT INTO paciente VALUES(3,5),(4,6)");
        sql("ALTER TABLE paciente ADD FOREIGN KEY(id_usuario) REFERENCES usuario(id_usuario)");
        sql("ALTER TABLE medico ADD FOREIGN KEY(id_usuario) REFERENCES usuario(id_usuario)");
        sql("ALTER TABLE disponibilidad ADD FOREIGN KEY(id_medico) REFERENCES medico(id_medico)");
        sql("ALTER TABLE cita ADD FOREIGN KEY(id_paciente) REFERENCES paciente(id_paciente)");
        sql("ALTER TABLE cita ADD FOREIGN KEY(id_medico) REFERENCES medico(id_medico)");
        sql("ALTER TABLE cita ADD FOREIGN KEY(id_disponibilidad) REFERENCES disponibilidad(id_disponibilidad)");
        sql("ALTER TABLE lista_espera ADD FOREIGN KEY(id_paciente) REFERENCES paciente(id_paciente)");
        sql("ALTER TABLE lista_espera ADD FOREIGN KEY(id_medico_preferido) REFERENCES medico(id_medico)");
        sql("ALTER TABLE reasignacion_inteligente ADD FOREIGN KEY(id_cita_original) REFERENCES cita(id_cita)");
        sql("ALTER TABLE reasignacion_inteligente ADD FOREIGN KEY(id_cita_asignada) REFERENCES cita(id_cita)");
        sql("ALTER TABLE reasignacion_inteligente ADD FOREIGN KEY(id_lista_espera) REFERENCES lista_espera(id_lista_espera)");
        sql("ALTER TABLE notificacion ADD FOREIGN KEY(id_usuario) REFERENCES usuario(id_usuario)");
        sql("ALTER TABLE notificacion ADD FOREIGN KEY(id_cita) REFERENCES cita(id_cita)");
    }

    private int registrar(int paciente) { return lista.registrar(paciente,1,1,"MANANA","MEDIA","CORREO,WHATSAPP"); }
    private int cancelada() throws Exception {
        dao.publicar(1,1,FECHA,"09:00","11:00",30);
        int cita=dao.reservar(1,1,FECHA,"09:00","MEDIA");
        sql("UPDATE cita SET estado='CANCELADA' WHERE id_cita="+cita);
        return cita;
    }

    @Test void registrationSupportsAnyClinicMultipleContactPreferencesAndIdempotency() throws Exception {
        int id=lista.registrar(2,1,null,"INDIFERENTE","MEDIA","WHATSAPP,CORREO");
        reloj.avanzar(Duration.ofMinutes(1));
        assertEquals(id,lista.registrar(2,1,null,"INDIFERENTE","MEDIA","CORREO,WHATSAPP"));
        assertEquals(1,count("SELECT COUNT(*) FROM lista_espera WHERE estado='ACTIVA' AND id_sede IS NULL AND canal_notificacion='CORREO,WHATSAPP'"));
        assertEquals(1,count("SELECT COUNT(*) FROM notificacion"));
        assertEquals("Cualquier sede",lista.obtenerEstadoListaEspera(2).get("sede"));
    }

    @Test void invalidRegistrationDoesNotCancelAnExistingValidEntry() throws Exception {
        int id=registrar(2);
        assertThrows(AgendaException.class,()->lista.registrar(2,999,1,"MANANA","MEDIA","CORREO"));
        assertThrows(AgendaException.class,()->lista.registrar(2,1,999,"MANANA","MEDIA","CORREO"));
        assertThrows(AgendaException.class,()->lista.registrar(2,1,1,"OTRO","MEDIA","CORREO"));
        assertThrows(AgendaException.class,()->lista.registrar(2,1,1,"MANANA","MEDIA","DESCONOCIDO"));
        assertEquals(id,lista.obtenerEstadoListaEspera(2).get("idListaEspera"));
        assertEquals(1,count("SELECT COUNT(*) FROM lista_espera"));
    }

    @Test void generatingAnOfferUsesActualDoctorDateAndDeadlineAndIsIdempotent() throws Exception {
        registrar(2); int cita=cancelada();
        int oferta=ofertas.generar(cita);
        assertTrue(oferta>0); assertEquals(0,ofertas.generar(cita));
        var data=ofertas.obtenerParaPaciente(2);
        assertEquals(oferta,data.get("idOferta"));
        assertEquals("Médico Prueba",data.get("medico"));
        assertTrue(data.get("fechaOfrecida").toString().contains("09:00"));
        assertEquals(900L,data.get("tiempoRestanteSegundos"));
        reloj.avanzar(Duration.ofMinutes(2));
        assertEquals(780L,ofertas.obtenerParaPaciente(2).get("tiempoRestanteSegundos"));
        assertTrue(ofertas.obtenerParaPaciente(3).isEmpty());
        assertEquals(1,count("SELECT COUNT(*) FROM reasignacion_inteligente WHERE estado='PENDIENTE'"));
    }

    @Test void activeOfferProtectsSlotFromNormalReservationsAndCatalog() throws Exception {
        registrar(2); int cita=cancelada(); ofertas.generar(cita);
        assertEquals(409,assertThrows(AgendaException.class,()->dao.reservar(3,1,FECHA,"09:00","MEDIA")).status());
        var medicos=new MedicoDAO(this::abrir,reloj);
        sql("ALTER TABLE medico ADD COLUMN cmp VARCHAR(20)");
        assertFalse(medicos.obtenerHorariosDisponibles(1,FECHA).contains("09:00"));
        assertTrue(medicos.obtenerHorariosDisponibles(1,FECHA).contains("09:30"));
    }

    @Test void acceptanceActuallyCreatesOneAppointmentAndCannotBeRepeatedToCreateMore() throws Exception {
        registrar(2); int original=cancelada(); int oferta=ofertas.generar(original);
        int cita=ofertas.aceptar(2,oferta);
        assertTrue(cita>original);
        assertEquals(cita,ofertas.aceptar(2,oferta));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE id_paciente=2 AND estado='CONFIRMADA' AND fecha_hora='2026-12-01 09:00:00'"));
        assertEquals(1,count("SELECT COUNT(*) FROM lista_espera WHERE id_paciente=2 AND estado='ATENDIDA'"));
        assertEquals(1,count("SELECT COUNT(*) FROM reasignacion_inteligente WHERE estado='ACEPTADA' AND id_cita_asignada="+cita));
        assertTrue(ofertas.obtenerParaPaciente(2).isEmpty());
    }

    @Test void foreignOfferAndMissingIdentifiersNeverAffectTheOwner() throws Exception {
        registrar(2); int oferta=ofertas.generar(cancelada());
        assertEquals(404,assertThrows(AgendaException.class,()->ofertas.aceptar(3,oferta)).status());
        assertEquals(404,assertThrows(AgendaException.class,()->ofertas.rechazar(3,oferta)).status());
        assertEquals(400,assertThrows(AgendaException.class,()->ofertas.aceptar(2,0)).status());
        assertEquals(1,count("SELECT COUNT(*) FROM reasignacion_inteligente WHERE estado='PENDIENTE'"));
        assertEquals(0,count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA'"));
    }

    @Test void expiryIsCheckedByServerAndNextPatientCanReceiveOffer() throws Exception {
        registrar(2); registrar(3); int original=cancelada(); int id=ofertas.generar(original);
        reloj.avanzar(Duration.ofMinutes(16));
        assertTrue(ofertas.obtenerParaPaciente(2).isEmpty());
        assertTrue(ofertas.tieneOfertaVencida(2));
        assertEquals(410,assertThrows(AgendaException.class,()->ofertas.aceptar(2,id)).status());
        assertEquals(1,ofertas.procesarPendientes());
        assertFalse(ofertas.obtenerParaPaciente(3).isEmpty());
        assertEquals(1,count("SELECT COUNT(*) FROM reasignacion_inteligente WHERE estado='EXPIRADA'"));
    }

    @Test void rejectionKeepsRegistrationAndPassesTheSameRealSlotToNextCandidate() throws Exception {
        int listaId=registrar(2); registrar(3); int original=cancelada(); int id=ofertas.generar(original);
        assertTrue(ofertas.rechazar(2,id)); assertTrue(ofertas.rechazar(2,id));
        assertEquals(listaId,lista.obtenerEstadoListaEspera(2).get("idListaEspera"));
        assertFalse(ofertas.obtenerParaPaciente(3).isEmpty());
        assertEquals(0,count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA'"));
        assertEquals(1,count("SELECT COUNT(*) FROM reasignacion_inteligente WHERE estado='PENDIENTE'"));
    }

    @Test void withdrawalRequiresOwnerAndReleasesExistingHold() throws Exception {
        int listaId=registrar(2); int id=ofertas.generar(cancelada());
        assertEquals(404,assertThrows(AgendaException.class,()->lista.retirarListaEspera(listaId,3)).status());
        assertTrue(lista.retirarListaEspera(listaId,2)); assertTrue(lista.retirarListaEspera(listaId,2));
        assertTrue(lista.obtenerEstadoListaEspera(2).isEmpty());
        assertEquals(409,assertThrows(AgendaException.class,()->ofertas.aceptar(2,id)).status());
        assertTrue(dao.reservar(3,1,FECHA,"09:00","MEDIA")>0);
    }

    @Test void notificationFailureRollsBackOfferAndAcceptance() throws Exception {
        registrar(2); int original=cancelada();
        sql("DROP TABLE notificacion");
        assertEquals(503,assertThrows(AgendaException.class,()->ofertas.generar(original)).status());
        assertEquals(0,count("SELECT COUNT(*) FROM reasignacion_inteligente"));
        assertEquals(1,count("SELECT COUNT(*) FROM lista_espera WHERE estado='ACTIVA'"));
    }

    @Test void failedAcceptanceKeepsOfferAndDoesNotCreateAppointment() throws Exception {
        registrar(2); int id=ofertas.generar(cancelada());
        sql("DROP TABLE notificacion");
        assertEquals(503,assertThrows(AgendaException.class,()->ofertas.aceptar(2,id)).status());
        assertEquals(1,count("SELECT COUNT(*) FROM reasignacion_inteligente WHERE estado='PENDIENTE'"));
        assertEquals(0,count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA'"));
        assertEquals(1,count("SELECT COUNT(*) FROM lista_espera WHERE estado='OFERTADA'"));
    }

    @Test void clinicAndTimePreferencesAreRespected() throws Exception {
        sql("UPDATE medico SET id_sede=2 WHERE id_medico=2");
        lista.registrar(2,1,1,"MANANA","MEDIA","CORREO");
        lista.registrar(3,1,null,"MANANA","MEDIA","CORREO");
        dao.publicar(2,2,FECHA,"09:00","10:00",30);
        int cita=dao.reservar(1,2,FECHA,"09:00","MEDIA");
        sql("UPDATE cita SET estado='CANCELADA' WHERE id_cita="+cita);
        assertTrue(ofertas.generar(cita)>0);
        assertTrue(ofertas.obtenerParaPaciente(2).isEmpty()); assertFalse(ofertas.obtenerParaPaciente(3).isEmpty());
    }

    @Test void requiredNoticeIsNotInterpretedAsClinicalUrgency() throws Exception {
        lista.registrar(2,1,1,"TARDE","MEDIA","CORREO",720);
        lista.registrar(3,1,1,"TARDE","MEDIA","CORREO",180);
        dao.publicar(1,1,"2026-10-27","15:00","16:00",30);
        int cita=dao.reservar(1,1,"2026-10-27","15:00","BAJA");
        sql("UPDATE cita SET estado='CANCELADA' WHERE id_cita="+cita);
        assertTrue(ofertas.generar(cita)>0);
        assertTrue(ofertas.obtenerParaPaciente(2).isEmpty()); assertFalse(ofertas.obtenerParaPaciente(3).isEmpty());
        assertEquals(2,count("SELECT COUNT(*) FROM lista_espera WHERE nivel_urgencia='MEDIA'"));
    }

    @Test void preferredDoctorIsARealRestrictionRatherThanAnIgnoredFormField() throws Exception {
        lista.registrar(2,new ListaEsperaDAO.Preferencias(1,1,"MANANA","MEDIA","CORREO",180,2));
        registrar(3);
        ofertas.generar(cancelada());
        assertTrue(ofertas.obtenerParaPaciente(2).isEmpty());
        assertFalse(ofertas.obtenerParaPaciente(3).isEmpty());
        assertThrows(AgendaException.class,()->lista.registrar(4,new ListaEsperaDAO.Preferencias(2,1,"MANANA","MEDIA","CORREO",180,1)));
    }

    @Test void concurrentAcceptsReturnSameAppointmentWithoutDuplicatingIt() throws Exception {
        registrar(2); int id=ofertas.generar(cancelada());
        CountDownLatch start=new CountDownLatch(1);
        var executor=Executors.newFixedThreadPool(2);
        try {
            var a=executor.submit(()->{assertTrue(start.await(5,TimeUnit.SECONDS));return ofertas.aceptar(2,id);});
            var b=executor.submit(()->{assertTrue(start.await(5,TimeUnit.SECONDS));return ofertas.aceptar(2,id);});
            start.countDown();
            assertEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
        } finally {executor.shutdownNow();}
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA'"));
    }

    @Test void concurrentOfferGenerationCannotPromiseOneSlotTwice() throws Exception {
        registrar(2); registrar(3); int cita=cancelada();
        CountDownLatch start=new CountDownLatch(1);
        var executor=Executors.newFixedThreadPool(2);
        try {
            var a=executor.submit(()->{assertTrue(start.await(5,TimeUnit.SECONDS));return ofertas.generar(cita);});
            var b=executor.submit(()->{assertTrue(start.await(5,TimeUnit.SECONDS));return ofertas.generar(cita);});
            start.countDown();
            assertEquals(1,(a.get(15,TimeUnit.SECONDS)>0?1:0)+(b.get(15,TimeUnit.SECONDS)>0?1:0));
        } finally {executor.shutdownNow();}
        assertEquals(1,count("SELECT COUNT(*) FROM reasignacion_inteligente WHERE estado='PENDIENTE'"));
    }

    @Test void inactiveRecipientDoesNotKeepARealSlotBlockedUntilMaintenance() throws Exception {
        registrar(2); ofertas.generar(cancelada());
        sql("UPDATE usuario SET activo=FALSE WHERE id_usuario=2");
        assertTrue(dao.reservar(3,1,FECHA,"09:00","MEDIA")>0);
        ofertas.procesarPendientes();
        assertEquals(0,count("SELECT COUNT(*) FROM reasignacion_inteligente WHERE estado='PENDIENTE'"));
    }

    @Test void ownNotificationsAreIsolatedAndDoNotPretendToBeReminders() throws Exception {
        registrar(2); cancelada();
        var own=notificaciones.obtenerNotificacionesUsuario(2);
        assertEquals(1,own.size());
        int id=(Integer)own.get(0).get("idNotificacion");
        assertFalse(notificaciones.marcarNotificacionIndividualLeida(id,5));
        assertFalse(notificaciones.confirmarRecordatorioCita(id,2));
        assertEquals(1,notificaciones.contarNotificacionesSinLeer(2));
        assertTrue(notificaciones.marcarNotificacionIndividualLeida(id,2));
        assertEquals(0,notificaciones.contarNotificacionesSinLeer(2));
        assertEquals("Ver",notificaciones.obtenerNotificacionesUsuario(1).get(0).get("accion"));
    }

    @Test void reminderConfirmationRequiresOwnedActiveAppointmentAndExplicitType() throws Exception {
        dao.publicar(1,1,FECHA,"09:00","10:00",30);
        int cita=dao.reservar(1,1,FECHA,"09:00","MEDIA");
        sql("UPDATE cita SET estado='SOLICITADA'");
        notificaciones.crearNotificacion(1,cita,"Recordatorio de cita","Confirma tu cita.");
        int id=count("SELECT MAX(id_notificacion) FROM notificacion");
        assertFalse(notificaciones.confirmarRecordatorioCita(id,2));
        assertTrue(notificaciones.confirmarRecordatorioCita(id,1));
        assertEquals(1,count("SELECT COUNT(*) FROM cita WHERE estado='CONFIRMADA'"));
        sql("UPDATE cita SET estado='CANCELADA'");
        assertFalse(notificaciones.confirmarRecordatorioCita(id,1));
    }
}
