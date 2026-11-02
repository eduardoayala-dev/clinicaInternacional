# Recordatorios de asistencia por correo

Entrega incremental de RF14. V017 crea correo_recordatorio: una fila por cita, independiente de la confirmación inicial de reserva. No cambia citas ni confirma asistencia. El envío utiliza Gmail y las plantillas HTML existentes.

Con APPOINTMENT_EMAIL_ENABLED=true, MAIL_MODE=gmail y RECORDATORIOS_EMAIL_ENABLED=true se inicia un ciclo cada 60 segundos mientras Java esté activo. Desde V019, el administrador configura la anticipación en el panel (1–168 horas, inicial 48); se consulta en cada ciclo. APP_PUBLIC_URL determina el enlace del portal; nunca se deriva del encabezado Host de una petición. La activación global del envío sigue siendo del despliegue.

Solo se encolan citas futuras CONFIRMADA cuya asistencia esté pendiente y con paciente activo. Se procesan lotes de 100. La clave primaria y el guardado sin sobrescritura impiden reiniciar una entrega por otro ciclo o proceso. Antes de enviar se vuelve a comprobar la cita y la asistencia. Una cancelación, reprogramación, asistencia confirmada o cuenta inactiva omite el mensaje pendiente. Una cita reprogramada recibe una fila propia.

Se reutiliza CorreosServicio: toma exclusiva, tres intentos como máximo, esperas de 60 y 300 segundos para fallos recuperables; una entrega incierta queda NO_CONFIRMADO y no se reenvía automáticamente. No se promete entrega exactamente una vez a través de un proveedor externo. No se registran destinatarios, síntomas ni contenido en los logs.

El correo distingue reserva de asistencia y lleva a Mis citas. Acceder al enlace no modifica datos y requiere iniciar sesión; confirmar o cancelar conserva las autorizaciones y comprobaciones del portal. Los avisos de cambios y la política de contactos ya están implementados. Los enlaces autenticados de acción están en revisión; no se habilita acceso anónimo mediante tokens. Este envío no cuenta como un contacto humano comprobado.

Railway puede suspender Java: durante ese tiempo no corre el programador. Al despertar recupera citas que todavía son futuras y están dentro del margen; no envía recordatorios de citas pasadas. Esto no instala un heartbeat ni evita la suspensión.

## Validación

Pruebas con emisor ficticio verifican duplicados, programación concurrente, margen, confirmación, cancelación, reprogramación y entrega incierta. No envían mensajes reales. Aplicar V017 con respaldo antes de activar RECORDATORIOS_EMAIL_ENABLED. Readiness comprueba la tabla cuando la función está habilitada.

Para probar el envío real, usar una cita propia futura con asistencia pendiente y una dirección autorizada, habilitar las variables del servidor local y comprobar que llega un recordatorio HTML y que el enlace abre Mis citas después del login. Dos ciclos no deben generar otro recordatorio. No activar en Railway antes de revisar esta prueba.
