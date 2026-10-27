package pe.edu.utp.clinica.service.dni;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.support.HttpExchange;
import java.time.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class DniConsultaSesionTest {
    @Test void bindsNamesToDocumentAndSession() {
        var a = new HttpExchange(); var b = new HttpExchange();
        var data = new DniDatos("99000001", "PRUEBA", "FICTICIO", "LOCAL");
        DniConsultaSesion.guardar(a.session, data);
        assertEquals(data, DniConsultaSesion.obtener(a.session, "99000001"));
        assertNull(DniConsultaSesion.obtener(a.session, "99000002"));
        assertNull(DniConsultaSesion.obtener(b.session, "99000001"));
        assertNull(DniConsultaSesion.obtener(null, "99000001"));
    }

    @Test void expiresAndBoundsStoredPersonalData() {
        var x = new HttpExchange();
        var now = Instant.parse("2026-10-27T00:00:00Z");
        for (int i = 0; i < 6; i++) {
            var clock = Clock.fixed(now.plusSeconds(i), ZoneOffset.UTC);
            DniConsultaSesion.guardar(x.session, new DniDatos("9900000" + i, "PRUEBA", "FICTICIO", "LOCAL"), clock);
        }
        assertNull(DniConsultaSesion.obtener(x.session, "99000000", Clock.fixed(now.plusSeconds(10), ZoneOffset.UTC)));
        assertNotNull(DniConsultaSesion.obtener(x.session, "99000005", Clock.fixed(now.plusSeconds(10), ZoneOffset.UTC)));
        assertNull(DniConsultaSesion.obtener(x.session, "99000005", Clock.fixed(now.plusSeconds(606), ZoneOffset.UTC)));
        assertNull(x.session.getAttribute(DniConsultaSesion.class.getName()));
    }

    @Test void supportsContainerSessionSerializationWithoutLosingExpiry() throws Exception {
        var x = new HttpExchange();
        var data = new DniDatos("99000001", "Prueba", "Ficticio", "Local");
        DniConsultaSesion.guardar(x.session, data);
        String attribute = DniConsultaSesion.class.getName();
        var bytes = new ByteArrayOutputStream();
        try (var out = new ObjectOutputStream(bytes)) { out.writeObject(x.session.getAttribute(attribute)); }
        var restored = new HttpExchange();
        try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored.session.setAttribute(attribute, in.readObject());
        }
        assertEquals(data, DniConsultaSesion.obtener(restored.session, data.numero()));
    }
}
