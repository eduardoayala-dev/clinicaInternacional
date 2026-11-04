<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html><html lang="es"><head>
<title>IA de inasistencias — Clínica Internacional</title>
<%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/ia-inasistencia.css">
<script defer src="${pageContext.request.contextPath}/assets/js/ia-inasistencia-api.js"></script>
<script defer src="${pageContext.request.contextPath}/assets/js/ia-inasistencia.js"></script>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-forms.css?v=20261104-1"></head><body>
<header class="ia-cabecera"><a href="${pageContext.request.contextPath}/sysadmin/dashboard"><img src="${pageContext.request.contextPath}/assets/images/medico/logo-clinica.svg" alt="">Clínica Internacional</a><span>Administrador del sistema</span></header>
<main class="ia-contenido">
    <a class="ia-volver" href="${pageContext.request.contextPath}/sysadmin/priorizacion">‹ Volver a priorización</a>
    <h1>IA de inasistencias</h1>
    <p>Simulador del modelo entrenado. Permite comprobar la integración sin usar datos de pacientes ni cambiar citas.</p>
    <aside class="ia-aviso"><strong>Experimental · entrenamiento con datos sintéticos</strong><p>Los resultados demuestran el funcionamiento técnico. No representan precisión validada para pacientes reales. La IA solo sugiere.</p></aside>
    <div class="ia-columnas">
        <section class="ia-tarjeta" aria-labelledby="iaDatos"><h2 id="iaDatos">Escenario de prueba</h2>
            <form id="formIa" action="${pageContext.request.contextPath}/sysadmin/ia/inasistencia" method="POST">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <label for="faltas">Inasistencias anteriores</label><input id="faltas" name="inasistenciasPrevias" type="number" min="0" max="100" value="0" required>
                <label for="atenciones">Atenciones anteriores</label><input id="atenciones" name="atencionesPrevias" type="number" min="0" max="100" value="5" required>
                <label for="anticipacion">Días entre reserva y cita</label><input id="anticipacion" name="anticipacionDias" type="number" min="0" max="180" value="7" required>
                <label for="confirmada">¿El paciente confirmó expresamente?</label><select id="confirmada" name="confirmadaPorPaciente" required><option value="true">Sí</option><option value="false">No</option></select>
                <p class="ia-ayuda">Una reserva creada automáticamente como «confirmada» no equivale a una confirmación expresa del paciente.</p>
                <button id="iaCalcular" type="submit">Simular predicción</button>
            </form>
        </section>
        <section class="ia-tarjeta" aria-labelledby="iaResultado"><h2 id="iaResultado">Resultado y factores</h2>
            <output id="iaEstado" aria-live="polite">Completa un escenario para evaluar el modelo.</output>
            <div id="iaError" role="alert" hidden></div>
            <div id="iaPrediccion" hidden>
                <p class="ia-probabilidad" id="iaProbabilidad"></p><p id="iaNivel"></p><p id="iaAccion"></p>
                <h3>Influencia respecto al promedio de entrenamiento</h3><ul id="iaFactores"></ul>
                <p class="ia-ayuda">Las contribuciones se expresan en la escala interna del modelo; no son puntos porcentuales ni causas.</p>
                <ul id="iaAdvertencias"></ul><p class="ia-ayuda" id="iaVersion"></p>
            </div>
        </section>
    </div>
</main></body></html>
