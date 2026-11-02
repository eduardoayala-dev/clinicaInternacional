# Enlaces de correo para gestionar una cita

RF10 y RF14. Los correos de reserva y recordatorio incluyen enlaces a /cita con identificador y acción de la cita. No contienen contraseñas, JWT ni un acceso que sustituya la autenticación. Se requiere iniciar sesión como paciente y se comprueba la propiedad consultando la base con el usuario autenticado. Reenviar el correo a otra cuenta no le permite leer ni modificar la cita.

GET solo abre la revisión. La confirmación o cancelación se ejecuta por POST con sesión y CSRF. No se cambia una cita por abrir el mensaje, por un escáner del proveedor de correo ni por visitar el enlace. La cancelación requiere pulsar un botón explícito en la página de revisión. Se revalida el estado y el horario al ejecutar; confirmar respeta el plazo guardado de asistencia. Una cancelación de una cita ya iniciada se rechaza también en el método compartido del portal.

Sin sesión, se conserva durante diez minutos únicamente un retorno interno tipado (cita y acción permitida). El login lo captura antes de regenerar la sesión y solo lo usa para pacientes. No acepta un parámetro returnUrl ni destinos externos. Se consume tras el login; un retorno vencido o de otro tipo no redirige. La autorización sigue siendo la cuenta autenticada, no el identificador del enlace.

Las URLs del correo se generan desde APP_PUBLIC_URL o el dominio público configurado de Railway; nunca desde el encabezado Host de una petición. El HTML escapa los parámetros de los enlaces. La respuesta de revisión no se almacena en caché y no envía el identificador en el Referer.

No se implementa un acceso anónimo mediante tokens de un solo uso: esta entrega mantiene la autenticación del portal. Tampoco crea tablas ni necesita una migración. Las acciones conservan los controles y las colas de aviso existentes. La mejora visual general de las plantillas sigue pendiente por decisión del usuario.

## Prueba manual

Reservar una cita propia futura. En el nuevo correo, abrir Confirmar asistencia sin sesión: debe pedir login y después mostrar la revisión de esa cita. Abrir la página no debe confirmar nada. Pulsar Confirmar mi asistencia y comprobarlo en Mis citas. Abrir el enlace No podré asistir: debe mostrar otra revisión y cancelar únicamente después del botón explícito. La cita debe pasar al historial y generar el aviso de cancelación.

Con otra cuenta, el mismo enlace no debe mostrar la cita ni permitir acciones. Un enlace de cita cancelada o pasada debe mostrar que ya no está disponible; confirmar fuera del plazo requiere coordinar con admisión. Prueba manual del flujo aprobada por el usuario el 2 de noviembre de 2026. Se conserva la rama y no se despliega Railway en esta fase.
