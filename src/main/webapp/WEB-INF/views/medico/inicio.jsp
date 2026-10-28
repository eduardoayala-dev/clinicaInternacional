<%@ page import="pe.edu.utp.clinica.util.Html" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreMedico = (String) request.getAttribute("nombreMedico");
    if (nombreMedico == null && usuario != null) {
        nombreMedico = "Dr. " + usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreMedico == null) nombreMedico = "Médico";

    String especialidadMedico = (String) request.getAttribute("especialidadMedico");
    if (especialidadMedico == null) especialidadMedico = "Medicina General";

    String sedeMedico = (String) request.getAttribute("sedeMedico");
    if (sedeMedico == null) sedeMedico = "Sin registrar";

    String fechaActual = (String) request.getAttribute("fechaActual");
    if (fechaActual == null) fechaActual = "Jue 15 oct 2026";

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
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:ital,wght@0,600;0,700;0,800;1,600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-medico.css">
    <script src="https://unpkg.com/lucide@latest"></script>
</head>
<body>


    <aside class="med-sidebar">
        <a href="${pageContext.request.contextPath}/medico/inicio" class="med-brand">
            <img src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png" alt="Clínica Internacional" class="med-brand-logo-img" onerror="this.src='${pageContext.request.contextPath}/assets/images/logoClinical.png'">
            <div>
                <div class="med-brand-name">Clínica Internacional</div>
                <div class="med-brand-sub">Portal del médico</div>
            </div>
        </a>

        <nav class="med-nav">
            <a href="${pageContext.request.contextPath}/medico/inicio" class="med-nav-item active">
                <i data-lucide="home"></i> Inicio
            </a>
            <a href="${pageContext.request.contextPath}/medico/agenda" class="med-nav-item">
                <i data-lucide="calendar"></i> Mi agenda
            </a>
            <a href="${pageContext.request.contextPath}/medico/atencion" class="med-nav-item">
                <i data-lucide="stethoscope"></i> Atención de paciente actual
            </a>
        </nav>

        <div class="med-user-footer">
            <a href="${pageContext.request.contextPath}/medico/perfil" class="med-user-info-link">
                <div class="med-avatar"><%= Html.texto(inicialesMedico) %></div>
                <div style="flex: 1; min-width: 0;">
                    <div class="med-user-name"><%= Html.texto(nombreMedico) %></div>
                    <div class="med-user-role"><%= Html.texto(especialidadMedico) %></div>
                </div>
            </a>
            <form method="POST" action="${pageContext.request.contextPath}/logout" style="margin:0;display:inline;">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
<button type="submit" class="med-logout-btn" title="Cerrar sesión" style="border:0;cursor:pointer;font-family:inherit;">
                <i data-lucide="log-out"></i>
            </button>
</form>
        </div>
    </aside>


    <div class="med-main-wrapper">
        <header class="med-topbar">
            <div class="med-breadcrumb">Médico / <span>Inicio</span></div>
        </header>

        <main class="med-content-scroll">

            <% if (pacienteEnSala != null) { %>
                <div class="med-banner-alert">
                    <div class="med-banner-icon">
                        <i data-lucide="users"></i>
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
                    <a href="${pageContext.request.contextPath}/medico/atencion<% if (pacienteEnSala != null) { %>?idCita=<%= Html.texto(pacienteEnSala.get("idCita")) %><% } %>" class="med-btn-outline">
                        <i data-lucide="stethoscope" style="width: 16px; height: 16px;"></i> Gestionar atención actual
                    </a>
                    <a href="${pageContext.request.contextPath}/medico/agenda" class="med-btn-primary">
                        Ir a mi agenda del día <i data-lucide="chevron-right" style="width: 16px; height: 16px;"></i>
                    </a>
                </div>
            </div>


            <div class="med-kpi-grid">

                <div class="med-kpi-card">
                    <div class="med-kpi-top">
                        <span class="med-kpi-label">Citas programadas hoy</span>
                        <div class="med-kpi-icon"><i data-lucide="calendar"></i></div>
                    </div>
                    <div class="med-kpi-val"><%= Html.texto(totalCitas) %> citas</div>
                    <div class="med-kpi-pill-group">
                        <span class="med-pill med-pill-green"><%= Html.texto(atendidos) %> atendido</span>
                        <span class="med-pill med-pill-blue"><%= Html.texto(enEspera) %> en sala</span>
                        <span class="med-pill med-pill-gray"><%= Html.texto(pendientes) %> pendientes</span>
                    </div>
                </div>


                <div class="med-kpi-card">
                    <div class="med-kpi-top">
                        <span class="med-kpi-label">Pacientes pendientes de atención</span>
                        <div class="med-kpi-icon"><i data-lucide="users"></i></div>
                    </div>
                    <div class="med-kpi-val"><%= Html.texto(pendientesRestantes) %> restantes</div>
                    <div class="med-kpi-foot-text">
                        Citas confirmadas, solicitadas o en sala de hoy
                    </div>
                </div>


                <div class="med-kpi-card">
                    <div class="med-kpi-top">
                        <span class="med-kpi-label">Inasistencias registradas</span>
                        <div class="med-kpi-icon"><i data-lucide="calendar"></i></div>
                    </div>
                    <div class="med-kpi-val" style="font-size: 22px;"><%= Html.texto(request.getAttribute("inasistencias")) %></div>
                    <div class="med-kpi-foot-text">Citas marcadas como no asistidas en el día</div>
                </div>
            </div>


            <div class="med-atencion-card">
                <p>Consultas en curso: <strong><%= Html.texto(request.getAttribute("enAtencion")) %></strong> · <a href="${pageContext.request.contextPath}/medico/atencion">Continuar mi atención actual</a></p>
                <p>Espera media hasta iniciar: <strong><%= Html.texto(request.getAttribute("esperaPromedioMinutos") == null ? "Sin datos" : request.getAttribute("esperaPromedioMinutos") + " min") %></strong></p>
                <p>Duración media de consultas cerradas: <strong><%= Html.texto(request.getAttribute("duracionPromedioMinutos") == null ? "Sin datos" : request.getAttribute("duracionPromedioMinutos") + " min") %></strong></p>
            </div>

            <div class="med-table-header-row">
                <div class="med-table-title">Citas del día &nbsp;·&nbsp; <%= Html.texto(sedeMedico) %></div>
                <div class="med-table-count"><%= Html.texto(agenda != null ? agenda.size() : 0) %> pacientes registrados</div>
            </div>

            <div class="med-table-card">
                <table class="med-table">
                    <thead>
                        <tr>
                            <th style="width: 13%;">HORA</th>
                            <th style="width: 25%;">PACIENTE</th>
                            <th style="width: 16%;">N° DOCUMENTO</th>
                            <th style="width: 22%;">MOTIVO DE CONSULTA</th>
                            <th style="width: 14%;">ESTADO DE ASISTENCIA</th>
                            <th style="width: 10%; text-align: right;">ACCIÓN</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (agenda != null && !agenda.isEmpty()) {
                            for (Map<String, Object> c : agenda) {
                                String est = (String) c.get("estado");
                                boolean esAtendido = "ATENDIDA".equalsIgnoreCase(est) || "ATENDIDO".equalsIgnoreCase(est);
                                boolean esEnSala = "EN_SALA".equals(est);
                        %>
                            <tr>
                                <td>
                                    <div class="td-time-bold"><%= Html.texto(c.get("hora")) %></div>
                                    <% if (pacienteEnSala != null && pacienteEnSala.get("idCita").equals(c.get("idCita"))) { %>
                                        <div class="td-time-badge">Turno actual</div>
                                    <% } %>
                                </td>
                                <td class="td-patient-name"><%= Html.texto(c.get("paciente")) %></td>
                                <td class="td-doc-num"><%= Html.texto(c.get("dni")) %></td>
                                <td class="td-motivo-txt"><%= Html.texto(c.get("motivo") != null ? c.get("motivo") : "Control general") %></td>
                                <td>
                                    <% if (esAtendido) { %>
                                        <span class="med-badge med-badge-atendido">Atendido</span>
                                    <% } else if (esEnSala) { %>
                                        <span class="med-badge med-badge-ensala"><%= Html.texto(Boolean.TRUE.equals(c.get("llegoTarde")) ? "Llegó tarde" : "En sala") %></span>
                                        <div><small>Llegada: <%= Html.texto(c.get("horaLlegada")) %></small></div>
                                    <% } else { %>
                                        <span class="med-badge med-badge-pendiente"><%= Html.texto(c.get("estadoTexto")) %></span>
                                    <% } %>
                                </td>
                                <td class="med-action-cell"><a href="${pageContext.request.contextPath}/medico/atencion?idCita=<%= Html.texto(c.get("idCita")) %>" class="med-tbl-btn med-tbl-btn-outline"><%= Html.texto("EN_ATENCION".equals(c.get("estado")) ? "Continuar atención" : "Ver ficha") %></a></td>
                            </tr>
                        <%  }
                           } else { %>
                            <tr>
                                <td colspan="6" style="text-align: center; padding: 40px; color: #6b7c93;">
                                    No hay citas registradas en la base de datos para este turno.
                                </td>
                            </tr>
                        <% } %>
                    </tbody>
                </table>
            </div>


            <div class="med-table-footer-legend">
                <span>Duración estimada por cita: 20 min</span>
                <span>Tolerancia de llegada: 10 min</span>
            </div>
        </main>
    </div>

    <script>
        globalThis.lucide?.createIcons();
    </script>
</body>
</html>
