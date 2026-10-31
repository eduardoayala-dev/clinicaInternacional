package pe.edu.utp.clinica.correo;

import pe.edu.utp.clinica.util.Html;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record ConfirmacionCita(int id,String destinatario,String paciente,String medico,String especialidad,String sede,LocalDateTime fecha) {
    private static final DateTimeFormatter FECHA=DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA=DateTimeFormatter.ofPattern("HH:mm");

    public Correo correo() {
        String codigo="CIT-"+id;
        String texto="Hola, "+paciente+". Tu cita está confirmada.\n\nCódigo: "+codigo+"\nFecha: "+fecha.format(FECHA)
                +"\nHora de Perú: "+fecha.format(HORA)+"\nMédico: "+medico+"\nEspecialidad: "+especialidad+"\nSede: "+sede
                +"\n\nPuedes consultar el estado actualizado de tu cita en tu portal. Si necesitas cambiarla o cancelarla, ingresa a tu cuenta."
                +"\nEste correo es informativo y no requiere responder ni confirmar nuevamente tu reserva.";
        String contenido="""
                <p style="margin:0 0 10px;color:#1668b8;font-size:12px;font-weight:bold;letter-spacing:1px">RESERVA CONFIRMADA</p>
                <h1 style="margin:0 0 16px;color:#0d2b4e;font-family:Georgia,serif;font-size:28px">Tu cita está registrada</h1>
                <p style="color:#406c96;line-height:1.6">Hola, %s. Estos son los datos de tu reserva:</p>
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="10" style="background:#ebf5ff;border-radius:10px;color:#0d2b4e;font-size:14px;table-layout:fixed;word-wrap:break-word">
                <tr><td><strong>Código</strong></td><td>%s</td></tr>
                <tr><td><strong>Fecha</strong></td><td>%s</td></tr>
                <tr><td><strong>Hora de Perú</strong></td><td>%s</td></tr>
                <tr><td><strong>Médico</strong></td><td>%s</td></tr>
                <tr><td><strong>Especialidad</strong></td><td>%s</td></tr>
                <tr><td><strong>Sede</strong></td><td>%s</td></tr></table>
                <p style="color:#406c96;line-height:1.6;font-size:14px">Consulta el estado actualizado en tu portal. Para cambiar o cancelar tu cita, ingresa a tu cuenta.</p>
                <p style="color:#64748b;font-size:12px;line-height:1.6">Este correo es informativo. No necesitas responder ni confirmar nuevamente la reserva.</p>
                """.formatted(Html.texto(paciente),codigo,fecha.format(FECHA),fecha.format(HORA),Html.texto(medico),Html.texto(especialidad),Html.texto(sede));
        return new Correo(destinatario,"Confirmación de cita "+codigo+" — Sistema de citas",texto,PlantillasCorreo.marco(contenido,"Confirmación de cita"));
    }

    @Override public String toString(){return "ConfirmacionCita[privada]";}
}
