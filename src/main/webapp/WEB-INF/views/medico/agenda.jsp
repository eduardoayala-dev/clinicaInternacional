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

    String fechaHoyStr = (String) request.getAttribute("fechaHoyStr");
    if (fechaHoyStr == null) fechaHoyStr = "Jue 15/10/2026";

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
    <title>Mi Agenda — Portal del Médico | Clínica Internacional</title>
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
            <div class="med-breadcrumb">Médico / <span>Mi agenda</span></div>
        </header>

        <main class="med-content-scroll">
            <!-- HEADER DE LA PÁGINA -->
            <div class="med-header-row" style="margin-bottom: 20px;">
                <div>
                    <h1 class="med-title">Mi agenda de citas</h1>
                    <div class="med-subtitle">Agenda publicada por administración</div>
                </div>
                <div>
                    <span class="med-tag-published">Agenda publicada</span>
                </div>
            </div>

            <!-- TARJETA DE FILTROS SUPERIORES -->
            <div class="med-filter-card">
                <!-- Filtro: Sede -->
                <div class="med-form-group">
                    <label class="med-form-label">Sede clínica</label>
                    <select class="med-form-select" id="filtroSede">
                        <option value="<%= sedeMedico %>" selected><%= sedeMedico %></option>
                        <% if (sedes != null) {
                            for (Map<String, Object> s : sedes) { 
                                String nom = (String) s.get("nombre");
                                if (!nom.equalsIgnoreCase(sedeMedico)) { %>
                                    <option value="<%= nom %>"><%= nom %></option>
                        <%      }
                            }
                        } %>
                    </select>
                </div>

                <!-- Filtro: Fecha -->
                <div class="med-form-group">
                    <label class="med-form-label">Fecha de consulta</label>
                    <input type="text" class="med-form-input" value="<%= fechaHoyStr %>" readonly style="cursor: pointer; background: #ffffff;">
                </div>

                <!-- Filtro: Buscar paciente -->
                <div class="med-form-group">
                    <label class="med-form-label">Buscar paciente o N° cita</label>
                    <div class="med-search-wrapper">
                        <input type="text" class="med-form-input" id="searchPaciente" placeholder="Nombre, documento o N° de cita">
                        <i data-lucide="search" class="med-search-icon" style="width: 16px; height: 16px;"></i>
                    </div>
                </div>

                <!-- Filtro: Modo de vista -->
                <div class="med-form-group">
                    <label class="med-form-label">Modo de vista</label>
                    <div class="med-view-switcher">
                        <button type="button" class="med-view-btn active">Día</button>
                        <button type="button" class="med-view-btn">Semana</button>
                        <button type="button" class="med-view-btn">Mes</button>
                    </div>
                </div>
            </div>

            <!-- SUBBARRA INFORMATIVA CON MÉTRICAS DEL TURNO -->
            <div class="med-agenda-info-bar">
                <div class="med-agenda-loc">
                    <i data-lucide="map-pin" style="width: 14px; height: 14px; color: #0077c8;"></i>
                    <span><%= sedeMedico %> · Consultorio 102 · Turno mañana</span>
                </div>
                <div class="med-agenda-pills">
                    <span class="med-agenda-pill-item" style="color: #0077c8;">Total asignados: <%= totalAsignados %></span>
                    <span class="med-pill med-pill-green">Atendidos: <%= atendidos %></span>
                    <span class="med-pill med-pill-blue">En sala: <%= enEspera %></span>
                    <span class="med-pill med-pill-gray">Pendientes: <%= pendientes %></span>
                </div>
            </div>

            <!-- TABLA DE MI AGENDA -->
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
                            int idx = 0;
                            for (Map<String, Object> c : agenda) {
                                idx++;
                                String est = (String) c.get("estado");
                                boolean esAtendido = "ATENDIDA".equalsIgnoreCase(est) || "ATENDIDO".equalsIgnoreCase(est);
                                boolean esEnSala = "EN_ESPERA".equalsIgnoreCase(est) || "EN SALA".equalsIgnoreCase(est) || (idx == 2 && !esAtendido);
                                boolean esTarde = "LLEGO_TARDE".equalsIgnoreCase(est) || (idx == 3 && !esAtendido);
                                String origen = (idx % 2 == 0) ? "Reserva web" : "Reasignación <br><small style='color:#7b8e9f;'>desde lista de espera</small>";
                        %>
                            <tr>
                                <td>
                                    <div class="td-time-bold"><%= c.get("hora") %></div>
                                    <% if (esEnSala) { %>
                                        <div class="td-time-badge">En sala</div>
                                    <% } %>
                                </td>
                                <td class="td-patient-name"><%= c.get("paciente") %></td>
                                <td class="td-doc-num"><%= c.get("dni") %></td>
                                <td class="td-motivo-txt"><%= c.get("motivo") != null ? c.get("motivo") : "Control general" %></td>
                                <td><%= origen %></td>
                                <td>
                                    <% if (esAtendido) { %>
                                        <span class="med-badge med-badge-atendido">Atendido</span>
                                    <% } else if (esEnSala) { %>
                                        <span class="med-badge med-badge-ensala">En sala</span>
                                    <% } else if (esTarde) { %>
                                        <span class="med-badge med-badge-tarde">Llegó tarde · <%= c.get("hora") %></span>
                                    <% } else { %>
                                        <span class="med-badge med-badge-pendiente">Pendiente</span>
                                    <% } %>
                                </td>
                                <td class="med-action-cell">
                                    <% if (esAtendido) { %>
                                        <a href="${pageContext.request.contextPath}/medico/atencion?idCita=<%= c.get("idCita") %>" class="med-tbl-btn med-tbl-btn-outline">
                                            Ver
                                        </a>
                                    <% } else if (esEnSala) { %>
                                        <a href="${pageContext.request.contextPath}/medico/atencion?idCita=<%= c.get("idCita") %>" class="med-tbl-btn med-tbl-btn-primary">
                                            Abrir
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
                                <td colspan="7" style="text-align: center; padding: 40px; color: #6b7c93;">
                                    No hay citas registradas en la base de datos para esta fecha y sede.
                                </td>
                            </tr>
                        <% } %>
                    </tbody>
                </table>
            </div>

            <!-- TOOLBAR INFERIOR: EXPORTACIÓN Y PAGINACIÓN -->
            <div class="med-footer-toolbar">
                <div class="med-export-btns">
                    <a href="${pageContext.request.contextPath}/export?tipo=excel&modulo=medico" class="med-btn-export">
                        <i data-lucide="download" style="width: 14px; height: 14px;"></i> Exportar Excel
                    </a>
                    <a href="${pageContext.request.contextPath}/export?tipo=pdf&modulo=medico" class="med-btn-export">
                        <i data-lucide="download" style="width: 14px; height: 14px;"></i> Exportar PDF
                    </a>
                </div>
                <div class="med-pagination">
                    <a href="#" class="med-btn-page">&lsaquo; Anterior</a>
                    <span>Página 1 de 1</span>
                    <a href="#" class="med-btn-page">Siguiente &rsaquo;</a>
                </div>
            </div>
        </main>
    </div>

    <script>
        lucide.createIcons();

        // Búsqueda en vivo de pacientes por nombre o DNI
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
