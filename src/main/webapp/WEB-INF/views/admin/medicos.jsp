<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombrePersonal = (String) request.getAttribute("nombrePersonal");
    if (nombrePersonal == null && usuario != null) {
        nombrePersonal = usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombrePersonal == null) nombrePersonal = "Lucía Ramírez";

    String inicialesPersonal = "LR";
    if (nombrePersonal != null && !nombrePersonal.trim().isEmpty()) {
        String cleanNom = nombrePersonal.replaceAll("(?i)\\b(Lic\\.|Ing\\.|Sr\\.|Sra\\.)\\b", "").trim();
        String[] partes = cleanNom.split("\\s+");
        if (partes.length >= 2) {
            inicialesPersonal = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else if (partes.length == 1 && !partes[0].isEmpty()) {
            inicialesPersonal = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }

    List<Map<String, Object>> listaMedicos = (List<Map<String, Object>>) request.getAttribute("listaMedicos");
    List<Map<String, Object>> sedes = (List<Map<String, Object>>) request.getAttribute("sedes");
    List<Map<String, Object>> especialidades = (List<Map<String, Object>>) request.getAttribute("especialidades");
    List<Map<String, Object>> listaDisponibilidades = (List<Map<String, Object>>) request.getAttribute("listaDisponibilidades");

    Integer totalMedicos = (Integer) request.getAttribute("totalMedicos");
    if (totalMedicos == null) totalMedicos = (listaMedicos != null ? listaMedicos.size() : 84);

    Integer totalActivos = (Integer) request.getAttribute("totalActivos");
    if (totalActivos == null) totalActivos = 79;

    Integer totalEspecialidades = (Integer) request.getAttribute("totalEspecialidades");
    if (totalEspecialidades == null) totalEspecialidades = (especialidades != null ? especialidades.size() : 18);

    String filtroQuery = (String) request.getAttribute("filtroQuery");
    if (filtroQuery == null) filtroQuery = "";

    String filtroEspecialidad = (String) request.getAttribute("filtroEspecialidad");
    if (filtroEspecialidad == null) filtroEspecialidad = "";

    String filtroSede = (String) request.getAttribute("filtroSede");
    if (filtroSede == null) filtroSede = "";

    String filtroEstado = (String) request.getAttribute("filtroEstado");
    if (filtroEstado == null) filtroEstado = "ACTIVO";

    String flashMessage = (String) session.getAttribute("flashMessage");
    if (flashMessage != null) {
        session.removeAttribute("flashMessage");
    }

    int medicosCount = listaMedicos != null ? listaMedicos.size() : 0;
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Médicos y Especialidades — Clínica Internacional</title>
    <!-- FUENTES: Inter para interfaz y Playfair Display para títulos institucionales -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:ital,wght@0,600;0,700;0,800;1,600&display=swap" rel="stylesheet">
    <!-- ICONOS LUCIDE -->
    <script src="https://unpkg.com/lucide@latest"></script>
    <!-- ESTILOS INSTITUCIONALES DEL PORTAL ADMINISTRATIVO -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-administrativo.css">
</head>
<body>

    <!-- PANEL LATERAL INSTITUCIONAL (UNIFICADO) -->
    <aside class="adm-sidebar">
        <a href="${pageContext.request.contextPath}/admin/dashboard" class="adm-brand">
            <div class="adm-brand-logo-wrap">
                <img src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png" alt="Clínica Internacional" class="adm-brand-logo-img" onerror="this.src='${pageContext.request.contextPath}/assets/images/logoClinical.png'">
            </div>
            <div>
                <div class="adm-brand-name">Clínica Internacional</div>
                <div class="adm-brand-sub">Portal administrativo</div>
            </div>
        </a>

        <nav class="adm-nav">
            <a href="${pageContext.request.contextPath}/admin/dashboard" class="adm-nav-item">
                <i data-lucide="home" style="width: 18px; height: 18px; min-width: 18px; flex-shrink: 0;"></i> <span>Inicio</span>
            </a>
            <a href="${pageContext.request.contextPath}/admin/medicos" class="adm-nav-item active">
                <i data-lucide="stethoscope" style="width: 18px; height: 18px; min-width: 18px; flex-shrink: 0;"></i> <span>Médicos y especialidades</span>
            </a>
            <a href="${pageContext.request.contextPath}/admin/pacientes" class="adm-nav-item">
                <i data-lucide="users" style="width: 18px; height: 18px; min-width: 18px; flex-shrink: 0;"></i> <span>Pacientes</span>
            </a>
            <a href="${pageContext.request.contextPath}/admin/reasignaciones" class="adm-nav-item">
                <i data-lucide="refresh-cw" style="width: 18px; height: 18px; min-width: 18px; flex-shrink: 0;"></i> <span>Reasignaciones y excepciones</span>
            </a>
            <a href="${pageContext.request.contextPath}/admin/dashboard-operativo" class="adm-nav-item">
                <i data-lucide="trending-up" style="width: 18px; height: 18px; min-width: 18px; flex-shrink: 0;"></i> <span>Dashboard operativo</span>
            </a>
        </nav>

        <div class="adm-user-footer">
            <div class="adm-user-info-link">
                <div class="adm-avatar"><%= inicialesPersonal %></div>
                <div class="adm-user-text">
                    <div class="adm-user-name" title="<%= nombrePersonal %>"><%= nombrePersonal %></div>
                    <div class="adm-user-role">Personal administrativo</div>
                </div>
            </div>
            <form method="POST" action="${pageContext.request.contextPath}/logout" style="margin:0;display:inline;">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
<button type="submit" class="adm-logout-btn" title="Cerrar sesión" style="border:0;cursor:pointer;font-family:inherit;">
                <i data-lucide="log-out" style="width: 17px; height: 17px; min-width: 17px; flex-shrink: 0;"></i>
            </button>
</form>
        </div>
    </aside>

    <!-- CONTENEDOR PRINCIPAL -->
    <div class="adm-main-wrapper">
        <!-- TOPBAR CON BREADCRUMB -->
        <header class="adm-topbar">
            <div class="adm-breadcrumb">
                Personal administrativo &nbsp;/&nbsp; <span>Médicos y especialidades</span>
            </div>
        </header>

        <!-- ÁREA CON SCROLL -->
        <main class="adm-content-scroll">

            <!-- MENSAJE DE ÉXITO FLASH -->
            <% if (flashMessage != null) { %>
                <div style="background: #dcfce7; border: 1px solid #86efac; color: #166534; padding: 12px 18px; border-radius: 8px; margin-bottom: 20px; font-size: 13.5px; font-weight: 600; display: flex; align-items: center; gap: 10px;">
                    <i data-lucide="check-circle" style="width: 18px; height: 18px; color: #16a34a;"></i>
                    <%= flashMessage %>
                </div>
            <% } %>

            <c:if test="${not empty sessionScope.flashError}">
                <p role="alert" style="padding: 14px; color: #991b1b; background: #fef2f2; border-radius: 10px;">
                    <c:out value="${sessionScope.flashError}"/>
                </p>
                <c:remove var="flashError" scope="session"/>
            </c:if>

            <!-- ENCABEZADO -->
            <div class="adm-header-block">
                <h1 class="adm-page-title">Médicos y especialidades</h1>
                <div class="adm-page-subtitle">Padrón de médicos, horarios y motivos de consulta</div>
            </div>

            <!-- TABS DE NAVEGACIÓN SECUNDARIA -->
            <div class="adm-tabs-row">
                <a href="${pageContext.request.contextPath}/admin/medicos" class="adm-tab-item active">Médicos</a>
                <a href="${pageContext.request.contextPath}/admin/medicos?tab=horarios" class="adm-tab-item">Horarios</a>
                <a href="${pageContext.request.contextPath}/admin/medicos?tab=motivos" class="adm-tab-item">Motivos de consulta</a>
            </div>

            <!-- FILTROS DE BÚSQUEDA OPERATIVA -->
            <form method="GET" action="${pageContext.request.contextPath}/admin/medicos" id="formFiltros" class="adm-filters-card">
                <!-- Búsqueda por texto -->
                <div class="adm-search-input-wrap">
                    <label class="adm-filter-label" for="inputBuscar">Buscar</label>
                    <div style="position: relative;">
                        <input type="text" id="inputBuscar" name="query" class="adm-search-input" placeholder="Nombre o colegiatura (CMP)" value="<%= filtroQuery %>">
                        <i data-lucide="search" class="adm-search-icon"></i>
                    </div>
                </div>

                <!-- Filtro Especialidad -->
                <div class="adm-select-wrap">
                    <label class="adm-filter-label" for="selectEspecialidad">Especialidad</label>
                    <select id="selectEspecialidad" name="especialidad" class="adm-select" onchange="document.getElementById('formFiltros').submit()">
                        <option value="">Todas</option>
                        <% if (especialidades != null) { 
                            for (Map<String, Object> esp : especialidades) {
                                String nEsp = (String) esp.get("nombre");
                                boolean sel = nEsp != null && nEsp.equalsIgnoreCase(filtroEspecialidad);
                        %>
                            <option value="<%= nEsp %>" <%= sel ? "selected" : "" %>><%= nEsp %></option>
                        <%  } 
                           } %>
                    </select>
                </div>

                <!-- Filtro Sede -->
                <div class="adm-select-wrap">
                    <label class="adm-filter-label" for="selectSede">Sede</label>
                    <select id="selectSede" name="sede" class="adm-select" onchange="document.getElementById('formFiltros').submit()">
                        <option value="">Todas las sedes</option>
                        <% if (sedes != null) { 
                            for (Map<String, Object> s : sedes) {
                                String nSede = (String) s.get("nombre");
                                boolean sel = nSede != null && nSede.equalsIgnoreCase(filtroSede);
                        %>
                            <option value="<%= nSede %>" <%= sel ? "selected" : "" %>><%= nSede %></option>
                        <%  } 
                           } %>
                    </select>
                </div>

                <!-- Filtro Estado -->
                <div class="adm-select-wrap">
                    <label class="adm-filter-label" for="selectEstado">Estado</label>
                    <select id="selectEstado" name="estado" class="adm-select" onchange="document.getElementById('formFiltros').submit()">
                        <option value="ACTIVO" <%= "ACTIVO".equalsIgnoreCase(filtroEstado) ? "selected" : "" %>>Activo</option>
                        <option value="INACTIVO" <%= "INACTIVO".equalsIgnoreCase(filtroEstado) ? "selected" : "" %>>Inactivo</option>
                        <option value="Todos" <%= "Todos".equalsIgnoreCase(filtroEstado) ? "selected" : "" %>>Todos</option>
                    </select>
                </div>
            </form>

            <!-- BARRA DE ACCIONES (NUEVO MÉDICO / EXPORTAR EXCEL / REINICIAR) -->
            <div class="adm-actions-bar">
                <div class="adm-actions-left">
                    <button type="button" class="adm-btn-primary" onclick="abrirModalNuevoMedico()">
                        <i data-lucide="plus" style="width: 16px; height: 16px;"></i> Nuevo médico
                    </button>
                    <button type="button" class="adm-btn-outline" onclick="exportarExcel()">
                        <i data-lucide="download" style="width: 16px; height: 16px;"></i> Exportar Excel
                    </button>
                </div>
                <div>
                    <a href="${pageContext.request.contextPath}/admin/medicos" class="adm-btn-reset">
                        Reiniciar filtros
                    </a>
                </div>
            </div>

            <!-- TABLA DEL PADRÓN DE MÉDICOS -->
            <div class="adm-table-card">
                <table class="adm-table">
                    <thead>
                        <tr>
                            <th style="width: 10%;">CMP</th>
                            <th style="width: 22%;">NOMBRE Y APELLIDOS</th>
                            <th style="width: 18%;">ESPECIALIDAD(ES)</th>
                            <th style="width: 18%;">SEDE(S) ASIGNADA(S)</th>
                            <th style="width: 10%;">HORAS / SEMANA</th>
                            <th style="width: 8%;">ESTADO</th>
                            <th style="width: 14%; text-align: right;">ACCIONES</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (listaMedicos != null && !listaMedicos.isEmpty()) { 
                            for (Map<String, Object> m : listaMedicos) {
                                Object idMedObj = m.get("idMedico");
                                int idMedico = idMedObj != null ? ((Number) idMedObj).intValue() : 1;
                                String cmp = (String) m.get("cmp");
                                String dni = (String) m.get("dni");
                                String nombre = (String) m.get("nombre");
                                String especialidad = (String) m.get("especialidad");
                                String sede = (String) m.get("sede");
                                String horas = (String) m.get("horas");
                                String est = (String) m.get("estado");
                                boolean esActivo = est == null || "ACTIVO".equalsIgnoreCase(est);
                        %>
                            <tr>
                                <td class="adm-td-cmp"><%= cmp %></td>
                                <td>
                                    <div class="adm-td-name"><%= nombre %></div>
                                    <div class="adm-td-sub">DNI <%= dni %></div>
                                </td>
                                <td><%= especialidad %></td>
                                <td><%= sede %></td>
                                <td><%= horas != null ? horas : "30 h" %></td>
                                <td>
                                    <% if (esActivo) { %>
                                        <span class="adm-badge-status activo">Activo</span>
                                    <% } else { %>
                                        <span class="adm-badge-status inactivo">Inactivo</span>
                                    <% } %>
                                </td>
                                <td style="text-align: right; white-space: nowrap;">
                                    <button type="button" class="adm-btn-action-turnos" onclick="abrirModalTurnos('<%= idMedico %>', '<%= nombre != null ? nombre.replace("'", "\\'") : "" %>', '<%= cmp %>', '<%= especialidad != null ? especialidad.replace("'", "\\'") : "" %>', '<%= sede != null ? sede.replace("'", "\\'") : "" %>')">
                                        <i data-lucide="calendar" style="width: 13px; height: 13px;"></i> Turnos
                                    </button>
                                    <a href="javascript:void(0)" class="adm-btn-action-edit" onclick="editarMedico('<%= cmp %>', '<%= nombre != null ? nombre.replace("'", "\\'") : "" %>')">
                                        Editar
                                    </a>
                                </td>
                            </tr>
                        <%  } 
                           } else { %>
                            <tr>
                                <td colspan="7" style="text-align: center; padding: 40px; color: #64748b;">
                                    No se encontraron médicos con los filtros seleccionados.
                                </td>
                            </tr>
                        <% } %>
                    </tbody>
                </table>

                <!-- PIE DE TABLA / PAGINACIÓN -->
                <div class="adm-table-footer">
                    <div>
                        Mostrando <%= medicosCount %> de <%= totalMedicos %> médicos
                    </div>
                    <div class="adm-pagination-controls">
                        <button type="button" class="adm-btn-page" disabled>‹ Anterior</button>
                        <span>Página 1 de <%= Math.max(1, (int) Math.ceil((double) totalMedicos / 5)) %></span>
                        <button type="button" class="adm-btn-page">Siguiente ›</button>
                    </div>
                </div>
            </div>

            <!-- TARJETAS KPI INFERIORES -->
            <div class="adm-kpi-grid">
                <!-- Tarjeta 1: Total en padrón -->
                <div class="adm-kpi-card">
                    <div>
                        <div class="adm-kpi-label">Total en padrón</div>
                        <div class="adm-kpi-num"><%= totalMedicos %></div>
                        <div class="adm-kpi-sub">Médicos registrados</div>
                    </div>
                    <div class="adm-kpi-icon-wrap">
                        <i data-lucide="users"></i>
                    </div>
                </div>

                <!-- Tarjeta 2: Total activos -->
                <div class="adm-kpi-card">
                    <div>
                        <div class="adm-kpi-label">Total activos</div>
                        <div class="adm-kpi-num"><%= totalActivos %></div>
                        <div class="adm-kpi-sub">
                            <%= totalMedicos > 0 ? Math.round(((double) totalActivos / totalMedicos) * 100) : 94 %> % del padrón
                        </div>
                    </div>
                    <div class="adm-kpi-icon-wrap" style="color: #16a34a; background: #dcfce7;">
                        <i data-lucide="check"></i>
                    </div>
                </div>

                <!-- Tarjeta 3: Especialidades homologadas -->
                <div class="adm-kpi-card">
                    <div>
                        <div class="adm-kpi-label">Especialidades homologadas</div>
                        <div class="adm-kpi-num"><%= totalEspecialidades %></div>
                        <div class="adm-kpi-sub">En 4 sedes de la red</div>
                    </div>
                    <div class="adm-kpi-icon-wrap">
                        <i data-lucide="stethoscope"></i>
                    </div>
                </div>
            </div>

        </main>
    </div>

    <!-- MODAL DE REGISTRO: NUEVO MÉDICO -->
    <div class="adm-modal-overlay" id="modalNuevoMedico">
        <div class="adm-modal-box">
            <div class="adm-modal-head">
                <div class="adm-modal-title">Registrar Nuevo Médico en el Padrón</div>
                <button type="button" class="adm-modal-close" onclick="cerrarModalNuevoMedico()">&times;</button>
            </div>

            <form action="${pageContext.request.contextPath}/admin/medicos" method="POST">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="registrarMedico">

                <div class="adm-form-group">
                    <label>Nombres Completos</label>
                    <input type="text" name="nombres" class="adm-form-input" required placeholder="Ej. Carlos Eduardo">
                </div>

                <div class="adm-form-group">
                    <label>Apellidos Completos</label>
                    <input type="text" name="apellidos" class="adm-form-input" required placeholder="Ej. Mendoza Morales">
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                    <div class="adm-form-group">
                        <label>Colegiatura (CMP)</label>
                        <input type="text" name="cmp" class="adm-form-input" required placeholder="045821">
                    </div>
                    <div class="adm-form-group">
                        <label>DNI</label>
                        <input type="text" name="dni" class="adm-form-input" required placeholder="09452178">
                    </div>
                </div>

                <div class="adm-form-group">
                    <label>Correo Electrónico Institucional</label>
                    <input type="email" name="email" class="adm-form-input" required placeholder="cmendoza@clinicainternacional.com.pe">
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                    <div class="adm-form-group">
                        <label>Especialidad</label>
                        <select name="idEspecialidad" class="adm-form-select" required>
                            <% if (especialidades != null) { 
                                for (Map<String, Object> esp : especialidades) { %>
                                    <option value="<%= esp.get("id") %>"><%= esp.get("nombre") %></option>
                            <%  } 
                               } %>
                        </select>
                    </div>
                    <div class="adm-form-group">
                        <label>Sede Principal</label>
                        <select name="idSede" class="adm-form-select" required>
                            <% if (sedes != null) { 
                                for (Map<String, Object> s : sedes) { %>
                                    <option value="<%= s.get("id") %>"><%= s.get("nombre") %></option>
                            <%  } 
                               } %>
                        </select>
                    </div>
                </div>

                <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 24px;">
                    <button type="button" class="adm-btn-outline" onclick="cerrarModalNuevoMedico()">Cancelar</button>
                    <button type="submit" class="adm-btn-primary">Guardar en Padrón</button>
                </div>
            </form>
        </div>
    </div>

    <!-- MODAL DE GESTIÓN: TURNOS Y DISPONIBILIDAD -->
    <div class="adm-modal-overlay" id="modalGestionarTurnos" style="display: none;">
        <div class="adm-modal-box" style="max-width: 780px; width: 95%; max-height: 92vh; overflow-y: auto; padding: 26px;">
            <div class="adm-modal-head" style="margin-bottom: 14px;">
                <div>
                    <div class="adm-modal-title" style="display: flex; align-items: center; gap: 8px;">
                        <i data-lucide="calendar-clock" style="color: #0077c8; width: 22px; height: 22px;"></i>
                        Gestionar Turnos y Disponibilidad
                    </div>
                    <div style="font-size: 13px; color: #64748b; margin-top: 4px;">
                        Configure los bloques de atención presencial y habilitación de cupos para citas médicas.
                    </div>
                </div>
                <button type="button" class="adm-modal-close" onclick="cerrarModalTurnos()">&times;</button>
            </div>

            <!-- Resumen del Médico Seleccionado -->
            <div style="background: #f0f7ff; border: 1px solid #bae0fd; border-radius: 8px; padding: 12px 16px; margin-bottom: 20px; display: flex; align-items: center; justify-content: space-between;">
                <div>
                    <div style="font-size: 14.5px; font-weight: 700; color: #0369a1;" id="turnoDocNombre">Dr. Carlos Mendoza</div>
                    <div style="font-size: 12.5px; color: #0284c7; margin-top: 2px;">
                        CMP: <span id="turnoDocCmp" style="font-weight: 600;">045821</span> &nbsp;•&nbsp; <span id="turnoDocEsp">Cardiología · Sede San Borja</span>
                    </div>
                </div>
                <span style="background: #e0f2fe; color: #0284c7; font-size: 11.5px; font-weight: 700; padding: 4px 10px; border-radius: 6px;">
                    Padrón Activo
                </span>
            </div>

            <!-- Listado de Bloques de Turnos Existentes -->
            <div style="margin-bottom: 22px;">
                <div style="font-size: 13.5px; font-weight: 700; color: #1e293b; margin-bottom: 10px; display: flex; align-items: center; gap: 6px;">
                    <i data-lucide="layers" style="width: 15px; height: 15px; color: #475569;"></i>
                    Turnos y Cupos Configurados
                </div>

                <div id="tableWrapTurnos" style="border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden;">
                    <table style="width: 100%; border-collapse: collapse; font-size: 12.5px; text-align: left;">
                        <thead style="background: #f8fafc; border-bottom: 1px solid #e2e8f0; color: #475569; font-weight: 700;">
                            <tr>
                                <th style="padding: 10px;">FECHA</th>
                                <th style="padding: 10px;">HORARIO (BLOQUES)</th>
                                <th style="padding: 10px;">SEDE</th>
                                <th style="padding: 10px; text-align: center;">CITAS AGENDADAS</th>
                                <th style="padding: 10px; text-align: center;">ESTADO</th>
                                <th style="padding: 10px; text-align: right;">ACCIÓN</th>
                            </tr>
                        </thead>
                        <tbody id="tbodyTurnosMedico">
                            <!-- Inyectado por JS -->
                        </tbody>
                    </table>
                </div>

                <div id="emptyTurnosMensaje" style="display: none; padding: 24px; text-align: center; background: #fafafa; border: 1px dashed #cbd5e1; border-radius: 8px; color: #64748b; font-size: 13px;">
                    <i data-lucide="calendar-x" style="width: 28px; height: 28px; margin: 0 auto 8px auto; color: #94a3b8; display: block;"></i>
                    Este médico no registra turnos de atención para los próximos días.<br>
                    Habilite un bloque horario con el formulario inferior para permitir agendar citas.
                </div>
            </div>

            <!-- Formulario de Publicación de Nuevo Turno -->
            <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 18px;">
                <div style="font-size: 13.5px; font-weight: 700; color: #0f172a; margin-bottom: 12px; display: flex; align-items: center; gap: 8px;">
                    <i data-lucide="calendar-plus" style="width: 16px; height: 16px; color: #0077c8;"></i>
                    Publicar Nuevo Bloque de Horarios
                </div>

                <form action="${pageContext.request.contextPath}/admin/medicos" method="POST">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="publicar_horario_medico">
                    <input type="hidden" id="formTurnoIdMedico" name="idMedico" value="">

                    <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; margin-bottom: 12px;">
                        <div class="adm-form-group" style="margin-bottom: 0;">
                            <label style="font-size: 12px; font-weight: 600; color: #334155; margin-bottom: 4px; display: block;">Fecha de Atención</label>
                            <input type="date" id="formTurnoFecha" name="fecha" class="adm-form-input" required style="font-size: 13px; padding: 8px 10px;">
                        </div>
                        <div class="adm-form-group" style="margin-bottom: 0;">
                            <label style="font-size: 12px; font-weight: 600; color: #334155; margin-bottom: 4px; display: block;">Hora Inicio</label>
                            <input type="time" name="horaInicio" value="08:00" class="adm-form-input" required style="font-size: 13px; padding: 8px 10px;">
                        </div>
                        <div class="adm-form-group" style="margin-bottom: 0;">
                            <label style="font-size: 12px; font-weight: 600; color: #334155; margin-bottom: 4px; display: block;">Hora Fin</label>
                            <input type="time" name="horaFin" value="13:00" class="adm-form-input" required style="font-size: 13px; padding: 8px 10px;">
                        </div>
                    </div>

                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px;">
                        <div class="adm-form-group" style="margin-bottom: 0;">
                            <label style="font-size: 12px; font-weight: 600; color: #334155; margin-bottom: 4px; display: block;">Sede Hospitalaria</label>
                            <select id="formTurnoSede" name="idSede" class="adm-form-select" required style="font-size: 13px; padding: 8px 10px;">
                                <% if (sedes != null) { 
                                    for (Map<String, Object> s : sedes) { %>
                                        <option value="<%= s.get("id") %>"><%= s.get("nombre") %></option>
                                <%  } 
                                   } %>
                            </select>
                        </div>
                        <div class="adm-form-group" style="margin-bottom: 0;">
                            <label style="font-size: 12px; font-weight: 600; color: #334155; margin-bottom: 4px; display: block;">Duración por Paciente</label>
                            <select name="duracion" class="adm-form-select" style="font-size: 13px; padding: 8px 10px;">
                                <option value="20">20 minutos (Alta rotación)</option>
                                <option value="30" selected>30 minutos (Estándar)</option>
                                <option value="45">45 minutos (Especializada)</option>
                                <option value="60">60 minutos (Procedimiento)</option>
                            </select>
                        </div>
                    </div>

                    <div style="display: flex; justify-content: flex-end; gap: 10px;">
                        <button type="button" class="adm-btn-outline" onclick="cerrarModalTurnos()" style="font-size: 13px; padding: 8px 16px;">Cerrar</button>
                        <button type="submit" class="adm-btn-primary" style="font-size: 13px; padding: 8px 18px;">
                            <i data-lucide="check-circle" style="width: 15px; height: 15px;"></i> Publicar Bloque de Turnos
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- SCRIPTS -->
    <script>
        lucide.createIcons();

        // Datos de disponibilidades pasados desde el backend
        const turnosData = [
            <% if (listaDisponibilidades != null) {
                for (int i = 0; i < listaDisponibilidades.size(); i++) {
                    Map<String, Object> d = listaDisponibilidades.get(i);
                    if (i > 0) out.print(",");
            %>
            {
                idDisponibilidad: <%= d.get("idDisponibilidad") %>,
                idMedico: <%= d.get("idMedico") %>,
                idSede: <%= d.get("idSede") %>,
                sedeNombre: "<%= d.get("sedeNombre") %>",
                fecha: "<%= d.get("fecha") %>",
                horaInicio: "<%= d.get("horaInicio") %>",
                horaFin: "<%= d.get("horaFin") %>",
                duracionMinutos: <%= d.get("duracionMinutos") %>,
                estado: "<%= d.get("estado") %>",
                citasAgendadas: <%= d.get("citasAgendadas") %>
            }
            <%  }
               } %>
        ];

        function abrirModalNuevoMedico() {
            document.getElementById('modalNuevoMedico').style.display = 'flex';
        }

        function cerrarModalNuevoMedico() {
            document.getElementById('modalNuevoMedico').style.display = 'none';
        }

        function abrirModalTurnos(idMedico, nombre, cmp, especialidad, sede) {
            document.getElementById('formTurnoIdMedico').value = idMedico;
            document.getElementById('turnoDocNombre').textContent = nombre;
            document.getElementById('turnoDocCmp').textContent = cmp;
            document.getElementById('turnoDocEsp').textContent = especialidad + (sede ? ' · ' + sede : '');
            
            const hoyStr = new Date().toISOString().split('T')[0];
            const fechaInput = document.getElementById('formTurnoFecha');
            if (fechaInput && !fechaInput.value) {
                fechaInput.value = hoyStr;
                fechaInput.min = hoyStr;
            }

            const selSede = document.getElementById('formTurnoSede');
            if (selSede && sede) {
                for (let i = 0; i < selSede.options.length; i++) {
                    if (selSede.options[i].text.toLowerCase().includes(sede.toLowerCase())) {
                        selSede.selectedIndex = i;
                        break;
                    }
                }
            }

            renderizarTurnosMedico(idMedico);
            document.getElementById('modalGestionarTurnos').style.display = 'flex';
            if (window.lucide) lucide.createIcons();
        }

        function renderizarTurnosMedico(idMedico) {
            const list = turnosData.filter(t => t.idMedico == idMedico);
            const tbody = document.getElementById('tbodyTurnosMedico');
            const emptyDiv = document.getElementById('emptyTurnosMensaje');
            const tableDiv = document.getElementById('tableWrapTurnos');
            
            if (list.length === 0) {
                tbody.innerHTML = '';
                tableDiv.style.display = 'none';
                emptyDiv.style.display = 'block';
            } else {
                tableDiv.style.display = 'block';
                emptyDiv.style.display = 'none';
                
                let html = '';
                list.forEach(t => {
                    const esPub = (t.estado === 'PUBLICADO');
                    const badgeClass = esPub ? 'badge-turno-pub' : 'badge-turno-bloq';
                    const badgeText = esPub ? 'PUBLICADO' : 'BLOQUEADO';
                    const proxEstado = esPub ? 'CANCELADO' : 'PUBLICADO';
                    const btnColor = esPub ? '#b91c1c' : '#166534';
                    const btnBorder = esPub ? '#fca5a5' : '#86efac';
                    const btnBg = esPub ? '#fef2f2' : '#f0fdf4';
                    const btnText = esPub ? 'Bloquear' : 'Habilitar';
                    
                    html += '<tr style="border-bottom: 1px solid #f1f5f9;">' +
                        '<td style="padding: 10px; font-weight: 600; color: #1e293b;">' + t.fecha + '</td>' +
                        '<td style="padding: 10px; color: #334155;">' + t.horaInicio + ' - ' + t.horaFin + ' <span style="font-size: 11px; color:#64748b;">(' + t.duracionMinutos + ' min)</span></td>' +
                        '<td style="padding: 10px; color: #475569;">' + t.sedeNombre + '</td>' +
                        '<td style="padding: 10px; text-align: center;"><span style="font-weight: 700; color:' + (t.citasAgendadas > 0 ? '#0284c7' : '#94a3b8') + ';">' + t.citasAgendadas + '</span></td>' +
                        '<td style="padding: 10px; text-align: center;"><span class="' + badgeClass + '">' + badgeText + '</span></td>' +
                        '<td style="padding: 10px; text-align: right;">' +
                            '<form method="POST" action="${pageContext.request.contextPath}/admin/medicos" style="margin: 0; display: inline;">' +
                                '<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">' +
                                '<input type="hidden" name="action" value="cambiar_estado_horario">' +
                                '<input type="hidden" name="idDisponibilidad" value="' + t.idDisponibilidad + '">' +
                                '<input type="hidden" name="nuevoEstado" value="' + proxEstado + '">' +
                                '<button type="submit" style="background: ' + btnBg + '; color: ' + btnColor + '; border: 1px solid ' + btnBorder + '; border-radius: 4px; padding: 4px 10px; font-size: 11.5px; font-weight: 600; cursor: pointer; transition: all 0.15s ease;">' +
                                    btnText +
                                '</button>' +
                            '</form>' +
                        '</td>' +
                    '</tr>';
                });
                tbody.innerHTML = html;
            }
        }

        function cerrarModalTurnos() {
            document.getElementById('modalGestionarTurnos').style.display = 'none';
        }

        function editarMedico(cmp, nombre) {
            alert('Editar médico: ' + nombre + ' (' + cmp + '). Módulo en edición activa.');
        }

        function exportarExcel() {
            window.print();
        }

        // Cerrar modal al pulsar escape
        window.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                cerrarModalNuevoMedico();
                cerrarModalTurnos();
            }
        });
    </script>
</body>
</html>
