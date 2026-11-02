# Recordatorio configurable y contactos antes de cancelar

RF10, RF14 y RF18. V019 añade recordatorio_horas (1–168, inicial 48) e intentos_confirmacion (1–10, inicial 2) a configuracion_ofertas. No crea tablas nuevas ni cambia citas existentes. Se conserva la auditoría y la versión optimista del formulario del administrador. Los métodos anteriores de guardado preservan los dos parámetros nuevos.

El programador consulta la anticipación en cada ciclo desde la configuración persistida, si RECORDATORIOS_EMAIL_ENABLED está habilitado junto al correo. RECORDATORIO_CITA_HORAS_ANTES deja de ser la fuente de ese valor. Cambiar la anticipación no reinicia envíos ya procesados ni modifica los plazos de asistencia guardados. La activación global del envío sigue siendo una opción del despliegue.

En Seguimiento de confirmaciones, admisión puede cancelar por falta de confirmación únicamente si la cita es de su sede, sigue pendiente de asistencia, es futura, su plazo ha vencido y cuenta con los contactos mínimos realizados. Solo cuentan SIN_RESPUESTA y NO_CONFIRMA del registro de gestión, no los correos automáticos. El botón pide confirmación; no hay cancelación automática ni al cambiar de día ni al vencer un plazo.

La comprobación, cancelación, auditoría y cola del correo ocurren en una transacción. Se bloquea el paciente antes de leer y bloquear la cita; una confirmación simultánea impide un estado confirmado/cancelado contradictorio. Se conserva la versión de política aplicada, el número de contactos y el actor en auditoría. No se permite usar esta operación para una cita con llegada registrada. Las cancelaciones operativas por otros motivos conservan su flujo independiente; su motivo obligatorio sigue pendiente.

## Comprobación local

Aplicar V019 con respaldo antes del WAR. Como administrador, cambiar las horas del recordatorio y contactos mínimos, guardar y volver a entrar: deben persistir. Como admisión, Seguimiento de confirmaciones debe mostrar el mínimo configurado. Antes del plazo o del mínimo, el servidor rechaza la cancelación incluso si se manipula el formulario. Tras ambos requisitos y sin asistencia confirmada, aparece la acción de cancelación con confirmación explícita.

Las pruebas con reloj controlado cubren el límite exacto, mínimo vigente, roles, otra sede, auditoría y confirmación simultánea. No requieren alterar fechas de citas reales ni enviar correos a terceros. El usuario confirmó la prueba manual del panel y del mínimo mostrado en admisión el 02/11/2026. No se modifica Railway.

