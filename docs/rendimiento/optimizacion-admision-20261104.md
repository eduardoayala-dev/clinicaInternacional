# Optimización de admisión

Base anterior: `baseline/admision-antes-optimizacion-20261104`, commit `462dee2bbbaefc2b3adfec1a6af9317541965f25`.
Rama: `perf/consultas-admision`.

## Hallazgo

El diagnóstico secuencial autenticado en Railway registró 19 946 ms al abrir el inicio y HTTP 503 después de 18 149 ms en confirmaciones. Los logs mostraron `CommunicationsException`, `SQLSTATE 08S01` y `SocketTimeoutException` durante la lectura de MySQL, incluida la consulta de sesión. No acreditan un reinicio de Java ni un problema de firma JWT. La captura posterior de procesos no mostró consultas de aplicación ni esperas de bloqueo.

Se conservan las mediciones originales en `evidencias/admision-20261104`. Son observaciones de una navegación secuencial, no una prueba de carga ni percentiles. No deben compararse los tiempos de Railway con los locales como si fueran el mismo entorno. La primera navegación puede incluir compilación JSP y calentamiento.

## Cambios

- El conteo de cupos del inicio pasa de una consulta de agendas más tres por médico/día a cuatro consultas en total cuando existen agendas publicadas. Las ocupaciones se agrupan en memoria por médico y fecha, conservando intervalos, ofertas vigentes, retenciones, deduplicación y hora de Perú. No se almacena disponibilidad en caché.
- La búsqueda de sesión selecciona directamente por clave primaria o por el índice único de renovación. Se elimina el predicado alternativo con `OR`; se conserva la transacción, el bloqueo y la comprobación de revocación, usuario activo, rol, credencial y vencimientos. Este cambio evita la búsqueda alternativa; por sí solo no demuestra que esa consulta causara los timeouts observados.
- Se incluye un diagnóstico HTTP reproducible que solo inicia sesión y lee los seis menús, sin modificar citas ni guardar contenido de pacientes o cookies.

## Verificación

La regresión Maven terminó correctamente. La verificación focalizada ejecutó 14 pruebas de catálogo y 10 de sesiones, sin errores ni fallos. La prueba nueva compara el resultado agrupado con los horarios individuales durante siete días, con bloques superpuestos, cita que ocupa parcialmente dos turnos, retención y oferta pendiente. Verifica que se preparen exactamente cuatro consultas.

## Comparación tras desplegar

Ejecutar `node scripts/rendimiento/diagnostico-admision.cjs` con `DIAGNOSTICO_URL`, `DIAGNOSTICO_USUARIO`, `DIAGNOSTICO_PASSWORD` y `DIAGNOSTICO_SALIDA` definidos en el entorno. La carpeta de salida debe existir. No subir credenciales ni archivos de entorno.

Conservar el resultado del primer acceso por separado; repetir con el mismo conjunto de datos y recursos, registrar HTTP, tiempos, uso de CPU/RAM y errores MySQL. No aumentar `socketTimeout` ni desactivar controles para obtener un resultado favorable. La validación final de disponibilidad queda pendiente hasta desplegar y medir en Railway.
