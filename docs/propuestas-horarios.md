# Propuestas de horarios del médico

Entrega local de HU005/RF05. El usuario confirmó el funcionamiento del flujo; quedan detalles estéticos pendientes. La publicación se revisa mediante PR.

## Flujo

1. Médico → Mis horarios → días de la semana, inicio/fin de vigencia, horas y minutos por cita → Enviar a admisión.
2. Admisión → Médicos y especialidades → Propuestas de horarios. Solo aparecen propuestas de la sede asignada al personal.
3. Aprobar publica todos los turnos futuros seleccionados en una transacción. Un cruce con disponibilidad publicada revierte toda la aprobación. Dos aprobaciones simultáneas no deben duplicar turnos.
4. Rechazar exige un motivo, visible para el médico. No publica cupos. Una propuesta revisada no vuelve a aprobarse; los cambios se presentan como una nueva propuesta.
5. El paciente puede reservar únicamente en los bloques publicados y dentro de su vigencia. No hay renovación automática.

La sede y el médico se obtienen del perfil autenticado, no de identificadores enviados por el formulario. Se usan los filtros de autenticación/JWT/CSRF del proyecto. Las mutaciones son POST; las consultas GET no cambian estados. JSON usa `code`, `message`, `data`; consultas paginadas devuelven `items`, `pagina`, `tamano`, `tieneSiguiente`. La vista muestra seis propuestas por página y un máximo de 100 recientes por médico/sede.

Las horas son de America/Lima; las fechas de creación/revisión se guardan en UTC. Los turnos ya iniciados al aprobar se omiten, sin publicar fechas pasadas. Si no queda ninguno futuro, se rechaza la operación manteniendo la propuesta pendiente. Cambios de sede o desactivación del médico impiden publicar con su perfil anterior.

## Límites iniciales y trabajo pendiente

- Vigencia máxima de 90 días inclusivos, duración entre 5 y 240 minutos; no se permiten turnos que crucen medianoche ni fracciones de una cita al final del bloque.
- Se muestra «Por vencer» durante los siete días anteriores al fin de vigencia. Es una advertencia visual, no un aviso por correo.
- Estos límites son iniciales. Su administración/auditoría central corresponde a HU018 y todavía no está implementada.
- No se valida ocupación de consultorios: la disponibilidad existente no asigna un consultorio. Esta relación sigue pendiente en RF03/RF05; admisión debe revisarlo antes de aprobar.
- Este flujo no reemplaza la publicación directa existente por admisión ni retira turnos ya publicados. Cambiar horarios con citas requiere reprogramación, no borrar bloques.
- Los recordatorios, renovación asistida e invitación inicial del personal siguen en la matriz de requisitos.

## Esquema y despliegue

Aplicar `sql/migrations/V009__propuestas_horarios.sql` con respaldo previo antes de publicar el WAR. Es idempotente en MySQL 8. Añade `propuesta_horario` y `disponibilidad.id_propuesta`, nullable para conservar los bloques previos. No modifica pacientes, usuarios ni citas. La comprobación de disponibilidad del nuevo WAR exige esta migración.

La propuesta es una entidad de negocio nueva, con médico, sede, revisor, estado y motivo; debe incorporarse al modelo de datos del informe. El vínculo con disponibilidad permite identificar todos los bloques generados. El correo de cita de V008 es una cola técnica distinta.

`schema.sql` solo sirve para bases nuevas: contiene recreación y datos de demostración. No ejecutarlo sobre una base existente. Railway no se actualiza con esta entrega local.

## Recorrido de aceptación del usuario

1. Entrar como médico, proponer días de la semana que existan entre dos fechas futuras y un turno con citas completas.
2. Confirmar que el estado sea pendiente y que el paciente aún no vea esos cupos.
3. Entrar como admisión de la misma sede, aprobar y comprobar los turnos publicados.
4. Entrar como paciente, buscar ese médico/fecha y reservar. Si el correo de cita está activado, comprobar la confirmación en el correo de la cuenta.
5. Enviar una segunda propuesta cruzada: aprobar debe informar el conflicto sin publicar parcialmente.
6. Rechazar otra propuesta con motivo y comprobar que el médico pueda leerlo.
7. Revisar en el portal de paciente que una cita confirmada de un día anterior figure como pasada, pendiente de registro de asistencia, sin transformarse automáticamente en inasistencia.

No se crean propuestas reales, reservas ni envíos de prueba automáticamente en la base de la clínica. Las pruebas automatizadas usan esquemas aislados y correos ficticios.

## Evidencia automatizada de esta entrega

Aceptación manual del usuario: confirmó que el recorrido funciona correctamente, incluidos el envío del médico y la revisión por admisión. Los ajustes visuales se mantienen como trabajo posterior.

- Suite general: 720 casos descubiertos, 470 ejecutados, 250 omitidos por configuración, cero fallos/errores (`target/propuestas-regresion.log`).
- Contrato de propuestas en MySQL con TLS: 9 casos más 3 comprobaciones de entorno, cero fallos; migración ejecutada dos veces. Contenedor temporal retirado (`target/pruebas-mysql/ejecucion.log`).
- Los ajustes finales de validación de vigencia se comprobaron repitiendo los 9 casos JDBC y empaquetando el WAR (`target/propuestas-final.log`).
- Interacción del JavaScript en Edge con solicitudes simuladas: días requeridos, vigencia inclusiva, motivo, cancelación de confirmación y un único envío con acción correcta.
- Revisión visual y permisos de las rutas locales en Edge: vistas y JSON de médico/admisión, rechazo de acceso cruzado por rol, seis elementos por página y tamaños de escritorio/móvil. No reemplaza la aceptación manual con una propuesta real.
- Recorrido integral en Edge con JWT y MySQL aislado (30/10/2026): envío del formulario médico, propuesta pendiente sin cupos visibles, aprobación de admisión que publica dos turnos, reserva del paciente desde el calendario y visualización del motivo de rechazo por el médico. Acceso de médico a admisión devuelve 403; cero errores JavaScript. SQL confirmó una reserva y cero elementos en la cola de correo, con envíos desactivados. Se retiraron el contenedor, la base y el usuario temporales al finalizar. No reemplaza la aceptación manual del usuario.
- Revisión del contenido separado para el PR: WAR empaquetado y 453 pruebas Java ejecutadas, 220 omitidas por configuración, cero fallos/errores (`target/pr-horarios-validacion.log`); 48 pruebas JavaScript aprobadas (`target/pr-horarios-js.log`). Esta revisión excluye los cambios pendientes de correo de citas, fechas pasadas, modelo de inasistencia y rendimiento.
- Durante la primera navegación automatizada desde el login animado, Edge emitió avisos nativos de ViewTransition al pasar a páginas sin esa transición. Las rutas respondieron correctamente. La revisión aislada de las nuevas vistas utiliza movimiento reducido; revisar ese aviso por separado en el flujo de acceso.

La base local se respaldó antes de V009; se conservaron 112 usuarios, 58 pacientes y 45 citas. No se publicaron propuestas ni se reservaron citas automáticamente.
