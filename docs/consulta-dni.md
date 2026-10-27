# Consulta opcional de DNI en el registro

El formulario del paciente permite consultar nombres y apellidos por DNI a través
del servidor Java. El navegador nunca recibe la clave ni se conecta a APIsPERU.
Primero se elige el tipo de documento y se ingresa el número. La consulta se
realiza automáticamente al salir del campo DNI, si tiene 8 dígitos. No hay botón.
Los nombres y apellidos consultados quedan en solo lectura; fecha de nacimiento
y sexo/género se completan manualmente.
Los nombres se presentan y guardan con iniciales mayúsculas: `CARLOS EDUARDO`
pasa a `Carlos Eduardo`. Se conservan tildes y ñ, partículas como `del` o `de la`,
guiones y apóstrofos. Se respetan grafías que ya incluyen mayúsculas diferenciadas,
como `McDonald`; no se infieren tildes que el proveedor no haya proporcionado.
El modelo de usuario aplica el mismo formato al cargar cuentas existentes, sin
ejecutar una actualización masiva de la base de datos.
Carné de Extranjería y registro manual continúan disponibles.

## Configuración privada

### Preparación local en Windows

El archivo privado `.env.apisperu` contiene únicamente la configuración de APIsPERU,
separada de las contraseñas de MySQL. Copiar las entradas de
`config/apisperu.env.example` si el archivo aún no existe. Pegar la **nueva** clave
después de `APISPERU_TOKEN=` y guardar, sin comillas ni prefijo `Bearer`.
El archivo está excluido de Git y de la imagen Docker.

El iniciador carga esas variables en Java sin mostrarlas ni incluirlas en los
argumentos de los procesos. No cambia variables globales de Windows ni usa
`Invoke-Expression` para leer la configuración:

```powershell
.\scripts\Iniciar-ConsultaDni.ps1 -SoloValidar
.\scripts\Iniciar-ConsultaDni.ps1 -Reiniciar
```

El segundo comando reinicia únicamente Jetty de este proyecto en el puerto 8082,
si ya está ejecutándose, y abre el servicio en
`http://127.0.0.1:8082/login/paciente?action=register`.
Usar esa dirección permite mantener separada su cookie de la aplicación en
`localhost:8080`. No reinicia Docker ni configura MySQL. La validación del archivo
no consulta APIsPERU; la cuota se utiliza al salir de un DNI válido. Volver a salir
del mismo campo sin cambiar el documento no repite la consulta, incluso si falló.
Si el servidor devuelve HTTP 429, el mensaje muestra la espera indicada por
`Retry-After`; al vencer, salir otra vez del mismo DNI permite reintentar.

### Docker y despliegue

1. Revocar cualquier clave que se haya compartido y generar una nueva en APIsPERU.
2. Añadir las entradas de `config/apisperu.env.example` al `.env` privado existente,
   sin reemplazar sus contraseñas de MySQL. No versionar ese archivo.
3. Establecer `APISPERU_TOKEN` y ajustar `DNI_DAILY_LIMIT` a la cuota contratada.
   El valor 100 es un límite local predeterminado, **no una afirmación de cuota gratuita**.
4. Reiniciar el proceso que ejecuta la aplicación para cargar las variables.

Java/Jetty no lee `.env` automáticamente: las variables deben existir en el entorno
del proceso. Para Railway, configurarlas en las variables privadas del servicio Java.
No ponerlas en JSP, JavaScript, argumentos de URL, capturas ni colecciones compartidas.
No pegar la clave en el chat. Ausencia o formato inválido de clave: respuesta 503,
sin ninguna llamada externa y con registro manual disponible.

En Docker, `compose.dni.yml` agrega las variables al servicio `app` sin sustituir
la configuración de base de datos. **La rama de DNI parte de develop; el PR #25 de
conexión segura es independiente y debe prepararse/integrarse antes de reconstruir
la pila local existente con la configuración de MySQL de ese PR.** Después:

```powershell
docker compose -f docker-compose.yml -f compose.dni.yml up -d --build app
```

Reutilizar ambos archivos en ejecuciones posteriores para conservar las variables.
No se ha usado la clave compartida ni consultado personas reales durante el desarrollo.

## Contrato HTTP propio

`POST /api/v1/documentos/dni/consulta`

Headers: `Content-Type: application/json`, `X-CSRF-Token: <token de esta sesión>`.
También se requiere la cookie de sesión obtenida al abrir el formulario de registro.
El navegador no recibe ni envía el token del proveedor. Según el contrato de
APIsPERU, Java realiza un GET a `/api/v1/dni/{numero}?token=...` mediante HTTPS.
La URL externa contiene una credencial: nunca debe registrarse ni exponerse en
trazas, errores o respuestas. No se siguen redirecciones.

```json
{"numero":"99000001"}
```

Ejemplo **simulado** de éxito (no garantiza que el DNI exista en el proveedor):

```json
{
  "code": "DNI_ENCONTRADO",
  "message": "Datos consultados correctamente.",
  "data": {
    "numero": "99000001",
    "nombres": "PACIENTE PRUEBA",
    "apellidoPaterno": "FICTICIO",
    "apellidoMaterno": "LOCAL"
  }
}
```

En errores se mantienen `code`, `message` y `data: null`. Cabecera `Cache-Control:
no-store`. La respuesta del proveedor no se transmite al navegador.

| HTTP | Significado en nuestra aplicación |
| --- | --- |
| 200 | Consulta exitosa y documento de respuesta coincidente |
| 400 | JSON, campos o DNI inválidos (exactamente 8 dígitos ASCII, como texto) |
| 403 | Sesión/CSRF ausente, incorrecto o vencido |
| 404 | El proveedor devuelve 404 para la consulta |
| 405 | Método diferente de POST; `Allow: POST` |
| 413 | Cuerpo superior a 1024 caracteres |
| 415 | Tipo de contenido diferente de JSON |
| 429 | Límite local; incluye `Retry-After` en segundos |
| 502 | Fallo de transporte, respuesta inválida, redirección o estado inesperado |
| 503 | Sin configuración, credenciales rechazadas, cuota del proveedor o fallo externo 5xx |
| 504 | Se agotó el tiempo de espera del proveedor |

APIsPERU documenta HTTP 200 tanto para datos como para `ErrorResponse`, con
`success` y `message`. Un `success: false` se transforma en 503 controlado,
sin reenviar su mensaje. El 400 externo se trata como fallo de respuesta (502),
sin suponer que significa «persona inexistente». El mapeo defensivo de otros
estados no supone que el proveedor los tenga documentados.
Esta entrega define el contrato de este endpoint y añade `data: null` al error CSRF
global; no migra todavía los contratos de los demás endpoints de la aplicación.

## Seguridad, cuota y límites

- Destino fijo `https://dniruc.apisperu.com/api/v1/dni/{numero}`, sin redirecciones;
  certificado HTTPS verificado por Java. Conexión: 3 s; petición: 6 s.
- Respuesta externa limitada a 16 KiB durante la recepción. Validación de tipos,
  longitud de nombres y coincidencia del DNI. Se conservan como máximo cinco
  consultas por sesión, con validez de diez minutos, para respaldar los nombres
  usados en el registro. No se comparten entre sesiones ni se guardan en logs.
  Consultar nuevamente un DNI vigente en la misma sesión reutiliza esos datos
  antes de consumir contadores o llamar al proveedor, incluso después de recargar
  la página. Las peticiones simultáneas de esa sesión se serializan para evitar
  consumir cuota por duplicado. Tras vencer el dato se aplica una consulta normal.
- CSRF por sesión también antes del proveedor; límite de 5 solicitudes por sesión
  y por IP cada 15 minutos, 30 por hora por instancia y presupuesto diario configurable.
  Solicitudes con DNI inválido no consumen esos contadores ni llaman a APIsPERU.
  Los intentos válidos fallidos sí consumen el presupuesto local, de forma conservadora.
- Contadores en memoria, sincronizados y acotados. Se reinician con el proceso y no
  se comparten entre réplicas. Para escalar, migrar a un almacén compartido y persistente.
  Estos límites no garantizan por sí solos una cuota mensual del proveedor.
- Se usa `getRemoteAddr`, sin confiar en `X-Forwarded-For` enviado por clientes.
  Tras un proxy, clientes pueden compartir el límite de IP. Configurar la resolución
  de IP únicamente en un proxy/contenedor de confianza antes de ajustar esta política.
- No registrar claves, DNI, nombres ni cuerpos del proveedor. Revisar también los
  logs HTTP de infraestructura; no habilitar trazas de peticiones del cliente Java.
- Es una ayuda de autocompletado, **no autenticación ni prueba de titularidad del DNI**.
  Los nombres consultados quedan en solo lectura. Durante diez minutos, el servidor
  utiliza los nombres conservados en la sesión para ese DNI, ignorando modificaciones
  de nombres enviadas por el cliente. Sin una consulta vigente sigue disponible el
  registro manual; esta ayuda no convierte la cuenta en una identidad verificada.
  Fecha de nacimiento, dirección, correo y teléfono no vienen de esta consulta.

## Verificación sin cuota externa

```powershell
mvn clean verify
node --test tests/consulta-dni.test.cjs
```

Las pruebas Java inyectan un transporte simulado; no requieren clave ni conexión
a APIsPERU. Cubren proveedor, contrato, CSRF, validación, fallos y límites concurrentes.
Las pruebas JavaScript cubren autocompletado, registro manual, cambio de documento,
respuesta atrasada, campos en solo lectura, envío durante una consulta y ausencia
de solicitudes duplicadas al salir varias veces del mismo DNI.

Para previsualizar el formulario sin sustituir el servicio existente en 8080:

```powershell
mvn -Djetty.http.port=8082 jetty:run
```

Abrir `http://127.0.0.1:8082/login/paciente?action=register`. Jetty queda enlazado
a `127.0.0.1`. Esta previsualización y las pruebas del endpoint no necesitan conexión
a MySQL; los demás flujos funcionales sí requieren configurar su base de datos.

Importar `docs/postman/Consulta-DNI.postman_collection.json` para verificar el contrato
con un servidor local. La colección captura sesión/CSRF y solo hace consultas inválidas
o rechazadas **antes del proveedor**. La petición real está desactivada salvo que se
complete expresamente la variable `dniConsultaAutorizada` con un DNI propio/autorizado.
Nunca introducir la clave del proveedor en Postman: las pruebas apuntan a nuestro servidor.

Fuente del contrato externo: https://dniruc.apisperu.com/doc

### Resultado local del 27 de octubre de 2026

- `mvn clean verify`: 68 pruebas Java, sin fallos, errores ni omisiones; WAR generado.
- `node --test tests/consulta-dni.test.cjs`: 8 pruebas, todas correctas.
- Postman CLI contra Jetty local: 7 solicitudes, 14 comprobaciones correctas;
  petición real omitida. Reporte local no versionado: `target/postman-dni.json`.
- Formulario servido con HTTP 200 y consulta automática; sin clave configurada,
  una consulta sintética válida devuelve HTTP 503 con el contrato definido.

Las pruebas automatizadas usan un transporte simulado. El usuario confirmó además
que la consulta real funciona desde el formulario con su token privado. No se
incluyen DNI reales ni credenciales en los ejemplos o reportes compartidos.
La validación del entorno cloud sigue pendiente.
