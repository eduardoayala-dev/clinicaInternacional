<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Iniciar Sesión - Clínica Internacional</title>
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

        /* FONDO ASIMÉTRICO: La sección azul ocupa el 62% y la blanca el 38% */
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

        /* SECCIÓN IZQUIERDA INFORMACIÓN (62% DEL ANCHO) */
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
            max-width: 680px; /* Contenido amplio y bien respirado */
            padding: 50px 40px;
            height: 100vh;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
        }

        .hero-section::before {
            content: '';
            position: absolute;
            top: -80px;
            right: -80px;
            width: 400px;
            height: 400px;
            border-radius: 50%;
            background: rgba(255, 255, 255, 0.04);
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

        .lang-selector {
            background: rgba(255, 255, 255, 0.12);
            border: 1px solid rgba(255, 255, 255, 0.25);
            border-radius: 20px;
            padding: 6px 16px;
            font-size: 12.5px;
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
            box-shadow: 0 0 8px #10b981;
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
            margin-bottom: 20px;
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

        .testimonial-card {
            background: rgba(255, 255, 255, 0.08);
            backdrop-filter: blur(14px);
            border: 1px solid rgba(255, 255, 255, 0.15);
            border-radius: 14px;
            padding: 24px;
            max-width: 560px;
        }

        .testimonial-card p {
            font-size: 14px;
            line-height: 1.55;
            margin-bottom: 14px;
            color: rgba(255, 255, 255, 0.95);
        }

        .author {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .author-avatar {
            width: 38px;
            height: 38px;
            border-radius: 50%;
            background: #ffffff;
            color: #005bb5;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
            font-size: 13px;
        }

        .author-info h4 {
            font-size: 13.5px;
            font-weight: 600;
        }

        .author-info span {
            font-size: 11.5px;
            color: rgba(255, 255, 255, 0.7);
        }

        /* SECCIÓN DERECHA FORMULARIO (38% DEL ANCHO) PERFECTAMENTE CENTRADO */
        .form-section {
            flex: 1;
            background-color: var(--bg-right);
            display: flex;
            align-items: center;
            justify-content: center; /* CENTRADO ABSOLUTO HORIZONTAL Y VERTICAL */
            padding: 40px;
            height: 100vh;
        }

        .form-inner {
            width: 100%;
            max-width: 400px; /* Ancho elegante de formulario sin verse estirado */
        }

        .mobile-brand {
            display: none;
            text-align: center;
            margin-bottom: 30px;
        }

        .mobile-brand img {
            height: 44px;
            width: auto;
        }

        .form-header {
            margin-bottom: 32px;
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

        .eye-icon {
            position: absolute;
            right: 14px;
            color: #94a3b8;
            width: 18px;
            height: 18px;
            cursor: pointer;
        }

        .form-options {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 28px;
            font-size: 13px;
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
            width: 16px;
            height: 16px;
        }

        .forgot-link {
            color: #0284c7;
            text-decoration: none;
            font-weight: 600;
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
            margin-bottom: 30px;
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

        /* MÓVIL */
        @media (max-width: 960px) {
            body {
                background: var(--bg-right);
            }

            .hero-section {
                display: none;
            }

            .form-section {
                width: 100%;
                padding: 40px 20px;
            }

            .mobile-brand {
                display: block;
            }
        }
    </style>
</head>
<body>

<div class="login-wrapper">

    <!-- SECCIÓN IZQUIERDA INFORMACIÓN (62% ANCHO) -->
    <div class="hero-section">
        <div class="hero-inner">
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
    </div>

    <!-- SECCIÓN DERECHA FORMULARIO CENTRADO (38% ANCHO) -->
    <div class="form-section">
        <div class="form-inner">
            <div class="mobile-brand">
                <img src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png" alt="Clinica Internacional">
            </div>

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

            <div class="register-footer">
                ¿No tienes cuenta? <a href="${pageContext.request.contextPath}/registro">Regístrate</a>
            </div>
        </div>
    </div>

</div>

<script>
    // Inicializar iconos vectoriales Lucide SVG
    lucide.createIcons();
</script>

</body>
</html>
