# Cierre funcional previo a las mediciones

Cambios sin commit, pendientes de revisión final del usuario.

- HU009: médicos ordenados por su primer cupo libre del día y turno seleccionados; horas cronológicas; empate por identificador.
- HU014: correo como canal operativo gratuito. SMS permanece fuera de alcance y no se declara implementado. Valor predeterminado de recordatorio: 24 horas. V023 modifica el valor predeterminado y ajusta el registro inicial de 48 horas solo si conserva versión 0; no sobrescribe una política modificada por el administrador.
- HU015: ocupación = cupos únicos consumidos / cupos únicos publicados, por sede y período. Cancelaciones y reprogramaciones no consumen el cupo anterior. No equivale a ocupación física de consultorios. La ocupación y ofertas pendientes no dependen del filtro de estado de citas. Una oferta pendiente debe estar vigente, corresponder a una cita futura cancelada y un horario publicado. Actualización por eventos con respaldo periódico; respeta los filtros en edición.

## Versionado de rendimiento

Aplicar docs/rendimiento/comparacion-antes-despues.md después de aprobar los commits y PR funcionales. No crear el baseline sobre cambios sin commit. Conservar los reportes originales y SHA en cada evaluación. Separar Selenium, carga/estrés y Lighthouse/PageSpeed. No introducir una optimización antes de capturar la medición inicial de su categoría. No publicar capacidades ni resultados estimados.
