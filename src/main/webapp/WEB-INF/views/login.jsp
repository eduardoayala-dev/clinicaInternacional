<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Iniciar Sesión - Clínica Internacional</title>
    <!-- Fuentes Google Exactas de Prototipo -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:ital,wght@1,600;1,700&display=swap" rel="stylesheet">
    <!-- Lucide Icons Vectoriales Sin Emojis -->
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        :root {
            --blue-hero: #0066cc;
            --blue-dark: #004b99;
            --blue-button: #1d70b8;
            --bg-right: #f0f7ff;
            --text-dark: #1e293b;
            --text-muted: #64748b;
            --border-color: #e2e8f0;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Inter', sans-serif;
        }

        body {
            min-height: 100vh;
            display: flex;
            background-color: var(--bg-right);
        }

        .login-wrapper {
            display: flex;
            width: 100vw;
            min-height: 100vh;
        }

        /* LADO IZQUIERDO AZUL FIGMA EXACTO */
        .hero-section {
            flex: 1.15;
            background: linear-gradient(135deg, #005bb5 0%, #003e80 100%);
            color: #ffffff;
            padding: 45px 55px;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            position: relative;
            overflow: hidden;
        }

        /* Círculos decorativos de fondo Figma */
        .hero-section::before {
            content: '';
            position: absolute;
            top: -80px;
            right: -80px;
            width: 350px;
            height: 350px;
            border-radius: 50%;
            background: rgba(255, 255, 255, 0.04);
            pointer-events: none;
        }

        .hero-section::after {
            content: '';
            position: absolute;
            bottom: 120px;
            right: 40px;
            width: 220px;
            height: 220px;
            border-radius: 50%;
            background: rgba(255, 255, 255, 0.03);
            pointer-events: none;
        }

        .hero-top {
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .hero-brand {
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .hero-brand img {
            height: 32px;
            width: auto;
        }

        .hero-brand span {
            font-size: 15px;
            font-weight: 700;
            letter-spacing: 0.8px;
            text-transform: uppercase;
        }

        .lang-selector {
            background: rgba(255, 255, 255, 0.12);
            border: 1px solid rgba(255, 255, 255, 0.25);
            border-radius: 20px;
            padding: 4px 12px;
            font-size: 12px;
            color: #ffffff;
            display: flex;
            align-items: center;
            gap: 6px;
            cursor: pointer;
        }

        .badge-status {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            background: rgba(255, 255, 255, 0.12);
            backdrop-filter: blur(10px);
            padding: 6px 14px;
            border-radius: 20px;
            font-size: 12px;
            margin-top: 25px;
            width: fit-content;
        }

        .dot-green {
            width: 7px;
            height: 7px;
            background-color: #10b981;
            border-radius: 50%;
            box-shadow: 0 0 8px #10b981;
        }

        .hero-middle {
            margin: 30px 0;
        }

        .hero-title {
            font-family: 'Playfair Display', serif;
            font-size: 44px;
            font-weight: 700;
            line-height: 1.15;
            margin-bottom: 16px;
        }

        .hero-title em {
            font-style: italic;
            font-weight: 400;
        }

        .hero-desc {
            font-size: 14.5px;
            color: rgba(255, 255, 255, 0.85);
            line-height: 1.55;
            max-width: 480px;
            margin-bottom: 35px;
        }

        .stats-grid {
            display: flex;
            gap: 40px;
        }

        .stat-item h3 {
            font-size: 28px;
            font-weight: 700;
        }

        .stat-item p {
            font-size: 12px;
            color: rgba(255, 255, 255, 0.7);
        }

        .testimonial-card {
            background: rgba(255, 255, 255, 0.08);
            backdrop-filter: blur(14px);
            border: 1px solid rgba(255, 255, 255, 0.15);
            border-radius: 14px;
            padding: 20px;
            max-width: 480px;
        }

        .testimonial-card p {
            font-size: 13px;
            line-height: 1.5;
            margin-bottom: 14px;
            color: rgba(255, 255, 255, 0.95);
        }

        .author {
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .author-avatar {
            width: 36px;
            height: 36px;
            border-radius: 50%;
            background: #ffffff;
            color: var(--blue-hero);
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
            font-size: 13px;
        }

        .author-info h4 {
            font-size: 13px;
            font-weight: 600;
        }

        .author-info span {
            font-size: 11px;
            color: rgba(255, 255, 255, 0.7);
        }

        /* LADO DERECHO FORMULARIO EXACTO FIGMA */
        .form-section {
            width: 500px;
            background-color: #f4f8fc;
            padding: 50px 45px;
            display: flex;
            flex-direction: column;
            justify-content: center;
        }

        .form-header h2 {
            font-size: 26px;
            font-weight: 700;
            color: var(--text-dark);
            margin-bottom: 6px;
        }

        .form-header p {
            font-size: 13.5px;
            color: #93c5fd;
            margin-bottom: 25px;
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
            margin-bottom: 20px;
        }

        .form-group {
            margin-bottom: 18px;
        }

        .form-group label {
            display: block;
            font-size: 12.5px;
            font-weight: 600;
            color: #475569;
            margin-bottom: 6px;
        }

        .input-icon-wrapper {
            position: relative;
            display: flex;
            align-items: center;
        }

        .input-icon-wrapper input {
            width: 100%;
            padding: 12px 14px 12px 40px;
            border: 1px solid #cbd5e1;
            border-radius: 8px;
            font-size: 13.5px;
            background-color: #ffffff;
            color: var(--text-dark);
            transition: all 0.2s ease;
        }

        .input-icon-wrapper input:focus {
            outline: none;
            border-color: #0066cc;
            box-shadow: 0 0 0 3px rgba(0, 102, 204, 0.1);
        }

        .input-icon {
            position: absolute;
            left: 13px;
            color: #94a3b8;
            width: 17px;
            height: 17px;
            pointer-events: none;
        }

        .eye-icon {
            position: absolute;
            right: 13px;
            color: #94a3b8;
            width: 17px;
            height: 17px;
            cursor: pointer;
        }

        .form-options {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 22px;
            font-size: 12.5px;
        }

        .remember-me {
            display: flex;
            align-items: center;
            gap: 6px;
            color: #475569;
            cursor: pointer;
        }

        .remember-me input {
            accent-color: #0066cc;
        }

        .forgot-link {
            color: #0284c7;
            text-decoration: none;
            font-weight: 500;
        }

        .btn-primary {
            width: 100%;
            padding: 13px;
            background-color: #0284c7;
            border: none;
            border-radius: 8px;
            color: #ffffff;
            font-size: 14.5px;
            font-weight: 600;
            cursor: pointer;
            transition: background-color 0.2s ease;
        }

        .btn-primary:hover {
            background-color: #0369a1;
        }

        .social-divider {
            text-align: center;
            margin: 20px 0;
            font-size: 12px;
            color: #94a3b8;
            position: relative;
        }

        .social-divider::before, .social-divider::after {
            content: '';
            position: absolute;
            top: 50%;
            width: 38%;
            height: 1px;
            background-color: #e2e8f0;
        }

        .social-divider::before { left: 0; }
        .social-divider::after { right: 0; }

        .social-buttons {
            display: flex;
            gap: 12px;
            margin-bottom: 25px;
        }

        .btn-social {
            flex: 1;
            padding: 10px;
            background-color: #ffffff;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 500;
            color: #334155;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            cursor: pointer;
        }

        .register-footer {
            text-align: center;
            font-size: 13px;
            color: #64748b;
        }

        .register-footer a {
            color: #0284c7;
            font-weight: 600;
            text-decoration: none;
        }

        @media (max-width: 960px) {
            .hero-section { display: none; }
            .form-section { width: 100%; }
        }
    </style>
</head>
<body>

<div class="login-wrapper">

    <!-- LADO IZQUIERDO REPLICA EXACTA FIGMA -->
    <div class="hero-section">
        <div class="hero-top">
            <div class="hero-brand">
                <img src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png" alt="Clinica Internacional">
                <span>CLINICA INTERNACIONAL</span>
            </div>
            <div class="lang-selector">
                <i data-lucide="globe" style="width: 14px; height: 14px;"></i>
                <span>ES</span>
                <i data-lucide="chevron-down" style="width: 12px; height: 12px;"></i>
            </div>
        </div>

        <div class="badge-status">
            <span class="dot-green"></span>
            Sistema activo · 1,284 pacientes
        </div>

        <div class="hero-middle">
            <h1 class="hero-title">Tu salud,<br><em>gestionada</em><br>con precisión.</h1>
            <p class="hero-desc">
                Programa citas, accede a tu historial clínico y mantén comunicación directa con tus especialistas desde un solo lugar.
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
                    <p>Atención</p>
                </div>
            </div>
        </div>

        <div class="testimonial-card">
            <p>"El sistema de la Clínica Internacional transformó cómo me organizo para mis citas médicas. La atención y la reasignación de cupos funcionan excelente."</p>
            <div class="author">
                <div class="author-avatar">DA</div>
                <div class="author-info">
                    <h4>Diana Arévalo</h4>
                    <span>Paciente desde 2024</span>
                </div>
            </div>
        </div>
    </div>

    <!-- LADO DERECHO REPLICA EXACTA FIGMA -->
    <div class="form-section">
        <div class="form-header">
            <h2>Bienvenido de vuelta</h2>
            <p>Ingresa tus credenciales para acceder a tu cuenta.</p>
        </div>

        <c:if test="${not empty errorMessage}">
            <div class="alert-error">
                <i data-lucide="alert-circle" style="width: 18px; height: 18px; flex-shrink: 0;"></i>
                <span><c:out value="${errorMessage}" /></span>
            </div>
        </c:if>

        <form action="${pageContext.request.contextPath}/login" method="POST" autocomplete="off">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}" />

            <div class="form-group">
                <label for="email">Correo electrónico</label>
                <div class="input-icon-wrapper">
                    <i data-lucide="mail" class="input-icon"></i>
                    <input type="email" id="email" name="email" required placeholder="Ingresa tu correo">
                </div>
            </div>

            <div class="form-group">
                <label for="password">Contraseña</label>
                <div class="input-icon-wrapper">
                    <i data-lucide="lock" class="input-icon"></i>
                    <input type="password" id="password" name="password" required placeholder="Ingresa tu contraseña">
                    <i data-lucide="eye-off" class="eye-icon"></i>
                </div>
            </div>

            <div class="form-options">
                <label class="remember-me">
                    <input type="checkbox" name="remember" checked> Mantener sesión iniciada
                </label>
                <a href="#" class="forgot-link">¿Olvidaste tu contraseña?</a>
            </div>

            <button type="submit" class="btn-primary">Iniciar sesión</button>
        </form>

        <div class="social-divider">o continúa con</div>

        <div class="social-buttons">
            <button type="button" class="btn-social">
                <span style="color: #ea4335; font-weight: 700;">G</span> Google
            </button>
            <button type="button" class="btn-social">
                <span style="color: #00a4ef; font-weight: 700;">M</span> Microsoft
            </button>
        </div>

        <div class="register-footer">
            ¿No tienes cuenta? <a href="${pageContext.request.contextPath}/registro">Regístrate</a>
        </div>
    </div>

</div>

<script>
    // Lucide Icons SVG vectoriales profesionales sin emojis
    lucide.createIcons();
</script>

</body>
</html>
