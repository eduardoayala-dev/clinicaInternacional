# Verificación del portal médico

Fecha: 28 de octubre de 2026. Entorno: Docker local, Tomcat 10.1, Java 17 y MySQL 8. Rama: `fix/diseno-portal-medico`. Cambios pendientes de revisión del usuario, sin commit.

## Resultado y correcciones

Se revisaron Inicio, Agenda, Atención y Perfil, sus controladores, permisos y consultas JDBC. Se corrigió una diferencia entre la presentación y la regla transaccional: una llegada de un día anterior ya no anuncia las acciones Iniciar o No atender para una cita de hoy. La transacción ya rechazaba esa operación. Se añadió una regresión para ambas acciones y pruebas de los límites de los períodos y del aislamiento por médico.

Gestionar atención actual consulta siempre la atención activa en el servidor, incluso si comenzó antes de medianoche; no depende del contador de citas del día. Se aclararon los mensajes para citas pendientes de llegada, otra consulta activa y registros de solo lectura. Agenda muestra Atender cuando el servidor permite iniciar, y Continuar atención cuando está en curso. Se retiraron variables JSP sin uso y se corrigió la concordancia de pendiente en Inicio.

## Pruebas ejecutadas

- Regresión Java: 294 pruebas ejecutadas, cero fallos. Los contratos MySQL se ejecutaron además de forma explícita en Docker.
- Contratos MySQL aislados: 93 pruebas, cero fallos ni omisiones. La base temporal se retiró al finalizar.
- JavaScript: 32 pruebas, cero fallos.
- Navegador y HTTP: cuatro pantallas, calendario, sesión/permisos y flujo completo con casos sintéticos; sin errores de JavaScript en las pantallas comprobadas.

## Comprobaciones

| Área | Resultado comprobado |
| --- | --- |
| Identidad y permisos | La identidad procede de la sesión y del perfil activo en BD. Cambiar `idMedico` no cambia la agenda. Las citas ajenas e inexistentes responden 404. Pacientes y visitantes no acceden al portal médico. |
| Sesiones y CSRF | Dos sesiones tienen tokens distintos. POST sin token o con el de otra sesión devuelve 403. Cerrar sesión impide volver a consultar el perfil con esa sesión. |
| Entrada y estados | Un identificador con SQL se rechaza con 400; una modificación por GET devuelve 405; repetir un cierre devuelve 409 y conserva el registro. Consultas parametrizadas y salida HTML codificada. |
| Atención | Se verificó llegada por admisión, inicio, campos habilitados solo durante la consulta, prioridad con teclado, restablecimiento y validación de indicaciones obligatorias. El cierre persiste indicaciones y seguimiento y deja el registro de solo lectura. |
| No atención | Cancelar la confirmación no envía el formulario. Confirmarla registra el motivo y cierra la cita. El caso sintético sin llegada quedó intacto. |
| Transacciones | Los contratos JDBC verifican exclusión de consultas activas simultáneas por médico y paciente, concurrencia, permisos, rollback, historial único y cierre después de medianoche. |
| Agenda | Día usa seis filas por página. Semana y Mes consultan rangos reales. Se verificaron semanas de siete días, meses de 35 y 42 celdas, Anterior/Siguiente/Hoy, búsqueda, apertura de ficha y acceso a todas las citas de un día. |
| Exportación | CSV incluye todas las citas del período, no solo la página. La impresión conserva el aislamiento por médico y la codificación de los datos. |
| Interfaz | Las cuatro pantallas devolvieron 200. Se comprobaron 1440, 768, 390 y 320 px, sin desbordamiento horizontal de la página, tildes, recursos locales, foco, Escape, encierro del foco en el menú móvil y paginación a la derecha. |

La prueba de flujo en navegador utilizó tres pacientes sintéticos propios de esta auditoría (`audit.verificacion.*@example.test`). No se modificaron cuentas ni citas reales. Las pruebas MySQL usan una base temporal independiente con prefijo `clinica_agendas_test_`; su preparación no ejecuta `schema.sql` sobre `clinica_db`.

Las evidencias locales están en `target/diseno-medico/auditoria-java.log`, `auditoria-js.log`, `auditoria-mysql.log`, `auditoria-seguridad.log`, `auditoria-front.log`, `auditoria-flujo.log` y `calendario-final.log`. Son archivos de trabajo ignorados por Git.

## Funcionalidades recomendadas

La siguiente prioridad es profundizar el trabajo del médico, no multiplicar enlaces a páginas vacías. Mantendría Inicio, Mi agenda, Atención actual y el perfil en el pie, y añadiría dos secciones en entregas separadas:

1. **Mis pacientes / Historial de atenciones.** Buscar entre pacientes con una relación asistencial autorizada; revisar atenciones previas, indicaciones, alergias y seguimiento. La autorización debe comprobarse también en cada detalle y exportación, sin abrir una búsqueda universal por DNI. Los registros cerrados deben conservarse; una corrección posterior necesita motivo, autor y fecha.
2. **Seguimientos.** Lista de revisiones recomendadas pendientes, vencidas y ya reservadas. Una fecha recomendada no representa una cita confirmada: el paciente o admisión deben completar la reserva sobre disponibilidad publicada.

Dentro de **Atención actual**, una entrega posterior puede ampliar las notas a una consulta estructurada: motivo, antecedentes relevantes, hallazgos, evaluación, diagnóstico y plan. El formulario actual de indicaciones de hasta 200 caracteres y prioridad es un registro básico, no una historia clínica longitudinal completa.

Después pueden añadirse órdenes de exámenes, revisión de resultados y recetas, con estados y trazabilidad propios. La disponibilidad publicada puede mostrarse al médico, conservando la publicación en administración si esa es la regla del negocio. No conviene habilitar cambios de horarios o de citas ajenas por añadir un menú.

Como referencia de alcance, [HL7 FHIR, resumen clínico](https://fhir.hl7.org/fhir/clinicalsummary-module.html) distingue condiciones, alergias, observaciones y otras piezas del registro clínico. La propuesta aprovecha esa separación conceptual; esta rama no implementa interoperabilidad FHIR.

## Alcance de la aprobación

La verificación corresponde al flujo local y a los casos automatizados descritos. No se probó un despliegue cloud ni carga concurrente de una clínica completa. SonarCloud se debe volver a ejecutar cuando estos cambios lleguen al PR; el resultado de un PR anterior no certifica esta rama.
