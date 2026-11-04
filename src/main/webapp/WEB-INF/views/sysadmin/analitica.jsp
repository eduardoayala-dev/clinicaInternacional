<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="pe.edu.utp.clinica.dao.PanelSistemaDAO,pe.edu.utp.clinica.util.Html" %>
<% var ps=(PanelSistemaDAO.Panel)request.getAttribute("panelSistema");var datos=ps.datos(); %>
<!DOCTYPE html><html lang="es"><head><%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
<meta name="viewport" content="width=device-width,initial-scale=1"><title>Indicadores y analítica</title>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:wght@600;700&display=swap" rel="stylesheet">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-administrativo.css?v=20261104-admin2">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/sysadmin-shell.css?v=20261104-admin2">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/panel-sistema.css?v=20261104-admin2">
<script defer src="${pageContext.request.contextPath}/assets/js/panel-sistema.js"></script>
</head><body class="sysadmin-portal"><%@ include file="/WEB-INF/jspf/sysadmin-sidebar.jspf" %>
<main class="adm-main-wrapper"><header class="adm-topbar"><span class="adm-breadcrumb">Administrador / Indicadores y analítica</span></header><div class="adm-content-scroll">
<h1 class="adm-page-title">Indicadores y analítica global</h1><p class="adm-page-subtitle">Actividad asistencial registrada, agrupada por sede y período.</p>
<%@ include file="/WEB-INF/jspf/panel-sistema-filtros.jspf" %>
<% if(datos.citas()==0){ %><p class="sistema-vacio" role="status">No hay citas registradas para los filtros seleccionados.</p><% } %>
<div class="sistema-kpis sistema-kpis-cinco">
<section class="sistema-kpi"><span>Citas registradas</span><strong><%= datos.citas() %></strong><small>Incluye todos los estados del período.</small></section>
<section class="sistema-kpi"><span>Atendidas</span><strong><%= datos.atendidas() %></strong><small>Estado ATENDIDA.</small></section>
<section class="sistema-kpi"><span>Canceladas</span><strong><%= datos.canceladas() %></strong><small>Estado CANCELADA.</small></section>
<section class="sistema-kpi"><span>Citas reasignadas</span><strong><%= datos.reasignadas() %></strong><small>Con oferta aceptada vinculada.</small></section>
<section class="sistema-kpi"><span>Inasistencias documentadas</span><strong><%= datos.inasistencias() %></strong><small>Registro expreso de admisión.</small></section>
</div>
<%@ include file="/WEB-INF/jspf/panel-sistema-sedes.jspf" %>
<div class="sistema-dos-columnas">
<section class="sistema-panel"><h2>Evolución por semanas del período</h2><p class="sistema-nota">Bloques de siete días a partir de la fecha inicial. Las barras representan citas registradas.</p><div class="sistema-tabla"><table><thead><tr><th>Período</th><th>Citas</th><th>Atendidas</th><th>Volumen</th></tr></thead><tbody>
<% long maximo=ps.semanas().stream().mapToLong(PanelSistemaDAO.Semana::citas).max().orElse(0);for(var semana:ps.semanas()){ %><tr><td><%= semana.desde() %> — <%= semana.hasta() %></td><td><%= semana.citas() %></td><td><%= semana.atendidas() %></td><td><progress max="<%= Math.max(1,maximo) %>" value="<%= semana.citas() %>" aria-label="<%= semana.citas() %> citas"></progress></td></tr><% } %>
</tbody></table></div></section>
<section class="sistema-panel"><h2>Riesgo de cancelación registrado</h2><p class="sistema-nota">Clasificación almacenada en las citas del período. No representa una nueva evaluación ni una medida de precisión del modelo.</p><div class="sistema-tabla"><table><thead><tr><th>Clasificación</th><th>Citas</th><th>Porcentaje</th></tr></thead><tbody>
<% String[] niveles={"Bajo","Medio","Alto","Sin clasificación"};long[] cantidades={datos.bajo(),datos.medio(),datos.alto(),datos.sinRiesgo()};for(int n=0;n<niveles.length;n++){ %><tr><td><%= niveles[n] %></td><td><%= cantidades[n] %></td><td><%= datos.citas()==0?"Sin datos":String.format(java.util.Locale.ROOT,"%.1f %%",cantidades[n]*100.0/datos.citas()) %></td></tr><% } %>
</tbody></table></div></section></div>
</div></main></body></html>