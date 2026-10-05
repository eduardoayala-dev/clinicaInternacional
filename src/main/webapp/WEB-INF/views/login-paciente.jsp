<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Portal de Pacientes - Clínica Internacional</title>
    <!-- Fuentes Google -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:ital,wght@1,600;1,700&display=swap" rel="stylesheet">
    <!-- Lucide Icons SVG sin emojis -->
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        :root {
            --blue-hero-start: #005bb5;
            --blue-hero-end: #003e80;
            --bg-right: #f8fafc;
            --text-dark: #0f172a;
            --text-muted: #64748b;
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
            background: linear-gradient(to right, var(--blue-hero-start) 62%, var(--bg-right) 38%);
        }

        .login-wrapper {
            display: flex;
            width: 100%;
            min-height: 100vh;
        }

        /* SECCIÓN IZQUIERDA INFORMACIÓN PACIENTES (62% ANCHO) */
        .hero-section {
            flex: 1.6;
            background: linear-gradient(135deg, var(--blue-hero-start) 0%, var(--blue-hero-end) 100%);
            color: #ffffff;
            display: flex;
            justify-content: center;
            align-items: center;
            position: relative;
            overflow: hidden;
        }

        .hero-inner {
            width: 100%;
            max-width: 680px;
            padding: 50px 40px;
            height: 100vh;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
        }

        .hero-brand {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .hero-brand img {
            height: 38px;
            width: auto;
        }

        .hero-brand span {
            font-size: 15px;
            font-weight: 700;
            letter-spacing: 0.8px;
            text-transform: uppercase;
        }

        .badge-status {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            background: rgba(255, 255, 255, 0.12);
            backdrop-filter: blur(10px);
            padding: 7px 16px;
            border-radius: 20px;
            font-size: 12.5px;
            margin-top: 25px;
            width: fit-content;
        }

        .dot-green {
            width: 8px;
            height: 8px;
            background-color: #10b981;
            border-radius: 50%;
        }

        .hero-middle {
            margin: auto 0;
            padding: 20px 0;
        }

        .hero-title {
            font-family: 'Playfair Display', serif;
            font-size: 48px;
            font-weight: 700;
            line-height: 1.15;
            margin-bottom: 18px;
        }

        .hero-title em {
            font-style: italic;
            font-weight: 400;
        }

        .hero-desc {
            font-size: 15.5px;
            color: rgba(255, 255, 255, 0.88);
            line-height: 1.6;
            max-width: 540px;
            margin-bottom: 40px;
        }

        .stats-grid {
            display: flex;
            gap: 50px;
        }

        .stat-item h3 {
            font-size: 32px;
            font-weight: 700;
        }

        .stat-item p {
            font-size: 13px;
            color: rgba(255, 255, 255, 0.7);
        }

        /* SECCIÓN DERECHA FORMULARIO PACIENTE (38% ANCHO) */
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
            margin-bottom: 25px;
        }

        .portal-switch a {
            color: #0284c7;
            font-size: 12.5px;
            font-weight: 600;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 6px;
        }

        .form-header {
            margin-bottom: 30px;
        }

        .form-header h2 {
            font-size: 28px;
            font-weight: 700;
            color: var(--text-dark);
            margin-bottom: 8px;
        }

        .form-header p {
            font-size: 14px;
            color: var(--text-muted);
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
            margin-bottom: 20px;
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
            padding: 13px 14px 13px 42px;
            border: 1px solid #cbd5e1;
            border-radius: 10px;
            font-size: 14px;
            background-color: #ffffff;
            color: var(--text-dark);
            transition: all 0.2s ease;
        }

        .input-icon-wrapper input:focus {
            outline: none;
            border-color: #0066cc;
            box-shadow: 0 0 0 4px rgba(0, 102, 204, 0.1);
        }

        .input-icon {
            position: absolute;
            left: 14px;
            color: #94a3b8;
            width: 18px;
            height: 18px;
            pointer-events: none;
        }

        .btn-primary {
            width: 100%;
            padding: 14px;
            background-color: #0284c7;
            border: none;
            border-radius: 10px;
            color: #ffffff;
            font-size: 15px;
            font-weight: 600;
            cursor: pointer;
            transition: background-color 0.2s ease;
            margin-top: 10px;
            margin-bottom: 25px;
        }

        .btn-primary:hover {
            background-color: #0369a1;
        }

        .register-footer {
            text-align: center;
            font-size: 13.5px;
            color: #64748b;
        }

        .register-footer a {
            color: #0284c7;
            font-weight: 600;
            text-decoration: none;
        }

        @media (max-width: 960px) {
            body { background: var(--bg-right); }
            .hero-section { display: none; }
            .form-section { width: 100%; }
        }
    </style>
</head>
<body>

<div class="login-wrapper">

    <!-- HERO PACIENTES -->
    <div class="hero-section">
        <div class="hero-inner">
            <div class="hero-brand">
                <img src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png" alt="Clinica Internacional">
                <span>CLINICA INTERNACIONAL</span>
            </div>

            <div class="badge-status">
                <span class="dot-green"></span>
                Portal del Paciente Activo
            </div>

            <div class="hero-middle">
                <h1 class="hero-title">Tu salud,<br><em>gestionada</em><br>con precisión.</h1>
                <p class="hero-desc">
                    Programa citas médicas, consulta la disponibilidad de consultorios y accede a tu historial de atención en línea.
                </p>

                <div class="stats-grid">
                    <div class="stat-item">
                        <h3>400+</h3>
                        <p>Especialistas</p>
                    </div>
                    <div class="stat-item">
                        <h3>40 esp.</h3>
                        <p>Especialidades</p>
                    </div>
                    <div class="stat-item">
                        <h3>24/7</h3>
                        <p>Reserva en Línea</p>
                    </div>
                </div>
            </div>

            <div class="testimonial-card">
                <p>"La reserva de citas y la reasignación de cupos liberados me permitieron conseguir atención médica el mismo día."</p>
            </div>
        </div>
    </div>

    <!-- FORMULARIO PACIENTE -->
    <div class="form-section">
        <div class="form-inner">
            <div class="portal-switch">
                <a href="${pageContext.request.contextPath}/login/personal">
                    <i data-lucide="shield" style="width: 14px; height: 14px;"></i>
                    <span>Acceso Personal Médico / Admisión</span>
                </a>
            </div>

            <div class="form-header">
                <h2>Portal del Paciente</h2>
                <p>Ingresa con tu DNI o Correo electrónico registrado.</p>
            </div>

            <c:if test="${not empty errorMessage}">
                <div class="alert-error">
                    <i data-lucide="alert-circle" style="width: 18px; height: 18px; flex-shrink: 0;"></i>
                    <span><c:out value="${errorMessage}" /></span>
                </div>
            </c:if>

            <form action="${pageContext.request.contextPath}/login/paciente" method="POST" autocomplete="off">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}" />

                <div class="form-group">
                    <label for="identificador">DNI o Correo electrónico</label>
                    <div class="input-icon-wrapper">
                        <i data-lucide="user" class="input-icon"></i>
                        <input type="text" id="identificador" name="identificador" required placeholder="72345678 o correo@ejemplo.com">
                    </div>
                </div>

                <div class="form-group">
                    <label for="password">Contraseña</label>
                    <div class="input-icon-wrapper">
                        <i data-lucide="lock" class="input-icon"></i>
                        <input type="password" id="password" name="password" required placeholder="••••••••">
                    </div>
                </div>

                <button type="submit" class="btn-primary">Ingresar al Portal</button>
            </form>

            <div class="register-footer">
                ¿Primera vez en la clínica? <a href="${pageContext.request.contextPath}/registro">Crear cuenta de Paciente</a>
            </div>
        </div>
    </div>

</div>

<script>
    lucide.createIcons();
</script>

</body>
</html>
