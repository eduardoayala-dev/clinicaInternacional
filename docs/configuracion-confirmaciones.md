# Configuración de confirmación y llegada

HU010, HU012 y HU018; RF10, RF12 y RF18. Esta entrega centraliza dos parámetros existentes; el programador de recordatorios y los intentos mínimos antes de cancelar siguen pendientes.

V016 añade confirmacion_horas (1–168, inicial 24) y tolerancia_llegada (0–120, inicial 10) a configuracion_ofertas. No crea una tabla nueva ni reinicia el plazo de las ofertas. La fila y su versión son compartidas por el formulario de configuración. Solo un administrador activo puede guardar; se valida la versión para evitar que una pestaña antigua sobrescriba cambios.

El guardado de los cuatro parámetros y su auditoría ocurre en una transacción. Si falla la auditoría, se revierte el cambio. El registro contiene actor, fecha y valores anterior/nuevo.

La anticipación para confirmar se lee al crear cada cita. El vencimiento es la hora de la cita menos las horas configuradas. Si ese límite ya pasó al reservar, el vencimiento queda en la propia hora de la cita, evitando crear un plazo que ya haya vencido. Cambiar la configuración no modifica confirmacion_asistencia existente. Reprogramar crea un plazo nuevo conforme a la configuración vigente.

La tolerancia se consulta al registrar una llegada o comprobar una ausencia. Exactamente en el límite todavía se considera dentro de tolerancia. Cada llegada y cada inasistencia conserva el número de minutos usado, incluso si cambia después la configuración. Nunca se marca inasistencia automáticamente.

Las variables ADMISION_TOLERANCIA_MINUTOS y CONFIRMACION_CITA_HORAS_ANTES dejan de ser la fuente de estos parámetros; se retiran de los ejemplos y Docker Compose para evitar valores contradictorios.

## Prueba local

1. Entrar como administrador y abrir Configuración de priorización.
2. Guardar una anticipación de confirmación y tolerancia válidas; salir y volver a entrar: deben conservarse.
3. Comprobar que una cita reservada antes mantiene su plazo, y una nueva usa el nuevo valor.
4. Como admisión, comprobar una llegada respecto a la tolerancia configurada; la llegada ya registrada no cambia al modificarla.
5. Abrir el formulario en dos pestañas. Después de guardar en una, guardar la antigua debe pedir recargar, sin sobrescribir.

Aplicar V016 con respaldo antes de reemplazar el WAR. Readiness comprueba las columnas; no se inventan valores si falta la migración. No se despliega Railway desde esta entrega.
