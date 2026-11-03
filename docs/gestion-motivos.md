# Gestión de motivos de consulta

HU006/RF06. Rama feature/gestion-motivos. Flujo comprobado por el usuario el 3 de noviembre de 2026; listo para revisión por PR.

El catálogo es global. Según RF06, admisión habilitada puede crear y editar motivos por especialidad activa y cambiar su estado. El administrador del sistema también puede gestionarlo. Se comprueba la identidad de sesión y, de nuevo en la base, la cuenta activa, el rol y la sede habilitada de admisión. Los POST conservan el filtro CSRF global; GET no modifica el catálogo.

Se rechazan motivos duplicados en una especialidad, aunque estén inactivos. El bloqueo de la especialidad serializa las altas y ediciones de administradores diferentes. Una descripción ya utilizada en citas no se puede sobrescribir ni trasladar a otra especialidad: se desactiva y se crea otro motivo. No se eliminan citas ni motivos. Desactivar impide futuras selecciones mediante el catálogo y las validaciones existentes de reserva y reprogramación; no cancela citas ya registradas.

Los cambios y sus registros de auditoría se guardan en una transacción. Un fallo de auditoría revierte la operación. No se añaden tablas ni se modifica el diagrama de entidades. La importación Excel de motivos sigue pendiente y no se presenta como implementada.

Las rutas /sysadmin/motivos y /admin/motivos admiten consulta HTML y JSON con el contrato code/message/data. Los filtros y la paginación se procesan en el servidor; las búsquedas escapan comodines LIKE. Las acciones HTML usan POST y redirección después de guardar. Las descripciones y mensajes se escapan al renderizar.

Accesos: administrador → Configuración → Gestionar motivos de consulta; admisión → Médicos y especialidades → Motivos de consulta. Probar un motivo nuevo, edición antes de utilizarlo, desactivación y reactivación; comprobar que un motivo usado queda protegido y que admisión habilitada puede gestionar el catálogo. Las pruebas no requieren modificar una cita real. La prueba manual debe realizarse con un motivo identificable para retirarlo al terminar.

## Evidencia local del 3 de noviembre de 2026

16 pruebas nuevas de DAO y servlet aprobadas. Nueve pruebas específicas MySQL aprobadas en un esquema temporal con usuario exclusivo, retirados al finalizar. La primera ejecución MySQL no llegó a iniciar porque Docker estaba detenido; se abrió Docker Desktop y se repitió con éxito. La regresión integrada ejecutó 939 pruebas sin fallos y omitió 568 casos condicionados a MySQL; los nueve casos del catálogo se ejecutaron por separado en MySQL. Tras separar la validación, los bloqueos, la persistencia y la auditoría en métodos específicos, se repitieron las 16 pruebas afectadas. Diez vistas de admisión y catálogo compilaron con Jasper 10.1.61 y source/target 11, sin errores. La comprobación visual autenticada queda pendiente del usuario.

Confirmación del usuario: catálogo e importación funcionan correctamente. La rama independiente se compiló y ejecutó sus pruebas antes del commit.
