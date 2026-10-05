<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Acceso Institucional - Clínica Internacional</title>
    <!-- Fuentes Google -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=Playfair+Display:ital,wght@1,600;1,700&display=swap" rel="stylesheet">
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

        /* SECCIÓN IZQUIERDA CORPORATIVA PERSONAL (62% ANCHO) */
        .hero-section {
            flex: 1.6;
            background: radial-gradient(circle at 15% 20%, rgba(56, 189, 248, 0.1) 0%, transparent 45%),
                        radial-gradient(circle at 85% 80%, rgba(30, 41, 59, 0.5) 0%, transparent 50%),
                        linear-gradient(135deg, var(--slate-dark-start) 0%, var(--slate-dark-end) 100%);
            color: #ffffff;
            display: flex;
            justify-content: center;
            align-items: center;
            position: relative;
            overflow: hidden;
        }

        .hero-section::before {
            content: '';
            position: absolute;
            top: -120px;
            right: -120px;
            width: 450px;
            height: 450px;
            border-radius: 50%;
            background: rgba(255, 255, 255, 0.02);
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
            filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.4));
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
            color: var(--brand-cyan);
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

        .hero-desc {
            font-size: 16px;
            color: rgba(255, 255, 255, 0.82);
            line-height: 1.6;
            max-width: 580px;
            margin-bottom: 32px;
        }

        /* TARJETAS DE MODULOS DEL SISTEMA INTERNO */
        .modules-grid {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 16px;
            margin-bottom: 32px;
        }

        .module-card-glass {
            background: rgba(255, 255, 255, 0.05);
            backdrop-filter: blur(12px);
            border: 1px solid rgba(255, 255, 255, 0.09);
            border-radius: 12px;
            padding: 16px;
            display: flex;
            align-items: flex-start;
            gap: 14px;
        }

        .module-icon-box {
            width: 36px;
            height: 36px;
            border-radius: 8px;
            background: rgba(56, 189, 248, 0.15);
            color: var(--brand-cyan);
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
        }

        .module-title {
            font-size: 13.5px;
            font-weight: 700;
            color: #ffffff;
            margin-bottom: 4px;
        }

        .module-desc {
            font-size: 12px;
            color: rgba(255, 255, 255, 0.65);
            line-height: 1.4;
        }

        .hero-footer-trust {
            display: flex;
            align-items: center;
            gap: 10px;
            font-size: 12px;
            color: rgba(255, 255, 255, 0.6);
            padding-top: 20px;
            border-top: 1px solid rgba(255, 255, 255, 0.1);
        }

        .trust-icon {
            width: 16px;
            height: 16px;
            stroke: var(--brand-cyan);
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
</head>
<body>

<div class="login-wrapper">

    <!-- HERO PERSONAL INSTITUCIONAL (SECCIÓN IZQUIERDA ENRIQUECIDA) -->
    <div class="hero-section">
        <div class="hero-inner">
            <!-- LOGO MAS GRANDE Y DESTACADO -->
            <div class="hero-brand">
                <img src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png" alt="Clinica Internacional Logo" onerror="this.src='${pageContext.request.contextPath}/assets/images/logoClinical.png'">
                <div class="brand-text-wrapper">
                    <span class="brand-text-main">CLÍNICA INTERNACIONAL</span>
                    <span class="brand-text-sub">SISTEMA INSTITUCIONAL</span>
                </div>
            </div>

            <div class="hero-middle">
                <h1 class="hero-title">Sistema Interno de<br>Gestión y Control.</h1>
                <p class="hero-desc">
                    Módulo unificado para médicos especialistas, personal administrativo y admisiones para la gestión de agendas, triaje y control operativo.
                </p>

                <!-- TARJETAS DE MODULOS DEL SISTEMA -->
                <div class="modules-grid">
                    <div class="module-card-glass">
                        <div class="module-icon-box">
                            <i data-lucide="calendar" style="width: 20px; height: 20px;"></i>
                        </div>
                        <div>
                            <div class="module-title">Agenda Médica</div>
                            <div class="module-desc">Programación y gestión de citas de especialistas en tiempo real.</div>
                        </div>
                    </div>

                    <div class="module-card-glass">
                        <div class="module-icon-box">
                            <i data-lucide="refresh-cw" style="width: 20px; height: 20px;"></i>
                        </div>
                        <div>
                            <div class="module-title">Reasignación de Cupos</div>
                            <div class="module-desc">Control automatizado de citas canceladas y lista de espera.</div>
                        </div>
                    </div>

                    <div class="module-card-glass">
                        <div class="module-icon-box">
                            <i data-lucide="users" style="width: 20px; height: 20px;"></i>
                        </div>
                        <div>
                            <div class="module-title">Admisión & Pacientes</div>
                            <div class="module-desc">Registro y validación de identidad para ingreso a consultorio.</div>
                        </div>
                    </div>

                    <div class="module-card-glass">
                        <div class="module-icon-box">
                            <i data-lucide="shield-alert" style="width: 20px; height: 20px;"></i>
                        </div>
                        <div>
                            <div class="module-title">Auditoría de Seguridad</div>
                            <div class="module-desc">Trazabilidad completa de accesos y prevención de amenazas.</div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="hero-footer-trust">
                <i data-lucide="lock" class="trust-icon"></i>
                <span>Red Privada Corporativa &bull; Acceso Restringido a Personal Autorizado</span>
            </div>
        </div>
    </div>

    <!-- FORMULARIO INSTITUCIONAL -->
    <div class="form-section">
        <div class="form-inner">
            <div class="portal-switch">
                <a href="${pageContext.request.contextPath}/login/paciente">
                    <i data-lucide="user" style="width: 15px; height: 15px;"></i>
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

            <form action="${pageContext.request.contextPath}/login/personal" method="POST" autocomplete="off">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}" />

                <div class="form-group">
                    <label for="identificador">Correo Institucional</label>
                    <div class="input-icon-wrapper">
                        <i data-lucide="mail" class="input-icon"></i>
                        <input type="email" id="identificador" name="identificador" required placeholder="usuario@clinicainternacional.pe">
                    </div>
                </div>

                <div class="form-group">
                    <label for="password">Contraseña</label>
                    <div class="input-icon-wrapper">
                        <i data-lucide="lock" class="input-icon"></i>
                        <input type="password" id="password" name="password" required placeholder="••••••••">
                    </div>
                </div>

                <button type="submit" class="btn-primary">Acceder al Sistema</button>
            </form>
        </div>
    </div>

</div>

<script>
    lucide.createIcons();
</script>

</body>
</html>
