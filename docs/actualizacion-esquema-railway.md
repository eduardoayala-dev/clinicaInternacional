# Actualización del esquema Railway

Comparación del 29 de octubre de 2026 contra la estructura exportada de `railway` y `sql/schema.sql`. No se exportaron registros. Estado: ensayo MySQL local aprobado y actualización aplicada en Railway por el usuario. El verificador devolvió vacío en columnas, restricciones e índices faltantes, y se comprobaron las seis definiciones modificadas. El usuario confirmó ingreso y carga del portal del paciente; los flujos completos de otros roles quedan fuera de esta confirmación.

## Diferencias identificadas

| Objeto | Exportación | Actualización pendiente |
| --- | --- | --- |
| Tablas | 18 de las 23 actuales | admision_cita, atencion_cita, inasistencia_cita, consulta_clinica, seguimiento_cita |
| lista_espera | Sin anticipacion_minutos ni id_medico_preferido | Agregar columnas, valores predeterminados y FK al médico preferido |
| lista_espera.id_sede | NOT NULL | Permitir NULL para inscripciones sin una sede exclusiva |
| lista_espera.canal_notificacion | ENUM nullable | VARCHAR(40) NOT NULL, predeterminado CORREO |
| reasignacion_inteligente | Sin id_cita_asignada | Agregar columna y FK a cita |
| cita.estado | Sin EN_ATENCION ni NO_ATENDIDA | Ampliar ENUM conservando los estados existentes |
| cita | Sin índice compuesto | idx_cita_medico_paciente_fecha |

Las columnas de perfil de V001 ya existen. No se pretende volver a crear registros ni importar semillas. Las cinco tablas nuevas incluyen las claves, checks e índices de las migraciones V003–V005; entre ellos la exclusión de atenciones simultáneas para médico y paciente.

Las diferencias de representación del dump (INT con PRIMARY KEY separado, defaults numéricos entre comillas, índices automáticos de claves foráneas y valores AUTO_INCREMENT) no requieren cambios. Se conserva el índice heredado cita.fk_cita_medico: el esquema nuevo puede prescindir de él al crear el índice compuesto, pero no hace falta eliminarlo para recuperar el funcionamiento. Las tablas existentes son utf8mb4_general_ci; se conservan y se usa esa codificación explícitamente en las nuevas. No se convierte la base a utf8mb4_unicode_ci.

## Archivos

- `sql/maintenance/Verificar-Railway.sql`: solo lectura. Contrasta presencia de todas las columnas, restricciones nombradas e índices explícitos del esquema. Muestra los tipos/defaults de las columnas modificadas; no demuestra equivalencia completa de todas las definiciones ni comprueba los datos.
- `sql/maintenance/Actualizar-Railway.sql`: reúne V001–V005 sobre `railway`, con comprobación de existencia del índice final. Las migraciones históricas no cambian. No contiene DROP TABLE, TRUNCATE, DELETE ni INSERT de datos.

La actualización puede repetirse para el estado exportado y el estado esperado: las adiciones comprueban existencia, las tablas usan IF NOT EXISTS y el índice tiene guardia. No es un reparador universal de tablas parcialmente creadas o restricciones modificadas. CREATE TABLE IF NOT EXISTS no repara una tabla existente con definición incompatible.

## Ensayo realizado

Ejecutado el 29 de octubre de 2026 en MySQL 8.4.13 dentro de un contenedor local sin red ni puertos publicados. Se usaron las 18 tablas exportadas y datos sintéticos en todas ellas, incluyendo tildes, Ñ y emoji. El contenedor y su volumen de ensayo se eliminaron al terminar.

- Respaldo y restauración del fixture sintético comprobados. No se descargó ni restauró el respaldo real de Railway.
- Actualización aplicada dos veces, conservando los valores de todas las columnas originales tras ambas ejecuciones.
- Comparación de columnas (tipos, orden, defaults, generación y collation), restricciones, índices, reglas referenciales, checks y columnas de claves contra las 23 tablas del esquema actual: coincide, admitiendo únicamente el índice heredado identificado.
- Rechazadas tres inserciones inválidas: referencia inexistente, atención sin fechas y dos atenciones simultáneas del mismo médico/paciente. Los dos estados nuevos se aceptan y la anticipación predeterminada es 180.
- Evidencia local en target/validacion-migracion-railway/resultado.json y TSV de metadatos. Script reproducible: `python scripts/Validar-MigracionRailway.py --estructura RUTA_AL_DUMP_DE_ESTRUCTURA.sql`.

SHA-256 del contenido UTF-8 de Actualizar-Railway.sql ensayado, normalizando los saltos a LF: `25f44b23f0f64a7bb02f64921519f610864c8b80d9a2de526804b523c93069dd`.

El usuario confirmó canales_nulos=0 y respaldo de Railway en `/var/lib/mysql/railway-respaldo-20261029T144106Z.sql` (45 KB). La existencia del archivo no acredita su restaurabilidad; el ensayo de restauración anterior es exclusivamente sintético. Guardar también una copia del respaldo fuera del volumen.

## Antes de aplicar

1. Ensayar sobre una base aislada con la estructura exportada y registros sintéticos; aplicar dos veces, comprobar estructura, conservación de registros y flujos. La exportación contiene DROP TABLE; se usa exclusivamente como fixture aislado, nunca sobre Railway.
2. Establecer una ventana sin reservas ni cambios de datos. MySQL hace commits implícitos en operaciones DDL; no existe rollback global de todo este script. Evitar escritores concurrentes y conservar el error si un paso falla; no usar mysql --force.
3. Hacer un respaldo completo antes del DDL, guardarlo también fuera del volumen y comprobar que puede restaurarse. No compartirlo en el chat.
4. Ejecutar el verificador de solo lectura y estas comprobaciones sobre la cuenta administrativa:

```sql
USE railway;
SELECT DATABASE();
SELECT COUNT(*) AS canales_nulos
FROM lista_espera WHERE canal_notificacion IS NULL;
SELECT estado, COUNT(*) AS cantidad FROM cita GROUP BY estado;
```

Si canales_nulos no es cero, detenerse y acordar cómo completar esos datos antes de imponer NOT NULL. No sustituir valores automáticamente. Si el esquema cambió después de la exportación, volver a compararlo antes de aplicar.

5. Ejecutar la actualización completa con una cuenta administrativa independiente; Java conserva clinica y sus permisos limitados. No ejecutar schema.sql ni la exportación de estructura.
6. Repetir el verificador: las consultas de faltantes deben quedar vacías, y revisar los tipos mostrados. Comprobar conteos de registros existentes antes/después e ingresar como paciente, personal y médico; verificar reservas, lista de espera, admisión, atención y seguimientos.

Un error al aplicar requiere detenerse y revisar qué pasos completó MySQL. Conservar el respaldo y no intentar restaurarlo sobre la base activa sin revisar primero el plan de recuperación. Las cinco tablas nuevas empezarán vacías: no se inventan registros de asistencia ni consultas clínicas para citas antiguas.
