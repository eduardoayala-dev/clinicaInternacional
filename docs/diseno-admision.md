# Diseño del portal de admisión

Rama: fix/diseno-admision. Revisión del 2 de noviembre de 2026, pendiente de prueba manual antes de publicar.

Se usa una plantilla compartida de navegación y perfil en Inicio, Médicos, Pacientes, Propuestas de horarios, Confirmaciones, Seguimiento IA, Reasignaciones y Dashboard operativo. Se conservan los accesos adicionales y se identifica la sección actual desde la ruta original de la petición. El cierre de sesión mantiene POST y CSRF. La plantilla declara UTF-8, utiliza iconos locales y permite envolver los textos del menú.

El contenedor central tiene límites flexibles de altura y anchura. La cabecera permanece fuera del área que desplaza y las tablas conservan desplazamiento horizontal propio. El ancho máximo de seguimiento se aplica al contenido interior. En móvil el menú conserva etiquetas accesibles y muestra sus iconos. Dashboard operativo comparte estructura, fuentes y colores; sus estilos específicos pasan de la vista a un archivo CSS. No cambian sus filtros, descargas ni actualización automática.

La información del entrenamiento de IA queda en un desplegable. Se conserva que el modelo es experimental, sintético y pendiente de evaluación representativa; no se atribuye precisión clínica. La tabla presenta la confirmación como Sí/No y mantiene los factores y niveles calculados. No se cambian probabilidades, prioridades ni estados de citas.

El enlace de Figma no pudo abrirse mediante la consulta web y la herramienta de navegador falló al inicializarse. No se afirma una comparación completa con todas las pantallas PA. Se usaron las referencias visuales ya proporcionadas y los estilos existentes.

Comprobaciones: ocho vistas compiladas con Jasper, 923 pruebas generales ejecutadas sin fallos y 559 omitidas por requerir MySQL; 28 pruebas de formularios aprobadas. Tras los ajustes de mantenimiento se repitieron las pruebas Java afectadas. La composición de IA se midió y capturó con Edge aislado a 1440, 1024 y 390 píxeles, sin desbordamiento horizontal de tarjetas y con título debajo de cabecera. Las capturas usan una representación con datos ficticios y los estilos originales; no sustituyen el recorrido autenticado ni certifican visualmente todas las vistas.

Revisar manualmente la navegación completa, encabezado de IA, tabla y desplegable; filtros y páginas de Reasignaciones; consultar, actualizar y descargar desde Dashboard operativo; abrir propuestas de horarios desde Médicos. El cambio visual queda sin commit hasta esa comprobación.

Corrección posterior: Tomcat usa nivel de lenguaje JSP 11 por defecto. La primera comprobación forzó 17 y no detectó un patrón instanceof incompatible en la plantilla del menú. Se reemplazó por una comprobación y conversión compatibles, se recompilaron las ocho vistas con source/target 11 sin errores y se aplicó el fragmento corregido al servidor local sin reiniciar la sesión. Las comprobaciones de disponibilidad no se consideran evidencia del renderizado de una vista autenticada.

La comprobación reproducible queda en scripts/Verificar-Vistas-Admision.ps1. Usa por defecto Jasper 10.1.61 y nivel de lenguaje 11, igual que el contenedor verificado; exige el WAR expandido de Maven y no modifica el servidor. Jasper puede devolver código de proceso cero con errores, por lo que el script también comprueba el resumen de compilación y falla si no contiene cero errores. El usuario confirmó que el portal volvió a cargar tras aplicar la corrección.

## Revisión del padrón, 3 de noviembre de 2026

Se encontró que Anterior/Siguiente en Médicos eran controles sin comportamiento. Se añadió paginación de diez registros sobre el resultado ya filtrado por el servidor, con resumen y límites accesibles. No cambia la consulta ni el ámbito por sede. Se retiraron los valores ficticios usados como respaldo en indicadores de Médicos y Pacientes. Dos pruebas del recorrido de páginas y los casos vacío/una página pasaron; las once vistas compilaron con Jasper y lenguaje Java 11. Aplicado al servidor local sin reinicio; pendiente de comprobación visual antes del commit.

La revisión detectó además que Editar médico todavía muestra un aviso provisional y Exportar Excel del padrón llama a imprimir. Ambas acciones se completan en la entrega posterior de datos profesionales y Excel descrita al final; no afectan a las exportaciones reales del Dashboard operativo.

## Entrega de datos profesionales y Excel, 3 de noviembre de 2026

La exportación del padrón produce XLSX mediante Apache POI, conserva CMP como texto y aplica búsqueda, especialidad, sede y estado. El alcance procede de la cuenta de admisión activa, no de parámetros del navegador. La edición permite especialidad y estado profesional; identidad, CMP, correo y traslado se mantienen en administración del sistema. No se cambia una especialidad con historial de citas, ni se da licencia/baja con disponibilidad publicada o citas pendientes. Una cuenta deshabilitada no se reactiva desde este flujo. Cada cambio y su auditoría se confirman juntos.

Verificación: 983 pruebas integradas ejecutadas sin fallos; 587 omitidas por depender de MySQL. Diez pruebas adicionales del padrón se ejecutaron en MySQL aislado sin fallos y el esquema temporal se retiró. Se compilaron las doce vistas con Jasper y Java 11. Se autoriza la publicación por petición expresa del usuario; continúa pendiente la revisión visual autenticada de esta edición nueva. No se ejecutaron carga, Selenium ni Lighthouse en esta entrega.
