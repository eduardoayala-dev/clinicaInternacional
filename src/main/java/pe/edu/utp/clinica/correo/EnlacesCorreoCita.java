package pe.edu.utp.clinica.correo;

import pe.edu.utp.clinica.service.InvitacionPersonalServicio;
import pe.edu.utp.clinica.util.EnlaceCita;
import pe.edu.utp.clinica.util.Html;
import java.util.Map;

public final class EnlacesCorreoCita {
    private EnlacesCorreoCita(){}
    public static String base(){return InvitacionPersonalServicio.basePublica(System.getenv());}
    private static String url(String base,int cita,EnlaceCita.Accion accion){
        String validada=InvitacionPersonalServicio.basePublica(Map.of("APP_PUBLIC_URL",base));
        return validada+new EnlaceCita(cita,accion).ruta();
    }
    public static String texto(String base,int cita){
        return "\nConfirma tu asistencia: "+url(base,cita,EnlaceCita.Accion.CONFIRMAR)
                +"\nSi no puedes asistir: "+url(base,cita,EnlaceCita.Accion.CANCELAR)
                +"\nLos enlaces requieren iniciar sesión. Revisa la cita y confirma la acción en el portal.";
    }
    public static String html(String base,int cita){
        return "<p><a href=\""+Html.texto(url(base,cita,EnlaceCita.Accion.CONFIRMAR))+"\" style=\"display:inline-block;padding:12px 18px;background:#1668b8;color:#fff;border-radius:8px;text-decoration:none\">Confirmar asistencia</a></p>"
                +"<p><a href=\""+Html.texto(url(base,cita,EnlaceCita.Accion.CANCELAR))+"\" style=\"color:#b91c1c\">No podré asistir</a></p>"
                +"<p style=\"color:#64748b;font-size:12px\">Los enlaces requieren iniciar sesión. Abrirlos no modifica tu cita: revisa y confirma la acción en el portal.</p>";
    }
}
