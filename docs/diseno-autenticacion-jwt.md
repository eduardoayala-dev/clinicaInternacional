# Gestor de autenticación y migración a JWT

Estado: diseño implementado en modo de activación explícita; ver [guía y validación actual](autenticacion-jwt.md).
Fecha: 2026-10-29.

## Problema observado

El acceso se determina mediante `HttpSession.usuarioLogueado`. `LoginServlet`
invalida la sesión anónima al autenticar y crea otra con un CSRF nuevo.
`RegistroPacienteServlet` repite la creación de la identidad autenticada.
`CsrfFilter` rechaza un formulario anterior antes de llegar al controlador.
Una pestaña abierta antes de iniciar o cerrar sesión conserva su formulario,
pero el navegador comparte la cookie actual con las demás pestañas del mismo
sitio. Por eso puede enviar el CSRF anterior con una sesión diferente.

El caso de dos pestañas es coherente con este flujo, pero falta registrar una
reproducción definitiva de cada fallo intermitente. No debe atribuirse todo
error CSRF a la misma causa sin comprobarlo.

Los controladores y JSP también consultan atributos `rol`, `usuarioId` y
`nombreUsuario`. Cambiar únicamente el formato del token no migra estas
dependencias ni resuelve la coordinación entre pestañas.

## Comportamiento requerido

- Paciente ingresa con documento; personal con correo. Se conservan los cuatro roles.
- El botón muestra la carga y evita envíos duplicados.
- El acceso se renueva en segundo plano mientras la sesión siga autorizada.
- La renovación conserva la identidad y el CSRF de la misma sesión lógica.
- Cerrar sesión revoca el acceso y comunica el cambio a las otras pestañas.
- Una pestaña antigua no muestra JSON técnico ni necesita una pantalla adicional
  de confirmación para el cierre normal de sesión.
- Si otra pestaña cambia de cuenta, los formularios de la cuenta anterior no
  pueden enviarse como si pertenecieran a la nueva. Se bloquea la operación y se
  solicita volver al portal correspondiente, con un mensaje dentro de la interfaz.
- Ninguna recuperación vuelve a enviar automáticamente reservas, cancelaciones,
  registros o guardados clínicos.

Una identidad por navegador y sitio es el comportamiento inicial. Abrir el login
de personal en otra pestaña no crea una sesión independiente de la de paciente.
Para probar dos identidades simultáneas se usan perfiles separados del navegador.

## Decisión de arquitectura

Un `GestorAutenticacion` será la única autoridad para iniciar, verificar, renovar
y revocar accesos. Login y registro llaman al mismo gestor. El filtro autentica
antes de autorizar por rol y antes de aceptar operaciones de negocio.

El JWT de acceso dura inicialmente cinco minutos. Contiene `iss`, `aud`, `sub`
(identificador interno), `sid` (sesión lógica), `jti` único por emisión, `iat`,
`nbf` y `exp`. No contiene contraseña, DNI, nombre ni información clínica.
La firma garantiza integridad; el contenido del JWT no es un secreto cifrado.

Se utiliza una biblioteca mantenida y un algoritmo configurado explícitamente.
Se rechazan firma incorrecta, algoritmo distinto, campos ausentes, emisor o
audiencia incorrectos y fechas inválidas. La clave se inyecta por configuración;
no se genera una clave diferente en cada arranque ni se guarda en Git.

El acceso viaja en una cookie `HttpOnly`, `SameSite=Lax`, `Secure` en producción,
sin `Domain` compartido y con la ruta de la aplicación. No va en URLs, registros,
`localStorage` ni eventos entre pestañas. La configuración local permite HTTP
solamente en el entorno de desarrollo.

La renovación usa una credencial opaca aleatoria de al menos 256 bits, también
en cookie HttpOnly. La base guarda su hash SHA-256 y el estado de la sesión.
No se almacena la credencial en claro. La primera implementación mantiene una
credencial por sesión lógica y la revoca al cerrar sesión; no afirma tener
rotación de refresh tokens ni detección de reutilización. Una rotación futura
requiere resolver transaccionalmente las solicitudes simultáneas de pestañas.

Límites iniciales: 30 minutos de inactividad y ocho horas absolutas. La renovación
no amplía el límite absoluto. Los sondeos automáticos de disponibilidad o estado
no cuentan como actividad del usuario.

La autenticación conserva estado en el servidor para permitir revocación
inmediata. Un JWT válido criptográficamente no basta si su sesión está revocada,
su usuario está desactivado o sus permisos cambiaron.

## Persistencia propuesta

Tabla nueva `sesion_autenticacion`, mediante migración aditiva:

| Campo | Propósito |
| --- | --- |
| `id_sesion` | Identificador aleatorio de la sesión lógica; clave primaria |
| `id_usuario` | Clave foránea al usuario autenticado |
| `refresh_hash` | Hash binario de 32 bytes; único |
| `csrf_token` | Secreto aleatorio de la sesión; nunca registrado en logs |
| `creada_en` | Inicio UTC |
| `ultima_actividad_en` | Actividad autorizada; no sondeos |
| `vence_en` | Límite absoluto UTC |
| `revocada_en` | Nulo mientras esté vigente |

El DAO usa consultas parametrizadas. La renovación y revocación se coordinan
con transacciones para que un refresh concurrente no reactive una sesión
revocada. Las filas vencidas se limpian con una política de retención definida;
la limpieza no es un requisito para negar accesos vencidos.

Esta tabla modifica el modelo físico y debe incorporarse al diagrama de la BD
cuando se apruebe la implementación. Este documento no ejecuta DDL en Railway.

## Integración con Servlet/JSP

Orden explícito en `web.xml`: autenticación, validación CSRF y autorización.
El filtro de autenticación cubre también exportaciones, APIs, login y registro;
las rutas públicas no exigen credenciales, pero nunca se consideran autenticadas
por un atributo antiguo de HttpSession.

Durante la transición, `HttpSession` puede mantener datos de presentación y
formularios anónimos. Los atributos usados por JSP se actualizan únicamente
después de validar el gestor; no son una vía alternativa de autenticación.
Los controladores migran a un contexto de usuario resuelto por solicitud y se
eliminan las inicializaciones duplicadas de login y registro.

Se mantiene CSRF para las operaciones autenticadas con cookies. Su valor permanece
estable durante renovaciones del mismo `sid`; cambia al iniciar otra identidad.
Login y registro también protegen sus formularios anónimos. Un endpoint de estado
no revela el CSRF de una cuenta nueva a una página antigua para reenviar su acción.

Si vence el JWT, el gestor verifica la credencial de renovación y el registro
vigente antes de emitir otro. No confía en un JWT vencido para identificar al
usuario. Si MySQL no está disponible, responde indisponibilidad temporal; no
convierte una caída de BD en credenciales incorrectas ni acepta accesos sin verificar.

Los eventos entre pestañas contienen sólo una notificación de cambio. La pestaña
receptora consulta al servidor; el evento por sí solo no concede ni revoca permisos.
No se recarga automáticamente un formulario clínico con cambios sin guardar.
Al cambiar de identidad se oculta el contenido protegido y se bloquean acciones
de la página anterior hasta resolver el estado actual.

Logout es POST protegido. Sin sesión activa es idempotente: vuelve al login.
Un formulario de logout de otra identidad nunca cierra silenciosamente la cuenta
actual. GET no cierra sesión. No se deshabilita CSRF para esconder el problema.

## Secuencia de implementación y verificación

1. Rama `feature/autenticacion-jwt` desde la base integrada, conservando los
   cambios pendientes de CSRF y rendimiento fuera de sus commits.
2. Configuración y componente JWT con pruebas de firma, fechas y claims.
3. Migración, DAO transaccional y gestor con expiración/revocación verificadas.
4. Integración conjunta de login, registro, filtros, logout y todos los roles.
5. Sincronización de pestañas y mensajes dentro de los formularios; retirar el
   borrador de confirmaciones que no formará parte del flujo normal.
6. Pruebas automatizadas y comprobación manual local antes de add/commit/push.
7. PR a develop; despliegue manual y pruebas en Railway con configuración propia.

No se activa una convivencia donde HttpSession o JWT puedan autorizar por
separado. La integración se publica sólo cuando el flujo completo esté probado.
Las sesiones antiguas requerirán un nuevo ingreso al activar la migración.

## Matriz de aceptación

| Caso | Resultado esperado |
| --- | --- |
| Login y logout de cada rol | Portal correcto; cierre sin página técnica |
| Registro de paciente | Misma emisión de credenciales que login |
| Dos pestañas antes de login | Identidad coherente tras autenticar |
| Dos pestañas, cierre desde una | La otra deja de aceptar acciones; logout repetido vuelve al login |
| Cambio de paciente a personal | No se envía un formulario de paciente como personal |
| JWT vence durante uso normal | Renovación transparente del mismo sid y CSRF |
| Dos renovaciones simultáneas | Ambas conservan la misma identidad sin expulsiones falsas |
| Logout concurrente con renovación | La sesión permanece revocada |
| Vence inactividad o límite absoluto | Reautenticación; ningún cambio clínico ejecutado |
| Firma, algoritmo, emisor o audiencia alterados | Acceso rechazado sin confiar en claims |
| Usuario desactivado o rol modificado | Credenciales anteriores dejan de autorizar |
| CSRF ausente o de otra sesión | Acción rechazada; respuesta HTML o JSON según contrato |
| Cambiar IDs de paciente/cita | Autorización de propiedad sigue impidiendo acceso ajeno |
| MySQL cae durante la verificación | Error temporal controlado; sin bypass |
| Atrás/restaurar pestaña | Estado comprobado sin repetir operaciones |
| Consulta de DNI local | Token del proveedor sólo en backend; sin cambios por JWT |
| Reinicio del contenedor | Clave estable y estado persistente; no pérdida de pacientes |
| Cookies de producción | HttpOnly, Secure, SameSite y alcance verificados |

## Referencias

- [JWT Best Current Practices, RFC 8725](https://www.rfc-editor.org/rfc/rfc8725.html).
- [OWASP: gestión de sesiones](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html).
- [OWASP: prevención de CSRF](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html).

## Estado de implementación

La migración aditiva, gestor, cookies, filtros, renovación y sincronización de
pestañas están implementados. La confirmación provisional de logout fue retirada.
HttpSession conserva una proyección para las JSP; en modo JWT se reconstruye
sólo después de la validación. `AUTH_MODE=session` conserva la configuración
anterior para una activación por entorno; no es una alternativa dentro del modo JWT.
Ver [configuración y pruebas actuales](autenticacion-jwt.md). Railway y la prueba
manual del usuario siguen pendientes antes de la publicación.
