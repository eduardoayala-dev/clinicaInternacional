# Agendas y reservas locales

El personal administrativo publica turnos desde **Médicos y especialidades → Gestionar turnos** en `/admin/medicos`. Cada turno debe corresponder a la sede del médico activo, tener una fecha y hora futuras, una duración positiva y espacio para al menos una consulta completa. No se permiten bloques publicados que se superpongan para el mismo médico.

Para probar, publicar un bloque en una fecha futura de 09:00 a 11:00 con consultas de 30 minutos. En el portal del paciente elegir la misma sede, especialidad, médico y fecha. Deben aparecer 09:00, 09:30, 10:00 y 10:30 si están libres. Una consulta debe caber entera en el bloque y comenzar en uno de sus intervalos.

## Validación y concurrencia

Las horas clínicas se leen y escriben como `LocalDateTime` mediante JDBC, sin convertirlas a instantes ni depender de la zona horaria del proceso Java. La validación del futuro usa la hora de Lima.

La reserva no crea agendas. El servidor verifica paciente, médico, sede, especialidad, fecha, horario publicado y ocupación. La comprobación y la inserción se ejecutan en una transacción con aislamiento `READ_COMMITTED` y bloqueo de la fila del médico mediante `SELECT ... FOR UPDATE`. Así, las solicitudes de reserva de un mismo médico comprueban la ocupación de forma consecutiva, incluso desde varias instancias de la aplicación.

La reprogramación comprueba además el propietario, el estado y la fecha de la cita original. El cambio de estado anterior, la nueva cita y la notificación se guardan juntos; si falla un paso, se revierten todos. Se rechazan intervalos ocupados aunque los comienzos no coincidan exactamente. Cancelar o reprogramar libera el cupo original. Los estados finales no se pueden reactivar desde el cambio administrativo de estado.

Un turno con citas futuras activas no se puede retirar: antes deben resolverse esas citas. Consultar la sala de espera no modifica las fechas de las citas existentes.

Estas garantías corresponden a los flujos de la aplicación; modificaciones manuales de SQL deben respetar las mismas reglas. El flujo de ofertas de lista de espera conserva su comportamiento y requiere una revisión independiente.

## Contrato de creación

`POST /paciente/reservar` acepta un formulario con `idMedico`, `fecha`, `hora`, `urgencia` y token CSRF. El paciente se obtiene exclusivamente de la sesión. Con `Accept: application/json` devuelve:

```json
{"code":"CITA_RESERVADA","message":"Cita reservada correctamente.","data":{"idCita":72,"codigoCita":"CI-2026-000072"}}
```

El estado de creación es **201**. Datos inválidos reciben **400**, una sesión no autorizada o un token CSRF inválido reciben **403**, un horario ocupado o no publicado recibe **409**, una operación de base de datos fallida recibe **503** y un error inesperado recibe **500**. Los errores usan `code`, `message` y `data: null`.

Los formularios HTML conservan las redirecciones y muestran los errores de agenda en la página. El comprobante aparece solo después de recibir el identificador real y el código del servidor. Ante una pérdida de conexión después de enviar la solicitud, revisar **Mis citas** antes de reintentar: la pérdida de la respuesta no demuestra que la transacción fallara.

## Pruebas

Importar [Agendas y citas locales](postman/Agendas-Citas-Local.postman_collection.json). Configurar `correoPersonal`, `clavePersonal`, `fechaAgenda`, `idMedico` e `idSede`. Ejecutar en orden con cookies activas y sin seguir redirecciones. La colección deja un paciente sintético con citas canceladas o reprogramadas y una agenda publicada para la revisión manual. No utiliza el proveedor de DNI ni modifica la cuenta del usuario.

Las pruebas JDBC automatizadas cubren publicación, ocupación, solicitudes simultáneas, reprogramación concurrente con reserva, cancelación, autorización y rollback. H2 se utiliza exclusivamente para las pruebas aisladas. Para repetir el contrato contra MySQL real, configurar `AGENDA_MYSQL_URL`, `AGENDA_MYSQL_USER` y `AGENDA_MYSQL_PASSWORD`, y ejecutar `mvn -Dtest=AgendaCitaMySqlTest test`.

La base de pruebas debe ser independiente y comenzar con `clinica_agendas_test_`: el contrato elimina y recrea sus tablas de prueba. Nunca apuntar esas variables a `clinica_db` ni a una base que se deba conservar.
