# Clínica Internacional Web 🏥

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Jakarta EE](https://img.shields.io/badge/Jakarta_EE-10-blue?style=for-the-badge)
![Maven](https://img.shields.io/badge/Apache_Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0%2B-4479A1?style=for-the-badge&logo=mysql&logoColor=white)

Sistema integral de gestión de citas médicas y reasignación inteligente para la Clínica Internacional, desarrollado bajo el estándar de arquitectura **MVC (Modelo-Vista-Controlador)**.

## 🔐 Credenciales de Acceso

Para explorar las funcionalidades del sistema, se dispone de las siguientes cuentas de demostración:

| Rol | Usuario / DNI | Contraseña |
|---|---|---|
| **Paciente** | `70976343` | `Lucascool123@` |
| **Médico** | `eduardo@medico.com` | `Password123!` |
| **Personal Administrativo** | `eduardo@padmin.com` | `Password123!` |
| **Administrador del Sistema (SysAdmin)** | `eduardo@admin.com` | `Password123!` |

> **Nota:** Estas credenciales son exclusivamente para fines académicos y de demostración. No deben utilizarse en un entorno de producción.

## 🚀 Características Principales del Sistema Completo (Aun en fase de desarrollo)

- **Portal del Paciente:** Reserva de citas interactiva mediante un Wizard (Asistente), historial de atenciones y notificaciones.
- **Módulo Administrativo:** Gestión del flujo de trabajo, reasignación inteligente de médicos y panel de estadísticas.
- **Seguridad:** Autenticación y protección de contraseñas mediante el algoritmo de hashing BCrypt.
- **Despliegue Contenerizado:** Entorno configurado para Docker y Docker Compose, facilitando la portabilidad (PaaS).

## 🏗️ Arquitectura y Tecnologías

Este proyecto ha sido estructurado buscando alta cohesión y bajo acoplamiento:

- **Backend:** Java 17, Jakarta EE 10 (Servlets, JSP).
- **Acceso a Datos:** JDBC nativo implementando el patrón de diseño **DAO** (Data Access Object) y pool de conexiones con **HikariCP**.
- **Frontend:** HTML5, CSS3 nativo (variables globales), JavaScript (Fetch API/AJAX) y Lucide Icons.
- **Gestión de dependencias:** Apache Maven.
- **Base de Datos:** MySQL 8.0+.
- **Despliegue:** Railway, utilizando Apache Tomcat 10.1 y MySQL.

## 🛠️ Prerrequisitos

- **JDK 17** o superior configurado en las variables de entorno.
- **Apache Maven** instalado.
- **MySQL** (XAMPP/WAMP o servidor local).
- *(Opcional)* Docker Desktop para despliegue por contenedores.

## ⚙️ Instalación y Configuración Local

**1. Clonar el repositorio**

```bash
git clone https://github.com/eduardoayala-dev/clinicaInternacional.git
cd clinicaInternacional
```

**2. Configuración de Base de Datos**

1. Iniciar el servicio de MySQL.
2. Abrir el gestor de base de datos (por ejemplo, phpMyAdmin o Workbench) y crear una base de datos llamada `clinica_db`.
3. Ejecutar el script `sql/schema.sql` para generar las tablas y registros semilla.

Por defecto, la aplicación utiliza el usuario `root` sin contraseña. Esta configuración puede modificarse mediante variables de entorno.

**3. Compilación y Ejecución**

El proyecto incluye un servidor embebido Jetty para agilizar el desarrollo.

```bash
mvn clean package
mvn jetty:run
```

La aplicación estará disponible en: `http://localhost:8080/`

## ☁️ Despliegue en Railway

La aplicación se encuentra configurada para su despliegue en Railway mediante dos servicios:

- **Aplicación Java:** Apache Tomcat 10.1 ejecutando el archivo WAR.
- **Base de datos:** MySQL 8.4 alojado en Railway.

La conexión a la base de datos se configura mediante las siguientes variables de entorno:

| Variable | Descripción |
|---|---|
| `DB_HOST` | Dirección del servidor MySQL |
| `DB_PORT` | Puerto de conexión |
| `DB_NAME` | Nombre de la base de datos |
| `DB_USER` | Usuario de MySQL |
| `DB_PASSWORD` | Contraseña de conexión |

Estas variables permiten configurar la conexión sin modificar directamente el código fuente.

## Consulta de DNI

La configuración, contrato y pruebas del autocompletado de pacientes están en
[Consulta de DNI](docs/consulta-dni.md). La clave del proveedor se configura únicamente
en el servidor; el registro manual sigue disponible si la consulta falla.

## 🌿 Flujo de Trabajo (GitFlow)

El equipo de desarrollo sigue una estrategia basada en GitFlow:

- `main`: Entorno de producción. Únicamente código estable.
- `develop`: Entorno de integración.
- `feature/*`: Desarrollo de nuevas historias de usuario o módulos.
  
Todo commit debe seguir el formato semántico convencional en español. Ej: `feat: implementar módulo de reservas` o `fix: corregir error en conexión a base de datos`.
