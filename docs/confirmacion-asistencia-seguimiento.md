# Confirmación de asistencia y seguimiento de admisión

La cita conserva su estado operativo (`CONFIRMADA`, `EN_SALA`, etc.). La confirmación de asistencia se guarda por separado en `confirmacion_asistencia`; reservar una cita ya no se presenta como una respuesta del paciente.

## Reglas

- El plazo se calcula al reservar: 24 horas antes de la cita, o hasta la hora de la cita cuando se reservó con menos de 24 horas de anticipación. `CONFIRMACION_CITA_HORAS_ANTES` acepta valores entre 1 y 168.
- Una respuesta vencida queda pendiente de gestión; no cambia el estado ni cancela la cita automáticamente.
- El paciente confirma desde **Mis citas**. El servidor obtiene el paciente desde la sesión y valida propiedad, estado, fecha y plazo.
- Admisión solo ve citas futuras de su sede y puede registrar una llamada o un correo que realizó. El formulario guarda canal y resultado, pero no envía mensajes.
- “Confirmó asistencia” solo crea la confirmación cuando el personal registra que el paciente respondió afirmativamente. “Sin respuesta” y “No confirma” permanecen como intentos en el historial y no cancelan la reserva.
- SMS y WhatsApp no forman parte de este flujo porque no hay proveedor configurado.

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
