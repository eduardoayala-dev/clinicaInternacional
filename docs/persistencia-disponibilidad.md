# Persistencia de disponibilidad con Spring JDBC

La búsqueda semanal del paciente consulta las agendas publicadas y calcula los cupos
libres por mañana y tarde. El inicio de admisión reutiliza el mismo cálculo para su
contador de disponibilidad.

## Versiones para revisar

- [Código anterior](https://github.com/eduardoayala-dev/clinicaInternacional/tree/antes-persistencia-disponibilidad), tag sobre `bfee4e6`.
- La mejora se desarrolla en `feature/persistencia-disponibilidad`.
- El tag `despues-persistencia-disponibilidad` se publicará al integrar el PR comprobado.

## Cambios de código

| Archivo | Responsabilidad |
| --- | --- |
| `src/main/java/pe/edu/utp/clinica/repository/DisponibilidadRepository.java` | Ejecutar las cinco consultas con `NamedParameterJdbcTemplate` de Spring JDBC y mapear filas a registros tipados. |
| `src/main/resources/sql/disponibilidad/*.sql` | Definir las consultas completas con parámetros con nombre y sin composición de fragmentos. |
| `src/main/java/pe/edu/utp/clinica/dao/MedicoDAO.java` | Coordinar la búsqueda y conservar el cálculo de intervalos y cupos. |
| `pom.xml` | Incorporar Spring JDBC 6.2.19, compatible con Java 17. |

El recorrido es `ApiHorariosServlet → CatalogoService → MedicoDAO →
DisponibilidadRepository → MySQL`. Las consultas se cargan desde recursos del
classpath incluidos en el WAR. Los nombres de recursos proceden de constantes
internas del repositorio.

El repositorio recibe la conexión de la operación y la adapta con
`SingleConnectionDataSource(conexion, true)`. Spring administra sentencias y
resultados. El llamador conserva el control de cierre, commit y rollback de esa
conexión. Cada instancia del repositorio pertenece a una operación y no se
comparte entre peticiones.

## Seguridad y comportamiento

La consulta anterior ya asignaba los valores mediante `PreparedStatement`. En
esta mejora los parámetros se expresan como `:especialidad`, `:sede`, `:medico`,
`:desde`, `:hasta`, `:hoy` y `:ahora`. Spring los enlaza a la sentencia preparada.
La API valida identificadores y fechas antes de consultar.

Se conservan las condiciones de médico, usuario, especialidad y sede activos,
la pertenencia de la disponibilidad a la sede del médico, y el estado publicado.
Los horarios pasados, citas ocupadas, ofertas pendientes vigentes y retenciones
vigentes se excluyen con las mismas reglas. Los errores de persistencia mantienen
el contrato `CatalogoDatosException`.

Esta primera migración comprende las cinco consultas del cálculo agrupado. Las
consultas individuales y los demás módulos JDBC continúan en sus DAO actuales.

## Comprobaciones reproducibles

```powershell
mvn "-Dtest=MedicoCatalogoDaoTest,CatalogosApiServletTest" test
mvn test
mvn -DskipTests package
```

La referencia anterior aprueba 24 pruebas de catálogo y API. La versión migrada
aprueba las mismas comprobaciones y tres casos adicionales, 27 en total. Los
casos nuevos verifican el control de la conexión y rollback, el contrato ante un
error de Spring y el rechazo de fragmentos SQL en los identificadores de la API.

Los casos existentes comprueban el mismo número de cupos por sede, médico y
turno, incluyendo ofertas y retenciones. La búsqueda semanal con agendas mantiene
cinco sentencias preparadas y el conteo agrupado de admisión mantiene cuatro.
Esta refactorización mejora la separación de responsabilidades. Una nueva cifra
de latencia requiere repetir el laboratorio de rendimiento.

La suite general ejecutó 1.043 pruebas sin fallos, con otras 613 omitidas por sus
condiciones de entorno. Las pruebas MySQL requieren `AGENDA_MYSQL_URL`,
`AGENDA_MYSQL_USER` y `AGENDA_MYSQL_PASSWORD`, con una base temporal de nombre
`clinica_agendas_test_*`. Los reportes generados permanecen en `target`.

La ejecución adicional en MySQL aislado completó 113 pruebas, con 112 aprobadas
y un error en la creación concurrente de códigos de sede, SQLSTATE `40001`.
Las 76 pruebas de ofertas y el caso del ciclo de citas que consulta la semana
aprobaron. Al repetir los siete casos de `CicloVidaCitaMySqlTest` con el código
del tag anterior, se reprodujo el mismo interbloqueo de sedes. Esa incidencia
previa queda pendiente de una corrección específica de gestión de sedes.

## Prueba desde el portal

1. Iniciar la versión de esta rama y entrar como paciente.
2. Abrir Programar cita, elegir sede y especialidad y consultar una semana con horarios publicados.
3. Comprobar el conteo de mañana y tarde y filtrar por un médico de esa sede.
4. Reservar un horario y consultar de nuevo la semana para comprobar la disminución de cupos.
5. Revisar el contador de cupos del inicio de admisión en la sede correspondiente.

## Referencia técnica

[Spring JDBC y parámetros con nombre](https://docs.spring.io/spring-framework/reference/6.2/data-access/jdbc/core.html).
