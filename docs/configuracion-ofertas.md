# Configuración de ofertas

El administrador del sistema puede cambiar el plazo de respuesta (5–60 minutos) y la anticipación mínima global (0–10080 minutos) desde Configuración de priorización. Los valores se guardan en `configuracion_ofertas`, se auditan y permanecen después de reiniciar la aplicación.

El motor lee los parámetros al generar una nueva oferta. La oferta vence al terminar el plazo configurado o al comenzar la cita, lo que ocurra primero. Las ofertas ya emitidas conservan su vencimiento. La anticipación global se combina con la preferencia del paciente: deben cumplirse ambas.

Una oferta mantiene el cupo exclusivo para un candidato. El rechazo o vencimiento permite continuar con el siguiente. No se presentan envíos simultáneos como una función implementada. La compatibilidad de especialidad, sede, turno y médico preferido sigue siendo obligatoria; prioridad clínica y antigüedad conservan su orden.

Solo un administrador activo puede guardar. El servidor toma su identidad de la sesión y comprueba su estado en la base. Una versión oculta del formulario evita sobrescribir un cambio realizado desde otra pestaña. Si la auditoría o el guardado fallan, la transacción se revierte.

Aplicar `sql/migrations/V013__configuracion_ofertas.sql` con respaldo previo antes de desplegar a una base existente. La migración crea la configuración inicial de 15 minutos y 0 de anticipación global, conservando el comportamiento previo. La comprobación de disponibilidad detecta si falta esta migración.

Prueba manual:

1. Entrar como administrador del sistema y abrir Configuración de priorización.
2. Guardar 7 minutos de plazo y 0 de anticipación, salir y volver a entrar. Deben conservarse.
3. Abrir dos pestañas; guardar en una y luego intentar guardar el formulario antiguo de la otra. Debe advertir que la configuración cambió.
4. Generar una oferta nueva en el recorrido de lista de espera. Su plazo debe corresponder a la configuración, sin modificar ofertas anteriores.
5. Restaurar 15 minutos y 0 de anticipación si se desea conservar los valores iniciales.

Esta entrega cubre parámetros de ofertas. Los recordatorios, límites de intentos, tolerancia de llegada y parámetros del modelo predictivo se mantienen fuera de este formulario hasta integrar sus respectivos flujos.
