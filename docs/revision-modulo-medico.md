# Revisión del módulo médico

El flujo vigente de inicio, cierre e inasistencia se describe en [flujo-atencion.md](flujo-atencion.md). Requiere las migraciones V003 y V004 y reemplaza la política anterior de cierre directo.

## Fallos comprobados y correcciones

| Problema | Comportamiento corregido |
| --- | --- |
| Un día sin citas mostraba las últimas citas de otras fechas. | Consulta por fecha exacta y zona horaria de Lima. Un día vacío permanece vacío. El selector permite revisar citas anteriores y futuras. |
| Inicio y agenda deducían llegada, retraso y origen según la posición de la fila. | Las métricas utilizan el estado persistido. Solo `EN_SALA` representa una llegada registrada; el origen utiliza la relación con una oferta aceptada. |
| La atención completaba alergias, contactos, fechas y consultorios con valores ficticios. | Presenta los datos existentes. Una alergia no registrada se muestra como «Sin registrar», sin afirmar que el paciente no tenga alergias. |
| Un cierre clínico podía realizarse antes de la cita. | El cierre requiere una consulta propia iniciada. El inicio exige llegada registrada para el día clínico y disponibilidad del médico y paciente. Permite adelantar una cita presente en sala. |
| Las indicaciones no se recuperaban al abrir el registro guardado. | Se muestra la observación persistida del historial y la fecha recomendada real. Repetir el cierre no crea otro historial. |
| Registrar inasistencia modificaba la prioridad a baja. | La inasistencia corresponde a admisión, tras verificar la ausencia y superar la tolerancia. El médico registra No atendida con un motivo si el paciente llegó. |
| Fechas imposibles podían normalizarse y las indicaciones no tenían límite en servidor. | Fecha ISO estricta, seguimiento futuro opcional, prioridad válida y máximo de 200 caracteres. Cita e historial se actualizan en una transacción. |
| Se insertaban datos de usuarios y pacientes directamente en HTML. | Salida codificada en las cuatro vistas y la impresión. La nota se almacena como texto sin destruir tildes o símbolos. |
| El perfil usaba clases del portal sin cargar sus estilos. | Carga la hoja compartida, conserva el menú médico y elimina credenciales, consultorio y horario inventados. |
| Las exportaciones médicas apuntaban a una ruta inexistente. | CSV y vista para imprimir basados exclusivamente en la agenda del médico autenticado. El CSV neutraliza valores que podrían interpretarse como fórmulas. |
| Se repetían comprobaciones y se ocultaban fallos de consulta como listas vacías. | Control común de sesión, perfil médico activo, errores y contratos HTTP. Consulta por rango de fecha, sin segunda consulta de reemplazo ni carga de datos clínicos innecesarios en la lista. |

El perfil, la sede, la especialidad, el rol y la habilitación se verifican en la base de datos, incluso si existe una sesión previa. Los identificadores enviados por el cliente no cambian la identidad del médico. Una cita ajena o inexistente devuelve el mismo 404.

## Contratos

Con `Accept: application/json`, las consultas y el cierre responden `{code,message,data}`. Los POST requieren sesión y CSRF válido. Códigos: 400 para datos inválidos; 403 para acceso denegado; 404 para cita no asignada o inexistente; 405 para método o acción GET no permitida; 409 para un cierre incompatible con el estado o la fecha; 503 para fallos de datos. No se anuncian guardados que no se completaron.

| Ruta | Operación |
| --- | --- |
| GET `/medico/perfil` | Perfil vigente y propio; no permite editar credenciales. |
| GET `/medico/inicio` | Métricas y citas del día de Lima. |
| GET `/medico/agenda?fecha=AAAA-MM-DD` | Agenda e historial por fecha del médico autenticado. |
| GET `/medico/atencion?idCita=ID` | Detalle propio, seguimiento y posibilidad real de edición. |
| POST `/medico/atencion` | `accion=guardar` o `accion=no_asistio`, `idCita` y CSRF. |
| GET `/medico/export?fecha=AAAA-MM-DD&format=csv` | Archivo CSV propio. |
| GET `/medico/export?fecha=AAAA-MM-DD&format=print` | Vista propia para imprimir o guardar como PDF desde el navegador. |

La fecha recomendada orienta el seguimiento; no crea automáticamente otra reserva. El registro de continuidad no constituye una historia clínica completa. Los cierres fuera del día de atención requerirían un flujo de corrección autorizado, que no se añade en esta entrega.

## Validación y prueba manual

Se ejecutaron la regresión Java, 30 casos sobre MySQL real en una base temporal con claves foráneas, 30 pruebas JavaScript y 50 comprobaciones de Postman en 32 solicitudes. Se verificaron el perfil en escritorio y móvil, la agenda, el historial de solo lectura, la restauración del formulario y que el texto de prueba XSS no se ejecute en un navegador.

La colección `docs/postman/Modulo-Medico-Local.postman_collection.json` requiere cuentas y citas sintéticas. Utiliza ahora los casos Completa, Segunda, SinAtencion e Inasistencia, con inicio formal y roles separados: no utilizar citas reales. Configurar sus variables, mantener cookies activas y desactivar redirecciones automáticas.

En este entorno se prepararon cuentas `audit.*@example.test` y seis citas sintéticas. Sus identificadores y credenciales locales están en `target/medico/variables-locales.json`; no forman parte del repositorio. Las cuentas existentes se conservaron. Las capturas y resultados están en `target/medico/`.

Para la revisión manual del flujo actual, seguir los pasos de [flujo-atencion.md](flujo-atencion.md), utilizando los casos preparados en `target/atencion/variables-locales.json`. Cada paciente sintético del flujo representa un escenario distinto; el caso Manual queda reservado al usuario.

La revisión se limita al módulo médico y sus rutas de exportación. Las exportaciones administrativas, la seguridad de los demás módulos y el despliegue cloud continúan pendientes de su revisión específica.
