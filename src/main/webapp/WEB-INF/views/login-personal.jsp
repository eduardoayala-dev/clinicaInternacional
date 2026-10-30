<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<% request.setAttribute("portalPersonal", true); %>
<!DOCTYPE html>
<html lang="es">
<head>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <%@ include file="/WEB-INF/jspf/disponibilidad-head.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Acceso Institucional - Clínica Internacional</title>
    <!-- Fuentes Google -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=DM+Serif+Display:ital@0;1&display=swap" rel="stylesheet">
    <!-- Lucide Icons SVG sin emojis -->
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        :root {
            --slate-dark-start: #0f172a;
            --slate-dark-end: #1e293b;
            --bg-right: #ffffff;
            --text-dark: #0f172a;
            --text-muted: #64748b;
            --brand-cyan: #38bdf8;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Inter', sans-serif;
        }

        html, body {
            height: 100%;
            width: 100%;
            margin: 0;
            padding: 0;
            overflow-x: hidden;
        }

        body {
            display: flex;
            min-height: 100vh;
            background: linear-gradient(to right, var(--slate-dark-start) 62%, var(--bg-right) 38%);
        }

        .login-wrapper {
            display: flex;
            width: 100%;
            min-height: 100vh;
        }

        /* SECCIÓN DERECHA FORMULARIO INSTITUCIONAL (38% ANCHO) */
        .form-section {
            flex: 1;
            background-color: var(--bg-right);
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 40px;
            height: 100vh;
        }

        .form-inner {
            width: 100%;
            max-width: 400px;
        }

        .portal-switch {
            text-align: right;
            margin-bottom: 30px;
        }

        .portal-switch a {
            color: #475569;
            font-size: 13px;
            font-weight: 600;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 6px 12px;
            border-radius: 8px;
            background-color: #f1f5f9;
            transition: all 0.2s ease;
        }

        .portal-switch a:hover {
            background-color: #e2e8f0;
        }

        .form-header {
            margin-bottom: 30px;
        }

        .form-header h2 {
            font-size: 28px;
            font-weight: 800;
            color: var(--text-dark);
            margin-bottom: 8px;
            letter-spacing: -0.5px;
        }

        .form-header p {
            font-size: 14px;
            color: var(--text-muted);
            line-height: 1.4;
        }

        .alert-error {
            display: flex;
            align-items: center;
            gap: 10px;
            background-color: #fef2f2;
            border: 1px solid #fca5a5;
            color: #b91c1c;
            padding: 12px 14px;
            border-radius: 8px;
            font-size: 13px;
            margin-bottom: 22px;
        }

        .form-group {
            margin-bottom: 22px;
        }

        .form-group label {
            display: block;
            font-size: 13px;
            font-weight: 600;
            color: #334155;
            margin-bottom: 8px;
        }

        .input-icon-wrapper {
            position: relative;
            display: flex;
            align-items: center;
        }

        .input-icon-wrapper input {
            width: 100%;
            padding: 14px 14px 14px 44px;
            border: 1px solid #cbd5e1;
            border-radius: 10px;
            font-size: 14.5px;
            background-color: #ffffff;
            color: var(--text-dark);
            transition: all 0.2s ease;
        }

        .input-icon-wrapper input:focus {
            outline: none;
            border-color: #0f172a;
            box-shadow: 0 0 0 4px rgba(15, 23, 42, 0.12);
        }

        .input-icon {
            position: absolute;
            left: 14px;
            color: #94a3b8;
            width: 20px;
            height: 20px;
            pointer-events: none;
        }

        .btn-primary {
            width: 100%;
            padding: 14px;
            background-color: #0f172a;
            border: none;
            border-radius: 10px;
            color: #ffffff;
            font-size: 15px;
            font-weight: 700;
            cursor: pointer;
            transition: background-color 0.2s ease;
            margin-top: 10px;
            box-shadow: 0 4px 12px rgba(15, 23, 42, 0.2);
        }

        .btn-primary:hover {
            background-color: #1e293b;
        }

        @media (max-width: 960px) {
            body { background: var(--bg-right); }
            .hero-section { display: none; }
            .form-section { width: 100%; }
        }
    </style>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/acceso-tema.css">
    <script defer src="${pageContext.request.contextPath}/assets/js/acceso.js"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/recuperacion-contenido.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/recuperacion-inline.css">
    <script defer src="${pageContext.request.contextPath}/assets/js/recuperacion.js"></script>
    <script defer src="${pageContext.request.contextPath}/assets/js/recuperacion-inline.js"></script>
</head>
<body class="portal-personal">

<div class="login-wrapper">

    <%@ include file="/WEB-INF/jspf/acceso-marca.jspf" %>

    <!-- FORMULARIO INSTITUCIONAL -->
    <div class="form-section">
        <div class="form-inner">
            <section data-acceso-login>
            <div class="portal-switch">
                <a href="${pageContext.request.contextPath}/login/paciente">
                    <span>Ir al Portal de Pacientes</span>
                </a>
            </div>

            <div class="form-header">
                <h2>Acceso Personal</h2>
                <p>Ingreso exclusivo para Médicos, Admisión y Administradores de Sistema.</p>
            </div>

            <c:if test="${not empty errorMessage}">
                <div class="alert-error">
                    <i data-lucide="alert-circle" style="width: 18px; height: 18px; flex-shrink: 0;"></i>
                    <span><c:out value="${errorMessage}" /></span>
                </div>
            </c:if>

            <form action="${pageContext.request.contextPath}/login/personal" method="POST" autocomplete="off" data-comprobar-disponibilidad>
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}" />

                <div class="form-group">
                    <label for="identificador">Correo Institucional</label>
                    <div class="input-icon-wrapper">
                        <input type="email" id="identificador" name="identificador" required placeholder="usuario@clinicainternacional.pe">
                    </div>
                </div>

                <div class="form-group">
                    <label for="password">Contraseña</label>
                    <div class="input-icon-wrapper">
                        <input type="password" id="password" name="password" required placeholder="Ingresa tu contraseña">
                        <button type="button" class="acceso-ver-password" data-password-toggle="password" aria-pressed="false" title="Mostrar contraseña"><img src="${pageContext.request.contextPath}/assets/images/seguridad/ojo.svg" alt=""></button>
                    </div>
                </div>

                <button type="submit" class="btn-primary">Acceder al Sistema</button>
                <p><a class="forgot-personal" data-abrir-recuperacion href="${pageContext.request.contextPath}/recuperar?portal=personal">¿Olvidaste tu contraseña?</a></p>
            </form>
            </section>
            <section class="acceso-recuperacion" data-recuperacion-inline hidden></section>
        </div>
    </div>

</div>

<script>
    lucide.createIcons();
</script>

</body>
</html>
