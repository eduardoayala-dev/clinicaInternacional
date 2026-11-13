# Estado de integración y pendientes

Revisión: 3 de noviembre de 2026. Este documento sustituye el diagnóstico inicial del 31 de octubre, que describía defectos ya corregidos en entregas posteriores. La matriz detallada está en [seguimiento-requisitos.md](seguimiento-requisitos.md).

## Entregas de este bloque

- PR65: gestión de motivos por especialidad.
- PR66: importación Excel por fila, confirmación de un uso y auditoría.
- PR67: seguimiento de IA, ofertas reales por sede, navegación de admisión, paginación y edición/exportación del padrón médico.
- PR68: iconos distintos en el menú médico.
- Laboratorio de rendimiento: scripts, colección Postman y perfiles propuestos, sin resultados de capacidad acreditados.

Las ramas se conservan. Publicar o fusionar código no acredita por sí mismo rendimiento, disponibilidad ni precisión de un modelo.

## Ciclo de citas

Existen reserva con retención real, confirmación de asistencia separada, reprogramación, cancelación, llegada por admisión, atención, cierre, inasistencia explícita y oferta de cupos liberados. No se infiere inasistencia automáticamente al cambiar de día. El motivo obligatorio de cancelación y cuatro recorridos completos sobre MySQL real están comprobados en feature/motivo-cancelacion; el usuario aprobó la prueba local el 4 de noviembre de 2026. Falta integrar la entrega y validar el despliegue final.

## Pendientes principales

- IA específica de aceptación de ofertas y evaluación representativa del modelo de inasistencia; el entrenamiento sintético no acredita precisión real.
- Importación de sedes, consultorios y ampliaciones de horarios conforme al alcance acordado.
- Criterios centralizados de priorización y protección contra postergación.
- Revisión de indicadores y exportaciones del administrador del sistema.
- Uniformar políticas de contraseña/inactividad y documentar las diferencias con los requisitos originales.
- Evidencias comparables de Selenium, carga/estrés, Lighthouse, accesibilidad y móvil.
- Médico y paciente con una misma cuenta: aplazado por decisión del usuario.

No se declara el sistema terminado ni se asigna un porcentaje de cumplimiento sin verificar los criterios de cada HU.
