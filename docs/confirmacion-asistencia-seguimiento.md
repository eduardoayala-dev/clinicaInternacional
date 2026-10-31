# Confirmación de asistencia y seguimiento de admisión

La cita conserva su estado operativo (`CONFIRMADA`, `EN_SALA`, etc.). La confirmación de asistencia se guarda por separado en `confirmacion_asistencia`; reservar una cita ya no se presenta como una respuesta del paciente.

## Reglas

- El plazo se calcula al reservar: 24 horas antes de la cita, o hasta la hora de la cita cuando se reservó con menos de 24 horas de anticipación. `CONFIRMACION_CITA_HORAS_ANTES` acepta valores entre 1 y 168.
- Una respuesta vencida queda pendiente de gestión; no cambia el estado ni cancela la cita automáticamente.
- El paciente confirma desde **Mis citas**. El servidor obtiene el paciente desde la sesión y valida propiedad, estado, fecha y plazo.
- Admisión solo ve citas futuras de su sede y puede registrar una llamada o un correo que realizó. El formulario guarda canal y resultado, pero no envía mensajes.
- “Confirmó asistencia” solo crea la confirmación cuando el personal registra que el paciente respondió afirmativamente. “Sin respuesta” y “No confirma” permanecen como intentos en el historial y no cancelan la reserva.
- SMS y WhatsApp no forman parte de este flujo porque no hay proveedor configurado.
- El panel de admisión recibe eventos por sede tras el commit de reservas, reprogramaciones, cancelaciones, confirmaciones y gestiones. Reconcilia cada 60 segundos para reflejar plazos vencidos y usa consultas cada 15 segundos como respaldo si se interrumpe el canal. Solo funciona con la pestaña visible y pausa los cambios de tabla al preparar una gestión.

## Actualización de base de datos

Aplicar una sola vez `sql/migrations/V010__confirmacion_asistencia_seguimiento.sql` sobre la base existente, después de generar y comprobar un respaldo. La migración crea las tablas y prepara plazos para citas futuras activas; no cambia estados ni datos clínicos. Para una base nueva, `sql/schema.sql` ya contiene las tablas.

En Railway, se agrega `CONFIRMACION_CITA_HORAS_ANTES=24` como variable del servicio Java cuando se programe el despliegue. En local, la variable está documentada en `.env.example` y la lee Docker Compose.

## Recorrido de prueba local

1. Un paciente reserva una cita futura disponible.
2. En **Mis citas** ve “Cita programada”, el plazo y **Confirmar asistencia**. Confirmar dos veces no crea registros adicionales.
3. Comprueba que la cita conserva el estado programado y que aparece “Asistencia confirmada”.
4. En una sesión de personal administrativo abre **Confirmaciones y seguimiento**, registra un intento “Sin respuesta” y verifica que la cita continúa programada.
5. Registra un contacto con “Confirmó asistencia” y verifica que desaparece de pendientes y queda identificado como confirmación de admisión.
6. Verifica que el personal de otra sede no puede consultar ni modificar esa cita, y que una cita ajena no puede confirmarse con una petición manipulada.

## Confirmar desde Notificaciones

Mis citas y Notificaciones utilizan la misma comprobación de propietario, estado futuro y plazo, y escriben en `confirmacion_asistencia`. Confirmar no cambia el estado operativo de `cita`. El aviso de reserva y los recordatorios muestran la respuesta de asistencia y el plazo disponible.

Leer una notificación, individualmente o mediante Marcar todas como leídas, solo cambia su lectura. El botón Confirmar asistencia sigue disponible en los avisos leídos mientras la cita sea confirmable. Ver y Confirmar asistencia se presentan como acciones separadas.

`POST /paciente/notificaciones` con `action=confirmar&id=<idNotificacion>` devuelve `ASISTENCIA_CONFIRMADA`. El alias `accion=confirmarCita&idNotificacion=...` utiliza la misma operación. Los errores de plazo y de estado conservan sus códigos, sin marcar el aviso como leído ni confirmar parcialmente. El paciente se obtiene de la sesión; una notificación ajena o vinculada a una cita ajena no permite confirmar.

La asistencia y la lectura del aviso se guardan en una sola transacción. El evento de admisión se emite después del commit, y una confirmación repetida no duplica registros. Confirmar desde Mis citas actualiza el estado visible de todos los avisos relacionados al volver a consultar Notificaciones, sin marcar esos avisos como leídos. No requiere otra migración además de V010.

Comprobación manual: reservar una cita futura con plazo abierto, abrir el aviso en Notificaciones y marcarlo como leído. Verificar que la asistencia sigue pendiente y el botón permanece disponible. Confirmar desde allí y revisar Mis citas y el seguimiento de admisión. Con otra cita, confirmar primero desde Mis citas y comprobar que Notificaciones muestra Asistencia confirmada sin ofrecer otra confirmación.

## Canal de eventos

- Endpoint autenticado: `GET /admin/eventos-confirmaciones`. La sede se obtiene del perfil de admisión, nunca de un parámetro del navegador.
- Los eventos solo avisan de un cambio; no contienen nombres, documentos, correos ni detalles de citas. La tabla se consulta por el acceso habitual con permisos.
- Se usa I/O asíncrona de Tomcat, con un máximo de 128 conexiones y 3 por usuario. Las conexiones se renuevan como máximo cada 45 segundos para volver a comprobar el acceso.
- El canal no renueva el tiempo de actividad JWT. Cerrar u ocultar la pantalla cierra la conexión.
- El publicador está en memoria y corresponde al despliegue actual de una instancia. Un despliegue con varias réplicas requerirá un transporte compartido para avisos inmediatos entre ellas; la reconciliación periódica sigue recuperando el estado desde MySQL.
- La dependencia Tomcat embebido es solo de pruebas y no se incluye en el WAR.
- Tomcat standalone no ofrece un executor administrado de Jakarta EE. El servlet mantiene un único temporizador daemon compartido para los latidos y lo detiene en `destroy()`, junto con sus canales. Por ese modelo de despliegue se excluye únicamente su inicialización de la regla `java:S2654`; no se crea un hilo por conexión. Si se migra a un servidor Jakarta EE completo, debe usarse su executor administrado.
