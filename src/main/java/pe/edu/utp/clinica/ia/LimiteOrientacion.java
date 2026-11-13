package pe.edu.utp.clinica.ia;

import pe.edu.utp.clinica.dao.AgendaException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

public final class LimiteOrientacion {
    private final Map<String,Long> vencimientos=new HashMap<>();
    private final LongSupplier reloj;
    public LimiteOrientacion() {this(System::currentTimeMillis);}
    LimiteOrientacion(LongSupplier reloj) {this.reloj=reloj;}

    public synchronized void adquirir(String sesion) {
        long ahora=reloj.getAsLong();
        vencimientos.entrySet().removeIf(entrada->entrada.getValue()<=ahora);
        if(vencimientos.containsKey(sesion) || vencimientos.size()>=4096) throw new AgendaException(429,"ORIENTACION_LIMITE","Espera unos segundos antes de volver a consultar.");
        vencimientos.put(sesion,ahora+15000);
    }
}
