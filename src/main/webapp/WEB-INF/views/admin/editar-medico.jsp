<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="pe.edu.utp.clinica.dao.PadronMedicosDAO,pe.edu.utp.clinica.util.Html" %>
<% PadronMedicosDAO.Edicion edicion=(PadronMedicosDAO.Edicion)request.getAttribute("edicionMedico"); %>
<!DOCTYPE html>
<html lang="es">
<head>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Editar médico · Clínica Internacional</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-administrativo.css?v=20261103-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/padron-medicos.css?v=20261103-1">
</head>
<body>
<%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>
<main class="adm-main-wrapper">
    <header class="adm-topbar">Admisión / Médicos / Datos profesionales</header>
    <div class="adm-content-scroll">
        <h1 class="adm-page-title">Editar datos profesionales</h1>
        <% if(request.getAttribute("errorMedico")!=null) { %>
        <div class="padron-error" role="alert"><%= Html.texto(request.getAttribute("errorMedico")) %></div>
        <a class="adm-btn-outline" href="${pageContext.request.contextPath}/admin/medicos">Volver al padrón</a>
        <% } else if(edicion!=null) { PadronMedicosDAO.Medico medico=edicion.medico(); %>
        <form method="post" action="${pageContext.request.contextPath}/admin/medicos/editar" class="padron-edicion">
            <input type="hidden" name="csrfToken" value="<%= Html.texto(session.getAttribute("csrfToken")) %>">
            <input type="hidden" name="id" value="<%= medico.id() %>">
            <h2><%= Html.texto(medico.nombre()) %></h2>
            <dl><dt>CMP</dt><dd><%= Html.texto(medico.cmp()) %></dd><dt>Sede asignada</dt><dd><%= Html.texto(medico.sede()) %></dd></dl>
            <p class="padron-ayuda">La identidad, el CMP, el correo y los traslados se gestionan desde la administración del sistema.</p>
            <label for="especialidad">Especialidad</label>
            <select id="especialidad" name="especialidad" required>
                <% for(PadronMedicosDAO.Especialidad especialidad:edicion.especialidades()) { %>
                <option value="<%= especialidad.id() %>" <%= especialidad.id()==medico.especialidadId()?"selected":"" %>><%= Html.texto(especialidad.nombre()) %></option>
                <% } %>
            </select>
            <label for="estado">Estado profesional</label>
            <select id="estado" name="estado" required>
                <% for(String estado:new String[]{"ACTIVO","LICENCIA","INACTIVO"}) { %>
                <option value="<%= estado %>" <%= estado.equals(medico.estado())?"selected":"" %>><%= estado.equals("ACTIVO")?"Activo":estado.equals("LICENCIA")?"Licencia":"Inactivo" %></option>
                <% } %>
            </select>
            <p class="padron-ayuda">Los cambios respetan las citas registradas y los horarios publicados. Si existe una incompatibilidad, el sistema explicará qué debes coordinar primero.</p>
            <div class="padron-acciones"><a class="adm-btn-outline" href="${pageContext.request.contextPath}/admin/medicos">Cancelar</a><button class="adm-btn-primary" type="submit">Guardar cambios</button></div>
        </form>
        <% } %>
    </div>
</main>
<script src="${pageContext.request.contextPath}/assets/js/padron-edicion.js?v=20261103-1"></script>
</body>
</html>
