package pe.edu.utp.clinica.config;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class EntornoAplicacionTest {
    @Test void everyRailwayIndicatorEnablesProductionEvenWithoutAppEnv() {
        for(String key:new String[]{"RAILWAY_ENVIRONMENT","RAILWAY_ENVIRONMENT_NAME","RAILWAY_PROJECT_ID"}) {
            assertTrue(EntornoAplicacion.produccion(Map.of(key,"test")),key);
            assertTrue(EntornoAplicacion.produccion(Map.of(key,"test","APP_ENV","development")),key);
        }
    }
    @Test void localAndExplicitEnvironmentsAreRecognized() {
        assertFalse(EntornoAplicacion.produccion(Map.of()));
        assertFalse(EntornoAplicacion.produccion(Map.of("APP_ENV","test")));
        assertTrue(EntornoAplicacion.produccion(Map.of("APP_ENV"," production ")));
    }
}
