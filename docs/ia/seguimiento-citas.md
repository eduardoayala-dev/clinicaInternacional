# Seguimiento de citas con un modelo entrenado

HU017/RF17, entrega en revisión local. Admisión abre «Ver apoyo de IA» desde Confirmaciones y seguimiento. La evaluación usa el modelo de regresión logística empaquetado; no requiere API externa, suscripción ni un proceso Python en el servidor. La inferencia es real, pero el entrenamiento y las métricas disponibles siguen siendo sintéticos y no acreditan precisión para pacientes reales.

## Identidad y extracción

La consulta obtiene el actor de la sesión y comprueba en la base su cuenta activa, rol de admisión y sede activa. La cita se autoriza por la sede del bloque original, con la sede del médico como alternativa cuando no existe bloque. No se acepta un paciente ni una sede enviados por el formulario. Otras sedes y cuentas reciben 404. Solo se evalúan reservas futuras SOLICITADA/CONFIRMADA de pacientes activos.

El instante de reserva procede de confirmacion_asistencia.creada_en, escrito por el flujo de reserva con el reloj de la aplicación. No se sustituye por la fecha por defecto del servidor de base de datos. El historial cuenta únicamente resultados documentados antes de ese instante: atenciones finalizadas y concordantes con paciente/médico, e inasistencias con registro de admisión. Las cancelaciones, reprogramaciones, estados sin evidencia, registros en el límite exacto y resultados posteriores no se cuentan como antecedentes. La confirmación expresa corresponde al momento de evaluar; el estado CONFIRMADA de la reserva no la sustituye.

Una única consulta obtiene esos datos en una instantánea de lectura. No se recortan historiales ni se inventan fechas para adaptarlos al modelo: datos incoherentes, ausentes o fuera de los límites permitidos producen una respuesta 409 y permiten continuar el seguimiento habitual. Un modelo no disponible produce 503. La evaluación no envía nombres, documentos, correos ni síntomas al modelo; sus cuatro entradas son los recuentos, la anticipación y la confirmación expresa.

## Resultado y reglas

La pantalla identifica versión, origen sintético, instante, corte del historial, probabilidad y factores. El nivel de seguimiento se muestra por separado del nivel aprendido. Al vencer el plazo sin confirmación, una regla eleva un nivel el seguimiento (BAJO a MEDIO, MEDIO a ALTO, ALTO permanece ALTO); no modifica la probabilidad ni se presenta como aprendizaje automático.

La lectura no modifica riesgo_cancelacion, prioridad clínica, cita, ofertas ni contactos. No equivale a una cancelación predictiva ni reserva cupos. No se guarda una evaluación histórica: abrir nuevamente reconstruye el resultado actual y se identifica con su instante. La trazabilidad persistida y la evaluación representativa quedan pendientes antes de atribuirle efecto operativo. El historial reconstruido usa estados actuales junto con evidencias fechadas; no constituye una reconstrucción completa de todas las versiones históricas de un registro corregido.

## Comprobación manual pendiente

1. Admisión → Confirmaciones y seguimiento → Ver apoyo de IA de una reserva futura.
2. Revisar fecha de evaluación, versión, procedencia, cuatro factores y niveles diferenciados. La cita mantiene su estado.
3. Registrar una confirmación expresa por el flujo habitual y volver a evaluar la misma cita: cambia la entrada de confirmación y deja de aplicarse la regla de plazo vencido.
4. Probar desde otra sede y con otro rol: no se accede a los datos. Cancelar una cita por el flujo normal impide evaluarla como futura activa.
5. Revisar la pantalla con teclado y en móvil. El simulador del administrador conserva su acceso separado para escenarios manuales.

Las pruebas JDBC cubren permisos, sede del bloque original, antecedentes fechados, exclusión de resultados posteriores, confirmación expresa, datos ausentes y citas no evaluables. Las pruebas del servlet verifican identidad de sesión, parámetros repetidos y contrato. La paridad Java/Python usa los casos de referencia empaquetados; no sustituye una evaluación con datos reales.

No se requiere una nueva tabla o migración. Esta rama parte de la entrega del PR #61; debe integrarse después de ella. Se mantiene sin commit hasta la prueba manual del usuario.
