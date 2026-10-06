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
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=Playfair+Display:ital,wght@1,500;1,600;1,700&display=swap" rel="stylesheet">
    <!-- Lucide Icons SVG sin emojis -->
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        :root {
            --blue-hero-bg: #00509e;
            --blue-hero-dark: #00366b;
            --bg-right: #f8fafc;
            --text-dark: #0f172a;
            --text-muted: #64748b;
            --primary-btn: #0077c8;
            --primary-btn-hover: #005fa0;
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
            background: linear-gradient(to right, var(--blue-hero-bg) 62%, var(--bg-right) 38%);
        }

        .login-wrapper {
            display: flex;
            width: 100%;
            min-height: 100vh;
        }

        /* SECCIÓN IZQUIERDA EXACTA SEGÚN MOCKUP PACIENTES (62% ANCHO) */
        .hero-section {
            flex: 1.6;
            background: linear-gradient(135deg, #00509e 0%, #00376e 100%);
            color: #ffffff;
            display: flex;
            justify-content: center;
            align-items: center;
            position: relative;
            overflow: hidden;
        }

        /* Patron de circulos vectoriales decorativos exactamente como la referencia */
        .bg-circles-svg {
            position: absolute;
            top: 0;
            right: 0;
            width: 100%;
            height: 100%;
            pointer-events: none;
            opacity: 0.18;
        }

        .hero-inner {
            width: 100%;
            max-width: 720px;
            padding: 48px 48px;
            height: 100vh;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            position: relative;
            z-index: 2;
        }

        /* TOP HEADER ROW: LOGO + IDIOMA */
        .hero-top-row {
            display: flex;
            justify-content: space-between;
            align-items: center;
            width: 100%;
        }

        .hero-brand {
            display: flex;
            align-items: center;
            gap: 14px;
        }

        .hero-brand img {
            height: 52px;
            width: auto;
            object-fit: contain;
        }

        .brand-title {
            font-size: 21px;
            font-weight: 800;
            letter-spacing: 1.2px;
            text-transform: uppercase;
            color: #ffffff;
        }

        .lang-selector-pill {
            display: flex;
            align-items: center;
            gap: 6px;
            background: rgba(255, 255, 255, 0.12);
            border: 1px solid rgba(255, 255, 255, 0.2);
            padding: 6px 14px;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 600;
            color: #ffffff;
            cursor: pointer;
            backdrop-filter: blur(8px);
        }

        .hero-middle {
            margin: auto 0;
            padding: 20px 0;
        }

        .hero-title {
            font-size: 52px;
            font-weight: 800;
            line-height: 1.12;
            margin-bottom: 20px;
            color: #ffffff;
            letter-spacing: -0.5px;
        }

        .hero-title em {
            font-family: 'Playfair Display', serif;
            font-style: italic;
            font-weight: 500;
            color: #ffffff;
        }

        .hero-desc {
            font-size: 16.5px;
            color: rgba(255, 255, 255, 0.88);
            line-height: 1.6;
            max-width: 560px;
            margin-bottom: 40px;
        }

        /* ESTRUCTURA DE ESTADISTICAS */
        .stats-row {
            display: flex;
            gap: 48px;
            margin-bottom: 36px;
        }

        .stat-item h3 {
            font-size: 34px;
            font-weight: 800;
            color: #ffffff;
            line-height: 1;
            margin-bottom: 6px;
        }

        .stat-item p {
            font-size: 13.5px;
            color: rgba(255, 255, 255, 0.75);
            font-weight: 500;
        }

        /* TARJETA DE TESTIMONIAL DEL MOCKUP */
        .testimonial-card {
            background: rgba(255, 255, 255, 0.08);
            border: 1px solid rgba(255, 255, 255, 0.16);
            border-radius: 16px;
            padding: 24px;
            backdrop-filter: blur(10px);
        }

        .testimonial-text {
            font-size: 14.5px;
            line-height: 1.55;
            color: rgba(255, 255, 255, 0.95);
            margin-bottom: 16px;
            font-weight: 400;
        }

        .testimonial-user {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .user-avatar-circle {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            background-color: #0284c7;
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
            font-size: 14px;
            border: 2px solid rgba(255, 255, 255, 0.3);
        }

        .user-info {
            display: flex;
            flex-direction: column;
        }

        .user-name {
            font-size: 14px;
            font-weight: 700;
            color: #ffffff;
        }

        .user-role {
            font-size: 12px;
            color: rgba(255, 255, 255, 0.65);
        }

        /* SECCIÓN DERECHA FORMULARIO PACIENTES (38% ANCHO) */
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
            color: #0284c7;
            font-size: 13px;
            font-weight: 600;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 6px 12px;
            border-radius: 8px;
            background-color: #e0f2fe;
            transition: all 0.2s ease;
        }

        .portal-switch a:hover {
            background-color: #bae6fd;
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
            border-color: var(--primary-btn);
            box-shadow: 0 0 0 4px rgba(0, 119, 200, 0.12);
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
            background-color: var(--primary-btn);
            border: none;
            border-radius: 10px;
            color: #ffffff;
            font-size: 15px;
            font-weight: 700;
            cursor: pointer;
            transition: background-color 0.2s ease;
            margin-top: 10px;
            margin-bottom: 25px;
            box-shadow: 0 4px 12px rgba(0, 119, 200, 0.25);
        }

        .btn-primary:hover {
            background-color: var(--primary-btn-hover);
        }

        .register-footer {
            text-align: center;
            font-size: 13.5px;
            color: #64748b;
        }

        .register-footer a {
            color: var(--primary-btn);
            font-weight: 700;
            text-decoration: none;
        }

        .register-footer a:hover {
            text-decoration: underline;
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

    <!-- HERO PACIENTES (SECCIÓN IZQUIERDA REPLICANDO EL MOCKUP EXACTO) -->
    <div class="hero-section">
        <!-- SVG DE CIRCULOS DE FONDO -->
        <svg class="bg-circles-svg" viewBox="0 0 800 800" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="700" cy="400" r="350" stroke="white" stroke-width="1.5"/>
            <circle cx="700" cy="400" r="220" stroke="white" stroke-width="1.5"/>
            <circle cx="200" cy="650" r="400" stroke="white" stroke-width="1"/>
        </svg>

        <div class="hero-inner">
            <!-- HEADER CON LOGO Y SELECTOR DE IDIOMA -->
            <div class="hero-top-row">
                <div class="hero-brand">
                    <img src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png" alt="Clinica Internacional Logo" onerror="this.src='${pageContext.request.contextPath}/assets/images/logoClinical.png'">
                    <span class="brand-title">CLINICA INTERNACIONAL</span>
                </div>

                <div class="lang-selector-pill">
                    <i data-lucide="globe" style="width: 15px; height: 15px;"></i>
                    <span>ES</span>
                    <i data-lucide="chevron-down" style="width: 13px; height: 13px;"></i>
                </div>
            </div>

            <!-- CONTENIDO CENTRAL DEL MOCKUP -->
            <div class="hero-middle">
                <h1 class="hero-title">
                    Tu salud,<br>
                    <em>gestionada</em><br>
                    con precisión.
                </h1>

                <p class="hero-desc">
                    Programa citas, accede a tu historial clínico y mantén comunicación directa con tus especialistas desde un solo lugar.
                </p>

                <!-- FILA DE ESTADISTICAS -->
                <div class="stats-row">
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

                <!-- TARJETA DE TESTIMONIAL DE PACIENTE -->
                <div class="testimonial-card">
                    <p class="testimonial-text">
                        "El sistema de la Clínica Internacional transformó cómo gestiono mis citas, tengo todo coordinado con mis especialistas."
                    </p>
                    <div class="testimonial-user">
                        <div class="user-avatar-circle">DA</div>
                        <div class="user-info">
                            <span class="user-name">Diana Arévalo</span>
                            <span class="user-role">Paciente desde 2024</span>
                        </div>
                    </div>
                </div>
            </div>

            <div></div>
        </div>
    </div>

    <!-- FORMULARIO PACIENTE (EXCLUSIVAMENTE CON DNI) -->
    <div class="form-section">
        <div class="form-inner">
            <div class="portal-switch">
                <a href="${pageContext.request.contextPath}/login/personal">
                    <i data-lucide="shield" style="width: 15px; height: 15px;"></i>
                    <span>Acceso Personal Médico / Admisión</span>
                </a>
            </div>

            <div class="form-header">
                <h2>Portal del Paciente</h2>
                <p>Ingresa con tu DNI para acceder a tus citas y servicios.</p>
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
                    <label for="identificador">Documento de Identidad (DNI / CE)</label>
                    <div class="input-icon-wrapper">
                        <i data-lucide="id-card" class="input-icon"></i>
                        <input type="text" id="identificador" name="identificador" required maxlength="15" pattern="[A-Za-z0-9]+" placeholder="Ej. 72345678 o 001234567">
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
