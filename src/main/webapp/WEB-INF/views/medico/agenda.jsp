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

    String fechaHoyStr = (String) request.getAttribute("fechaHoyStr");
    if (fechaHoyStr == null) fechaHoyStr = "";

    Integer totalAsignados = (Integer) request.getAttribute("totalAsignados");
    if (totalAsignados == null) totalAsignados = 0;

    Integer atendidos = (Integer) request.getAttribute("atendidos");
    if (atendidos == null) atendidos = 0;

    Integer enEspera = (Integer) request.getAttribute("enEspera");
    if (enEspera == null) enEspera = 0;

    Integer pendientes = (Integer) request.getAttribute("pendientes");
    if (pendientes == null) pendientes = 0;

    List<Map<String, Object>> agenda = (List<Map<String, Object>>) request.getAttribute("agenda");
    List<Map<String, Object>> sedes = (List<Map<String, Object>>) request.getAttribute("sedes");

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
    <title>Mi Agenda — Portal del Médico | Clínica Internacional</title>
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
            <a href="${pageContext.request.contextPath}/medico/inicio" class="med-nav-item">
                <i data-lucide="home"></i> Inicio
            </a>
            <a href="${pageContext.request.contextPath}/medico/agenda" class="med-nav-item active">
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
            <div class="med-breadcrumb">Médico / <span>Mi agenda</span></div>
        </header>

        <main class="med-content-scroll">

            <div class="med-header-row" style="margin-bottom: 20px;">
                <div>
                    <h1 class="med-title">Mi agenda de citas</h1>
                    <div class="med-subtitle">Consulta de citas por fecha</div>
                </div>
                <div>
                    <span class="med-tag-published">Agenda médica</span>
                </div>
            </div>


            <div class="med-filter-card">

                <div class="med-form-group">
                    <label class="med-form-label" for="filtroSede">Sede clínica</label>
                    <input class="med-form-input" id="filtroSede" value="<%= Html.texto(sedeMedico) %>" readonly>
                </div>


                <div class="med-form-group">
                    <label class="med-form-label" for="fechaAgenda">Fecha de consulta</label>
                    <form method="GET" action="${pageContext.request.contextPath}/medico/agenda"><input type="date" class="med-form-input" name="fecha" id="fechaAgenda" value="<%= Html.texto(request.getAttribute("fechaSeleccionada")) %>" onchange="this.form.requestSubmit()"><button type="submit" class="med-btn-outline">Consultar</button></form>
                </div>


                <div class="med-form-group">
                    <label class="med-form-label" for="searchPaciente">Buscar paciente o N° cita</label>
                    <div class="med-search-wrapper">
                        <input type="text" class="med-form-input" id="searchPaciente" placeholder="Nombre, documento o N° de cita">
                        <i data-lucide="search" class="med-search-icon" style="width: 16px; height: 16px;"></i>
                    </div>
                </div>


                <div class="med-form-group">
                    <span class="med-form-label">Modo de vista</span>
                    <div class="med-view-switcher"><span class="med-view-btn active">Lista del día seleccionado</span></div>
                </div>
            </div>


            <div class="med-agenda-info-bar">
                <div class="med-agenda-loc">
                    <i data-lucide="map-pin" style="width: 14px; height: 14px; color: #0077c8;"></i>
                    <span><%= Html.texto(sedeMedico) %> · Citas de la fecha seleccionada</span>
                </div>
                <div class="med-agenda-pills">
                    <span class="med-agenda-pill-item" style="color: #0077c8;">Total asignados: <%= Html.texto(totalAsignados) %></span>
                    <span class="med-pill med-pill-green">Atendidos: <%= Html.texto(atendidos) %></span>
                    <span class="med-pill med-pill-blue">En sala: <%= Html.texto(enEspera) %></span>
                    <span class="med-pill med-pill-gray">Pendientes: <%= Html.texto(pendientes) %></span>
                </div>
            </div>


            <div class="med-table-card">
                <table class="med-table" id="tablaAgenda">
                    <thead>
                        <tr>
                            <th style="width: 10%;">HORA</th>
                            <th style="width: 22%;">PACIENTE</th>
                            <th style="width: 14%;">N° DOCUMENTO</th>
                            <th style="width: 18%;">MOTIVO DE CONSULTA</th>
                            <th style="width: 16%;">TIPO / ORIGEN</th>
                            <th style="width: 10%;">ESTADO</th>
                            <th style="width: 10%; text-align: right;">ACCIÓN</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (agenda != null && !agenda.isEmpty()) {
                            for (Map<String, Object> c : agenda) {
                                String est = (String) c.get("estado");
                                boolean esAtendido = "ATENDIDA".equalsIgnoreCase(est) || "ATENDIDO".equalsIgnoreCase(est);
                                boolean esEnSala = "EN_SALA".equals(est);
                                String origen = String.valueOf(c.get("origen"));
                        %>
                            <tr>
                                <td>
                                    <div class="td-time-bold"><%= Html.texto(c.get("hora")) %></div>
                                    <% if (esEnSala) { %>
                                        <div class="td-time-badge">En sala</div>
                                    <% } %>
                                </td>
                                <td class="td-patient-name"><%= Html.texto(c.get("paciente")) %></td>
                                <td class="td-doc-num"><%= Html.texto(c.get("dni")) %></td>
                                <td class="td-motivo-txt"><%= Html.texto(c.get("motivo") != null ? c.get("motivo") : "Control general") %></td>
                                <td><%= Html.texto(origen) %></td>
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
                                <td colspan="7" style="text-align: center; padding: 40px; color: #6b7c93;">
                                    No hay citas registradas en la base de datos para esta fecha y sede.
                                </td>
                            </tr>
                        <% } %>
                    </tbody>
                </table>
            </div>


            <div class="med-footer-toolbar">
                <div class="med-export-btns">
                    <a href="${pageContext.request.contextPath}/medico/export?format=csv&amp;fecha=${fechaSeleccionada}" class="med-btn-export">
                        <i data-lucide="download" style="width: 14px; height: 14px;"></i> CSV para Excel
                    </a>
                    <a href="${pageContext.request.contextPath}/medico/export?format=print&amp;fecha=${fechaSeleccionada}" class="med-btn-export">
                        <i data-lucide="download" style="width: 14px; height: 14px;"></i> Imprimir / PDF
                    </a>
                </div>
                <div class="med-pagination">Se muestran las citas de la fecha seleccionada.</div>
            </div>
        </main>
    </div>

    <script>
        globalThis.lucide?.createIcons();

        document.getElementById('searchPaciente')?.addEventListener('input', function(e) {
            const query = e.target.value.toLowerCase().trim();
            const rows = document.querySelectorAll('#tablaAgenda tbody tr');
            rows.forEach(row => {
                const text = row.innerText.toLowerCase();
                row.style.display = text.includes(query) ? '' : 'none';
            });
        });
    </script>
</body>
</html>
