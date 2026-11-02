# Avisos de cancelación y reprogramación

RF10 y RF14. V018 añade correo_cambio_cita, con una fila por cita y tipo de cambio. Se conserva el historial de entregas de la confirmación inicial y no se reutilizan sus estados para otro mensaje.

La cancelación del paciente, la cancelación por admisión y la reprogramación encolan el aviso dentro de la transacción que cambia la cita. Si falla la cola, se revierte la operación. Solo se encola con APPOINTMENT_EMAIL_ENABLED=true. Con la función deshabilitada no se necesita la tabla para estas operaciones. Con la función habilitada, readiness exige el esquema V018.

La reprogramación avisa de ambos códigos y horarios. No encola además una confirmación inicial para la nueva reserva. Si la cita se vuelve a reprogramar o cancelar antes de enviar, el aviso anterior se omite y se procesa el cambio vigente. Cancelar repetidamente no vuelve a encolar la misma cancelación. Los correos usan el destinatario actual y solo se envían a cuentas activas de pacientes. La sede se obtiene del bloque reservado, aunque el médico cambie de sede después.

El despachador usa toma exclusiva y reintentos limitados de CorreosServicio; una entrega incierta no se repite automáticamente. La notificación del portal y los eventos de agenda conservan su funcionamiento; los eventos se publican después de confirmar la transacción. El envío externo ocurre fuera de la transacción.

El contenido HTML escapa nombres y datos de la base. No incluye diagnósticos ni medicación. Estos avisos son informativos y no ejecutan operaciones desde el correo. Los enlaces de acción de un solo uso, el motivo obligatorio de cancelación y la política mínima de contactos siguen pendientes.

## Prueba local

Aplicar V018 con respaldo antes del WAR y del envío habilitado. Reservar una cita propia futura, reprogramarla y comprobar un aviso con el horario anterior y el nuevo. Cancelar la nueva cita y comprobar su aviso específico. Mis citas debe mostrar el estado correspondiente. No repetir acciones si el envío está en estado incierto; revisar el resultado primero. Para probar duplicados y fallos se usa una base temporal y un emisor ficticio, sin correos reales.

La integración para revisión se trabaja en feature/notificaciones-citas, conservando las ramas previas de recordatorios y avisos. No se despliega Railway en esta fase.


## Comprobación local del 02/11/2026

783 pruebas Java y 153 MySQL integradas aprobadas. Tras corregir la caché de JSP, el usuario reservó la cita 52 con el formulario actual. Sobre esa cita y con autorización expresa se procesó un recordatorio, se reprogramó a la cita 53 y se canceló la nueva. Las tres colas terminaron ENVIADO con un intento, usando exclusivamente la dirección autorizada. Gmail aceptó los envíos y el usuario confirmó la recepción. La mejora visual de las plantillas se pospone por indicación del usuario. Estado final: 52 REPROGRAMADA, 53 CANCELADA.

Se detectó una vista JSP compilada anterior en el contenedor: enviaba motivo mientras el servlet exigía idMotivo. La fuente actual ya era correcta, pero los archivos del WAR conservaban fechas internas iguales y la compilación anterior permanecía en work. Iniciar-Tomcat.sh ahora elimina únicamente org/apache/jsp del directorio de trabajo de ROOT al arrancar, para regenerar las vistas. Conserva la base, la aplicación y los archivos de sesión. Se comprobó la vista regenerada con idMotivo y el usuario pudo reservar. La preparación explícita del contrato de reserva también se verifica con ocho pruebas JavaScript y pruebas del servlet.

