<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jspf/medico-contexto.jspf" %>
<%
    String seccionMedico = "seguimientos";
    String tituloPagina = "Seguimientos";
    Pagina<SeguimientoResumen> paginaDatos = (Pagina<SeguimientoResumen>) request.getAttribute("seguimientos");
    SeguimientoResumen seleccion = (SeguimientoResumen) request.getAttribute("seleccion");
    java.util.List<CitaControl> controles = (java.util.List<CitaControl>) request.getAttribute("controles");
    String consultaSeguimientos = java.util.Objects.toString(request.getParameter("q"), "");
    String estadoSeguimientos = java.util.Objects.toString(request.getParameter("estado"), "");
    String rutaPagina = "/medico/seguimientos?q="+java.net.URLEncoder.encode(consultaSeguimientos,java.nio.charset.StandardCharsets.UTF_8)+"&estado="+java.net.URLEncoder.encode(estadoSeguimientos,java.nio.charset.StandardCharsets.UTF_8);
%>
<!DOCTYPE html>
<html lang="es">
<head><%@ include file="/WEB-INF/jspf/medico-head.jspf" %></head>
<body>
<%@ include file="/WEB-INF/jspf/medico-sidebar.jspf" %>
<div class="med-main-wrapper">
    <%@ include file="/WEB-INF/jspf/medico-topbar.jspf" %>
    <main class="med-content-scroll" id="medContenido" tabindex="-1">
        <div class="med-header-row"><div><h1 class="med-title">Seguimientos</h1><p class="med-subtitle">Controles recomendados en tus consultas. Una recomendación no es una reserva confirmada.</p></div></div>
        <form class="med-search-form" method="GET" action="${pageContext.request.contextPath}/medico/seguimientos">
            <div class="med-form-group"><label class="med-form-label" for="buscarSeguimiento">Nombre o documento</label><input class="med-form-input" type="search" id="buscarSeguimiento" name="q" maxlength="100" value="<%= Html.texto(consultaSeguimientos) %>" placeholder="Buscar paciente"></div>
            <div class="med-form-group"><label class="med-form-label" for="estadoSeguimiento">Estado del seguimiento</label>
                <select class="med-form-input" name="estado" id="estadoSeguimiento">
                    <% for (String estado : java.util.List.of("", "PENDIENTE", "VENCIDO", "RESERVADO", "REALIZADO")) { %>
                    <option value="<%= Html.texto(estado) %>" <%= Html.texto(estado.equals(estadoSeguimientos) ? "selected" : "") %>><%= Html.texto(estado.isEmpty() ? "Todos" : FormatoClinico.estado(estado)) %></option>
                    <% } %>
                </select>
            </div>
            <button type="submit" class="med-btn-primary">Consultar</button>
        </form>
        <% if (seleccion != null) { %>
        <section class="med-atencion-card" aria-label="Vincular cita de control">
            <h2 class="med-section-heading">Control de <%= Html.texto(seleccion.paciente()) %></h2>
            <p>Fecha recomendada: <strong><%= Html.texto(FormatoClinico.fecha(seleccion.fecha())) %></strong> · <%= Html.texto(FormatoClinico.estado(seleccion.estado())) %></p>
            <% if ("RESERVADO".equals(seleccion.estado()) || "REALIZADO".equals(seleccion.estado())) { %>
            <p>Reserva vinculada para <strong><%= Html.texto(FormatoClinico.fechaHora(seleccion.fechaControl())) %></strong>.</p>
            <a class="med-btn-outline" href="${pageContext.request.contextPath}/medico/atencion?idCita=<%= Html.texto(seleccion.idControl()) %>">Ver cita de control</a>
            <% } else if (controles.isEmpty()) { %>
            <p>No hay una cita posterior disponible para vincular. El paciente o admisión debe reservarla primero en la disponibilidad publicada.</p>
            <% } else { %>
            <p>Selecciona una reserva existente del mismo paciente contigo. Vincularla no crea ni cambia la fecha de la cita.</p>
            <form class="med-search-form" id="formVincularSeguimiento" method="POST" action="${pageContext.request.contextPath}/medico/seguimientos" data-pending-label="Vinculando…" onsubmit="return confirm('¿Vincular esta reserva como control de la consulta seleccionada?')">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <input type="hidden" name="accion" value="vincular">
                <input type="hidden" name="idCitaOrigen" value="<%= Html.texto(seleccion.idCita()) %>">
                <div class="med-form-group"><label class="med-form-label" for="idCitaControl">Reserva de control</label>
                    <select class="med-form-input" id="idCitaControl" name="idCitaControl" required><option value="">Seleccionar reserva</option>
                    <% for (CitaControl control : controles) { %><option value="<%= Html.texto(control.idCita()) %>">Cita <%= Html.texto(control.idCita()) %> · <%= Html.texto(FormatoClinico.fechaHora(control.fecha())) %> · <%= Html.texto(FormatoClinico.estado(control.estado())) %></option><% } %>
                    </select>
                </div>
                <button class="med-btn-primary" type="submit"><span class="med-button-label">Vincular reserva</span></button>
            </form>
            <% } %>
        </section>
        <% } %>
        <p class="med-table-hint">Desliza la tabla para ver todas las columnas.</p>
        <section class="med-table-card" aria-label="Seguimientos recomendados">
            <table class="med-table"><thead><tr><th>PACIENTE</th><th>DOCUMENTO</th><th>FECHA RECOMENDADA</th><th>ESTADO</th><th>RESERVA DE CONTROL</th><th>ACCIÓN</th></tr></thead><tbody>
            <% for (SeguimientoResumen fila : paginaDatos.items()) { %>
            <tr><td class="td-patient-name"><%= Html.texto(fila.paciente()) %></td><td><%= Html.texto(fila.dni()) %></td><td><%= Html.texto(FormatoClinico.fecha(fila.fecha())) %></td>
                <td><span class="med-badge <%= Html.texto("VENCIDO".equals(fila.estado()) ? "med-badge-tarde" : "REALIZADO".equals(fila.estado()) ? "med-badge-atendido" : "med-badge-ensala") %>"><%= Html.texto(FormatoClinico.estado(fila.estado())) %></span></td>
                <td><%= Html.texto("RESERVADO".equals(fila.estado()) || "REALIZADO".equals(fila.estado()) ? FormatoClinico.fechaHora(fila.fechaControl()) : "Sin reserva vigente") %></td>
                <td><a class="med-tbl-btn med-tbl-btn-outline" href="${pageContext.request.contextPath}/medico/seguimientos?origen=<%= Html.texto(fila.idCita()) %>"><%= Html.texto("RESERVADO".equals(fila.estado()) || "REALIZADO".equals(fila.estado()) ? "Ver control" : "Vincular reserva") %></a>
                    <a class="med-tbl-btn med-tbl-btn-outline" href="${pageContext.request.contextPath}/medico/atencion?idCita=<%= Html.texto(fila.idCita()) %>">Ver consulta</a></td></tr>
            <% } %>
            <% if (paginaDatos.items().isEmpty()) { %><tr><td colspan="6">No hay seguimientos que coincidan con los filtros.</td></tr><% } %>
            </tbody></table>
        </section>
        <%@ include file="/WEB-INF/jspf/medico-paginacion.jspf" %>
    </main>
</div>
</body>
</html>
