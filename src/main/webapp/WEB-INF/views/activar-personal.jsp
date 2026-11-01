<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="referrer" content="no-referrer">
    <title>Activar cuenta de personal · Clínica Internacional</title>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/recuperacion.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/recuperacion-contenido.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/acceso-tema.css">
    <script defer src="${pageContext.request.contextPath}/assets/js/activar-personal.js"></script>
</head>
<body class="portal-personal">
<div class="seguridad-layout">
    <%@ include file="/WEB-INF/jspf/acceso-marca.jspf" %>
    <main class="formulario-panel">
        <div class="contenido-seguridad">
            <header><h1>Activa tu cuenta</h1><p class="subtitulo">Crea tu contraseña para ingresar al portal de personal.</p></header>
            <output id="activacionMensaje" aria-live="polite"><c:out value="${mensajeInvitacion}" /></output>
            <c:choose><c:when test="${invitacionVigente}">
                <form id="formActivacion" method="post" action="${pageContext.request.contextPath}/activar-personal">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="activar">
                    <input type="hidden" name="referencia" value="${referenciaInvitacion}">
                    <div class="campo"><label for="password">Nueva contraseña</label><input id="password" name="password" type="password" autocomplete="new-password" minlength="12" required></div>
                    <p class="ayuda">Usa 12 caracteres como mínimo, mayúscula, minúscula, número y símbolo. Máximo 72 bytes.</p>
                    <div class="campo"><label for="confirmacion">Confirmar contraseña</label><input id="confirmacion" name="confirmacion" type="password" autocomplete="new-password" minlength="12" required></div>
                    <button class="boton" type="submit">Activar mi cuenta</button>
                </form>
            </c:when><c:otherwise><p id="activacionSinVinculo">Abre el enlace que recibiste por correo. Si venció o ya se utilizó, solicita una nueva invitación al administrador.</p></c:otherwise></c:choose>
            <button type="button" id="reintentarInvitacion" class="boton" hidden>Volver a comprobar invitación</button>
            <a class="volver-enlace" href="${pageContext.request.contextPath}/login/personal">Ir a iniciar sesión</a>
        </div>
    </main>
</div>
</body></html>
