# Revisión previa al PR de autenticación

Fecha: 2026-10-29. Rama examinada: `fix/recuperacion-login-csrf`.

## Dictamen inicial (histórico, antes de las correcciones)

No está lista para presentarse como migración a JWT. Hay una corrección de
recuperación CSRF en borrador y un documento de diseño JWT, pero no existe todavía
el gestor JWT, su integración, renovación ni revocación persistente.

La revisión cubre los cambios pendientes y su interacción con login, logout,
registro y autorización de los cuatro portales. No constituye una certificación
de toda la aplicación ni una prueba de capacidad en Railway.

## Hallazgos pendientes

1. **Alcance incompleto:** `LoginServlet`, `RegistroPacienteServlet` y `AuthFilter`
   siguen autenticando mediante HttpSession. No hay pruebas de JWT implementado.
2. **Flujo pendiente de sustituir:** con una sesión activa y un formulario de
   logout antiguo se redirige a `/logout/confirmar`. Se reprodujo en navegador.
   Ese comportamiento es seguro frente a un cierre sin CSRF, pero conserva la
   pantalla adicional que se decidió sustituir. No debe darse por aprobado como
   la experiencia final solicitada.
3. **Revocación pendiente en el gestor:** `AuthFilter` consulta el usuario guardado
   en sesión; no vuelve a consultar en ese punto el estado actual de la cuenta.
   `AuthService` comprueba `activo` al ingresar. Esta inspección no demuestra que
   desactivar una cuenta ya autenticada revoque inmediatamente todas sus sesiones.
   Debe probarse e implementarse con el gestor antes de afirmar esa garantía.
4. **Cambios mezclados en el directorio:** hay archivos de rendimiento y un
   iniciador local fuera del alcance de autenticación. Se deben separar al
   preparar commits; en esta revisión no se añadieron al índice.
5. **Demora observada:** la primera auditoría HTTP agotó 30 segundos al abrir
   GET `/logout` (página de error 405). Una consulta posterior tardó unos 0,10 s
   y la auditoría completa repetida pasó. La causa no se ha demostrado; no debe
   presentarse como rendimiento estable ni atribuirse sin evidencia a Railway.
6. **Preparación de pruebas MySQL incompatible entre suites:** al ejecutar las
   seis suites juntas, `OfertasContrato` deja `notificacion` con una FK a `cita`.
   `CitaAutorizacionTest.setUp` intenta borrar `cita` sin retirar esa dependencia.
   Eso provoca 116 errores de preparación en admisión, atención, historial y
   portal médico. Se requiere aislar el esquema por suite o unificar la limpieza
   respetando dependencias; no desactivar las claves foráneas de producción.
7. **Conexión interrumpida en integración:**
   `AgendaCitaMySqlTest.cancellationReleasesSlotAndCannotBeRepeated` reportó
   `CommunicationsException` / `SocketException: Broken pipe` durante el handshake
   TLS. No se ha demostrado que proceda de código de negocio ni que esté
   resuelto. El entorno local mostró demoras; esa coincidencia no prueba la causa.

## Pruebas realizadas

| Comprobación | Resultado |
| --- | --- |
| Maven, suite general del borrador | 513 pruebas: 339 aprobadas, 174 omitidas, cero fallos/errores |
| Node, todos los archivos `tests/*.test.cjs` | 37 aprobadas, cero fallos |
| Matriz nueva de autorización por rol | Cuatro tests de AuthFilter aprobados; la nueva matriz cruza 16 combinaciones |
| Recuperación de login paciente y personal | POST antiguo devuelve 303; GET crea formulario nuevo y descarta contraseña |
| JSON con CSRF incorrecto | Mantiene 403 y `CSRF_INVALIDO` |
| Paciente en dos pestañas de distintos logins | Comparte la identidad; segundo logout vuelve al login |
| Logout antiguo con identidad activa | No invalida mediante GET; muestra confirmación; POST válido cierra |
| Cookies de los dos entornos locales | Separadas entre localhost:8080 y 127.0.0.1:8083 |
| Auditoría HTTP en laboratorio | 30 comprobaciones aprobadas en repetición, tras el timeout registrado |
| Diff | `git diff --check` sin errores |
| Seis suites contra MySQL 8.0.46 separado, con TLS REQUIRED | 171 ejecutadas: 54 aprobadas, 117 errores, cero omitidas; ejecución NO aprobada |

La matriz nueva se ejecutó después de la suite general; no se suma como si se
hubiera repetido toda la suite modificada. Las pruebas de navegador usan el
contenedor local ya construido; el único cambio de código posterior a esa
construcción durante esta revisión es una prueba Java de autorización.

El script antiguo `target/csrf-recuperacion-ui.cjs` esperaba el flujo anterior
de 403 para logout y falló contra el borrador con confirmación. Se conservó ese
resultado y se generó una comprobación actualizada separada. La confirmación
activa se verifica además en `target/logout-multitab-ui.cjs`; no se elimina esa
comprobación para aparentar conformidad con el diseño nuevo.

Los dos primeros intentos de integración se interrumpieron para separar la
compilación y después evitar las esperas de escritura sobre el almacenamiento
del servidor local. No se contabilizan como ejecuciones aprobadas. El intento
final terminó completo en un MySQL desechable con datos en memoria; sus errores
no se ocultaron mediante reintentos ni aumentando los timeouts. El runner retiró
el contenedor temporal al terminar. La integración de esta revisión no valida
TLS VERIFY_IDENTITY de Railway; utiliza REQUIRED del laboratorio.

## Evidencias locales

- `target/revision-pr-java.log`
- `target/revision-pr-js.log`
- `target/revision-pr-roles.log`
- `target/revision-pr-multitab.log`
- `target/revision-pr-recuperacion.log` (expectativas antiguas; fallo conservado)
- `target/revision-pr-recuperacion-actual.log`
- `target/revision-pr-http.log` (timeout inicial)
- `target/revision-pr-http-repeticion.log`
- `target/revision-pr-mysql-aislado.log`
- `target/revision-pr-mysql-aislado-ejecucion.log`
- `target/revision-pr-mysql-reports/`

Los archivos de target son evidencia local ignorada por Git, no entregables del
PR. No se consultaron DNIs externos ni se imprimieron claves. Los datos reales
de pacientes y los volúmenes existentes se conservaron.

## Condiciones para cerrar el PR de JWT

Implementar y verificar el [gestor diseñado](diseno-autenticacion-jwt.md), retirar
el flujo provisional de confirmación, comprobar expiración/renovación/revocación
y concurrencia entre pestañas, y probar manualmente antes de add/commit/push.
SonarCloud del futuro commit y el despliegue en Railway requieren validación
posterior; esta revisión local no los da por aprobados.
Antes de la siguiente validación completa, corregir la contaminación entre
suites MySQL e investigar/reproducir el error de conexión registrado.

## Correcciones y validación posteriores

Implementadas en `feature/gestor-autenticacion-jwt`:

- Limpieza común del esquema temporal, con tablas hijas antes de sus padres y
  validación del catálogo antes de cualquier DROP de pruebas.
- Pool Hikari para las suites reales; inicialización completada antes de prestar
  conexiones. TLS sigue REQUIRED y se comprueba el cifrado negociado.
- Gestor JWT, sesiones persistentes revocables, cookies, renovación y protección
  de formularios entre pestañas. Login y registro comparten la inicialización.
- Retirada la confirmación adicional de logout. Un cambio real de identidad
  bloquea el formulario antiguo y una sesión cerrada vuelve al login.
- Detección de producción compartida entre base y cookies, incluyendo los
  indicadores de Railway; así se aplica Secure de manera coherente.
- Esquema, migración consolidada y verificador actualizados a 24 tablas. Se quitó
  un bloque idéntico duplicado del verificador.

Resultados actuales:

| Comprobación | Resultado |
| --- | --- |
| Java, suite final y paquete WAR | 547 ejecutadas: 362 aprobadas, 185 omitidas por entorno; cero fallos/errores |
| MySQL 8.0 real aislado | 184 aprobadas, cero omitidas/fallos/errores; incluye sesiones JWT y cifrado TLS |
| JavaScript | 43 aprobadas, cero fallos |
| Navegador | Cuatro roles; renovación; dos pestañas; logout; revocación; formularios antiguos sin confirmación |
| Migración MySQL 8.4.13 | 18 a 24 tablas, aplicada dos veces; seis comparaciones de metadatos y tres restricciones negativas aprobadas |
| Datos y codificación del respaldo sintético | Conservados; restauración sintética comprobada |
| Migración local real | Respaldo privado y cantidades de usuarios/pacientes/citas conservadas |
| Local habitual | AUTH_MODE=jwt, healthcheck 200 y dos formularios con el cliente JWT; clave DNI configurada sin consultarla |

No se suman las cifras de Java/MySQL como si fueran casos distintos: muchas
pruebas comparten contratos. Las tres pruebas opcionales de privilegios/TLS
VERIFY_IDENTITY de cloud no se ejecutaron en esta ronda. La integración real de
184 casos utiliza TLS REQUIRED; no certifica la configuración de Railway.

La ejecución intermedia de 183 casos tuvo cuatro errores de adquisición antes de
la corrección final de inicialización del pool. La ejecución posterior de 184
casos terminó sin errores. No se desactivó TLS ni se eliminaron aserciones de
negocio para obtener el resultado.

Las primeras aperturas del portal administrativo agotaron el tiempo de espera de
navegación; con la vista ya disponible se comprobó el acceso real y los permisos.
Se conserva esa observación de arranque en frío; no se presenta como un resultado
positivo de rendimiento o carga.

La revisión automática rechazó un intento de ejecutar el verificador completo
por posible escritura. Se inspeccionó el contenido, se comprobó que sólo contenía
USE y SELECT, se eliminó el duplicado y se ejecutaron exclusivamente sus cinco
SELECT. No se ejecutaron escrituras por esa comprobación. La migración completa
se ensayó por separado en un contenedor desechable sin red.

Evidencias adicionales:

- `target/jwt-validacion-publicable.log`
- `target/jwt-js.log`
- `target/pruebas-mysql/ejecucion.log` y `reports/`
- `target/jwt-navegador-final.log` y `target/jwt-navegador.json`
- `target/jwt-roles-navegador.log`
- `target/jwt-migracion-completa.log`
- `target/validacion-migracion-railway/resultado.json`
- `target/jwt-migracion-local.json`

Estado: implementación y comprobaciones locales completadas. El usuario confirmó
el cierre de sesión sincronizado entre pestañas y autorizó continuar con el PR.
Pendientes revisión SonarCloud del commit y validación cloud cuando se publique
el despliegue. Railway no se cambió.
Guía: [autenticacion-jwt.md](autenticacion-jwt.md).

## Revisión de SonarCloud del PR 39

La primera revisión señaló la composición dinámica de la consulta de sesiones,
complejidad y propagación de errores de transporte. La consulta ahora es estática
y parametrizada para ambas formas de búsqueda; se comprueba que un ID desconocido
no pueda resolverse por un refresh presentado a la vez. Se separó el rechazo CSRF
en un método y se manejan las desconexiones sin repetir las operaciones.

La suite Java posterior aprobó 363 casos y omitió 185 por entorno, sin fallos.
Incluye la prueba de desconexión HTTP y los contratos de sesiones actualizados.
La comprobación MySQL específica se repite por el cambio de consulta. Estos
ajustes no cambian la interfaz ni los permisos; la nueva revisión de SonarCloud
debe aprobar antes de fusionar el PR.
