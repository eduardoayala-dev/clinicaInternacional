<%@ page import="pe.edu.utp.clinica.util.Html" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    String seccionMedico = "agenda";
    String vistaAgenda = String.valueOf(request.getAttribute("vista"));
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreMedico = (String) request.getAttribute("nombreMedico");
    if (nombreMedico == null && usuario != null) {
        nombreMedico = "Dr. " + usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreMedico == null) nombreMedico = "Médico";

    String sedeMedico = (String) request.getAttribute("sedeMedico");
    if (sedeMedico == null) sedeMedico = "Sin registrar";

    Integer totalAsignados = (Integer) request.getAttribute("totalAsignados");
    if (totalAsignados == null) totalAsignados = 0;

    Integer atendidos = (Integer) request.getAttribute("atendidos");
    if (atendidos == null) atendidos = 0;

    Integer enEspera = (Integer) request.getAttribute("enEspera");
    if (enEspera == null) enEspera = 0;

    Integer pendientes = (Integer) request.getAttribute("pendientes");
    if (pendientes == null) pendientes = 0;

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
    <title>Mi Agenda — Portal del Médico | Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Serif+Display&amp;family=Inter:wght@400;500;600;700&amp;display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-medico.css?v=20261028">
    <script defer src="${pageContext.request.contextPath}/assets/js/medico-portal.js?v=20261028"></script>
    <script defer src="${pageContext.request.contextPath}/assets/js/medico-agenda.js?v=20261028"></script>
</head>
<body>


    <%@ include file="/WEB-INF/jspf/medico-sidebar.jspf" %>


    <div class="med-main-wrapper">
        <%@ include file="/WEB-INF/jspf/medico-topbar.jspf" %>

        <main class="med-content-scroll" id="medContenido" tabindex="-1">

            <div class="med-header-row">
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
                    <div class="med-search-wrapper">
                        <select class="med-form-input" id="filtroSede" disabled aria-label="Sede clínica asignada"><option><%= Html.texto(sedeMedico) %></option></select>
                        <img src="${pageContext.request.contextPath}/assets/images/medico/desplegar.svg" alt="" class="med-icon med-field-icon">
                    </div>
                </div>


                <div class="med-form-group">
                    <label class="med-form-label" for="fechaAgenda">Fecha de consulta</label>
                    <form id="medAgendaFiltros" method="GET" class="med-date-form" action="${pageContext.request.contextPath}/medico/agenda">
                        <input type="hidden" name="vista" id="medVista" value="<%= Html.texto(vistaAgenda) %>">
                        <div class="med-input-date-wrap"><input type="date" class="med-form-input" name="fecha" id="fechaAgenda" value="<%= Html.texto(request.getAttribute("fechaSeleccionada")) %>"><img src="${pageContext.request.contextPath}/assets/images/medico/calendario-campo.svg" alt="" class="med-icon med-field-icon"></div>
                        <button type="submit" class="med-btn-outline">Consultar</button>
                    </form>
                </div>


                <div class="med-form-group">
                    <label class="med-form-label" for="searchPaciente">Buscar paciente o N° cita</label>
                    <div class="med-search-wrapper">
                        <input type="text" class="med-form-input" id="searchPaciente" name="q" form="medAgendaFiltros" value="<%= Html.texto(request.getParameter("q")) %>" placeholder="Nombre, documento o N° de cita">
                        <img src="${pageContext.request.contextPath}/assets/images/medico/buscar.svg" alt="" class="med-icon med-search-icon">
                    </div>
                </div>


                <div class="med-form-group">
                    <span class="med-form-label">Modo de vista</span>
                    <div class="med-view-switcher" role="group" aria-label="Período de la agenda">
                        <% for (String modo : List.of("dia", "semana", "mes")) { %>
                        <button type="button" class="med-view-btn <%= Html.texto(modo.equals(vistaAgenda) ? "active" : "") %>" data-vista="<%= Html.texto(modo) %>" aria-pressed="<%= Html.texto(modo.equals(vistaAgenda)) %>"><%= Html.texto("dia".equals(modo) ? "Día" : "semana".equals(modo) ? "Semana" : "Mes") %></button>
                        <% } %>
                    </div>
                </div>
            </div>


            <div class="med-agenda-info-bar">
                <div class="med-agenda-loc">
                    <img src="${pageContext.request.contextPath}/assets/images/medico/sede.svg" alt="" class="med-icon">
                    <span><%= Html.texto(sedeMedico) %> · <%= Html.texto(request.getAttribute("periodoDesde")) %> al <%= Html.texto(request.getAttribute("periodoHasta")) %></span>
                </div>
                <div class="med-agenda-pills">
                    <span class="med-agenda-pill-item">Total asignados: <%= Html.texto(totalAsignados) %></span>
                    <span class="med-pill med-pill-green">Atendidos: <%= Html.texto(atendidos) %></span>
                    <span class="med-pill med-pill-blue">En sala: <%= Html.texto(enEspera) %></span>
                    <span class="med-pill med-pill-gray">Pendientes: <%= Html.texto(pendientes) %></span>
                </div>
            </div>


            <section id="medCalendario" class="med-calendar" hidden aria-label="Calendario de citas"
                data-vista="<%= Html.texto(vistaAgenda) %>" data-desde="<%= Html.texto(request.getAttribute("periodoDesde")) %>" data-hasta="<%= Html.texto(request.getAttribute("periodoHasta")) %>"
                data-hoy="<%= Html.texto(java.time.LocalDate.now(java.time.ZoneId.of("America/Lima"))) %>">
                <div class="med-calendar-toolbar">
                    <h2 id="medCalendarioTitulo" class="med-table-title"></h2>
                    <div class="med-calendar-navigation" role="group" aria-label="Navegar por períodos">
                        <button type="button" class="med-tbl-btn med-tbl-btn-outline" id="medPeriodoAnterior">‹ Anterior</button>
                        <button type="button" class="med-tbl-btn med-tbl-btn-outline" id="medPeriodoHoy">Hoy</button>
                        <button type="button" class="med-tbl-btn med-tbl-btn-outline" id="medPeriodoSiguiente">Siguiente ›</button>
                    </div>
                </div>
                <p id="medCalendarioEstado" class="med-calendar-status" role="status"></p>
                <div id="medCalendarioDias" class="med-calendar-grid"></div>
            </section>
            <p class="med-table-hint" id="medAgendaTableHint">Desliza la tabla para ver todas las columnas.</p>
            <div class="med-table-card" id="medAgendaTablaRegion" role="region" aria-label="Citas del médico" tabindex="0">
                <table class="med-table med-table-agenda" id="tablaAgenda">
                    <thead>
                        <tr>
                            <th><%= Html.texto("dia".equals(vistaAgenda) ? "HORA" : "FECHA / HORA") %></th>
                            <th>PACIENTE</th>
                            <th>N° DOCUMENTO</th>
                            <th>MOTIVO DE CONSULTA</th>
                            <th>TIPO / ORIGEN</th>
                            <th>ESTADO</th>
                            <th>ACCIÓN</th>
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
                            <tr data-cita="<%= Html.texto(c.get("idCita")) %>" data-fecha="<%= Html.texto(c.get("fecha")) %>" data-hora="<%= Html.texto(c.get("hora")) %>" data-paciente="<%= Html.texto(c.get("paciente")) %>" data-estado="<%= Html.texto(c.get("estadoTexto")) %>">
                                <td>
                                    <% if (!"dia".equals(vistaAgenda)) { %><div><%= Html.texto(c.get("fecha")) %></div><% } %>
                                    <div class="td-time-bold"><%= Html.texto(c.get("hora")) %></div>
                                    <% if (esEnSala) { %>
                                        <div class="td-time-badge">En sala</div>
                                    <% } %>
                                </td>
                                <td class="td-patient-name"><%= Html.texto(c.get("paciente")) %></td>
                                <td class="td-doc-num"><%= Html.texto(c.get("dni")) %></td>
                                <td class="td-motivo-txt"><%= Html.texto(c.get("motivo") != null && !c.get("motivo").toString().isBlank() ? c.get("motivo") : "Sin registrar") %></td>
                                <td><%= Html.texto(origen) %></td>
                                <td>
                                    <% if (esAtendido) { %>
                                        <span class="med-badge med-badge-atendido">Atendido</span>
                                    <% } else if (esEnSala) { %>
                                        <span class="med-badge <%= Html.texto(Boolean.TRUE.equals(c.get("llegoTarde")) ? "med-badge-tarde" : "med-badge-ensala") %>"><%= Html.texto(Boolean.TRUE.equals(c.get("llegoTarde")) ? "Llegó tarde" : "En sala") %></span>
                                        <div><small>Llegada: <%= Html.texto(c.get("horaLlegada")) %></small></div>
                                    <% } else { %>
                                        <span class="med-badge <%= Html.texto("EN_ATENCION".equals(c.get("estado")) ? "med-badge-atencion" : "med-badge-pendiente") %>"><%= Html.texto(c.get("estadoTexto")) %></span>
                                    <% } %>
                                </td>
                                <td class="med-action-cell"><a href="${pageContext.request.contextPath}/medico/atencion?idCita=<%= Html.texto(c.get("idCita")) %>" class="med-tbl-btn <%= Html.texto(Boolean.TRUE.equals(c.get("puedeIniciar")) || "EN_ATENCION".equals(est) ? "med-tbl-btn-primary" : "med-tbl-btn-outline") %>"><%= Html.texto("EN_ATENCION".equals(est) ? "Continuar atención" : Boolean.TRUE.equals(c.get("puedeIniciar")) ? "Atender" : "Ver ficha") %></a></td>
                            </tr>
                        <%  }
                           } else { %>
                            <tr>
                                <td colspan="7">
                                    No hay citas registradas en la base de datos para esta fecha y sede.
                                </td>
                            </tr>
                        <% } %>
                        <tr id="medAgendaSinResultados" hidden><td colspan="7">No hay citas que coincidan con la búsqueda.</td></tr>
                    </tbody>
                </table>
            </div>


            <div class="med-footer-toolbar">
                <div class="med-export-btns">
                    <a href="${pageContext.request.contextPath}/medico/export?format=csv&amp;fecha=${fechaSeleccionada}&amp;vista=<%= Html.texto(vistaAgenda) %>" class="med-btn-export">
                        <img src="${pageContext.request.contextPath}/assets/images/medico/descargar.svg" alt="" class="med-icon"> CSV para Excel
                    </a>
                    <a href="${pageContext.request.contextPath}/medico/export?format=print&amp;fecha=${fechaSeleccionada}&amp;vista=<%= Html.texto(vistaAgenda) %>" class="med-btn-export">
                        <img src="${pageContext.request.contextPath}/assets/images/medico/descargar.svg" alt="" class="med-icon"> Imprimir / PDF
                    </a>
                </div>
                <nav class="med-pagination" aria-label="Páginas de la agenda">
                    <button type="button" class="med-tbl-btn med-tbl-btn-outline" id="medPaginaAnterior">‹ Anterior</button>
                    <span id="medPaginaEstado" role="status"></span>
                    <button type="button" class="med-tbl-btn med-tbl-btn-outline" id="medPaginaSiguiente">Siguiente ›</button>
                </nav>
            </div>
        </main>
    </div>


</body>
</html>
