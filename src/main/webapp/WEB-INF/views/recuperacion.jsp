<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Seguridad de cuenta · Clínica Internacional</title>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/fuentes-locales.css?v=20261104-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/recuperacion.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/recuperacion-contenido.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/acceso-tema.css">
    <script defer src="${pageContext.request.contextPath}/assets/js/recuperacion.js"></script>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-forms.css?v=20261104-2"></head>
<body class="${portalPersonal ? 'portal-personal' : 'portal-paciente'}">
<div class="seguridad-layout">
    <%@ include file="/WEB-INF/jspf/acceso-marca.jspf" %>
    <main class="formulario-panel">
        <jsp:include page="/WEB-INF/views/recuperacion-contenido.jsp" />
    </main>
</div>
</body>
</html>
