package pe.edu.utp.clinica.correo;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ConfirmacionCitaTest {
    @Test void incluyeDatosDeLaReservaYEscapaElContenidoHtml() {
        var cita=new ConfirmacionCita(123,"paciente@example.test","María <script>","Médico Ñúñez","Medicina general","Sede & Central",LocalDateTime.of(2026,12,1,9,20));
        var correo=cita.correo();
        assertTrue(correo.texto().contains("01/12/2026"));assertTrue(correo.texto().contains("09:20"));
        assertTrue(correo.html().contains("María &lt;script&gt;"));assertFalse(correo.html().contains("<script>"));
        assertTrue(correo.html().contains("Sede &amp; Central"));assertTrue(correo.html().contains("Confirmación de cita"));
        assertFalse(correo.html().contains("href="));assertFalse(cita.toString().contains("@"));
    }
}
