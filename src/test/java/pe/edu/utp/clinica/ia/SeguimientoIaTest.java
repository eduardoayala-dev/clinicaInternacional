package pe.edu.utp.clinica.ia;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.SeguimientoIaDAO;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class SeguimientoIaTest {
    @Test void reglaDePlazoNoModificaLaProbabilidadAprendida() throws Exception {
        var modelo=ModeloInasistencia.demostracion();
        var ahora=LocalDateTime.of(2026,11,2,10,0);
        var entrada=new EntradaInasistencia(0,5,2,false);
        var vigente=SeguimientoIa.evaluar(new SeguimientoIaDAO.Instantanea(7,ahora,ahora.minusDays(1),entrada,false),modelo);
        var vencida=SeguimientoIa.evaluar(new SeguimientoIaDAO.Instantanea(7,ahora,ahora.minusDays(1),entrada,true),modelo);
        assertEquals(vigente.prediccion().probabilidad(),vencida.prediccion().probabilidad());
        assertEquals("BAJO",vigente.nivelSeguimiento());assertEquals("MEDIO",vencida.nivelSeguimiento());
        assertTrue(vencida.prediccion().experimental()); assertEquals("SINTETICO",vencida.prediccion().origen());
    }
}
