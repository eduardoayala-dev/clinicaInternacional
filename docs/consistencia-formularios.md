# Consistencia visual de los formularios

Referencia interna: modal Nuevo paciente de admisión. Referencias externas: https://design-system.service.gov.uk/components/notification-banner/ y https://design-system.service.gov.uk/styles/spacing/ . Se toma la organización de contenido y espaciado; no se incorpora una dependencia nueva.

Avisos de operación con superficie blanca, borde neutro y el texto/icono existente. Se conservan los mensajes y roles accesibles. Los errores de validación y advertencias clínicas mantienen su significado; no dependen solo del color. Se retira la etiqueta decorativa Agenda médica.

Mis horarios usa dos columnas, separación de 24 px y pie de acciones independiente. Nuevo usuario alinea rol/estado y muestra la explicación de activación en una fila completa. Priorización elimina etiquetas redundantes y organiza campos, ayudas y acciones con los mismos espacios. Formularios de una columna en móvil.

Estilos compartidos en portal-forms.css, sin modificar reglas de negocio ni validaciones.

## Revisión ampliada

- Se elimina el límite de ancho del contenedor desplazable de Priorización y de la tarjeta de propuesta de horarios para conservar la alineación de la página.
- Se sustituye el foco universal por foco en controles interactivos. El estilo compartido usa contorno de 2 px, sin acumular sombras y bordes gruesos. Se conserva la navegación por teclado.
- Se excluyen casillas, radios y campos ocultos de las alturas mínimas de campos de texto.
- Se corrige la cuadrícula de dos indicadores del dashboard, que usaba cinco columnas.
- Se revisan los avisos en línea de admisión, los accesos de seguridad del médico y los estilos de autenticación, recuperación y catálogos.
- Se amplían los ajustes para modales, acciones, paginación y columnas en pantallas estrechas.

Verificación: compilación completa de las JSP mediante Jasper, sin errores; comprobación de activación pendiente/sede del formulario de usuarios aprobada; diff sin errores de espacios. No se presenta esta comprobación como prueba visual completa: el acceso al navegador conectado está deshabilitado en esta sesión. La revisión final de escritorio/móvil queda pendiente en el navegador del usuario.
