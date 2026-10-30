<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreAdmin = (String) request.getAttribute("nombreAdmin");
    if (nombreAdmin == null && usuario != null) {
        nombreAdmin = usuario.getNombreCompleto();
    }
    if (nombreAdmin == null || nombreAdmin.trim().isEmpty()) {
        nombreAdmin = "Jorge Vargas";
    }

    List<Usuario> listaUsuarios = (List<Usuario>) request.getAttribute("listaUsuarios");
    Map<String, Object> metricas = (Map<String, Object>) request.getAttribute("metricasUsuarios");
    List<Map<String, Object>> sedes = (List<Map<String, Object>>) request.getAttribute("sedes");
    List<Map<String, Object>> especialidades = (List<Map<String, Object>>) request.getAttribute("especialidades");

    String flashMessage = (String) session.getAttribute("flashMessage");
    if (flashMessage != null) {
        session.removeAttribute("flashMessage");
    }
    String flashError = (String) session.getAttribute("flashError");
    if (flashError != null) {
        session.removeAttribute("flashError");
    }

    int medicosTotal = metricas != null && metricas.get("medicosTotal") != null ? (Integer) metricas.get("medicosTotal") : 0;
    int medicosActivos = metricas != null && metricas.get("medicosActivos") != null ? (Integer) metricas.get("medicosActivos") : 0;
    int personalTotal = metricas != null && metricas.get("personalTotal") != null ? (Integer) metricas.get("personalTotal") : 0;
    int personalActivos = metricas != null && metricas.get("personalActivos") != null ? (Integer) metricas.get("personalActivos") : 0;
    int adminsTotal = metricas != null && metricas.get("adminsTotal") != null ? (Integer) metricas.get("adminsTotal") : 0;
    int adminsActivos = metricas != null && metricas.get("adminsActivos") != null ? (Integer) metricas.get("adminsActivos") : 0;
    int totalUsuariosGeneral = metricas != null && metricas.get("totalUsuarios") != null ? (Integer) metricas.get("totalUsuarios") : (listaUsuarios != null ? listaUsuarios.size() : 0);
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Usuarios y roles - Clínica Internacional</title>
    <!-- Tipografía Institucional: Playfair Display + Inter -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=Playfair+Display:ital,wght@0,600;0,700;1,600&display=swap" rel="stylesheet">
    
    <!-- CSS del Layout Institucional y Estilos de Usuarios -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/sysadmin-layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/sysadmin-usuarios.css">
    
    <script src="https://unpkg.com/lucide@latest"></script>
</head>
<body>

    <!-- ==========================================================================
         PANEL IZQUIERDO INSTITUCIONAL (260px)
         ========================================================================== -->
    <aside class="sidebar">
        <a href="${pageContext.request.contextPath}/sysadmin/dashboard" class="brand-container">
            <img src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png" 
                 alt="Logo Clínica Internacional" class="brand-logo-img"
                 onerror="this.src='${pageContext.request.contextPath}/assets/images/logoClinical.png'">
            <div class="brand-text">
                <span class="brand-title">Clínica Internacional</span>
                <span class="brand-subtitle">Portal del administrador</span>
            </div>
        </a>

        <nav class="nav-menu">
            <a href="${pageContext.request.contextPath}/sysadmin/dashboard" class="nav-item">
                <i data-lucide="home"></i>
                <span>Inicio</span>
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/usuarios" class="nav-item active">
                <i data-lucide="shield"></i>
                <span>Usuarios y roles</span>
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/sedes" class="nav-item">
                <i data-lucide="building"></i>
                <span>Sedes</span>
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/priorizacion" class="nav-item">
                <i data-lucide="sliders"></i>
                <span>Configuración de priorización</span>
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/analitica" class="nav-item">
                <i data-lucide="trending-up"></i>
                <span>Indicadores y analítica</span>
            </a>
        </nav>

        <div class="user-profile">
            <div class="avatar"><%= nombreAdmin != null && !nombreAdmin.trim().isEmpty() ? ("" + nombreAdmin.charAt(0) + (nombreAdmin.contains(" ") ? ("" + nombreAdmin.charAt(nombreAdmin.indexOf(" ") + 1)) : "")).toUpperCase() : "JV" %></div>
            <div class="user-info">
                <span class="user-name"><%= nombreAdmin %></span>
                <span class="user-role">Administrador</span>
            </div>
            <form method="POST" action="${pageContext.request.contextPath}/logout?portal=personal" style="margin:0;display:inline;">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
<button type="submit" class="btn-logout" title="Cerrar sesión" style="border:0;cursor:pointer;font-family:inherit;">
                <i data-lucide="log-out"></i>
            </button>
</form>
        </div>
    </aside>

    <!-- ==========================================================================
         CONTENEDOR PRINCIPAL CON HEADER SUPERIOR BLANCO
         ========================================================================== -->
    <main class="main-content">
        <!-- BARRA SUPERIOR BLANCA / GRIS CLARO (DISTINGUIDA DEL FONDO #f3f6fb) -->
        <header class="dash-top-header">
            <nav class="breadcrumb-trail">
                <span>Administrador</span>
                <span class="bc-sep">/</span>
                <strong>Usuarios y roles</strong>
            </nav>
        </header>

        <!-- ÁREA CON SCROLL Y FONDO #f3f6fb -->
        <div class="dash-scroll-container">
            <!-- Título y Subtítulo en Playfair Display -->
            <div class="page-title-box">
                <h1 class="page-title-serif">Usuarios y roles</h1>
                <p class="page-subtitle">Cuentas del personal con acceso al sistema</p>
            </div>

            <% if (flashMessage != null) { %>
                <div class="flash-alert">
                    <i data-lucide="check-circle" style="width: 18px; color: #166534;"></i>
                    <span><%= flashMessage %></span>
                </div>
            <% } %>

            <% if (flashError != null) { %>
                <div class="flash-alert" style="background: #fee2e2; color: #991b1b; border: 1px solid #fecaca;">
                    <i data-lucide="alert-circle" style="width: 18px; color: #dc2626;"></i>
                    <span><%= flashError %></span>
                </div>
            <% } %>

            <!-- 1. TARJETAS DE MÉTRICAS (3 COLUMNAS) -->
            <div class="metrics-row">
                <div class="stat-card">
                    <div class="stat-header">
                        <span class="stat-label">Médicos</span>
                        <div class="stat-icon-badge">
                            <i data-lucide="stethoscope"></i>
                        </div>
                    </div>
                    <div class="stat-number"><%= medicosTotal %></div>
                    <div class="stat-subtext">Activos: <%= medicosActivos %></div>
                </div>

                <div class="stat-card">
                    <div class="stat-header">
                        <span class="stat-label">Personal administrativo</span>
                        <div class="stat-icon-badge">
                            <i data-lucide="users"></i>
                        </div>
                    </div>
                    <div class="stat-number"><%= personalTotal %></div>
                    <div class="stat-subtext">Activos: <%= personalActivos %></div>
                </div>

                <div class="stat-card">
                    <div class="stat-header">
                        <span class="stat-label">Administradores</span>
                        <div class="stat-icon-badge">
                            <i data-lucide="shield"></i>
                        </div>
                    </div>
                    <div class="stat-number"><%= adminsTotal %></div>
                    <div class="stat-subtext">Activos: <%= adminsActivos %></div>
                </div>
            </div>

            <!-- 2. TARJETA DE FILTROS -->
            <div class="filters-card">
                <div class="filter-group">
                    <label for="inputBuscar">Buscar</label>
                    <div class="search-input-wrapper">
                        <input type="text" id="inputBuscar" placeholder="Nombre o correo" oninput="filtrarUsuarios()">
                        <i data-lucide="search"></i>
                    </div>
                </div>

                <div class="filter-group">
                    <label for="selectRol">Rol</label>
                    <select id="selectRol" class="filter-select" onchange="filtrarUsuarios()">
                        <option value="">Todos los roles</option>
                        <option value="Médico">Médico</option>
                        <option value="Personal administrativo">Personal administrativo</option>
                        <option value="Administrador">Administrador</option>
                    </select>
                </div>

                <div class="filter-group">
                    <label for="selectEstado">Estado</label>
                    <select id="selectEstado" class="filter-select" onchange="filtrarUsuarios()">
                        <option value="">Todos (activo, inactivo, suspendido)</option>
                        <option value="Activo">Activo</option>
                        <option value="Inactivo">Inactivo</option>
                        <option value="Suspendido">Suspendido</option>
                    </select>
                </div>
            </div>

            <!-- 3. FILA DE BOTONES DE ACCIÓN -->
            <div class="actions-row">
                <div class="actions-left">
                    <button class="btn-primary-action" onclick="abrirModalNuevo()">
                        <i data-lucide="plus"></i>
                        <span>Nuevo usuario</span>
                    </button>
                    <button class="btn-secondary-action" onclick="exportarExcel()">
                        <i data-lucide="download"></i>
                        <span>Exportar Excel</span>
                    </button>
                </div>
                <button class="btn-reset-filters" onclick="reiniciarFiltros()">Reiniciar filtros</button>
            </div>

            <!-- 4. TABLA DE USUARIOS Y ROLES -->
            <div class="table-card">
                <table class="users-table" id="tablaUsuarios">
                    <thead>
                        <tr>
                            <th>NOMBRE Y APELLIDOS</th>
                            <th>CORREO / IDENTIFICADOR</th>
                            <th>ROL ASIGNADO</th>
                            <th>SEDE PRINCIPAL</th>
                            <th>ESTADO</th>
                            <th>FECHA DE REGISTRO</th>
                            <th>ACCIONES</th>
                        </tr>
                    </thead>
                    <tbody id="tbodyUsuarios">
                        <% if (listaUsuarios != null && !listaUsuarios.isEmpty()) { 
                            for (Usuario u : listaUsuarios) { 
                                String estadoClass = u.getEstadoDetallado().toLowerCase();
                        %>
                        <tr class="user-row" 
                            data-nombre="<%= u.getNombreMostrar().toLowerCase() %>" 
                            data-correo="<%= u.getEmail() != null ? u.getEmail().toLowerCase() : "" %>"
                            data-rol="<%= u.getRolEtiqueta() %>"
                            data-estado="<%= u.getEstadoDetallado() %>"
                            data-id="<%= u.getIdUsuario() %>"
                            data-nombres="<%= u.getNombres() != null ? u.getNombres().replace("\"", "&quot;") : "" %>"
                            data-apellidos="<%= u.getApellidos() != null ? u.getApellidos().replace("\"", "&quot;") : "" %>"
                            data-email="<%= u.getEmail() != null ? u.getEmail() : "" %>"
                            data-rol-code="<%= u.getRol() %>"
                            data-sede-id="<%= u.getIdSede() != null ? u.getIdSede() : "" %>"
                            data-cmp="<%= u.getCmp() != null ? u.getCmp() : "" %>"
                            data-esp-id="<%= u.getIdEspecialidad() != null ? u.getIdEspecialidad() : "" %>"
                            data-cargo="<%= u.getCargo() != null ? u.getCargo() : "" %>">
                            <td>
                                <div class="user-cell-name">
                                    <span class="user-name-title"><%= u.getNombreMostrar() %></span>
                                    <% if (u.getMotivoEstado() != null && !u.getMotivoEstado().isEmpty()) { %>
                                        <span class="user-name-note"><%= u.getMotivoEstado() %></span>
                                    <% } %>
                                </div>
                            </td>
                            <td><%= u.getEmail() %></td>
                            <td><%= u.getRolEtiqueta() %></td>
                            <td><%= u.getNombreSede() %></td>
                            <td>
                                <span class="status-pill <%= estadoClass %>"><%= u.getEstadoDetallado() %></span>
                            </td>
                            <td><%= u.getFechaRegistroFormateada() %></td>
                            <td>
                                <button type="button" class="btn-table-edit" onclick="abrirModalEditarDesdeFila(this)">Editar</button>
                            </td>
                        </tr>
                        <%  } 
                           } %>
                    </tbody>
                </table>
            </div>

            <!-- 5. BARRA DE PAGINACIÓN -->
            <div class="pagination-bar">
                <span class="pagination-info" id="paginationInfo">Mostrando 5 de <%= totalUsuariosGeneral %> usuarios</span>
                <div class="pagination-controls">
                    <button class="btn-pagination" id="btnPrevPage" onclick="cambiarPagina(-1)">&lt; Anterior</button>
                    <span class="pagination-page-label" id="paginationPage">Página 1 de 27</span>
                    <button class="btn-pagination btn-pagination-next" id="btnNextPage" onclick="cambiarPagina(1)">Siguiente &gt;</button>
                </div>
            </div>
        </div>
    </main>

    <!-- ==========================================================================
         MODAL CREAR / EDITAR USUARIO
         ========================================================================== -->
    <div class="modal-backdrop" id="modalUsuarioBackdrop" onclick="cerrarModalFondo(event)">
        <div class="modal-window">
            <div class="modal-header-row">
                <h3 class="modal-header-title" id="modalTitulo">Nuevo usuario</h3>
                <button type="button" class="btn-modal-close" onclick="cerrarModal()">&times;</button>
            </div>

            <form action="${pageContext.request.contextPath}/sysadmin/usuarios" method="POST" id="formUsuario">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" id="formAction" value="registrar">
                <input type="hidden" name="idUsuario" id="formIdUsuario" value="">

                <div class="form-grid-modal">
                    <div class="form-field">
                        <label for="formNombres">Nombres</label>
                        <input type="text" id="formNombres" name="nombres" required placeholder="Ej. Carlos">
                    </div>

                    <div class="form-field">
                        <label for="formApellidos">Apellidos</label>
                        <input type="text" id="formApellidos" name="apellidos" required placeholder="Ej. Mendoza">
                    </div>

                    <div class="form-field full">
                        <label for="formEmail">Correo electrónico institucional</label>
                        <input type="email" id="formEmail" name="email" required placeholder="cmendoza@clinica.pe">
                    </div>

                    <div class="form-field">
                        <label for="formRol">Rol asignado</label>
                        <select id="formRol" name="rol" required onchange="actualizarCamposCondicionales()">
                            <option value="MEDICO">Médico</option>
                            <option value="PERSONAL_ADMINISTRATIVO">Personal administrativo</option>
                            <option value="ADMINISTRADOR">Administrador</option>
                        </select>
                    </div>

                    <div class="form-field">
                        <label for="formEstado">Estado de la cuenta</label>
                        <select id="formEstado" name="estado" required>
                            <option value="ACTIVO">Activo</option>
                            <option value="INACTIVO">Inactivo</option>
                            <option value="SUSPENDIDO">Suspendido</option>
                        </select>
                    </div>

                    <div class="form-field full" id="groupSede">
                        <label for="formSede">Sede asignada</label>
                        <select id="formSede" name="id_sede" required>
                            <option value="" disabled selected>Seleccione la sede asignada...</option>
                            <% if (sedes != null) { 
                                for (Map<String, Object> s : sedes) { %>
                                    <option value="<%= s.get("id") %>"><%= s.get("nombre") %></option>
                            <%  } 
                               } %>
                        </select>
                    </div>

                    <!-- Campos condicionales para Médicos -->
                    <div class="conditional-role-box" id="boxMedico">
                        <div class="conditional-role-title">Datos clínicos del médico</div>
                        <div class="form-grid-modal" style="margin-bottom: 0;">
                            <div class="form-field">
                                <label for="formCmp">N° Colegiatura (CMP)</label>
                                <input type="text" id="formCmp" name="cmp" placeholder="Ej. CMP-45211">
                            </div>
                            <div class="form-field">
                                <label for="formEspecialidad">Especialidad médica</label>
                                <select id="formEspecialidad" name="id_especialidad">
                                    <option value="">Seleccione especialidad...</option>
                                    <% if (especialidades != null) { 
                                        for (Map<String, Object> esp : especialidades) { %>
                                            <option value="<%= esp.get("id") %>"><%= esp.get("nombre") %></option>
                                    <%  } 
                                       } %>
                                </select>
                            </div>
                        </div>
                    </div>

                    <!-- Campos condicionales para Personal Administrativo -->
                    <div class="conditional-role-box" id="boxPersonal">
                        <div class="conditional-role-title">Datos del personal administrativo</div>
                        <div class="form-field full">
                            <label for="formCargo">Cargo o puesto</label>
                            <input type="text" id="formCargo" name="cargo" placeholder="Ej. Coordinador de Admisión">
                        </div>
                    </div>

                    <div class="form-field full" id="groupPassword">
                        <label for="formPassword">Contraseña de acceso</label>
                        <input type="password" id="formPassword" name="password" placeholder="Por defecto: Password123!">
                    </div>
                </div>

                <div class="modal-footer-row">
                    <button type="button" class="btn-modal-cancel" onclick="cerrarModal()">Cancelar</button>
                    <button type="submit" class="btn-modal-submit" id="btnSubmitModal">Guardar usuario</button>
                </div>
            </form>
        </div>
    </div>

    <!-- ==========================================================================
         LÓGICA JAVASCRIPT: FILTRADO EN TIEMPO REAL, MODALES Y EXPORTACIÓN EXCEL
         ========================================================================== -->
    <script>
        let paginaActual = 1;
        const filasPorPagina = 5;

        document.addEventListener("DOMContentLoaded", function () {
            if (window.lucide) {
                lucide.createIcons();
            }
            actualizarCamposCondicionales();
            filtrarUsuarios();
        });

        // Filtrado en vivo de la tabla por Nombre/Correo, Rol y Estado
        function filtrarUsuarios() {
            const texto = document.getElementById("inputBuscar").value.toLowerCase().trim();
            const rolSeleccionado = document.getElementById("selectRol").value;
            const estadoSeleccionado = document.getElementById("selectEstado").value;

            const filas = document.querySelectorAll("#tbodyUsuarios .user-row");
            let visibles = [];

            filas.forEach(fila => {
                const nombre = fila.getAttribute("data-nombre") || "";
                const correo = fila.getAttribute("data-correo") || "";
                const rol = fila.getAttribute("data-rol") || "";
                const estado = fila.getAttribute("data-estado") || "";

                const coincideTexto = !texto || nombre.includes(texto) || correo.includes(texto);
                const coincideRol = !rolSeleccionado || rol === rolSeleccionado;
                const coincideEstado = !estadoSeleccionado || estado === estadoSeleccionado;

                if (coincideTexto && coincideRol && coincideEstado) {
                    visibles.push(fila);
                } else {
                    fila.style.display = "none";
                }
            });

            // Paginación visual
            const totalVisibles = visibles.length;
            const totalPaginas = Math.max(1, Math.ceil(totalVisibles / filasPorPagina));
            if (paginaActual > totalPaginas) paginaActual = 1;

            const inicio = (paginaActual - 1) * filasPorPagina;
            const fin = inicio + filasPorPagina;

            visibles.forEach((fila, idx) => {
                if (idx >= inicio && idx < fin) {
                    fila.style.display = "";
                } else {
                    fila.style.display = "none";
                }
            });

            // Actualizar etiquetas
            const mostrandoHasta = Math.min(fin, totalVisibles);
            const infoTexto = totalVisibles === 0 
                ? "Mostrando 0 de 0 usuarios" 
                : "Mostrando " + (inicio + 1) + " - " + mostrandoHasta + " de " + totalVisibles + " usuarios";
            document.getElementById("paginationInfo").innerText = infoTexto;
            document.getElementById("paginationPage").innerText = "Página " + paginaActual + " de " + totalPaginas;

            document.getElementById("btnPrevPage").disabled = (paginaActual <= 1);
            document.getElementById("btnNextPage").disabled = (paginaActual >= totalPaginas);
        }

        function cambiarPagina(delta) {
            paginaActual += delta;
            filtrarUsuarios();
        }

        function reiniciarFiltros() {
            document.getElementById("inputBuscar").value = "";
            document.getElementById("selectRol").value = "";
            document.getElementById("selectEstado").value = "";
            paginaActual = 1;
            filtrarUsuarios();
        }

        // Exportar tabla a Excel / CSV en tiempo real sin recargar página
        function exportarExcel() {
            const filas = document.querySelectorAll("#tbodyUsuarios .user-row");
            let csv = "\uFEFF"; // BOM UTF-8 para apertura directa en Microsoft Excel
            csv += "NOMBRE Y APELLIDOS;CORREO / IDENTIFICADOR;ROL ASIGNADO;SEDE PRINCIPAL;ESTADO;FECHA DE REGISTRO\r\n";

            filas.forEach(f => {
                const cols = f.querySelectorAll("td");
                if (cols.length >= 6) {
                    const nombre = cols[0].querySelector(".user-name-title") ? cols[0].querySelector(".user-name-title").innerText.trim() : cols[0].innerText.trim();
                    const correo = cols[1].innerText.trim();
                    const rol = cols[2].innerText.trim();
                    const sede = cols[3].innerText.trim();
                    const estado = cols[4].innerText.trim();
                    const fecha = cols[5].innerText.trim();

                    csv += '"' + nombre + '";"' + correo + '";"' + rol + '";"' + sede + '";"' + estado + '";"' + fecha + '"\r\n';
                }
            });

            const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = "usuarios_clinica_internacional.csv";
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        }

        // Gestión de Modales
        function abrirModalNuevo() {
            document.getElementById("modalTitulo").innerText = "Nuevo usuario";
            document.getElementById("formAction").value = "registrar";
            document.getElementById("formIdUsuario").value = "";
            document.getElementById("formNombres").value = "";
            document.getElementById("formApellidos").value = "";
            document.getElementById("formEmail").value = "";
            document.getElementById("formEmail").readOnly = false;
            document.getElementById("formRol").value = "MEDICO";
            document.getElementById("formEstado").value = "ACTIVO";
            document.getElementById("formSede").value = "3"; // Sede San Borja por defecto
            document.getElementById("formCmp").value = "";
            document.getElementById("formEspecialidad").value = "";
            document.getElementById("formCargo").value = "";
            document.getElementById("groupPassword").style.display = "flex";
            document.getElementById("btnSubmitModal").innerText = "Guardar usuario";

            actualizarCamposCondicionales();
            document.getElementById("modalUsuarioBackdrop").style.display = "flex";
            if (window.lucide) lucide.createIcons();
        }

        function abrirModalEditarDesdeFila(btn) {
            const fila = btn.closest(".user-row");
            if (!fila) return;

            document.getElementById("modalTitulo").innerText = "Editar usuario";
            document.getElementById("formAction").value = "editar";
            document.getElementById("formIdUsuario").value = fila.getAttribute("data-id") || "";
            document.getElementById("formNombres").value = fila.getAttribute("data-nombres") || "";
            document.getElementById("formApellidos").value = fila.getAttribute("data-apellidos") || "";
            document.getElementById("formEmail").value = fila.getAttribute("data-email") || "";
            document.getElementById("formEmail").readOnly = true;
            
            const rolCode = fila.getAttribute("data-rol-code") || "MEDICO";
            document.getElementById("formRol").value = rolCode;

            const estado = fila.getAttribute("data-estado") || "Activo";
            document.getElementById("formEstado").value = estado.toUpperCase();

            document.getElementById("formSede").value = fila.getAttribute("data-sede-id") || "";
            document.getElementById("formCmp").value = fila.getAttribute("data-cmp") || "";
            document.getElementById("formEspecialidad").value = fila.getAttribute("data-esp-id") || "";
            document.getElementById("formCargo").value = fila.getAttribute("data-cargo") || "";
            document.getElementById("groupPassword").style.display = "none";
            document.getElementById("btnSubmitModal").innerText = "Actualizar usuario";

            actualizarCamposCondicionales();
            document.getElementById("modalUsuarioBackdrop").style.display = "flex";
            if (window.lucide) lucide.createIcons();
        }

        function cerrarModal() {
            document.getElementById("modalUsuarioBackdrop").style.display = "none";
        }

        function cerrarModalFondo(event) {
            if (event.target === document.getElementById("modalUsuarioBackdrop")) {
                cerrarModal();
            }
        }

        function actualizarCamposCondicionales() {
            const rol = document.getElementById("formRol").value;
            const boxMedico = document.getElementById("boxMedico");
            const boxPersonal = document.getElementById("boxPersonal");
            const groupSede = document.getElementById("groupSede");
            const formSede = document.getElementById("formSede");

            if (rol === "MEDICO") {
                boxMedico.style.display = "block";
                boxPersonal.style.display = "none";
                if (groupSede) {
                    groupSede.style.display = "flex";
                    formSede.required = true;
                }
            } else if (rol === "PERSONAL_ADMINISTRATIVO") {
                boxMedico.style.display = "none";
                boxPersonal.style.display = "block";
                if (groupSede) {
                    groupSede.style.display = "flex";
                    formSede.required = true;
                }
            } else {
                // ADMINISTRADOR
                boxMedico.style.display = "none";
                boxPersonal.style.display = "none";
                if (groupSede) {
                    groupSede.style.display = "none";
                    formSede.required = false;
                    formSede.value = "";
                }
            }
        }
    </script>
</body>
</html>
