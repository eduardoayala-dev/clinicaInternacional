# Aviso de confirmación vencida

RF14. Cuando una cita futura sigue pendiente de asistencia y alcanza su plazo de confirmación, se encola una sola vez CONFIRMACION_VENCIDA en correo_cambio_cita. El mensaje pide coordinar con admisión y mantiene la reserva vigente. No cancela citas, no determina prioridad clínica y no calcula una predicción de IA.

Se reutilizan la toma exclusiva, los reintentos limitados y el control de entregas inciertas de la cola existente. La clave (id_cita,tipo) evita reiniciar mensajes procesados, incluso tras otro ciclo o un reinicio. Se revalidan cuenta activa, estado, plazo y asistencia al enviar; confirmar, cancelar, reprogramar o pasar la hora de la cita omite un aviso pendiente.

El recordatorio ordinario solo se programa antes del plazo. Si un recordatorio anterior sigue pendiente al vencer, se omite; no se envía además del aviso vencido. Ambos se programan en el mismo ciclo con un instante común. Esto conserva la confirmación tardía por admisión y la cancelación manual con contactos mínimos.

La programación continúa vinculada a RECORDATORIOS_EMAIL_ENABLED y APPOINTMENT_EMAIL_ENABLED; habilitar correo no activa por sí solo el programador. El ciclo usa la anticipación guardada y despierta ambas colas. Durante la suspensión de Java no hay ejecución: al volver solo se consideran citas todavía futuras. Los avisos automáticos no cuentan como contactos humanos registrados.

## Migración y despliegue

V020 amplía la restricción de tipos permitidos en correo_cambio_cita. No crea tablas ni columnas ni modifica citas. Añade primero ck_aviso_cambio_tipo_v2 y después retira la restricción anterior. Si solo se aplica el primer paso, la restricción antigua continúa protegiendo los datos y readiness rechaza el esquema incompleto. La sintaxis de ALTER TABLE para restricciones CHECK se documenta en el [manual de MySQL 8.0](https://docs.oracle.com/cd/E17952_01/mysql-8.0-en/alter-table.html).

Aplicar una vez con respaldo y antes del WAR. Mantener el programador deshabilitado durante la actualización de instancias y activarlo al terminar, para evitar que un trabajador antiguo intente procesar el nuevo tipo de aviso. No desplegar Railway en esta fase. El usuario aprobó el flujo local el 2 de noviembre de 2026 y autorizó publicar los cambios. Se conserva la rama.

## Verificación automatizada

Las pruebas cubren el límite exacto, programación repetida y concurrente, conservación del estado y prioridad, omisión al confirmar/cancelar/reprogramar, cita pasada, cuenta inactiva y entrega incierta. La prueba MySQL de migración comprueba el estado anterior, la actualización parcial y la actualización completa, manteniendo el rechazo de tipos no permitidos. Se usa un emisor ficticio; estas pruebas no envían correos reales.
