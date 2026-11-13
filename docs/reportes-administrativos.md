# Reportes administrativos

Rama: feature/reportes-administrativos. Reemplaza los datos de ejemplo del tablero operativo y de /admin/export por consultas reales. No requiere migraciones nuevas.

El personal de admisión solo obtiene información de su sede habilitada. El identificador de sede no se recibe del formulario: se resuelve con el usuario autenticado y se vuelve a comprobar en MySQL. La sede del horario original prevalece sobre la asignación actual del médico. Solo para citas antiguas sin horario se usa la sede del médico.

## Filtros y cifras

Desde y hasta son inclusivos, en America/Lima. El período máximo es 31 días. Se puede filtrar por estado; los estados desconocidos y las fechas inválidas devuelven 400. La tabla pagina de 20 en 20, ordenada por fecha e identificador. La consulta del resumen y sus filas comparten transacción y aislamiento REPEATABLE_READ.

- Citas del período: incluye todos los estados seleccionados, también canceladas y reprogramadas.
- Activas pendientes de cierre: SOLICITADA, CONFIRMADA, EN_SALA y EN_ATENCION.
- Canceladas: estado actual CANCELADA dividido entre citas filtradas. No mide el momento en que se cancelaron.
- Médicos habilitados: personal médico activo de la sede actualmente, independiente del período y estado de citas. No es ocupación de consultorios.
- Ofertas aceptadas: registros ACEPTADA de cupos cuya cita original corresponde al período, estado y sede seleccionados. La tasa usa todas esas ofertas como denominador.
- Espera media: diferencia entre llegada e inicio de atención, solo cuando existen ambas horas y el orden es válido. Si falta evidencia se muestra Sin datos.
- Riesgo: valores almacenados de citas activas. No se presentan como una nueva predicción de IA. Los valores nulos o desconocidos se cuentan aparte.

## Actualización

El tablero se consulta automáticamente cada cinco minutos solo si la pestaña está visible. No sustituye los datos durante una edición de filtros. Si falla la consulta, cambia la sesión o vence el tiempo de espera, conserva la última información con un aviso. El refresco automático no prolonga la inactividad de la sesión JWT. Actualizar ahora vuelve a consultar los filtros aplicados; Consultar aplica filtros nuevos.

## Descargas

PDF usa Apache PDFBox; Excel usa Apache POI y produce .xlsx auténtico; CSV usa UTF-8 con BOM. Se conservan los documentos como texto y se neutralizan fórmulas en CSV. En Excel las celdas se escriben como texto, sin fórmulas. El PDF repite el alcance en cada página y mantiene cada cita unida. La fuente PDF admite las tildes y la Ñ; caracteres fuera de su repertorio se sustituyen por ? para no invalidar el archivo. Excel y CSV conservan Unicode.

Los archivos PDF y Excel de citas también incluyen los indicadores. Para CSV se ofrece una descarga independiente de indicadores, con su sede, período y alcance en cada fila. También hay PDF y Excel de solo indicadores. Las agregaciones no tienen el límite de 2000 citas porque no cargan sus filas personales.

Las descargas aplican el mismo período, estado y sede. Exportan todas las filas filtradas, no solo la página visible, hasta 2000 citas; si se supera el límite se responde 422 y se pide reducir el filtro. La generación completa precede a la descarga para no devolver un archivo parcial como éxito. Se usan no-store y nombres de archivo sin datos personales.

El CSV de contacto preventivo añade un filtro explícito: citas SOLICITADA o CONFIRMADA sin asistencia confirmada y con riesgo MEDIO/ALTO o plazo vencido. No incluye citas canceladas ni inventa intentos de contacto. Su selección puede ser menor que la tabla general.

## Prueba manual

1. Entrar como personal administrativo y abrir Dashboard operativo.
2. Elegir un período con citas de la sede y comprobar cantidades y estados.
3. Cambiar estado y fechas; la tabla y los contadores deben corresponder al filtro.
4. Descargar PDF, Excel y CSV. Deben abrir y contener esas citas, con tildes y documentos completos.
5. En un período sin citas deben aparecer estados vacíos y archivos válidos sin filas inventadas.

Los demás tableros y exportaciones de padrones tienen revisión propia pendiente; esta entrega no certifica cifras ajenas al tablero operativo.
