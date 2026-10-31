# Confirmaciones de cita por correo

La reserva guarda en la misma transacción la cita, el aviso de plataforma y un
pendiente en `correo_cita`. El envío con Gmail ocurre después, en segundo plano.
Esto se aplica a las reservas web, las creadas por admisión y la aceptación de
ofertas de lista de espera: todas usan la reserva validada de agenda. Una
reprogramación genera un pendiente para la cita nueva.

El correo utiliza HTML con alternativa de texto y muestra número de cita, fecha,
hora de Perú, médico, especialidad y sede. No incluye DNI, diagnósticos, enlaces
con credenciales ni información de salud. No solicita una segunda confirmación:
la cita ya quedó registrada. Se usa el correo actual de la cuenta al procesar.

## Persistencia y fallos

`correo_cita` es una tabla técnica con una fila por cita, no otra entidad clínica.
Contiene la clave de cita, estado, intentos y fechas; no copia correos ni claves.
La clave primaria impide registrar dos trabajos para la misma cita.

El procesador reclama un trabajo en una transacción breve y libera la conexión
antes de enviar. Dos procesos no reclaman la misma fila. Se omiten citas que ya
no están confirmadas o cuya fecha pasó, y cuentas de pacientes inactivas.

- `PENDIENTE`: espera su turno o un reintento permitido.
- `ENVIANDO`: un proceso reclamó el trabajo.
- `ENVIADO`: Gmail aceptó el mensaje; no demuestra recepción ni lectura.
- `OMITIDO`: la cita dejó de ser vigente.
- `NO_CONFIRMADO`: no se pudo confirmar el envío; requiere revisión.

Se permiten hasta tres intentos cuando el transporte indica un fallo reintentable,
con esperas de un minuto y cinco minutos. Ante un fallo no reintentable o un envío
interrumpido durante más de quince minutos, no hay reenvío automático. Tampoco se
reenvía automáticamente si Gmail aceptó el mensaje pero falló el registro de ese
resultado. Esto reduce duplicados; no garantiza entrega exactamente una vez.
Una caída del proveedor no borra la cita confirmada.

El arranque y las operaciones POST despiertan al procesador. Se agrupan los avisos
en una única tarea pendiente. Solo se programa una nueva ejecución si quedan
trabajos o envíos por revisar. No se consulta periódicamente una cola vacía ni se
crea un heartbeat. Si falla la base, los pendientes se retoman ante otra operación
o en el siguiente arranque.

## Configuración

1. Respaldar la base existente y aplicar únicamente
   `sql/migrations/V008__correo_confirmacion_cita.sql`. Es idempotente y no modifica
   las filas de citas o usuarios. Añade una tabla técnica; la entidad de propuestas
   de horarios se incorpora por separado mediante V009.
2. Conservar las credenciales privadas de Gmail ya configuradas.
3. Añadir `APPOINTMENT_EMAIL_ENABLED=true` a `.env.mail` en local o a las variables
   del servicio Java en Railway. Requiere `MAIL_MODE=gmail`.
4. Reiniciar únicamente Java para leer la variable. En local, usar
   `./scripts/Iniciar-Local.ps1 -Jwt -Correo`.

El valor predeterminado es `false`: la reserva anterior sigue funcionando sin la
tabla nueva si esta función aún no se activa. Al activarla, disponibilidad valida
que la migración exista. No se encolan reservas antiguas de forma retroactiva.
No ejecutar `schema.sql` sobre una base existente.

La pantalla de notificaciones mantiene su aviso de plataforma. Marcarlo como leído
no altera el estado de salida del correo. No se envían SMS ni WhatsApp.

## Prueba manual

Reservar una cita futura usando una cuenta propia con acceso al correo asociado.
Comprobar el aviso en el portal y el correo con los mismos datos. Revisar también
spam. Una reserva rechazada por horario ocupado no genera correo. Si se
reprograma antes de que se procese el aviso anterior, solo se confirma la nueva
cita vigente. Las pruebas automatizadas usan un transporte simulado, sin enviar
correos reales ni consultar DNI.

El usuario confirmó que la reserva aparece en Mis citas y recibe el correo con
fecha, hora, médico y sede coincidentes. También confirmó la visualización de
citas pasadas en el historial. Esta aceptación permite preparar los commits y PR
de ambas entregas por separado.
