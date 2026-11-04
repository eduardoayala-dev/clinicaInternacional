<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario,pe.edu.utp.clinica.model.catalogo.FilaImportacionMotivo,pe.edu.utp.clinica.util.Html,java.util.List" %>
<%
    Usuario usuario=(Usuario)session.getAttribute("usuarioLogueado");
    String nombrePersonal=usuario.getNombres()+" "+usuario.getApellidos();
    String inicialesPersonal=nombrePersonal.isBlank()?"A":nombrePersonal.substring(0,1);
    boolean sistema="ADMINISTRADOR".equals(usuario.getRol());
    String rutaImportacion=request.getContextPath()+(sistema?"/sysadmin/motivos/importar":"/admin/motivos/importar");
    String rutaCatalogo=request.getContextPath()+(sistema?"/sysadmin/motivos":"/admin/motivos");
    boolean terminada=Boolean.TRUE.equals(request.getAttribute("importacionTerminada"));
    List<FilaImportacionMotivo> filas=(List<FilaImportacionMotivo>)request.getAttribute("filasImportacion");
    String confirmacion=(String)request.getAttribute("confirmacionImportacion");
    long errores=filas.stream().filter(f->!f.error().isEmpty()).count();
%>
<!DOCTYPE html>
<html lang="es">
<head>
<%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Importar motivos | Clínica Internacional</title>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:wght@600;700&display=swap" rel="stylesheet">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-administrativo.css?v=20261103-2">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/catalogo-motivos.css?v=20261103-2">
<script defer src="${pageContext.request.contextPath}/assets/js/importar-motivos.js?v=20261103-1"></script>
<% if(sistema){ %><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/sysadmin-shell.css?v=20261104-admin2"><% } %><% if(!sistema){ %><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admision-shell.css?v=20261104-pa"><% } %></head>
<body class="<%= sistema ? "sysadmin-portal" : "portal-admision" %>">
<% if(sistema){ %><%@ include file="/WEB-INF/jspf/motivos-sysadmin-sidebar.jspf" %><% }else{ %><%@ include file="/WEB-INF/jspf/admin-sidebar-medicos.jspf" %><% } %>
<div class="adm-main-wrapper">
<header class="adm-topbar"><div class="adm-breadcrumb"><%= sistema?"Administración":"Admisión" %> / Motivos / Importar Excel</div></header>
<main class="adm-content-scroll motivos-page">
<a class="motivos-volver" href="<%= Html.texto(rutaCatalogo) %>">‹ Volver al catálogo</a>
<h1 class="adm-page-title">Importar motivos de consulta</h1>
<p class="adm-page-subtitle">Revisa el archivo antes de guardar. La importación crea motivos nuevos y conserva los existentes.</p>
<section class="motivos-alta" aria-labelledby="archivoTitulo">
<h2 id="archivoTitulo">1. Prepara y revisa tu archivo</h2>
<p class="motivos-nota">Descarga la plantilla, completa la hoja Motivos y usa los IDs de la hoja Especialidades. Máximo 200 filas y 1 MB. Solo XLSX, sin fórmulas ni macros.</p>
<p><a class="adm-btn-outline" href="<%= Html.texto(rutaImportacion+"?accion=plantilla") %>">Descargar plantilla Excel</a></p>
<output id="importacionArchivoError" role="alert" hidden></output>
<form data-importacion-motivos method="post" enctype="multipart/form-data" action="<%= Html.texto(rutaImportacion) %>">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
<input type="hidden" name="accion" value="previsualizar">
<label>Archivo XLSX<input type="file" name="archivo" aria-describedby="importacionArchivoError" accept=".xlsx" required></label>
<button class="adm-btn-primary" type="submit">Revisar archivo</button>
</form>
</section>
<% if(!filas.isEmpty()){ %>
<section class="motivos-panel" aria-labelledby="revisionTitulo">
<div class="motivos-total"><h2 id="revisionTitulo"><%= terminada?"Resultado de la importación":"2. Revisa las filas" %></h2><p><% if(terminada){ %><%= request.getAttribute("importados") %> motivos guardados · <%= errores %> filas no importadas.<% }else{ %><%= filas.size()-errores %> válidas · <%= errores %> con errores. Todavía no se guardaron motivos.<% } %></p></div>
<div class="motivos-tabla motivos-revision"><table>
<thead><tr><th>Fila de Excel</th><th>Especialidad ID</th><th>Motivo</th><th>Revisión</th></tr></thead>
<tbody>
<% for(var f:filas){ %>
<tr><td><%= f.numero() %></td><td><%= f.especialidad()>0?Integer.toString(f.especialidad()):"—" %></td><td><%= Html.texto(f.descripcion()) %></td><td><span class="<%= f.error().isEmpty()?"motivos-fila-valida":"motivos-fila-error" %>"><%= Html.texto(f.error().isEmpty()?(terminada?"Importado":"Lista para importar"):f.error()) %></span></td></tr>
<% } %>
</tbody></table></div>
<% if(errores>0){ %><p class="motivos-importacion-aviso"><%= terminada?"Las filas con errores no se guardaron. Corrígelas y vuelve a cargar únicamente esas filas.":"Puedes importar las filas correctas. Las filas con errores quedarán excluidas." %></p><% } %>
</section>
<% if(!confirmacion.isEmpty()){ %>
<section class="motivos-alta"><h2>3. Confirma las filas correctas</h2><p class="motivos-nota">Se crearán <%= filas.size()-errores %> motivos; las filas con errores quedarán excluidas. Esta revisión vence en 10 minutos y solo puede confirmarse una vez. Si el catálogo cambió, se volverá a validar antes de guardar.</p>
<form data-importacion-motivos method="post" action="<%= Html.texto(rutaImportacion) %>">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><input type="hidden" name="accion" value="confirmar"><input type="hidden" name="confirmacion" value="<%= Html.texto(confirmacion) %>">
<button class="adm-btn-primary" type="submit">Confirmar e importar <%= filas.size()-errores %> motivos</button>
<a class="adm-btn-outline" href="<%= Html.texto(rutaCatalogo) %>">Volver sin importar</a>
</form></section>
<% } %>
<% } %>
</main></div>
</body></html>
