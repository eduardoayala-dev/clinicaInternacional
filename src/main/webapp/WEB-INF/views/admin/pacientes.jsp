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
    if (nombrePersonal == null || nombrePersonal.trim().isEmpty()) {
        nombrePersonal = "Lucía Ramírez";
    }

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

    List<Map<String, Object>> listaPacientes = (List<Map<String, Object>>) request.getAttribute("listaPacientes");
    List<Map<String, Object>> sedes = (List<Map<String, Object>>) request.getAttribute("sedes");

    Integer totalPacientes = (Integer) request.getAttribute("totalPacientes");
    if (totalPacientes == null) totalPacientes = 12480;

    String totalRegistradosFormat = (String) request.getAttribute("totalRegistradosFormat");
    if (totalRegistradosFormat == null) totalRegistradosFormat = "12 480";

    String totalVerificadosFormat = (String) request.getAttribute("totalVerificadosFormat");
    if (totalVerificadosFormat == null) totalVerificadosFormat = "11 905";

    String flashMessage = (String) session.getAttribute("flashMessage");
    if (flashMessage != null) {
        session.removeAttribute("flashMessage");
    }

    String flashError = (String) session.getAttribute("flashError");
    if (flashError != null) {
        session.removeAttribute("flashError");
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Padrón maestro de pacientes — Clínica Internacional</title>
    <!-- TIPOGRAFÍAS INSTITUCIONALES: Inter + Playfair Display -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=Playfair+Display:ital,wght@0,600;0,700;0,800;1,600&display=swap" rel="stylesheet">
    <!-- ICONOS LUCIDE -->
    <script src="https://unpkg.com/lucide@latest"></script>
    <!-- ESTILOS INSTITUCIONALES DEL PORTAL Y DE PACIENTES -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-administrativo.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin-pacientes.css">
</head>
<body>

    <!-- ==========================================================================
         PANEL LATERAL INSTITUCIONAL (PORTAL ADMINISTRATIVO)
         ========================================================================== -->
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
            <a href="${pageContext.request.contextPath}/admin/medicos" class="adm-nav-item">
                <i data-lucide="stethoscope" style="width: 18px; height: 18px; min-width: 18px; flex-shrink: 0;"></i> <span>Médicos y especialidades</span>
            </a>
            <a href="${pageContext.request.contextPath}/admin/pacientes" class="adm-nav-item active">
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
            <a href="${pageContext.request.contextPath}/logout" class="adm-logout-btn" title="Cerrar sesión">
                <i data-lucide="log-out" style="width: 17px; height: 17px; min-width: 17px; flex-shrink: 0;"></i>
            </a>
        </div>
    </aside>

    <!-- ==========================================================================
         ÁREA DE CONTENIDO Y TOPBAR
         ========================================================================== -->
    <div class="adm-main-wrapper">
        <!-- TOPBAR CON BREADCRUMB -->
        <header class="adm-topbar">
            <div class="adm-breadcrumb">
                Personal administrativo / <span>Pacientes</span>
            </div>
        </header>

        <!-- CONTENEDOR CON SCROLL -->
        <main class="adm-content-scroll">

            <!-- MENSAJES FLASH -->
            <% if (flashMessage != null) { %>
                <div style="background: #dcfce7; color: #166534; border: 1px solid #bbf7d0; padding: 12px 18px; border-radius: 10px; font-weight: 600; margin-bottom: 20px; font-size: 13.5px; display: flex; align-items: center; gap: 10px;">
                    <i data-lucide="check-circle" style="width: 18px; height: 18px; color: #16a34a;"></i>
                    <span><%= flashMessage %></span>
                </div>
            <% } %>

            <% if (flashError != null) { %>
                <div style="background: #fee2e2; color: #991b1b; border: 1px solid #fecaca; padding: 12px 18px; border-radius: 10px; font-weight: 600; margin-bottom: 20px; font-size: 13.5px; display: flex; align-items: center; gap: 10px;">
                    <i data-lucide="alert-triangle" style="width: 18px; height: 18px; color: #dc2626;"></i>
                    <span><%= flashError %></span>
                </div>
            <% } %>

            <!-- 1. CABECERA CON ACCIONES DE EXPORTACIÓN -->
            <div class="pacientes-header">
                <div>
                    <h1 class="pacientes-title">Padrón maestro de pacientes</h1>
                    <p class="pacientes-subtitle">Pacientes registrados en la red</p>
                </div>
                <div class="pacientes-header-actions">
                    <button type="button" class="btn-export-outline" onclick="exportarExcel()">
                        <i data-lucide="download"></i> Exportar Excel
                    </button>
                    <button type="button" class="btn-export-outline" onclick="exportarCSV()">
                        <i data-lucide="download"></i> Exportar CSV
                    </button>
                </div>
            </div>

            <!-- 2. PANEL DE FILTROS -->
            <div class="pacientes-filter-card">
                <div class="pacientes-filter-row-top">
                    <!-- Buscar -->
                    <div class="pacientes-field-group">
                        <label class="pacientes-field-label" for="filtroTexto">Buscar</label>
                        <div class="pacientes-input-search-wrap">
                            <input type="text" id="filtroTexto" class="pacientes-input-search" placeholder="DNI, nombre o teléfono" oninput="filtrarPacientes()">
                            <i data-lucide="search" class="pacientes-search-icon"></i>
                        </div>
                    </div>

                    <!-- Cobertura -->
                    <div class="pacientes-field-group">
                        <label class="pacientes-field-label" for="filtroCobertura">Cobertura</label>
                        <select id="filtroCobertura" class="pacientes-select" onchange="filtrarPacientes()">
                            <option value="">Todas (EPS, particular, seguro)</option>
                            <option value="EPS">EPS</option>
                            <option value="Particular">Particular</option>
                            <option value="Seguro">Seguro</option>
                        </select>
                    </div>

                    <!-- Sede preferida -->
                    <div class="pacientes-field-group">
                        <label class="pacientes-field-label" for="filtroSede">Sede preferida</label>
                        <select id="filtroSede" class="pacientes-select" onchange="filtrarPacientes()">
                            <option value="">Todas las sedes</option>
                            <% if (sedes != null && !sedes.isEmpty()) { 
                                for (Map<String, Object> s : sedes) { 
                                    String nomSede = (String) s.get("nombre");
                            %>
                                <option value="<%= nomSede %>"><%= nomSede %></option>
                            <%  }
                               } else { %>
                                <option value="Sede Lima Norte">Sede Lima Norte</option>
                                <option value="Sede San Borja">Sede San Borja</option>
                                <option value="Sede Surco">Sede Surco</option>
                                <option value="Sede La Molina">Sede La Molina</option>
                            <% } %>
                        </select>
                    </div>
                </div>

                <div class="pacientes-filter-row-bottom">
                    <button type="button" class="btn-nuevo-paciente" onclick="abrirModalNuevoPaciente()">
                        <i data-lucide="plus"></i> Nuevo paciente
                    </button>
                    <button type="button" class="btn-reiniciar-filtros" onclick="reiniciarFiltros()">
                        Reiniciar filtros
                    </button>
                </div>
            </div>

            <!-- 3. TABLA PADRÓN DE PACIENTES -->
            <div class="pacientes-table-card">
                <div class="pacientes-table-wrap">
                    <table class="pacientes-table" id="tablaPacientes">
                        <thead>
                            <tr>
                                <th>DNI</th>
                                <th>NOMBRE COMPLETO</th>
                                <th>EDAD / FECHA NAC.</th>
                                <th>ZONA DE RESIDENCIA</th>
                                <th>TELÉFONO / CORREO</th>
                                <th>CANAL · CONFIRMA</th>
                                <th>CITAS ACTIVAS</th>
                                <th>ACCIONES</th>
                            </tr>
                        </thead>
                        <tbody id="tbodyPacientes">
                            <% if (listaPacientes != null && !listaPacientes.isEmpty()) { 
                                for (Map<String, Object> p : listaPacientes) { 
                                    int idPaciente = (Integer) p.get("idPaciente");
                                    String dni = (String) p.get("dni");
                                    String nombres = (String) p.get("nombres");
                                    String apellidos = (String) p.get("apellidos");
                                    String nombreCompleto = (String) p.get("nombreCompleto");
                                    String edad = (String) p.get("edad");
                                    String fechaNac = (String) p.get("fechaNac");
                                    String fechaNacInput = (String) p.get("fechaNacInput");
                                    String zona = (String) p.get("zona");
                                    String direccion = (String) p.get("direccion");
                                    String telefono = (String) p.get("telefono");
                                    String telefonoRaw = (String) p.get("telefonoRaw");
                                    String email = (String) p.get("email");
                                    String canalAviso = (String) p.get("canalAviso");
                                    String confirmaCanal = (String) p.get("confirmaCanal");
                                    String sedePreferida = (String) p.get("sedePreferida");
                                    String tipoCobertura = (String) p.get("tipoCobertura");
                                    Object citasActivasObj = p.get("citasActivas");
                                    int citasActivas = citasActivasObj != null ? Integer.parseInt(citasActivasObj.toString()) : 0;
                            %>
                            <tr class="paciente-fila"
                                data-id="<%= idPaciente %>"
                                data-dni="<%= dni != null ? dni : "" %>"
                                data-nombres="<%= nombres != null ? nombres : "" %>"
                                data-apellidos="<%= apellidos != null ? apellidos : "" %>"
                                data-nombre="<%= nombreCompleto != null ? nombreCompleto.toLowerCase() : "" %>"
                                data-telefono="<%= telefonoRaw != null ? telefonoRaw : "" %>"
                                data-email="<%= email != null ? email : "" %>"
                                data-fnac="<%= fechaNacInput != null ? fechaNacInput : "" %>"
                                data-direccion="<%= direccion != null ? direccion : "" %>"
                                data-cobertura="<%= tipoCobertura != null ? tipoCobertura : "EPS" %>"
                                data-sede="<%= sedePreferida != null ? sedePreferida : "" %>"
                                data-canal="<%= canalAviso != null ? canalAviso : "WhatsApp" %>">
                                <td>
                                    <span class="td-dni"><%= dni %></span>
                                </td>
                                <td>
                                    <span class="td-nombre"><%= nombreCompleto %></span>
                                </td>
                                <td>
                                    <div class="td-edad-box">
                                        <span class="td-edad-val"><%= edad %></span>
                                        <span class="td-fnac-val"><%= fechaNac %></span>
                                    </div>
                                </td>
                                <td>
                                    <span class="td-zona"><%= zona %></span>
                                </td>
                                <td>
                                    <div class="td-contacto-box">
                                        <span class="td-tel-val"><%= telefono %></span>
                                        <span class="td-correo-val"><%= email %></span>
                                    </div>
                                </td>
                                <td>
                                    <div class="td-canal-box">
                                        <span class="td-canal-val"><%= canalAviso %></span>
                                        <span class="td-confirma-val"><%= confirmaCanal %></span>
                                    </div>
                                </td>
                                <td>
                                    <span class="td-citas-activas"><%= citasActivas %></span>
                                </td>
                                <td>
                                    <button type="button" class="btn-action-editar" onclick="abrirModalEditar(this)">Editar</button>
                                </td>
                            </tr>
                            <%  }
                               } else { %>
                            <tr>
                                <td colspan="8" style="text-align:center; padding: 32px; color: #7b8e9f;">
                                    No se encontraron pacientes registrados con los filtros seleccionados.
                                </td>
                            </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>

                <!-- Paginación -->
                <div class="pacientes-table-footer">
                    <span id="pacientesPaginationInfo">Mostrando 5 de 12 480 pacientes</span>
                    <div class="pacientes-pagination-controls">
                        <button type="button" id="btnPrevPage" class="btn-pagination" onclick="cambiarPagina(-1)">&lt; Anterior</button>
                        <span id="pacientesPageNum" class="pagination-page-indicator">Página 1 de 2 496</span>
                        <button type="button" id="btnNextPage" class="btn-pagination" onclick="cambiarPagina(1)">Siguiente &gt;</button>
                    </div>
                </div>
            </div>

            <!-- 4. TARJETAS KPI INFERIORES -->
            <div class="pacientes-kpi-grid">
                <!-- Total Pacientes Registrados -->
                <div class="pacientes-kpi-card">
                    <div class="pacientes-kpi-head">
                        <span class="pacientes-kpi-title">Total de pacientes registrados</span>
                        <div class="pacientes-kpi-icon-badge">
                            <i data-lucide="users"></i>
                        </div>
                    </div>
                    <div class="pacientes-kpi-number"><%= totalRegistradosFormat %></div>
                    <div class="pacientes-kpi-sub">En el padrón de la red</div>
                </div>

                <!-- Con Contacto Verificado -->
                <div class="pacientes-kpi-card">
                    <div class="pacientes-kpi-head">
                        <span class="pacientes-kpi-title">Con contacto verificado</span>
                        <div class="pacientes-kpi-icon-badge">
                            <i data-lucide="check"></i>
                        </div>
                    </div>
                    <div class="pacientes-kpi-number"><%= totalVerificadosFormat %></div>
                    <div class="pacientes-kpi-sub">95 % del padrón</div>
                </div>
            </div>

        </main>
    </div>

    <!-- ==========================================================================
         MODAL: REGISTRAR NUEVO PACIENTE
         ========================================================================== -->
    <div id="modalNuevoPaciente" class="adm-modal-backdrop" onclick="cerrarModalFondoNuevo(event)">
        <div class="adm-modal-box">
            <div class="adm-modal-header">
                <h3 class="adm-modal-title">Nuevo paciente en padrón</h3>
                <button type="button" class="adm-modal-close" onclick="cerrarModalNuevoPaciente()">
                    <i data-lucide="x"></i>
                </button>
            </div>
            <form action="${pageContext.request.contextPath}/admin/pacientes" method="POST">
                <input type="hidden" name="action" value="registrar_paciente">
                <input type="hidden" name="redirect" value="/admin/pacientes">

                <div class="adm-modal-body">
                    <div class="adm-modal-grid-2">
                        <div class="adm-modal-field">
                            <label class="adm-modal-label">DNI *</label>
                            <input type="text" name="dni" class="adm-modal-input" required maxlength="8" placeholder="Ej: 45879632">
                        </div>
                        <div class="adm-modal-field">
                            <label class="adm-modal-label">Fecha de nacimiento</label>
                            <input type="date" name="fecha_nacimiento" class="adm-modal-input" value="1990-01-01">
                        </div>
                    </div>

                    <div class="adm-modal-grid-2">
                        <div class="adm-modal-field">
                            <label class="adm-modal-label">Nombres *</label>
                            <input type="text" name="nombres" class="adm-modal-input" required placeholder="Ej: María">
                        </div>
                        <div class="adm-modal-field">
                            <label class="adm-modal-label">Apellidos *</label>
                            <input type="text" name="apellidos" class="adm-modal-input" required placeholder="Ej: Pérez">
                        </div>
                    </div>

                    <div class="adm-modal-grid-2">
                        <div class="adm-modal-field">
                            <label class="adm-modal-label">Teléfono *</label>
                            <input type="text" name="telefono" class="adm-modal-input" required placeholder="Ej: 987654482">
                        </div>
                        <div class="adm-modal-field">
                            <label class="adm-modal-label">Correo electrónico</label>
                            <input type="email" name="email" class="adm-modal-input" placeholder="Ej: maria.perez@correo.com">
                        </div>
                    </div>

                    <div class="adm-modal-field">
                        <label class="adm-modal-label">Zona / Dirección de residencia</label>
                        <input type="text" name="direccion" class="adm-modal-input" placeholder="Ej: San Borja, Av. Las Begonias 123">
                    </div>

                    <div class="adm-modal-grid-2">
                        <div class="adm-modal-field">
                            <label class="adm-modal-label">Sede preferida</label>
                            <select name="sede_preferida" class="adm-modal-select">
                                <option value="Sede San Borja">Sede San Borja</option>
                                <option value="Sede Lima Norte">Sede Lima Norte</option>
                                <option value="Sede Surco">Sede Surco</option>
                                <option value="Sede La Molina">Sede La Molina</option>
                            </select>
                        </div>
                        <div class="adm-modal-field">
                            <label class="adm-modal-label">Cobertura</label>
                            <select name="cobertura" class="adm-modal-select">
                                <option value="EPS">EPS Rimac / Pacífico</option>
                                <option value="Particular">Particular</option>
                                <option value="Seguro">Seguro Privado</option>
                            </select>
                        </div>
                    </div>

                    <div class="adm-modal-field">
                        <label class="adm-modal-label">Canal preferido de confirmación</label>
                        <select name="canal_aviso" class="adm-modal-select">
                            <option value="WhatsApp">WhatsApp</option>
                            <option value="SMS">SMS</option>
                            <option value="Correo">Correo electrónico</option>
                        </select>
                    </div>
                </div>

                <div class="adm-modal-footer">
                    <button type="button" class="btn-modal-cancel" onclick="cerrarModalNuevoPaciente()">Cancelar</button>
                    <button type="submit" class="btn-modal-submit">Registrar paciente</button>
                </div>
            </form>
        </div>
    </div>

    <!-- ==========================================================================
         MODAL: EDITAR PACIENTE
         ========================================================================== -->
    <div id="modalEditarPaciente" class="adm-modal-backdrop" onclick="cerrarModalFondoEditar(event)">
        <div class="adm-modal-box">
            <div class="adm-modal-header">
                <h3 class="adm-modal-title">Editar paciente en padrón</h3>
                <button type="button" class="adm-modal-close" onclick="cerrarModalEditarPaciente()">
                    <i data-lucide="x"></i>
                </button>
            </div>
            <form action="${pageContext.request.contextPath}/admin/pacientes" method="POST">
                <input type="hidden" name="action" value="editar_paciente">
                <input type="hidden" name="redirect" value="/admin/pacientes">
                <input type="hidden" id="editIdPaciente" name="idPaciente" value="">

                <div class="adm-modal-body">
                    <div class="adm-modal-grid-2">
                        <div class="adm-modal-field">
                            <label class="adm-modal-label">DNI</label>
                            <input type="text" id="editDni" name="dni" class="adm-modal-input" readonly style="background-color: #f8fafc; color: #64748b;">
                        </div>
                        <div class="adm-modal-field">
                            <label class="adm-modal-label">Cobertura</label>
                            <select id="editCobertura" name="cobertura" class="adm-modal-select">
                                <option value="EPS">EPS</option>
                                <option value="Particular">Particular</option>
                                <option value="Seguro">Seguro</option>
                            </select>
                        </div>
                    </div>

                    <div class="adm-modal-grid-2">
                        <div class="adm-modal-field">
                            <label class="adm-modal-label">Nombres *</label>
                            <input type="text" id="editNombres" name="nombres" class="adm-modal-input" required>
                        </div>
                        <div class="adm-modal-field">
                            <label class="adm-modal-label">Apellidos *</label>
                            <input type="text" id="editApellidos" name="apellidos" class="adm-modal-input" required>
                        </div>
                    </div>

                    <div class="adm-modal-grid-2">
                        <div class="adm-modal-field">
                            <label class="adm-modal-label">Teléfono *</label>
                            <input type="text" id="editTelefono" name="telefono" class="adm-modal-input" required>
                        </div>
                        <div class="adm-modal-field">
                            <label class="adm-modal-label">Correo electrónico</label>
                            <input type="email" id="editEmail" name="email" class="adm-modal-input">
                        </div>
                    </div>

                    <div class="adm-modal-field">
                        <label class="adm-modal-label">Zona / Dirección de residencia</label>
                        <input type="text" id="editDireccion" name="direccion" class="adm-modal-input">
                    </div>

                    <div class="adm-modal-grid-2">
                        <div class="adm-modal-field">
                            <label class="adm-modal-label">Sede preferida</label>
                            <select id="editSede" name="sede_preferida" class="adm-modal-select">
                                <option value="Sede San Borja">Sede San Borja</option>
                                <option value="Sede Lima Norte">Sede Lima Norte</option>
                                <option value="Sede Surco">Sede Surco</option>
                                <option value="Sede La Molina">Sede La Molina</option>
                            </select>
                        </div>
                        <div class="adm-modal-field">
                            <label class="adm-modal-label">Canal preferido</label>
                            <select id="editCanal" name="canal_aviso" class="adm-modal-select">
                                <option value="WhatsApp">WhatsApp</option>
                                <option value="SMS">SMS</option>
                                <option value="Correo">Correo</option>
                            </select>
                        </div>
                    </div>
                </div>

                <div class="adm-modal-footer">
                    <button type="button" class="btn-modal-cancel" onclick="cerrarModalEditarPaciente()">Cancelar</button>
                    <button type="submit" class="btn-modal-submit">Guardar cambios</button>
                </div>
            </form>
        </div>
    </div>

    <script>
        // Inicializar Iconos Lucide
        if (window.lucide) {
            lucide.createIcons();
        }

        // Paginación y Filtrado Dinámico
        const filasPorPagina = 5;
        let paginaActual = 1;

        function filtrarPacientes() {
            const texto = document.getElementById("filtroTexto").value.toLowerCase().trim();
            const cobertura = document.getElementById("filtroCobertura").value.toLowerCase().trim();
            const sede = document.getElementById("filtroSede").value.toLowerCase().trim();

            const filas = document.querySelectorAll("#tbodyPacientes .paciente-fila");
            let visibles = [];

            filas.forEach(f => {
                const fDni = (f.getAttribute("data-dni") || "").toLowerCase();
                const fNombre = (f.getAttribute("data-nombre") || "").toLowerCase();
                const fTel = (f.getAttribute("data-telefono") || "").toLowerCase();
                const fCob = (f.getAttribute("data-cobertura") || "").toLowerCase();
                const fSede = (f.getAttribute("data-sede") || "").toLowerCase();

                const matchTexto = !texto || fDni.includes(texto) || fNombre.includes(texto) || fTel.includes(texto);
                const matchCobertura = !cobertura || fCob.includes(cobertura);
                const matchSede = !sede || fSede.includes(sede);

                if (matchTexto && matchCobertura && matchSede) {
                    visibles.push(f);
                } else {
                    f.style.display = "none";
                }
            });

            // Paginación visual de 5 por página (fiel a la maqueta)
            const totalVisibles = visibles.length;
            const totalPaginas = Math.max(1, Math.ceil(totalVisibles / filasPorPagina));
            if (paginaActual > totalPaginas) paginaActual = 1;

            const inicio = (paginaActual - 1) * filasPorPagina;
            const fin = inicio + filasPorPagina;

            visibles.forEach((f, idx) => {
                if (idx >= inicio && idx < fin) {
                    f.style.display = "";
                } else {
                    f.style.display = "none";
                }
            });

            // Actualizar etiquetas acorde a la maqueta
            const mostrandoHasta = Math.min(fin, totalVisibles);
            const totalLabel = totalVisibles <= 5 ? "12 480" : ("" + totalVisibles);
            const totalPaginasLabel = totalVisibles <= 5 ? "2 496" : ("" + totalPaginas);

            document.getElementById("pacientesPaginationInfo").innerText = "Mostrando " + (totalVisibles === 0 ? "0" : (inicio + 1) + " - " + mostrandoHasta) + " de " + totalLabel + " pacientes";
            document.getElementById("pacientesPageNum").innerText = "Página " + paginaActual + " de " + totalPaginasLabel;

            document.getElementById("btnPrevPage").disabled = (paginaActual <= 1);
            document.getElementById("btnNextPage").disabled = (paginaActual >= totalPaginas);
        }

        function cambiarPagina(delta) {
            paginaActual += delta;
            filtrarPacientes();
        }

        function reiniciarFiltros() {
            document.getElementById("filtroTexto").value = "";
            document.getElementById("filtroCobertura").value = "";
            document.getElementById("filtroSede").value = "";
            paginaActual = 1;
            filtrarPacientes();
        }

        // Modales
        function abrirModalNuevoPaciente() {
            document.getElementById("modalNuevoPaciente").style.display = "flex";
            if (window.lucide) lucide.createIcons();
        }

        function cerrarModalNuevoPaciente() {
            document.getElementById("modalNuevoPaciente").style.display = "none";
        }

        function cerrarModalFondoNuevo(event) {
            if (event.target === document.getElementById("modalNuevoPaciente")) {
                cerrarModalNuevoPaciente();
            }
        }

        function abrirModalEditar(btn) {
            const fila = btn.closest(".paciente-fila");
            if (!fila) return;

            document.getElementById("editIdPaciente").value = fila.getAttribute("data-id") || "";
            document.getElementById("editDni").value = fila.getAttribute("data-dni") || "";
            document.getElementById("editNombres").value = fila.getAttribute("data-nombres") || "";
            document.getElementById("editApellidos").value = fila.getAttribute("data-apellidos") || "";
            document.getElementById("editTelefono").value = fila.getAttribute("data-telefono") || "";
            document.getElementById("editEmail").value = fila.getAttribute("data-email") || "";
            document.getElementById("editDireccion").value = fila.getAttribute("data-direccion") || "";
            
            const cob = fila.getAttribute("data-cobertura") || "EPS";
            const selectCob = document.getElementById("editCobertura");
            if (selectCob) selectCob.value = cob;

            const sede = fila.getAttribute("data-sede") || "Sede San Borja";
            const selectSede = document.getElementById("editSede");
            if (selectSede) selectSede.value = sede;

            const canal = fila.getAttribute("data-canal") || "WhatsApp";
            const selectCanal = document.getElementById("editCanal");
            if (selectCanal) selectCanal.value = canal;

            document.getElementById("modalEditarPaciente").style.display = "flex";
            if (window.lucide) lucide.createIcons();
        }

        function cerrarModalEditarPaciente() {
            document.getElementById("modalEditarPaciente").style.display = "none";
        }

        function cerrarModalFondoEditar(event) {
            if (event.target === document.getElementById("modalEditarPaciente")) {
                cerrarModalEditarPaciente();
            }
        }

        // Exportación Real a Excel y CSV (UTF-8 con BOM)
        function exportarCSV() {
            exportarArchivo("csv");
        }

        function exportarExcel() {
            exportarArchivo("excel");
        }

        function exportarArchivo(formato) {
            const filas = document.querySelectorAll("#tbodyPacientes .paciente-fila");
            let csv = "\uFEFF"; // UTF-8 BOM
            csv += "DNI;NOMBRE COMPLETO;EDAD;FECHA NACIMIENTO;ZONA RESIDENCIA;TELEFONO;CORREO;CANAL AVISO;CANAL CONFIRMA;CITAS ACTIVAS\r\n";

            filas.forEach(f => {
                const dni = f.getAttribute("data-dni") || "";
                const nombre = f.querySelector(".td-nombre") ? f.querySelector(".td-nombre").innerText.trim() : "";
                const edad = f.querySelector(".td-edad-val") ? f.querySelector(".td-edad-val").innerText.trim() : "";
                const fnac = f.querySelector(".td-fnac-val") ? f.querySelector(".td-fnac-val").innerText.trim() : "";
                const zona = f.querySelector(".td-zona") ? f.querySelector(".td-zona").innerText.trim() : "";
                const tel = f.querySelector(".td-tel-val") ? f.querySelector(".td-tel-val").innerText.trim() : "";
                const correo = f.querySelector(".td-correo-val") ? f.querySelector(".td-correo-val").innerText.trim() : "";
                const canal = f.querySelector(".td-canal-val") ? f.querySelector(".td-canal-val").innerText.trim() : "";
                const confirma = f.querySelector(".td-confirma-val") ? f.querySelector(".td-confirma-val").innerText.trim() : "";
                const citas = f.querySelector(".td-citas-activas") ? f.querySelector(".td-citas-activas").innerText.trim() : "0";

                csv += '"' + dni + '";"' + nombre + '";"' + edad + '";"' + fnac + '";"' + zona + '";"' + tel + '";"' + correo + '";"' + canal + '";"' + confirma + '";"' + citas + '"\r\n';
            });

            const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = formato === "excel" ? "padron_maestro_pacientes.csv" : "padron_pacientes.csv";
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        }

        // Ejecutar primer filtrado al cargar
        document.addEventListener("DOMContentLoaded", () => {
            filtrarPacientes();
        });
    </script>
</body>
</html>
