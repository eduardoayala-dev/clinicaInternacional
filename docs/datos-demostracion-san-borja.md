# Datos de demostración: Clínica Internacional, San Borja

Se mantiene una sola sede, `SED-SB`, y el nombre Clínica Internacional. Las identidades, documentos, teléfonos e historias de pacientes y médicos son datos simulados. Los correos de esos perfiles terminan en `@clinica.example.test`; no son destinatarios reales. Las direcciones de contacto del establecimiento y las cuentas de gestión son las indicadas para el proyecto.

## Contenido

- Una cuenta de administrador: `eduardo@admin.com`.
- Una cuenta de admisión, asignada a San Borja: `eduardo@admision.com`.
- 26 médicos activos, 13 especialidades y 39 motivos de consulta.
- 180 pacientes con identidades inventadas.
- Propuestas y bloques de mañana/tarde, de lunes a sábado, para 60 días anteriores y 30 días desde la fecha de generación.
- Citas atendidas, canceladas y no asistidas con sus registros relacionados; reservas futuras y solicitudes en lista de espera.
- Las citas del día se clasifican según la hora de Perú; no se inventan consultas en curso.

La contraseña de gestión se recibe por `DEMO_ADMIN_PASSWORD`. Se guarda solo su hash BCrypt con sal aleatoria. Las cuentas simuladas de médicos y pacientes tienen una contraseña aleatoria que no se entrega ni se conserva. El usuario puede registrar sus propias cuentas para las pruebas de extremo a extremo.

No se fabrican probabilidades de IA: los riesgos quedan sin calcular hasta que intervenga el servicio correspondiente. Las colas de correo de las citas precargadas se marcan como omitidas. No representan correos enviados y no generan envío masivo hacia datos simulados.

## Ensayo local aislado

1. Empaquetar la aplicación con Maven.
2. Establecer `DEMO_ADMIN_PASSWORD` solo durante la ejecución.
3. Ejecutar `node scripts/demo/ensayar-local.cjs` desde la raíz de esta rama.
4. Revisar `target/demo-san-borja/resultado.json` y entrar en `http://127.0.0.1:8084/login/personal`.

Se usa una base `clinica_agendas_test_demo_*` y un usuario limitado a esa base. La aplicación habitual de `localhost:8080` y su base `clinica_db` se conservan. El ensayo no carga proveedores externos ni activa correo. Para evitar mezclar sesiones entre las instancias, revisar el ensayo en una ventana privada.

Las comprobaciones incluyen relaciones referenciales, disponibilidad compatible, ausencia de doble reserva de paciente/médico, motivos por especialidad, ausencia de citas pasadas activas, credenciales de gestión y respuestas autenticadas de las pantallas principales.

## Preparar el archivo de Railway

Después del ensayo satisfactorio:

```text
node scripts/demo/verificar-paquete.cjs
node scripts/demo/preparar-paquete.cjs railway
```

Los archivos privados se generan en `target/demo-san-borja/`, fuera de Git. `carga-san-borja-railway.sql` reemplaza los datos; no debe ejecutarse en la base habitual local. `preparar-archivo-railway.sh` solo reconstruye el SQL en el volumen de MySQL y verifica su SHA-256; **no ejecuta la carga**.

El paquete comprueba el nombre de la base y el inventario de tablas InnoDB. Ejecuta la sustitución dentro de una transacción y verifica las relaciones antes de confirmar. Ante una excepción, revierte la transacción. Esto no sustituye el respaldo.

## Operación desde el panel de Railway

Primero comprobar que la aplicación y el esquema en línea incorporan las migraciones de la versión que se va a demostrar. La carga no despliega código ni reemplaza migraciones.

Antes de aplicar el SQL:

1. Pausar temporalmente el servicio Java para evitar escrituras y procesamiento de colas durante la sustitución.
2. Abrir la terminal del servicio **MySQL**. Confirmar que la base de la aplicación se llama `railway`.
3. Crear el respaldo en el volumen persistente:

```bash
umask 077
respaldo="/var/lib/mysql/railway-antes-demo-$(date -u +%Y%m%dT%H%M%SZ).sql"
MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -u root \
  --single-transaction --routines --triggers --events \
  --set-gtid-purged=OFF --default-character-set=utf8mb4 railway > "$respaldo"
test -s "$respaldo"
ls -lh "$respaldo"
```

Conservar una copia externa del respaldo cuando sea posible. Su contenido puede incluir datos y tokens anteriores; no publicarlo en Git ni en el chat.

4. Preparar el SQL con el archivo generado y guardar la ruta que muestra. Para ejecutarlo, sustituir la ruta del ejemplo por esa ruta exacta:

```bash
MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 \
  -u root railway < /var/lib/mysql/demo-san-borja.XXXXXXXX.sql
```

Si hay un error, no continuar como si la carga hubiese terminado. Conservar el mensaje y el respaldo; el procedimiento revierte los cambios de datos si falla. No desactivar las comprobaciones del paquete para forzar una estructura diferente.

5. Reanudar Java e iniciar sesión de nuevo. Comprobar administrador, admisión, listado de médicos, pacientes, dashboard y disponibilidad de citas.

Para restaurar el respaldo, con Java aún pausado, usar el mismo cliente MySQL con el archivo de respaldo como entrada. No borrar el volumen ni recrear el servicio MySQL.

## Versiones y rendimiento

Usar la misma fecha base y el mismo conjunto generado para las mediciones comparables. El archivo `resultado.json` registra los conteos y la hora. Después de confirmar el cierre funcional se fijarán los tags de Selenium, carga/estrés y Lighthouse/PageSpeed, manteniendo los reportes originales. La carga de demostración no es una medición de capacidad.

Para retirar solo el ensayo local después de revisarlo:

```text
node scripts/demo/limpiar-ensayo.cjs
```

Este comando valida el nombre del contenedor, base y usuario temporales antes de eliminarlos. No borra `clinica_db` ni toca Railway.
