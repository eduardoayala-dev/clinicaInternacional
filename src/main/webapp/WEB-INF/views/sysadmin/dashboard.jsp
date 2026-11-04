<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="pe.edu.utp.clinica.dao.PanelSistemaDAO,pe.edu.utp.clinica.util.Html" %>
<% var ps=(PanelSistemaDAO.Panel)request.getAttribute("panelSistema");var datos=ps.datos();var totales=ps.totales(); %>
<!DOCTYPE html><html lang="es"><head><%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
<meta name="viewport" content="width=device-width,initial-scale=1"><title>Inicio del administrador</title>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:wght@600;700&display=swap" rel="stylesheet">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-administrativo.css?v=20261104-admin2">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/sysadmin-shell.css?v=20261104-admin2">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/panel-sistema.css?v=20261104-admin2">
<script defer src="${pageContext.request.contextPath}/assets/js/panel-sistema.js"></script>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-forms.css?v=20261104-2"></head><body class="sysadmin-portal"><%@ include file="/WEB-INF/jspf/sysadmin-sidebar.jspf" %>
<main class="adm-main-wrapper"><header class="adm-topbar"><span class="adm-breadcrumb">Administrador / Inicio</span></header><div class="adm-content-scroll">
<h1 class="adm-page-title">Vista general</h1><p class="adm-page-subtitle">Usuarios, actividad asistencial y eventos registrados del sistema.</p>
<div class="sistema-kpis">
<section class="sistema-kpi"><span>Usuarios registrados</span><strong><%= totales.usuarios() %></strong><small><%= totales.pacientes() %> pacientes · <%= totales.medicos() %> médicos · <%= totales.personal() %> personal · <%= totales.administradores() %> administradores</small></section>
<section class="sistema-kpi"><span>Sedes registradas</span><strong><%= totales.sedes() %></strong><small>Catálogo completo, incluidos estados inactivos.</small></section>
<section class="sistema-kpi"><span>Citas del período</span><strong><%= datos.citas() %></strong><small>Según los filtros seleccionados.</small></section>
<section class="sistema-kpi"><span>Ofertas vigentes pendientes</span><strong><%= datos.pendientes() %></strong><small>Plazo de respuesta vigente y cita original dentro del período.</small></section>
</div>
<%@ include file="/WEB-INF/jspf/panel-sistema-filtros.jspf" %>
<%@ include file="/WEB-INF/jspf/panel-sistema-sedes.jspf" %>
<section class="sistema-panel"><h2>Auditoría reciente del sistema</h2><p class="sistema-nota">Últimos ocho eventos registrados, de todas las sedes. Independiente de los filtros de citas.</p>
<% if(ps.auditoria().isEmpty()){ %><p class="sistema-vacio">No hay eventos de auditoría registrados.</p><% }else{ %><ul class="sistema-auditoria"><% for(var evento:ps.auditoria()){ %><li><strong><%= Html.texto(evento.actor()) %> · <%= Html.texto(evento.evento()) %></strong><span><%= Html.texto(evento.descripcion()) %></span><time><%= evento.fecha()==null?"Fecha no registrada":evento.fecha().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) %></time></li><% } %></ul><% } %></section>
</div></main></body></html>