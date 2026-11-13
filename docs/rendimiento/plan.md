# Plan de rendimiento de la clínica

## Objetivo y alcance

Medir el rendimiento del escenario **acceso y consulta médica**, identificar su degradación al subir la carga y comprobar la recuperación tras un pico. No atribuir a toda la aplicación una capacidad obtenida para este único escenario. Reservas simultáneas, atención clínica con escrituras y experiencia visual requieren escenarios adicionales.

La colección ejecuta ocho peticiones por iteración: abrir login, autenticar con BCrypt real, abrir Inicio y recuperar el nuevo CSRF, consultar agenda con datos, listar pacientes, consultar su historial, listar seguimientos y cerrar sesión. Cada iteración crea su propia sesión; no reutiliza el token de otra iteración ni modifica consultas clínicas. Hay una pausa de 500 ms entre vueltas.

Los tiempos incluyen el login. Comparar también cada petición por separado para distinguir el coste de autenticar del coste de consultar. Las pruebas HTTP no ejecutan JavaScript del navegador ni miden el renderizado de la interfaz.

## Entorno independiente

- Rama `feature/pruebas-rendimiento`.
- Aplicación de pruebas: `http://127.0.0.1:8083`; aplicación habitual: puerto 8080.
- Proyecto Docker `clinica-rendimiento`, red y volumen propios. No copia datos ni contraseñas de la base habitual.
- App: máximo 1 CPU, 512 MiB, heap Java de 256 MiB. MySQL: máximo 1 CPU, 768 MiB y buffer InnoDB de 128 MiB. El pool JDBC de la aplicación conserva sus cinco conexiones originales.
- Inicialización mediante el esquema del repositorio, exclusivamente en el volumen nuevo de pruebas. El preparador no ejecuta ese esquema sobre la clínica habitual.
- Datos predeterminados: 50 médicos sintéticos, 30 pacientes/consultas por médico, 1500 consultas cerradas, registros clínicos y recomendaciones. Las consultas corresponden al día anterior a la primera preparación para no crear cierres futuros.
- Credenciales de BD generadas en `.env.rendimiento`, ignorado por Git. CSV y resultados en `target/rendimiento`, también ignorado.
- Sin token de proveedor de DNI y sin llamadas a sus endpoints.

Las credenciales sintéticas de un médico pueden aparecer en más de un usuario virtual si Postman reutiliza filas del CSV. Cada iteración conserva la coherencia entre sus credenciales y sus datos y obtiene una sesión independiente. Si se requiere una identidad fija por VU durante toda la ejecución, usar un dataset de Postman con distribución Fixed. No confundir usuarios virtuales con personas únicas conectadas.

## Preparación

Requisitos: Docker Desktop iniciado, Python 3.9 o superior y Postman de escritorio; para automatizar, Postman CLI con el comando `performance run`.

Desde la raíz del proyecto:

```powershell
python scripts/rendimiento/preparar.py
```

La primera inicialización y el calentamiento pueden tardar varios minutos. No se incluyen dentro de la medición de carga. Volver a ejecutar conserva los datos si coincide el tamaño; no elimina ni reinicializa el volumen. Para otro tamaño, revisar primero el laboratorio existente.

Archivos generados:

- `target/rendimiento/usuarios.csv`: datos de entrada para Postman.
- `target/rendimiento/entorno.json`: imagen, commit, fecha y volumen de datos y recursos configurados.
- `target/rendimiento/preparacion.log`: arranque del laboratorio.

## Ejecución desde Postman de escritorio

1. Importar `docs/postman/Rendimiento-Medico-Local.postman_collection.json`.
2. Abrir Run y hacer primero una ejecución **Functional**, con `usuarios.csv` y una sola iteración. Todas las aserciones deben pasar. No seguir redirecciones automáticamente; la colección ya lo indica en cada petición.
3. Abrir Run de nuevo, seleccionar **Performance** y método **App**, para generar la carga desde la computadora local.
4. Seleccionar el CSV y el perfil de la tabla siguiente. Verificar que las ocho peticiones estén seleccionadas, en orden.
5. Guardar capturas y resultados por petición y por ejecución, no solo la gráfica general.

No ejecutar directamente la colección de flujo clínico para estas pruebas: sus casos de una sola ejecución cambian estados y no sirven como bucle de carga sostenida.

## Orden propuesto

| Paso | Usuarios virtuales | Duración | Propósito |
| --- | --- | --- | --- |
| Verificación funcional | 1 iteración | Lo que dure el flujo | Confirmar cuentas, cookies, CSRF y datos. No mide capacidad. |
| Humo | 1 | 1 min | Confirmar el generador y la instrumentación. |
| Carga inicial | 5 | 5 min | Primera referencia de concurrencia. |
| Carga intermedia | 10 | 5 min | Comparar latencia, throughput y recursos. |
| Carga mayor | 20 | 5 min | Identificar degradación respecto de la referencia. |
| Pico | 5 → 20 → 5 | 5 min | Subida en 10 s, 30 s en el pico y vuelta en 10 s; comprobar recuperación. |
| Estrés exploratorio | 5 → 50 | 10 min | Aumentar gradualmente más allá de la carga esperada, después de revisar los pasos anteriores. |
| Duración inicial | 10 | 10 min | Primera observación de estabilidad. No sustituye una prueba de horas. |

Los números son **configuraciones propuestas**, no resultados ni una promesa de capacidad. No escalar automáticamente si el paso anterior ya falla. Para estimar un límite sostenible, repetir cerca del punto de degradación con niveles intermedios y verificar que el generador no esté saturado.

## Ejecución automatizada local

```powershell
python scripts/rendimiento/ejecutar.py --perfil verificacion --postman postman
python scripts/rendimiento/ejecutar.py --perfil humo --postman postman
python scripts/rendimiento/ejecutar.py --perfil carga-5 --postman postman
```

Se puede pasar una ruta de Postman CLI con `--postman`. El entorno local de Codex puede guardar esa ruta privada en `target/rendimiento/herramientas.json`; no forma parte del repositorio. `--solo-comando` muestra la configuración sin enviar carga. Los demás perfiles están en `docs/rendimiento/perfiles.json`.

Cada ejecución genera una carpeta con configuración, salida de Postman, código de salida, duración y `recursos.csv` con muestras de CPU, memoria, red y disco de app y MySQL cada cinco segundos. La primera muestra y la última pueden no coincidir exactamente con los límites de la ventana de carga. Revisar si hay filas de monitoreo no disponible.

Las credenciales, cookies y respuestas completas quedan en archivos locales ignorados. Compartir con el profesor el informe agregado y las gráficas, no los archivos con sesiones.

## Criterios de evaluación propuestos

Antes de medir capacidad, acordar los objetivos con el profesor. Como referencia inicial del laboratorio: p95 global menor de 2000 ms y menos del 1 % de errores runtime de Postman. **Todas las aserciones funcionales también deben pasar**: los umbrales generales no sustituyen comprobar autenticación y contenido.

Registrar por petición y por nivel: p50/p95/p99, peticiones por segundo, timeouts, HTTP inesperados y aserciones fallidas. Distinguir errores de infraestructura, sesión y datos de prueba. Una respuesta 200 que en realidad sea el formulario de login no es un éxito; tampoco una lista vacía cuando el escenario exige datos.

Guardar CPU y memoria de ambos servicios, conexiones de BD si se amplía la instrumentación, recursos del generador y recuperación después del pico. Repetir las corridas relevantes al menos tres veces en condiciones semejantes. Detener la escalada ante errores funcionales, timeouts sostenidos, reinicios por memoria o saturación del generador.

Postman y Docker comparten máquina; documentarlo. Una medición local no determina la capacidad de Railway. Para Railway se necesita otra campaña con sus recursos reales y separación entre arranque tras hibernación y servicio ya caliente. Esta colección está limitada al laboratorio 8083 para no enviar allí carga accidental.

## Entrega al profesor

Completar `docs/rendimiento/resultados.csv` con resultados medidos. Adjuntar configuración del entorno, versión de la aplicación, colección y perfiles, fechas y datos usados, resultados por corrida, gráficas de latencia/throughput/CPU/memoria y conclusión del límite observado del escenario.

Una conclusión válida sería: «En este entorno, con este volumen de datos y este escenario, la carga X cumplió los objetivos durante Y minutos en tres corridas; el nivel Z los incumplió por estas razones». No afirmar un máximo universal ni estimar usuarios por el número de peticiones totales.

Para detener solo el laboratorio conservando su volumen:

```powershell
docker compose --project-name clinica-rendimiento --env-file .env.rendimiento -f compose.rendimiento.yml stop
```

Referencias: [configuración de rendimiento en Postman](https://learning.postman.com/docs/tests-and-scripts/performance-testing/performance-test-configuration/), [comando de rendimiento del CLI](https://learning.postman.com/docs/postman-cli/postman-cli-monitoring), [datos por usuario virtual](https://learning.postman.com/docs/tests-and-scripts/performance-testing/test-data/overview).

## Verificación de publicación

El 3 de noviembre de 2026 se verificaron la sintaxis de los cuatro scripts con Python 3.12 y la carga de las tres interfaces de comandos mediante --help en un contenedor desechable. La prueba de la colección que restringe las peticiones al laboratorio local pasó con Node. No se ejecutó carga en esta comprobación y resultados.csv conserva únicamente el encabezado; no representa mediciones.
