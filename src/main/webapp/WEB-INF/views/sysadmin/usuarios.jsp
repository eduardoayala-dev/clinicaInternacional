<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="pe.edu.utp.clinica.util.Html" %>
<%
    Map<Integer,String> estadosInvitacion=(Map<Integer,String>)request.getAttribute("estadosInvitacion");
    if(estadosInvitacion==null)estadosInvitacion=Map.of();
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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/fuentes-locales.css?v=20261104-1">

    <!-- CSS del Layout Institucional y Estilos de Usuarios -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/sysadmin-layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/sysadmin-usuarios.css">

    <script src="${pageContext.request.contextPath}/assets/vendor/lucide-1.68.0.min.js"></script>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-administrativo.css?v=20261104-admin"><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/sysadmin-shell.css?v=20261104-admin2"><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-forms.css?v=20261104-2"></head>
<body class="sysadmin-portal">

    <!-- ==========================================================================
         PANEL IZQUIERDO INSTITUCIONAL (260px)
         ========================================================================== -->
    <%@ include file="/WEB-INF/jspf/sysadmin-sidebar.jspf" %>

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
                    <span><%= Html.texto(flashMessage) %></span>
                </div>
            <% } %>

            <% if (flashError != null) { %>
                <div class="flash-alert" style="background: #fee2e2; color: #991b1b; border: 1px solid #fecaca;">
                    <i data-lucide="alert-circle" style="width: 18px; color: #dc2626;"></i>
                    <span><%= Html.texto(flashError) %></span>
                </div>
            <% } %>

            <p class="page-subtitle">Los totales y la tabla se actualizan con los filtros seleccionados.</p>
            <div class="metrics-row">
                <div class="stat-card">
                    <div class="stat-header">
                        <span class="stat-label">Médicos</span>
                        <div class="stat-icon-badge">
                            <i data-lucide="stethoscope"></i>
                        </div>
                    </div>
                    <div class="stat-number" id="medicosTotal"><%= Html.texto(medicosTotal) %></div>
                    <div class="stat-subtext">Activos: <span id="medicosActivos"><%= Html.texto(medicosActivos) %></span></div>
                </div>

                <div class="stat-card">
                    <div class="stat-header">
                        <span class="stat-label">Personal administrativo</span>
                        <div class="stat-icon-badge">
                            <i data-lucide="users"></i>
                        </div>
                    </div>
                    <div class="stat-number" id="personalTotal"><%= Html.texto(personalTotal) %></div>
                    <div class="stat-subtext">Activos: <span id="personalActivos"><%= Html.texto(personalActivos) %></span></div>
                </div>

                <div class="stat-card">
                    <div class="stat-header">
                        <span class="stat-label">Administradores</span>
                        <div class="stat-icon-badge">
                            <i data-lucide="shield"></i>
                        </div>
                    </div>
                    <div class="stat-number" id="adminsTotal"><%= Html.texto(adminsTotal) %></div>
                    <div class="stat-subtext">Activos: <span id="adminsActivos"><%= Html.texto(adminsActivos) %></span></div>
                </div>
            </div>

            <!-- 2. TARJETA DE FILTROS -->
            <div class="filters-card">
                <div class="filter-group">
                    <label for="inputBuscar">Buscar</label>
                    <div class="search-input-wrapper">
                        <input type="text" id="inputBuscar" placeholder="Nombre o correo" oninput="filtrarUsuarios(true)">
                        <i data-lucide="search"></i>
                    </div>
                </div>

                <div class="filter-group">
                    <label for="selectRol">Rol</label>
                    <select id="selectRol" class="filter-select" onchange="filtrarUsuarios(true)">
                        <option value="">Todos los roles</option>
                        <option value="Médico">Médico</option>
                        <option value="Personal administrativo">Personal administrativo</option>
                        <option value="Administrador">Administrador</option>
                    </select>
                </div>

                <div class="filter-group">
                    <label for="selectSede">Sede</label>
                    <select id="selectSede" class="filter-select" onchange="filtrarUsuarios(true)">
                        <option value="">Todas las sedes</option>
                        <% if(sedes!=null){for(Map<String,Object> sede:sedes){ %><option value="<%= Html.texto(sede.get("id")) %>"><%= Html.texto(sede.get("nombre")) %></option><% }} %>
                        <option value="multisede">Multisede</option><option value="sin-sede">Sin sede asignada</option>
                    </select>
                </div>                <div class="filter-group">
                    <label for="selectEstado">Estado</label>
                    <select id="selectEstado" class="filter-select" onchange="filtrarUsuarios(true)">
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
                        <span>Exportar CSV</span>
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
                            data-nombre="<%= Html.texto(u.getNombreMostrar().toLowerCase()) %>"
                            data-correo="<%= Html.texto(u.getEmail() != null ? u.getEmail().toLowerCase() : "") %>"
                            data-rol="<%= Html.texto(u.getRolEtiqueta()) %>"
                            data-estado="<%= Html.texto(u.getEstadoDetallado().toUpperCase(java.util.Locale.ROOT)) %>" data-activacion-pendiente="<%= estadosInvitacion.containsKey(u.getIdUsuario()) %>"
                            data-id="<%= Html.texto(u.getIdUsuario()) %>"
                            data-nombres="<%= Html.texto(u.getNombres() != null ? u.getNombres() : "") %>"
                            data-apellidos="<%= Html.texto(u.getApellidos() != null ? u.getApellidos() : "") %>"
                            data-email="<%= Html.texto(u.getEmail() != null ? u.getEmail() : "") %>"
                            data-rol-code="<%= Html.texto(u.getRol()) %>"
                            data-sede-id="<%= Html.texto(u.getIdSede() != null ? u.getIdSede() : "") %>"
                            data-cmp="<%= Html.texto(u.getCmp() != null ? u.getCmp() : "") %>"
                            data-esp-id="<%= Html.texto(u.getIdEspecialidad() != null ? u.getIdEspecialidad() : "") %>"
                            data-cargo="<%= Html.texto(u.getCargo() != null ? u.getCargo() : "") %>">
                            <td>
                                <div class="user-cell-name">
                                    <span class="user-name-title"><%= Html.texto(u.getNombreMostrar()) %></span>
                                    <% if (u.getMotivoEstado() != null && !u.getMotivoEstado().isEmpty()) { %>
                                        <span class="user-name-note"><%= Html.texto(u.getMotivoEstado()) %></span>
                                    <% } %>
                                </div>
                            </td>
                            <td><%= Html.texto(u.getEmail()) %></td>
                            <td><%= Html.texto(u.getRolEtiqueta()) %></td>
                            <td><%= Html.texto(u.getNombreSede()) %></td>
                            <td>
                                <span class="status-pill <%= Html.texto(estadoClass) %>"><%= Html.texto(estadosInvitacion.getOrDefault(u.getIdUsuario(),u.getEstadoDetallado())) %></span>
                            </td>
                            <td><%= Html.texto(u.getFechaRegistroFormateada()) %></td>
                            <td>
                                <button type="button" class="btn-table-edit" onclick="abrirModalEditarDesdeFila(this)">Editar</button>
                                <% if(estadosInvitacion.containsKey(u.getIdUsuario())) { %>
                                <form method="post" action="<%= Html.texto(request.getContextPath()) %>/sysadmin/personal">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="reinvitar"><input type="hidden" name="idUsuario" value="<%= Html.texto(u.getIdUsuario()) %>">
                                    <button class="btn-table-edit" type="submit">Reenviar invitación</button>
                                </form>
                                <% if(!"Invitación revocada".equals(estadosInvitacion.get(u.getIdUsuario()))) { %>
                                <form method="post" action="<%= Html.texto(request.getContextPath()) %>/sysadmin/personal" onsubmit="return confirm('El enlace dejará de permitir la activación. ¿Revocar la invitación?')">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="cambiarEstado"><input type="hidden" name="estado" value="false"><input type="hidden" name="idUsuario" value="<%= Html.texto(u.getIdUsuario()) %>">
                                    <button class="btn-table-edit" type="submit">Revocar invitación</button>
                                </form>
                                <% } %>
                                <% } %>
                            </td>
                        </tr>
                        <%  }
                           } %>
                    </tbody>
                </table>
            </div>

            <!-- 5. BARRA DE PAGINACIÓN -->
            <div class="pagination-bar">
                <span class="pagination-info" id="paginationInfo">Mostrando 5 de <%= Html.texto(totalUsuariosGeneral) %> usuarios</span>
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
        <div class="modal-window" role="dialog" aria-modal="true" aria-labelledby="modalTitulo">
            <div class="modal-header-row">
                <h3 class="modal-header-title" id="modalTitulo">Nuevo usuario</h3>
                <button type="button" class="btn-modal-close" aria-label="Cerrar formulario" onclick="cerrarModal()">&times;</button>
            </div>

            <form action="${pageContext.request.contextPath}/sysadmin/usuarios" method="POST" id="formUsuario">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" id="formAction" value="registrar">
                <input type="hidden" name="idUsuario" id="formIdUsuario" value=""><input type="hidden" name="rol" id="formRolFijo" disabled>

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
                        <input type="hidden" name="estado" id="formEstadoFijo" value="INACTIVO" disabled><select id="formEstado" name="estado" required>
                            <option value="ACTIVO">Activo</option>
                            <option value="INACTIVO">Inactivo</option>
                            <option value="SUSPENDIDO">Suspendido</option>
                        </select>
                    </div>

                    <p id="estadoInvitacionAviso" class="form-help full" hidden>La cuenta se activará cuando la persona cree su contraseña desde el enlace enviado a su correo.</p>
                    <div class="form-field full" id="groupSede">
                        <label for="formSede">Sede asignada</label>
                        <select id="formSede" name="id_sede" required>
                            <option value="" disabled selected>Seleccione la sede asignada...</option>
                            <% if (sedes != null) {
                                for (Map<String, Object> s : sedes) { if(!"ACTIVO".equals(s.get("estado")))continue; %>
                                    <option value="<%= Html.texto(s.get("id")) %>"><%= Html.texto(s.get("nombre")) %></option>
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
                                            <option value="<%= Html.texto(esp.get("id")) %>"><%= Html.texto(esp.get("nombre")) %></option>
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
                        <p>La cuenta permanecerá pendiente. Enviaremos un enlace de un solo uso al correo registrado para que la persona cree su contraseña.</p>
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
        function filtrarUsuarios(reiniciar = false) {
            if(reiniciar) paginaActual=1;
            const texto = document.getElementById("inputBuscar").value.toLowerCase().trim();
            const rolSeleccionado = document.getElementById("selectRol").value;
            const estadoSeleccionado = document.getElementById("selectEstado").value.toUpperCase();
            const sedeSeleccionada = document.getElementById("selectSede").value;

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

                const sedeFila=rol === "Administrador" ? "multisede" : (fila.dataset.sedeId && fila.dataset.sedeId !== "0" ? fila.dataset.sedeId : "sin-sede");
                const coincideSede=!sedeSeleccionada || sedeFila===sedeSeleccionada;
                fila.dataset.coincide=String(coincideTexto && coincideRol && coincideEstado && coincideSede);
                if (fila.dataset.coincide === "true") {
                    visibles.push(fila);
                } else {
                    fila.style.display = "none";
                }
            });

            for(const [clave,rol] of [['medicos','Médico'],['personal','Personal administrativo'],['admins','Administrador']]){
                const grupo=visibles.filter(f=>f.getAttribute('data-rol')===rol);
                document.getElementById(clave+'Total').textContent=grupo.length;
                document.getElementById(clave+'Activos').textContent=grupo.filter(f=>f.getAttribute('data-estado')==='ACTIVO').length;
            }
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
            document.getElementById("selectSede").value = "";
            paginaActual = 1;
            filtrarUsuarios();
        }

        // Exportar tabla a Excel / CSV en tiempo real sin recargar página
        function exportarExcel() {
            const proteger=valor=>{
                let texto=String(valor).replace(/\s+/g,' ').trim();
                if(/^[=+@-]/.test(texto))texto="'"+texto;
                return '"'+texto.replaceAll('"','""')+'"';
            };
            const filas=Array.from(document.querySelectorAll('#tbodyUsuarios .user-row')).filter(f=>f.dataset.coincide==='true');
            const datos=[['Nombre y apellidos','Correo','Rol','Sede','Estado','Fecha de registro'],...filas.map(f=>Array.from(f.cells).slice(0,6).map(c=>c.textContent))];
            const csv='\uFEFF'+datos.map(f=>f.map(proteger).join(';')).join('\r\n');
            const url=URL.createObjectURL(new Blob([csv],{type:'text/csv;charset=utf-8'}));
            const enlace=document.createElement('a');enlace.href=url;enlace.download='personal-filtrado.csv';enlace.click();URL.revokeObjectURL(url);
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
            document.getElementById("formRol").disabled = false;
            document.getElementById("formRolFijo").disabled = true;
            document.getElementById("formEstado").value = "INACTIVO";
            document.getElementById("formEstado").disabled = true;
            document.getElementById("formSede").value = "";
            document.getElementById("formEstadoFijo").disabled=false;
            document.getElementById("estadoInvitacionAviso").hidden=false;
            document.getElementById("formCmp").value = "";
            document.getElementById("formEspecialidad").value = "";
            document.getElementById("formCargo").value = "";
            document.getElementById("groupPassword").style.display = "flex";
            document.getElementById("btnSubmitModal").innerText = "Enviar invitación";

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
            document.getElementById("formRol").disabled = true;
            document.getElementById("formRolFijo").value = rolCode;
            document.getElementById("formRolFijo").disabled = false;

            const estado = fila.getAttribute("data-estado") || "Activo";
            document.getElementById("formEstado").value = estado.toUpperCase();
            const pendiente=fila.dataset.activacionPendiente === "true";
            document.getElementById("formEstado").disabled = pendiente;
            document.getElementById("formEstadoFijo").disabled = !pendiente;
            document.getElementById("estadoInvitacionAviso").hidden = !pendiente;

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

            formSede.disabled=false;
            if (rol === "MEDICO") {
                groupSede.style.display="flex";
                const perfilCompleto=document.getElementById('formAction').value==='editar'&&Boolean(document.getElementById('formCmp').value.trim());
                formSede.disabled=perfilCompleto;
                formSede.required=!perfilCompleto;
                document.getElementById('formCmp').disabled=true;
                document.getElementById('formEspecialidad').disabled=true;
                boxMedico.style.display="none";
                boxPersonal.style.display="none";            } else if (rol === "PERSONAL_ADMINISTRATIVO") {
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
