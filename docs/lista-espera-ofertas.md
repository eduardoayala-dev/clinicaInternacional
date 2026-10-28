# Lista de espera y ofertas de cupos

Una inscripción utiliza especialidad, sede opcional, médico preferido opcional, horario y aviso mínimo de 3 o 12 horas. La disponibilidad de aviso no cambia la prioridad clínica. Las preferencias de WhatsApp, correo y SMS se guardan; las notificaciones de esta implementación se entregan dentro del portal.

Al cancelarse una cita futura de un turno publicado, el sistema busca una inscripción compatible y activa. Ordena por prioridad clínica y antigüedad. Ofrece el cupo durante 15 minutos, sin superar el inicio de la cita. Durante ese plazo, el catálogo y las reservas normales excluyen el intervalo ofertado.

Aceptar verifica sesión, propiedad de la oferta, vencimiento y disponibilidad dentro de una transacción. Crea una cita real y su notificación, relaciona la oferta con esa cita y marca la inscripción como atendida. Una aceptación repetida devuelve el mismo identificador sin duplicar citas. Rechazar conserva la inscripción y ofrece el cupo al siguiente candidato compatible. Una oferta rechazada o vencida no vuelve a ofrecerse a esa misma inscripción para la misma cita original.

Un proceso de mantenimiento revisa las ofertas cada 60 segundos mientras la aplicación está encendida. Libera las vencidas y busca nuevos candidatos. La validez se comprueba también al reservar o aceptar; no depende del contador del navegador. Retirarse de la lista no cancela una cita confirmada.

## Base de datos existente

Aplicar `sql/migrations/V002__ofertas_lista_espera.sql` con una cuenta de migración, después de un respaldo. No ejecutar `sql/schema.sql` sobre una base con datos. El usuario de la aplicación mantiene permisos de operación, sin permisos de modificación del esquema. En este entorno local la migración ya se aplicó y el respaldo está en `target/lista-espera/respaldo-antes-v002.sql`.

## Contratos HTTP

Enviar `Accept: application/json` para recibir `{code,message,data}`. Los POST requieren la sesión y su token CSRF. El paciente siempre se obtiene de la sesión.

| Operación | Método y ruta | Resultado |
| --- | --- | --- |
| Consultar inscripción | GET `/paciente/lista-espera` | 200; `data` puede ser null |
| Inscribirse | POST `/paciente/lista-espera`, `action=inscribir` | 201 e `idListaEspera` |
| Retirarse | POST `/paciente/lista-espera`, `action=retirar`, `idListaEspera` | 200 |
| Consultar bandeja y oferta | GET `/paciente/notificaciones` | 200; oferta y plazo reales |
| Aceptar oferta | POST `/paciente/notificaciones`, `accion=aceptar`, `idOferta` | 200 e `idCita` |
| Rechazar oferta | POST `/paciente/notificaciones`, `accion=rechazar`, `idOferta` | 200 |
| Marcar aviso propio | POST `/paciente/notificaciones`, `action=marcarLeida`, `id` | 200 |

Los identificadores inválidos devuelven 400; una sesión o rol incorrecto, 403; un recurso ajeno o inexistente, 404; un estado incompatible, 409; una oferta vencida, 410; y un fallo de base de datos, 503. Los formularios HTML utilizan redirección y mensajes visibles. Los GET con acciones de escritura devuelven 405.

## Comprobación local

Importar `docs/postman/Lista-Espera-Local.postman_collection.json`. Configurar `correoPersonal`, `clavePersonal`, `fechaAgenda` futura, `idMedico`, `idSede` e `idEspecialidad` compatibles. Activar cookies y desactivar el seguimiento automático de redirecciones. Ejecutar en orden únicamente en local: crea dos pacientes sintéticos, comprueba la oferta, propiedad, bloqueo del cupo y aceptación repetida; finalmente cancela la cita sintética. Conserva las cuentas de prueba y la agenda. No consulta al proveedor de DNI.

Para probar en pantalla, inscribir un paciente en `/paciente/lista-espera` y cancelar desde otro paciente una cita compatible con suficiente anticipación. Volver a `/paciente/notificaciones` para ver y aceptar o rechazar la oferta. Al aceptar, comprobar la cita en el historial.
