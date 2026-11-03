<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="pe.edu.utp.clinica.util.Html,pe.edu.utp.clinica.model.Usuario" %>
<% Usuario u=(Usuario)session.getAttribute("usuarioLogueado");String portal=u!=null&&"ADMINISTRADOR".equals(u.getRol())?"/sysadmin/motivos":"/admin/motivos"; %>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Revisar catálogo</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/catalogo-motivos.css?v=20261103-1">
</head>
<body class="motivos-error">
<main>
<h1>No se pudo actualizar el catálogo</h1>
<p>
<%= Html.texto(request.getAttribute("mensajeCatalogo")) %>
</p>
<p>Revisa los datos antes de volver a enviar el formulario.</p>
<a href="<%= Html.texto(request.getContextPath()+portal) %>">Volver a motivos de consulta</a>
</main>
</body>
</html>
