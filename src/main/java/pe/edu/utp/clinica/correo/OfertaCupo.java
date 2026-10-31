package pe.edu.utp.clinica.correo;

import pe.edu.utp.clinica.util.Html;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record OfertaCupo(int id,String destinatario,String paciente,String medico,String especialidad,String sede,
                        LocalDateTime fecha,LocalDateTime vence) {
    private static final DateTimeFormatter FORMATO_FECHA=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public Correo correo() {
        String texto="Hola, "+paciente+". Se liberó un cupo compatible con tu lista de espera.\n\nOferta: OF-"+id
                +"\nEspecialidad: "+especialidad+"\nMédico: "+medico+"\nSede: "+sede+"\nFecha y hora de Perú: "+fecha.format(FORMATO_FECHA)
                +"\nResponde antes del "+vence.format(FORMATO_FECHA)+" (hora de Perú)."
                +"\n\nIngresa a tu portal y abre Notificaciones para aceptar o rechazar. El correo no reserva la cita."
                +" Si el plazo terminó, la oferta puede haber pasado a otra persona. Revisa siempre el estado en el portal."
                +"\nSi rechazas o no respondes, conservas tu inscripción y antigüedad. No respondas a este correo.";
        String contenido="""
                <h1 style="margin:0 0 16px;color:#0d2b4e;font-family:Georgia,serif;font-size:28px">Tienes una oferta de cupo</h1>
                <p style="color:#406c96;line-height:1.6">Hola, %s. Se liberó un horario compatible con tu inscripción:</p>
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="10" style="background:#ebf5ff;border-radius:10px;color:#0d2b4e;font-size:14px;table-layout:fixed;word-wrap:break-word">
                <tr><td><strong>Oferta</strong></td><td>OF-%s</td></tr>
                <tr><td><strong>Especialidad</strong></td><td>%s</td></tr>
                <tr><td><strong>Médico</strong></td><td>%s</td></tr>
                <tr><td><strong>Sede</strong></td><td>%s</td></tr>
                <tr><td><strong>Fecha y hora de Perú</strong></td><td>%s</td></tr></table>
                <p style="padding:16px;background:#fff8e6;color:#795000;border-radius:10px;line-height:1.6"><strong>Responde antes del %s (hora de Perú).</strong></p>
                <p style="color:#0d2b4e;line-height:1.6">Ingresa a tu portal y abre <strong>Notificaciones</strong> para aceptar o rechazar. Este correo no reserva la cita; comprueba el estado vigente en el portal.</p>
                <p style="color:#406c96;font-size:14px;line-height:1.6">Si rechazas o no respondes, conservas tu inscripción y antigüedad. No necesitas responder a este correo.</p>
                """.formatted(Html.texto(paciente),id,Html.texto(especialidad),Html.texto(medico),Html.texto(sede),fecha.format(FORMATO_FECHA),vence.format(FORMATO_FECHA));
        return new Correo(destinatario,"Oferta de cupo OF-"+id+" — Sistema de citas",texto,PlantillasCorreo.marco(contenido,"Lista de espera"));
    }

    @Override public String toString(){return "OfertaCupo[privada]";}
}
