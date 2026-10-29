# Confianza TLS en Railway

El certificado MySQL debe identificar `mysql.railway.internal` y estar firmado por la CA que confía la aplicación. Conservar `DB_SSL_MODE=VERIFY_IDENTITY`; no desactivar la validación.

En las variables del servicio Java:

| Variable | Valor |
| --- | --- |
| DB_CA_PEM | Contenido completo de `/var/lib/mysql/ca.pem`, incluyendo BEGIN/END CERTIFICATE y saltos de línea reales. No usar una clave privada. |
| DB_TRUSTSTORE_PASSWORD | Contraseña propia para el almacén de certificados, de al menos 6 caracteres. No es la contraseña de MySQL. |
| DB_SSL_MODE | VERIFY_IDENTITY |

No configurar DB_TRUSTSTORE_PATH al utilizar DB_CA_PEM. El punto de entrada de la imagen genera un PKCS12 privado en un directorio temporal y exporta su ruta antes de iniciar Tomcat. No cambia el almacén global de Java. Se vuelve a generar en cada arranque y no necesita volumen de la aplicación. Si hay un certificado inválido, contraseña ausente o configuración ambigua, el arranque falla explícitamente.

Los despliegues existentes necesitan la imagen con `scripts/Iniciar-Tomcat.sh`. No establecer un comando de inicio que omita este punto de entrada. Conservar las variables entre despliegues. Si se rota la CA o se recrea el volumen de MySQL, actualizar DB_CA_PEM con la CA verificada del servidor; nunca obtenerla automáticamente de una conexión sin verificar.

El certificado del servidor y sus claves permanecen en el volumen MySQL. `scripts/Preparar-CertificadoMysql.sh` prepara un certificado con SAN, pero no lo instala. Respaldar el certificado activo antes de reemplazarlo y ejecutar `ALTER INSTANCE RELOAD TLS;` mediante una cuenta administrativa. Verificar la fecha del certificado activo y planificar su renovación antes de vencer (el preparado el 29 de octubre de 2026 vence el 29 de octubre de 2027).

Comprobar tras desplegar: conexión MySQL establecida, ingreso con una cuenta existente y lectura de agenda. Un certificado confiable con nombre incorrecto debe seguir siendo rechazado. Esta configuración no cambia cuentas, grants ni `require_secure_transport`; la exigencia TLS del servidor y de las cuentas requiere una revisión independiente después de recuperar el acceso.
