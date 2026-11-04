<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="pe.edu.utp.clinica.ia.SeguimientoIa,pe.edu.utp.clinica.model.Usuario,pe.edu.utp.clinica.util.Html,pe.edu.utp.clinica.util.FormatoClinico" %>
<%
    var evaluacion=(SeguimientoIa)request.getAttribute("evaluacionIa");
    var prediccion=evaluacion.prediccion();
    var datos=evaluacion.instantanea();
    Usuario usuario=(Usuario)session.getAttribute("usuarioLogueado");
    String nombre=usuario.getNombres()+" "+usuario.getApellidos();
    String iniciales=nombre.substring(0,1);
%>
<!DOCTYPE html><html lang="es"><head>
<%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Seguimiento con IA | Clínica Internacional</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/fuentes-locales.css?v=20261104-1">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-administrativo.css?v=20261102-4">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/seguimiento-ia.css?v=20261102-4">
<script src="${pageContext.request.contextPath}/assets/vendor/lucide-1.68.0.min.js"></script>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admision-shell.css?v=20261104-pa"><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-forms.css?v=20261104-2"></head><body class="portal-admision">
<%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>
<div class="adm-main-wrapper"><header class="adm-topbar"><div class="adm-breadcrumb">Admisión / Seguimiento con IA</div></header>
<main class="adm-content-scroll ia-seguimiento"><div class="ia-seguimiento-inner">
<a class="ia-volver" href="${pageContext.request.contextPath}/admin/seguimiento-confirmaciones">‹ Volver a confirmaciones y seguimiento</a>
<div class="ia-cabecera"><div><h1 class="adm-page-title">Seguimiento de cita CIT-<%= datos.cita() %></h1>
<p class="adm-page-subtitle">Prioriza el seguimiento y coordina la asistencia con el paciente.</p></div><span class="ia-distintivo">Apoyo de IA</span></div>
<section class="ia-panel" aria-labelledby="ia-resultado">
<div class="ia-resumen"><div class="ia-probabilidad"><span>Probabilidad estimada de inasistencia</span><strong><%= String.format(java.util.Locale.ROOT,"%.1f",prediccion.probabilidad()*100) %> %</strong><span>Estimación de apoyo al seguimiento</span></div><div><h2 id="ia-resultado">Seguimiento de asistencia</h2><div class="ia-niveles"><span>Nivel estimado: <strong><%= Html.texto(prediccion.nivel()) %></strong></span><span>Seguimiento: <strong><%= Html.texto(evaluacion.nivelSeguimiento()) %></strong></span></div><p><%= datos.plazoVencido()?"El plazo venció sin confirmación. La regla de seguimiento eleva el nivel de atención administrativa.":"Revisa la confirmación y los contactos antes de coordinar con el paciente." %></p>
<p><%= Html.texto(prediccion.accionSugerida()) %></p>
</div></div><p class="ia-nota">Esta evaluación no cancela, reasigna ni cambia la prioridad clínica de la cita.</p>
</section>
<section class="ia-panel" aria-labelledby="ia-factores"><h2 id="ia-factores">Factores de la estimación</h2>
<div class="ia-tabla"><table><thead><tr><th>Factor</th><th>Valor</th><th>Influencia estimada</th></tr></thead><tbody>
<% for(var factor:prediccion.factores()) { %>
<tr><td><%= Html.texto("confirmada_por_paciente".equals(factor.variable())?"Confirmación expresa del paciente":factor.descripcion()) %></td><td><%= "confirmada_por_paciente".equals(factor.variable()) ? (factor.valor()==1?"Sí":"No") : String.format(java.util.Locale.ROOT,"%.0f",factor.valor()) %></td><td><%= Html.texto(factor.influencia()) %></td></tr>
<% } %>
</tbody></table></div>
<p>El historial incluye únicamente atenciones e inasistencias documentadas antes de la reserva, <%= Html.texto(FormatoClinico.fechaHora(datos.reservadaEn())) %>. La confirmación expresa corresponde al momento de esta evaluación.</p>
<p>Evaluada: <%= Html.texto(FormatoClinico.fechaHora(datos.evaluadaEn())) %> · Versión: <%= Html.texto(prediccion.version()) %></p>
</section>
<details class="ia-detalles"><summary>Información del modelo y alcance</summary><p>Versión: <%= Html.texto(prediccion.version()) %>. Modelo experimental entrenado con datos sintéticos para simular el flujo de seguimiento. Su precisión en pacientes reales está pendiente de evaluación.</p><ul><% for(String aviso:prediccion.advertencias()) { if(aviso.startsWith("Modelo entrenado")) continue; %><li><%= Html.texto(aviso) %></li><% } %></ul><p>Los intentos de contacto se registran en Confirmaciones y seguimiento. La estimación no sustituye la decisión del personal.</p></details>
</div></main></div><script>if(window.lucide)lucide.createIcons();</script></body></html>