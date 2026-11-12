# Persistencia del sistema con JPA

## Alcance de la migración

La rama `feature/persistencia-jpa` reemplaza el enfoque intermedio de Spring JDBC.
Todos los módulos de producción delegan el acceso a datos a repositorios JPA con
Hibernate y Criteria API. No hay SQL manual, consultas nativas, cadenas JPQL ni
`PreparedStatement` en `src/main/java`.

La migración incluye autenticación y revocación de sesiones, verificación de
cuentas, invitaciones y perfiles profesionales, catálogos e importaciones,
publicación de horarios, disponibilidad y retención de cupos, ciclo de citas,
registro de llegada y atención, historial y seguimientos, prioridad clínica,
lista de espera y ofertas, notificaciones, colas de correo y reportes.
Los contratos de los DAO y las rutas del sistema se conservan.

Los archivos SQL de creación, migración, respaldo y pruebas siguen siendo
necesarios para administrar MySQL. HikariCP y el driver JDBC siguen transportando
las operaciones generadas por Hibernate; usar ORM no elimina el driver.
La comprobación del esquema lee metadatos JDBC y valida la configuración y las
restricciones mediante repositorios, sin crear ni modificar tablas.

## Implementación

| Componente | Responsabilidad |
| --- | --- |
| `persistence/entity` | Mapear las tablas existentes, relaciones, proyecciones y escrituras del sistema. |
| `PersistenciaJpa` | Compartir las fábricas de Hibernate por motor y abrir sesiones sobre la conexión de la operación. |
| `MotivoCitaRepository` | Filtrar motivos activos de especialidades activas. |
| `CatalogoMedicoRepository` | Proyectar los datos de médicos, especialidades y sedes mediante Criteria API. |
| `DisponibilidadRepository` | Aplicar los filtros de agenda, citas, ofertas y retenciones con Criteria API. |
| `MedicoDAO` | Mantener los contratos del portal y calcular los intervalos disponibles. |
| `ReservaCitaRepository` | Bloquear paciente y médico, retener cupos y guardar citas, confirmaciones, avisos y auditoría. |
| `ConsultaCitaRepository` | Consultar citas próximas, históricas, canceladas, detalles e identificadores. |
| `TransaccionJdbc` | Conservar la interfaz de los DAO existentes y delegar commit/rollback a la unidad de trabajo JPA. |

Las consultas se construyen con Criteria API; Hibernate genera el SQL y enlaza los
valores como parámetros. Se han retirado Spring JDBC y los cinco archivos SQL del
enfoque anterior. No se usan consultas nativas ni cadenas JPQL en estos repositorios.

La aplicación conserva HikariCP. Las sesiones reciben la conexión de la operación;
no se crea un segundo pool y el cierre de sesión no cierra esa conexión.
Las lecturas independientes usan sesiones de solo lectura con flush manual.
Las escrituras comparten una sesión y transacción JPA por operación. Si falla una
confirmación, notificación, correo pendiente o auditoría, también se revierte la
cita. Los eventos en vivo se emiten después de confirmar la transacción.

Los bloqueos `PESSIMISTIC_WRITE` preservan la exclusión entre reservas del mismo
médico y la validación de pertenencia al paciente. El cambio de estado de una cita
mantiene filtros por propietario, estado previo y fecha futura. Las entidades de
bloqueo mapean únicamente las columnas que necesitan validar y son inmutables.
Las entidades de escritura conservan las columnas y claves generadas del esquema.

La fábrica se comparte por motor y se cierra al detener la aplicación.
`hibernate.type.java_time_use_direct_jdbc=true` conserva los valores `java.time`
sin desplazarlos al leer y escribir. Las conversiones temporales de agregaciones se realizan dentro de la consulta generada, no alteran el huso de los datos. Se comprueba con MySQL y una JVM en UTC
usando una conexión configurada para America/Lima, evitando desplazar horarios y
vencimientos.

`hibernate.hbm2ddl.auto=none` y
`jakarta.persistence.schema-generation.database.action=none` impiden que Hibernate
cree, cambie o elimine tablas. No es necesario importar datos ni modificar las
variables de conexión para desplegar este bloque en Railway.

## Reglas conservadas

- Médico, usuario, sede y especialidad activos en las búsquedas reservables.
- Disponibilidad publicada y perteneciente a la sede actual del médico.
- Exclusión de horas pasadas, citas ocupadas, ofertas y retenciones vigentes.
- Oferta válida únicamente para un paciente activo en una lista ofertada.
- Filtros por sede y médico aplicados en la base de datos.
- Agrupación semanal en cinco consultas y conteo de admisión en cuatro, sin una consulta por médico.
- Compatibilidad con la transacción del llamador y el contrato de errores del catálogo.

## Verificación

```powershell
mvn test
py scripts/Probar-MySql.py --toda-persistencia
mvn -DskipTests package
```

Las pruebas MySQL se ejecutan en un contenedor temporal con TLS y esquema
`clinica_agendas_test_*`, separado del volumen y de los datos locales de la clínica.
El ejecutor retira únicamente ese contenedor temporal al terminar.

Se comprueban autorización por usuario y sede, estados y fechas, filtros y
paginación, revocación, códigos de un uso, reservas y ofertas concurrentes,
rollback de escrituras compuestas e idempotencia de las colas de correo.
Los reportes conservan agrupaciones en la base de datos, y los contactos de las
citas se consultan por lote para evitar una consulta por fila.

Las tablas temporales de auditoría y notificaciones se ajustaron a las claves
primarias reales del esquema para comprobar los identificadores generados por JPA.
Resultados de la migración completa, comprobados el 12 de noviembre de 2026:

- Regresión general: 1.669 casos declarados, 1.054 ejecutados y 615 omitidos por condiciones de entorno; sin fallos ni errores.
- Integración MySQL con TLS: 614 casos ejecutados, sin fallos, errores ni omisiones.
- Compilación y empaquetado del WAR: correctos.
- Comprobación HTTP local: disponibilidad y logins, perfil e historial del paciente, padrones y seguimiento de admisión, usuarios, sedes, analítica y priorización del administrador responden HTTP 200 con las cuentas correspondientes.
- Revisión estática de `src/main/java`: sin SQL manual, consultas nativas ni llamadas a `PreparedStatement`.

La integración amplia detectó una tabla de reporte que impedía limpiar el esquema
temporal entre suites. Se completó la lista de tablas de prueba y se repitió toda
la integración con resultado correcto. Esto no modificó la base local ni Railway.
El usuario confirmó que cargan Mi agenda, Mis pacientes, Seguimientos y Mis horarios en el servidor local actualizado. Los casos automatizados del portal y del historial también están incluidos.

## Comparación de código

El punto anterior se conserva en el tag
[antes-persistencia-disponibilidad](https://github.com/eduardoayala-dev/clinicaInternacional/tree/antes-persistencia-disponibilidad).
El PR #83 de Spring JDBC no debe presentarse como la migración definitiva a ORM.
La etiqueta del después se publicará sobre la integración de la versión JPA probada.
No se atribuye una mejora de latencia a este cambio sin repetir las mediciones.

## Prueba manual

1. Entrar como paciente y consultar sede, especialidad y médico.
2. Comparar el número de cupos de mañana y tarde con los horarios del día.
3. Reservar un horario y comprobar que desaparece y que se actualiza el conteo.
4. Reprogramar la cita y comprobar que se libera el horario anterior y se ocupa el nuevo.
5. Cancelar con motivo y revisar que el cupo se libera y la cita aparece en Canceladas.
6. Revisar los avisos y el contador de cupos de admisión para la misma sede.
7. Registrar llegada, iniciar y cerrar atención como médico; revisar historial y seguimiento.
8. Inscribir una lista de espera y aceptar o rechazar una oferta válida.
9. Comprobar invitaciones, activación y recuperación con las cuentas de prueba.
10. Consultar reportes y exportaciones por sede y fechas; comprobar que los totales coinciden.

Referencias: [Hibernate 6.6 y compatibilidad](https://hibernate.org/orm/releases/6.6/),
[gestión de conexiones](https://docs.hibernate.org/orm/6.6/javadocs/org/hibernate/SessionBuilder.html).
