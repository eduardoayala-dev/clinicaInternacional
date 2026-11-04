# Comparación reproducible antes y después

Primero se cierra y prueba el funcionamiento HU001–HU015. No se presentan resultados de carga o capacidad hasta ejecutar las mediciones. Las pruebas funcionales no equivalen a pruebas de rendimiento.

## Versiones que se conservarán

Tras aprobar los commits y PR funcionales, crear el tag anotado `baseline/funcional-hu15`. No moverlo después. Registrar el SHA, fecha, versión de Java, imagen de MySQL, configuración de recursos y versión de las herramientas.

| Evaluación | Rama para las mejoras | Tag antes | Tag después |
|---|---|---|---|
| Recorridos Selenium | `test/selenium-hu15` | `baseline/funcional-hu15` | `validacion/selenium-hu15` |
| Carga y estrés | `perf/carga-hu15` | `baseline/carga-hu15` | `mejora/carga-hu15` |
| Lighthouse y PageSpeed | `perf/web-hu15` | `baseline/web-hu15` | `mejora/web-hu15` |

Los nombres son un protocolo pendiente de aprobación, no tags ya creados. Cada baseline apunta al código efectivamente medido; una rama sola cambia con el tiempo y no basta como referencia. No fusionar optimizaciones de una evaluación antes de capturar su medición inicial.

## Condiciones de comparación

- Mismo conjunto de datos de prueba, cantidad de citas, cuentas y horarios. Restaurar el conjunto antes de cada corrida que escriba datos.
- Mismos recursos de Java/MySQL, navegador, dispositivo emulado, red, rutas y duración. Anotar cualquier diferencia inevitable.
- Separar arranque en frío de solicitudes con servicios activos. Registrar tiempo de despertar de Railway como indicador propio.
- No ejecutar estrés contra Railway compartido. Usar el laboratorio local aislado y limitar los recursos; comprobar el despliegue con recorridos acotados.
- Desactivar envíos reales y consultas a proveedores externos en las pruebas masivas. Utilizar cuentas y datos de prueba.
- No guardar contraseñas, cookies, JWT, tokens de API ni datos personales en los reportes versionados.

## Selenium

Recorrer autenticación por rol, consulta de disponibilidad, reserva, confirmación de asistencia, reprogramación, cancelación, oferta y atención. Registrar resultado y duración del recorrido. Un tiempo de Selenium incluye navegador y servidor; no atribuirlo a una consulta SQL sin evidencia adicional.

## Postman: carga y estrés

Guardar colección, configuración del escenario y reporte exportado. Usar peticiones autenticadas y verificar estado y contenido: un 302 hacia login o una página de error no cuenta como operación exitosa. Probar carga gradual y pico con límites de parada, monitorizando memoria, CPU, conexiones y errores.

Comparar solicitudes/segundo, tasa de errores, p50/p95/p99 y tiempo máximo, con usuarios virtuales y duración indicados. Relacionar una demora con logs y métricas del servidor antes de cambiar código. Capacidad es la carga sostenible bajo un umbral de servicio declarado; no el mayor número enviado una sola vez.

## Lighthouse y PageSpeed

PageSpeed se usa para rutas públicas. Para pantallas privadas, Lighthouse en navegador autenticado o su flujo automatizado, manteniendo la protección de acceso. No añadir credenciales en URLs ni accesos públicos a portales privados.

Guardar los reportes originales HTML/JSON. Repetir al menos tres veces por ruta y dispositivo, comparar medianas de rendimiento, LCP, CLS, TBT y accesibilidad, junto con las recomendaciones atendidas. Lighthouse de laboratorio y datos de campo de PageSpeed se reportan por separado; si no hay datos de campo, indicarlo.

## Registro de cada resultado

En `docs/rendimiento/evidencias/<tipo>/<antes-o-despues>/` guardar resumen sin secretos y referencia al reporte original. Cada fila debe incluir SHA/tag, ruta/escenario, fecha, herramienta/versión, recursos, datos, estado frío/caliente, repeticiones y resultado. Dejar campos sin medir vacíos, sin estimarlos.
