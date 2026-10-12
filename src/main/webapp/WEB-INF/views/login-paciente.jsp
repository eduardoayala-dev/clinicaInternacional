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
    <title>Iniciar Sesión - Clínica Internacional</title>
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

            <!-- VISTA 2: CREAR CUENTA NUEVA -->
            <section id="view-register" class="auth-view ${showRegister ? 'active' : ''}">
                <div class="auth-header" style="margin-bottom: 20px;">
                    <h2 class="auth-title" style="font-size: 28px;">Crear cuenta de Paciente</h2>
                    <p class="auth-subtitle">Completa tus datos para agendar citas y gestionar tu historial médico.</p>
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

                <form id="registerForm" action="${pageContext.request.contextPath}/registro" method="POST">
                    
                    <div class="section-badge">01. Datos Personales</div>
                    <div class="form-grid-3">
                        <div class="form-group">
                            <label class="form-label">Nombres *</label>
                            <input type="text" name="nombres" class="form-input" required placeholder="Ej. Juan Carlos">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Apellidos *</label>
                            <input type="text" name="apellidos" class="form-input" required placeholder="Ej. Pérez García">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Fecha de Nacimiento *</label>
                            <input type="date" name="fechaNacimiento" class="form-input" required>
                        </div>
                    </div>

                    <div class="form-grid-3">
                        <div class="form-group">
                            <label class="form-label">Tipo de Documento *</label>
                            <select name="tipoDocumento" id="regTipoDoc" class="form-select" required>
                                <option value="DNI">DNI</option>
                                <option value="CE">Carnet de Extranjería</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Nº de Documento *</label>
                            <input type="text" name="dni" id="regDni" class="form-input" required placeholder="Ej. 72345678" pattern="[A-Za-z0-9]+">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Sexo / Género *</label>
                            <select name="genero" class="form-select" required>
                                <option value="">[ Seleccionar ]</option>
                                <option value="M">Masculino</option>
                                <option value="F">Femenino</option>
                                <option value="O">Otro</option>
                            </select>
                        </div>
                    </div>

                    <div class="section-badge">02. Datos de Contacto</div>
                    <div class="form-grid-3">
                        <div class="form-group">
                            <label class="form-label">Teléfono / Celular *</label>
                            <input type="text" name="telefono" class="form-input" required placeholder="Ej. 987 654 321">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Correo Electrónico *</label>
                            <input type="email" name="email" class="form-input" required placeholder="correo@ejemplo.com">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Dirección *</label>
                            <input type="text" name="direccion" class="form-input" required placeholder="Ej. Av. Javier Prado 123">
                        </div>
                    </div>

                    <div class="section-badge">03. Contraseña y Seguridad</div>
                    <div class="form-grid-2">
                        <div class="form-group">
                            <label class="form-label">Crear Contraseña *</label>
                            <div class="input-box has-right-icon">
                                <input type="password" name="password" id="regPwd" class="form-input" required placeholder="Crea tu contraseña">
                                <button type="button" class="btn-toggle-eye" onclick="togglePwdVisibility('regPwd', this)" title="Mostrar/ocultar contraseña">
                                    <i data-lucide="eye" style="width: 18px; height: 18px;"></i>
                                </button>
                            </div>
                            <p style="font-size: 12px; color: #64748b; margin-top: 6px;">Usa una clave segura y fácil de recordar.</p>
                        </div>

                        <div class="form-group">
                            <div class="pwd-requirements-box">
                                <div class="req-header">Requisitos de contraseña:</div>
                                <div class="req-item" id="chk-len"><i data-lucide="circle" style="width:13px;height:13px;"></i> Mínimo 8 caracteres</div>
                                <div class="req-item" id="chk-upper"><i data-lucide="circle" style="width:13px;height:13px;"></i> Al menos 1 mayúscula</div>
                                <div class="req-item" id="chk-lower"><i data-lucide="circle" style="width:13px;height:13px;"></i> Al menos 1 minúscula</div>
                                <div class="req-item" id="chk-num"><i data-lucide="circle" style="width:13px;height:13px;"></i> Al menos 1 número</div>
                                <div class="req-item" id="chk-sym"><i data-lucide="circle" style="width:13px;height:13px;"></i> Al menos 1 símbolo (@, #, $)</div>
                            </div>
                        </div>
                    </div>

                    <div class="terms-wrapper">
                        <input type="checkbox" id="terminos" name="terminos" required>
                        <label for="terminos">Comprendo y acepto los términos del servicio y las políticas de privacidad de la Clínica Internacional.</label>
                    </div>

                    <div class="reg-actions-row">
                        <button type="submit" id="btnSubmitReg" class="btn-submit-primary" style="flex: 1;" disabled>
                            Confirmar y Registrarme
                        </button>
                        <button type="button" class="btn-back-to-login" onclick="toggleView('view-login')">
                            <i data-lucide="arrow-left" style="width: 16px; height: 16px;"></i>
                            Volver a iniciar sesión
                        </button>
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
        const icon = btnEl.querySelector('[data-lucide]') || btnEl.querySelector('svg');
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
    }

    // Validación de contraseña en tiempo real
    const pwd = document.getElementById('regPwd');
    const dniInput = document.getElementById('regDni');
    const btnSubmit = document.getElementById('btnSubmitReg');
    const registerForm = document.getElementById('registerForm');
    const dniSubmitError = document.getElementById('dniSubmitError');

    function checkIcon(elementId, isValid) {
        const el = document.getElementById(elementId);
        if (!el) return;
        if (isValid) {
            el.classList.add('valid');
            el.innerHTML = '<i data-lucide="check-circle" style="width:13px;height:13px;"></i> ' + el.innerText.trim();
        } else {
            el.classList.remove('valid');
            el.innerHTML = '<i data-lucide="circle" style="width:13px;height:13px;"></i> ' + el.innerText.trim();
        }
        lucide.createIcons({root: el});
    }

    if (pwd) {
        pwd.addEventListener('input', function() {
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

            btnSubmit.disabled = !(isLen && isUpper && isLower && isNum && isSym);
        });
    }

    if (registerForm) {
        registerForm.addEventListener('submit', function(e) {
            const val = pwd.value;
            const dniVal = dniInput.value;

            if (dniVal.length > 0 && val.includes(dniVal)) {
                e.preventDefault();
                dniSubmitError.style.display = 'flex';
                pwd.focus();
                document.getElementById('formPane').scrollTo({ top: 0, behavior: 'smooth' });
            } else {
                dniSubmitError.style.display = 'none';
            }
        });
    }
</script>

</body>
</html>