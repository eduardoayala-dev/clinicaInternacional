<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreAdmin = (String) request.getAttribute("nombreAdmin");
    if (nombreAdmin == null && usuario != null) {
        nombreAdmin = usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreAdmin == null) nombreAdmin = "Roberto Vargas";

    List<Usuario> listaUsuarios = (List<Usuario>) request.getAttribute("listaUsuarios");
    List<Map<String, Object>> sedes = (List<Map<String, Object>>) request.getAttribute("sedes");
    List<Map<String, Object>> especialidades = (List<Map<String, Object>>) request.getAttribute("especialidades");

    String flashMessage = (String) session.getAttribute("flashMessage");
    if (flashMessage != null) {
        session.removeAttribute("flashMessage");
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gestión de Usuarios y Roles - Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        :root {
            --primary: #2563eb;
            --primary-light: #eff6ff;
            --bg-body: #f8fafc;
            --bg-surface: #ffffff;
            --text-main: #0f172a;
            --text-muted: #64748b;
            --border-light: #e2e8f0;
            --shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05);
            --radius-md: 8px;
            --radius-lg: 12px;
        }

        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Inter', sans-serif; }
        body { display: flex; flex-direction: column; height: 100vh; background-color: var(--bg-body); overflow: hidden; color: var(--text-main); }

        /* HEADER SUPERIOR (TOPBAR) */
        .topbar {
            height: 64px; flex-shrink: 0; background-color: var(--bg-surface);
            border-bottom: 1px solid var(--border-light); display: flex;
            align-items: center; justify-content: space-between; padding: 0 32px; z-index: 10;
        }
        .topbar-brand { display: flex; align-items: center; gap: 12px; }
        .topbar-brand img { height: 36px; }
        .topbar-right { display: flex; align-items: center; gap: 16px; }
        .user-info { display: flex; flex-direction: column; align-items: flex-end; }
        .user-name { font-weight: 700; font-size: 13px; color: var(--text-main); }
        .user-role { font-size: 11px; color: var(--primary); font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
        .user-avatar { width: 36px; height: 36px; border-radius: 50%; background-color: var(--primary-light); color: var(--primary); display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px; }
        .btn-logout { display: flex; align-items: center; gap: 6px; padding: 8px 12px; border-radius: var(--radius-md); font-size: 13px; font-weight: 500; color: var(--text-muted); text-decoration: none; transition: all 0.2s; }
        .btn-logout:hover { color: #ef4444; background: #fef2f2; }

        /* CONTENEDOR INFERIOR */
        .app-body { display: flex; height: calc(100vh - 64px); overflow: hidden; }

        /* SIDEBAR BLANCO */
        .sidebar {
            width: 260px; background-color: var(--bg-surface); border-right: 1px solid var(--border-light);
            padding: 24px 16px; display: flex; flex-direction: column; gap: 6px; flex-shrink: 0; overflow-y: auto;
        }
        .nav-item {
            padding: 12px 16px; display: flex; align-items: center; gap: 12px;
            color: var(--text-muted); text-decoration: none; font-size: 14px; font-weight: 500;
            border-radius: var(--radius-md); transition: all 0.2s ease;
        }
        .nav-item:hover { background-color: var(--bg-body); color: var(--text-main); }
        .nav-item.active { background-color: var(--primary-light); color: var(--primary); font-weight: 600; }

        /* ÁREA PRINCIPAL */
        .main-content { flex: 1; height: 100%; overflow-y: auto; padding: 32px 40px; }
        .page-header { margin-bottom: 24px; }
        .page-title { font-size: 24px; font-weight: 700; color: var(--text-main); margin-bottom: 6px; letter-spacing: -0.5px; }
        .page-subtitle { font-size: 14px; color: var(--text-muted); }

        .flash-alert {
            background: #dcfce7; color: #166534; border: 1px solid #bbf7d0; padding: 14px 18px;
            border-radius: var(--radius-md); font-weight: 600; margin-bottom: 24px; font-size: 14px; display: flex; align-items: center; gap: 10px;
        }

        /* METRIC CARDS 4 COLUMNS */
        .metrics-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 24px; }
        .metric-card {
            background: var(--bg-surface); padding: 20px; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            box-shadow: var(--shadow-sm); display: flex; flex-direction: column; justify-content: space-between;
        }
        .metric-title { font-size: 11px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; display: flex; justify-content: space-between; align-items: center; }
        .metric-value { font-size: 28px; font-weight: 700; color: var(--text-main); margin-top: 10px; }

        /* FILTER CARD */
        .filter-card {
            background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px 24px; margin-bottom: 24px; box-shadow: var(--shadow-sm);
            display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap;
        }
        .filter-inputs { display: flex; gap: 12px; align-items: center; flex: 1; }
        .search-box {
            display: flex; align-items: center; gap: 8px; border: 1px solid var(--border-light);
            padding: 10px 16px; border-radius: 20px; background: #ffffff; flex: 1; max-width: 360px;
        }
        .search-box input { border: none; outline: none; width: 100%; font-size: 14px; }
        .select-filter {
            padding: 10px 16px; border-radius: 20px; border: 1px solid var(--border-light);
            font-size: 14px; color: var(--text-main); outline: none; background: #ffffff;
        }
        
        .btn-add-user {
            background: var(--primary); color: #ffffff; border: none; padding: 10px 22px; border-radius: 20px;
            font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 12px rgba(37,99,235,0.2);
            display: inline-flex; align-items: center; gap: 8px; text-decoration: none;
        }
        .btn-add-user:hover { background: #1d4ed8; transform: translateY(-1px); }

        /* TABLE PANEL */
        .panel {
            background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            overflow: hidden; box-shadow: var(--shadow-sm);
        }
        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { background: #ffffff; padding: 16px 20px; font-size: 12px; font-weight: 600; color: var(--text-muted); border-bottom: 1px solid var(--border-light); }
        td { padding: 16px 20px; font-size: 14px; color: var(--text-main); border-bottom: 1px solid var(--border-light); vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover { background: #f8fafc; }

        .badge-role { padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; display: inline-block; }
        .badge-admin { background: #fee2e2; color: #991b1b; }
        .badge-medico { background: #e0f2fe; color: #0369a1; }
        .badge-personal { background: #fef3c7; color: #92400e; }

        .badge-activo { background: #dcfce7; color: #166534; }
        .badge-inactivo { background: #fee2e2; color: #991b1b; }

        /* ICON BUTTONS LIMPIOS Y SENCILLOS */
        .btn-icon {
            background: #ffffff; border: 1px solid var(--border-light); border-radius: var(--radius-md);
            width: 34px; height: 34px; display: inline-flex; align-items: center; justify-content: center;
            color: var(--text-muted); cursor: pointer; transition: all 0.2s; outline: none;
        }
        .btn-icon:hover { background: var(--primary-light); color: var(--primary); border-color: var(--primary); }
        .btn-icon-disable:hover { background: #fef2f2; color: #ef4444; border-color: #fca5a5; }
        .btn-icon-enable:hover { background: #ecfdf5; color: #10b981; border-color: #6ee7b7; }

        /* MODAL STYLING */
        .modal-overlay {
            position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(15,23,42,0.4); backdrop-filter: blur(4px);
            display: none; align-items: center; justify-content: center; z-index: 1000;
        }
        .modal-card {
            background: #ffffff; border-radius: var(--radius-lg); width: 100%; max-width: 580px; padding: 28px;
            box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1); border: 1px solid var(--border-light); max-height: 90vh; overflow-y: auto;
        }
        .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
        .modal-title { font-size: 18px; font-weight: 700; color: var(--text-main); }
        .modal-close { background: none; border: none; font-size: 20px; color: var(--text-muted); cursor: pointer; }
        
        .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
        .form-group { display: flex; flex-direction: column; gap: 6px; }
        .form-group.full { grid-column: 1 / -1; }
        .form-group label { font-size: 13px; font-weight: 600; color: var(--text-main); }
        .form-group input, .form-group select {
            padding: 10px 14px; border-radius: var(--radius-md); border: 1px solid var(--border-light); font-size: 14px; outline: none;
        }
        .form-group input:focus, .form-group select:focus { border-color: var(--primary); }

        .clinical-box {
            background: #f8fafc; border: 1px solid var(--border-light); border-radius: var(--radius-md);
            padding: 16px; margin-bottom: 20px; grid-column: 1 / -1; display: none;
        }
        .clinical-box-title { font-size: 12px; font-weight: 700; color: var(--primary); text-transform: uppercase; margin-bottom: 12px; }

        .modal-footer { display: flex; justify-content: flex-end; gap: 12px; }
        .btn-cancel { padding: 10px 20px; border-radius: 20px; border: 1px solid var(--border-light); background: #ffffff; cursor: pointer; font-weight: 600; }
        .btn-save { padding: 10px 22px; border-radius: 20px; border: none; background: var(--primary); color: #ffffff; cursor: pointer; font-weight: 600; }

        @media(max-width:1024px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } .metrics-grid { grid-template-columns: repeat(2, 1fr); } }
    </style>
</head>
<body>

    <header class="topbar">
        <div class="topbar-brand">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>
        <div class="topbar-right">
            <div class="user-info">
                <span class="user-name"><%= nombreAdmin %></span>
                <span class="user-role">ADMINISTRADOR DE SISTEMAS</span>
            </div>
            <div class="user-avatar"><i data-lucide="user" style="width: 18px; height: 18px;"></i></div>
            <div style="width: 1px; height: 24px; background: var(--border-light); margin: 0 4px;"></div>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout"><i data-lucide="log-out" style="width: 16px; height: 16px;"></i> Salir</a>
        </div>
    </header>

    <div class="app-body">
        <aside class="sidebar">
            <a href="${pageContext.request.contextPath}/sysadmin/dashboard" class="nav-item">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> Inicio
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/usuarios" class="nav-item active">
                <i data-lucide="users" style="width: 18px;"></i> Usuarios y Roles
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/sedes" class="nav-item">
                <i data-lucide="building-2" style="width: 18px;"></i> Gestión de Sedes
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/priorizacion" class="nav-item">
                <i data-lucide="sliders-horizontal" style="width: 18px;"></i> Config. Priorización
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/analitica" class="nav-item">
                <i data-lucide="pie-chart" style="width: 18px;"></i> Métricas
            </a>
            <a href="#" class="nav-item" style="margin-top: auto; border-top: 1px solid var(--border-light); border-radius: 0; padding-top: 16px;">
                <i data-lucide="user" style="width: 18px;"></i> Mi Perfil
            </a>
        </aside>

        <main class="main-content">
            <div class="page-header">
                <h1 class="page-title">Gestión de Usuarios y Roles del Sistema</h1>
                <p class="page-subtitle">Administración de accesos de médicos asistenciales, personal administrativo y administradores TI.</p>
            </div>

            <% if (flashMessage != null) { %>
                <div class="flash-alert">
                    <i data-lucide="check-circle" style="width: 18px;"></i>
                    <%= flashMessage %>
                </div>
            <% } %>

            <!-- METRIC CARDS 4 COLUMNS -->
            <div class="metrics-grid">
                <div class="metric-card">
                    <div class="metric-title">MÉDICOS <i data-lucide="stethoscope" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value">142</div>
                </div>

                <div class="metric-card">
                    <div class="metric-title">PERSONAL ADM. <i data-lucide="briefcase" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value">64</div>
                </div>

                <div class="metric-card">
                    <div class="metric-title">PACIENTES REGISTRADOS <i data-lucide="users" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value">1,248</div>
                </div>

                <div class="metric-card">
                    <div class="metric-title">ADMINISTRADORES TI <i data-lucide="shield" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value">8</div>
                </div>
            </div>

            <!-- FILTER CARD -->
            <div class="filter-card">
                <div class="filter-inputs">
                    <div class="search-box">
                        <i data-lucide="search" style="width: 18px; color: var(--text-muted);"></i>
                        <input type="text" id="searchInput" placeholder="Buscar por nombre, correo o rol..." onkeyup="filtrarTabla()">
                    </div>
                    <select class="select-filter" id="rolFilter" onchange="filtrarTabla()">
                        <option value="">Todos los Roles</option>
                        <option value="MEDICO">Médico</option>
                        <option value="PERSONAL_ADMINISTRATIVO">Personal Adm.</option>
                        <option value="ADMINISTRADOR">Admin TI</option>
                    </select>
                    <select class="select-filter" id="estadoFilter" onchange="filtrarTabla()">
                        <option value="">Todos los Estados</option>
                        <option value="ACTIVO">Activo</option>
                        <option value="INACTIVO">Inactivo</option>
                    </select>
                </div>
                <button class="btn-add-user" onclick="abrirModalNuevo()">
                    <i data-lucide="user-plus" style="width: 18px;"></i> + Nuevo Usuario
                </button>
            </div>

            <!-- TABLE PANEL -->
            <div class="panel">
                <table id="usuariosTable">
                    <thead>
                        <tr>
                            <th>Nombre y Apellidos</th>
                            <th>Correo Electrónico</th>
                            <th>Rol Asignado</th>
                            <th>Estado</th>
                            <th>Acciones</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (listaUsuarios != null && !listaUsuarios.isEmpty()) { 
                            for (Usuario u : listaUsuarios) { 
                                String badgeClass = "badge-personal";
                                if ("ADMINISTRADOR".equals(u.getRol())) badgeClass = "badge-admin";
                                else if ("MEDICO".equals(u.getRol())) badgeClass = "badge-medico";
                        %>
                        <tr>
                            <td><strong><%= u.getNombreCompleto() %></strong></td>
                            <td style="color: var(--text-muted);"><%= u.getEmail() %></td>
                            <td><span class="badge-role <%= badgeClass %>"><%= u.getRol() %></span></td>
                            <td>
                                <% if (u.isActivo()) { %>
                                    <span class="badge-role badge-activo">ACTIVO</span>
                                <% } else { %>
                                    <span class="badge-role badge-inactivo">INACTIVO</span>
                                <% } %>
                            </td>
                            <td style="display: flex; gap: 8px;">
                                <button class="btn-icon" title="Editar usuario" onclick="editarUsuario('<%= u.getIdUsuario() %>', '<%= u.getNombres() %>', '<%= u.getApellidos() %>', '<%= u.getEmail() %>', '<%= u.getRol() %>')">
                                    <i data-lucide="edit-2" style="width: 16px;"></i>
                                </button>
                                <form action="${pageContext.request.contextPath}/sysadmin/usuarios" method="POST" style="margin: 0;">
                                    <input type="hidden" name="action" value="cambiarEstado">
                                    <input type="hidden" name="idUsuario" value="<%= u.getIdUsuario() %>">
                                    <% if (u.isActivo()) { %>
                                        <input type="hidden" name="estado" value="false">
                                        <button type="submit" class="btn-icon btn-icon-disable" title="Deshabilitar usuario">
                                            <i data-lucide="user-x" style="width: 16px;"></i>
                                        </button>
                                    <% } else { %>
                                        <input type="hidden" name="estado" value="true">
                                        <button type="submit" class="btn-icon btn-icon-enable" title="Habilitar usuario">
                                            <i data-lucide="user-check" style="width: 16px;"></i>
                                        </button>
                                    <% } %>
                                </form>
                            </td>
                        </tr>
                        <% } } else { %>
                        <tr>
                            <td colspan="5" style="text-align: center; color: var(--text-muted); padding: 32px;">
                                No hay personal médico o administrativo registrado.
                            </td>
                        </tr>
                        <% } %>
                    </tbody>
                </table>
            </div>
        </main>
    </div>

    <!-- MODAL REGISTRO/EDICION -->
    <div class="modal-overlay" id="userModal">
        <div class="modal-card">
            <div class="modal-header">
                <h3 class="modal-title" id="modalTitle">Nuevo Usuario del Sistema</h3>
                <button class="modal-close" onclick="cerrarModal()">&times;</button>
            </div>
            <form action="${pageContext.request.contextPath}/sysadmin/usuarios" method="POST">
                <input type="hidden" name="action" id="modalAction" value="registrar">
                <input type="hidden" name="idUsuario" id="idUsuario">

                <div class="form-grid">
                    <div class="form-group">
                        <label>Nombres *</label>
                        <input type="text" name="nombres" id="modalNombres" required>
                    </div>
                    <div class="form-group">
                        <label>Apellidos *</label>
                        <input type="text" name="apellidos" id="modalApellidos" required>
                    </div>
                    <div class="form-group full">
                        <label>Correo Electrónico *</label>
                        <input type="email" name="email" id="modalEmail" required>
                    </div>
                    <div class="form-group full">
                        <label>Rol del Sistema *</label>
                        <select name="rol" id="modalRol" required onchange="onRolChange()">
                            <option value="" disabled selected>Seleccione un rol...</option>
                            <option value="MEDICO">Médico Especialista</option>
                            <option value="PERSONAL_ADMINISTRATIVO">Personal Administrativo / Admisión</option>
                            <option value="ADMINISTRADOR">Administrador TI</option>
                        </select>
                    </div>

                    <!-- CAMPOS DINÁMICOS MÉDICO -->
                    <div class="clinical-box" id="boxMedico">
                        <div class="clinical-box-title"><i data-lucide="stethoscope" style="width: 14px; vertical-align: middle;"></i> Información Asistencial del Médico</div>
                        <div class="form-grid" style="margin-bottom: 0;">
                            <div class="form-group">
                                <label>Colegiatura (CMP) *</label>
                                <input type="text" name="cmp" id="modalCmp" placeholder="Ej. CMP 45211">
                            </div>
                            <div class="form-group">
                                <label>Especialidad Principal *</label>
                                <select name="id_especialidad" id="modalEspecialidad">
                                    <option value="">Seleccione especialidad...</option>
                                    <% if (especialidades != null) { for (Map<String, Object> esp : especialidades) { %>
                                        <option value="<%= esp.get("id") %>"><%= esp.get("nombre") %></option>
                                    <% } } %>
                                </select>
                            </div>
                            <div class="form-group full">
                                <label>Sede Principal de Atención *</label>
                                <select name="id_sede_medico" id="modalSedeMedico">
                                    <option value="">Seleccione sede...</option>
                                    <% if (sedes != null) { for (Map<String, Object> s : sedes) { %>
                                        <option value="<%= s.get("id") %>"><%= s.get("nombre") %></option>
                                    <% } } %>
                                </select>
                            </div>
                        </div>
                    </div>

                    <!-- CAMPOS DINÁMICOS PERSONAL ADM -->
                    <div class="clinical-box" id="boxPersonal">
                        <div class="clinical-box-title"><i data-lucide="briefcase" style="width: 14px; vertical-align: middle;"></i> Información de Admisión / Recepción</div>
                        <div class="form-grid" style="margin-bottom: 0;">
                            <div class="form-group">
                                <label>Código de Empleado</label>
                                <input type="text" value="Autogenerado por el sistema (Ej. ADM-003)" disabled style="background: #f1f5f9; color: var(--text-muted); font-style: italic;">
                            </div>
                            <div class="form-group">
                                <label>Cargo / Área *</label>
                                <input type="text" name="cargo" id="modalCargo" placeholder="Ej. Admisión - Ventanilla 2">
                            </div>
                            <div class="form-group full">
                                <label>Sede Asignada *</label>
                                <select name="id_sede_personal" id="modalSedePersonal">
                                    <option value="">Seleccione sede...</option>
                                    <% if (sedes != null) { for (Map<String, Object> s : sedes) { %>
                                        <option value="<%= s.get("id") %>"><%= s.get("nombre") %></option>
                                    <% } } %>
                                </select>
                            </div>
                        </div>
                    </div>

                    <div class="form-group full" id="passwordGroup">
                        <label>Contraseña Inicial</label>
                        <input type="password" name="password" id="modalPassword" placeholder="Contraseña de acceso para nuevo usuario">
                    </div>
                </div>

                <div class="modal-footer">
                    <button type="button" class="btn-cancel" onclick="cerrarModal()">Cancelar</button>
                    <button type="submit" class="btn-save">Guardar Usuario</button>
                </div>
            </form>
        </div>
    </div>

    <script>
        lucide.createIcons();

        function onRolChange() {
            let rol = document.getElementById('modalRol').value;
            let boxMedico = document.getElementById('boxMedico');
            let boxPersonal = document.getElementById('boxPersonal');

            if (rol === 'MEDICO') {
                boxMedico.style.display = 'block';
                boxPersonal.style.display = 'none';
            } else if (rol === 'PERSONAL_ADMINISTRATIVO') {
                boxMedico.style.display = 'none';
                boxPersonal.style.display = 'block';
            } else {
                boxMedico.style.display = 'none';
                boxPersonal.style.display = 'none';
            }
        }

        function abrirModalNuevo() {
            document.getElementById('modalTitle').innerText = "Nuevo Usuario del Sistema";
            document.getElementById('modalAction').value = "registrar";
            document.getElementById('idUsuario').value = "";
            document.getElementById('modalNombres').value = "";
            document.getElementById('modalApellidos').value = "";
            document.getElementById('modalEmail').value = "";
            document.getElementById('modalRol').value = "";
            document.getElementById('passwordGroup').style.display = "flex";
            document.getElementById('modalPassword').required = true;
            onRolChange();
            document.getElementById('userModal').style.display = "flex";
        }

        function editarUsuario(id, nombres, apellidos, email, rol) {
            document.getElementById('modalTitle').innerText = "Editar Usuario #" + id;
            document.getElementById('modalAction').value = "editar";
            document.getElementById('idUsuario').value = id;
            document.getElementById('modalNombres').value = nombres;
            document.getElementById('modalApellidos').value = apellidos;
            document.getElementById('modalEmail').value = email;
            document.getElementById('modalRol').value = rol;
            document.getElementById('passwordGroup').style.display = "none";
            document.getElementById('modalPassword').required = false;
            onRolChange();
            document.getElementById('userModal').style.display = "flex";
        }

        function cerrarModal() {
            document.getElementById('userModal').style.display = "none";
        }

        function filtrarTabla() {
            let q = document.getElementById('searchInput').value.toLowerCase();
            let rol = document.getElementById('rolFilter').value.toLowerCase();
            let estado = document.getElementById('estadoFilter').value.toLowerCase();
            let rows = document.querySelectorAll('#usuariosTable tbody tr');

            rows.forEach(tr => {
                let text = tr.innerText.toLowerCase();
                let match = text.includes(q) && tr.innerText.toLowerCase().includes(rol) && tr.innerText.toLowerCase().includes(estado);
                tr.style.display = match ? '' : 'none';
            });
        }
    </script>
</body>
</html>
