package pe.edu.utp.clinica.filter;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.config.CorreosCitaContextListener;
import pe.edu.utp.clinica.support.HttpExchange;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class CorreosCitaFilterTest {
    @Test void avisaDespuesDeLaOperacionSinDespertarPorCadaLectura()throws Exception {
        var e=new HttpExchange();var veces=new AtomicInteger();
        e.contextAttributes.put(CorreosCitaContextListener.DESPERTAR,(Runnable)veces::incrementAndGet);
        new CorreosCitaFilter().doFilter(e.request,e.response,(req,res)->assertEquals(0,veces.get()));
        assertEquals(1,veces.get());e.method="GET";
        new CorreosCitaFilter().doFilter(e.request,e.response,(req,res)->{});
        assertEquals(1,veces.get());
    }
    @Test void sinConfiguracionNoInterfiereConLaOperacion()throws Exception {
        var e=new HttpExchange();var veces=new AtomicInteger();
        new CorreosCitaFilter().doFilter(e.request,e.response,(req,res)->veces.incrementAndGet());
        assertEquals(1,veces.get());
    }
}
