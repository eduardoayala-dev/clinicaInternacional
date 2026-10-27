# Conexión segura y permisos de MySQL

Esta entrega prepara la aplicación para usar una cuenta limitada y verificar la identidad del servidor MySQL en producción. La configuración local de Compose cifra el transporte, pero usa el certificado generado por MySQL sin verificar su identidad; no es la configuración de producción.

## Variables del proceso Java

| Variable | Regla |
|---|---|
| APP_ENV | development, test o production. Un valor desconocido se rechaza. |
| DB_MODE | mysql es el único valor admitido; se puede omitir. |
| DB_HOST | Obligatoria. Nombre DNS, IPv4 o IPv6 entre corchetes; sin parámetros JDBC. |
| DB_PORT | 3306 por defecto, entre 1 y 65535. |
| DB_NAME | Obligatoria. Letras, números y guion bajo. |
| DB_USER | Obligatoria, distinta de root. |
| DB_PASSWORD | Obligatoria y no vacía. Se conserva exactamente, incluidos los espacios. |
| DB_SSL_MODE | Producción: VERIFY_IDENTITY obligatorio. Desarrollo: REQUIRED por defecto; se permite DISABLED solo mediante configuración expresa local. PREFERRED se rechaza para evitar degradación silenciosa. |
| DB_TRUSTSTORE_PATH | Opcional. Ruta absoluta a un archivo JKS/PKCS12 legible dentro del contenedor o proceso. Sin este valor se utiliza la confianza de Java. |
| DB_TRUSTSTORE_TYPE | PKCS12 por defecto; también JKS. |
| DB_TRUSTSTORE_PASSWORD | Contraseña del truststore, suministrada fuera del repositorio. |

La presencia de RAILWAY_ENVIRONMENT, RAILWAY_ENVIRONMENT_NAME o RAILWAY_PROJECT_ID también activa la política de producción. No basta poner APP_ENV=development para desactivarla en Railway.

Las credenciales y opciones TLS se pasan al driver como propiedades separadas de la URL. No se habilita allowPublicKeyRetrieval. Un fallo de MySQL produce error de conexión y nunca crea una base temporal. La aplicación mantiene reintentos en producción y pool de conexiones; no crea ni altera tablas al conectar a MySQL.

Los archivos .env y truststores privados se excluyen del repositorio y del contexto Docker. Java no carga .env automáticamente: Compose lo lee; con Jetty hay que definir las variables del proceso desde la terminal o el IDE.

## Desarrollo local con Docker Compose

1. Copiar .env.example a .env y establecer DB_PASSWORD y MYSQL_ROOT_PASSWORD con valores diferentes. No guardar .env en Git.
2. Iniciar Docker Desktop.
3. Ejecutar `docker compose config --quiet` y `docker compose up --build`.
4. Abrir http://localhost:8080/.

Compose inicia MySQL con require_secure_transport=ON, usa la cuenta clinica_app y espera su healthcheck antes de arrancar la app. No publica el puerto MySQL. Los datos se guardan en el volumen nombrado mysql_data. phpMyAdmin es opcional mediante `docker compose --profile tools up --build` y solo se publica en localhost:8081, sin credenciales administrativas automáticas.

Para un volumen nuevo, la imagen crea clinica_app con la contraseña indicada; después sql/init/20-app-permissions.sql elimina sus permisos generales y concede únicamente SELECT, INSERT, UPDATE y DELETE sobre clinica_db, además de exigir SSL. La cuenta de aplicación no tiene permisos CREATE, ALTER, DROP ni GRANT OPTION.

Los scripts de inicialización solo se ejecutan en un volumen vacío. Cambiar .env no cambia la contraseña de una cuenta existente. Esta configuración introduce un volumen nombrado: si ya hay datos en volúmenes anteriores, respaldarlos y restaurarlos expresamente antes de cambiar de entorno. No se eliminó ni migró ningún volumen durante esta entrega.

## Migración de una base existente

sql/schema.sql es un script de reinicialización con DROP TABLE y datos sintéticos; no debe ejecutarse sobre una base que se quiera conservar.

La actualización de las cuatro columnas de perfil está en sql/migrations/V001__perfil_paciente.sql. Ejecutarla con una cuenta de migración que tenga ALTER y con la base seleccionada. Consulta information_schema y añade solo las columnas ausentes; puede repetirse sin recrear tablas ni modificar valores existentes. No cambia el tipo de columnas ya presentes. El script está diseñado para MySQL, no H2.

Ejemplo desde el cliente mysql, que solicita la contraseña sin incluirla en los argumentos:

```text
mysql --host=SERVIDOR --user=USUARIO_MIGRACION --password --ssl-mode=VERIFY_IDENTITY --ssl-ca=CA.pem --database=clinica_db
```

En esa sesión:

```sql
SOURCE sql/migrations/V001__perfil_paciente.sql;
```

La aplicación ya no intenta corregir el esquema durante el registro ni descarta campos para aparentar compatibilidad. Si falta una columna, el registro falla y revierte la creación del usuario. Si falla el guardado de campos extendidos de un perfil, se revierten también sus cambios básicos.

MySQL hace commits implícitos en muchas operaciones DDL: respaldar antes de migrar y usar una única ejecución de migraciones a la vez. Este script no sustituye todavía un sistema completo de historial de migraciones.

## Preparación de producción y Railway

Antes de integrar y desplegar este cambio:

1. Respaldar la base actual y ejecutar la migración V001 con una cuenta administrativa independiente.
2. Crear una cuenta exclusiva para la aplicación con contraseña individual. Ejemplo de instrucciones para adaptar y ejecutar desde la administración de MySQL:

```sql
CREATE USER 'clinica_app'@'%' IDENTIFIED BY 'SUSTITUIR_POR_UNA_CLAVE_UNICA' REQUIRE SSL;
GRANT SELECT, INSERT, UPDATE, DELETE ON clinica_db.* TO 'clinica_app'@'%';
SHOW GRANTS FOR 'clinica_app'@'%';
```

El host de la cuenta debe restringirse cuando la infraestructura lo permita. Si la cuenta ya existe, revisar sus permisos actuales; crearla no revoca permisos heredados ni privilegios anteriores. El script local de permisos no se ejecuta automáticamente en Railway.

3. Obtener la CA y comprobar que el certificado del servidor incluye el hostname que usará DB_HOST. Una red privada no sustituye esta verificación. Si el certificado predeterminado del proveedor no corresponde al hostname, provisionar el certificado apropiado o un servicio que lo soporte; no degradar la verificación para ocultar el error.
4. Si la CA no está en Java, crear un truststore con keytool, solicitando su contraseña de forma interactiva:

```text
keytool -importcert -alias mysql-ca -file CA.pem -keystore mysql-truststore.p12 -storetype PKCS12
```

Montarlo fuera de la imagen y configurar DB_TRUSTSTORE_PATH, DB_TRUSTSTORE_TYPE y DB_TRUSTSTORE_PASSWORD. No incluirlo en Git ni en el Dockerfile.

5. Configurar APP_ENV=production, DB_MODE=mysql, DB_SSL_MODE=VERIFY_IDENTITY y las variables DB_* para la cuenta limitada. Verificar host, puerto y nombre de la base real; el ejemplo clinica_db puede diferir del servicio existente.
6. Ejecutar la prueba real de solo lectura desde un proceso que tenga esas variables y acceso a la red/CA:

```powershell
$env:MYSQL_SECURITY_TEST = 'true'
mvn test '-Dtest=MySqlSecurityTest'
```

La prueba valida el cipher TLS de la sesión real, la cuenta conectada y sus grants directos. Rechaza permisos fuera de USAGE/SELECT/INSERT/UPDATE/DELETE, privilegios globales y grants de roles no previstos. No escribe datos. La verificación de CA/hostname ocurre al conectar con VERIFY_IDENTITY. Las pruebas negativas adicionales se habilitan con MYSQL_TLS_NEGATIVE_TEST=true y requieren un entorno aislado: certificado válido para 127.0.0.1 pero no localhost, y MYSQL_TEST_UNTRUSTED_STORE con un truststore de una CA distinta. No deben activarse indiscriminadamente contra un servicio cloud.

7. Probar login, registro, perfil, reserva y persistencia en el entorno de prueba antes de publicar. Conservar evidencia sin contraseñas ni truststore passwords.

## Evidencia de esta entrega

- Suite con MySQL 8.0 aislado y certificados temporales: **53 pruebas, 0 fallos, 0 errores, 0 omitidas**.
- Se comprobó TLS con VERIFY_IDENTITY mediante el driver real; se rechazaron CA incorrecta y hostname incorrecto.
- MySQL rechazó una conexión sin TLS y un ALTER TABLE desde clinica_app. La prueba de grants comprobó cuenta distinta de root, permisos CRUD y ausencia de privilegios adicionales/globales.
- Se retiraron las cuatro columnas de perfil solo en la base desechable y se ejecutó V001 dos veces. Las cuatro columnas se restauraron y los conteos de usuarios/citas se conservaron.
- Las pruebas H2 verificaron registro con cuenta limitada a CRUD y rollback ante columnas ausentes en registro y perfil. No se presenta H2 como prueba del protocolo TLS.
- Docker Compose se validó con config --quiet usando valores de prueba, sin mostrar secretos. No se arrancó la pila completa Compose ni se verificó su interfaz web en esta entrega.
- Se generaron credenciales y certificados solo para el ensayo; los contenedores y sus volúmenes se eliminaron al terminar.
- En una ejecución normal sin las variables de integración, las tres pruebas de MySQL real se omiten expresamente. No deben contarse como validación cloud.
- Railway no se modificó: faltan la preparación y verificación de la cuenta limitada, migración, certificados/hostname y variables en ese entorno antes de integrar y desplegar el cambio si develop activa despliegue automático.

## Referencias

- [Autenticación del servidor con Connector/J](https://dev.mysql.com/doc/connector-j/en/connector-j-server-authentication.html).
- [Propiedades de seguridad del driver](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-security.html).
- [Inicialización de la imagen oficial MySQL](https://hub.docker.com/_/mysql/).

## Persistencia local

La aplicación solo utiliza MySQL. H2 tiene alcance Maven `test`: sirve para pruebas aisladas y no se incluye en el WAR. No existe un modo temporal de ejecución ni se cargan datos de demostración desde Java. Una configuración antigua con `DB_MODE=h2` se rechaza.

Para trabajar con la base local persistente, usar la aplicación de Docker en `http://localhost:8080`. Cerrar Docker o reconstruir la aplicación conserva el volumen `mysql_data`. Eliminar ese volumen o ejecutar `sql/schema.sql` sobre la base elimina datos. Una instancia Java adicional solo comparte cuentas si apunta al mismo servidor y base de MySQL.

## Codificación de textos

Los archivos SQL se guardan en UTF-8. `sql/schema.sql` declara `SET NAMES utf8mb4` antes de insertar datos para impedir que el cliente interprete sus bytes como latin1. Para importaciones manuales, usar además `mysql --default-character-set=utf8mb4`. Esto no exige recrear volúmenes ni tablas existentes.

Cambiar la codificación de la conexión no repara textos ya dañados. Antes de corregir datos existentes, guardar un respaldo, identificar los bytes afectados y aplicar actualizaciones específicas con verificación del valor anterior. No ejecutar de nuevo `schema.sql`, porque elimina tablas.
