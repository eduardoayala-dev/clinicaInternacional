# Orientación demostrativa por catálogo

Rama `feature/orientacion-especialidades-ia`. Solo casos ficticios para una demostración universitaria. No es un servicio clínico validado ni una evaluación de urgencia.

## Cambios

- Se retira el clasificador de palabras clave del navegador, sus identificadores fijos y la asignación de prioridad a la reserva.
- El servidor consulta especialidades activas. La sugerencia se valida por ID, nunca por coincidencias parciales del nombre.
- La respuesta de Gemini solo puede aportar `idEspecialidad` y un motivo enumerado. Java rechaza campos adicionales, IDs inexistentes, respuestas incompletas o bloqueadas y datos malformados. El texto explicativo visible se construye en Java; no se muestra texto médico generado libremente.
- El paciente debe pulsar «Continuar con esta especialidad» para aplicarla. Editar el relato, salir o cambiar de modo invalida la sugerencia. No se selecciona la primera especialidad si falta la sugerida.
- El flujo manual se conserva. Una sugerencia nunca reserva, altera urgencia ni estima consecuencias de no atenderse.
- Endpoint POST `/paciente/orientacion` protegido por autenticación, rol PACIENTE y los filtros JWT/CSRF existentes; contrato `code`, `message`, `data`.

## Estado del proveedor

Gemini queda **desactivado por defecto**. Tres ejemplos ficticios exactos permiten probar la interacción y se identifican como `EJEMPLO_LOCAL`, sin fingir una llamada de IA. Con el proveedor desactivado, otro texto libre informa que Gemini no está conectado y permite continuar manualmente. No se afirma que el problema de comprensión de texto libre esté resuelto por esos ejemplos.

El adaptador HTTP está preparado y probado con respuestas simuladas. No se hicieron llamadas reales a Google ni se consumió cuota. No se entregó una clave en esta tarea.

`config/gemini.env.example` es una plantilla sin secretos. Se dejó la configuración privada `.env.gemini`, ignorada por Git y desactivada por defecto, con `GEMINI_API_KEY`, el identificador del modelo disponible en la cuenta en `GEMINI_MODEL` (sin prefijo `models/` ni URL) y `GEMINI_ORIENTATION_ENABLED=false`. `compose.gemini.yml` y `scripts/Iniciar-Local.ps1 -Jwt -Correo -Gemini` permiten cargarla explícitamente. No activar el proveedor automáticamente ni publicar claves en el navegador, commits o capturas.

Antes de habilitar la conexión debe resolverse la compatibilidad de este caso de clasificación de síntomas con las condiciones del proveedor. Que sea académico o que el paciente marque «caso ficticio» no crea una excepción contractual. El código y esa casilla tampoco garantizan anonimización: no introducir datos personales ni síntomas reales. Las [condiciones de Gemini](https://ai.google.dev/gemini-api/terms) restringen asesoramiento médico/práctica clínica y el envío de información sensible a servicios gratuitos. La integración externa permanece pendiente de esa revisión y de la configuración privada.

## Transporte y privacidad

HTTPS al host fijo de Google, clave solo en cabecera, identificador del modelo validado, sin redirecciones, conexión de 4 s y petición de 12 s. No hay reintentos automáticos. Se limita a una consulta externa por sesión cada 15 s; no sustituye una cuota persistente/global ni los límites del proveedor. El relato tiene máximo 1200 caracteres y no se guarda ni registra. No se envían usuario, DNI, correo, historia clínica ni otra información de la cuenta; el texto libre seguiría siendo responsabilidad del caso ficticio introducido.

El proveedor no tiene herramientas ni acceso a SQL. Se envían únicamente el relato ficticio y el catálogo. El sistema no confía en instrucciones incluidas en el relato ni en la salida del modelo para efectuar cambios. La validación local es obligatoria aunque se solicite JSON estructurado.

Referencias: [generateContent](https://ai.google.dev/api/generate-content), [salidas estructuradas](https://ai.google.dev/gemini-api/docs/structured-output).

## Prueba manual local sin proveedor

1. Paciente → Programar cita → Orientación por especialidad.
2. Elegir un ejemplo ficticio y confirmar que no contiene información personal.
3. Consultar: debe indicar «Ejemplo local predefinido; Gemini no fue consultado» y una especialidad activa.
4. Continuar: debe aplicar únicamente esa especialidad, sin cambiar prioridad ni reservar.
5. Volver y escribir un texto distinto: debe informar que el proveedor no está conectado, sin inventar una respuesta.
6. Editar un ejemplo después de consultarlo: la sugerencia anterior debe desaparecer.

Las pruebas de transporte con doble HTTP cubren respuestas válidas, inválidas, cuota agotada, bloqueo y errores de red. La prueba de integración real con Gemini queda pendiente. Esta entrega no completa HU013/HU017 ni reemplaza el modelo de inasistencia.

## Evidencia de esta entrega

- Suite local combinada con otras entregas aún no publicadas: 746 casos descubiertos, 496 ejecutados sin fallos y 250 omitidos por configuración (`target/orientacion-regresion.log`).
- Se añadió después la comprobación de GET/405 con contrato JSON y se repitieron los 15 casos específicos de catálogo, proveedor y servlet (`target/orientacion-final.log`).
- 12 pruebas JavaScript existentes de reservas/catálogos aprobadas (`target/orientacion-js.log`).
- Revisión en navegador con cuenta de paciente de demostración: catálogo real, casilla de caso ficticio, selección solo al confirmar, prioridad intacta, invalidación al editar, CSRF y rechazo de una respuesta con ID ajeno. Escritorio y móvil sin desbordamiento; una respuesta tardía tampoco reaparece al editar el relato. No se reservaron citas ni se hicieron consultas a Gemini.
- Capturas y auxiliar de revisión en `target/orientacion-*.png` y `target/probar-orientacion-vistas.cjs`. Aceptación manual del flujo corregido confirmada por el usuario el 30/10/2026; no se publicó en Railway.

## Corrección tras la revisión manual: regreso y ejemplo de rodilla

El catálogo local usa «Traumatología y Ortopedia»; el ejemplo buscaba únicamente «Traumatología». Se añadieron ambos nombres como equivalencias explícitas del ejemplo, conservando el ID real del catálogo. Si no existe una coincidencia activa inequívoca, ahora se informa `EJEMPLO_SIN_ESPECIALIDAD` (409), en lugar de atribuir el fallo a Gemini. No se usa coincidencia parcial ni se elige otra especialidad por defecto.

La búsqueda conserva su origen al aplicar la sugerencia: Calendario → Volver → Filtros → Volver → Orientación. Un nuevo Volver desde orientación lleva a la elección de modos. El modo manual mantiene Filtros → Elección de modos. Se conservan el relato y la confirmación de caso ficticio para volver a consultar.

Se repitieron 17 pruebas backend y 12 JavaScript. El recorrido de navegador con los tres ejemplos está en `target/probar-orientacion-regreso.cjs`; no requiere crear reservas ni llamar al proveedor externo. El usuario confirmó satisfactoriamente esta corrección el 30/10/2026.

## Validación independiente para el PR

La rama contiene únicamente la orientación por catálogo, su integración del paciente y la configuración opcional. Se exportaron sus commits con `git archive` a una copia aislada basada en `develop` (`7790a72`), excluyendo los trabajos locales pendientes de correo, horarios, citas pasadas, predicción de inasistencia y rendimiento.

- Maven `package`: 652 casos descubiertos, 441 ejecutados, 211 omitidos por configuración, cero fallos/errores.
- JavaScript de reservas y catálogos: 12 pruebas aprobadas en esa misma copia.
- Regresión de orientación: los 17 casos específicos están incluidos en la suite.
- Recorrido manual corregido aprobado por el usuario; reproducción de los tres ejemplos y del regreso desde calendario completada.
- No requiere migración SQL ni configurar Gemini para utilizar los ejemplos. La prueba real de texto libre contra Gemini continúa pendiente; la conexión externa está apagada por defecto.

Evidencia local: `target/orientacion-pr-aislado.log` y `target/orientacion-pr-js-aislado.log`. Los checks remotos de GitHub/SonarCloud se revisan por separado; una suite local aprobada no implica que el Quality Gate remoto ya esté aprobado.
