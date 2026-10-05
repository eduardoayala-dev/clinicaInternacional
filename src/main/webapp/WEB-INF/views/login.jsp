<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Iniciar Sesión - Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #0056b3;
            --primary-hover: #004085;
            --accent: #00a8cc;
            --bg-dark: #0f172a;
            --card-bg: rgba(30, 41, 59, 0.7);
            --text-light: #f8fafc;
            --text-muted: #94a3b8;
            --error-bg: rgba(239, 68, 68, 0.2);
            --error-border: #ef4444;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Inter', sans-serif;
        }

        body {
            background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #090d16 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
            color: var(--text-light);
        }

        .login-container {
            width: 100%;
            max-width: 440px;
            background: var(--card-bg);
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 20px;
            padding: 40px 30px;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
            animation: fadeIn 0.6s ease-out;
        }

        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(20px); }
            to { opacity: 1; transform: translateY(0); }
        }

        .brand-header {
            text-align: center;
            margin-bottom: 30px;
        }

        .brand-header h1 {
            font-size: 26px;
            font-weight: 700;
            color: #ffffff;
            letter-spacing: -0.5px;
            margin-bottom: 8px;
        }

        .brand-header h1 span {
            color: var(--accent);
        }

        .brand-header p {
            color: var(--text-muted);
            font-size: 14px;
        }

        .alert-error {
            background-color: var(--error-bg);
            border: 1px solid var(--error-border);
            color: #fca5a5;
            padding: 12px 16px;
            border-radius: 10px;
            font-size: 13px;
            margin-bottom: 20px;
            line-height: 1.4;
        }

        .form-group {
            margin-bottom: 20px;
        }

        .form-group label {
            display: block;
            font-size: 13px;
            font-weight: 500;
            color: var(--text-muted);
            margin-bottom: 8px;
        }

        .form-group input {
            width: 100%;
            padding: 14px 16px;
            background: rgba(15, 23, 42, 0.6);
            border: 1px solid rgba(255, 255, 255, 0.15);
            border-radius: 10px;
            color: #ffffff;
            font-size: 14px;
            transition: all 0.3s ease;
        }

        .form-group input:focus {
            outline: none;
            border-color: var(--accent);
            box-shadow: 0 0 0 3px rgba(0, 168, 204, 0.2);
        }

        .btn-submit {
            width: 100%;
            padding: 14px;
            background: linear-gradient(135deg, var(--primary) 0%, var(--accent) 100%);
            border: none;
            border-radius: 10px;
            color: #ffffff;
            font-size: 15px;
            font-weight: 600;
            cursor: pointer;
            transition: transform 0.2s ease, box-shadow 0.2s ease;
            margin-top: 10px;
        }

        .btn-submit:hover {
            transform: translateY(-2px);
            box-shadow: 0 10px 20px -5px rgba(0, 168, 204, 0.4);
        }

        .btn-submit:active {
            transform: translateY(0);
        }

        .demo-credentials {
            margin-top: 30px;
            padding: 16px;
            background: rgba(255, 255, 255, 0.03);
            border-radius: 12px;
            border: 1px dashed rgba(255, 255, 255, 0.1);
        }

        .demo-credentials h3 {
            font-size: 12px;
            font-weight: 600;
            color: var(--accent);
            text-transform: uppercase;
            letter-spacing: 0.5px;
            margin-bottom: 10px;
        }

        .demo-credentials p {
            font-size: 12px;
            color: var(--text-muted);
            margin-bottom: 4px;
        }

        .demo-credentials code {
            color: #38bdf8;
            background: rgba(0, 0, 0, 0.3);
            padding: 2px 6px;
            border-radius: 4px;
            font-family: monospace;
        }
    </style>
</head>
<body>

<div class="login-container">
    <div class="brand-header">
        <h1>Clínica <span>Internacional</span></h1>
        <p>Portal de Gestión de Citas Médicas</p>
    </div>

    <!-- Escapado de salida para XSS (HU020 / CA-3) -->
    <c:if test="${not empty errorMessage}">
        <div class="alert-error">
            <c:out value="${errorMessage}" />
        </div>
    </c:if>

    <form action="${pageContext.request.contextPath}/login" method="POST" autocomplete="off">
        <!-- Token CSRF Obligatorio (HU020 / CA-1) -->
        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}" />

        <div class="form-group">
            <label for="email">Correo Electrónico</label>
            <input type="email" id="email" name="email" required placeholder="ejemplo@clinicainternacional.pe">
        </div>

        <div class="form-group">
            <label for="password">Contraseña</label>
            <input type="password" id="password" name="password" required placeholder="••••••••">
        </div>

        <button type="submit" class="btn-submit">Iniciar Sesión</button>
    </form>

    <div class="demo-credentials">
        <h3>Cuentas Sintéticas de Prueba</h3>
        <p><strong>Paciente:</strong> <code>juan.perez@correo.com</code> | <code>Admin123!</code></p>
        <p><strong>Médico:</strong> <code>cmendoza@clinicainternacional.pe</code> | <code>Admin123!</code></p>
        <p><strong>Admin:</strong> <code>admin@clinicainternacional.pe</code> | <code>Admin123!</code></p>
    </div>
</div>

</body>
</html>
