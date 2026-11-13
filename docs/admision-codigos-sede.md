# Ajustes locales de admisión y códigos de sedes

Fecha: 04/11/2026. Integración de trabajo: `integration/cierre-hu01-hu15`. Cambios nuevos sin commit ni PR.

- `feature/codigo-sede-iniciales`: altas de formulario generan el código desde el nombre, sin aceptar el código enviado por el cliente. San Borja produce SED-SB; las colisiones añaden -02, -03, etc. Se serializa la asignación entre administradores y se conserva la restricción única. Los códigos existentes no se regeneran ni cambian al renombrar; Excel conserva su identificación por código.
- `fix/vistas-operaciones-admision`: estilos compartidos de admisión, sede asignada en inicio, estados vacíos reales, restauración de iconos y actualización del listado de actividad junto con las métricas. La actualización automática respeta un formulario abierto de inasistencia.
- La actividad procede de notificaciones vinculadas a citas de la sede autorizada. Se presenta un resumen administrativo con paciente, cita y fecha; no se modifica el mensaje original dirigido al paciente. No se presenta este listado como auditoría completa de todas las operaciones.
- Figma devolvió el límite del plan Starter al consultar la pantalla PA Inicio (269:84). La comparación exacta con los diseños completos queda pendiente; los ajustes visuales utilizan la captura suministrada y los componentes existentes.

Pruebas: regresión conjunta sin fallos; 26 pruebas MySQL aprobadas en esquema temporal y eliminado al terminar, incluida creación simultánea por administradores distintos; 19 vistas compiladas con Jasper/Java 11; escenarios JavaScript de vista previa, paginación y actualización dinámica aprobados. No se enviaron correos reales ni se crearon sedes en la base de la clínica durante las pruebas.
