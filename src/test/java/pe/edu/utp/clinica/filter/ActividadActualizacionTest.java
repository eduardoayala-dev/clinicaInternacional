package pe.edu.utp.clinica.filter;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.support.HttpExchange;
import static org.junit.jupiter.api.Assertions.*;

class ActividadActualizacionTest {
    @Test void refrescarAutomaticamenteNoProlongaLaSesion() {
        var x=new HttpExchange();x.headers.put("X-Actualizacion-Automatica","true");
        assertFalse(AutenticacionJwtFilter.actividadHumana(x.request,"/admin/dashboard-operativo"));
        x.headers.remove("X-Actualizacion-Automatica");assertTrue(AutenticacionJwtFilter.actividadHumana(x.request,"/admin/dashboard-operativo"));
    }
    @Test void elMarcadorNoCambiaOtrasRutasYLosEventosSiguenSinActividadHumana() {
        var x=new HttpExchange();x.headers.put("X-Actualizacion-Automatica","true");
        assertTrue(AutenticacionJwtFilter.actividadHumana(x.request,"/paciente/citas"));
        assertFalse(AutenticacionJwtFilter.actividadHumana(x.request,"/auth/estado"));
        assertFalse(AutenticacionJwtFilter.actividadHumana(x.request,"/admin/eventos-confirmaciones"));
    }
}
