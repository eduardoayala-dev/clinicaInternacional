package pe.edu.utp.clinica.ia;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.AgendaException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class LimiteOrientacionTest {
    @Test void mantieneLaEsperaDeQuinceSegundosYPurgadoPorSesion() {
        var ahora=new AtomicLong(0);var limite=new LimiteOrientacion(ahora::get);
        limite.adquirir("sesion-a");limite.adquirir("sesion-b");
        assertEquals(429,assertThrows(AgendaException.class,()->limite.adquirir("sesion-a")).status());
        ahora.set(14999);assertThrows(AgendaException.class,()->limite.adquirir("sesion-a"));
        ahora.set(15000);assertDoesNotThrow(()->limite.adquirir("sesion-a"));
    }
    @Test void peticionesSimultaneasDeLaMismaSesionSoloConsumenUnaConsulta() throws Exception {
        var limite=new LimiteOrientacion(()->0);var inicio=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Integer> consultar=()->{inicio.await();try{limite.adquirir("misma-sesion");return 1;}catch(AgendaException e){assertEquals(429,e.status());return 0;}};
            var a=pool.submit(consultar);var b=pool.submit(consultar);inicio.countDown();
            assertEquals(1,a.get(5,TimeUnit.SECONDS)+b.get(5,TimeUnit.SECONDS));
        } finally {pool.shutdownNow();}
    }
}
