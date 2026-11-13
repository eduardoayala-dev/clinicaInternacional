# Autenticación JWT y prueba local

Implementada en `feature/gestor-autenticacion-jwt`. Pendiente de la prueba manual
antes de commits/PR y de la comprobación posterior en Railway.

## Funcionamiento

Login y registro utilizan `InicioSesion`. En modo JWT, `GestorAutenticacion`
valida un JWT HS256 de cinco minutos y consulta la sesión persistente antes de
proyectar el usuario en HttpSession para los controladores/JSP existentes.
HttpSession por sí sola no autoriza en este modo. Los cuatro roles conservan sus
portales y permisos; se siguen comprobando propietario y médico asignado a la cita.

Las credenciales viajan en cookies HttpOnly/SameSite=Lax, con Secure en producción.
No se guardan en URLs ni localStorage. La notificación entre pestañas sólo escribe
una marca de tiempo. El JWT incluye identificadores internos, emisor, audiencia,
rol y fechas; no contiene nombres, DNI ni datos clínicos.

La renovación utiliza una credencial aleatoria de 256 bits, cuyo hash SHA-256 se
almacena en `sesion_autenticacion`. Conserva el sid y el CSRF. No rota la credencial
de renovación en cada petición: evita invalidaciones falsas entre pestañas, pero
no implementa detección de reutilización de refresh tokens rotativos.

Cada sesión tiene un máximo de ocho horas y 30 minutos de inactividad. Los sondeos
de `/auth/estado` y `/health/ready` no prolongan la actividad. La renovación no
amplía el límite absoluto. Un logout, una desactivación, un cambio de rol o un
cambio de contraseña impiden que sus credenciales anteriores autoricen nuevas
peticiones. No cancela retroactivamente una operación que ya había sido autorizada.

El estado se comprueba también al enviar un formulario, recuperar una pestaña o
recibir un cambio de otra pestaña. La misma sesión actualiza su formulario sin
confirmaciones adicionales. Si cambió la cuenta, se bloquea el formulario anterior;
no se reenvían acciones clínicas. Si terminó la sesión se vuelve al login.
La protección CSRF continúa activa y estable durante la renovación.

Si MySQL no está disponible, la autenticación devuelve 503 y Retry-After. No
acepta una HttpSession antigua como sustituto. El healthcheck, en modo JWT, también
comprueba la presencia de las columnas necesarias de la nueva tabla.

## Configuración y activación

La activación es explícita: `AUTH_MODE=jwt`. Sin ella se conserva temporalmente
el modo anterior `session` para permitir una migración controlada. No hay una
configuración donde ambos mecanismos autoricen de forma alternativa.

| Variable | Valor |
| --- | --- |
| AUTH_MODE | jwt |
| AUTH_JWT_KEY | Base64 estándar de 32 bytes aleatorios; privada y estable |
| AUTH_ISSUER | clinica-internacional |
| AUTH_AUDIENCE | clinica-web |

`config/auth.env.example` es una plantilla sin clave. `scripts/Preparar-Autenticacion.ps1`
crea `.env.auth` una sola vez y conserva una clave existente. Ese archivo está
excluido de Git y del contexto Docker. Nunca se imprime su contenido.

Aplicar primero `sql/migrations/V006__sesiones_autenticacion.sql` con una cuenta de
migración. Es aditiva e idempotente; no ejecutar `schema.sql` sobre una base existente.
La cuenta de aplicación sólo necesita sus permisos CRUD sobre la nueva tabla.
El esquema completo para instalaciones nuevas también incluye la tabla.

En el entorno local de esta revisión ya se aplicó la migración y se creó un
respaldo privado dentro del volumen MySQL. Se compararon las cantidades de usuarios,
pacientes y citas antes/después y se conservaron. No se ha ensayado restaurar ese
respaldo real ni se ha modificado Railway.

Arranque habitual después de la migración:

```powershell
./scripts/Preparar-Autenticacion.ps1
./scripts/Iniciar-Local.ps1 -Jwt
```

Usar http://localhost:8080/login/paciente o http://localhost:8080/login/personal.
Las cuentas siguen en la misma base; no hay que registrarlas de nuevo. Al activar
la migración se debe ingresar nuevamente. La consulta DNI conserva `.env.apisperu`.
No usar cuentas de Railway suponiendo que existen en la base local.

Para Railway: aplicar la migración con respaldo, configurar las cuatro variables
en el servicio Java y desplegar manualmente. Usar una clave propia del entorno y
mantenerla estable en todos sus reinicios/réplicas. Comprobar Secure/HTTPS, login,
renovación y logout allí; las pruebas HTTP locales no validan esas cookies cloud.

## Pruebas automatizadas

```powershell
mvn test
node --test tests/*.test.cjs
python scripts/Probar-MySql.py
```

El runner MySQL requiere Docker, la imagen mysql:8.0 y la red local
`clinicainternacional_default`. Crea un servidor temporal con credenciales
aleatorias y una base `clinica_agendas_test_*`. Compila en una carpeta interna
separada y elimina el servidor temporal al terminar. No reinicia ni limpia la base
habitual. Las conexiones de prueba usan Hikari y TLS REQUIRED; se comprueba que
`Ssl_cipher` no esté vacío. El arranque del pool espera su inicialización antes de
comenzar los casos, mientras que la espera de cada adquisición sigue limitada.
Los ensayos no desactivan TLS ni las claves foráneas.

La limpieza compartida `EsquemaPruebas` retira tablas dependientes primero y rechaza
bases MySQL que no correspondan al prefijo temporal. H2 sólo se utiliza en pruebas
en memoria; no vuelve a ser una alternativa de almacenamiento de la aplicación.

Cobertura específica: firma/algoritmo/claims, expiración, cookies, restauración de
sesión, renovación concurrente, logout concurrente, cambio de contraseña/rol/estado,
inactividad sin extensión por sondeos, límite absoluto, sesión HttpSession falsa,
indisponibilidad de MySQL, formularios de otra identidad y envíos duplicados.

Las pruebas de navegador usan cuentas sintéticas en Tomcat y verifican los cuatro
roles, renovación, logout entre pestañas, revocación y protección CSRF. No consultan
DNIs externos. Las evidencias locales se guardan en target y no contienen claves.

## Comprobación manual antes del PR

1. Abrir paciente y personal en dos pestañas del mismo navegador.
2. Ingresar con una cuenta local: ambas pestañas deben reconocer esa identidad.
3. Cerrar sesión desde una; la otra debe volver al login sin JSON ni confirmación.
4. Ingresar como personal y navegar por el portal; cerrar sesión normalmente.
5. Probar datos incorrectos: se muestra el mensaje habitual y el botón se recupera.
6. Para cuentas distintas a la vez, utilizar perfiles de navegador separados.

La tabla nueva debe añadirse al diagrama físico de la base de datos. Incluye sid,
usuario, hash de renovación, CSRF, rol inicial, huella de credencial, fechas de
creación/actividad/vencimiento y revocación. La limpieza periódica de filas antiguas
es una tarea de mantenimiento posterior; no condiciona el rechazo de sesiones vencidas.

## Resultado de esta implementación

Java: 362 pruebas aprobadas (547 totales; 185 condicionadas al entorno).
MySQL real: 184 aprobadas sin omisiones; JavaScript: 43 aprobadas.
Los contratos MySQL repiten casos de la suite Java; no son cifras para sumar.
Migración aislada MySQL 8.4.13: 18 a 24 tablas, dos aplicaciones idempotentes,
seis comparaciones de metadatos y tres restricciones negativas aprobadas.
Se conservó la codificación del respaldo sintético. El respaldo local real se
creó antes de la migración, pero no se restauró para este ensayo.

Los cuatro roles se verificaron en navegador. Tras recompilar el WAR se repitió
el flujo de paciente/personal, renovación y cierre entre pestañas. El Tomcat 8084
y las dos cuentas temporales administrativas se retiraron; el laboratorio 8083
fue reanudado. La aplicación habitual permanece en localhost:8080 con JWT y
configuración DNI existente. No se realizó ninguna consulta DNI externa.

La primera compilación de algunas vistas locales produjo tiempos de espera en
el navegador; eso queda registrado, no resuelto como mejora de rendimiento. La
validación funcional posterior pasó. No se ejecutaron en esta ronda las tres
pruebas opcionales de privilegios/TLS cloud; Railway no se modificó.
