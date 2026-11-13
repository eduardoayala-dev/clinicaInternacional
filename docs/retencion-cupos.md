# Retención temporal de cupos

Al pasar del horario al resumen, POST /paciente/retencion retiene el intervalo durante cinco minutos como máximo. La fecha de vencimiento está en MySQL, no depende del contador del navegador. Repetir la misma solicitud devuelve el identificador y plazo restantes, sin extenderlos. Solo existe una retención por paciente.

El propietario se obtiene de la sesión autenticada. La confirmación comprueba propietario, identificador, médico, horario y vencimiento, consume la retención e inserta la cita en la misma transacción. Un error revierte ambas operaciones. El formulario no puede apropiarse de una retención ajena y repetir una confirmación no crea otra cita.

Los bloqueos de médico serializan la adquisición con reservas, reprogramaciones y ofertas; se comprueba el cruce de intervalos, no solo la igualdad de horas. Los catálogos ocultan retenciones vigentes. Las ofertas exclusivas también impiden adquirir una retención sobre su cupo. La retención no genera cita, correo ni confirmación de asistencia.

Volver a resultados solicita la liberación. Si se cierra la pestaña o no llega la petición, la retención vence sin requerir un proceso en segundo plano. Las filas vencidas no bloquean, y se retiran al reutilizar la retención del paciente o el horario del médico. No se reinicia el plazo al abrir el resumen.

Aplicar V015__retencion_cupos.sql con respaldo. Requiere V014 si se integra junto con prioridad clínica. Crea retencion_cupo, una tabla operativa temporal que no sustituye a cita. Las rutas internas de reserva conservan su contrato y respetan retenciones; el formulario web exige la suya al confirmar.

Prueba manual: abrir dos sesiones de pacientes distintos y elegir el mismo horario. La primera que pasa a Confirmar obtiene la retención; la segunda debe recibir conflicto o dejar de ver ese cupo. Volver a resultados libera el cupo. Confirmar crea una sola cita; esperar al vencimiento obliga a elegir de nuevo.
