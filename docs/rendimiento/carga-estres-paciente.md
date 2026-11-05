# Pruebas de carga y estrés: búsqueda de disponibilidad del paciente

Rama: `perf/carga-hu15`. Herramienta: **Postman (Performance, método App)**. Entorno: laboratorio local aislado `127.0.0.1:8083` (`compose.rendimiento.yml`). No se envía carga a Railway.

## 1. Flujo evaluado

Un paciente abre el login, inicia sesión con DNI (BCrypt real), abre su inicio y consulta la **disponibilidad semanal** de una especialidad (`GET /api/horarios?vista=semana`), que es el paso de búsqueda de la reserva (HU009). Al final cierra sesión y "piensa" 4,5 s antes de volver a empezar.

Colección: `docs/postman/Rendimiento-Paciente-Local.postman_collection.json` (5 peticiones). Cada respuesta valida estado y contenido; la petición 04 además verifica la meta RNF‑12 (≤ 2 s).

Datos: 50 médicos sintéticos de la misma especialidad y sede, con agenda los 7 días siguientes (08:00‑12:00 y 14:00‑18:00, cupos de 20 min) y 3 citas confirmadas por turno: **700 agendas médico‑turno y 8 400 cupos por semana**. Se generan con `scripts/rendimiento/preparar_paciente.py`.

## 2. Antes y después

| Versión | Commit | Cómo cuenta los cupos de la semana |
|---|---|---|
| Antes | `be0f568` | Una consulta de agendas y luego **3 consultas por cada médico y día** (≈ 1 050 consultas con 50 médicos × 7 días) |
| Después | `ef52581` (`perf/disponibilidad-semanal`) | **5 consultas en total**: agendas, y luego citas, ofertas y retenciones agrupadas en memoria |

Entre ambos commits solo cambian `MedicoDAO.java` y su prueba, por lo que la diferencia en la petición 04 se atribuye a esa mejora (eliminación del patrón de consultas N+1). Mismos datos, misma BD y mismos límites: app 1 CPU / 512 MiB, MySQL 1 CPU / 768 MiB, pool de 5 conexiones.

Cambiar de versión: `python scripts/rendimiento/cambiar_version.py antes` (o `despues`).

## 3. Sustento del número de usuarios (Ley de Little)

N = X × (R + Z)

- **N**: usuarios concurrentes (usuarios virtuales).
- **X**: throughput, iteraciones del flujo por segundo.
- **R**: tiempo de respuesta del flujo.
- **Z**: tiempo de pensar entre iteraciones (4,5 s en la colección).

RNF‑13 fija **50 usuarios concurrentes ≈ 10 peticiones/s**. Con R ≈ 0,5 s y Z = 4,5 s: N = 10 × (0,5 + 4,5) = **50 usuarios virtuales**. Ese es el nivel de **carga nominal**. Para **estrés** se duplica la carga nominal (100 usuarios) con subida gradual para observar dónde aparece la degradación. Si R crece, la misma N produce menos X: con la ley se explica por qué cae el throughput cuando el sistema se satura.

> La cifra de 10 peticiones/s viene del RNF‑13 del equipo, no de un registro real de tráfico de la clínica.

## 4. Escenarios

| # | Versión | Tipo en Postman | Usuarios | Duración | Propósito |
|---|---|---|---|---|---|
| 0 | Antes | Run funcional, 1 iteración | 1 | — | Confirmar que todas las aserciones pasan |
| 1 | Antes | Fixed | 50 | 3 min | Carga nominal, línea base |
| 2 | Después | Fixed | 50 | 3 min | Carga nominal tras la mejora |
| 3 | Después | Ramp up 10 → 100 | 100 | 5 min | Estrés: buscar el punto de degradación |

Métricas por escenario (de la petición 04 y del total): tiempo promedio, p90/p95/p99, máximo, peticiones por segundo y % de errores. Criterios: p95 ≤ 2 000 ms (RNF‑12) y errores < 1 %.

## 5. Resultados

Ejecución del 4 de noviembre de 2026 (18:00-18:30, hora de Perú) en la laptop de un integrante con Docker Desktop (WSL 2) y Postman Performance (método App). Reportes PDF y capturas en `docs/rendimiento/evidencias/carga-paciente/`.

### 5.1 Un usuario (Run funcional)

| Versión | Búsqueda (04) | Aserciones |
|---|---|---|
| Antes | 2 205 ms | 11/12 (falla la meta ≤ 2 s) |
| Después | 710 ms (primer acceso tras arrancar) | 12/12 |

### 5.2 Carga nominal y estrés

| Escenario | Usuarios | Req/s | Prom. total (ms) | p95 total (ms) | p95 búsqueda 04 (ms) | Errores HTTP % | Aserciones fallidas % | ¿Búsqueda ≤ 2 s? |
|---|---|---|---|---|---|---|---|---|
| 1 Antes | 50 | 7,18 | 5 424 | 17 337 | 11 568 | 10,85 | 32,46 | No |
| 2 Después | 50 | 28,38 | 810 | 3 963 | 1 371 | 0,00 | 0,16 | Sí |
| 3 Estrés después | 10→100 | 29,24 | 1 262 | 7 048 | 2 453 | 0,00 | 1,43 | No (7,30 % de aserciones fallidas) |

p95 por petición (ms):

| Petición | Antes 50 | Después 50 | Estrés 100 |
|---|---|---|---|
| 01 Abrir login | 459 | 466 | 1 689 |
| 02 Autenticar (BCrypt) | 15 966 | 5 962 | 10 037 |
| 03 Abrir inicio | 27 139 | 3 026 | 5 511 |
| 04 Disponibilidad semanal | 11 568 | 1 371 | 2 453 |
| 05 Cerrar sesión | 38 | 169 | 167 |

### 5.3 Interpretación

- Con 50 usuarios, la mejora reduce el p95 de la búsqueda un 88 % (11 568 → 1 371 ms), cuadruplica el throughput (7,18 → 28,38 req/s) y elimina los errores HTTP (10,85 → 0 %). La búsqueda pasa a cumplir RNF‑12.
- Antes, el login y el inicio también eran lentos; la explicación probable (no medida con monitoreo del pool) es que cada búsqueda retenía una de las 5 conexiones JDBC durante segundos y las demás peticiones esperaban. Después, el p95 total (3 963 ms) lo determinan el login (BCrypt, costoso por diseño de seguridad) y el inicio, con 1 CPU para la app: el cuello de botella se desplaza a la autenticación.
- En el estrés, el throughput se estanca en ≈ 29 req/s, igual que con 50 usuarios: esa es la capacidad de este entorno. Según la Ley de Little, si N se duplica y X no crece, R aumenta; el p95 total pasa de 3,9 s a 7,0 s y el 7,3 % de las peticiones de búsqueda falla alguna aserción (principalmente la meta de 2 s). El punto de degradación está entre 50 y 100 usuarios, sin caídas (0 % de errores HTTP).
- Postman marca "Fail" en las corridas 2 y 3 porque su criterio usa el p95 de todas las peticiones juntas, no solo el de la búsqueda.

## 6. Límites

Postman y Docker comparten la misma laptop: la memoria llegó al 95,3 % (antes), 86,0 % (después) y 97,7 % (estrés), y Postman advirtió que la máquina limitaba la carga. Una sola corrida por escenario. El resultado vale para este entorno, estos datos y este flujo, no para Railway ni para toda la aplicación. Las pruebas HTTP no miden el renderizado en el navegador (eso es Lighthouse/PageSpeed).
