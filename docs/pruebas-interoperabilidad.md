# Pruebas de interoperabilidad (ISO/IEC 25010 – Compatibilidad)

Suite que valida la integración del sistema con sus servicios externos: **interoperabilidad** (intercambio y uso correcto de datos) y **coexistencia** (uso de recursos compartidos sin perjudicar a otros sistemas).

## Sistemas externos

| Sistema | Uso | Cliente |
|---|---|---|
| Google Gemini API | Orientación de especialidad | `ia/GeminiCatalogo` |
| Gmail API (OAuth 2.0) | Correos de citas | `correo/GmailCorreo` |
| apisperu | Consulta de DNI en el registro | `service/dni/ApiPeruClient` |
| MySQL en Railway | Persistencia (TLS) | `config/DatabaseSettings`, `dao/` |

## Suite JUnit 5 (respuestas simuladas, sin consumir cuota)

```
mvn test -Dtest="GeminiCatalogoTest,GmailCorreoTest,ApiPeruClientTest,ConsultaDniServletTest,PacienteOrientacionServletTest,DniRateLimiterTest,DisponibilidadSistemaTest,DatabaseSettingsTest,ApiJsonTest" -Dsurefire.failIfNoSpecifiedTests=false
```

En NetBeans: clic derecho sobre cada clase en *Test Packages* → **Test File**.

## Colecciones Postman (sistema desplegado)

| Colección | Sistema externo | Llamada real |
|---|---|---|
| `Consulta-DNI` | apisperu | Solo la 08, si se configura `dniConsultaAutorizada` |
| `Orientacion-IA` | Gemini | Solo la 09 (caso ficticio, consume cuota) |

Uso: **Import** → en *Variables* de la colección, `baseUrl` = URL del entorno → **Run** (cookies activas, sin detener ante errores).
En `Orientacion-IA`, `pacienteDni` y `pacienteClave` se configuran solo en Postman: **no guardar credenciales en el repositorio**.

Verificación rápida de la base de datos en la nube: `GET {{baseUrl}}/health/ready` → `SISTEMA_DISPONIBLE`.

## Fuera de alcance

- Gmail no se prueba en vivo desde Postman porque enviaría correos reales.
- Estas pruebas no miden capacidad bajo carga (ver `docs/rendimiento`).
