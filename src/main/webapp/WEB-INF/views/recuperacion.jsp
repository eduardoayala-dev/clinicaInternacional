<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Seguridad de cuenta · Clínica Internacional</title>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Serif+Display:ital@0;1&amp;family=Inter:wght@400;500;600;700;800&amp;display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/recuperacion.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/recuperacion-contenido.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/acceso-tema.css">
    <script defer src="${pageContext.request.contextPath}/assets/js/recuperacion.js"></script>
</head>
<body class="${portalPersonal ? 'portal-personal' : 'portal-paciente'}">
<div class="seguridad-layout">
    <%@ include file="/WEB-INF/jspf/acceso-marca.jspf" %>
    <main class="formulario-panel">
        <jsp:include page="/WEB-INF/views/recuperacion-contenido.jsp" />
    </main>
</div>
</body>
</html>
