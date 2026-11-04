# Revisión de consultas de los portales

Fecha: 4 de noviembre de 2026. Revisión estática de bucles en DAO y servlets; no sustituye un perfil completo bajo carga.

| Caso | Hallazgo | Acción |
| --- | --- | --- |
| Inicio de admisión | Una consulta inicial más tres por médico/día | Corregido en PR #76: cuatro consultas |
| Calendario semanal del paciente | El mismo patrón al contar mañana/tarde | Se reutiliza el conteo agrupado: cinco consultas como máximo, una para identificar agendas y cuatro para resolver cupos |
| Historial del paciente | Las confirmaciones se cargan en conjunto; el bucle solo combina resultados | No se encontró consulta adicional por cita en ese bucle |
| Inicio del médico | El bucle calcula totales y promedios en memoria | No se encontró consulta adicional por cita en ese bucle |
| Pacientes y seguimientos del médico | Consultas paginadas mediante `LIMIT`/`OFFSET` | No se identificó el mismo patrón de consultas por fila |
| Usuarios del administrador | Se cargan todos los usuarios y después se excluyen pacientes en Java | Pendiente: filtrar y paginar desde SQL, manteniendo las métricas globales separadas |
| Padrón médico de admisión | Se cargan todas las disponibilidades de la sede; un subquery cuenta citas por disponibilidad | Pendiente: carga paginada o bajo demanda de turnos y medición del plan SQL |
| Mantenimiento de ofertas | Recorre médicos y citas canceladas, con transacciones y bloqueos individuales | No agrupar indiscriminadamente: conserva exclusividad y prevención de doble reserva; requiere medición específica y lotes acotados si crece |

La prueba del calendario compara sedes y médico específicos con la búsqueda global, comprueba que una sede inactiva quede excluida y exige cinco consultas con siete días de agendas. Se mantienen las pruebas de retenciones, ofertas e intervalos superpuestos del conteo compartido. No se modifican datos ni se cachean cupos reservables.
