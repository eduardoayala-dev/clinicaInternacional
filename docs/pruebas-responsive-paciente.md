# Portal del paciente en móvil y tableta

Rama: `fix/responsive-portal-paciente`. La versión base publicada continúa siendo `v1.0.0`; no se mueve ese tag ni se crea un tag adicional para esta corrección.

## Comportamiento

Hasta 900 px, la navegación se abre desde el botón de menú. Se cierra con Escape, el botón de cierre, el fondo o la selección de un enlace. El panel recibe el foco y lo mantiene dentro mientras permanece abierto; al cerrarse, el foco vuelve al botón. El contenido de fondo queda inactivo durante la apertura. En escritorio se conserva el menú lateral.

Los formularios se organizan en una columna cuando el espacio es reducido. El calendario y las tablas permiten desplazarse dentro de su contenedor para conservar la lectura de los datos. La página completa no necesita desplazamiento horizontal. Las áreas de desplazamiento tienen nombre accesible y pueden recibir el foco. Se respeta la preferencia de movimiento reducido.

## Verificación

- Compilación Jasper de las ocho JSP del paciente: sin errores.
- Regresión JavaScript de acceso, recuperación y reservas API: 13 pruebas aprobadas.
- Navegador Chrome con cuenta sintética en una copia MySQL local aislada: inicio, programar cita, historial, lista de espera, notificaciones, perfil y reprogramación a 320, 390, 768 y 1280 px.
- Comprobaciones de ancho del documento y campos, respuestas HTTP, apertura y cierre del menú, Escape, retorno de foco y ciclo inverso de Tab.
- Recorrido adicional del formulario de búsqueda y calendario sin seleccionar un cupo ni crear reservas. La apertura de reprogramación no confirma cambios en la cita.

Las capturas y resultados se guardan en `target/qa-responsive`; no contienen cuentas reales. La comprobación automatizada se limita al ensayo local y no acredita por sí sola todos los dispositivos físicos ni todos los estados del flujo clínico.

## Repetir la comprobación

Requiere Node, Chrome y `puppeteer-core@25.16.0` instalado en `target/auditoria-web`. Definir `QA_URL` con la URL local, `QA_PACIENTE_DNI` y `QA_PACIENTE_PASSWORD` con una cuenta sintética que tenga una cita futura reprogramable. Ejecutar:

```powershell
node scripts/rendimiento/web/verificar-responsive.mjs
```

No se ejecutan confirmaciones de reserva, cambios de perfil, cancelaciones, consultas de DNI, orientación externa ni envíos de correo.
