# Conexión local de Gemini

La validación del adaptador acepta claves con punto, utilizadas por las claves de autenticación actuales, y conserva el límite de longitud y el rechazo de espacios o saltos de línea. La clave se transmite únicamente en la cabecera x-goog-api-key al host fijo de Google; no forma parte de URLs, respuestas ni archivos versionados.

El 2 de noviembre de 2026 se consultaron los modelos disponibles con la clave privada del usuario. Google rechazó gemini-2.5-flash-lite para cuentas nuevas, pese a aparecer en el catálogo. gemini-3.1-flash-lite devolvió indisponibilidad temporal 503. gemini-3.5-flash-lite respondió HTTP 200, finalización STOP y una especialidad válida para un caso ficticio. La prueba recorrió el adaptador Java y la validación local del resultado; no creó una cita ni cambió prioridades.

El usuario confirmó que su proyecto muestra nivel gratuito. La modalidad Standard de gemini-3.5-flash-lite tiene entrada y salida gratuitas dentro de su cuota según la [tarifa del proveedor](https://ai.google.dev/gemini-api/docs/pricing). La disponibilidad y cuotas pueden cambiar; listar un modelo no garantiza que acepte generación para una cuenta concreta. No se modificó la facturación.

La configuración de esta máquina reside en .env.gemini, ignorado por Git: GEMINI_API_KEY contiene el secreto, GEMINI_MODEL identifica el modelo probado y GEMINI_ORIENTATION_ENABLED permite habilitarlo. La plantilla pública sigue desactivada y sin credenciales. En Railway se configurarían esas variables por separado, una vez aprobada la prueba local; esta entrega no publica ni despliega Railway.

La conexión externa se usa únicamente para casos ficticios de la demostración, sin datos personales ni información real de salud. No ofrece diagnósticos, tratamientos, urgencia o prioridad clínica. Los modelos locales de seguimiento y la IA pendiente de aceptación de ofertas son componentes distintos.

Las pruebas automatizadas cubren conservación exacta de la clave en cabecera, ausencia en la URL, límites y caracteres de control, respuestas JSON, cuotas, bloqueos, errores y contrato del servlet. La revisión manual pendiente se realiza en Paciente → Programar cita → Orientación, escribiendo un caso ficticio en texto libre. La prueba solo se acepta como externa si el resultado identifica proveedor GEMINI, no EJEMPLO_LOCAL.

## Ajuste del flujo de orientación

La pantalla elimina referencias a configuración técnica y demostración académica. Conserva una indicación breve sobre el alcance y la confirmación de usar un caso ficticio sin datos personales. Si el modelo necesita aclaración, se ofrece un acceso directo a la selección manual.

La instrucción de clasificación usa la ruta general disponible en el catálogo: Medicina General si existe y, en su ausencia, Medicina Interna. No convierte una aclaración del modelo en una sugerencia local ni acepta identificadores ajenos. Se comprobó con Gemini y el catálogo local un caso ficticio de dolor de cabeza y fiebre: respondió Medicina Interna, ID 4, proveedor GEMINI. No se creó una reserva ni se modificó el catálogo.

El campo incorpora writingsuggestions=false y autocomplete=off. Según la [documentación del atributo](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Global_attributes/writingsuggestions), el navegador puede desactivar sus sugerencias en ese campo si soporta la función. Esto no elimina de forma universal las herramientas o menús propios de Edge; su configuración global permanece bajo control del usuario.

## Fallos intermitentes y recuperación

El adaptador distingue demora (504), cuota (429), ocupado (503), conexión y JSON inválido. El registro técnico incluye solo fase, tipo de excepción, estado y duración; no incluye clave, relato ni respuesta del proveedor. Un 503 explícito puede reintentarse una vez tras una pausa breve y con el tiempo restante del presupuesto total de 20 segundos. No se repite automáticamente una petición con tiempo agotado o conexión incierta, para evitar duplicar una generación potencialmente procesada.

El navegador espera hasta 25 segundos, recupera el botón, ofrece Reintentar análisis y mantiene visible la búsqueda manual ante errores. Editar el caso elimina el mensaje anterior; una respuesta inválida o una aclaración no se convierten en una sugerencia inventada. Pruebas: 24 casos Java afectados y cuatro casos de formulario; vista compilada. La comprobación real adicional respondió HTTP 200 en 1,3 segundos y Medicina Interna para el caso ficticio documentado. Esa medición puntual no acredita disponibilidad continua de Google.
