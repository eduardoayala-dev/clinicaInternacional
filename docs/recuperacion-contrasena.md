# Verificación de cambios de cuenta mediante códigos

Rama Gitflow: `feature/recuperacion-verificacion-correo`, sobre `develop` con el
PR 39 de JWT ya fusionado. El conector Gmail fue comprobado con entregas reales.
Los flujos nuevos y su diseño permanecen sin commit hasta la prueba manual.

## Recorridos

| Operación | Verificación | Resultado |
| --- | --- | --- |
| Olvidé mi contraseña | DNI/CE de paciente o correo de personal → código al correo registrado → contraseña nueva | Revoca todas las sesiones de esa cuenta |
| Cambiar contraseña | Sesión JWT activa + contraseña actual → código al correo actual → contraseña nueva | Conserva la sesión actual y revoca las otras |
| Cambiar correo | Sesión JWT activa + contraseña actual → código al correo actual → nueva dirección → código al correo nuevo | Actualiza sólo tras ambas comprobaciones, revoca las otras sesiones y solicita un aviso a la dirección anterior |

Pacientes acceden a `/recuperar`; personal a `/recuperar?portal=personal`. La gestión
autenticada común a los cuatro roles está en `/cuenta/seguridad`. Hay enlaces desde
el perfil del paciente, perfil médico y los inicios de admisión y administrador.
Los formularios de edición de perfiles y personal ya no modifican correos sin
este proceso; la creación inicial de cuentas conserva su campo de correo.

## Controles del servidor

- Códigos numéricos de seis cifras generados con CSPRNG, diez minutos y cinco
  intentos como máximo. Solicitar otro invalida el anterior de la misma operación.
- Sólo se guarda HMAC-SHA256, con una clave privada de 32 bytes independiente del
  JWT. Incluye el identificador del reto, propósito y etapa; no se guarda el código
  en claro ni se usa un hash simple fácil de probar fuera de línea.
- El reto se liga a la sesión de navegador y, en cambios autenticados, a la sesión
  JWT original. No hay IDs de usuario elegibles en el formulario. No se puede usar
  una prueba de contraseña para autorizar un cambio de correo.
- El código se consume al verificarlo. El permiso para elegir una contraseña o
  introducir correo nuevo dura cinco minutos; el recorrido completo tiene un
  máximo de veinte minutos. Ningún token/código viaja por URL.
- Cambiar el correo, contraseña o estado de la cuenta invalida el reto. Un cambio
  de rol o revocación de la sesión también invalida los cambios autenticados.
- Todas las escrituras exigen CSRF y se vuelven a comprobar en el servidor. GET
  de `/restablecer` no muestra el formulario si falta la verificación vigente;
  un POST directo tampoco permite guardar un cambio.
- Contraseña nueva: mínimo doce caracteres, mayúscula, minúscula, número, símbolo,
  máximo 72 bytes UTF-8 por BCrypt y sin DNI del paciente. Se confirma dos veces.
- Guardado, revocación de sesiones, consumo del permiso y auditoría pertenecen a
  una transacción. Un fallo revierte el cambio. Dos solicitudes concurrentes no
  pueden consumir dos veces la misma autorización.

La recuperación anónima responde igual para datos ausentes, inactivos o limitados.
Procesa el envío en segundo plano para no revelar existencia por la espera del
proveedor. No publica un correo real parcialmente oculto antes de verificar.
La verificación del correo no equivale a implementar MFA para todos los logins.

## Límites y envío

Se conservan contadores en MySQL: cien solicitudes globales por hora, diez por IP
y tres por identificador/cuenta. Las solicitudes de cuentas inexistentes también
cuentan. La reautenticación tiene además cinco intentos por quince minutos. Para
IP se usa `getRemoteAddr`; no se confía en un X-Forwarded-For arbitrario. Tras un
proxy puede agrupar usuarios; revisar esa configuración antes de aumentar tráfico.

La cola local tiene dos trabajadores y 32 entradas. No es durable: un redeploy
puede interrumpir un correo pendiente y requerir otra solicitud. Los errores de
envío no modifican contraseñas ni correos. Un aviso posterior fallido no revierte
un cambio ya confirmado; tampoco se presenta aceptación de Gmail como entrega.
No hay reenvío automático ante resultados inciertos ni envíos en pruebas de carga.

## Esquema y configuración

V007 aún no se ha publicado. Ahora crea `verificacion_cuenta`, compartida por los
tres propósitos, y `limite_recuperacion`. El modelo pasa de 24 a 26 tablas; actualizar
el diagrama físico con esta entrega. La anterior tabla del prototipo con enlaces
no forma parte del modelo final. En local se retira después de un respaldo y
comprobar que no contiene solicitudes vigentes; no se borran cuentas ni citas.

1. Tener JWT y V006 configurados.
2. Respaldar la base y aplicar **sólo**
   `sql/migrations/V007__verificacion_cuenta.sql`; no ejecutar schema.sql sobre
   una base con datos. El script consolidado y verificador incluyen el nuevo modelo.
3. Autorizar el buzón según `docs/correo-gratuito.md`.
4. Ejecutar `./scripts/Preparar-Verificacion.ps1`: añade una clave aleatoria a
   `.env.mail` sin mostrarla ni reemplazar una clave ya configurada.
5. Configurar `MAIL_MODE=gmail`, `RECOVERY_ENABLED=true` y la clave privada
   `ACCOUNT_VERIFICATION_KEY`. Conservar client ID, client secret y refresh token.
6. Arrancar con `./scripts/Iniciar-Local.ps1 -Jwt -Correo`.

La clave de verificación debe ser estable entre reinicios y privada. Rotarla
invalida códigos pendientes, sin cambiar claves JWT. Gmail API usa HTTPS y no
necesita SMTP ni dominio propio. OAuth External/Testing puede caducar a los siete
días; volver a autorizar cuando sea necesario. No guardar `.env.mail` ni el JSON
de Google en Git. Railway no se ha configurado ni desplegado en esta entrega.

## Diseño y pruebas manuales

Referencias: Figma `7McVtmlYL8lEcM1NEVm93l`, frames `253:54` (paciente) y `475:198`
(personal). Se conserva su distribución, tipografía, paleta y SVG locales. Los
textos de enlace se sustituyen por código según el nuevo requisito. Las nuevas
etapas reutilizan el mismo diseño y son adaptables a móvil.

Desde los logins de paciente y personal, la recuperación se muestra dentro de la
misma página. Solicitud, código y nueva contraseña usan peticiones en segundo
plano y conservan la URL del login. Los formularios se renderizan una sola vez en
el servidor, también para las vistas independientes. El encabezado
`X-Recuperacion-Inline` únicamente solicita ese fragmento visual: mantiene CSRF,
vínculo con la sesión, límites y verificación del código. No se reintentan los POST
automáticamente ante errores o tiempos de espera. Los cambios autenticados desde
el perfil conservan su flujo independiente.

1. En recuperación, usar el DNI de una cuenta de paciente cuyo correo controles
   o el correo registrado de una cuenta del personal. Comprobar que llega un código.
2. Verificarlo, elegir una contraseña nueva y comprobar el acceso. Reutilizar la
   verificación debe rechazarse. No compartir códigos en el chat.
3. Desde el perfil o Seguridad de cuenta, cambiar contraseña con la actual y el
   código. Otra pestaña de la misma sesión se conserva; otra sesión independiente
   de la cuenta pierde acceso.
4. Cambiar correo: verificar dirección actual, introducir una dirección distinta
   que controles y verificar su código. Comprobar que antes de ese segundo código
   se conserva el correo actual y que al finalizar llega el aviso al anterior.
5. Probar código incorrecto, caducado y otro propósito sin forzar solicitudes reales
   repetidas: los límites y la concurrencia ya se ensayan con datos sintéticos.

Las suites Java, MySQL y JavaScript usan códigos sintéticos y emisor simulado.
La validación en navegador comprueba diseño y accesos sin enviar a terceros.
Los resultados definitivos y evidencia local se registran en `target/verificacion-*`.
Quedan pendientes la prueba manual del usuario, SonarCloud del futuro PR y cloud.

## Resultados del 30 de octubre de 2026

- Suite Java: 622 casos encontrados, 411 aprobados y 211 omitidos por entorno,
  sin errores ni fallos. Los contratos MySQL omitidos se ejecutan por separado.
- MySQL temporal con TLS: 29 aprobados, sin omisiones. Incluye los 26 casos de
  los recorridos de códigos y tres controles del entorno de pruebas.
- JavaScript de verificación, sesión y disponibilidad: 13 aprobados.
- Edge: variantes paciente/personal, móvil, fuentes, SVG locales con dimensiones
  originales, ausencia de desbordamiento y bloqueo del cambio sin código válido.
  Cero errores JavaScript y sin envíos externos durante esa prueba.
- Migración de 18 a 26 tablas, dos aplicaciones sobre MySQL 8.4.13, con metadatos,
  datos y UTF-8 conservados y restauración del respaldo sintético comprobada.
- Antes de actualizar local se guardó un respaldo privado. Se retiró la tabla del
  prototipo que sólo contenía una solicitud vencida, sin solicitudes vigentes.
  Se conservaron las cantidades de usuarios, pacientes y citas y quedan 26 tablas.

No se suman contratos compartidos como casos únicos. No se restauró el respaldo
real, no se midió capacidad de carga y no se desplegó esta entrega en Railway.
