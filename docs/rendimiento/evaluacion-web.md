# Evaluación web: Lighthouse y PageSpeed Insights

## Versión base

- Tag publicado: `v1.0.0`.
- Commit: `8d0fd85a630ac0633c692c397d5dd2342d73460c`.
- Rama de trabajo: `perf/rendimiento-web`.
- Despliegue esperado: `https://clinicainternacional-production.up.railway.app`.
- Confirmación del commit desplegado: el responsable confirmó el despliegue del PR #77 en Railway antes de iniciar las mediciones.

El después se identificará con una versión posterior aprobada una vez aplicadas y comprobadas las mejoras. Se conservan los tags de versiones `v0.1.0-alpha` y `v1.0.0`, sin tags auxiliares por prueba. Los scripts de medición no modifican el código servido de la versión base.

## Diseño de la prueba

1. Comprobar que el commit esperado está desplegado y que Java/MySQL responden. Registrar fecha y límites de CPU/RAM, sin modificar el plan durante la comparación.
2. Guardar el primer acceso después de inactividad como observación separada de arranque en frío.
3. Ejecutar tres auditorías de navegación por página y dispositivo. Una sola auditoría a la vez; no ejecutar Postman de carga simultáneamente.
4. Mantener el mismo equipo, navegador, versión de Lighthouse, perfiles y conjunto de datos para antes y después. Lighthouse simula condiciones móviles o de escritorio; no es una prueba de concurrencia.
5. Guardar HTML y JSON originales, métricas de cada ejecución y la mediana de las tres. Mantener iguales las opciones de caché y almacenamiento en ambas fases.
6. Seleccionar mejoras de las auditorías reales, implementarlas en la rama, ejecutar pruebas funcionales, desplegar y repetir el mismo procedimiento.

Las puntuaciones de PageSpeed y Lighthouse local se presentan en tablas separadas: cambian el equipo y el lugar de ejecución. PageSpeed ya utiliza Lighthouse para su análisis de laboratorio; no son dos experimentos totalmente independientes. Los datos de campo CrUX, cuando existan, se distinguen de los datos de laboratorio. La ausencia de datos de campo se registra como «sin datos suficientes», no como cero.

## Páginas y acceso

El inventario inicial está en `scripts/rendimiento/web/paginas.json`. Las dos pantallas de login se evalúan con PageSpeed móvil/escritorio y Lighthouse. Los portales se evalúan con Lighthouse autenticado. Registrar cada URL final: una redirección al login o una página de error invalida la medición de la vista privada.

No incluir contraseñas o tokens en URLs, ni hacer públicas las rutas privadas. La autenticación se prepara en un perfil temporal de Chrome. No ejecutar botones de guardar, enviar correo, consultar DNI o analizar síntomas durante una auditoría de navegación.

Para paciente y médico se necesitan cuentas de prueba con acceso válido. Sus credenciales se proporcionan mediante variables de entorno, nunca en archivos versionados. Los reportes completos pueden contener capturas y fragmentos del DOM: se guardan en `target/auditoria-web/reportes`, fuera de Git. Antes de incorporarlos al informe, comprobar que solo muestran datos simulados y ocultar información que identifique cuentas personales.

## Ejecución reproducible

Instalación temporal de herramientas (no cambia las dependencias Java):

```powershell
npm install --prefix target/auditoria-web --no-audit --no-fund lighthouse@13.6.0 puppeteer-core@25.16.0 chrome-launcher@1.2.2
$env:AUDIT_COMMIT_DESPLEGADO='8d0fd85a630ac0633c692c397d5dd2342d73460c'
$env:AUDIT_FASE='antes'
$env:AUDIT_PAGINA='login-paciente'
$env:AUDIT_DISPOSITIVO='mobile'
$env:AUDIT_REPETICIONES='3'
node scripts/rendimiento/web/lighthouse.mjs
node scripts/rendimiento/web/pagespeed.mjs
```

Definir `AUDIT_COMMIT_DESPLEGADO` solo después de comprobar ese commit en Railway. Cambiar el dispositivo a `desktop` y repetir para el otro perfil. Cada ejecución guarda su propia carpeta con fecha, sin sobrescribir reportes anteriores.

Para una ruta privada, seleccionar su ID del inventario y proporcionar `AUDIT_USUARIO` y `AUDIT_PASSWORD` en el entorno privado de la terminal; ejecutar únicamente el script Lighthouse. No pegar esos valores en el informe. El script abre un perfil separado, inicia sesión y comprueba la ruta final. No reutiliza las cookies del navegador habitual. Un fallo de acceso no se cuenta como puntuación válida de la página.

Si PageSpeed devuelve HTTP 429 por cuota, el script conserva el error y no insiste automáticamente. Puede obtenerse el reporte desde https://pagespeed.web.dev/ y guardarlo junto con la evidencia, registrando que la ejecución fue manual. No reutilizar claves de otros proveedores para habilitar la API.

## Indicadores medidos

| Indicador | Unidad | Interpretación |
| --- | --- | --- |
| Rendimiento | 0–100 | Puntuación calculada por Lighthouse |
| Accesibilidad | 0–100 | Comprobaciones automáticas; no sustituye revisión manual |
| Buenas prácticas | 0–100 | Comprobaciones de la herramienta; no certifica seguridad |
| SEO | 0–100 | Registrar contexto público/privado, sin abrir portales a indexación |
| FCP | ms | Primera aparición de contenido |
| LCP | ms | Aparición del mayor elemento de contenido |
| TBT | ms | Bloqueo total del hilo principal en la carga |
| CLS | sin unidad | Inestabilidad visual durante la carga |
| Speed Index | ms | Progreso visual de la carga |

No presentar TBT como INP ni resultados de una carga de página como capacidad de usuarios simultáneos. Si Lighthouse devuelve errores o métricas no disponibles, conservar el error y no asignar puntuaciones inventadas.

## Evidencias del informe

Por cada caso: captura de puntuaciones, tabla de las tres corridas y su mediana, captura de las oportunidades relevantes, commit/tag, fecha y configuración. Después: mismo conjunto de evidencias, descripción del cambio aplicado y tabla comparativa. Calcular la diferencia de puntuación en puntos y la reducción de tiempos en porcentaje; no decir «porcentaje de mejora» cuando se comparan puntos.

Fuentes oficiales:
- https://developers.google.com/speed/docs/insights/v5/about
- https://github.com/GoogleChrome/lighthouse/blob/main/docs/authenticated-pages.md
- https://developer.chrome.com/docs/devtools/lighthouse
