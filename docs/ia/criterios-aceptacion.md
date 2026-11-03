# Criterios de aceptación de la IA

El simulador es la primera entrega técnica de HU017/RF17. Sus pruebas no sustituyen la aceptación de los flujos operativos. Este documento separa lo implementado de los siguientes bloques y evita declarar cerradas historias incompletas.

| Bloque | Estado | Evidencia necesaria para cerrar |
|---|---|---|
| Modelo de inasistencia | Implementado con datos sintéticos; revisión manual pendiente | Reproducción del entrenamiento, paridad Python/Java, explicación, procedencia y límites visibles |
| Simulador protegido | Implementado; revisión manual pendiente | Administrador autorizado, CSRF, validación, resultado actualizado y recuperación tras errores |
| Seguimiento preventivo RF17 | Integración local experimental; revisión manual pendiente | Historial reconstruible, confirmación expresa, plazo y contactos por admisión de la sede |
| Selección con aprendizaje HU013 | Pendiente | Modelo de aceptación de ofertas evaluado y subordinado a compatibilidad y prioridad |
| Configuración RF18 | Plazos, recordatorios y contactos mínimos integrados y probados; criterios pendientes | Completar criterios de selección persistidos y auditados |

## Comprobación manual del simulador

1. Entrar como administrador del sistema y abrir Priorización → simulador de IA de inasistencias. Verificar que se identifica como experimental y sintético.
2. Introducir 3 inasistencias, 2 atenciones, 30 días de anticipación y sin confirmación expresa. El escenario produce aproximadamente 77,3 %. No genera citas ni contactos.
3. Cambiar un dato: el resultado anterior debe ocultarse. Repetir con confirmación expresa: la probabilidad del mismo escenario baja aproximadamente a 52,0 %. Ambos valores siguen en el nivel demostrativo alto; no se debe simular un cambio de nivel inexistente.
4. Probar un historial vacío y luego valores fuera del rango de entrenamiento pero permitidos por el formulario: deben aparecer las advertencias correspondientes. Un número negativo o una anticipación superior a 180 días debe bloquearse.
5. Verificar explicación, factores y lectura en móvil. Una petición pendiente bloquea el botón; cambiar las entradas cancela la petición y permite una nueva simulación. Una respuesta antigua no debe sobrescribir la nueva.

Las comprobaciones de permisos, contrato, CSRF y fallos se automatizan. No es necesario alterar la base ni enviar correos para aceptar este bloque.

## Siguiente bloque: seguimiento preventivo

- El servidor obtendrá paciente y sede desde la cita y la identidad autorizada. El formulario no podrá seleccionar un paciente ajeno ni una sede distinta.
- Una reserva con estado `CONFIRMADA` no acredita confirmación expresa. Esta necesita fecha, actor y medio verificables; si faltan, debe indicarse la ausencia, sin inventar una respuesta.
- Las características históricas deben poder reconstruirse en el instante definido para la predicción. No se contarán resultados conocidos después de ese instante, cancelaciones como inasistencias ni citas pendientes de cierre.
- Separar la probabilidad y el nivel del modelo del nivel de seguimiento. Subir un nivel por plazo de confirmación vencido es una regla de RF17, debe explicarse como tal y no alterar la probabilidad aprendida.
- Admisión registrará cada intento de contacto con fecha, medio, resultado y responsable. El envío de un correo no acredita respuesta o lectura. SMS y WhatsApp seguirán deshabilitados mientras no haya integración real.
- El riesgo no cambiará el estado de la cita. La cancelación corresponde a una operación explícita del personal autorizado, con motivo y controles de RF10/RF18.
- Cada evaluación deberá identificar versión del modelo, instante y procedencia. Los datos faltantes impedirán una evaluación operativa válida; no se reemplazarán silenciosamente por ceros.
- El modelo sintético seguirá identificado como experimental al utilizar datos del sistema. Sus métricas no se presentarán como validación de pacientes reales.

## Siguiente bloque: IA de aceptación de ofertas

- El objetivo será aceptar la oferta antes de su vencimiento, no asistencia a una cita. No se calculará como `1 - riesgo de inasistencia`.
- Filtrar primero sede, especialidad, médico/preferencias, horario, anticipación, citas incompatibles y ofertas ya pendientes.
- Respetar prioridad clínica y protección contra postergación antes de usar la puntuación aprendida como criterio autorizado de selección. Los criterios y desempates dependerán de parámetros persistidos y auditados.
- Validar resultados con particiones temporales y por paciente, referencia determinista y métricas de selección. Una demostración sintética se identificará como tal.
- Ante un modelo ausente o incompatible, conservar el orden determinista, explicar el motivo y no bloquear las reservas.
- Crear una oferta no reserva una cita para el candidato. Aceptación, rechazo y expiración usarán el flujo transaccional existente, sin reservas duplicadas ni ofertas reutilizables después del vencimiento.

La integración de estos bloques se desarrollará en ramas separadas, con prueba manual previa a sus commits y PR, y conservando las ramas después de integrarlas.
