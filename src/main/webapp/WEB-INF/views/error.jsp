<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    // Obtener información del error
    Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
    String errorMessage = (String) request.getAttribute("jakarta.servlet.error.message");
    
    // Valores por defecto
    if (statusCode == null) statusCode = 500;
    
    String title = "Ocurrió un error inesperado";
    String description = "El servidor encontró un problema y no pudo completar su solicitud.";
    String icon = "alert-triangle";
    
    if (statusCode == 404) {
        title = "Página no encontrada";
        description = "Lo sentimos, la ruta que intentas buscar no existe o ha sido movida temporalmente.";
        icon = "file-question";
    } else if (statusCode == 403) {
        title = "Acceso Denegado";
        description = "No tienes los privilegios necesarios para acceder a este módulo del sistema.";
        icon = "shield-alert";
    } else if (statusCode == 500) {
        title = "Error del Sistema";
        description = "Ha ocurrido un error interno en el servidor. Nuestro equipo técnico ha sido notificado automáticamente.";
        icon = "server-crash";
    }
%>

<!-- Lógica inteligente para el botón "Volver al Inicio" -->
<c:set var="homeUrl" value="${pageContext.request.contextPath}/" />
<c:if test="${not empty sessionScope.usuarioLogueado}">
    <c:choose>
        <c:when test="${sessionScope.rol eq 'ADMINISTRADOR'}">
            <c:set var="homeUrl" value="${pageContext.request.contextPath}/admin/dashboard" />
        </c:when>
        <c:when test="${sessionScope.rol eq 'PACIENTE'}">
            <c:set var="homeUrl" value="${pageContext.request.contextPath}/paciente/dashboard" />
        </c:when>
        <c:otherwise>
            <c:set var="homeUrl" value="${pageContext.request.contextPath}/admin/dashboard" />
        </c:otherwise>
    </c:choose>
</c:if>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Error <%= statusCode %> - Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        :root {
            --primary: #2563eb;
            --primary-dark: #1d4ed8;
            --bg-body: #f8fafc;
            --text-main: #0f172a;
            --text-muted: #64748b;
        }

        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Inter', sans-serif; }
        
        body {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            height: 100vh;
            background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
            color: var(--text-main);
        }

        /* Logo superior fuera de la caja */
        .brand-header {
            position: absolute;
            top: 40px;
            left: 50%;
            transform: translateX(-50%);
        }
        .brand-header img {
            height: 48px;
        }

        .error-container {
            text-align: center;
            background: #ffffff;
            padding: 56px 48px;
            border-radius: 20px;
            box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.01);
            border: 1px solid rgba(255, 255, 255, 0.5);
            max-width: 480px;
            width: 90%;
            position: relative;
        }

        .icon-box {
            width: 88px;
            height: 88px;
            background: #eff6ff;
            color: var(--primary);
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0 auto 24px auto;
            box-shadow: 0 0 0 8px rgba(239, 246, 255, 0.5);
        }

        .status-code {
            font-size: 15px;
            font-weight: 700;
            color: var(--primary);
            letter-spacing: 1.5px;
            text-transform: uppercase;
            margin-bottom: 12px;
            display: block;
        }

        .error-title {
            font-size: 26px;
            font-weight: 800;
            color: var(--text-main);
            margin-bottom: 16px;
            letter-spacing: -0.5px;
        }

        .error-desc {
            font-size: 15px;
            color: var(--text-muted);
            line-height: 1.6;
            margin-bottom: 40px;
        }

        .btn-home {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 10px;
            background-color: var(--primary);
            color: #ffffff;
            padding: 14px 32px;
            border-radius: 10px;
            font-size: 15px;
            font-weight: 600;
            text-decoration: none;
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
            box-shadow: 0 4px 6px -1px rgba(37, 99, 235, 0.2);
            width: 100%;
        }

        .btn-home:hover {
            background-color: var(--primary-dark);
            transform: translateY(-2px);
            box-shadow: 0 10px 15px -3px rgba(37, 99, 235, 0.3);
        }

    </style>
</head>
<body>

    <div class="brand-header">
        <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
    </div>

    <div class="error-container">
        <div class="icon-box">
            <i data-lucide="<%= icon %>" style="width: 44px; height: 44px;"></i>
        </div>
        
        <span class="status-code">Error <%= statusCode %></span>
        <h1 class="error-title"><%= title %></h1>
        <p class="error-desc"><%= description %></p>
        
        <a href="${homeUrl}" class="btn-home">
            <i data-lucide="arrow-left" style="width: 18px;"></i>
            Regresar al Dashboard
        </a>
    </div>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
