<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jspf/medico-contexto.jspf" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    String seccionMedico = "horarios";
    String tituloPagina = "Mis horarios";
    var propuestas = (java.util.List<PropuestaHorario>) request.getAttribute("propuestas");
    var hoy = (java.time.LocalDate) request.getAttribute("hoy");
%>
<!DOCTYPE html><html lang="es"><head><title>Mis horarios — Portal del médico</title>
<%@ include file="/WEB-INF/jspf/medico-head.jspf" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/horarios-propuestas.css">
<script defer src="${pageContext.request.contextPath}/assets/js/horarios-propuestas.js"></script>
</head><body>
<%@ include file="/WEB-INF/jspf/medico-sidebar.jspf" %>
<div class="med-main-wrapper"><%@ include file="/WEB-INF/jspf/medico-topbar.jspf" %>
<main class="med-content-scroll" id="medContenido" tabindex="-1">
    <div class="med-header-row"><div><h1 class="med-title">Mis horarios</h1><p class="med-subtitle">Propón tus turnos. Admisión revisa y publica la disponibilidad para los pacientes.</p></div></div>
    <%@ include file="/WEB-INF/jspf/horarios-mensajes.jspf" %>
    <section class="med-atencion-card" aria-labelledby="proponerTitulo">
        <h2 class="med-section-heading" id="proponerTitulo">Nueva propuesta semanal</h2>
        <p>Sede asignada: <strong><%= Html.texto(request.getAttribute("sedeMedico")) %></strong>. Las horas corresponden a Perú.</p>
        <form method="POST" action="${pageContext.request.contextPath}/medico/horarios" data-pending-label="Enviando…">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
            <fieldset class="hor-dias"><legend>Días de atención</legend>
            <% String[] dias = {"Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo"}; for (int i=0;i<7;i++) { %>
                <label><input type="checkbox" name="dias" value="<%= i+1 %>"> <%= dias[i] %></label>
            <% } %></fieldset>
            <div class="hor-form-grid">
                <div class="med-form-group"><label class="med-form-label" for="desde">Vigente desde</label><input class="med-form-input" type="date" id="desde" name="desde" min="<%= hoy %>" value="<%= hoy %>" required></div>
                <div class="med-form-group"><label class="med-form-label" for="hasta">Vigente hasta</label><input class="med-form-input" type="date" id="hasta" name="hasta" min="<%= hoy %>" required></div>
                <div class="med-form-group"><label class="med-form-label" for="inicio">Hora de inicio</label><input class="med-form-input" type="time" id="inicio" name="inicio" required></div>
                <div class="med-form-group"><label class="med-form-label" for="fin">Hora de fin</label><input class="med-form-input" type="time" id="fin" name="fin" required></div>
                <div class="med-form-group"><label class="med-form-label" for="minutos">Minutos por cita</label><input class="med-form-input" type="number" id="minutos" name="minutos" min="5" max="240" value="30" required></div>
            </div>
            <p class="med-subtitle">Vigencia máxima: 90 días. El turno debe permitir citas completas. Las renovaciones se envían como nuevas propuestas.</p>
            <button type="submit" class="med-btn-primary"><span class="med-button-label">Enviar a admisión</span></button>
        </form>
    </section>
    <h2 class="med-section-heading">Propuestas recientes</h2>
    <p class="med-subtitle">Se muestran las últimas 100 propuestas. Los horarios publicados próximos a vencer se señalan con 7 días de anticipación.</p>
    <%@ include file="/WEB-INF/jspf/horarios-tabla.jspf" %>
</main></div></body></html>
