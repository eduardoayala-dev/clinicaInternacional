# Correo gratuito con Gmail API

La cuenta Gmail dedicada al proyecto envía mediante Google con OAuth2 y HTTPS.
No requiere dominio propio ni contraseña SMTP. El conector y la herramienta de
autorización están preparados; todavía no hay autorización real, mensajes enviados,
formulario de recuperación ni confirmaciones conectadas. Railway no se cambió.

## Configuración por el propietario

1. Elegir o crear una cuenta Gmail dedicada al proyecto.
2. En https://console.cloud.google.com/ crear un proyecto y habilitar Gmail API.
   No se necesita contratar Workspace ni servicios de pago para esta integración.
3. En Google Auth Platform completar Branding y Audience. Usar External y añadir
   únicamente la cuenta Gmail remitente como usuario de prueba en Testing.
4. En Data Access solicitar sólo `https://www.googleapis.com/auth/gmail.send`.
   No se necesita leer mensajes, contactos ni borrar correos. Los pacientes no
   autorizan Google: sólo el dueño del buzón remitente.
5. En Clients crear un cliente OAuth **Desktop app**. Descargar su JSON como
   `.env.gmail-client.json` en la raíz, nunca en `config/`. Está excluido de Git
   y del contexto Docker por las reglas existentes.
6. Desde la raíz ejecutar:

   ```powershell
   python scripts/Autorizar-Gmail.py --remitente correo-del-proyecto@gmail.com
   ```

7. Autorizar la cuenta indicada en la ventana de Google. La herramienta usa
   callback local en 127.0.0.1, puerto aleatorio, state y PKCE; espera hasta cinco
   minutos. Guarda los secretos en `.env.mail` sin imprimirlos. Sólo reemplaza
   una configuración anterior después de una autorización correcta.
8. Comprobar que `MAIL_FROM` coincide con el Gmail autorizado. La herramienta no
   envía correos y deja `MAIL_MODE=disabled`. Para probar entrega real se usará
   `MAIL_MODE=gmail`. No compartir claves ni tokens en el chat.

El cliente de escritorio autoriza el buzón localmente una vez. El servidor usa
el refresh token para obtener accesos temporales; no hay callback público en Railway.

## Caducidad y límites

El uso estándar de Gmail API no tiene costo adicional, pero mantiene cuotas y
límites de envío. No es correo masivo ilimitado; excluirlo de pruebas de carga.
En External/Testing los refresh tokens con permiso Gmail caducan normalmente a
los siete días: volver a ejecutar la autorización. Pasar a In production elimina
esa limitación específica de Testing, pero no elimina requisitos de verificación
de permisos sensibles ni garantiza tokens perpetuos. Revisar las excepciones de
uso propio y las condiciones de Google; no presentar una verificación pendiente
como aprobada. Cambios de contraseña o revocación también pueden invalidar tokens.

## Configuración del servidor

`config/mail.env.example` contiene sólo nombres vacíos. Los valores privados:

| Variable | Uso |
| --- | --- |
| MAIL_MODE | disabled (predeterminado) o gmail |
| MAIL_FROM | Cuenta Gmail autorizada |
| GMAIL_CLIENT_ID | Cliente OAuth |
| GMAIL_CLIENT_SECRET | Secreto del cliente |
| GMAIL_REFRESH_TOKEN | Autorización renovable del buzón |

`compose.mail.yml` inyecta estas variables al servicio app. Añadir
`--env-file .env.mail -f compose.mail.yml` a los archivos Compose actuales,
conservando proyecto, JWT, DNI y los volúmenes. En Railway se configuran en Java,
nunca en MySQL. Añadir variables no conecta por sí solo los formularios pendientes.

El adaptador renueva y comparte accesos entre hilos. Envía MIME UTF-8 en texto;
las futuras plantillas HTML conservarán una alternativa de texto y escape de datos.
Las credenciales se envían sólo a endpoints fijos de Google, sin redirecciones.
El ID significa aceptación por Google, no entrega al destinatario. Una desconexión
de resultado incierto no dispara reenvío automático. El OAuth del buzón es
independiente del JWT de los usuarios.

## Siguientes entregas

- Recuperación: tokens aleatorios de un solo uso, hash, caducidad, límites
  persistentes, respuesta genérica y revocación de sesiones al cambiar contraseña.
- Verificación y cambio de correo: contraseña actual, verificar nuevo correo y
  avisar al anterior; cerrar también los cambios directos desde perfiles.
- Confirmaciones: cola durable después del commit de la reserva, deduplicación,
  prioridad para correos de seguridad y pruebas de entrega real.

Estos flujos necesitan migraciones, vistas y pruebas propias; aún no están
implementados por el conector. Recordatorios programados requieren resolver la
ejecución con Railway dormido. Se conservan los avisos dentro del portal. SMS y
WhatsApp automáticos no se habilitan en esta entrega gratuita.

## Fuentes oficiales (30 de octubre de 2026)

- https://developers.google.com/workspace/gmail/api/guides/sending
- https://developers.google.com/workspace/gmail/api/auth/scopes
- https://developers.google.com/identity/protocols/oauth2/native-app
- https://developers.google.com/identity/protocols/oauth2#expiration
- https://developers.google.com/workspace/gmail/api/reference/quota
- https://docs.railway.com/networking/outbound-networking
