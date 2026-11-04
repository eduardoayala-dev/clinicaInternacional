<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html><html lang="es"><head><%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
<meta name="viewport" content="width=device-width,initial-scale=1"><title>Perfil profesional pendiente</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-administrativo.css"><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/padron-medicos.css">
</head><body><main class="adm-content-scroll"><section class="padron-edicion"><h1>Tu acceso está activado</h1>
<p>Admisión debe completar y verificar tu CMP, especialidad y sede para habilitar tu portal médico.</p><p>Cuando el perfil esté listo podrás proponer tus horarios. Los pacientes verán los cupos después de su publicación.</p>
<div class="padron-acciones"><a class="adm-btn-outline" href="${pageContext.request.contextPath}/medico/inicio">Comprobar estado</a>
<form method="post" action="${pageContext.request.contextPath}/logout"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><button type="submit" class="adm-btn-primary">Cerrar sesión</button></form></div>
</section></main></body></html>
