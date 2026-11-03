package pe.edu.utp.clinica.catalogo;

import jakarta.servlet.http.HttpSession;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.model.catalogo.FilaImportacionMotivo;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

public final class ConfirmacionImportacionMotivos {
    private static final String ATRIBUTO="importacionMotivosPendiente";
    public record Pendiente(int actor,String token,List<FilaImportacionMotivo> filas,Instant vence) {
        public Pendiente {filas=List.copyOf(filas);}
    }
    private ConfirmacionImportacionMotivos() {}
    public static void descartar(HttpSession sesion){synchronized(sesion){sesion.removeAttribute(ATRIBUTO);}}
    public static Pendiente preparar(HttpSession sesion,int actor,List<FilaImportacionMotivo> filas,Clock reloj){
        if(actor<=0||filas==null||filas.isEmpty()||filas.size()>200||filas.stream().anyMatch(java.util.Objects::isNull)||filas.stream().noneMatch(f->f.error().isEmpty()))throw invalida();
        var pendiente=new Pendiente(actor,UUID.randomUUID().toString(),filas,reloj.instant().plus(Duration.ofMinutes(10)));
        synchronized(sesion){sesion.setAttribute(ATRIBUTO,pendiente);}return pendiente;
    }
    public static Pendiente consumir(HttpSession sesion,int actor,String token,Clock reloj){
        synchronized(sesion){
            Object dato=sesion.getAttribute(ATRIBUTO);
            if(!(dato instanceof Pendiente p)||p.actor()!=actor||!reloj.instant().isBefore(p.vence())||token==null||token.length()>64||!MessageDigest.isEqual(p.token().getBytes(StandardCharsets.UTF_8),token.getBytes(StandardCharsets.UTF_8)))throw invalida();
            sesion.removeAttribute(ATRIBUTO);return p;
        }
    }
    private static AgendaException invalida(){return new AgendaException(409,"IMPORTACION_NO_VIGENTE","La vista previa venció o ya fue utilizada. Vuelve a revisar el archivo antes de confirmar.");}
}
