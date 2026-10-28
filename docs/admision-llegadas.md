# Registro de llegada por admisión

La llegada corresponde al personal administrativo de la sede. El servidor toma la hora de Lima, registra al usuario responsable y cambia la cita de `CONFIRMADA` a `EN_SALA` en una sola transacción. Un error revierte la operación; una segunda solicitud responde `409` y conserva la primera hora registrada.

El perfil se vuelve a consultar en MySQL: usuario activo, rol administrativo y sede activa. La cita debe pertenecer a esa sede y al día clínico actual. Una llegada anticipada del mismo día es válida. Los pacientes, médicos y perfiles deshabilitados reciben `403`; una cita de otra sede o inexistente recibe `404`.

La tolerancia propuesta es de 10 minutos y se configura con `ADMISION_TOLERANCIA_MINUTOS`, entre 0 y 120. Una llegada exactamente en el límite se considera dentro de la tolerancia. El valor utilizado queda guardado con cada llegada para conservar el criterio histórico. La marca de retraso informa al médico; la llegada tardía mantiene al paciente en sala.

## Instalación

Para una base existente, realizar un respaldo y ejecutar `sql/migrations/V003__registro_llegada.sql` antes de desplegar esta versión. La migración puede repetirse y crea únicamente `admision_cita`. Para un volumen nuevo, `sql/schema.sql` ya incluye la tabla. El usuario de la aplicación mantiene sus permisos de datos; las migraciones se ejecutan con la cuenta administrativa de MySQL.

Los registros anteriores conservan sus estados. La hora de llegada desconocida se presenta como «Sin registrar». Las cuentas y citas existentes se conservan. Para restaurar únicamente el código anterior puede mantenerse la tabla adicional; retirar la tabla elimina el registro de llegadas y requiere un respaldo previo.

## Contrato

`POST /admin/admision`, con la cookie de sesión, `csrfToken` e `idCita` en formulario. Para JSON, enviar `Accept: application/json`. Los identificadores de usuario, la sede y la fecha enviados por el cliente no determinan la autorización ni la hora registrada. La ruta anterior `POST /admin/dashboard?action=admitir_cita` delega al mismo controlador.

Respuesta `200`:

```json
{
  "code": "LLEGADA_REGISTRADA",
  "message": "Llegada registrada. El paciente está en sala.",
  "data": {
    "idCita": 7,
    "fechaLlegada": "2026-10-28T09:05",
    "llegoTarde": false,
    "toleranciaMinutos": 10
  }
}
```

Errores conservan `{code,message,data}`: `400` para identificador inválido, `403` para sesión/rol/CSRF/perfil inválido, `404` para cita fuera de la sede o inexistente, `409` para estado o día incompatible y `503` para un fallo transaccional. `GET` en la ruta dedicada responde `405`, sin modificar datos.

El portal médico expone `fechaLlegada`, `horaLlegada`, `llegadaRegistrada`, `llegoTarde` y `toleranciaMinutos` en su agenda y detalle propios. Inicio y agenda muestran la llegada registrada. El tablero de admisión muestra los estados persistidos, oculta la admisión de citas terminales y actualiza la tabla mediante una consulta real, conservando sus filas durante la carga. La actualización automática sigue el intervalo de cinco minutos; también existe actualización manual.

## Comprobación local

Importar `docs/postman/Admision-Local.postman_collection.json`, completar las variables de cuentas y citas sintéticas, habilitar cookies y desactivar el seguimiento de redirecciones. La colección registra una llegada una sola vez: utilizar una cita confirmada de hoy que aún no tenga llegada. La variable `idCitaManual` queda para revisión del usuario.

1. Ingresar como admisión y buscar una cita confirmada de la sede para hoy.
2. Pulsar «Registrar llegada»: debe aparecer la hora real y desaparecer el botón de llegada.
3. Actualizar la sala y volver a ingresar: el registro debe conservarse.
4. Abrir la agenda del médico asignado: debe mostrar la misma llegada, con la marca de retraso cuando corresponda.
5. Comprobar con la colección que repetir la llegada da `409` y que una cita cancelada no puede admitirse.

Las pruebas JDBC cubren roles vigentes, sede, límites de tolerancia, día de Lima, estados inválidos, rollback, lectura médica propia y solicitudes simultáneas. El contrato se ejecuta sobre H2 de pruebas y una base MySQL temporal aislada.

Validación local del 28/10/2026: regresión Java sin fallos, 27 casos del contrato en MySQL real, siete pruebas del controlador de admisión, 30 pruebas JavaScript y 25 comprobaciones en 15 solicitudes de Postman. La revisión en Edge comprobó el formulario anterior, persistencia, consulta real al actualizar, conservación de filas ante error de red, filtros y salida segura de texto HTML. El diseño móvil del tablero administrativo conserva limitaciones de ancho y requiere una entrega de estilos.

El inicio formal, el cierre y los casos de inasistencia y no atención se describen en [flujo-atencion.md](flujo-atencion.md). La migración V004 completa las restricciones de una consulta activa por médico y paciente.
