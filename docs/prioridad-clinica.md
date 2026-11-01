# Prioridad clínica y ofertas de cupos

La prioridad de una próxima cita o inscripción se obtiene de la última consulta finalizada del paciente en esa especialidad. Debe existir una atención cerrada, una cita atendida y la referencia de especialidad registrada durante el cierre. Sin seguimiento vigente se utiliza MEDIA únicamente como orden interno inicial; el paciente ve «Sin evaluación vigente». La fecha recomendada funciona como fecha límite inclusiva elegida por el médico: no se extiende automáticamente. Una reserva o un cupo posterior a esa fecha no hereda la prioridad.

El médico registra ALTA, MEDIA o BAJA, una indicación y, si corresponde, la fecha límite recomendada del seguimiento al finalizar la atención. Sin fecha, la evaluación queda en el historial y no prioriza ofertas. El cierre, la referencia de especialidad y la auditoría se confirman en una sola transacción. Un fallo revierte todo. La especialidad queda guardada en la atención: cambiar posteriormente la asignación del médico no mueve la evaluación a otra especialidad.

El paciente no puede elevar la prioridad enviando `urgencia` en una reserva, una reprogramación o una inscripción. Se conserva la compatibilidad de las interfaces, pero los valores enviados por el cliente no determinan el nivel guardado ni el orden de las ofertas.

El motor selecciona primero pacientes compatibles y luego ordena por ALTA, MEDIA y BAJA; a igual nivel conserva antigüedad e identificador como desempate. Consulta la evaluación vigente incluso si se registró después de inscribirse. Se considera solo la última evaluación: si vence, no reactiva una anterior. Completar el control vinculado mediante el módulo de seguimientos consume la prioridad; cancelar ese control no la consume. La nueva atención puede establecer una nueva indicación. Reenviar las mismas preferencias no reinicia la antigüedad por un cambio clínico. Una nueva evaluación no retira una oferta que ya está vigente para otra persona.

Aplicar `sql/migrations/V014__especialidad_prioridad_clinica.sql` con respaldo previo. Añade una columna a `atencion_cita`, sin crear otra tabla ni alterar citas existentes. Las consultas anteriores permanecen en el historial; no se atribuye automáticamente una especialidad histórica que la base no conservaba.

Prueba local:

1. Finalizar una atención como médico, indicando prioridad ALTA, indicación de control y fecha límite futura.
2. Con ese paciente, inscribirse en lista de espera para la misma especialidad. Debe verse ALTA en el estado de la inscripción.
3. Inscribir otro paciente compatible sin evaluación, con prioridad inicial MEDIA.
4. Liberar un cupo compatible desde el flujo de cancelación. La oferta debe ir al paciente con ALTA, aunque el otro se haya inscrito antes.
5. Verificar que ambos respetan sede, turno, médico preferido y anticipación. La prioridad no elimina esos requisitos ni confirma una reserva sin aceptación.
