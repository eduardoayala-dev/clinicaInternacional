# Clínica Internacional Web 🏥

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Jakarta EE](https://img.shields.io/badge/Jakarta_EE-10-blue?style=for-the-badge)
![Maven](https://img.shields.io/badge/Apache_Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)

Sistema integral de gestión de citas médicas y reasignación inteligente para la Clínica Internacional, desarrollado bajo el estándar de arquitectura **MVC (Modelo-Vista-Controlador)**.

## 🚀 Características Principales
* **Portal del Paciente:** Reserva de citas interactiva mediante un Wizard (Asistente), historial de atenciones y notificaciones.
* **Módulo Administrativo:** Gestión del flujo de trabajo, reasignación inteligente de médicos y panel de estadísticas.
* **Seguridad:** Autenticación y cifrado de contraseñas mediante algoritmo BCrypt.
* **Despliegue Contenerizado:** Entorno configurado para Docker y Docker Compose, facilitando la portabilidad (PaaS).

## 🏗️ Arquitectura y Tecnologías
Este proyecto ha sido estructurado garantizando alta cohesión y bajo acoplamiento:
* **Backend:** Java 17, Jakarta EE 10 (Servlets, JSP).
* **Acceso a Datos:** JDBC nativo implementando el patrón de diseño **DAO** (Data Access Object) y Pool de conexiones con **HikariCP**.
* **Frontend:** HTML5, CSS3 Nativo (Variables globales), JavaScript (Fetch API/AJAX) y Lucide Icons.
* **Gestión de dependencias:** Apache Maven.
* **Base de Datos:** MySQL 8.0+.

## 🛠️ Prerrequisitos
* **JDK 17** o superior configurado en las variables de entorno.
* **Apache Maven** instalado.
* **MySQL** (XAMPP/WAMP o servidor local).
* *(Opcional)* Docker Desktop para despliegue por contenedores.

## ⚙️ Instalación y Configuración Local

**1. Clonar el repositorio**
```bash
git clone https://github.com/eduardoayala-dev/clinicaInternacional.git
cd clinicaInternacional
```

**2. Configuración de Base de Datos**
1. Iniciar el servicio de MySQL.
2. Abrir el gestor de base de datos (Ej. phpMyAdmin o Workbench) y crear una base de datos llamada `clinica_db`.
3. Ejecutar el script `sql/schema.sql` para generar las tablas y registros semilla.
*Nota: Por defecto, la aplicación utiliza el usuario `root` sin contraseña. Esto es modificable mediante variables de entorno.*

**3. Compilación y Ejecución**
El proyecto incluye un servidor embebido Jetty para agilizar el desarrollo.
```bash
mvn clean package
mvn jetty:run
```
La aplicación estará disponible en: `http://localhost:8080/`

## 🌿 Flujo de Trabajo (GitFlow)
El equipo de desarrollo se rige estrictamente por el estándar GitFlow:
* `main`: Entorno de Producción. Únicamente código estable.
* `develop`: Entorno de Integración.
* `feature/*`: Desarrollo de nuevas historias de usuario o módulos.

Todo commit debe seguir el formato semántico convencional en español. Ej: `feat: implementar módulo de reservas` o `fix: corregir error en conexión a base de datos`.