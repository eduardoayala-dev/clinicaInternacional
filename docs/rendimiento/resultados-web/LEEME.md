# Evidencias antes y después de las mejoras web

Todas las métricas Lighthouse de esta carpeta corresponden a la URL en línea de Railway. La expresión «Lighthouse local» en el registro identifica dónde corre el auditor, no dónde está alojada la aplicación.

Referencias: versión histórica `v1.0.0` (`8d0fd85`); responsive integrado en `e3320b4`; mejoras web del PR 79 desplegadas en `042fd0c`. La rama `perf/rendimiento-web` conserva las mejoras y `docs/despues-mejoras-rendimiento-web` sus evidencias. Se conservan únicamente los tags de versiones solicitados.

## Resultados destacados

| Lighthouse | Perfil | Antes | Después |
| --- | --- | --- | --- |
| Login paciente | Escritorio | 97 | 100 |
| Login personal | Escritorio | 98 | 100 |
| Inicio admisión | Escritorio | 76 | 86 |
| Login paciente | Móvil | 92 | 98 |
| Login personal | Móvil | 72 | 99 |
| Inicio paciente | Móvil | 99 | 100 |

Son medianas de tres corridas válidas; los valores son puntuaciones de rendimiento, no porcentajes de capacidad. Las demás métricas y casos se conservan en JSON/CSV. El CLS del inicio de admisión pasó de 0,844 a 0,274; mejora pero sigue siendo una limitación. No se afirma que todo el sistema esté optimizado.

## PageSpeed Insights

Reportes manuales de la misma URL `/login`, el 4 de noviembre de 2026, 15:01 antes y 16:06 después, hora de Perú:

| Perfil | Rendimiento antes/después | LCP antes/después | TBT antes/después | Speed Index antes/después |
| --- | --- | --- | --- | --- |
| Móvil | 95 / 97 | 2,3 / 1,6 s | 0 / 60 ms | 2,8 / 3,9 s |
| Escritorio | 99 / 100 | 0,7 / 0,4 s | 0 / 0 ms | 0,7 / 0,5 s |

Accesibilidad y buenas prácticas permanecieron en 100; SEO pasó de 91 a 100. No hay datos de campo suficientes. Son corridas puntuales, no medianas. La API devolvió 429 y no se usó ese error como puntuación.

## Capturas y alcance

El paquete local de evidencias contiene reportes HTML/JSON, capturas de Lighthouse antes/después, vistas internas conservadas por las auditorías iniciales y capturas actuales de alta resolución de los cuatro roles. Las vistas originales extraídas de Lighthouse tienen 500 px de ancho: se identifican como capturas finales de carga y no se presentan como capturas de alta resolución tomadas antes.

Las series incompletas incluyen usuarios del administrador antes (dos corridas) e indicadores de admisión después (dos corridas), entre otros casos móviles. No se calcula una mediana de tres donde faltan ejecuciones. Los errores HTTP 503 quedan conservados; estas pruebas no certifican disponibilidad continua ni sustituyen estrés o pruebas con usuarios.
