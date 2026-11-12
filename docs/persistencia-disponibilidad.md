# Persistencia del catálogo y ciclo de citas con JPA

## Alcance de la migración

La rama `feature/persistencia-jpa` reemplaza el enfoque intermedio de Spring JDBC.
`MedicoDAO` delega la lectura del catálogo de médicos, especialidades y sedes, las
fechas y los horarios diarios, el calendario semanal y el conteo de cupos a
repositorios JPA con Hibernate. No contiene SQL manual ni `PreparedStatement`.
Los motivos de consulta también se leen mediante `MotivoCitaRepository` y Criteria API.
La migración incluye la retención de cupos, reserva, reprogramación y cancelación
del paciente, así como las lecturas de Mis citas e historial. `AgendaCitaDAO` y
`CitaDAO` ya no contienen SQL manual. Sus contratos públicos se conservan.
Los catálogos administrativos, autenticación, procesamiento de correo, lista de
espera y otros módulos continúan usando JDBC mientras avanza su migración.

## Implementación

| Componente | Responsabilidad |
| --- | --- |
| `persistence/entity` | Mapear las tablas existentes y sus relaciones para las lecturas del catálogo. |
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
sin convertirlos a `java.sql.Timestamp`. Se comprueba con MySQL y una JVM en UTC
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
mvn "-Dtest=MedicoCatalogoDaoTest,CatalogosApiServletTest,DisponibilidadServletTest" test
mvn test
mvn -DskipTests package
py scripts/Probar-MySql.py --solo-catalogos
py scripts/Probar-MySql.py --solo-ciclo-citas
```

Los casos del catálogo comprueban permisos implícitos por estados activos, separación
por sede, fechas, cruces de intervalos, ofertas y retenciones. También verifican la
misma cantidad de consultas y el rollback de una cita insertada sin commit en la
conexión del llamador. Las pruebas de integración usan una base MySQL temporal
`clinica_agendas_test_*`, separada de los datos locales de la clínica.
La primera etapa de catálogo aprobó 1.045 pruebas generales y una suite adicional
de 120 casos con MySQL. Tras incorporar el ciclo de citas, la suite general
completó 1.047 pruebas sin fallos y omitió 613 por condiciones de entorno.
Las pruebas específicas de autorización, reserva, historial y avisos completaron
135 casos sin fallos. La integración ampliada en MySQL completó 307 pruebas sin fallos ni omisiones,
incluyendo reservas concurrentes, atención, admisión, correos, ofertas e historial.
Las tablas temporales de auditoría se ajustaron a la clave primaria real del
esquema para probar la generación de identificadores JPA.

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

Referencias: [Hibernate 6.6 y compatibilidad](https://hibernate.org/orm/releases/6.6/),
[gestión de conexiones](https://docs.hibernate.org/orm/6.6/javadocs/org/hibernate/SessionBuilder.html).
