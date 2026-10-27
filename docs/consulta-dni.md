# Consulta opcional de DNI en el registro

El formulario del paciente permite consultar nombres y apellidos por DNI a través
del servidor Java. El navegador nunca recibe la clave ni se conecta a Decolecta.
No se consultan datos al escribir: se requiere pulsar **Consultar DNI**.
Carné de Extranjería y registro manual continúan disponibles.

## Configuración privada

1. Revocar cualquier clave que se haya compartido y generar una nueva en Decolecta.
2. Añadir las entradas de `config/decolecta.env.example` al `.env` privado existente,
   sin reemplazar sus contraseñas de MySQL. No versionar ese archivo.
3. Establecer `DECOLECTA_API_KEY` y ajustar `DNI_DAILY_LIMIT` a la cuota contratada.
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
No se envía `Authorization` desde el navegador: el Bearer es exclusivo de Java → Decolecta.

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

Decolecta solo documenta 200 y un 400 genérico en el material revisado. No interpretamos
ese 400 como «persona inexistente»: lo tratamos como respuesta externa fallida (502).
El mapeo defensivo de otros estados no supone que el proveedor los tenga documentados.
Esta entrega define el contrato de este endpoint y añade `data: null` al error CSRF
global; no migra todavía los contratos de los demás endpoints de la aplicación.

## Seguridad, cuota y límites

- Destino fijo `https://api.decolecta.com/v1/reniec/dni`, sin redirecciones;
  certificado HTTPS verificado por Java. Conexión: 3 s; petición: 6 s.
- Respuesta externa limitada a 16 KiB durante la recepción. Validación de tipos,
  longitud de nombres y coincidencia del DNI. Sin caché de datos personales.
- CSRF por sesión también antes del proveedor; límite de 5 solicitudes por sesión
  y por IP cada 15 minutos, 30 por hora por instancia y presupuesto diario configurable.
  Solicitudes con DNI inválido no consumen esos contadores ni llaman a Decolecta.
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
  Los nombres siguen editables y la cuenta se crea por el flujo de registro existente.
  Fecha de nacimiento, dirección, correo y teléfono no vienen de esta consulta.

## Verificación sin cuota externa

```powershell
mvn clean verify
node --test tests/consulta-dni.test.cjs
```

Las pruebas Java inyectan un transporte simulado; no requieren clave ni conexión
a Decolecta. Cubren proveedor, contrato, CSRF, validación, fallos y límites concurrentes.
Las pruebas JavaScript cubren autocompletado, registro manual, cambio de documento,
respuesta atrasada y conservación de ediciones durante la consulta.

Para previsualizar el formulario sin sustituir el servicio existente en 8080:

```powershell
mvn -Djetty.http.port=8082 jetty:run
```

Abrir `http://localhost:8082/login/paciente?action=register`. Jetty queda enlazado
a `127.0.0.1`. Esta previsualización y las pruebas del endpoint no necesitan conexión
a MySQL; los demás flujos funcionales sí requieren configurar su base de datos.

Importar `docs/postman/Consulta-DNI.postman_collection.json` para verificar el contrato
con un servidor local. La colección captura sesión/CSRF y solo hace consultas inválidas
o rechazadas **antes del proveedor**. La petición real está desactivada salvo que se
complete expresamente la variable `dniConsultaAutorizada` con un DNI propio/autorizado.
Nunca introducir la clave del proveedor en Postman: las pruebas apuntan a nuestro servidor.

Fuente del contrato externo: https://decolecta.gitbook.io/docs/servicios/integrations-2

### Resultado local del 27 de octubre de 2026

- `mvn verify`: 55 pruebas Java, sin fallos, errores ni omisiones; WAR generado.
- `node --test tests/consulta-dni.test.cjs`: 5 pruebas, todas correctas.
- Postman CLI contra Jetty local: 7 solicitudes, 14 comprobaciones correctas;
  petición real omitida. Reporte local no versionado: `target/postman-dni.json`.
- Formulario servido con HTTP 200 y botón de consulta; sin clave configurada,
  una consulta sintética válida devuelve HTTP 503 con el contrato definido.

La respuesta exitosa del proveedor se verificó con transporte simulado. Queda
pendiente comprobar una consulta real con una clave nueva y un DNI autorizado;
estos resultados no se presentan como validación del proveedor ni del entorno cloud.
