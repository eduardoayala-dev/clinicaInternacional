<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="pe.edu.utp.clinica.dao.GestionMotivosDAO,pe.edu.utp.clinica.model.Usuario,pe.edu.utp.clinica.util.Html,java.net.URLEncoder,java.nio.charset.StandardCharsets" %>
<%
    var panel=(GestionMotivosDAO.Panel)request.getAttribute("panelMotivos");var filtro=panel.filtro();
    Usuario usuario=(Usuario)session.getAttribute("usuarioLogueado");
    String nombrePersonal=usuario.getNombres()+" "+usuario.getApellidos();String inicialesPersonal=nombrePersonal.isBlank()?"A":nombrePersonal.substring(0,1);
    String ruta=request.getContextPath()+(panel.administrador()?"/sysadmin/motivos":"/admin/motivos");
    String consulta="?especialidad="+filtro.especialidad()+"&estado="+filtro.estado()+"&buscar="+URLEncoder.encode(filtro.busqueda(),StandardCharsets.UTF_8);
    String mensaje=(String)session.getAttribute("mensajeMotivos");session.removeAttribute("mensajeMotivos");
%>
<!DOCTYPE html>
<html lang="es">
<head>
<%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Motivos de consulta | Clínica Internacional</title>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:wght@600;700&display=swap" rel="stylesheet">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-administrativo.css?v=20261103-1">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/catalogo-motivos.css?v=20261103-1">
</head>
<body>
<% if(!panel.administrador()){ %>
<%@ include file="/WEB-INF/jspf/admin-sidebar-medicos.jspf" %>
<% }else{ %>
<aside class="adm-sidebar" aria-label="Navegación del administrador">
<a class="adm-brand" href="${pageContext.request.contextPath}/sysadmin/dashboard">
<div class="adm-brand-logo-wrap">
<img class="adm-brand-logo-img" alt="" src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png">
</div>
<div>
<div class="adm-brand-name">Clínica Internacional</div>
<div class="adm-brand-sub">Administrador del sistema</div>
</div>
</a>
<nav class="adm-nav">
<a class="adm-nav-item" href="${pageContext.request.contextPath}/sysadmin/dashboard">
<img class="adm-nav-icon" alt="" src="${pageContext.request.contextPath}/assets/images/medico/inicio.svg">
<span>Inicio</span>
</a>
<a class="adm-nav-item" href="${pageContext.request.contextPath}/sysadmin/usuarios">
<img class="adm-nav-icon" alt="" src="${pageContext.request.contextPath}/assets/images/medico/pacientes.svg">
<span>Usuarios y roles</span>
</a>
<a class="adm-nav-item" href="${pageContext.request.contextPath}/sysadmin/sedes">
<img class="adm-nav-icon" alt="" src="${pageContext.request.contextPath}/assets/images/medico/sede.svg">
<span>Sedes</span>
</a>
<a class="adm-nav-item active" aria-current="page" href="${pageContext.request.contextPath}/sysadmin/motivos">
<img class="adm-nav-icon" alt="" src="${pageContext.request.contextPath}/assets/images/medico/nota.svg">
<span>Motivos de consulta</span>
</a>
<a class="adm-nav-item" href="${pageContext.request.contextPath}/sysadmin/priorizacion">
<img class="adm-nav-icon" alt="" src="${pageContext.request.contextPath}/assets/images/medico/guardar.svg">
<span>Configuración</span>
</a>
</nav>
<div class="adm-user-footer">
<div class="adm-user-text">
<div class="adm-user-name">
<%= Html.texto(nombrePersonal) %>
</div>
<div class="adm-user-role">Administrador</div>
</div>
<form method="post" action="${pageContext.request.contextPath}/logout">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
<button class="adm-logout-btn" aria-label="Cerrar sesión">
<img src="${pageContext.request.contextPath}/assets/images/medico/salir.svg" width="18" alt="">
</button>
</form>
</div>
</aside>
<% } %>
<div class="adm-main-wrapper">
<header class="adm-topbar">
<div class="adm-breadcrumb">
<%= panel.administrador()?"Administración":"Admisión" %> / Motivos de consulta</div>
</header>
<main class="adm-content-scroll motivos-page">
<h1 class="adm-page-title">Motivos de consulta</h1>
<p class="adm-page-subtitle">Catálogo compartido por especialidad. Desactivar un motivo conserva las citas registradas.</p>
<% if(mensaje!=null){ %>
<output class="motivos-exito">
<%= Html.texto(mensaje) %>
</output>
<% } %>
<% if(!panel.administrador()){ %>
<div class="adm-tabs-row">
<a class="adm-tab-item" href="${pageContext.request.contextPath}/admin/medicos">Médicos</a>
<a class="adm-tab-item" href="${pageContext.request.contextPath}/admin/horarios">Propuestas de horarios</a>
<a class="adm-tab-item active" aria-current="page" href="<%= Html.texto(ruta) %>">Motivos de consulta</a>
</div>
<p class="motivos-nota">Gestiona los motivos disponibles para las reservas y conserva el historial de las citas.</p>
<% } %>
<form class="motivos-filtros" method="get" action="<%= Html.texto(ruta) %>">
<label>Especialidad<select name="especialidad">
<option value="0">Todas</option>
<% for(var e:panel.especialidades()){ %>
<option value="<%= e.id() %>" <%= e.id()==filtro.especialidad()?"selected":"" %>>
<%= Html.texto(e.nombre()) %>
</option>
<% } %>
</select>
</label>
<label>Estado<select name="estado">
<option value="">Todos</option>
<option value="ACTIVO" <%= "ACTIVO".equals(filtro.estado())?"selected":"" %>>Activo</option>
<option value="INACTIVO" <%= "INACTIVO".equals(filtro.estado())?"selected":"" %>>Inactivo</option>
</select>
</label>
<label>Buscar motivo<input name="buscar" maxlength="80" value="<%= Html.texto(filtro.busqueda()) %>" placeholder="Descripción">
</label>
<button class="adm-btn-primary">Consultar</button>
<a class="adm-btn-outline" href="<%= Html.texto(ruta) %>">Limpiar</a>
</form>
<% if(panel.puedeEditar()){ %>
<details class="motivos-alta">
<summary>Crear motivo de consulta</summary>
<form method="post" action="<%= Html.texto(ruta) %>">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
<input type="hidden" name="accion" value="guardar">
<label>Especialidad<select name="especialidad" required>
<option value="" selected disabled>Selecciona</option>
<% for(var e:panel.especialidades()){ %>
<option value="<%= e.id() %>">
<%= Html.texto(e.nombre()) %>
</option>
<% } %>
</select>
</label>
<label>Descripción<input name="descripcion" minlength="3" maxlength="200" required>
</label>
<button class="adm-btn-primary">Crear motivo</button>
</form>
</details>
<% } %>
<section class="motivos-panel" aria-label="Catálogo de motivos">
<div class="motivos-total">
<%= panel.total() %> motivos registrados</div>
<div class="motivos-tabla">
<table>
<thead>
<tr>
<th>Especialidad</th>
<th>Motivo de consulta</th>
<th>Estado</th>
<% if(panel.puedeEditar()){ %>
<th>Acciones</th>
<% } %>
</tr>
</thead>
<tbody>
<% if(panel.motivos().isEmpty()){ %>
<tr>
<td colspan="<%= panel.puedeEditar()?4:3 %>">No hay motivos para los filtros seleccionados.</td>
</tr>
<% } %>
<% for(var m:panel.motivos()){ %>
<tr>
<td>
<%= Html.texto(m.nombreEspecialidad()) %>
</td>
<td>
<%= Html.texto(m.descripcion()) %>
<% if(m.utilizado()){ %>
<small>Registrado en citas · descripción protegida</small>
<% } %>
</td>
<td>
<span class="motivos-estado">
<%= "ACTIVO".equals(m.estado())?"Activo":"Inactivo" %>
</span>
</td>
<% if(panel.puedeEditar()){ %>
<td>
<% if(!m.utilizado()){ %>
<details class="motivos-editar">
<summary>Editar descripción</summary>
<form method="post" action="<%= Html.texto(ruta) %>">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
<input type="hidden" name="accion" value="guardar">
<input type="hidden" name="id" value="<%= m.id() %>">
<input type="hidden" name="especialidad" value="<%= m.especialidad() %>">
<label>Descripción<input name="descripcion" minlength="3" maxlength="200" required value="<%= Html.texto(m.descripcion()) %>">
</label>
<button class="adm-btn-primary">Guardar descripción</button>
</form>
</details>
<% } %>
<form method="post" action="<%= Html.texto(ruta) %>" onsubmit="return confirm('¿Cambiar la disponibilidad de este motivo para futuras reservas? Las citas existentes se conservan.');">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
<input type="hidden" name="accion" value="estado">
<input type="hidden" name="id" value="<%= m.id() %>">
<input type="hidden" name="estado" value="<%= "ACTIVO".equals(m.estado())?"INACTIVO":"ACTIVO" %>">
<button class="adm-btn-outline">
<%= "ACTIVO".equals(m.estado())?"Desactivar":"Activar" %>
</button>
</form>
</td>
<% } %>
</tr>
<% } %>
</tbody>
</table>
</div>
<nav class="motivos-paginacion" aria-label="Páginas del catálogo">
<% if(filtro.pagina()>1){ %>
<a class="adm-btn-outline" href="<%= Html.texto(ruta+consulta+"&pagina="+(filtro.pagina()-1)) %>">Anterior</a>
<% } %>
<span>Página <%= filtro.pagina() %> de <%= panel.paginas() %>
</span>
<% if(filtro.pagina()<panel.paginas()){ %>
<a class="adm-btn-outline" href="<%= Html.texto(ruta+consulta+"&pagina="+(filtro.pagina()+1)) %>">Siguiente</a>
<% } %>
</nav>
</section>
</main>
</div>
</body>
</html>
