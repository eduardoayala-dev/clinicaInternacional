package pe.edu.utp.clinica.correo;

import pe.edu.utp.clinica.util.Html;

public final class PlantillasCorreo {
    private PlantillasCorreo() {}

    public static Correo verificacion(String destinatario,String operacion,String codigo) {
        if(codigo==null || !codigo.matches("\\d{6}"))throw new IllegalArgumentException("Código de verificación inválido.");
        String texto="Tu código para "+operacion+" es: "+codigo
                +"\n\nVence en 10 minutos y sólo puede utilizarse una vez. Tienes hasta 5 intentos."
                +"\nIngresa el código en la ventana donde solicitaste el cambio. No compartas este código."
                +"\nSi no solicitaste esta operación, ignora este mensaje: no se ha modificado tu cuenta."
                +"\n\nProyecto universitario — Sistema de citas";
        String contenido="""
                <p style="margin:0 0 10px;color:#1668b8;font-size:12px;font-weight:bold;letter-spacing:1px;text-transform:uppercase">Seguridad de tu cuenta</p>
                <h1 style="margin:0 0 16px;color:#0d2b4e;font-family:Georgia,serif;font-size:28px;line-height:1.25">Tu código de verificación</h1>
                <p style="margin:0 0 24px;color:#406c96;font-size:15px;line-height:1.6">Usa este código para <strong>%s</strong>.</p>
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"><tr><td align="center" style="padding:24px 12px;background:#ebf5ff;border:1px solid #daeaf8;border-radius:12px">
                <span style="font-family:Consolas,'Courier New',monospace;font-size:36px;font-weight:bold;letter-spacing:7px;color:#0b4f8a">%s</span>
                </td></tr></table>
                <p style="margin:18px 0 24px;text-align:center;color:#406c96;font-size:13px">Válido por <strong>10 minutos</strong> · Hasta <strong>5 intentos</strong></p>
                <p style="margin:0 0 18px;font-size:14px;line-height:1.6;color:#0d2b4e">Vuelve a la ventana donde solicitaste el cambio e ingresa el código. No necesitas responder a este correo.</p>
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"><tr><td style="padding:16px;background:#f7fbff;border-radius:10px;color:#406c96;font-size:13px;line-height:1.6">
                <strong>No compartas este código.</strong><br>Si no solicitaste esta operación, puedes ignorar este mensaje. Tu cuenta no se ha modificado.
                </td></tr></table>
                """.formatted(Html.texto(operacion),Html.texto(codigo));
        return new Correo(destinatario,"Código de verificación — Sistema de citas",texto,marco(contenido));
    }

    public static Correo correoActualizado(String destinatario) {
        String texto="Se completó un cambio de correo en tu cuenta tras verificar ambas direcciones. Las otras sesiones quedaron cerradas. Si no reconoces esta operación, contacta al administrador del sistema.\n\nProyecto universitario — Sistema de citas";
        String contenido="""
                <p style="margin:0 0 10px;color:#1668b8;font-size:12px;font-weight:bold;letter-spacing:1px;text-transform:uppercase">Aviso de seguridad</p>
                <h1 style="margin:0 0 16px;color:#0d2b4e;font-family:Georgia,serif;font-size:28px">Tu correo se actualizó</h1>
                <p style="margin:0 0 18px;color:#406c96;font-size:15px;line-height:1.6">El cambio se completó después de verificar tu dirección anterior y la nueva.</p>
                <p style="margin:0 0 24px;color:#0d2b4e;font-size:14px;line-height:1.6">Tus otras sesiones quedaron cerradas para proteger tu cuenta.</p>
                <p style="margin:0;padding:16px;background:#ebf5ff;border-radius:10px;color:#0d2b4e;font-size:14px;line-height:1.6"><strong>¿No reconoces el cambio?</strong><br>Contacta al administrador del sistema para revisar el acceso a tu cuenta.</p>
                """;
        return new Correo(destinatario,"Tu correo de acceso se actualizó — Sistema de citas",texto,marco(contenido));
    }

    private static String marco(String contenido) {
        return """
                <!doctype html><html lang="es"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Seguridad de cuenta</title></head>
                <body style="margin:0;padding:0;background:#f1f5f9;font-family:Arial,Helvetica,sans-serif">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f1f5f9"><tr><td align="center" style="padding:32px 12px">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#fff;border:1px solid #daeaf8;border-radius:16px">
                <tr><td style="padding:28px 32px;background:#0b4f8a;border-radius:16px 16px 0 0;color:#fff"><p style="margin:0;font-size:20px;font-weight:bold">Clínica Internacional</p><p style="margin:6px 0 0;font-size:13px;color:#c6e4ff">Sistema de citas · Seguridad de cuenta</p></td></tr>
                <tr><td style="padding:32px">%s</td></tr>
                <tr><td style="padding:20px 32px;border-top:1px solid #daeaf8;color:#64748b;font-size:12px;line-height:1.6">Proyecto universitario · Mensaje automático<br>No respondas con tu contraseña ni compartas códigos de verificación.</td></tr>
                </table></td></tr></table></body></html>
                """.formatted(contenido);
    }
}
