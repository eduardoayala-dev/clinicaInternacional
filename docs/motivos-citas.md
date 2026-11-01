# Motivo de consulta en reservas y reprogramaciones

Ambos formularios consultan `motivo_cita`, incluyendo únicamente motivos y especialidades activos. Los identificadores vienen de la base; no se generan a partir de la posición del selector ni del nombre de la especialidad. Un catálogo vacío no produce opciones de ejemplo y un fallo de lectura se informa como indisponibilidad.

La reserva de paciente envía `idMotivo`, un entero positivo obligatorio, junto a `idMedico`, `fecha`, `hora` y la protección CSRF existente. La reprogramación utiliza el mismo nombre `idMotivo`; allí continúa siendo opcional. En ambos casos el servidor comprueba que el motivo indicado esté activo y pertenezca a la especialidad del médico. Una petición inválida no crea una cita ni una notificación.

El paciente se obtiene de la sesión. El cliente no determina el propietario de la reserva. La comprobación del motivo y el guardado de `cita.id_motivo` ocurren en la misma transacción de la agenda.

Las colecciones de Postman Agendas y Lista de espera incluyen `motivoId`. Su valor inicial es 1, correspondiente a Cardiología en el esquema inicial: cambiarlo si se prueba con otra especialidad o una base diferente.

Prueba manual:

1. Programar una cita y escoger un motivo del catálogo de la especialidad.
2. Confirmar que el mismo texto aparece antes de finalizar la reserva.
3. Abrir la cita desde el portal médico y comprobar el motivo guardado.
4. Reprogramar, elegir otro motivo de esa especialidad y comprobarlo en la nueva cita.
5. Cambiar de especialidad en el formulario: el motivo anterior no debe conservarse si pertenece a otra.

No requiere una migración nueva. Las citas anteriores sin motivo siguen mostrando su estado actual; no se inventa un motivo para ellas.
