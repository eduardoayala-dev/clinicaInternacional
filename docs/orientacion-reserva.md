# Orientación y motivo de consulta

Rama feature/orientacion-reserva, dependiente de las correcciones de conexión de Gemini. La pantalla de resultado sigue la referencia del usuario: indicador de éxito, especialidad sugerida como título, explicación breve, botón para buscar horarios, edición y selección manual alternativa. La ruta de orientación y el enlace Volver a opciones se muestran sobre el contenido. Se conservan la paleta y la navegación del paciente.

La orientación externa recibe únicamente el caso ficticio y el catálogo activo, ahora con sus motivos. Puede devolver idEspecialidad, idMotivo y un motivo técnico enumerado. Java comprueba que la especialidad esté activa y que el motivo pertenezca a ella; el nombre del motivo se obtiene de la base, no de una descripción libre generada. No crea motivos compartidos a partir del relato ni guarda los síntomas introducidos como parte de la reserva. Los mensajes anteriores sin idMotivo siguen siendo válidos, pero requieren selección manual del motivo.

Al continuar, el buscador aplica la especialidad y el motivo compatibles, muestra una indicación de que este último es sugerido y permite cambiarlo. Cambiar la especialidad limpia un motivo incompatible y oculta la etiqueta de orientación anterior. No se selecciona la primera opción automáticamente cuando falta una sugerencia. Se exige un motivo antes de buscar horarios y se conserva en el resumen y la reserva mediante idMotivo; los controles de pertenencia y estado del servidor siguen siendo obligatorios.

## Medicina General

sql/catalogos/medicina-general.sql añade la especialidad y cinco motivos generales de manera idempotente. No depende de un identificador fijo ni reactiva especialidades o motivos desactivados por el administrador. Los mismos datos se incluyen en schema.sql para instalaciones nuevas. No cambia tablas ni crea cuentas, médicos o disponibilidad.

Publicar horarios de Medicina General requiere registrar o asignar un médico de esa especialidad y publicar una disponibilidad por el flujo habitual. La ausencia de horarios no se solventa trasladando profesionales de otra especialidad sin una decisión administrativa. El paciente conserva las opciones de búsqueda y lista de espera.

## Comprobación

Pruebas del catálogo: carga repetida, identificadores no fijos, preservación de inactivos, motivos activos y ausencia de médicos inventados. Pruebas del contrato: motivo válido, motivo de otra especialidad, dato ausente, identificador fraccionario o texto y campo extra. Pruebas del formulario: aplicación, edición, limpieza al cambiar de especialidad, conservación de sesión/CSRF y recuperación tras errores del proveedor.

El usuario confirmó la prueba manual del flujo de orientación y motivo el 2 de noviembre de 2026. La reserva definitiva en Medicina General necesita un médico y un horario realmente publicados.

## Evidencia local del 2 de noviembre de 2026

Regresión general: 923 pruebas ejecutadas, sin fallos; 559 casos MySQL omitidos en esa ejecución. Tres pruebas específicas de carga en un esquema MySQL temporal y nueve pruebas de JavaScript aprobadas. La primera instancia temporal de MySQL no inició dentro del plazo; se usó un esquema separado con un usuario limitado en el motor local y ambos se eliminaron después. La vista JSP compiló sin errores.

Se creó un respaldo local antes de cargar Medicina General y cinco motivos. Una primera conexión auxiliar JDBC falló de forma transitoria; la disponibilidad de la aplicación y el motor siguieron correctos. La repetición accedió al catálogo y Gemini respondió Medicina General con el motivo Fiebre y malestar general, ID 51 en esta instancia. Los identificadores son evidencia de esa prueba y no constantes del código. No se generó una reserva.

