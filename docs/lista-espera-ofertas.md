# Lista de espera y ofertas de cupos

Una inscripción utiliza especialidad, sede opcional, médico preferido opcional, horario y aviso mínimo de 3 o 12 horas. La disponibilidad de aviso no cambia la prioridad clínica. Las ofertas se entregan en el portal y por correo cuando el envío está habilitado y la inscripción incluye CORREO. SMS y WhatsApp no tienen un proveedor activo.

Al cancelarse una cita futura de un turno publicado, el sistema busca una inscripción compatible y activa. Ordena por prioridad clínica y antigüedad. Ofrece el cupo durante 15 minutos, sin superar el inicio de la cita. Durante ese plazo, el catálogo y las reservas normales excluyen el intervalo ofertado.

Aceptar verifica sesión, propiedad de la oferta, vencimiento y disponibilidad dentro de una transacción. Crea una cita real y su notificación, relaciona la oferta con esa cita y marca la inscripción como atendida. Una aceptación repetida devuelve el mismo identificador sin duplicar citas. Rechazar conserva la inscripción y ofrece el cupo al siguiente candidato compatible. Una oferta rechazada o vencida no vuelve a ofrecerse a esa misma inscripción para la misma cita original.

Un proceso de mantenimiento revisa las ofertas cada 60 segundos mientras la aplicación está encendida. Libera las vencidas y busca nuevos candidatos. La validez se comprueba también al reservar o aceptar; no depende del contador del navegador. Retirarse de la lista no cancela una cita confirmada.

## Correo de la oferta

`APPOINTMENT_EMAIL_ENABLED=true` habilita las confirmaciones de reserva y las ofertas mediante la configuración Gmail existente. Cada oferta encola un único registro en `correo_oferta` dentro de la misma transacción que el aviso del portal. Una caída no deja una oferta creada sin su tarea de correo. El envío ocurre fuera de la petición y de la transacción de reserva.

Antes de enviar se comprueban el destinatario actual, la cuenta activa, la inscripción ofertada, la agenda y el plazo. Se omiten ofertas aceptadas, rechazadas, retiradas, vencidas o no disponibles. El mensaje HTML incluye el horario, la sede, el médico y el límite de respuesta en hora de Perú; dirige al paciente a Notificaciones y no acepta ni reserva por abrir el correo. Siempre prevalece el estado del portal: una oferta puede vencer mientras el proveedor entrega el mensaje.

Reservas y ofertas comparten el procesador, la toma exclusiva de trabajos y los reintentos persistentes. Los fallos temporales se reintentan a los 60 y 300 segundos, hasta tres intentos, comprobando otra vez la vigencia. Una entrega incierta no se reenvía automáticamente; un trabajo interrumpido queda `NO_CONFIRMADO` después de 15 minutos. Un fallo de base de datos vuelve a despertar el procesador a los 60 segundos. El mantenimiento también despierta el envío de las nuevas ofertas que genera sin una petición del navegador.

Aceptar una oferta reserva la cita y encola su confirmación de reserva. La confirmación de asistencia sigue siendo una operación posterior desde Mis citas.

La toma de trabajos usa `READ COMMITTED` y `FOR UPDATE SKIP LOCKED` para que varios trabajadores no compitan por el mismo mensaje ni bloqueen los trabajos libres. Este aislamiento se utiliza únicamente en la cola de correos; no cambia las transacciones de reserva.

## Base de datos existente

Aplicar `sql/migrations/V002__ofertas_lista_espera.sql` con una cuenta de migración, después de un respaldo. No ejecutar `sql/schema.sql` sobre una base con datos. El usuario de la aplicación mantiene permisos de operación, sin permisos de modificación del esquema. En este entorno local la migración ya se aplicó y el respaldo está en `target/lista-espera/respaldo-antes-v002.sql`.

Para habilitar el correo de ofertas en una base existente, aplicar además `sql/migrations/V011__correo_oferta_cupo.sql` antes de desplegar esta versión. Crea una tabla de cola sin modificar citas ni pacientes y puede ejecutarse nuevamente. No encola retrospectivamente ofertas anteriores. La comprobación de disponibilidad valida la tabla si el envío está habilitado. `sql/schema.sql` ya la incluye para instalaciones nuevas.

## Contratos HTTP

Enviar `Accept: application/json` para recibir `{code,message,data}`. Los POST requieren la sesión y su token CSRF. El paciente siempre se obtiene de la sesión.

| Operación | Método y ruta | Resultado |
| --- | --- | --- |
| Consultar inscripción | GET `/paciente/lista-espera` | 200; `data` puede ser null |
| Inscribirse | POST `/paciente/lista-espera`, `action=inscribir` | 201 e `idListaEspera` |
| Retirarse | POST `/paciente/lista-espera`, `action=retirar`, `idListaEspera` | 200 |
| Consultar bandeja y oferta | GET `/paciente/notificaciones` | 200; oferta y plazo reales |
| Aceptar oferta | POST `/paciente/notificaciones`, `accion=aceptar`, `idOferta` | 200 e `idCita` |
| Rechazar oferta | POST `/paciente/notificaciones`, `accion=rechazar`, `idOferta` | 200 |
| Marcar aviso propio | POST `/paciente/notificaciones`, `action=marcarLeida`, `id` | 200 |

Los identificadores inválidos devuelven 400; una sesión o rol incorrecto, 403; un recurso ajeno o inexistente, 404; un estado incompatible, 409; una oferta vencida, 410; y un fallo de base de datos, 503. Los formularios HTML utilizan redirección y mensajes visibles. Los GET con acciones de escritura devuelven 405.

## Comprobación local

Importar `docs/postman/Lista-Espera-Local.postman_collection.json`. Configurar `correoPersonal`, `clavePersonal`, `fechaAgenda` futura, `idMedico`, `idSede` e `idEspecialidad` compatibles. Activar cookies y desactivar el seguimiento automático de redirecciones. Ejecutar en orden únicamente en local: crea dos pacientes sintéticos, comprueba la oferta, propiedad, bloqueo del cupo y aceptación repetida; finalmente cancela la cita sintética. Conserva las cuentas de prueba y la agenda. No consulta al proveedor de DNI.

Para probar en pantalla, usar dos sesiones independientes (por ejemplo, navegador normal e incógnito), porque las pestañas de una misma sesión comparten la cuenta:

1. El paciente A reserva una cita futura. Elegir una fecha con más de 12 horas de anticipación facilita comprobar ambas preferencias de aviso.
2. El paciente B se inscribe en la lista de espera con la misma especialidad, sede, médico si lo especifica y turno de esa cita. Mantener Correo como contacto y comprobar que la cuenta tiene una dirección válida.
3. A cancela la cita desde Mis citas. B abre Notificaciones y comprueba el horario y el plazo de la oferta. El aviso de correo llega a la dirección actual de B cuando Gmail está habilitado.
4. B acepta. La cita debe aparecer una sola vez en Mis citas, la inscripción debe quedar atendida y debe llegar el correo de reserva. La asistencia sigue pendiente hasta confirmarla por separado.
5. Para comprobar el rechazo, usar otra oferta y otro candidato compatible. Rechazar debe conservar la inscripción de B y pasar el cupo al siguiente candidato. Si no hay otro candidato, el horario queda disponible para reserva normal.
6. Sin responder, el plazo vence a los 15 minutos. Aceptar con una pantalla antigua no debe crear una cita. El mantenimiento libera la oferta y selecciona otro candidato en su siguiente revisión, como máximo cada 60 segundos mientras la aplicación esté activa.

Las pruebas automatizadas utilizan emisores en memoria y no envían mensajes reales ni consultan la API de DNI. Incluyen dos trabajadores de correo simultáneos, aceptación simultánea, vencimiento exacto y rollback del encolado.
