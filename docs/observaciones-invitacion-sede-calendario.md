# Invitación por sede y disponibilidad semanal

Corrección local del 04/11/2026, sin commits ni PR nuevos.

## Personal médico

El administrador elige una sede activa al crear el acceso médico. La invitación guarda `id_sede_destino`; no se crea un perfil profesional con CMP ficticio. Admisión solo lista y completa cuentas destinadas a su sede. Cambiar el identificador en una petición no permite completar un perfil de otra sede.

La cuenta permanece inactiva hasta utilizar la invitación. El formulario de edición bloquea el estado para invitaciones pendientes y el servidor rechaza activarlas desde el panel. Un acceso médico sin perfil permite al administrador corregir su sede de destino. Cambiar la sede de un perfil profesional ya completo requiere coordinar su perfil y agenda.

V022 añade una columna a `invitacion_personal`, sin añadir tablas. Es idempotente y conserva los datos: solo recupera la sede desde perfiles profesionales existentes; no inventa asignaciones para accesos antiguos sin perfil. Estos últimos deben recibir sede explícita desde el administrador. Se mantiene la migración V012 original.

Antes de desplegar esta aplicación en otra instancia se debe respaldar y ejecutar `sql/migrations/V022__sede_destino_invitacion_medica.sql`. Localmente ya se aplicó con respaldo de invitaciones; no se borraron cuentas ni perfiles.

## Reserva del paciente

El estado inicial y la selección visual usan MANANA. `GET /api/horarios?vista=semana&idEspecialidad=...&idSede=...&idMedico=...&desde=AAAA-MM-DD` devuelve el contrato común code/message/data con siete elementos fecha/manana/tarde. Sede y médico son opcionales. Los conteos reutilizan la disponibilidad reservable, con ocupaciones, retenciones y ofertas vigentes; no se infieren desde bloques publicados.

El calendario muestra cantidades para cada día según el turno. Una consulta fallida muestra «Sin verificar», y una respuesta antigua no sobrescribe otra semana. El cupo definitivo sigue requiriendo retención y confirmación transaccional.

## Validación

Regresión Java aprobada, 27 pruebas MySQL aprobadas en esquema temporal eliminado al terminar, migración ensayada dos veces, y 18 vistas compiladas con Jasper/Java 11. Pruebas JavaScript del estado de invitación, sede obligatoria, turno inicial, conteos y respuestas atrasadas aprobadas. En localhost el endpoint semanal respondió HTTP 200, CUPOS_SEMANA_OBTENIDOS y siete días. No se enviaron correos reales durante las pruebas.
