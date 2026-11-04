# Mejoras de rendimiento web

Referencia histórica: `v1.0.0`, commit `8d0fd85a630ac0633c692c397d5dd2342d73460c`. El responsive se integró posteriormente en `e3320b4776383cd846bf5689736b6fa680d8e6b6`; las series posteriores a ese despliegue registran ese SHA. La rama del después es `perf/rendimiento-web`, sin modificar el tag original.

## Cambios derivados de las auditorías

| Hallazgo inicial | Cambio |
| --- | --- |
| Peticiones externas bloqueantes de fuentes e iconos | Fuentes WOFF2 y lucide 1.68.0 servidos por la propia aplicación, con licencias conservadas |
| Peso del recurso JavaScript al alojarlo localmente | Maven genera su versión gzip y la hoja de fuentes comprimida en cada empaquetado; Tomcat negocia la versión precomprimida |
| Salto de diseño del inicio de admisión al paginar después del primer pintado | La primera respuesta ya muestra únicamente las cuatro filas iniciales; el filtrado y la navegación siguen trabajando con todas las filas |
| Falta de descripción de las páginas públicas | Metadescripciones en los dos accesos |
| Imágenes sin dimensiones declaradas | Dimensiones explícitas del logotipo y del icono de contraseña |
| Contraste insuficiente en admisión | Ajuste de textos secundarios, indicadores y etiquetas conservando la paleta |
| Select sin nombre y login de personal sin región principal | Etiqueta accesible del filtro de estado y región principal del formulario |

## Verificación anterior al despliegue

Las vistas se compilan con Jasper. La regresión JavaScript completa ejecuta 95 pruebas sin fallos. La comprobación HTTP local de integración confirma `Content-Encoding: gzip` y 114 233 bytes para lucide, frente a 478 389 bytes sin comprimir. Este dato verifica empaquetado y negociación; no se presenta como una métrica de Railway.

Las métricas comparativas de Lighthouse y PageSpeed se obtienen exclusivamente de la URL desplegada en Railway. Los reportes se conservan en `target/auditoria-web/reportes/antes` y `despues`; las series incompletas por errores HTTP no reciben una mediana y quedan documentadas como incidentes.
