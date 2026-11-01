# Alta del personal por invitación

El administrador del sistema crea médicos, personal de admisión y otros administradores desde **Usuarios**. La cuenta queda inactiva y recibe un enlace para crear su contraseña. No se entrega una contraseña compartida.

El enlace vence a los 30 minutos y solo puede usarse una vez. Reenviar genera otro enlace e invalida el anterior; hay una espera de 60 segundos entre envíos. Revocar impide activar la invitación. Si Gmail no confirma el envío, la cuenta permanece pendiente y el administrador puede reenviar después.

La contraseña debe tener al menos 12 caracteres, mayúscula, minúscula, número y símbolo. Activar la cuenta no inicia sesión automáticamente. El correo y el rol no se cambian mediante la edición de datos del personal.

## Configuración

Aplicar `sql/migrations/V012__invitacion_personal.sql` a una base existente, después de respaldarla. No ejecutar el script completo de creación sobre datos existentes.

Configurar Gmail con las variables ya utilizadas por el sistema y `APP_PUBLIC_URL` con la dirección pública de la aplicación. Localmente se usa `http://localhost:8080`. En producción se exige HTTPS. El dominio de la petición no determina los enlaces enviados.

La base guarda únicamente el hash del enlace. El navegador retira el secreto de la dirección antes de enviarlo por POST protegido. El envío se intenta después de confirmar la transacción; un fallo del proveedor no activa la cuenta ni elimina su registro.

## Permisos de admisión

Las operaciones y los padrones de admisión se limitan a la sede del usuario autenticado. No basta con cambiar un identificador en el formulario. Las cancelaciones comprueban sede, fecha y estado dentro de la transacción. Un médico con citas futuras pendientes o una atención en curso no puede desactivarse hasta resolverlas.

## Prueba manual local

1. Entrar como administrador del sistema y abrir **Usuarios → Nuevo usuario**.
2. Usar un correo que controles y completar el perfil del personal con sede y especialidad vigentes, si corresponde.
3. Enviar la invitación y comprobar que la cuenta figure pendiente y no permita iniciar sesión.
4. Abrir el correo en una ventana privada, crear una contraseña válida e iniciar sesión en el portal de personal.
5. Volver a abrir el enlace utilizado: no debe permitir otra activación.
6. Para otra invitación pendiente, comprobar que reenviar invalide el enlace anterior y que revocar impida su activación.

Las pruebas automatizadas utilizan emisores de correo falsos y bases aisladas. Esta mejora no cambia las contraseñas de cuentas existentes ni completa las funciones pendientes de otros requisitos.
