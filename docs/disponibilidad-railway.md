# Disponibilidad y despliegues de Railway

## Comprobación

GET /health/ready responde con el contrato code/message/data. Devuelve 200 y SISTEMA_DISPONIBLE si SELECT 1 respondió; 503 y Retry-After: 3 si sigue iniciando o MySQL no responde. No informa host, usuario, versiones ni excepciones al cliente y no crea sesión ni token CSRF. No sustituye las pruebas de permisos, integridad de esquema o flujos clínicos.

Una comprobación asíncrona inicia al arrancar Java. Las llamadas concurrentes comparten un único trabajo y se reutiliza el resultado durante cinco segundos, tanto para éxito como fallo. La petición HTTP espera como máximo 1.5 segundos al trabajo compartido; la adquisición JDBC conserva sus límites y reintentos actuales y SELECT 1 tiene timeout de dos segundos. No hay un temporizador de keep-alive dentro de este servicio. El mantenimiento de ofertas existente sigue consultando MySQL cada minuto mientras Java está activo: también influye en consumo y suspensión.

El login de pacientes y el institucional comprueban disponibilidad antes de enviar credenciales, con espera limitada y sin repetir automáticamente el POST. Si no hay disponibilidad, restauran el botón y muestran el mensaje; los portales conservan sus permisos y CSRF. Sin JavaScript se mantiene el envío tradicional del formulario. Esto mejora el arranque de MySQL después de Java; no elimina la espera mientras Railway todavía no puede servir ningún HTML.

## Heartbeat durante una evaluación

Desde una computadora externa a Railway, después de desplegar esta versión:

```powershell
./scripts/Monitor-Disponibilidad.ps1 -Minutos 60
```

Visita únicamente /health/ready cada tres minutos, guarda estado y duración en target/disponibilidad y termina tras una hora. Ctrl+C lo detiene. No requiere una cuenta adicional ni envía credenciales. La computadora debe permanecer encendida y conectada; no es un servicio cloud de monitorización permanente. Un 502/503 inicial puede corresponder al despertar; el siguiente sondeo intentará de nuevo. El intervalo reduce el riesgo de suspensión, pero no garantiza disponibilidad ni evita los límites o agotamiento del crédito gratuito. No activar todo el mes sin medir el coste.

Para usar un proveedor externo de monitorización, configurar un monitor HTTP GET con esta URL, éxito HTTP 200 y frecuencia inferior a cinco minutos si el plan lo admite. Habilitarlo solo durante la ventana acordada y pausarlo al terminar. Su cuenta y configuración requieren acceso del usuario; este cambio no crea ni activa un monitor cloud. No usar login como destino: no comprueba necesariamente la BD. Si el proveedor solo admite intervalos de cinco minutos o mayores, no garantizar que evite el reposo.

El Healthcheck Path de Railway puede apuntar a /health/ready para verificar un despliegue nuevo; no equivale al heartbeat continuo ni asegura arranques sin demora.

## Publicación controlada

El usuario confirmó haber desactivado Autodeploy desde el panel el 29 de octubre de 2026, conservando develop como rama de origen. La versión activa se conserva y los merges posteriores no deben publicar automáticamente. Publicar una versión verificada manualmente mediante Deploy Latest Commit de la rama conectada. En todos los casos registrar el SHA desplegado.

La sesión CLI de Railway estaba caducada; el ajuste de Autodeploy fue realizado por el usuario. Para recuperar el acceso ejecutar railway login de forma interactiva; no compartir tokens en el chat. No cambiar credenciales de MySQL ni su volumen al ajustar la rama del servicio Java.

Cuando se acuerde una versión estable, preparar release/*, revisar migraciones y variables necesarias, integrar por PR en main y etiquetar la versión. Después conectar Railway a main. El desarrollo incompleto puede continuar en develop. No conectar ahora una main antigua ni crear un segundo entorno permanente con el mismo crédito gratuito.

## Evidencia

Verificación local del 29 de octubre de 2026 en el laboratorio separado clinica-rendimiento, puerto 8083:

- Suite Java: 332 aprobadas y 174 omitidas por condiciones de sus entornos; ninguna fallida. Incluye cinco pruebas nuevas de disponibilidad y contrato HTTP.
- Suite JavaScript: 37 aprobadas, incluidas tres nuevas de espera, recuperación y doble envío.
- Navegador con cuenta médica sintética: dos respuestas 503 simuladas antes de recuperar el endpoint real, un único POST de login y acceso a la agenda. Con indisponibilidad prolongada en el login de paciente, cero POST de credenciales y botón reactivado. Comprobación visual y de ancho móvil sin desborde horizontal.
- Caída real de la BD del laboratorio: tres solicitudes simultáneas devolvieron 503 en 1.506–1.509 segundos, sin Set-Cookie. Tras volver a arrancar MySQL se recuperó 200, observado a los 15.11 segundos mediante sondeos. No es una medida de Railway ni una garantía de recuperación.
- Monitor probado un minuto contra localhost: primera respuesta 503 durante el arranque y tres respuestas 200 posteriores; genera CSV. No se dejó un proceso de monitorización persistente.
- Evidencia privada local bajo target/disponibilidad*. No se cambiaron datos, configuración ni monitores de Railway durante estas pruebas.

El usuario también confirmó el ingreso desde el login institucional del laboratorio antes de autorizar el cierre de esta rama. La conexión de esta versión y el despertar real en Railway se comprobarán después del despliegue manual; no se consideran verificados por el ensayo local.

Separar pruebas de arranque frío y de servicio activo. Guardar commit, recursos, datos, tiempos de primera disponibilidad y resultados por ruta. El heartbeat y el mantenimiento interno alteran el estado térmico del servicio: registrar si estaban activos. No comparar una medición con servicios dormidos contra otra precalentada atribuyendo toda la mejora a cambios de código.
