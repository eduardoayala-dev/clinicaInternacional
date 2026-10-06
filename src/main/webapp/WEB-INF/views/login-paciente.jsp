<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    boolean showRegister = false;
    if ("register".equals(request.getParameter("action")) || session.getAttribute("registerError") != null) {
        showRegister = true;
    }
    request.setAttribute("showRegister", showRegister);
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Portal de Pacientes - Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=Playfair+Display:ital,wght@1,500;1,600;1,700&display=swap" rel="stylesheet">
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
            -webkit-font-smoothing: antialiased;
            -moz-osx-font-smoothing: grayscale;
        }

        html, body {
            height: 100vh;
            width: 100%;
            overflow: hidden;
        }

        body {
            display: flex;
            background: linear-gradient(to right, var(--blue-hero-bg) 62%, var(--bg-right) 38%);
        }

        .login-wrapper {
            display: flex;
            width: 100%;
            height: 100vh;
        }

        .hero-section {
            flex: 1.6;
            background: linear-gradient(135deg, #00509e 0%, #00376e 100%);
            color: #ffffff;
            display: flex;
            justify-content: center;
            align-items: center;
            position: relative;
            overflow: hidden;
            height: 100vh;
            will-change: flex;
            transition: flex 0.45s cubic-bezier(0.25, 1, 0.5, 1);
        }

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
            padding: clamp(30px, 5vw, 48px);
            height: 100vh;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            position: relative;
            z-index: 2;
        }

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
            height: clamp(40px, 4vw, 52px);
            width: auto;
            object-fit: contain;
        }

        .brand-title {
            font-size: clamp(16px, 1.8vw, 21px);
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
            font-size: clamp(32px, 4vw, 52px);
            font-weight: 800;
            line-height: 1.12;
            margin-bottom: 20px;
            color: #ffffff;
            letter-spacing: -0.5px;
            transition: font-size 0.45s ease;
        }

        .hero-title em {
            font-family: 'Playfair Display', serif;
            font-style: italic;
            font-weight: 500;
            color: #ffffff;
        }

        .hero-desc {
            font-size: clamp(15px, 1.5vw, 16.5px);
            color: rgba(255, 255, 255, 0.88);
            line-height: 1.6;
            max-width: 560px;
            margin-bottom: 40px;
        }

        .stats-row {
            display: flex;
            gap: clamp(20px, 3vw, 48px);
            margin-bottom: 36px;
            transition: opacity 0.3s ease;
            flex-wrap: wrap;
        }

        .stat-item h3 {
            font-size: clamp(26px, 2.5vw, 34px);
            font-weight: 800;
            color: #ffffff;
            line-height: 1;
            margin-bottom: 6px;
        }

        .stat-item p {
            font-size: clamp(12px, 1.2vw, 13.5px);
            color: rgba(255, 255, 255, 0.75);
            font-weight: 500;
        }

        .testimonial-card {
            background: rgba(255, 255, 255, 0.08);
            border: 1px solid rgba(255, 255, 255, 0.16);
            border-radius: 16px;
            padding: clamp(16px, 2vw, 24px);
            backdrop-filter: blur(10px);
            transition: opacity 0.3s ease;
        }

        .testimonial-text {
            font-size: clamp(13px, 1.3vw, 14.5px);
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

        .user-name { font-size: 14px; font-weight: 700; color: #ffffff; }
        .user-role { font-size: 12px; color: rgba(255, 255, 255, 0.65); }

        /* SECCIÓN DERECHA FORMULARIO PACIENTES */
        .form-section {
            flex: 1;
            background-color: var(--bg-right);
            padding: clamp(20px, 3vw, 40px);
            height: 100vh;
            overflow-y: auto; 
            overflow-x: hidden;
            display: flex;
            flex-direction: column;
            align-items: center;
            will-change: flex;
            transition: flex 0.45s cubic-bezier(0.25, 1, 0.5, 1);
        }

        .form-inner-wrapper {
            margin: auto 0;
            width: 100%;
            max-width: 400px;
            position: relative;
            will-change: max-width;
            transition: max-width 0.45s cubic-bezier(0.25, 1, 0.5, 1);
        }

        .login-wrapper.is-registering .hero-section { flex: 0.8; }
        .login-wrapper.is-registering .form-section { flex: 1.8; }
        .login-wrapper.is-registering .form-inner-wrapper { max-width: 780px; }
        .login-wrapper.is-registering .hero-title { font-size: clamp(26px, 3vw, 40px); }
        .login-wrapper.is-registering .stats-row, 
        .login-wrapper.is-registering .testimonial-card { opacity: 0.15; }

        .spa-view {
            transition: opacity 0.3s ease, transform 0.3s ease;
            width: 100%;
        }

        .view-active {
            opacity: 1;
            transform: translateY(0);
            position: relative;
            z-index: 2;
            pointer-events: auto;
        }

        .view-hidden {
            opacity: 0;
            transform: translateY(15px);
            position: absolute;
            top: 0;
            left: 0;
            z-index: 1;
            pointer-events: none;
            visibility: hidden;
        }

        .portal-switch {
            text-align: right;
            margin-bottom: clamp(20px, 3vh, 30px);
        }

        .portal-switch a {
            color: #0284c7;
            font-size: clamp(12px, 1vw, 13px);
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
            margin-bottom: clamp(15px, 2vh, 25px);
        }

        .form-header h2 {
            font-size: clamp(22px, 2.5vw, 28px);
            font-weight: 800;
            color: var(--text-dark);
            margin-bottom: 8px;
            letter-spacing: -0.5px;
        }

        .form-header p {
            font-size: clamp(13px, 1.2vw, 14px);
            color: var(--text-muted);
            line-height: 1.4;
        }

        .alert-error, .alert-success {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 12px 14px;
            border-radius: 8px;
            font-size: clamp(12px, 1vw, 13px);
            margin-bottom: 22px;
        }

        .alert-error {
            background-color: #fef2f2;
            border: 1px solid #fca5a5;
            color: #b91c1c;
        }

        .alert-success {
            background-color: #f0fdf4;
            border: 1px solid #86efac;
            color: #15803d;
        }

        .form-group {
            margin-bottom: clamp(15px, 2vh, 22px);
            position: relative;
        }

        .form-group label {
            display: block;
            font-size: clamp(12.5px, 1vw, 13.5px);
            font-weight: 600;
            color: #334155;
            margin-bottom: 8px;
        }

        .input-icon-wrapper {
            position: relative;
            display: flex;
            align-items: center;
        }

        .input-icon-wrapper input, .registro-form select, .registro-form input:not([type="checkbox"]) {
            width: 100%;
            padding: clamp(12px, 1.5vh, 14px);
            border: 1px solid #cbd5e1;
            border-radius: 10px;
            font-size: 16px; 
            background-color: #ffffff;
            color: var(--text-dark);
            transition: all 0.2s ease;
        }
        
        .input-icon-wrapper input {
            padding-left: 44px;
            padding-right: 44px; /* Space for the eye icon */
        }
        
        .pwd-input-clean {
            padding-left: 14px !important;
            padding-right: 44px !important;
        }

        .input-icon-wrapper input:focus, .registro-form select:focus, .registro-form input:not([type="checkbox"]):focus {
            outline: none;
            border-color: var(--primary-btn);
            box-shadow: 0 0 0 4px rgba(0, 119, 200, 0.12);
        }

        .registro-form input.error-input {
            border-color: #ef4444;
            box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.12);
        }

        .input-icon {
            position: absolute;
            left: 14px;
            color: #94a3b8;
            width: 20px;
            height: 20px;
            pointer-events: none;
        }
        
        .input-icon-right {
            position: absolute;
            right: 14px;
            color: #94a3b8;
            width: 20px;
            height: 20px;
            cursor: pointer;
            transition: color 0.2s;
        }
        
        .input-icon-right:hover {
            color: #0f172a;
        }

        .btn-primary {
            width: 100%;
            padding: clamp(14px, 2vh, 16px);
            background-color: var(--primary-btn);
            border: none;
            border-radius: 10px;
            color: #ffffff;
            font-size: clamp(14px, 1.2vw, 15px);
            font-weight: 700;
            cursor: pointer;
            transition: background-color 0.2s ease;
            margin-top: 10px;
            margin-bottom: clamp(15px, 2vh, 25px);
            box-shadow: 0 4px 12px rgba(0, 119, 200, 0.25);
        }

        .btn-primary:hover { background-color: var(--primary-btn-hover); }
        .btn-primary:disabled {
            background-color: #94a3b8;
            cursor: not-allowed;
            box-shadow: none;
        }

        .register-footer {
            text-align: center;
            font-size: clamp(12px, 1vw, 13.5px);
            color: #64748b;
        }

        .register-footer a {
            color: var(--primary-btn);
            font-weight: 700;
            text-decoration: none;
            cursor: pointer;
        }

        .register-footer a:hover { text-decoration: underline; }

        .grid-3 {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: clamp(10px, 1.5vw, 20px);
            margin-bottom: 5px;
        }

        .grid-2 {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: clamp(10px, 1.5vw, 20px);
            margin-bottom: 5px;
        }

        .section-title {
            font-size: clamp(11px, 1vw, 12px);
            font-weight: 800;
            color: #94a3b8;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            margin: clamp(10px, 1.5vh, 20px) 0 clamp(8px, 1vh, 15px) 0;
            padding-left: 10px;
            border-left: 3px solid var(--primary-btn);
        }

        .checkbox-group {
            display: flex;
            align-items: flex-start;
            gap: 10px;
            margin: clamp(10px, 1.5vh, 20px) 0;
            background: #f1f5f9;
            padding: clamp(10px, 1.5vw, 15px);
            border-radius: 10px;
            border: 1px solid #e2e8f0;
        }

        .checkbox-group input { margin-top: 3px; width: 18px; height: 18px; cursor: pointer; }
        .checkbox-group label {
            font-size: clamp(12.5px, 1vw, 14px);
            line-height: 1.5;
            color: #334155;
            font-weight: 500;
            margin: 0;
            cursor: pointer;
        }

        .pwd-checklist {
            background: #f8fafc;
            padding: 14px;
            border-radius: 10px;
            border: 1px solid #e2e8f0;
            display: flex;
            flex-direction: column;
            justify-content: center;
            height: 100%;
            gap: 8px;
        }
        
        .pwd-check-item {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 13px;
            color: #64748b;
            transition: color 0.3s;
        }
        
        .pwd-check-item.valid { color: #15803d; font-weight: 500; }
        .pwd-check-item.valid i { color: #22c55e; }

        @media (max-width: 1200px) {
            .login-wrapper.is-registering .form-inner-wrapper { max-width: 650px; }
            .grid-3 { grid-template-columns: 1fr 1fr; }
        }

        @media (max-width: 960px) {
            html, body { overflow: auto; }
            body { background: var(--bg-right); }
            .login-wrapper { height: auto; min-height: 100vh; }
            .hero-section { display: none !important; }
            .form-section { width: 100% !important; flex: 1 !important; height: auto; min-height: 100vh; padding: 20px; overflow: visible; justify-content: center; }
            .form-inner-wrapper { max-width: 500px !important; margin: 40px auto; }
            .grid-3, .grid-2 { grid-template-columns: 1fr; gap: 15px; }
            .pwd-checklist { margin-top: -10px; margin-bottom: 10px; }
        }
        
        @media (max-width: 480px) {
            .form-inner-wrapper { margin: 10px auto; }
            .form-section { padding: 15px; }
            .checkbox-group { padding: 12px; }
        }
    </style>
</head>
<body>

<div class="login-wrapper ${showRegister ? 'is-registering' : ''}" id="main-wrapper">

    <!-- HERO PACIENTES -->
    <div class="hero-section">
        <svg class="bg-circles-svg" viewBox="0 0 800 800" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="700" cy="400" r="350" stroke="white" stroke-width="1.5"/>
            <circle cx="700" cy="400" r="220" stroke="white" stroke-width="1.5"/>
            <circle cx="200" cy="650" r="400" stroke="white" stroke-width="1"/>
        </svg>

        <div class="hero-inner">
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

            <div class="hero-middle">
                <h1 class="hero-title">
                    Tu salud,<br>
                    <em>gestionada</em><br>
                    con precisión.
                </h1>

                <p class="hero-desc">
                    Programa citas, accede a tu historial clínico y mantén comunicación directa con tus especialistas desde un solo lugar.
                </p>

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

    <!-- SECCIÓN DERECHA FORMULARIO -->
    <div class="form-section" id="rightSection">
        <div class="form-inner-wrapper">
            
            <!-- VISTA DE LOGIN -->
            <div id="view-login" class="spa-view ${not showRegister ? 'view-active' : 'view-hidden'}">
                <div class="portal-switch">
                    <a href="${pageContext.request.contextPath}/login/personal">
                        <i data-lucide="shield" style="width: 15px; height: 15px;"></i>
                        <span>Acceso Médico / Admisión</span>
                    </a>
                </div>

                <div class="form-header">
                    <h2>Portal del Paciente</h2>
                    <p>Ingresa con tu DNI para acceder a tus citas y servicios.</p>
                </div>

                <c:if test="${not empty errorMessage and not showRegister}">
                    <div class="alert-error">
                        <i data-lucide="alert-circle" style="width: 18px; height: 18px; flex-shrink: 0;"></i>
                        <span><c:out value="${errorMessage}" /></span>
                    </div>
                </c:if>
                <c:if test="${param.msg == 'registro_exitoso'}">
                    <div class="alert-success">
                        <i data-lucide="check-circle" style="width: 18px; height: 18px; flex-shrink: 0;"></i>
                        <span>¡Cuenta creada con éxito! Ahora puedes iniciar sesión.</span>
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
                            <i data-lucide="eye" class="input-icon-right" onclick="togglePwdVisibility('password', this)"></i>
                        </div>
                    </div>

                    <button type="submit" class="btn-primary">Ingresar al Portal</button>
                </form>

                <div class="register-footer">
                    ¿Primera vez en la clínica? <a onclick="toggleView('view-register')">Crear cuenta de Paciente</a>
                </div>
            </div>

            <!-- VISTA DE REGISTRO EXPANDIDA -->
            <div id="view-register" class="spa-view ${showRegister ? 'view-active' : 'view-hidden'}">
                
                <div class="form-header" style="margin-bottom: 15px;">
                    <h2>Crear Cuenta de Paciente</h2>
                    <p>Complete sus datos de forma segura. La información será validada y protegida.</p>
                </div>

                <c:if test="${not empty sessionScope.registerError}">
                    <div class="alert-error">
                        <i data-lucide="alert-circle" style="width: 18px; height: 18px; flex-shrink: 0;"></i>
                        <span><c:out value="${sessionScope.registerError}" /></span>
                    </div>
                    <c:remove var="registerError" scope="session" />
                </c:if>
                
                <!-- ERROR DE DNI AL ENVIAR -->
                <div id="dniSubmitError" class="alert-error" style="display:none;">
                    <i data-lucide="alert-circle" style="width: 18px; height: 18px; flex-shrink: 0;"></i>
                    <span>Por razones de seguridad, la contraseña no debe contener su número de documento.</span>
                </div>

                <form class="registro-form" id="registerForm" action="${pageContext.request.contextPath}/registro" method="POST">
                    
                    <div class="section-title">01. Datos Personales</div>
                    <div class="grid-3">
                        <div class="form-group">
                            <label>Nombres Completos *</label>
                            <input type="text" name="nombres" required placeholder="Ej. Juan Alberto">
                        </div>
                        <div class="form-group">
                            <label>Apellidos *</label>
                            <input type="text" name="apellidos" required placeholder="Ej. Pérez Gómez">
                        </div>
                        <div class="form-group">
                            <label>Fecha de Nacimiento *</label>
                            <input type="date" name="fechaNacimiento" required>
                        </div>
                    </div>
                    <div class="grid-3">
                        <div class="form-group">
                            <label>Tipo de Documento *</label>
                            <select name="tipoDocumento" id="regTipoDoc" required>
                                <option value="DNI">DNI</option>
                                <option value="CE">Carnet de Extranjería</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>Nº de Documento *</label>
                            <input type="text" name="dni" id="regDni" required placeholder="Ej. 72345678" pattern="[A-Za-z0-9]+">
                        </div>
                        <div class="form-group">
                            <label>Sexo / Género *</label>
                            <select name="genero" required>
                                <option value="">[ Seleccionar ]</option>
                                <option value="M">Masculino</option>
                                <option value="F">Femenino</option>
                                <option value="O">Otro</option>
                            </select>
                        </div>
                    </div>

                    <div class="section-title">02. Contacto</div>
                    <div class="grid-3">
                        <div class="form-group">
                            <label>Teléfono / Celular *</label>
                            <input type="text" name="telefono" required placeholder="Ej. 987 654 321">
                        </div>
                        <div class="form-group">
                            <label>Correo Electrónico *</label>
                            <input type="email" name="email" required placeholder="correo@clinica.com">
                        </div>
                        <div class="form-group">
                            <label>Dirección *</label>
                            <input type="text" name="direccion" required placeholder="Ej. Av. Las Flores 123">
                        </div>
                    </div>

                    <div class="section-title">03. Seguridad</div>
                    <div class="grid-2">
                        <!-- COL 1: Input de Contraseña -->
                        <div class="form-group" style="margin-bottom:0;">
                            <label>Crear Contraseña *</label>
                            <div class="input-icon-wrapper">
                                <input type="password" name="password" id="regPwd" class="pwd-input-clean" required placeholder="Crea tu contraseña">
                                <i data-lucide="eye" class="input-icon-right" onclick="togglePwdVisibility('regPwd', this)"></i>
                            </div>
                            <p style="font-size:12px; color:#64748b; margin-top:8px;">Te sugerimos usar una frase que te sea fácil de recordar. Ya no es necesario confirmar la contraseña.</p>
                        </div>
                        
                        <!-- COL 2: Requisitos (se adapta a la derecha ahorrando espacio vertical) -->
                        <div class="form-group" style="margin-bottom:0;">
                            <div class="pwd-checklist">
                                <div style="font-size:12px; font-weight:700; color:#475569; margin-bottom:4px; text-transform:uppercase; letter-spacing:0.5px;">Requisitos</div>
                                <div class="pwd-check-item" id="chk-len"><i data-lucide="circle" style="width:14px;height:14px;"></i> Mínimo 8 caracteres</div>
                                <div class="pwd-check-item" id="chk-upper"><i data-lucide="circle" style="width:14px;height:14px;"></i> Al menos 1 mayúscula</div>
                                <div class="pwd-check-item" id="chk-lower"><i data-lucide="circle" style="width:14px;height:14px;"></i> Al menos 1 minúscula</div>
                                <div class="pwd-check-item" id="chk-num"><i data-lucide="circle" style="width:14px;height:14px;"></i> Al menos 1 número</div>
                                <div class="pwd-check-item" id="chk-sym"><i data-lucide="circle" style="width:14px;height:14px;"></i> Al menos 1 símbolo (@, #, $)</div>
                            </div>
                        </div>
                    </div>

                    <div class="checkbox-group">
                        <input type="checkbox" id="terminos" name="terminos" required>
                        <label for="terminos">Comprendo y acepto los términos del servicio y política de privacidad de la Clínica Internacional.</label>
                    </div>

                    <div class="grid-2" style="align-items: center;">
                        <button type="submit" id="btnSubmitReg" class="btn-primary" style="margin: 0;" disabled>Confirmar y Registrarme</button>
                        <div class="register-footer" style="margin: 0; text-align: left; padding-left: 10px;">
                            <a onclick="toggleView('view-login')" style="font-size: clamp(13px, 1.2vw, 14.5px);">
                                <i data-lucide="arrow-left" style="width: 16px; height: 16px; display: inline-block; vertical-align: middle;"></i> 
                                Volver a iniciar sesión
                            </a>
                        </div>
                    </div>
                </form>
            </div>

        </div>
    </div>

</div>

<script>
    lucide.createIcons();

    // Alternar visibilidad de contraseña (Eye icon)
    function togglePwdVisibility(inputId, iconEl) {
        const input = document.getElementById(inputId);
        if (input.type === 'password') {
            input.type = 'text';
            iconEl.setAttribute('data-lucide', 'eye-off');
        } else {
            input.type = 'password';
            iconEl.setAttribute('data-lucide', 'eye');
        }
        lucide.createIcons({root: iconEl.parentNode});
    }

    function toggleView(targetId) {
        window.requestAnimationFrame(() => {
            const loginView = document.getElementById('view-login');
            const registerView = document.getElementById('view-register');
            const wrapper = document.getElementById('main-wrapper');
            
            window.history.replaceState({}, document.title, "${pageContext.request.contextPath}/login");
            
            if (targetId === 'view-register') {
                wrapper.classList.add('is-registering');
                loginView.style.opacity = '0';
                setTimeout(() => {
                    loginView.classList.remove('view-active');
                    loginView.classList.add('view-hidden');
                    registerView.classList.remove('view-hidden');
                    registerView.classList.add('view-active');
                    loginView.style.opacity = '';
                }, 200); 
            } else {
                wrapper.classList.remove('is-registering');
                registerView.style.opacity = '0';
                setTimeout(() => {
                    registerView.classList.remove('view-active');
                    registerView.classList.add('view-hidden');
                    loginView.classList.remove('view-hidden');
                    loginView.classList.add('view-active');
                    registerView.style.opacity = '';
                }, 200);
            }
        });
    }

    // VALIDACION EN TIEMPO REAL
    const pwd = document.getElementById('regPwd');
    const dniInput = document.getElementById('regDni');
    const btnSubmit = document.getElementById('btnSubmitReg');
    const registerForm = document.getElementById('registerForm');
    const dniSubmitError = document.getElementById('dniSubmitError');
    
    function checkIcon(elementId, isValid) {
        const el = document.getElementById(elementId);
        if(isValid) {
            el.classList.add('valid');
            el.innerHTML = '<i data-lucide="check-circle" style="width:16px;height:16px;"></i> ' + el.innerText;
        } else {
            el.classList.remove('valid');
            el.innerHTML = '<i data-lucide="circle" style="width:14px;height:14px;"></i> ' + el.innerText;
        }
        lucide.createIcons({root: el});
    }

    function validatePassword() {
        const val = pwd.value;
        const isLen = val.length >= 8;
        const isUpper = /[A-Z]/.test(val);
        const isLower = /[a-z]/.test(val);
        const isNum = /[0-9]/.test(val);
        const isSym = /[@#$%^&+=!_\-]/.test(val);

        checkIcon('chk-len', isLen);
        checkIcon('chk-upper', isUpper);
        checkIcon('chk-lower', isLower);
        checkIcon('chk-num', isNum);
        checkIcon('chk-sym', isSym);

        if(isLen && isUpper && isLower && isNum && isSym) {
            btnSubmit.disabled = false;
        } else {
            btnSubmit.disabled = true;
        }
    }

    pwd.addEventListener('input', validatePassword);

    // Validación final al enviar (No DNI)
    registerForm.addEventListener('submit', function(e) {
        const val = pwd.value;
        const dniVal = dniInput.value;
        
        if (dniVal.length > 0 && val.includes(dniVal)) {
            e.preventDefault();
            dniSubmitError.style.display = 'flex';
            pwd.classList.add('error-input');
            
            // Auto scroll up to show error
            document.getElementById('rightSection').scrollTo({ top: 0, behavior: 'smooth' });
        } else {
            dniSubmitError.style.display = 'none';
            pwd.classList.remove('error-input');
        }
    });

</script>

</body>
</html>