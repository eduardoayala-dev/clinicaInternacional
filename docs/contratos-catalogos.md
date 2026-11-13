# Contratos de catálogos médicos

Los catálogos usan JSON con tres campos: `code`, `message` y `data`. La consulta de DNI y los errores CSRF comparten esta estructura; cada operación conserva su propio tipo de datos.

| Solicitud GET | Parámetros | Contenido de `data` |
|---|---|---|
| `/api/medicos` | `idEspecialidad` obligatorio; `idSede` opcional | Médicos con `idMedico`, `nombreCompleto` y `cmp` |
| `/api/horarios` | `idMedico` obligatorio | Fechas `YYYY-MM-DD` |
| `/api/horarios` | `idMedico` y `fecha=YYYY-MM-DD` | Horas `HH:mm` |

Los identificadores deben ser enteros positivos. Para consultar todas las sedes se omite `idSede`; enviar `0` es inválido. Las fechas deben existir en el calendario.

```json
{"code":"HORARIOS_OBTENIDOS","message":"Consulta de horarios completada.","data":["09:00","09:30"]}
```

| Estado HTTP | Significado |
|---|---|
| 200 | Consulta correcta; `data: []` significa que no hay resultados |
| 400 | Parámetro inválido; `data: null` |
| 403 | Falta el token CSRF o no es válido en una solicitud de escritura |
| 405 | Método distinto de GET; cabecera `Allow: GET` |
| 503 | No se puede consultar la base de datos |
| 500 | Error interno inesperado |

Las respuestas de error no incluyen SQL, credenciales ni detalles internos. El filtro CSRF se ejecuta antes del servlet: un POST sin token devuelve 403, mientras que un POST con token válido recibe 405.

Solo se muestran médicos activos, de especialidades y sedes activas, con usuario activo. El filtro por sede se respeta aunque no encuentre médicos. Los horarios proceden de agendas publicadas de la sede del médico: se descartan fechas pasadas, horas ocupadas y bloques que no permiten una consulta completa. Se usa la zona horaria de Lima. No se generan médicos, consultorios, cupos ni horarios de reemplazo en las pantallas.

Reserva y reprogramación consumen `data`; las respuestas antiguas que eran arreglos directamente ya no son válidas. Otros consumidores deben actualizarse también.

## Pruebas locales

Importar [la colección de catálogos](postman/Catalogos-API.postman_collection.json) en Postman. Su variable `baseUrl` apunta a `http://localhost:8080`. Ejecutar la colección en orden para obtener primero la sesión y el token CSRF. No necesita claves de proveedores ni hace consultas de DNI.

En la aplicación, probar reserva y reprogramación cambiando sede, especialidad, médico, fecha y turno. Una agenda sin horarios debe mostrar un mensaje vacío; un error de consulta debe mostrar el error. El contador del día seleccionado refleja los horarios recibidos.

Esta modificación cubre la lectura de catálogos. La revisión de contratos de escritura, la validación transaccional de reservas simultáneas y el despliegue cloud requieren trabajo posterior.
