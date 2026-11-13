# Recuperación de formularios y sesiones

El borrador de confirmación adicional de logout fue sustituido por el
[gestor JWT y la coordinación de pestañas](autenticacion-jwt.md).

Una renovación de acceso conserva el CSRF de la sesión lógica. Antes de enviar
un formulario, el cliente comprueba que sigue perteneciendo a la misma identidad.
Si ya terminó la sesión vuelve al login. Si cambió la cuenta, bloquea el formulario
antiguo. Nunca repite automáticamente un registro, reserva o guardado clínico.

El servidor mantiene CSRF para los métodos que modifican datos. Los clientes JSON
reciben 403/CSRF_INVALIDO ante tokens incorrectos. Un login HTML vencido vuelve
mediante 303 al formulario; no conserva ni reenvía la contraseña. Un logout antiguo
sin sesión vuelve al login; con otra identidad vuelve mediante GET al portal actual
sin cerrar esa cuenta. GET /logout no cierra sesión y mantiene HTTP 405.

Las cookies se comparten por sitio y ruta, no se aíslan por puerto. Usar localhost:8080
para la aplicación habitual; no mezclarla con el laboratorio mediante el mismo host
y cookies equivalentes. Para dos cuentas simultáneas, utilizar perfiles separados.
