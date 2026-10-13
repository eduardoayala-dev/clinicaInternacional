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
    if (nombreMedico == null) nombreMedico = "Dr. Carlos Mendoza";

    String especialidadMedico = (String) request.getAttribute("especialidadMedico");
    if (especialidadMedico == null) especialidadMedico = "Medicina General";

    String sedeMedico = (String) request.getAttribute("sedeMedico");
    if (sedeMedico == null) sedeMedico = "Sede San Borja";

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

    String inicialesMedico = "CM";
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
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Portal del Médico — Clínica Internacional</title>
    <!-- FUENTES: Inter para el cuerpo y Playfair Display para el título idéntico al mockup -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:ital,wght@0,600;0,700;0,800;1,600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-medico.css">
    <script src="https://unpkg.com/lucide@latest"></script>
</head>
<body>

    <!-- SIDEBAR DE NAVEGACIÓN -->
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
                <div class="med-avatar"><%= inicialesMedico %></div>
                <div style="flex: 1; min-width: 0;">
                    <div class="med-user-name"><%= nombreMedico %></div>
                    <div class="med-user-role"><%= especialidadMedico %></div>
                </div>
            </a>
            <a href="${pageContext.request.contextPath}/logout" class="med-logout-btn" title="Cerrar sesión">
                <i data-lucide="log-out"></i>
            </a>
        </div>
    </aside>

    <!-- WRAPPER PRINCIPAL CON TOPBAR BREADCRUMB -->
    <div class="med-main-wrapper">
        <header class="med-topbar">
            <div class="med-breadcrumb">Médico / <span>Inicio</span></div>
        </header>

        <main class="med-content-scroll">
            <!-- BANNER SUPERIOR: PACIENTE EN SALA DE ESPERA (CONEXIÓN BD) -->
            <% if (pacienteEnSala != null) { %>
                <div class="med-banner-alert">
                    <div class="med-banner-icon">
                        <i data-lucide="users"></i>
                    </div>
                    <div class="med-banner-text">
                        <div class="med-banner-title">Paciente en sala de espera</div>
                        <div class="med-banner-desc">
                            <strong><%= pacienteEnSala.get("paciente") %></strong> registró su llegada en admisión para su cita a las <strong><%= pacienteEnSala.get("hora") %></strong>.
                        </div>
                    </div>
                </div>
            <% } %>

            <!-- HEADER Y SALUDO (TIPOGRAFÍA SERIF PLAYFAIR DISPLAY) -->
            <div class="med-header-row">
                <div>
                    <h1 class="med-title">Hola, <%= nombreMedico %></h1>
                    <div class="med-subtitle">
                        <%= fechaActual %> &nbsp;·&nbsp; <%= sedeMedico %> &nbsp;·&nbsp; Consultorio 102
                    </div>
                </div>
                <div class="med-header-actions">
                    <a href="${pageContext.request.contextPath}/medico/atencion<% if (pacienteEnSala != null) { %>?idCita=<%= pacienteEnSala.get("idCita") %><% } %>" class="med-btn-outline">
                        <i data-lucide="stethoscope" style="width: 16px; height: 16px;"></i> Gestionar atención actual
                    </a>
                    <a href="${pageContext.request.contextPath}/medico/agenda" class="med-btn-primary">
                        Ir a mi agenda del día <i data-lucide="chevron-right" style="width: 16px; height: 16px;"></i>
                    </a>
                </div>
            </div>

            <!-- TARJETAS KPI -->
            <div class="med-kpi-grid">
                <!-- KPI 1: Citas programadas hoy -->
                <div class="med-kpi-card">
                    <div class="med-kpi-top">
                        <span class="med-kpi-label">Citas programadas hoy</span>
                        <div class="med-kpi-icon"><i data-lucide="calendar"></i></div>
                    </div>
                    <div class="med-kpi-val"><%= totalCitas %> citas</div>
                    <div class="med-kpi-pill-group">
                        <span class="med-pill med-pill-green"><%= atendidos %> atendido</span>
                        <span class="med-pill med-pill-blue"><%= enEspera %> en sala</span>
                        <span class="med-pill med-pill-gray"><%= pendientes %> pendientes</span>
                    </div>
                </div>

                <!-- KPI 2: Pacientes pendientes de atención -->
                <div class="med-kpi-card">
                    <div class="med-kpi-top">
                        <span class="med-kpi-label">Pacientes pendientes de atención</span>
                        <div class="med-kpi-icon"><i data-lucide="users"></i></div>
                    </div>
                    <div class="med-kpi-val"><%= pendientesRestantes %> restantes</div>
                    <div class="med-kpi-foot-text">
                        Turno mañana: <%= pendientesRestantes %> &nbsp;·&nbsp; Turno tarde: 0
                    </div>
                </div>

                <!-- KPI 3: Disponibilidad próxima semana -->
                <div class="med-kpi-card">
                    <div class="med-kpi-top">
                        <span class="med-kpi-label">Disponibilidad próxima semana</span>
                        <div class="med-kpi-icon"><i data-lucide="calendar"></i></div>
                    </div>
                    <div class="med-kpi-val" style="font-size: 22px;">Agenda publicada</div>
                    <div class="med-kpi-foot-text">Lun a vie · turno mañana</div>
                </div>
            </div>

            <!-- TABLA DE PACIENTES DEL TURNO DE LA MAÑANA -->
            <div class="med-table-header-row">
                <div class="med-table-title">Pacientes del turno de la mañana &nbsp;·&nbsp; <%= sedeMedico %></div>
                <div class="med-table-count"><%= agenda != null ? agenda.size() : 0 %> pacientes registrados</div>
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
                            int i = 0;
                            for (Map<String, Object> c : agenda) {
                                i++;
                                String est = (String) c.get("estado");
                                boolean esAtendido = "ATENDIDA".equalsIgnoreCase(est) || "ATENDIDO".equalsIgnoreCase(est);
                                boolean esEnSala = "EN_ESPERA".equalsIgnoreCase(est) || (i == 2 && !esAtendido);
                                boolean esTarde = "LLEGO_TARDE".equalsIgnoreCase(est) || (i == 3 && !esAtendido);
                        %>
                            <tr>
                                <td>
                                    <div class="td-time-bold"><%= c.get("hora") %></div>
                                    <% if (i == 2 || (pacienteEnSala != null && pacienteEnSala.get("idCita").equals(c.get("idCita")))) { %>
                                        <div class="td-time-badge">Turno actual</div>
                                    <% } %>
                                </td>
                                <td class="td-patient-name"><%= c.get("paciente") %></td>
                                <td class="td-doc-num"><%= c.get("dni") %></td>
                                <td class="td-motivo-txt"><%= c.get("motivo") != null ? c.get("motivo") : "Control general" %></td>
                                <td>
                                    <% if (esAtendido) { %>
                                        <span class="med-badge med-badge-atendido">Atendido</span>
                                    <% } else if (esEnSala) { %>
                                        <span class="med-badge med-badge-ensala">En sala</span>
                                    <% } else if (esTarde) { %>
                                        <span class="med-badge med-badge-tarde">Llegó tarde · <%= c.get("hora") %></span>
                                    <% } else { %>
                                        <span class="med-badge med-badge-pendiente">Pendiente de llegada</span>
                                    <% } %>
                                </td>
                                <td class="med-action-cell">
                                    <% if (esAtendido) { %>
                                        <a href="${pageContext.request.contextPath}/medico/atencion?idCita=<%= c.get("idCita") %>" class="med-tbl-btn med-tbl-btn-outline">
                                            Ver ficha
                                        </a>
                                    <% } else if (esEnSala) { %>
                                        <a href="${pageContext.request.contextPath}/medico/atencion?idCita=<%= c.get("idCita") %>" class="med-tbl-btn med-tbl-btn-primary">
                                            Atender ahora
                                        </a>
                                    <% } else if (esTarde) { %>
                                        <a href="${pageContext.request.contextPath}/medico/atencion?idCita=<%= c.get("idCita") %>" class="med-tbl-btn med-tbl-btn-primary">
                                            Atender
                                        </a>
                                        <a href="${pageContext.request.contextPath}/medico/atencion?idCita=<%= c.get("idCita") %>&accion=no_atender" class="med-tbl-btn med-tbl-btn-danger-outline">
                                            No atender
                                        </a>
                                    <% } else { %>
                                        <a href="${pageContext.request.contextPath}/medico/atencion?idCita=<%= c.get("idCita") %>" class="med-tbl-btn med-tbl-btn-outline">
                                            Abrir
                                        </a>
                                    <% } %>
                                </td>
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

            <!-- LEYENDA INFERIOR -->
            <div class="med-table-footer-legend">
                <span>Duración estimada por cita: 20 min</span>
                <span>Tolerancia de llegada: 10 min</span>
            </div>
        </main>
    </div>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
