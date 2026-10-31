package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

abstract class PropuestaHorarioContrato {
    private AgendaCitaContrato fixture;
    private PropuestaHorarioDAO propuestas;
    protected abstract String url();

    @BeforeEach void preparar() throws Exception {
        fixture = new AgendaCitaContrato() {
            @Override protected void configurar() { url = PropuestaHorarioContrato.this.url(); }
        };
        fixture.preparar();
        fixture.sql("ALTER TABLE sede ADD COLUMN nombre VARCHAR(100)");
        fixture.sql("UPDATE sede SET nombre='Sede Ñúñez'");
        fixture.sql("INSERT INTO usuario VALUES(5,'Admisión','Uno','PERSONAL_ADMINISTRATIVO',TRUE),(6,'Admisión','Dos','PERSONAL_ADMINISTRATIVO',TRUE)");
        fixture.sql("CREATE TABLE personal_administrativo(id_usuario INT PRIMARY KEY,id_sede INT)");
        fixture.sql("INSERT INTO personal_administrativo VALUES(5,1),(6,2)");
        migrar(); migrar();
        propuestas = new PropuestaHorarioDAO(fixture::abrir, fixture.clock);
    }

    private void migrar() throws Exception {
        String ddl = Files.readString(Path.of("sql/migrations/V009__propuestas_horarios.sql"));
        try (Connection conn = fixture.abrir(); var st = conn.createStatement()) {
            if (url().startsWith("jdbc:h2:")) {
                st.execute(ddl.split("SET @agregar_propuesta")[0].replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci", ""));
                st.execute("ALTER TABLE disponibilidad ADD COLUMN IF NOT EXISTS id_propuesta INT REFERENCES propuesta_horario(id_propuesta)");
            } else for (String sql : ddl.split(";")) if (!sql.isBlank()) st.execute(sql);
        }
    }

    private int proponer() {
        return propuestas.proponer(3, 3, LocalDate.of(2026,12,1), LocalDate.of(2026,12,8), LocalTime.of(9,0), LocalTime.of(11,0),30);
    }

    @Test void propuestaNoPublicaYLaAprobacionGeneraSoloDiasElegidos() throws Exception {
        int id = proponer();
        assertEquals(0, fixture.count("SELECT COUNT(*) FROM disponibilidad"));
        assertThrows(AgendaException.class, () -> fixture.dao.reservar(1,1,"2026-12-01","09:00","BAJA"));
        assertEquals(3, propuestas.revisar(5,id,true,null));
        assertEquals(3, fixture.count("SELECT COUNT(*) FROM disponibilidad WHERE estado='PUBLICADO' AND id_propuesta="+id));
        assertEquals(1, fixture.count("SELECT COUNT(*) FROM propuesta_horario WHERE estado='PUBLICADO' AND id_revisor=5 AND revisada_en IS NOT NULL"));
        assertTrue(fixture.dao.reservar(1,1,"2026-12-01","09:00","BAJA")>0);
        assertThrows(AgendaException.class, () -> fixture.dao.reservar(1,1,"2026-12-02","09:00","BAJA"));
        assertThrows(AgendaException.class, () -> fixture.dao.reservar(1,1,"2026-12-15","09:00","BAJA"));
    }

    @Test void soloElMedicoTitularConsultaYSoloAdmisionDeLaSedeRevisa() {
        int id = proponer();
        assertEquals(1, propuestas.delMedico(3).size());
        assertTrue(propuestas.delMedico(4).isEmpty());
        assertTrue(propuestas.paraAdmision(6).isEmpty());
        assertEquals(404, assertThrows(AgendaException.class, () -> propuestas.revisar(6,id,true,null)).status());
        assertEquals(403, assertThrows(AgendaException.class, () -> propuestas.revisar(3,id,true,null)).status());
        assertEquals(403, assertThrows(AgendaException.class, () -> propuestas.delMedico(1)).status());
    }

    @Test void unCruceRevierteInclusoLosBloquesInsertadosAntesDelConflicto() throws Exception {
        fixture.dao.publicar(1,1,"2026-12-08","10:00","12:00",30);
        int id=proponer();
        assertEquals("TURNOS_SUPERPUESTOS", assertThrows(AgendaException.class, () -> propuestas.revisar(5,id,true,null)).code());
        assertEquals(1,fixture.count("SELECT COUNT(*) FROM disponibilidad"));
        assertEquals("PENDIENTE", propuestas.delMedico(3).get(0).estado());
    }

    @Test void rechazarExigeMotivoYNoPublicaNiPermiteAprobarDespues() throws Exception {
        int id=proponer();
        assertThrows(AgendaException.class, () -> propuestas.revisar(5,id,false," "));
        propuestas.revisar(5,id,false,"Consultorio ocupado en ese turno.");
        var p=propuestas.delMedico(3).get(0);
        assertEquals("RECHAZADO",p.estado()); assertEquals("Consultorio ocupado en ese turno.",p.motivo());
        assertEquals(0,fixture.count("SELECT COUNT(*) FROM disponibilidad"));
        assertEquals(409,assertThrows(AgendaException.class,()->propuestas.revisar(5,id,true,null)).status());
    }

    @Test void dosAprobacionesSimultaneasNoDuplicanLosTurnos() throws Exception {
        int id=proponer();var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Integer> aprobar=()->{start.await();try{return propuestas.revisar(5,id,true,null);}catch(AgendaException e){assertEquals(409,e.status());return 0;}};
            var a=pool.submit(aprobar);var b=pool.submit(aprobar);start.countDown();
            assertEquals(3,a.get(15,TimeUnit.SECONDS)+b.get(15,TimeUnit.SECONDS));
            assertEquals(3,fixture.count("SELECT COUNT(*) FROM disponibilidad"));
        } finally {pool.shutdownNow();}
    }

    @Test void validaRangoDiasDuracionYTurnosCompletos() {
        LocalDate desde = LocalDate.of(2026,12,1);
        LocalDate hasta = LocalDate.of(2026,12,8);
        LocalDate pasada = LocalDate.of(2026,10,26);
        LocalDate lejana = LocalDate.of(2027,12,8);
        LocalTime inicio = LocalTime.of(9,0);
        LocalTime fin = LocalTime.of(11,0);
        for(int dias:new int[]{0,128,-1}) assertThrows(AgendaException.class,()->propuestas.proponer(3,dias,desde,hasta,inicio,fin,30));
        for(int minutos:new int[]{0,4,241,50}) assertThrows(AgendaException.class,()->propuestas.proponer(3,3,desde,hasta,inicio,fin,minutos));
        assertThrows(AgendaException.class,()->propuestas.proponer(3,3,pasada,hasta,inicio,fin,30));
        assertThrows(AgendaException.class,()->propuestas.proponer(3,3,desde,lejana,inicio,fin,30));
        assertThrows(AgendaException.class,()->propuestas.proponer(3,1,desde,desde,inicio,fin,30));
    }

    @Test void noApruebaPropuestaVencidaNiDeMedicoDesactivado() throws Exception {
        int id=proponer();
        var futuro=new PropuestaHorarioDAO(fixture::abrir,java.time.Clock.fixed(java.time.Instant.parse("2027-01-01T15:00:00Z"),java.time.ZoneOffset.UTC));
        assertThrows(AgendaException.class,()->futuro.revisar(5,id,true,null));
        fixture.sql("UPDATE medico SET estado='INACTIVO' WHERE id_medico=1");
        assertThrows(AgendaException.class,()->propuestas.revisar(5,id,true,null));
        assertEquals(0,fixture.count("SELECT COUNT(*) FROM disponibilidad"));
    }

    @Test void cambioDeSedeNoPublicaEnLaSedeAnterior() throws Exception {
        int id=proponer();fixture.sql("UPDATE medico SET id_sede=2 WHERE id_medico=1");
        assertThrows(AgendaException.class,()->propuestas.revisar(5,id,true,null));
        assertEquals(0,fixture.count("SELECT COUNT(*) FROM disponibilidad"));
    }

    @Test void advierteVigenciaSinReescribirEstadoDeLaPropuesta() {
        proponer(); var p=propuestas.delMedico(3).get(0);
        assertEquals("Lun · Mar",p.diasTexto());
        propuestas.revisar(5,p.id(),true,null);p=propuestas.delMedico(3).get(0);
        assertEquals("Por vencer",p.estadoVisible(LocalDate.of(2026,12,2)));
        assertEquals("Vencido",p.estadoVisible(LocalDate.of(2026,12,9)));
        assertEquals("PUBLICADO",p.estado());
    }
}
