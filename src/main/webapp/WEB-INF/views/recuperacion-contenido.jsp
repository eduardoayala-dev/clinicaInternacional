<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <div class="contenido-seguridad" data-vista-recuperacion="${vistaVerificacion}">
            <c:if test="${portalPersonal}"><div class="acceso-alterno"><a href="${pageContext.request.contextPath}/login/paciente">Ir al Portal de Pacientes</a></div></c:if>
            <header>
                <h1><c:choose><c:when test="${vistaVerificacion == 'codigo'}">Verifica tu correo</c:when><c:when test="${vistaVerificacion == 'password'}">Nueva contraseña</c:when><c:when test="${vistaVerificacion == 'nuevo-correo'}">Nuevo correo</c:when><c:when test="${vistaVerificacion == 'cuenta'}">Seguridad de cuenta</c:when><c:when test="${vistaVerificacion == 'completado'}">Cambio completado</c:when><c:otherwise>Recuperar contraseña</c:otherwise></c:choose></h1>
                <p class="subtitulo"><c:choose><c:when test="${vistaVerificacion == 'solicitud'}">${portalPersonal ? 'Ingresa tu correo registrado. Te enviaremos un código para crear una nueva contraseña.' : 'Ingresa tu documento de identidad. Te enviaremos un código al correo registrado en tu cuenta.'}</c:when><c:when test="${vistaVerificacion == 'codigo'}">${codigoNuevo ? 'Ingresa el código enviado a tu nueva dirección. Tu correo actual se conserva hasta completar este paso.' : 'Si los datos corresponden a una cuenta activa y no superaste el límite, recibirás un código. Revisa también spam.'}</c:when><c:when test="${vistaVerificacion == 'password'}">Ya verificaste tu correo. Elige y confirma la nueva contraseña.</c:when><c:when test="${vistaVerificacion == 'nuevo-correo'}">Ya verificaste tu correo actual. Ahora comprobaremos que tienes acceso a la nueva dirección.</c:when><c:when test="${vistaVerificacion == 'cuenta'}">Confirma tu contraseña actual para recibir un código de verificación.</c:when></c:choose></p>
            </header>
            <c:if test="${not empty mensajeRecuperacion}"><output class="aviso mensaje"><c:out value="${mensajeRecuperacion}"/></output></c:if>
            <c:choose>
                <c:when test="${not recuperacionDisponible}"><p class="aviso">La verificación no está disponible temporalmente. Inténtalo más tarde.</p></c:when>
                <c:when test="${vistaVerificacion == 'completado'}"><a class="boton boton-enlace" href="${pageContext.request.contextPath}${portalPersonal ? '/login/personal' : '/login/paciente'}">Continuar</a></c:when>
                <c:when test="${vistaVerificacion == 'codigo'}">
                    <form method="post" action="${pageContext.request.contextPath}/verificar-codigo">
                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                        <label for="codigoVerificacion">Código de verificación *</label>
                        <input id="codigoVerificacion" name="codigo" class="entrada-codigo" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" minlength="6" maxlength="6" placeholder="000000" required aria-describedby="ayudaCodigo">
                        <button class="boton" type="submit">Verificar código</button>
                    </form>
                    <p class="aviso" id="ayudaCodigo"><img src="${pageContext.request.contextPath}/assets/images/seguridad/correo.svg" alt=""><span>El código vence en 10 minutos y admite hasta 5 intentos. Usa el último código recibido en esta misma ventana.</span></p>
                    <a class="volver" href="${pageContext.request.contextPath}${cambioAutenticado ? '/cuenta/seguridad' : portalPersonal ? '/recuperar?portal=personal' : '/recuperar'}">Iniciar otra solicitud</a>
                </c:when>
                <c:when test="${vistaVerificacion == 'password'}">
                    <form method="post" action="${pageContext.request.contextPath}/restablecer" id="restablecerForm">
                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                        <label for="nuevaPassword">Nueva contraseña *</label>
                        <input type="password" id="nuevaPassword" name="password" autocomplete="new-password" minlength="12" maxlength="72" required aria-describedby="requisitos">
                        <label for="confirmarPassword">Confirmar contraseña *</label>
                        <input type="password" id="confirmarPassword" name="confirmacion" autocomplete="new-password" minlength="12" maxlength="72" required>
                        <p id="requisitos" class="ayuda">Al menos 12 caracteres, mayúscula, minúscula, número y símbolo. No incluyas tu DNI. Máximo 72 bytes UTF-8.</p>
                        <button class="boton" type="submit">Guardar nueva contraseña</button>
                    </form>
                </c:when>
                <c:when test="${vistaVerificacion == 'nuevo-correo'}">
                    <form method="post" action="${pageContext.request.contextPath}/cuenta/nuevo-correo">
                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                        <label for="correoNuevo">Nueva dirección de correo *</label>
                        <input type="email" id="correoNuevo" name="email" autocomplete="email" maxlength="150" required>
                        <button class="boton" type="submit">Verificar nuevo correo</button>
                    </form>
                </c:when>
                <c:when test="${vistaVerificacion == 'cuenta'}">
                    <form method="post" action="${pageContext.request.contextPath}/cuenta/seguridad">
                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                        <label for="operacionCuenta">¿Qué deseas cambiar?</label>
                        <select id="operacionCuenta" name="operacion"><option value="password">Mi contraseña</option><option value="correo">Mi correo electrónico</option></select>
                        <label for="passwordActual">Contraseña actual *</label>
                        <input type="password" id="passwordActual" name="actual" autocomplete="current-password" maxlength="72" required>
                        <button class="boton" type="submit">Enviar código a mi correo actual</button>
                    </form>
                    <p class="aviso">El cambio de correo requiere verificar la dirección actual y la nueva. Las otras sesiones se cerrarán al guardar el cambio.</p>
                </c:when>
                <c:otherwise>
                    <form method="post" action="${pageContext.request.contextPath}/recuperar">
                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="portal" value="${portalPersonal ? 'personal' : 'paciente'}">
                        <label for="identificadorRecuperacion">${portalPersonal ? 'Correo registrado *' : 'Documento de identidad (DNI / CE) *'}</label>
                        <input id="identificadorRecuperacion" name="identificador" type="${portalPersonal ? 'email' : 'text'}" maxlength="${portalPersonal ? '254' : '15'}" autocomplete="username" placeholder="${portalPersonal ? 'correo@ejemplo.com' : 'Ej. 45879632'}" required>
                        <button class="boton" type="submit">Enviar código de verificación</button>
                    </form>
                    <p class="aviso"><c:if test="${not portalPersonal}"><img src="${pageContext.request.contextPath}/assets/images/seguridad/correo.svg" alt=""></c:if><span>${portalPersonal ? 'Si el correo pertenece a una cuenta del personal, recibirás un código que vence en 10 minutos y sirve una sola vez.' : 'Si el documento está registrado, recibirás un código en tu correo. Vence en 10 minutos y sirve una sola vez.'}</span></p>
                </c:otherwise>
            </c:choose>
            <a class="volver" href="${pageContext.request.contextPath}${portalPersonal ? '/login/personal' : '/login/paciente'}"><c:if test="${not portalPersonal}"><img src="${pageContext.request.contextPath}/assets/images/seguridad/volver.svg" alt=""></c:if>${portalPersonal ? '← Volver al acceso del personal' : 'Volver a iniciar sesión'}</a>
            <c:if test="${cambioAutenticado or vistaVerificacion == 'cuenta'}"><a class="volver" href="${pageContext.request.contextPath}${portalDestino}">Volver a mi portal</a></c:if>
        </div>
