# Seguimiento de historias de usuario y requisitos

Actualización: 01/11/2026. Fuente: requerimientos funcionales versión 2 y RNF suministrados por el equipo. HU001–HU018 se corresponden con RF01–RF18. El estado refleja funciones concretas; no se asigna un porcentaje ni se considera cerrado un requisito solo por tener su pantalla.

## Entregas comprobadas recientes

- PR52: invitaciones del personal y controles de ámbito administrativo.
- PR53: catálogo y persistencia del motivo en reserva/reprogramación.
- PR54: plazo de oferta y anticipación mínima persistentes y auditados.
- PR55: prioridad clínica autorizada para seguimientos vigentes, sin elevación desde el cliente.
- PR56: retención real del cupo y confirmación transaccional; prueba con dos pacientes aprobada.
- Rama feature/reportes-administrativos: datos reales por sede, filtros y exportaciones. La primera versión fue revisada por el usuario; la ampliación de indicadores y actualización automática requiere comprobación antes de sus commits.

## Matriz de cierre

| HU / RF | Base existente | Pendiente para el alcance completo |
|---|---|---|
| 001 / 01 | JWT revocable, recuperación y cambios verificados, invitación inicial | Unificar política de contraseña e inactividad de 15 minutos; documentar códigos frente a enlaces originales |
| 002 / 02 | Roles, invitación de un uso, perfiles y controles por ámbito | Regresión integral de modificaciones/bajas y separación entre cuenta y datos profesionales |
| 003 / 03 | Catálogo de sedes | Importación Excel por código, consultorios/turnos y eliminación de respuestas de ejemplo ante fallos |
| 004 / 04 | Médico, especialidad y sede; gestión de personal autorizada | Contrastar multiplicidad de asignaciones, cuenta buscada por DNI y cambios con citas vigentes |
| 005 / 05 | Propuesta médica revisada/publicada por admisión, bloques reservables | Renovación/Por vencer, recurrencia y consultorios según prototipo; documentar la ampliación del médico proponente |
| 006 / 06 | Motivos reales guardados y validados por especialidad | CRUD administrativo e importación por plantilla |
| 007 / 07 | Registro, DNI, perfil, correo y contraseña verificados | Estado de contacto verificado y medio habitual de confirmación; uniformar edición por admisión |
| 008 / 08 | Prioridad, fecha e indicación; vigencia y control vinculado | Regresión conjunta de seguimientos con los demás flujos y alcance por motivo de seguimiento |
| 009 / 09 | Búsqueda de horarios publicados y retención real | Medición de búsqueda menor de 3 segundos |
| 010 / 10 | Reserva, asistencia separada, reprogramación/cancelación y retención | Enlaces de correo para confirmar/cancelar e intentos configurados antes de cancelar por falta de confirmación |
| 011 / 11 | Compatibilidad, prioridad vigente, antigüedad y ofertas exclusivas | Criterios configurables, protección contra postergación y explicación del candidato elegido |
| 012 / 12 | Llegada por admisión, tardanza y atención por médico | Regresión integral por sede y cierre de atenciones pendientes; no inferir inasistencia por cambio de día |
| 013 / 13 | Oferta, aceptación, rechazo, vencimiento y reasignación transaccional | IA específica de aceptación de ofertas integrada y evaluada; mantener prioridad clínica y compatibilidad como restricciones |
| 014 / 14 | Correo de reserva/oferta y avisos del portal | Programador de recordatorios, urgente tras vencer, modificación/cancelación por correo y acciones verificadas desde enlaces |
| 015 / 15 | Seguimiento de admisión con SSE; tablero operativo real por sede | Comprobar refresco de cinco minutos y errores en el nuevo tablero; revisar indicadores del inicio administrativo y SysAdmin |
| 016 / 16 | Exportación médica; reportes de admisión con filtros en PDF/XLSX/CSV | Comprobar exportación de indicadores; completar analítica y exportaciones del administrador del sistema y padrones |
| 017 / 17 | Confirmaciones e intentos de contacto registrados | Integrar modelo con datos históricos, factores y aumento de nivel al vencer; no presentar el simulador sintético como validación real |
| 018 / 18 | Plazo de oferta y anticipación mínima configurables | Recordatorio, confirmación, intentos, tolerancia y criterios de priorización centralizados |

## Requisitos no funcionales

RNF10 tiene evidencia de retención y concurrencia en MySQL y prueba manual aceptada. RNF07 se cubre en seguimiento de admisión y se amplía al tablero operativo; el refresco no debe prolongar la sesión sin actividad humana. RNF16 sigue parcial mientras falten las importaciones por fila. RNF09, RNF11 y RNF13–14 requieren mediciones y recorridos de rendimiento, aprendizaje, accesibilidad y móvil; las pruebas unitarias no los sustituyen. Las metas de disponibilidad y el cumplimiento normativo no se acreditan mediante este listado.

## Siguiente entrega prioritaria

RF14 y RF18: parámetros de recordatorio/confirmación e infraestructura de recordatorios y avisos de cambios, con pruebas de duplicados, vencimiento y envío fallido. Después RF17/RF13 para integrar IA y RF03/RF06 para catálogos e importaciones pendientes. Mantener las ramas, pruebas manuales y PR por entrega.
