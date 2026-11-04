<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="pe.edu.utp.clinica.dao.SeguimientoOfertasDAO,pe.edu.utp.clinica.model.Usuario,pe.edu.utp.clinica.util.Html,pe.edu.utp.clinica.util.FormatoClinico" %>
<%
    var panel=(SeguimientoOfertasDAO.Panel)request.getAttribute("panelOfertas");
    var filtro=panel.filtro();
    Usuario usuario=(Usuario)session.getAttribute("usuarioLogueado");
    String nombre=usuario.getNombres()+" "+usuario.getApellidos();
    String iniciales=nombre.substring(0,1);
    request.setAttribute("menuReasignaciones",Boolean.TRUE);
    String consulta="?estado="+filtro.estado()+"&especialidad="+filtro.especialidad()+"&fecha="+(filtro.fecha()==null?"":filtro.fecha());
%>
<!DOCTYPE html><html lang="es"><head>
<%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Seguimiento de reasignaciones | Clínica Internacional</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/fuentes-locales.css?v=20261104-1">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-administrativo.css?v=20261102-4">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/seguimiento-ofertas.css">
<script src="${pageContext.request.contextPath}/assets/vendor/lucide-1.68.0.min.js"></script>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admision-shell.css?v=20261104-pa"><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-forms.css?v=20261104-2"></head><body class="portal-admision">
<%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>
<div class="adm-main-wrapper"><header class="adm-topbar"><div class="adm-breadcrumb">Admisión / Reasignaciones</div></header>
<main class="adm-content-scroll ofertas-page">
<h1 class="adm-page-title">Seguimiento de reasignaciones</h1>
<p class="adm-page-subtitle">Ofertas de cupos liberados · <%= Html.texto(panel.sede()) %></p>
<form method="GET" action="${pageContext.request.contextPath}/admin/reasignaciones" class="ofertas-filtros">
<label>Estado<select name="estado"><option value="">Todos los estados</option>
<% for(String estado:SeguimientoOfertasDAO.ESTADOS) { %><option value="<%= estado %>" <%= estado.equals(filtro.estado())?"selected":"" %>><%= estado %></option><% } %>
</select></label>
<label>Especialidad<select name="especialidad"><option value="0">Todas las especialidades</option>
<% for(var especialidad:panel.especialidades()) { %><option value="<%= especialidad.id() %>" <%= especialidad.id()==filtro.especialidad()?"selected":"" %>><%= Html.texto(especialidad.nombre()) %></option><% } %>
</select></label>
<label>Fecha de oferta<input type="date" name="fecha" value="<%= filtro.fecha()==null?"":filtro.fecha() %>"></label>
<button type="submit" class="ofertas-principal">Actualizar</button>
<a class="ofertas-secundario" href="${pageContext.request.contextPath}/admin/reasignaciones">Limpiar filtros</a>
</form>
<section class="ofertas-indicadores" aria-label="Indicadores de las ofertas filtradas">
<article><span>Ofertas encontradas</span><strong><%= panel.total() %></strong></article>
<article><span>Pendientes registradas</span><strong><%= panel.estados().getOrDefault("PENDIENTE",0L) %></strong></article>
<article><span>Aceptadas</span><strong><%= panel.estados().getOrDefault("ACEPTADA",0L) %></strong></article>
</section>
<section class="ofertas-panel" aria-labelledby="ofertas-titulo">
<div class="ofertas-titulo"><h2 id="ofertas-titulo">Historial de ofertas</h2><span>Consultado <%= Html.texto(FormatoClinico.fechaHora(panel.consultadoEn())) %></span></div>
<% if(panel.ofertas().isEmpty()) { %>
<div class="ofertas-vacio"><i data-lucide="inbox"></i><strong>No hay ofertas con estos filtros</strong><p>Las ofertas aparecerán cuando se libere un cupo y exista un paciente compatible en lista de espera.</p></div>
<% } else { %>
<div class="ofertas-tabla"><table><thead><tr><th>Oferta / cupo</th><th>Médico y especialidad</th><th>Paciente destinatario</th><th>Plazo de respuesta</th><th>Resultado</th></tr></thead><tbody>
<% for(var oferta:panel.ofertas()) { %><tr>
<td><strong>OFE-<%= oferta.id() %> · CIT-<%= oferta.citaOriginal() %></strong><small><%= Html.texto(FormatoClinico.fechaHora(oferta.cita())) %></small><small>Ofrecida <%= Html.texto(FormatoClinico.fechaHora(oferta.ofrecida())) %></small></td>
<td><strong><%= Html.texto(oferta.medico()) %></strong><small><%= Html.texto(oferta.especialidad()) %></small></td>
<td><strong><%= Html.texto(oferta.paciente()) %></strong><small>Lista de espera #<%= oferta.lista() %></small></td>
<td><%= Html.texto(FormatoClinico.fechaHora(oferta.limite())) %><% if(oferta.plazoVencido()) { %><small class="ofertas-aviso">Plazo vencido · pendiente de procesamiento</small><% } %></td>
<td><span class="oferta-estado"><%= Html.texto(oferta.estado()) %></span><% if(oferta.citaAsignada()!=null) { %><small>Cita asignada CIT-<%= oferta.citaAsignada() %></small><% } %></td>
</tr><% } %>
</tbody></table></div>
<% } %>
<nav class="ofertas-paginacion" aria-label="Páginas del historial">
<% if(panel.pagina()>1) { %><a class="ofertas-secundario" href="${pageContext.request.contextPath}/admin/reasignaciones<%= Html.texto(consulta) %>&amp;pagina=<%= panel.pagina()-1 %>">‹ Anterior</a><% } else { %><button class="ofertas-secundario" disabled>‹ Anterior</button><% } %>
<span>Página <%= panel.pagina() %> de <%= panel.paginas() %></span>
<% if(panel.pagina()<panel.paginas()) { %><a class="ofertas-secundario" href="${pageContext.request.contextPath}/admin/reasignaciones<%= Html.texto(consulta) %>&amp;pagina=<%= panel.pagina()+1 %>">Siguiente ›</a><% } else { %><button class="ofertas-secundario" disabled>Siguiente ›</button><% } %>
</nav></section>
<p class="ofertas-nota">La consulta no genera ofertas ni cambia reservas. El orden de selección respeta compatibilidad, prioridad clínica y antigüedad. La aceptación de una oferta es la que crea la nueva cita.</p>
</main></div><script>if(window.lucide)lucide.createIcons();</script></body></html>
