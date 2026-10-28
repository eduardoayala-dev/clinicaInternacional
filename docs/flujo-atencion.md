# Flujo de atención ambulatoria

## Estados y responsabilidades

| Origen | Acción | Responsable | Resultado |
| --- | --- | --- | --- |
| Confirmada, para hoy | Registrar llegada | Admisión de la misma sede | En sala, con hora real y responsable |
| En sala, con llegada registrada | Iniciar atención | Médico asignado habilitado | En atención, con hora de inicio |
| En atención | Finalizar atención | Médico asignado habilitado | Atendida, con hora de cierre y registro persistido |
| En sala o en atención | Registrar no atención y motivo | Médico asignado habilitado | No atendida, con motivo y hora de cierre |
| Confirmada, sin llegada, después de la tolerancia | Verificar inasistencia y motivo | Admisión de la misma sede | No asistió, con responsable y fecha del registro |

Una llegada tardía mantiene al paciente en sala. El médico puede adelantar una cita del mismo día si el paciente ya está presente. Las citas de otros días conservan su información para lectura. Una consulta iniciada antes de medianoche puede cerrarse después de medianoche; la fecha recomendada de seguimiento debe ser posterior al día del cierre.

El médico y el paciente pueden tener como máximo una consulta activa cada uno. La comprobación se realiza con bloqueos de paciente, médico y cita en una transacción. Dos índices únicos sobre columnas generadas en MySQL refuerzan esta regla entre procesos distintos. La segunda solicitud incompatible devuelve `409`, sin crear otro inicio ni sobrescribir horarios.

«Ver ficha» consulta los datos sin iniciar la atención. «Iniciar atención» habilita el formulario clínico. «Finalizar atención» requiere prioridad válida e indicación no vacía de hasta 200 caracteres. El seguimiento es opcional y recomienda una fecha; la reserva de una nueva cita sigue siendo un proceso separado.

Una cita cerrada queda en solo lectura. Un cierre repetido devuelve `409`. Las correcciones posteriores de registros cerrados requieren una futura operación de enmienda con auditoría; los endpoints de este flujo no permiten reabrir ni sobrescribir el registro.

## Registro y métricas

`admision_cita` conserva llegada, personal responsable y tolerancia aplicada. `atencion_cita` conserva médico, paciente, inicio, fin y motivo de no atención. `inasistencia_cita` conserva quién verificó la ausencia, cuándo, el motivo y la tolerancia utilizada.

Los horarios se toman del servidor en `America/Lima`. Los campos de hora, usuario, médico o paciente enviados por el cliente no sustituyen la identidad de sesión ni el reloj del servidor. Los registros anteriores mantienen sus datos; un horario desconocido no se reconstruye a partir de la hora programada.

La espera es `inicio - llegada` y la duración es `fin - inicio`. Los promedios del inicio médico utilizan únicamente registros con los horarios necesarios, para las citas de la fecha consultada. Sin muestras se presenta «Sin datos». Una cita no atendida antes del inicio conserva inicio y duración vacíos. Una consulta interrumpida después de comenzar conserva su duración real.

El historial del paciente se organiza por estado persistido. Las citas activas siguen pendientes aunque haya pasado su hora; una inasistencia o una cita no atendida no se presenta como consulta realizada. Se retiró el diagnóstico ficticio asignado automáticamente.

## Contratos HTTP

Los cambios utilizan formulario POST con cookie de sesión, `csrfToken` e `idCita`. Las consultas GET no modifican datos. Con `Accept: application/json`, las respuestas mantienen `{code,message,data}`.

`POST /medico/atencion`:

| `accion` | Otros campos | Resultado |
| --- | --- | --- |
| `iniciar` | Ninguno | Inicia la cita propia presente en sala |
| `guardar` | `prioridad`, `indicaciones`, `fechaProxima` opcional | Finaliza una consulta activa propia |
| `no_atender` | `motivo`, entre 3 y 200 caracteres | Termina sin atención completa, conservando prioridad y seguimiento previos |
| `no_asistio` | Cualquiera | `403`: corresponde a admisión |

`GET /medico/atencion?idCita=...` devuelve el detalle propio. Sin identificador retoma la consulta activa del médico; si no existe, devuelve `data: null` o la vista vacía.

`POST /admin/admision` acepta `accion=llegada` (también el valor omitido por compatibilidad) o `accion=no_asistio`. La inasistencia requiere `motivo` de entre 3 y 200 caracteres. Solo se puede registrar para una cita confirmada de hoy, sin llegada y una vez superada la hora programada más la tolerancia. El límite exacto todavía está dentro de la tolerancia.

Estados HTTP: `400` para campos inválidos; `403` para sesión, perfil, rol o CSRF no permitido; `404` para cita inexistente o fuera del alcance del usuario; `405` para acciones enviadas mediante GET; `409` para transiciones o consultas simultáneas incompatibles; `503` para fallos transaccionales. Un fallo al guardar el historial revierte el cierre y conserva la consulta activa para reintentar.

## Base de datos y despliegue local

Con una base existente, respaldar y aplicar `V003__registro_llegada.sql` y después `V004__flujo_atencion.sql` antes de desplegar esta versión. La segunda migración amplía los estados y crea las tablas de consulta e inasistencia. Ambas pueden repetirse y conservan usuarios, citas e historiales. El usuario de la aplicación conserva permisos de datos; las migraciones requieren la cuenta administrativa de MySQL.

`sql/schema.sql` incluye el esquema para volúmenes nuevos. No se debe ejecutar ese archivo sobre la base existente, porque su inicialización elimina tablas. La reversión del código puede conservar las tablas adicionales y el enum ampliado; volver a un enum anterior requiere revisar primero si existen registros con estados nuevos.

## Prueba manual

En el entorno local se reserva un caso exclusivo llamado «Paciente Manual Prueba local», con datos sintéticos. Los identificadores, correos y fecha de preparación se guardan en `target/atencion/variables-locales.json`, fuera del repositorio.

1. Ingresar como admisión, buscar el DNI de ese caso y registrar su llegada.
2. Ingresar como el médico del flujo, abrir «Ver ficha» y pulsar «Iniciar atención».
3. Abrir «Atención de paciente actual»: debe recuperar la misma consulta.
4. Completar prioridad e indicación, opcionalmente seguimiento, y finalizar.
5. Volver a la ficha: debe mostrar inicio, cierre y datos guardados, en solo lectura.

Importar de nuevo `docs/postman/Modulo-Medico-Local.postman_collection.json` para la versión actual. La colección usa únicamente localhost, necesita citas sintéticas nuevas y consume los casos Completa, Segunda, SinAtencion e Inasistencia. Manual queda sin modificar. Mantener cookies activas y desactivar redirecciones automáticas.

## Validación del 28/10/2026

- Regresión Java: 287 pruebas ejecutadas sin fallos; las 149 condicionadas por variables de entorno quedaron omitidas en esa ejecución local.
- MySQL real: 61 casos de los contratos de atención y admisión verificados en bases temporales, con claves foráneas e índices únicos. La primera ejecución tuvo una interrupción TLS en la preparación de un caso; su repetición pasó, junto con una comprobación adicional del historial. Las bases temporales se retiraron al finalizar.
- JavaScript: 31 pruebas sin fallos, incluidas protección de salida y separación de formularios.
- Postman: 54 comprobaciones en 41 solicitudes del flujo completo. Después de actualizar la versión local, otras 20 comprobaciones en 12 solicitudes de lectura confirmaron la persistencia y que el caso Manual seguía sin consumir.
- Edge: llegada desde admisión, ficha previa, inicio adelantado, recuperación de consulta activa, cierre, bloqueo posterior, fechas legibles y detalle del historial del paciente con el resultado real.

Los respaldos previos a V003 y V004, resultados y capturas permanecen en `target/`, fuera del repositorio. Los ajustes generales de diseño móvil, las exportaciones administrativas y los demás módulos requieren sus revisiones correspondientes.
