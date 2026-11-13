package pe.edu.utp.clinica.reporte;

import pe.edu.utp.clinica.dao.AgendaException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

public record FiltroReporte(LocalDate desde,LocalDate hasta,String estado,String tipo) {
    private static final Set<String> ESTADOS=Set.of("","SOLICITADA","CONFIRMADA","EN_SALA","EN_ATENCION","ATENDIDA","NO_ASISTIO","NO_ATENDIDA","REPROGRAMADA","CANCELADA");
    public FiltroReporte {
        if(desde==null || hasta==null || hasta.isBefore(desde) || desde.isBefore(LocalDate.of(1000,1,1)) || hasta.isAfter(LocalDate.of(9999,12,30)) || ChronoUnit.DAYS.between(desde,hasta)>30 || !ESTADOS.contains(estado==null?"?":estado) || !Set.of("citas","contacto","indicadores").contains(tipo==null?"?":tipo))
            throw invalido();
    }
    public static FiltroReporte leer(String desde,String hasta,String estado,String tipo,Clock clock) {
        try {
            LocalDate inicio=desde==null||desde.isBlank()?LocalDate.now(clock):LocalDate.parse(desde);
            LocalDate fin=hasta==null||hasta.isBlank()?inicio:LocalDate.parse(hasta);
            return new FiltroReporte(inicio,fin,estado==null?"":estado,tipo==null||tipo.isBlank()?"citas":tipo);
        } catch(java.time.DateTimeException e) {throw invalido();}
    }
    private static AgendaException invalido(){return new AgendaException(400,"FILTROS_INVALIDOS","Selecciona un período de hasta 31 días y filtros válidos.");}
}
