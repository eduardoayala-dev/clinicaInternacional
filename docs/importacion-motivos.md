# Importación Excel de motivos

Rama feature/importacion-motivos, dependiente de feature/gestion-motivos. El usuario confirmó el funcionamiento del bloque y autorizó su publicación el 3 de noviembre de 2026. El original de RF06 asigna la gestión a PA y RNF16 exige que las filas con errores no bloqueen las correctas. Se corrigió el planteamiento inicial para respetar ambos criterios.

## Flujo

Admisión habilitada y administrador del sistema descargan una plantilla XLSX con las especialidades activas de la base. La hoja Motivos tiene Especialidad_ID y Motivo, sin altas de ejemplo. Las otras hojas explican el formato y muestran el catálogo. La implementación reutiliza Apache POI, ya incluido en la aplicación; referencia: https://poi.apache.org/components/spreadsheet/quick-guide.html.

La vista previa valida cada fila sin crear motivos. Muestra errores de datos, duplicados y especialidades inactivas. Si hay alguna fila correcta, permite confirmar esas filas; las erróneas quedan excluidas. Si todas fallan, no habilita la confirmación. Al guardar se revalida el catálogo y se muestra el resultado definitivo por fila, con el total importado y las rechazadas. La respuesta HTML usa redirección para que actualizar el resultado no repita las altas.

Se revisó también RF06 en el catálogo manual: admisión habilitada puede gestionar los motivos, no queda limitada a lectura. Las rutas de admisión y del administrador mantienen la comprobación del rol de sesión y, de nuevo en base, de cuenta activa y sede habilitada para PA.

## Controles

Se admite XLSX de hasta 1 MB y 200 filas. Antes de abrir el libro, el contenido ZIP se limita a 100 entradas y 8 MB descomprimidos. Se rechazan macros, vínculos externos, encabezados alterados y fórmulas o hipervínculos en las celdas de datos. No se publican archivos subidos ni se guardan sus nombres en la base. Las peticiones mantienen autenticación y CSRF; el formulario indica carga y evita el doble envío.

Una vista previa con filas correctas prepara una confirmación aleatoria ligada al actor y a su sesión. Vence en diez minutos, reemplaza la anterior y se consume una vez. El servidor conserva las filas; confirmar no acepta sustitutos desde el formulario. Un nuevo archivo descarta la confirmación anterior incluso si no es válido. El resultado visible está ligado al actor y vence en diez minutos.

Las especialidades se bloquean en orden. Los errores de validación de una fila se aíslan mediante un punto de recuperación y no impiden guardar las demás. Cada alta se guarda con su auditoría. Un fallo de infraestructura o auditoría revierte la transacción completa, sin presentar resultados de éxito. Los motivos existentes no se sobrescriben y no se añaden tablas.

## Recorrido de comprobación

PA → Médicos y especialidades → Motivos → Importar Excel. También disponible en Administrador → Configuración → Gestionar motivos. Descargar la plantilla y añadir un motivo nuevo, un duplicado y una descripción inválida. Revisar: todavía no debe aparecer el nuevo motivo en el catálogo. Confirmar las filas correctas: debe crearse el nuevo y mostrarse el error de las otras filas. Actualizar el resultado no debe repetir el alta. Desactivar el motivo de prueba al terminar.

## Evidencia

Se comprobaron lectura de Excel, archivo excesivo, macros, fórmulas, límites, confirmación de un uso, vencimiento, identidad de sesión, CSRF, protección del historial, permisos y reporte por fila. Las pruebas iniciales con política de lote completo se reemplazaron por las que verifican RNF16: guardar las filas correctas y reportar las rechazadas. La ejecución final en MySQL y la regresión integrada se registran antes de instalar esta entrega. Las métricas anteriores al ajuste no se presentan como prueba de la política corregida.

## Resultado de la comprobación final

La versión integrada ejecutó 967 pruebas sin fallos y omitió 577 casos condicionados a MySQL. Dieciocho pruebas de catálogo e importación se ejecutaron por separado en MySQL, sin fallos; el esquema y el usuario temporales se eliminaron después. Se repitieron las pruebas afectadas por el control estricto de encabezados. Once vistas de admisión y catálogo compilaron con Jasper 10.1.61 y nivel Java 11. Las tres pruebas del formulario de carga pasaron. La aprobación funcional del usuario permite publicar la entrega; las comprobaciones automatizadas no se presentan como capturas autenticadas.
