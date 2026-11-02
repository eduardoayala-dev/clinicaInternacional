package pe.edu.utp.clinica.util;

import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;

public record EnlaceCita(int cita,Accion accion) implements Serializable {
    private static final long serialVersionUID=1L;
    private static final String PENDIENTE=EnlaceCita.class.getName();
    public enum Accion { CONFIRMAR, CANCELAR }
    private record Pendiente(EnlaceCita enlace,Instant vence) implements Serializable {
        private static final long serialVersionUID=1L;
    }
    public EnlaceCita {
        if(cita<=0||accion==null)throw new IllegalArgumentException("El enlace de cita no es válido.");
    }
    public static EnlaceCita leer(String cita,String accion){
        if(cita==null||!cita.matches("[1-9]\\d{0,9}"))throw new IllegalArgumentException("El enlace de cita no es válido.");
        Accion valor=switch(accion==null?"":accion){case "confirmar"->Accion.CONFIRMAR;case "cancelar"->Accion.CANCELAR;default->throw new IllegalArgumentException("La acción del enlace no es válida.");};
        return new EnlaceCita(Integer.parseInt(cita),valor);
    }
    public String ruta(){return "/cita?cita="+cita+"&accion="+(accion==Accion.CONFIRMAR?"confirmar":"cancelar");}
    public void recordar(HttpSession sesion,Clock reloj){sesion.setAttribute(PENDIENTE,new Pendiente(this,reloj.instant().plusSeconds(600)));}
    public static String consumir(HttpSession sesion,Clock reloj){
        if(sesion==null)return null;
        Object valor=sesion.getAttribute(PENDIENTE);sesion.removeAttribute(PENDIENTE);
        if(!(valor instanceof Pendiente pendiente)||!reloj.instant().isBefore(pendiente.vence()))return null;
        return pendiente.enlace().ruta();
    }
}
