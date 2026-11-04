# Motivo de cancelación y cierre del ciclo de citas

Rama: feature/motivo-cancelacion. Revisión: 4 de noviembre de 2026. Prueba manual aprobada por el usuario el 4 de noviembre de 2026.

Paciente: Mis citas y los enlaces autenticados de correo requieren seleccionar un motivo. Admisión: Anular cita requiere un motivo de 5 a 160 caracteres. El servidor valida las mismas reglas y guarda el motivo, el actor y la cita en auditoria_seguridad dentro de la transacción de cancelación. No se añade una tabla. La ruta genérica de cambio de estado no permite cancelar sin motivo. La cancelación por falta de confirmación conserva una razón operativa determinada por la política, tras verificar plazo e intentos de contacto.

El formulario de Anular cita enviaba cambiar_estado, una acción que el servidor rechaza. Se conectó a cancelar_cita y se retiró un formulario oculto sin uso. Un motivo inválido conserva la reserva y muestra el mensaje; no genera ofertas ni correos. El correo de cancelación se mantiene en la cola transaccional existente.

## Migración

V021__auditoria_ip_opcional.sql permite NULL en auditoria_seguridad.ip_origen. El esquema exigía una IP mientras varias operaciones internas no la suministran, lo que bloqueaba escrituras bajo el esquema real. Se conservan las IP registradas; no se inventan direcciones ni se modifican usuarios o citas. La migración es repetible. Railway requiere aplicarla cuando se prepare el despliegue; no se ejecuta allí desde esta entrega.

## Comprobación manual

1. Paciente → Mis citas → Cancelar: sin motivo no debe enviar; con un motivo debe cancelar una sola vez. Actualizar no debe cancelar otra cita.
2. Abrir un enlace de cancelación recibido por correo: GET solo muestra la revisión y el motivo obligatorio; POST conserva autenticación y CSRF.
3. Admisión → Inicio → Anular cita futura: indicar motivo y comprobar cancelación. Una cita ajena a la sede no puede modificarse.
4. Verificar el motivo y actor en auditoría. No enviar datos clínicos sensibles en el texto.
5. Recorrer reserva, confirmación, llegada, atención y cierre. Reprogramar una cita distinta y cancelar para ofrecer el horario a la lista de espera. La inasistencia se registra expresamente tras verificar la llegada; el cambio de día no decide la ausencia.

## Evidencia automatizada

Regresión Java de esta entrega: 1007 pruebas ejecutadas sin fallos, 604 omitidas por requerir MySQL. Se adaptaron los casos de propietario, estado, concurrencia y cola de correo al motivo obligatorio. Catorce vistas compilaron con Jasper y nivel Java 11. Las pruebas MySQL de agenda y enlaces terminaron sin fallos antes de interrumpir la ejecución amplia para concentrar la validación en el esquema real. El resultado definitivo de los escenarios completos se registra al terminar su ejecución aislada; no se confunde con Selenium ni con una prueba de carga.

Resultado definitivo: cuatro escenarios completos ejecutados en MySQL sobre schema.sql, sin fallos: migración que conserva IP; retención/reserva, confirmación, llegada, atención y cierre; reprogramación, cancelación con motivo y aceptación de oferta; inasistencia explícita. Se utilizó un esquema temporal y un usuario limitado, eliminados al terminar. La regresión final ejecutó 1007 pruebas sin fallos y omitió 604 casos condicionados a MySQL. La migración se aplicó solamente a la base local; el WAR está instalado en localhost:8080. El usuario confirmó el funcionamiento local; entrega lista para revisión por PR.
