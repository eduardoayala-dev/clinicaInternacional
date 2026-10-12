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
    <title>Clínica Internacional - Portal del Paciente</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:wght@600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/login-paciente.css">
</head>
<body>

<div class="login-container ${showRegister ? 'is-registering' : ''}" id="mainContainer">

    <!-- ==================== LADO IZQUIERDO: HERO ==================== -->
    <aside class="hero-pane">
        <!-- Logo y Sistema -->
        <div class="hero-brand">
            <div class="brand-icon-box">
                <img src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png" alt="Logo" onerror="this.src='${pageContext.request.contextPath}/assets/images/logoClinical.png'">
            </div>
            <div class="brand-text">
                <span class="brand-title">Clínica Internacional</span>
                <span class="brand-subtitle">Sistema de citas</span>
            </div>
        </div>

        <!-- Mensaje Principal -->
        <div class="hero-body">
            <h1 class="hero-title">
                Tu salud, gestionada<br>
                con precisión
            </h1>
            <p class="hero-desc">
                Reserva, reprograma o cancela tus citas, y recibe la oferta de un cupo liberado si estás en la lista de espera.
            </p>

            <ul class="hero-features">
                <li><span class="bullet"></span> Pacientes</li>
                <li><span class="bullet"></span> Médicos</li>
                <li><span class="bullet"></span> Personal administrativo</li>
                <li><span class="bullet"></span> Administradores</li>
            </ul>
        </div>

        <!-- Footer Izquierdo: Acceso Personal -->
        <div class="hero-footer">
            <span>Portal de Pacientes</span>
            <a href="${pageContext.request.contextPath}/login/personal">
                Acceso Médicos y Admisión &rarr;
            </a>
        </div>
    </aside>

    <!-- ==================== LADO DERECHO: FORMULARIOS ==================== -->
    <main class="form-pane" id="formPane">
        <div class="form-inner-box">
            
            <!-- VISTA 1: INICIAR SESIÓN -->
            <section id="view-login" class="auth-view ${not showRegister ? 'active' : ''}">
                <div class="auth-header">
                    <h2 class="auth-title">Iniciar sesión</h2>
                    <p class="auth-subtitle">Ingresa con tu documento de identidad registrado.</p>
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
                        <span>¡Cuenta creada con éxito! Ya puedes iniciar sesión con tu documento.</span>
                    </div>
                </c:if>

                <c:if test="${param.msg == 'sesion_cerrada'}">
                    <div class="alert-success">
                        <i data-lucide="check-circle" style="width: 18px; height: 18px; flex-shrink: 0;"></i>
                        <span>Has cerrado sesión correctamente.</span>
                    </div>
                </c:if>

                <form action="${pageContext.request.contextPath}/login/paciente" method="POST" autocomplete="off">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}" />

                    <!-- Documento -->
                    <div class="form-group">
                        <label class="form-label" for="identificador">N° de documento (DNI o Carné de Extranjería) *</label>
                        <div class="input-box">
                            <input type="text" id="identificador" name="identificador" class="form-input" required maxlength="15" pattern="[A-Za-z0-9]+" placeholder="Ej. 45879632 o 001234567">
                        </div>
                    </div>

                    <!-- Contraseña -->
                    <div class="form-group" style="margin-bottom: 8px;">
                        <label class="form-label" for="password">Contraseña *</label>
                        <div class="input-box has-right-icon">
                            <input type="password" id="password" name="password" class="form-input" required placeholder="Ingresa tu contraseña">
                            <button type="button" class="btn-toggle-eye" onclick="togglePwdVisibility('password', this)" title="Mostrar/ocultar contraseña">
                                <i data-lucide="eye" style="width: 18px; height: 18px;"></i>
                            </button>
                        </div>
                    </div>

                    <!-- Olvidó contraseña -->
                    <div class="forgot-pwd-row">
                        <a href="javascript:void(0)" onclick="alert('Para restablecer su contraseña, por favor acérquese al módulo de admisión o llame al Call Center de Citas (01 619-6161).');" class="forgot-pwd-link">
                            ¿Olvidó su contraseña?
                        </a>
                    </div>

                    <!-- Botón Ingresar -->
                    <button type="submit" class="btn-submit-primary">
                        Ingresar
                    </button>

                    <!-- Separador -->
                    <div class="auth-divider">
                        <span>¿Paciente nuevo?</span>
                    </div>

                    <!-- Botón Crear Cuenta Nueva -->
                    <button type="button" class="btn-create-account" onclick="toggleView('view-register')">
                        Crear cuenta nueva
                    </button>
                </form>

                <!-- Footer de ayuda -->
                <div class="auth-help-footer">
                    ¿Necesita ayuda? Contactar call center citas
                </div>
            </section>

            <!-- VISTA 2: CREAR CUENTA DE PACIENTE -->
            <section id="view-register" class="auth-view ${showRegister ? 'active' : ''}">
                <div class="reg-header-row">
                    <h2 class="reg-title">Crear cuenta de paciente</h2>
                    <span class="req-note">* Campos obligatorios</span>
                </div>

                <c:if test="${not empty sessionScope.registerError}">
                    <div class="alert-error">
                        <i data-lucide="alert-circle" style="width: 18px; height: 18px; flex-shrink: 0;"></i>
                        <span><c:out value="${sessionScope.registerError}" /></span>
                    </div>
                    <c:remove var="registerError" scope="session" />
                </c:if>

                <div id="dniSubmitError" class="alert-error" style="display:none;">
                    <i data-lucide="alert-circle" style="width: 18px; height: 18px; flex-shrink: 0;"></i>
                    <span>Por seguridad, la contraseña no debe contener tu número de documento.</span>
                </div>

                <div id="pwdMismatchError" class="alert-error" style="display:none;">
                    <i data-lucide="alert-circle" style="width: 18px; height: 18px; flex-shrink: 0;"></i>
                    <span>Las contraseñas no coinciden. Por favor verifícalas.</span>
                </div>

                <form id="registerForm" action="${pageContext.request.contextPath}/registro" method="POST" autocomplete="off">
                    <input type="hidden" name="terminos" value="on">

                    <!-- SECCIÓN 01: DATOS PERSONALES -->
                    <div class="reg-card">
                        <div class="reg-card-header">
                            <span class="reg-badge">01</span>
                            <span class="reg-card-title">Datos personales</span>
                            <span class="reg-card-subtitle">· Identificación</span>
                        </div>

                        <div class="form-grid-2">
                            <div class="form-group">
                                <label class="form-label">Nombres completos *</label>
                                <input type="text" name="nombres" class="form-input" required placeholder="Ej. María Elena">
                            </div>
                            <div class="form-group">
                                <label class="form-label">Apellidos *</label>
                                <input type="text" name="apellidos" class="form-input" required placeholder="Ej. Pérez Gómez">
                            </div>
                        </div>

                        <div class="form-grid-2">
                            <div class="form-group">
                                <label class="form-label">Tipo de documento *</label>
                                <select name="tipoDocumento" id="regTipoDoc" class="form-select" required>
                                    <option value="DNI" selected>DNI</option>
                                    <option value="CE">Carnet de Extranjería</option>
                                </select>
                            </div>
                            <div class="form-group">
                                <label class="form-label">Número de documento *</label>
                                <input type="text" name="dni" id="regDni" class="form-input" required placeholder="Ej. 45879632" pattern="[A-Za-z0-9]+">
                            </div>
                        </div>

                        <div class="form-grid-2">
                            <div class="form-group">
                                <label class="form-label">Fecha de nacimiento *</label>
                                <input type="date" name="fechaNacimiento" class="form-input" required>
                            </div>
                            <div class="form-group">
                                <label class="form-label">Sexo / género *</label>
                                <select name="genero" class="form-select" required>
                                    <option value="" disabled selected>Seleccionar</option>
                                    <option value="F">Femenino</option>
                                    <option value="M">Masculino</option>
                                    <option value="O">Otro</option>
                                </select>
                            </div>
                        </div>
                    </div>

                    <!-- SECCIÓN 02: DATOS DE CONTACTO Y UBICACIÓN -->
                    <div class="reg-card">
                        <div class="reg-card-header">
                            <span class="reg-badge">02</span>
                            <span class="reg-card-title">Datos de contacto y ubicación</span>
                            <span class="reg-card-subtitle">· Notificaciones y citas</span>
                        </div>

                        <div class="form-grid-2">
                            <div class="form-group">
                                <label class="form-label">Teléfono / celular *</label>
                                <input type="text" name="telefono" class="form-input" required placeholder="Ej. 987 654 321">
                            </div>
                            <div class="form-group">
                                <label class="form-label">Correo electrónico *</label>
                                <input type="email" name="email" class="form-input" required placeholder="nombre@correo.com">
                            </div>
                        </div>

                        <div class="form-group">
                            <label class="form-label">Canal preferido de aviso *</label>
                            <div class="channel-options">
                                <label class="channel-pill" onclick="selectChannel(this)">
                                    <input type="radio" name="canalAviso" value="SMS">
                                    <span class="radio-circle"></span>
                                    <span>SMS</span>
                                </label>
                                <label class="channel-pill active" onclick="selectChannel(this)">
                                    <input type="radio" name="canalAviso" value="WhatsApp" checked>
                                    <span class="radio-circle"></span>
                                    <span>WhatsApp</span>
                                </label>
                                <label class="channel-pill" onclick="selectChannel(this)">
                                    <input type="radio" name="canalAviso" value="Correo">
                                    <span class="radio-circle"></span>
                                    <span>Correo</span>
                                </label>
                            </div>
                        </div>

                        <div class="form-group" style="margin-bottom: 6px;">
                            <label class="form-label">Dirección / zona de residencia *</label>
                            <input type="text" name="direccion" class="form-input" required placeholder="Ej. Av. San Borja Norte, San Borja">
                        </div>
                    </div>

                    <!-- SECCIÓN 03: CREDENCIALES DE ACCESO -->
                    <div class="reg-card">
                        <div class="reg-card-header">
                            <span class="reg-badge">03</span>
                            <span class="reg-card-title">Credenciales de acceso</span>
                            <span class="reg-card-subtitle">· Portal del paciente</span>
                        </div>

                        <div class="form-grid-2">
                            <div class="form-group">
                                <label class="form-label">Crear contraseña *</label>
                                <div class="input-box has-right-icon">
                                    <input type="password" name="password" id="regPwd" class="form-input" required placeholder="••••••••">
                                    <button type="button" class="btn-toggle-eye" onclick="togglePwdVisibility('regPwd', this)" title="Mostrar/ocultar contraseña">
                                        <i data-lucide="eye" style="width: 18px; height: 18px;"></i>
                                    </button>
                                </div>
                            </div>

                            <div class="form-group">
                                <label class="form-label">Confirmar contraseña *</label>
                                <div class="input-box has-right-icon">
                                    <input type="password" name="confirmPassword" id="regPwdConfirm" class="form-input" required placeholder="••••••••">
                                    <button type="button" class="btn-toggle-eye" onclick="togglePwdVisibility('regPwdConfirm', this)" title="Mostrar/ocultar contraseña">
                                        <i data-lucide="eye" style="width: 18px; height: 18px;"></i>
                                    </button>
                                </div>
                                <div class="pwd-match-indicator" id="pwdMatchMsg" style="display: none;">
                                    <i data-lucide="check" style="width: 14px; height: 14px;"></i> Las contraseñas coinciden
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- SECCIÓN 04: DATOS DE SALUD Y COBERTURA -->
                    <div class="reg-card">
                        <div class="reg-card-header">
                            <span class="reg-badge">04</span>
                            <span class="reg-card-title">Datos de salud y cobertura</span>
                            <span class="reg-card-subtitle">· Información del paciente</span>
                        </div>

                        <div class="form-grid-2">
                            <div class="form-group">
                                <label class="form-label">Alergias conocidas (opcional)</label>
                                <input type="text" name="alergias" class="form-input" placeholder="Ej. Penicilina">
                            </div>
                            <div class="form-group">
                                <label class="form-label">Tipo de cobertura *</label>
                                <select name="cobertura" class="form-select">
                                    <option value="Particular / EPS" selected>EPS / seguro privado / particular</option>
                                    <option value="Particular">Particular</option>
                                    <option value="EPS Rimac">EPS Rímac</option>
                                    <option value="EPS Pacifico">EPS Pacífico</option>
                                    <option value="EsSalud">EsSalud / Seguro Privado</option>
                                </select>
                            </div>
                        </div>
                    </div>

                    <!-- BARRA DE ACCIONES INFERIOR -->
                    <div class="reg-footer-bar">
                        <a href="javascript:void(0)" onclick="toggleView('view-login')" class="reg-cancel-link">
                            <i data-lucide="chevron-left" style="width: 16px; height: 16px;"></i> Cancelar y volver
                        </a>
                        <div class="reg-footer-right">
                            <a href="javascript:void(0)" onclick="toggleView('view-login')" class="reg-login-link">
                                Ya tengo cuenta, iniciar sesión
                            </a>
                            <button type="submit" id="btnSubmitReg" class="btn-register-action">
                                <span>Registrarme</span>
                                <i data-lucide="check" style="width: 16px; height: 16px;"></i>
                            </button>
                        </div>
                    </div>
                </form>
            </section>

        </div>
    </main>

</div>

<script>
    lucide.createIcons();

    function togglePwdVisibility(inputId, btnEl) {
        const input = document.getElementById(inputId);
        if (input.type === 'password') {
            input.type = 'text';
            btnEl.innerHTML = '<i data-lucide="eye-off" style="width: 18px; height: 18px;"></i>';
        } else {
            input.type = 'password';
            btnEl.innerHTML = '<i data-lucide="eye" style="width: 18px; height: 18px;"></i>';
        }
        lucide.createIcons({root: btnEl});
    }

    function toggleView(targetId) {
        const loginView = document.getElementById('view-login');
        const registerView = document.getElementById('view-register');
        const container = document.getElementById('mainContainer');

        if (targetId === 'view-register') {
            container.classList.add('is-registering');
            loginView.classList.remove('active');
            registerView.classList.add('active');
            document.getElementById('formPane').scrollTop = 0;
        } else {
            container.classList.remove('is-registering');
            registerView.classList.remove('active');
            loginView.classList.add('active');
            document.getElementById('formPane').scrollTop = 0;
        }
        lucide.createIcons();
    }

    function selectChannel(pillEl) {
        document.querySelectorAll('.channel-pill').forEach(p => p.classList.remove('active'));
        pillEl.classList.add('active');
        const radio = pillEl.querySelector('input[type="radio"]');
        if (radio) radio.checked = true;
    }

    // Validación de coincidencia de contraseñas
    const pwd = document.getElementById('regPwd');
    const pwdConfirm = document.getElementById('regPwdConfirm');
    const pwdMatchMsg = document.getElementById('pwdMatchMsg');
    const dniInput = document.getElementById('regDni');
    const registerForm = document.getElementById('registerForm');
    const dniSubmitError = document.getElementById('dniSubmitError');
    const pwdMismatchError = document.getElementById('pwdMismatchError');

    function checkPasswordMatch() {
        if (!pwd || !pwdConfirm || !pwdMatchMsg) return;
        const val1 = pwd.value;
        const val2 = pwdConfirm.value;

        if (val1 && val2 && val1 === val2) {
            pwdMatchMsg.style.display = 'flex';
            pwdMatchMsg.classList.remove('mismatch');
            pwdMatchMsg.innerHTML = '<i data-lucide="check" style="width: 14px; height: 14px;"></i> Las contraseñas coinciden';
            lucide.createIcons({root: pwdMatchMsg});
        } else if (val1 && val2 && val1 !== val2) {
            pwdMatchMsg.style.display = 'flex';
            pwdMatchMsg.classList.add('mismatch');
            pwdMatchMsg.innerHTML = '<i data-lucide="alert-circle" style="width: 14px; height: 14px;"></i> Las contraseñas no coinciden';
            lucide.createIcons({root: pwdMatchMsg});
        } else {
            pwdMatchMsg.style.display = 'none';
        }
    }

    if (pwd) pwd.addEventListener('input', checkPasswordMatch);
    if (pwdConfirm) pwdConfirm.addEventListener('input', checkPasswordMatch);

    if (registerForm) {
        registerForm.addEventListener('submit', function(e) {
            const val = pwd.value;
            const val2 = pwdConfirm.value;
            const dniVal = dniInput.value;

            if (val !== val2) {
                e.preventDefault();
                pwdMismatchError.style.display = 'flex';
                pwdConfirm.focus();
                document.getElementById('formPane').scrollTo({ top: 0, behavior: 'smooth' });
                return;
            } else {
                pwdMismatchError.style.display = 'none';
            }

            if (dniVal.length > 0 && val.includes(dniVal)) {
                e.preventDefault();
                dniSubmitError.style.display = 'flex';
                pwd.focus();
                document.getElementById('formPane').scrollTo({ top: 0, behavior: 'smooth' });
                return;
            } else {
                dniSubmitError.style.display = 'none';
            }
        });
    }
</script>

</body>
</html>