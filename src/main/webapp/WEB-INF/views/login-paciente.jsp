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
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=Playfair+Display:ital,wght@1,600;1,700&display=swap" rel="stylesheet">
    <!-- Lucide Icons SVG sin emojis -->
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        :root {
            --blue-hero-start: #0052cc;
            --blue-hero-end: #002b70;
            --bg-right: #f8fafc;
            --text-dark: #0f172a;
            --text-muted: #64748b;
            --accent-cyan: #38bdf8;
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
            background: radial-gradient(circle at 20% 20%, rgba(56, 189, 248, 0.15) 0%, transparent 40%),
                        radial-gradient(circle at 80% 80%, rgba(0, 82, 204, 0.3) 0%, transparent 50%),
                        linear-gradient(135deg, var(--blue-hero-start) 0%, var(--blue-hero-end) 100%);
            color: #ffffff;
            display: flex;
            justify-content: center;
            align-items: center;
            position: relative;
            overflow: hidden;
        }

        /* Círculos decorativos de fondo */
        .hero-section::before {
            content: '';
            position: absolute;
            top: -100px;
            right: -100px;
            width: 400px;
            height: 400px;
            border-radius: 50%;
            background: rgba(255, 255, 255, 0.03);
            pointer-events: none;
        }

        .hero-section::after {
            content: '';
            position: absolute;
            bottom: -150px;
            left: -100px;
            width: 500px;
            height: 500px;
            border-radius: 50%;
            background: rgba(56, 189, 248, 0.04);
            pointer-events: none;
        }

        .hero-inner {
            width: 100%;
            max-width: 720px;
            padding: 50px 48px;
            height: 100vh;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            position: relative;
            z-index: 2;
        }

        /* LOGO PROMINENTE Y MAS GRANDE */
        .hero-brand {
            display: flex;
            align-items: center;
            gap: 16px;
            padding-bottom: 20px;
            border-bottom: 1px solid rgba(255, 255, 255, 0.12);
        }

        .hero-brand img {
            height: 58px;
            width: auto;
            object-fit: contain;
            filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.2));
        }

        .brand-text-wrapper {
            display: flex;
            flex-direction: column;
        }

        .brand-text-main {
            font-size: 20px;
            font-weight: 800;
            letter-spacing: 1.2px;
            text-transform: uppercase;
            color: #ffffff;
            line-height: 1.1;
        }

        .brand-text-sub {
            font-size: 12px;
            font-weight: 500;
            letter-spacing: 1.5px;
            text-transform: uppercase;
            color: var(--accent-cyan);
            margin-top: 2px;
        }

        .hero-middle {
            margin: auto 0;
            padding: 30px 0;
        }

        .hero-title {
            font-family: 'Playfair Display', serif;
            font-size: 46px;
            font-weight: 700;
            line-height: 1.15;
            margin-bottom: 20px;
            color: #ffffff;
        }

        .hero-title em {
            font-style: italic;
            font-weight: 400;
            color: #93c5fd;
        }

        .hero-desc {
            font-size: 16px;
            color: rgba(255, 255, 255, 0.88);
            line-height: 1.6;
            max-width: 580px;
            margin-bottom: 32px;
        }

        /* TARJETAS DE CARACTERISTICAS Y SERVICIOS INTEGRADOS EN HERO */
        .features-grid {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 16px;
            margin-bottom: 32px;
        }

        .feature-card-glass {
            background: rgba(255, 255, 255, 0.07);
            backdrop-filter: blur(12px);
            border: 1px solid rgba(255, 255, 255, 0.14);
            border-radius: 12px;
            padding: 16px;
            display: flex;
            align-items: flex-start;
            gap: 14px;
        }

        .feature-icon-box {
            width: 36px;
            height: 36px;
            border-radius: 8px;
            background: rgba(56, 189, 248, 0.2);
            color: var(--accent-cyan);
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
        }

        .feature-title {
            font-size: 13.5px;
            font-weight: 700;
            color: #ffffff;
            margin-bottom: 4px;
        }

        .feature-desc {
            font-size: 12px;
            color: rgba(255, 255, 255, 0.72);
            line-height: 1.4;
        }

        .stats-grid {
            display: flex;
            gap: 40px;
            padding-top: 20px;
            border-top: 1px solid rgba(255, 255, 255, 0.12);
        }

        .stat-item h3 {
            font-size: 30px;
            font-weight: 800;
            color: #ffffff;
        }

        .stat-item p {
            font-size: 12.5px;
            color: rgba(255, 255, 255, 0.75);
            font-weight: 500;
        }

        /* CERTIFICACION DE SEGURIDAD */
        .hero-footer-trust {
            display: flex;
            align-items: center;
            gap: 10px;
            font-size: 12px;
            color: rgba(255, 255, 255, 0.7);
        }

        .trust-icon {
            width: 16px;
            height: 16px;
            stroke: var(--accent-cyan);
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
            border-color: var(--blue-hero-start);
            box-shadow: 0 0 0 4px rgba(0, 82, 204, 0.12);
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
            background-color: var(--blue-hero-start);
            border: none;
            border-radius: 10px;
            color: #ffffff;
            font-size: 15px;
            font-weight: 700;
            cursor: pointer;
            transition: background-color 0.2s ease;
            margin-top: 10px;
            margin-bottom: 25px;
            box-shadow: 0 4px 12px rgba(0, 82, 204, 0.25);
        }

        .btn-primary:hover {
            background-color: #0041a8;
        }

        .register-footer {
            text-align: center;
            font-size: 13.5px;
            color: #64748b;
        }

        .register-footer a {
            color: var(--blue-hero-start);
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

    <!-- HERO PACIENTES (SECCIÓN IZQUIERDA CON LENGUAJE VISUAL RICO) -->
    <div class="hero-section">
        <div class="hero-inner">
            <!-- LOGO MAS GRANDE Y DESTACADO -->
            <div class="hero-brand">
                <img src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png" alt="Clinica Internacional Logo" onerror="this.src='${pageContext.request.contextPath}/assets/images/logoClinical.png'">
                <div class="brand-text-wrapper">
                    <span class="brand-text-main">CLÍNICA INTERNACIONAL</span>
                    <span class="brand-text-sub">PORTAL DE PACIENTES</span>
                </div>
            </div>

            <div class="hero-middle">
                <h1 class="hero-title">Tu salud,<br><em>gestionada</em><br>con máxima precisión.</h1>
                <p class="hero-desc">
                    Accede a tu portal clínico para agendar citas presenciales o virtuales, consultar disponibilidad de especialistas y revisar tu historial de atención en tiempo real.
                </p>

                <!-- DETALLES Y CARACTERISTICAS VISUALES -->
                <div class="features-grid">
                    <div class="feature-card-glass">
                        <div class="feature-icon-box">
                            <i data-lucide="calendar-check" style="width: 20px; height: 20px;"></i>
                        </div>
                        <div>
                            <div class="feature-title">Reserva de Citas 24/7</div>
                            <div class="feature-desc">Elige médico, sede y horario de preferencia al instante.</div>
                        </div>
                    </div>

                    <div class="feature-card-glass">
                        <div class="feature-icon-box">
                            <i data-lucide="zap" style="width: 20px; height: 20px;"></i>
                        </div>
                        <div>
                            <div class="feature-title">Reasignación Inteligente</div>
                            <div class="feature-desc">Alertas automáticas de cupos liberados por cancelación.</div>
                        </div>
                    </div>

                    <div class="feature-card-glass">
                        <div class="feature-icon-box">
                            <i data-lucide="file-text" style="width: 20px; height: 20px;"></i>
                        </div>
                        <div>
                            <div class="feature-title">Historial Clínico Seguro</div>
                            <div class="feature-desc">Acceso confidencial a órdenes médicas y resultados.</div>
                        </div>
                    </div>

                    <div class="feature-card-glass">
                        <div class="feature-icon-box">
                            <i data-lucide="video" style="width: 20px; height: 20px;"></i>
                        </div>
                        <div>
                            <div class="feature-title">Telemedicina en Vivo</div>
                            <div class="feature-desc">Consultas virtuales con especialistas desde cualquier lugar.</div>
                        </div>
                    </div>
                </div>

                <div class="stats-grid">
                    <div class="stat-item">
                        <h3>400+</h3>
                        <p>Médicos Especialistas</p>
                    </div>
                    <div class="stat-item">
                        <h3>40</h3>
                        <p>Especialidades Clínicas</p>
                    </div>
                    <div class="stat-item">
                        <h3>100%</h3>
                        <p>Plataforma Segura</p>
                    </div>
                </div>
            </div>

            <!-- GARANTIA DE PRIVACIDAD -->
            <div class="hero-footer-trust">
                <i data-lucide="shield-check" class="trust-icon"></i>
                <span>Acreditación Internacional JCI &bull; Protección de Datos según Ley N° 29733</span>
            </div>
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
                <p>Ingresa con tu número de DNI para acceder a tus citas y servicios.</p>
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
                    <label for="identificador">Número de DNI</label>
                    <div class="input-icon-wrapper">
                        <i data-lucide="id-card" class="input-icon"></i>
                        <input type="text" id="identificador" name="identificador" required maxlength="8" pattern="[0-9]{8}" inputmode="numeric" placeholder="Ingrese su DNI (8 dígitos)">
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
