# Seguimiento de ofertas por sede

HU013/RF13 y HU015/RF15. `/admin/reasignaciones` deja de mostrar una cancelación y dos destinatarios de ejemplo. Obtiene el historial de `reasignacion_inteligente`, muestra una fila por oferta, la cita original, el destinatario, la fecha de oferta, su límite, el estado guardado y la nueva cita si fue aceptada. No agrupa intentos diferentes como si fueran una única decisión ni interpreta una aceptación como asistencia médica.

La sede procede de la cuenta activa de admisión y se comprueba en la base. Las ofertas se delimitan por la sede de su disponibilidad original, de modo que cambiar la sede actual del médico no traslada su historial. No existe un selector de sede global para admisión. Pacientes, médicos, administradores del sistema, cuentas inactivas y sedes inactivas no acceden a este endpoint.

Los filtros de estado, especialidad y fecha de oferta consultan la base y se conservan al navegar. La fecha usa un intervalo semiabierto desde medianoche hasta la siguiente, sin confundirla con la fecha del cupo. El catálogo de especialidades se deriva de las ofertas de la sede; los nombres de médico y especialidad corresponden al catálogo actual, no a una copia histórica de sus datos. La paginación es de 15 filas, ordenadas por fecha e identificador descendentes. Recuento y filas se consultan en la misma transacción de lectura repetible. Una página que ya no existe se ajusta a la última disponible.

Los indicadores respetan los filtros y cuentan estados almacenados. Si una oferta PENDIENTE ya alcanzó el plazo, se señala como pendiente de procesamiento: abrir el panel no la expira ni genera otra. La normalización sigue correspondiendo al proceso transaccional de ofertas. Los registros vacíos muestran cero y un estado vacío. La consulta no genera reservas, envía correos ni cancela citas; tampoco prolonga sesiones mediante un refresco automático.

No se presenta un tiempo de respuesta inventado: todavía no existe una fecha de resolución fiable para todas las ofertas históricas. Los datos antiguos carecen también de una instantánea completa de las condiciones y características conocidas al ofrecer. Incorporar esos eventos fechados, entrenar y evaluar el modelo específico de aceptación y comprobar la política contra postergación siguen pendientes de RF13. Este panel no se anuncia como una selección realizada por IA. La prioridad clínica y la compatibilidad continúan siendo restricciones del motor existente.

La vista comparte navegación y estilos del portal administrativo; escapa nombres y demás textos de base de datos. No requiere tablas ni migraciones nuevas.

## Revisión local

1. Entrar como admisión y abrir Reasignaciones. Revisar sede, ofertas reales, destinatarios y resultados. Si no existen, debe aparecer el estado vacío.
2. Probar estado, fecha de oferta y especialidad; comprobar que Actualizar consulta esos filtros y Limpiar los elimina.
3. Si existen más de 15 resultados, cambiar de página con los controles de la derecha; se conservan los filtros.
4. Aceptar o rechazar una oferta desde el portal del paciente por el flujo habitual. Volver al panel y pulsar Actualizar para comprobar su resultado y la cita asignada cuando corresponda.
5. Revisar las pantallas de seguimiento de confirmaciones y de IA para comprobar que el menú compartido marca la sección correcta.

Prueba manual pendiente. Los tests JDBC comprueban ámbitos de sede, filtros, límites temporales, paginación, estados vacíos y lectura sin cambios de estado; el contrato HTTP comprueba roles, identidad de sesión y parámetros repetidos.
