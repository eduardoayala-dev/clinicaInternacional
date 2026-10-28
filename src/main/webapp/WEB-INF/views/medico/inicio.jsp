<%@ page import="pe.edu.utp.clinica.util.Html" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    String seccionMedico = "inicio";
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreMedico = (String) request.getAttribute("nombreMedico");
    if (nombreMedico == null && usuario != null) {
        nombreMedico = "Dr. " + usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreMedico == null) nombreMedico = "Médico";

    String sedeMedico = (String) request.getAttribute("sedeMedico");
    if (sedeMedico == null) sedeMedico = "Sin registrar";

    String fechaConsulta = (String) request.getAttribute("fechaActual");
    java.time.LocalDate fechaInicioMedico = fechaConsulta == null
            ? java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")) : java.time.LocalDate.parse(fechaConsulta);
    String fechaActual = fechaInicioMedico.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM yyyy", java.util.Locale.forLanguageTag("es-PE")));

    Integer totalCitas = (Integer) request.getAttribute("totalCitas");
    if (totalCitas == null) totalCitas = 0;

    Integer atendidos = (Integer) request.getAttribute("atendidos");
    if (atendidos == null) atendidos = 0;

    Integer enEspera = (Integer) request.getAttribute("enEspera");
    if (enEspera == null) enEspera = 0;

    Integer pendientes = (Integer) request.getAttribute("pendientes");
    if (pendientes == null) pendientes = 0;

    Integer pendientesRestantes = (Integer) request.getAttribute("pendientesRestantes");
    if (pendientesRestantes == null) pendientesRestantes = 0;

    Integer enAtencion = (Integer) request.getAttribute("enAtencion");
    if (enAtencion == null) enAtencion = 0;

    Map<String, Object> pacienteEnSala = (Map<String, Object>) request.getAttribute("pacienteEnSala");
    List<Map<String, Object>> agenda = (List<Map<String, Object>>) request.getAttribute("agenda");

    String inicialesMedico = "M";
    if (nombreMedico != null && !nombreMedico.trim().isEmpty()) {
        String cleanNom = nombreMedico.replace("Dr.", "").replace("Dra.", "").trim();
        String[] partes = cleanNom.split("\\s+");
        if (partes.length >= 2) {
            inicialesMedico = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else if (partes.length == 1 && !partes[0].isEmpty()) {
            inicialesMedico = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Portal del Médico — Clínica Internacional</title>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Serif+Display&amp;family=Inter:wght@400;500;600;700&amp;display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-medico.css?v=20261028">
    <script defer src="${pageContext.request.contextPath}/assets/js/medico-portal.js?v=20261028"></script>
    <script defer src="${pageContext.request.contextPath}/assets/js/medico-inicio.js?v=20261028"></script>
</head>
<body>


    <%@ include file="/WEB-INF/jspf/medico-sidebar.jspf" %>


    <div class="med-main-wrapper">
        <%@ include file="/WEB-INF/jspf/medico-topbar.jspf" %>

        <main class="med-content-scroll" id="medContenido" tabindex="-1">

            <% if (pacienteEnSala != null) { %>
                <div class="med-banner-alert">
                    <div class="med-banner-icon">
                        <img src="${pageContext.request.contextPath}/assets/images/medico/sala.svg" alt="" class="med-icon">
                    </div>
                    <div class="med-banner-text">
                        <div class="med-banner-title">Paciente en sala de espera</div>
                        <div class="med-banner-desc">
                            <strong><%= Html.texto(pacienteEnSala.get("paciente")) %></strong> está en sala. Hora de llegada: <strong><%= Html.texto(pacienteEnSala.get("horaLlegada")) %></strong>.
                        </div>
                    </div>
                </div>
            <% } %>


            <div class="med-header-row">
                <div>
                    <h1 class="med-title">Hola, <%= Html.texto(nombreMedico) %></h1>
                    <div class="med-subtitle">
                        <%= Html.texto(fechaActual) %> &nbsp;·&nbsp; <%= Html.texto(sedeMedico) %>
                    </div>
                </div>
                <div class="med-header-actions">
                    <a href="${pageContext.request.contextPath}/medico/atencion" class="med-btn-outline">
                        <img src="${pageContext.request.contextPath}/assets/images/medico/atender.svg" alt="" class="med-icon"> Gestionar atención actual
                    </a>
                    <a href="${pageContext.request.contextPath}/medico/agenda" class="med-btn-primary">
                        Ir a mi agenda del día <img src="${pageContext.request.contextPath}/assets/images/medico/flecha-derecha.svg" alt="" class="med-icon">
                    </a>
                </div>
            </div>


            <div class="med-kpi-grid">

                <div class="med-kpi-card">
                    <div class="med-kpi-top">
                        <span class="med-kpi-label">Citas registradas hoy</span>
                        <div class="med-kpi-icon"><img src="${pageContext.request.contextPath}/assets/images/medico/calendario-indicador.svg" alt="" class="med-icon"></div>
                    </div>
                    <div class="med-kpi-val"><%= Html.texto(totalCitas) %> citas</div>
                    <div class="med-kpi-pill-group">
                        <span class="med-pill med-pill-green"><%= Html.texto(atendidos) %> <%= Html.texto(atendidos == 1 ? "atendido" : "atendidos") %></span>
                        <span class="med-pill med-pill-blue"><%= Html.texto(enEspera) %> en sala</span>
                        <span class="med-pill med-pill-gray"><%= Html.texto(pendientes) %> <%= Html.texto(pendientes == 1 ? "pendiente de llegada" : "pendientes de llegada") %></span>
                    </div>
                </div>


                <div class="med-kpi-card">
                    <div class="med-kpi-top">
                        <span class="med-kpi-label">Pacientes pendientes de atención</span>
                        <div class="med-kpi-icon"><img src="${pageContext.request.contextPath}/assets/images/medico/pacientes-indicador.svg" alt="" class="med-icon"></div>
                    </div>
                    <div class="med-kpi-val"><%= Html.texto(pendientesRestantes) %> <%= Html.texto(pendientesRestantes == 1 ? "restante" : "restantes") %></div>
                    <div class="med-kpi-foot-text">
                        <%= Html.texto(pendientes) %> <%= Html.texto(pendientes == 1 ? "pendiente de llegada" : "pendientes de llegada") %> · <%= Html.texto(enEspera) %> en sala · <%= Html.texto(request.getAttribute("enAtencion")) %> en atención
                    </div>
                </div>


                <div class="med-kpi-card">
                    <div class="med-kpi-top">
                        <span class="med-kpi-label">Pacientes en sala de espera</span>
                        <div class="med-kpi-icon"><img src="${pageContext.request.contextPath}/assets/images/medico/agenda-indicador.svg" alt="" class="med-icon"></div>
                    </div>
                    <div class="med-kpi-val"><%= Html.texto(enEspera) %></div>
                    <div class="med-kpi-foot-text">La llegada debe estar registrada por admisión.</div>
                </div>
            </div>


            <div class="med-table-header-row">
                <div class="med-table-title">Citas del día &nbsp;·&nbsp; <%= Html.texto(sedeMedico) %></div>
                <div class="med-turno-controls">
                    <label for="medTurno">Turno</label>
                    <select id="medTurno" class="med-turno-select" aria-controls="medCitasDia">
                        <option value="TODOS">Todo el día</option>
                        <option value="MANANA">Mañana · antes de las 12:00</option>
                        <option value="TARDE">Tarde · desde las 12:00</option>
                    </select>
                    <span class="med-table-count" id="medCitasVisibles" role="status"><%= Html.texto(agenda != null ? agenda.size() : 0) %> citas registradas</span>
                </div>
            </div>

            <% if (enEspera == 0 && enAtencion == 0) { %>
            <p class="med-arrival-help">No hay pacientes en sala. Para iniciar una consulta, admisión debe registrar primero la llegada del paciente.</p>
            <% } %>
            <p class="med-table-hint">Desliza la tabla para ver todas las columnas.</p>
            <div class="med-table-card" role="region" aria-label="Citas del médico" tabindex="0">
                <table class="med-table med-table-inicio" id="medCitasDia">
                    <thead>
                        <tr>
                            <th>HORA</th>
                            <th>PACIENTE</th>
                            <th>N° DOCUMENTO</th>
                            <th>MOTIVO DE CONSULTA</th>
                            <th>ESTADO DE ASISTENCIA</th>
                            <th>ACCIÓN</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (agenda != null && !agenda.isEmpty()) {
                            for (Map<String, Object> c : agenda) {
                                String est = (String) c.get("estado");
                                boolean esAtendido = "ATENDIDA".equalsIgnoreCase(est) || "ATENDIDO".equalsIgnoreCase(est);
                                boolean esEnSala = "EN_SALA".equals(est);
                        %>
                            <tr data-turno="<%= Html.texto(String.valueOf(c.get("hora")).compareTo("12:00") < 0 ? "MANANA" : "TARDE") %>">
                                <td>
                                    <div class="td-time-bold"><%= Html.texto(c.get("hora")) %></div>
                                    <% if (pacienteEnSala != null && pacienteEnSala.get("idCita").equals(c.get("idCita"))) { %>
                                        <div class="td-time-badge">Turno actual</div>
                                    <% } %>
                                </td>
                                <td class="td-patient-name"><%= Html.texto(c.get("paciente")) %></td>
                                <td class="td-doc-num"><%= Html.texto(c.get("dni")) %></td>
                                <td class="td-motivo-txt"><%= Html.texto(c.get("motivo") != null && !c.get("motivo").toString().isBlank() ? c.get("motivo") : "Sin registrar") %></td>
                                <td>
                                    <% if (esAtendido) { %>
                                        <span class="med-badge med-badge-atendido">Atendido</span>
                                    <% } else if (esEnSala) { %>
                                        <span class="med-badge <%= Html.texto(Boolean.TRUE.equals(c.get("llegoTarde")) ? "med-badge-tarde" : "med-badge-ensala") %>"><%= Html.texto(Boolean.TRUE.equals(c.get("llegoTarde")) ? "Llegó tarde" : "En sala") %></span>
                                        <div><small>Llegada: <%= Html.texto(c.get("horaLlegada")) %></small></div>
                                    <% } else { %>
                                        <span class="med-badge <%= Html.texto("EN_ATENCION".equals(c.get("estado")) ? "med-badge-atencion" : "med-badge-pendiente") %>"><%= Html.texto("CONFIRMADA".equals(est) || "SOLICITADA".equals(est) ? "Pendiente de llegada" : c.get("estadoTexto")) %></span>
                                    <% } %>
                                </td>
                                <td class="med-action-cell"><a href="${pageContext.request.contextPath}/medico/atencion?idCita=<%= Html.texto(c.get("idCita")) %>" class="med-tbl-btn <%= Html.texto(Boolean.TRUE.equals(c.get("puedeIniciar")) || "EN_ATENCION".equals(est) ? "med-tbl-btn-primary" : "med-tbl-btn-outline") %>"><%= Html.texto("EN_ATENCION".equals(est) ? "Continuar atención" : Boolean.TRUE.equals(c.get("puedeIniciar")) ? "Atender" : "Ver ficha") %></a></td>
                            </tr>
                        <%  }
                           } else { %>
                            <tr>
                                <td colspan="6">
                                    No hay citas registradas en la base de datos para este turno.
                                </td>
                            </tr>
                        <% } %>
                        <tr id="medTurnoVacio" hidden><td colspan="6">No hay citas registradas para el turno seleccionado.</td></tr>
                    </tbody>
                </table>
            </div>
            <nav class="med-pagination" aria-label="Páginas de las citas del día">
                <button type="button" class="med-tbl-btn med-tbl-btn-outline" id="medInicioAnterior">‹ Anterior</button>
                <span id="medInicioPagina" role="status"></span>
                <button type="button" class="med-tbl-btn med-tbl-btn-outline" id="medInicioSiguiente">Siguiente ›</button>
            </nav>

        </main>
    </div>


</body>
</html>
