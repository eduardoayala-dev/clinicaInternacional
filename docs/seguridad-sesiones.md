# Sesiones, tokens y autorización de citas

Primera corrección del diagnóstico, en `fix/seguridad-sesiones-citas`, creada desde `develop`.

## Controles implementados

| Control | Responsabilidad |
|---|---|
| JSESSIONID | El contenedor identifica la sesión; el servidor obtiene de ella al usuario autenticado. Solo se admite seguimiento mediante cookie. |
| Token CSRF | Valor aleatorio de 256 bits, distinto por sesión. Se compara en el servidor antes de ejecutar POST, PUT, PATCH o DELETE. |
| Rol y propietario | Se comprueba que el paciente sea dueño de la cita o que el médico esté asignado a ella. Tener un token válido no concede ese permiso. |

El identificador de sesión y el token CSRF se renuevan al iniciar sesión o registrarse. El cierre de sesión invalida ambos. El token CSRF permanece estable dentro de una sesión para permitir varias pestañas; no es una contraseña ni un token de un solo uso.

Las cookies se configuran HttpOnly y SameSite=Lax. En producción, `APP_ENV=production` (o el indicador Railway reconocido) activa Secure; el acceso debe ser HTTPS y debe comprobarse la cookie real detrás del proxy. Esta revisión verificó HttpOnly/SameSite en Jetty local por HTTP, no HTTPS cloud.

JWT no es necesario para esta aplicación JSP/Servlets con sesiones. Un token JWT tampoco sustituiría permisos por recurso ni la protección CSRF cuando la credencial se envía automáticamente en cookies.

## Contrato de solicitudes que modifican datos

- Formularios: campo oculto `csrfToken`, renderizado desde la sesión.
- AJAX: cabecera `X-CSRF-Token` o el mismo campo de formulario. Si existe la cabecera, se valida esa cabecera.
- Token ausente, incorrecto, de otra sesión o sesión vencida: HTTP 403, sin ejecutar el controlador.
- El token no se acepta desde una sesión distinta ni se crea una sesión para legitimar un POST rechazado.
- Error JSON del filtro: `{"code":"CSRF_INVALIDO","message":"La sesión o el token de seguridad no es válido. Recarga la página."}`.
- GET `/logout`: HTTP 405, Allow: POST. El cierre de sesión se realiza mediante formulario POST con token.
- Las acciones de marcar/confirmar notificaciones pasan a POST; GET con el antiguo parámetro `action` se rechaza con 405.
- Errores de ID o acción en los controladores de citas modificados: 400. Cita ajena, inexistente o no modificable: 404 sin revelar propietario.

La cancelación de un paciente se condiciona al propietario y al estado actual; solamente después de actualizar se ejecutan los efectos de reasignación/notificación. El flujo completo de reasignación sigue pendiente de la corrección de ofertas y transacciones identificada en el diagnóstico.

La atención médica exige el médico asignado y una cita CONFIRMADA o EN_SALA. Cita e historial se guardan en una transacción: si el historial falla, se revierte el cambio de estado. Se eliminaron las sustituciones por paciente, médico o usuario ID 1 y la creación automática de pacientes al resolver un identificador.

## Validación realizada

`mvn -o test`: 35 pruebas, 0 fallos, 0 errores. Incluye 28 regresiones nuevas de filtro, renovación de sesión, controladores y autorización SQL con H2 aislado. H2 permite verificar el SQL usado aquí, pero no sustituye las pruebas de integración pendientes con MySQL y concurrencia real.

Comprobación de formularios: los 52 formularios POST encontrados, incluido el generado en JavaScript, contienen un único campo CSRF.

Comprobación HTTP con Jetty local en puerto 18080:

1. GET de login devuelve 200 y token aleatorio de 43 caracteres Base64URL.
2. La cookie JSESSIONID contiene HttpOnly y SameSite=Lax.
3. POST sin token o con token incorrecto devuelve 403.
4. Token de un navegador sin la sesión correspondiente devuelve 403.
5. POST de registro con token válido llega a la validación de términos; no se escribieron datos en BD.
6. POST de logout con token válido invalida la sesión y la página de login genera otro token.

El servidor de comprobación se detuvo al terminar. No se desplegaron cambios ni se modificó la base de datos cloud.

## Pendientes del diagnóstico

Conexión TLS y permisos de MySQL; cierre de credenciales demo/compartidas; bloqueo persistente; XSS; coherencia global de roles; reservas simultáneas y agenda; ofertas/priorización; contratos HTTP completos; CI y despliegue cloud. Esta rama es el primer incremento de seguridad y no resuelve todo el diagnóstico.

Referencias: [sesiones OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html) y [tokens CSRF OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html).

## Entrega mediante Gitflow

Los commits agrupan cambios necesarios para un solo propósito y contienen una descripción del problema, la corrección y la validación. Las pruebas acompañan al cambio que verifican.

Esta entrega se divide en autorización/identidad de citas, protección CSRF/sesiones y documentación. El primer commit se verificó también de forma aislada: 23 pruebas sin fallos; el incremento completo tiene 35 pruebas sin fallos.

El pull request compara `fix/seguridad-sesiones-citas` contra `develop`. Después de la revisión y aprobación del equipo, integrar mediante merge commit para conservar los commits individuales. Conservar tanto la rama remota como la local; no usar la opción de borrar rama después del merge. Comprobar que el repositorio no tenga activado el borrado automático de ramas antes de integrar.

La rama se considera terminada cuando su PR está integrado, aunque se conserve como evidencia. Para el siguiente problema crear otra rama desde `develop` actualizado, sin reutilizar esta rama terminada. La próxima corrección propuesta es conexión TLS, credenciales y permisos mínimos de MySQL en una rama propia.
