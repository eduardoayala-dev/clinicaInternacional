# API de gestión de citas

Todos los endpoints responden con JSON: `code`, `message` y `data`. La API de citas requiere iniciar sesión como paciente mediante el portal. En los POST, conserva la cookie de sesión y envía el token vigente en `X-CSRF-Token`. Los parámetros del POST se envían como `application/x-www-form-urlencoded`.

| Método y ruta | Parámetros | Éxito |
| --- | --- | --- |
| GET /api/medicos | idEspecialidad, idSede opcional | 200 y lista de médicos |
| GET /api/horarios | idMedico, fecha opcional | 200 y lista de fechas u horas |
| GET /api/v1/citas | — | 200 y próximas citas del paciente autenticado |
| POST /api/v1/citas/retenciones | idMedico, fecha, hora | 201 y el identificador de retención |
| POST /api/v1/citas | idMedico, idMotivo, fecha, hora, retencion | 201 e idCita |
| POST /api/v1/citas/{id}/reprogramacion | idMedico, idMotivo, fecha, hora | 200 e idCita de la nueva cita |
| POST /api/v1/citas/{id}/cancelacion | motivo | 200 e idCita |

Fechas en formato `YYYY-MM-DD`, horas en `HH:mm` (Lima). La identidad del paciente se obtiene de la sesión. Un error conserva la misma estructura JSON. Los filtros de sesión y CSRF se aplican antes de las operaciones de escritura.

- [Especificación OpenAPI](openapi-citas.json)
- [Contrato de catálogos](contratos-catalogos.md)
- [Colección Postman de citas](postman/Gestion-Citas-API.postman_collection.json)

## Comparación de carga

1. Valida el flujo una vez en local con datos sintéticos. Importa la colección, inicia sesión como paciente en Postman y usa el token vigente del formulario autenticado. Completa los IDs y horarios disponibles.
2. Para carga, usa un entorno aislado. Configura el grupo de solo lectura con sesiones de prueba y horarios publicados. Si mides escrituras, asigna un paciente y cupos válidos por usuario virtual y restaura la base entre ejecuciones.
3. Tras integrar la API, etiqueta el commit medido como `antes-carga-api-citas`. Guarda perfil de usuarios, duración, datos, versión, p50/p95/p99, solicitudes por segundo y errores por ruta.
4. Corrige el cuello de botella observado en `perf/mejoras-despues-de-pruebas-de-carga`. Repite el mismo perfil y documenta la comparación antes de etiquetar el resultado. Consulta el [plan de rendimiento](rendimiento/plan.md) para los criterios de aceptación.
