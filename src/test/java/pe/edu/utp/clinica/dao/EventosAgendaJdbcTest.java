package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.eventos.EventosAgenda;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class EventosAgendaJdbcTest extends CitaAutorizacionTest {
    @Test void emiteSoloDespuesDelCommitYNoEmiteSiLaOperacionFalla() {
        var recibidos = new AtomicInteger();
        try (var registro = EventosAgenda.INSTANCIA.suscribir(1, 600, recibidos::incrementAndGet)) {
            TransaccionJdbc.ejecutar(this::abrir, conn -> {
                TransaccionJdbc.avisarSede(1);
                assertEquals(0, recibidos.get());
                return null;
            });
            assertEquals(1, recibidos.get());
            assertThrows(AgendaException.class, () -> TransaccionJdbc.ejecutar(this::abrir, conn -> {
                TransaccionJdbc.avisarSede(1);
                throw new AgendaException(409, "PRUEBA", "Reversión de prueba");
            }));
            assertEquals(1, recibidos.get());
        }
    }

    @Test void confirmarAsistenciaNotificaUnaVezYNoSeRepiteAlReintentar() throws Exception {
        try (Connection conn = abrir(); Statement st = conn.createStatement()) {
            st.execute("UPDATE cita SET fecha_hora='2026-10-30 09:00:00' WHERE id_cita=7");
        }
        var recibidos = new AtomicInteger();
        var confirmaciones = new ConfirmacionAsistenciaDAO(this::abrir, reloj());
        try (var registro = EventosAgenda.INSTANCIA.suscribir(1, 601, recibidos::incrementAndGet)) {
            confirmaciones.confirmarPaciente(10, 7);
            confirmaciones.confirmarPaciente(10, 7);
            assertEquals(1, recibidos.get());
        }
    }

    @Test void unFalloEnElAvisoNoRevierteUnaOperacionYaGuardada() {
        var resultado = TransaccionJdbc.ejecutar(this::abrir, conn -> {
            TransaccionJdbc.alConfirmar(() -> { throw new IllegalStateException(); });
            return 42;
        });
        assertEquals(42, resultado);
    }
}
