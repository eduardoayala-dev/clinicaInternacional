<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jspf/medico-contexto.jspf" %>
<%
    String seccionMedico = "pacientes";
    String tituloPagina = "Mis pacientes";
    PacienteResumen paciente = (PacienteResumen) request.getAttribute("paciente");
    Pagina<?> paginaDatos = (Pagina<?>) request.getAttribute(paciente == null ? "pacientes" : "atenciones");
    String consultaPacientes = java.util.Objects.toString(request.getParameter("q"), "");
    String rutaPagina = paciente == null ? "/medico/pacientes?q="+java.net.URLEncoder.encode(consultaPacientes,java.nio.charset.StandardCharsets.UTF_8) : "/medico/pacientes?idPaciente="+paciente.idPaciente();
%>
<!DOCTYPE html>
<html lang="es">
<head>
<title><%= Html.texto(tituloPagina) %> — Portal del médico</title>
<%@ include file="/WEB-INF/jspf/medico-head.jspf" %>
</head>
<body>
<%@ include file="/WEB-INF/jspf/medico-sidebar.jspf" %>
<div class="med-main-wrapper">
    <%@ include file="/WEB-INF/jspf/medico-topbar.jspf" %>
    <main class="med-content-scroll" id="medContenido" tabindex="-1">
        <div class="med-header-row">
            <div><h1 class="med-title"><%= Html.texto(paciente == null ? "Mis pacientes" : "Historial de atenciones") %></h1>
            <p class="med-subtitle">Pacientes que has atendido. El historial muestra tus propias consultas.</p></div>
            <% if (paciente != null) { %><a class="med-btn-outline" href="${pageContext.request.contextPath}/medico/pacientes">Volver a mis pacientes</a><% } %>
        </div>
        <% if (paciente == null) { %>
        <form class="med-search-form" method="GET" action="${pageContext.request.contextPath}/medico/pacientes">
            <div class="med-form-group"><label class="med-form-label" for="buscarPaciente">Nombre o documento</label>
            <input class="med-form-input" type="search" id="buscarPaciente" name="q" maxlength="100" value="<%= Html.texto(consultaPacientes) %>" placeholder="Buscar entre mis pacientes"></div>
            <button type="submit" class="med-btn-primary">Buscar</button>
        </form>
        <p class="med-table-hint">Desliza la tabla para ver todas las columnas.</p>
        <section class="med-table-card" aria-label="Mis pacientes">
            <table class="med-table"><thead><tr><th>PACIENTE</th><th>DOCUMENTO</th><th>ÚLTIMA CONSULTA</th><th>ATENCIONES</th><th>ACCIÓN</th></tr></thead><tbody>
            <% for (Object item : paginaDatos.items()) { PacienteResumen fila = (PacienteResumen) item; %>
            <tr><td class="td-patient-name"><%= Html.texto(fila.nombre()) %></td><td><%= Html.texto(fila.dni()) %></td><td><%= Html.texto(FormatoClinico.fechaHora(fila.ultimaAtencion())) %></td><td><%= Html.texto(fila.atenciones()) %></td>
                <td><a class="med-tbl-btn med-tbl-btn-outline" href="${pageContext.request.contextPath}/medico/pacientes?idPaciente=<%= Html.texto(fila.idPaciente()) %>">Ver historial</a></td></tr>
            <% } %>
            <% if (paginaDatos.items().isEmpty()) { %><tr><td colspan="5">No hay pacientes atendidos que coincidan con la búsqueda.</td></tr><% } %>
            </tbody></table>
        </section>
        <% } else { %>
        <section class="med-atencion-card" aria-label="Datos del paciente">
            <h2 class="med-section-heading"><%= Html.texto(paciente.nombre()) %></h2>
            <dl class="med-registro-tiempos">
                <div><dt>Documento</dt><dd><%= Html.texto(paciente.dni()) %></dd></div>
                <div><dt>Fecha de nacimiento</dt><dd><%= Html.texto(FormatoClinico.fecha(paciente.nacimiento())) %></dd></div>
                <div><dt>Alergias registradas</dt><dd><%= Html.texto(paciente.alergias().isBlank() ? "Sin registrar" : paciente.alergias()) %></dd></div>
            </dl>
        </section>
        <p class="med-table-hint">Desliza la tabla para ver todas las columnas.</p>
        <section class="med-table-card" aria-label="Historial de mis atenciones">
            <table class="med-table"><thead><tr><th>FECHA DE CITA</th><th>ESTADO</th><th>DIAGNÓSTICO</th><th>INDICACIONES</th><th>SEGUIMIENTO</th><th>ACCIÓN</th></tr></thead><tbody>
            <% for (Object item : paginaDatos.items()) { AtencionResumen fila = (AtencionResumen) item; %>
            <tr><td><%= Html.texto(FormatoClinico.fechaHora(fila.fecha())) %></td><td><span class="med-badge med-badge-pendiente"><%= Html.texto(FormatoClinico.estado(fila.estado())) %></span></td>
                <td><span class="med-clinical-preview"><%= Html.texto(fila.diagnostico().isBlank() ? "Sin registro estructurado" : fila.diagnostico()) %></span></td>
                <td><span class="med-clinical-preview"><%= Html.texto(fila.indicaciones().isBlank() ? "Sin registrar" : fila.indicaciones()) %></span></td><td><%= Html.texto(FormatoClinico.fecha(fila.seguimiento())) %></td>
                <td><a class="med-tbl-btn med-tbl-btn-outline" href="${pageContext.request.contextPath}/medico/atencion?idCita=<%= Html.texto(fila.idCita()) %>">Ver ficha</a></td></tr>
            <% } %>
            <% if (paginaDatos.items().isEmpty()) { %><tr><td colspan="6">No hay atenciones en esta página.</td></tr><% } %>
            </tbody></table>
        </section>
        <% } %>
        <%@ include file="/WEB-INF/jspf/medico-paginacion.jspf" %>
    </main>
</div>
</body>
</html>
