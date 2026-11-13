# Historial clínico y seguimientos del médico

## Ramas y entrega

La base visual y el calendario están en `fix/diseno-portal-medico` (PR 33 a `develop`). Esta ampliación vive en `feature/historial-seguimientos-medicos`, creada desde esa base. Se integra primero el PR de la base; después, el de funciones. No eliminar las ramas. Los cambios nuevos deben probarse localmente antes de sus commits.

## Menús y permisos

**Mis pacientes** muestra únicamente pacientes con una atención iniciada por el médico o una atención anterior con asistencia confirmada en su historial. Una reserva futura no concede acceso al historial. El detalle muestra solo consultas del mismo médico. Cambiar `idPaciente`, `idCita`, `idCitaOrigen`, `idCitaControl` o `idMedico` no amplía permisos. Una búsqueda es de nombre o documento dentro de esa relación asistencial, no un buscador global de DNI.

Las listas usan paginación en SQL de veinte registros, con una fila adicional para indicar Siguiente, orden estable y filtros acotados. Los comodines introducidos en la búsqueda se tratan como texto. No se cargan todas las consultas para paginarlas en el navegador.

**Seguimientos** muestra recomendaciones de consultas atendidas: Pendiente hasta la fecha recomendada, Vencido después de esa fecha, Reserva vinculada cuando el médico vincula una cita vigente, y Realizado cuando esa cita vinculada consta como atendida. Encontrar otra reserva del paciente no confirma automáticamente que sea el control.

Vincular no crea ni reprograma citas. El paciente o admisión deben reservar primero. El médico puede vincular una cita posterior de su mismo paciente y de su propia agenda. No se permite reemplazar una vinculación vigente por otra; sí se puede sustituir una reserva cancelada, reprogramada o que ya no cumple la relación de médico y paciente. Una misma reserva puede atender varias recomendaciones del mismo paciente. Se guarda quién vinculó y cuándo.

## Registro clínico

Al finalizar una atención nueva se requieren motivo, evaluación, diagnóstico o impresión diagnóstica y plan. Antecedentes y hallazgos son opcionales. Límites: 1000 caracteres para motivo y diagnóstico; 2000 para antecedentes, hallazgos, evaluación y plan. Las indicaciones breves al paciente conservan el límite existente de 200 caracteres; el seguimiento futuro sigue siendo opcional.

Los campos estructurados, cierre operativo, prioridad, recomendación e historial se guardan en una transacción. Si falla una parte, no se cierra parcialmente la cita. Solo una consulta propia en curso puede finalizarse. Una atención cerrada se consulta como solo lectura; esta entrega no introduce sobrescritura ni corrección retroactiva de registros clínicos.

Las atenciones antiguas se muestran sin inventar evaluación, diagnóstico o plan. Conservan sus indicaciones y, si corresponde, se indica que no tienen registro estructurado. El método interno anterior de cierre permanece para compatibilidad; el POST público de cierre utiliza el registro estructurado obligatorio.

## Contratos

Todas las rutas requieren sesión de médico y perfil vigente en BD. Con `Accept: application/json` responden `{code,message,data}`. Los POST requieren CSRF de la misma sesión. Errores: 400 datos/filtros inválidos; 403 acceso o CSRF; 404 registro no autorizado o inexistente; 405 método/acción GET de escritura; 409 conflicto de estado o vinculación; 503 fallo de datos.

| Ruta | Datos |
| --- | --- |
| GET `/medico/pacientes?q=&pagina=1` | `data.pacientes`: `{items,pagina,tamano,tieneSiguiente}`. |
| GET `/medico/pacientes?idPaciente=ID&pagina=1` | `data.paciente` y `data.atenciones` paginadas. |
| GET `/medico/seguimientos?q=&estado=&pagina=1` | `data.seguimientos` paginados, `seleccion` y `controles`. `estado`: vacío, PENDIENTE, VENCIDO, RESERVADO o REALIZADO. |
| GET `/medico/seguimientos?origen=ID` | Selección propia y hasta cien reservas posteriores candidatas, ordenadas por fecha. |
| POST `/medico/seguimientos` | `accion=vincular`, `idCitaOrigen`, `idCitaControl`, `csrfToken`. |
| POST `/medico/atencion` | Para `accion=guardar`: campos existentes y `motivoClinico`, `antecedentes`, `hallazgos`, `evaluacion`, `diagnostico`, `plan`. |

## Base de datos y mantenimiento

Aplicar **V005__historial_seguimientos_clinicos.sql una vez**, después de V004 y antes de desplegar el nuevo WAR. Añade `consulta_clinica`, `seguimiento_cita` e índice compuesto por médico, paciente y fecha. Hacer respaldo antes de la migración. No ejecutar `sql/schema.sql` sobre una base con datos: ese archivo sirve para inicialización y elimina tablas. La migración no elimina ni convierte registros anteriores.

En local, el respaldo previo está en `target/funciones-medico/respaldo-antes-v005.sql`, ignorado por Git. Las pruebas MySQL usan una base temporal independiente con prefijo `clinica_agendas_test_`. El volumen persistente de la clínica se conserva.

El repositorio de historial está separado de `CitaDAO`; implementa un contrato inyectable y utiliza DTO inmutables. Los controladores reutilizan autenticación, errores y contexto del portal. La validación clínica se concentra en `ConsultaClinicaService`. Los fragmentos compartidos resuelven cabecera, contexto y paginación; las vistas codifican toda salida dinámica.

## Prueba manual

1. Ingresar como médico y abrir Mis pacientes. Revisar un paciente atendido, sus consultas y una ficha anterior de solo lectura.
2. Registrar la llegada de un paciente desde admisión. Iniciar su consulta como médico y completar el registro clínico, prioridad, indicaciones y una fecha futura de seguimiento. Finalizar.
3. Recargar la ficha: los campos deben persistir y quedar bloqueados. El historial debe incluir esa atención.
4. Abrir Seguimientos: la recomendación debe estar Pendiente, aunque ya exista otra reserva sin vincular.
5. Reservar una cita posterior como paciente o admisión. En Seguimientos, seleccionar Vincular reserva y confirmar. El estado cambia a Reserva vinculada.
6. Atender ese control siguiendo el flujo habitual. El seguimiento de origen pasa a Realizado.
7. Comprobar búsqueda, filtros de estado, Anterior/Siguiente y vista móvil en los dos menús nuevos.

Las recetas, órdenes de exámenes, resultados y modificaciones retroactivas con auditoría son módulos posteriores; no se agregan enlaces sin funcionalidad ni se presentan indicaciones breves como una historia clínica hospitalaria completa.

## Verificación de esta entrega

- 325 pruebas Java y 33 pruebas JavaScript, sin fallos.
- 55 contratos JDBC sobre MySQL real en una base temporal aislada, sin fallos. La base temporal se retiró al finalizar.
- Navegador: registro clínico obligatorio, restablecimiento de campos, cierre persistido y bloqueado, historial propio, texto de prueba XSS inerte, reserva real desde el portal del paciente, vinculación con confirmación y CSRF, filtros y adaptación a 1440, 768, 390 y 320 px.
- Se corrigió la serialización de fechas Java en el contrato JSON y se añadió una regresión: el cuerpo se serializa completamente antes de escribir la respuesta, evitando devolver JSON parcial.

Las evidencias y credenciales sintéticas locales están en `target/funciones-medico/`, ignorado por Git. El caso Manual se conserva para revisión del usuario. Las funciones nuevas permanecen sin commit hasta esa revisión. SonarCloud del PR de la base aprobó el Quality Gate; la ampliación se analizará cuando tenga su propio PR.
