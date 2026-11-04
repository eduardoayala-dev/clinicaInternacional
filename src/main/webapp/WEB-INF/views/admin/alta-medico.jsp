<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List,java.util.Map,pe.edu.utp.clinica.util.Html" %>
<!DOCTYPE html><html lang="es"><head><%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
<meta name="viewport" content="width=device-width,initial-scale=1"><title>Completar perfil médico</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-administrativo.css"><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/padron-medicos.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admision-shell.css?v=20261104-pa"><link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:wght@600;700&display=swap" rel="stylesheet"><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-forms.css?v=20261104-1"></head><body class="portal-admision"><%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>
<main class="adm-main-wrapper"><header class="adm-topbar">Admisión / Médicos / Registrar</header><div class="adm-content-scroll"><h1 class="adm-page-title">Completar perfil médico</h1>
<p>Selecciona la cuenta creada por el administrador. Completa el CMP y la especialidad verificados; el perfil quedará asignado a tu sede.</p>
<% Object error=session.getAttribute("flashError");if(error!=null){ %><p class="padron-error" role="alert"><%= Html.texto(error) %></p><% session.removeAttribute("flashError");} %>
<form class="padron-edicion" method="post" action="${pageContext.request.contextPath}/admin/medicos/nuevo">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
<label for="cuentaMedica">Cuenta médica pendiente</label><select id="cuentaMedica" name="cuenta" required><option value="" disabled selected>Selecciona una cuenta</option>
<% for(pe.edu.utp.clinica.dao.PerfilProfesionalMedicoDAO.Cuenta cuenta:(List<pe.edu.utp.clinica.dao.PerfilProfesionalMedicoDAO.Cuenta>)request.getAttribute("cuentasMedicasPendientes")){ %>
<option value="<%= cuenta.id() %>"><%= Html.texto(cuenta.nombre()+" · "+cuenta.correo()+(cuenta.activa()?" · Activada":" · Invitación pendiente")) %></option><% } %></select>
<label for="altaCmp">CMP</label><input id="altaCmp" name="cmp" required pattern="[A-Za-z0-9-]{4,20}" maxlength="20">
<label for="altaEspecialidad">Especialidad</label><select id="altaEspecialidad" name="especialidad" required><option value="" disabled selected>Selecciona una especialidad</option>
<% for(Map<String,Object> especialidad:(List<Map<String,Object>>)request.getAttribute("especialidadesAlta")){ %><option value="<%= Html.texto(especialidad.get("id")) %>"><%= Html.texto(especialidad.get("nombre")) %></option><% } %>
</select><div class="padron-acciones"><a class="adm-btn-outline" href="${pageContext.request.contextPath}/admin/medicos">Cancelar</a><button class="adm-btn-primary" type="submit">Completar perfil profesional</button></div>
</form></div></main><script src="${pageContext.request.contextPath}/assets/js/padron-edicion.js"></script></body></html>
