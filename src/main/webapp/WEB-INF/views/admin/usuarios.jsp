<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gestión de Usuarios - Clínica Internacional</title>
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
        .topbar-brand img { height: 44px; }
        .topbar-right { display: flex; align-items: center; gap: 20px; }
        .user-info { display: flex; flex-direction: column; align-items: flex-end; }
        .user-name { font-weight: 600; font-size: 13px; color: var(--text-main); }
        .user-role { font-size: 11px; color: var(--text-muted); font-weight: 500; text-transform: uppercase; letter-spacing: 0.5px; }
        .user-avatar { width: 36px; height: 36px; border-radius: 50%; background-color: var(--primary-light); color: var(--primary); display: flex; align-items: center; justify-content: center; }
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
        .nav-item.active svg { color: var(--primary); }

        /* ÁREA PRINCIPAL */
        .main-content { flex: 1; height: 100%; overflow-y: auto; padding: 32px 40px; }
        .page-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 24px; }
        .page-title { font-size: 24px; font-weight: 700; color: var(--text-main); margin-bottom: 6px; letter-spacing: -0.5px; }
        .page-subtitle { font-size: 14px; color: var(--text-muted); }

        .btn-primary {
            background: var(--primary); color: white; border: none; padding: 10px 18px;
            border-radius: var(--radius-md); font-size: 14px; font-weight: 500;
            display: inline-flex; align-items: center; gap: 8px; cursor: pointer; transition: 0.2s;
        }
        .btn-primary:hover { background: #1d4ed8; }

        .panel {
            background: var(--bg-surface); border-radius: var(--radius-lg); padding: 24px;
            box-shadow: var(--shadow-sm); border: 1px solid var(--border-light);
        }

        .table-controls { display: flex; justify-content: space-between; margin-bottom: 16px; }
        .search-box {
            display: flex; align-items: center; gap: 8px; border: 1px solid var(--border-light);
            padding: 8px 12px; border-radius: var(--radius-md); width: 300px;
        }
        .search-box input { border: none; outline: none; width: 100%; font-size: 14px; }
        .search-box svg { color: var(--text-muted); }

        .data-table { width: 100%; border-collapse: separate; border-spacing: 0; }
        .data-table th, .data-table td { padding: 14px 12px; text-align: left; border-bottom: 1px solid var(--border-light); font-size: 13px; }
        .data-table th { font-size: 11px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; padding-bottom: 8px; letter-spacing: 0.5px; }
        .data-table tr:last-child td { border-bottom: none; }
        
        .badge { padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; display: inline-flex; align-items: center; }
        .badge-admin { background: #fee2e2; color: #b91c1c; }
        .badge-medico { background: #e0f2fe; color: #0369a1; }
        .badge-paciente { background: #f1f5f9; color: #475569; }
        .badge-personal { background: #fef3c7; color: #b45309; }

        .status-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; margin-right: 6px; }
        .status-active { background: #10b981; }
        .status-inactive { background: #ef4444; }

        .btn-action { background: none; border: none; cursor: pointer; color: var(--text-muted); padding: 4px; transition: 0.2s; }
        .btn-action:hover { color: var(--primary); }

        /* MODAL STYLES */
        .modal-overlay {
            position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
            background: rgba(15, 23, 42, 0.5); backdrop-filter: blur(4px);
            display: flex; align-items: center; justify-content: center;
            opacity: 0; visibility: hidden; transition: all 0.3s ease; z-index: 100;
        }
        .modal-overlay.active { opacity: 1; visibility: visible; }
        .modal-content {
            background: var(--bg-surface); width: 100%; max-width: 500px;
            border-radius: var(--radius-lg); box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
            transform: translateY(20px); transition: all 0.3s ease;
        }
        .modal-overlay.active .modal-content { transform: translateY(0); }
        .modal-header {
            padding: 24px; border-bottom: 1px solid var(--border-light);
            display: flex; justify-content: space-between; align-items: center;
        }
        .modal-title { font-size: 18px; font-weight: 700; color: var(--text-main); }
        .btn-close { background: none; border: none; cursor: pointer; color: var(--text-muted); }
        .modal-body { padding: 24px; }
        .form-group { margin-bottom: 16px; }
        .form-label { display: block; font-size: 13px; font-weight: 600; color: var(--text-main); margin-bottom: 8px; }
        .form-control {
            width: 100%; padding: 10px 12px; border: 1px solid var(--border-light);
            border-radius: var(--radius-md); font-size: 14px; outline: none; transition: 0.2s;
        }
        .form-control:focus { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-light); }
        .modal-footer {
            padding: 16px 24px; border-top: 1px solid var(--border-light);
            display: flex; justify-content: flex-end; gap: 12px; background: #f8fafc; border-radius: 0 0 var(--radius-lg) var(--radius-lg);
        }
        .btn-secondary {
            background: white; border: 1px solid var(--border-light); padding: 10px 18px;
            border-radius: var(--radius-md); font-size: 14px; font-weight: 500; cursor: pointer;
        }
        .btn-secondary:hover { background: #f1f5f9; }

        /* ANIMACIONES CAMPOS DINÁMICOS */
        .dynamic-wrapper {
            display: grid;
            grid-template-rows: 0fr;
            transition: grid-template-rows 0.35s cubic-bezier(0.4, 0, 0.2, 1);
        }
        .dynamic-wrapper.show {
            grid-template-rows: 1fr;
        }
        .dynamic-inner {
            overflow: hidden;
            opacity: 0;
            transition: opacity 0.35s ease, transform 0.35s ease;
            transform: translateY(-10px);
        }
        .dynamic-wrapper.show .dynamic-inner {
            opacity: 1;
            transform: translateY(0);
        }

        /* NEW METRICS AND FILTERS */
        .metrics-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px; margin-bottom: 24px; }
        .metric-card { background: var(--bg-surface); padding: 20px; border-radius: var(--radius-lg); box-shadow: 0 1px 3px rgba(0,0,0,0.05); border: 1px solid var(--border-light); display: flex; flex-direction: column; gap: 8px; transition: 0.2s; }
        .metric-card:hover { transform: translateY(-2px); box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); }
        .metric-title { font-size: 12px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; display: flex; justify-content: space-between; align-items: center; }
        .metric-value { font-size: 28px; font-weight: 700; color: var(--text-main); line-height: 1; }

        .search-bar { display: flex; gap: 12px; margin-bottom: 24px; }
        .search-bar .form-control { flex: 1; }
        .search-bar select.form-control { flex: 0 0 200px; }
        
        .user-meta { font-size: 11px; color: var(--text-muted); margin-top: 4px; display: flex; align-items: center; gap: 4px; }
    </style>
</head>
<body>

    <header class="topbar">
        <div class="topbar-brand">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>
        <div class="topbar-right">
            <div class="user-info">
                <span class="user-name"><c:out value="${sessionScope.nombreUsuario}" default="Roberto Vargas" /></span>
                <span class="user-role">Administrador del Sistema</span>
            </div>
            <div class="user-avatar"><i data-lucide="user" style="width: 18px; height: 18px;"></i></div>
            <div style="width: 1px; height: 24px; background: var(--border-light); margin: 0 4px;"></div>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout"><i data-lucide="log-out" style="width: 16px; height: 16px;"></i> Salir</a>
        </div>
    </header>

    <div class="app-body">
        <aside class="sidebar">
            <a href="${pageContext.request.contextPath}/admin/dashboard" class="nav-item">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> Inicio
            </a>
            <a href="${pageContext.request.contextPath}/admin/usuarios" class="nav-item active">
                <i data-lucide="users" style="width: 18px;"></i> Usuarios y Roles
            </a>
            <a href="#" class="nav-item">
                <i data-lucide="building-2" style="width: 18px;"></i> Gestión de Sedes
            </a>
            <a href="#" class="nav-item">
                <i data-lucide="sliders-horizontal" style="width: 18px;"></i> Config. Priorización
            </a>
            <a href="#" class="nav-item">
                <i data-lucide="pie-chart" style="width: 18px;"></i> Métricas
            </a>
            <a href="${pageContext.request.contextPath}/admin/perfil" class="nav-item" style="margin-top: auto; border-top: 1px solid var(--border-light); border-radius: 0; padding-top: 16px;">
                <i data-lucide="user-cog" style="width: 18px;"></i> Mi Perfil
            </a>
        </aside>

        <main class="main-content">
            <div class="page-header">
                <div>
                    <h1 class="page-title">Usuarios y Roles</h1>
                    <p class="page-subtitle">Administra los accesos de médicos, personal y pacientes.</p>
                </div>
                <button class="btn-primary" onclick="abrirModal()">
                    <i data-lucide="plus" style="width: 18px;"></i>
                    Nuevo Usuario
                </button>
            </div>
            
            <!-- METRIC CARDS -->
            <div class="metrics-grid">
                <div class="metric-card">
                    <div class="metric-title">Médicos <i data-lucide="stethoscope" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value">${countMedico}</div>
                </div>
                <div class="metric-card">
                    <div class="metric-title">Administrativos <i data-lucide="briefcase" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value">${countPersonal}</div>
                </div>
                <div class="metric-card">
                    <div class="metric-title">Pacientes <i data-lucide="users" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value">${countPaciente}</div>
                </div>
                <div class="metric-card">
                    <div class="metric-title">Soporte TI <i data-lucide="shield" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value">${countAdmin}</div>
                </div>
            </div>

            <!-- SEARCH AND FILTERS -->
            <div class="search-bar">
                <input type="text" id="filterSearch" class="form-control" placeholder="Buscar por nombre, correo o identificador..." onkeyup="filtrarTabla()">
                <select id="filterRol" class="form-control" onchange="filtrarTabla()">
                    <option value="">Todos los Roles</option>
                    <option value="MEDICO">Médicos</option>
                    <option value="PERSONAL_ADMINISTRATIVO">Personal Administrativo</option>
                    <option value="PACIENTE">Pacientes</option>
                </select>
                <select id="filterEstado" class="form-control" onchange="filtrarTabla()">
                    <option value="">Todos los Estados</option>
                    <option value="1">Activos</option>
                    <option value="0">Inactivos</option>
                </select>
            </div>

            <div class="panel">

                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Usuario</th>
                            <th>Correo Electrónico</th>
                            <th>Rol</th>
                            <th>Estado</th>
                            <th>Fecha Registro</th>
                            <th>Acciones</th>
                        </tr>
                    </thead>
                    <tbody id="userTableBody">
                        <c:forEach var="u" items="${listaUsuarios}">
                            <tr class="user-row" data-rol="${u.rol}" data-estado="${u.activo ? '1' : '0'}">
                                <td>
                                    <div style="font-weight: 500; color: var(--text-main);">
                                        ${u.nombres} ${u.apellidos}
                                    </div>
                                    <c:if test="${u.rol == 'MEDICO' && not empty u.cmp}">
                                        <div class="user-meta">
                                            <span style="color: var(--primary); font-weight: 600;">CMP: ${u.cmp}</span> • ${u.especialidad}
                                        </div>
                                    </c:if>
                                    <c:if test="${u.rol == 'PERSONAL_ADMINISTRATIVO' && not empty u.codigoEmpleado}">
                                        <div class="user-meta">
                                            <span style="color: var(--primary); font-weight: 600;">ID: ${u.codigoEmpleado}</span> • ${u.cargo}
                                        </div>
                                    </c:if>
                                </td>
                                <td>${u.email}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${u.rol eq 'ADMINISTRADOR'}"><span class="badge badge-admin">Admin</span></c:when>
                                        <c:when test="${u.rol eq 'MEDICO'}"><span class="badge badge-medico">Médico</span></c:when>
                                        <c:when test="${u.rol eq 'PACIENTE'}"><span class="badge badge-paciente">Paciente</span></c:when>
                                        <c:when test="${u.rol eq 'PERSONAL_ADMINISTRATIVO'}"><span class="badge badge-personal">Personal</span></c:when>
                                        <c:otherwise><span class="badge badge-paciente">${u.rol}</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:if test="${u.activo}"><span class="status-dot status-active"></span> Activo</c:if>
                                    <c:if test="${not u.activo}"><span class="status-dot status-inactive"></span> Inactivo</c:if>
                                </td>
                                <td>
                                    <fmt:parseDate value="${u.fechaRegistro}" pattern="yyyy-MM-dd'T'HH:mm:ss" var="parsedDate" type="both" />
                                    <fmt:formatDate pattern="dd/MM/yyyy HH:mm" value="${parsedDate}" />
                                </td>
                                <td style="display: flex; gap: 8px;">
                                    <button class="btn-action" title="Editar"><i data-lucide="edit" style="width: 16px;"></i></button>
                                    <c:if test="${u.activo}">
                                        <form action="${pageContext.request.contextPath}/admin/usuarios" method="POST" style="display:inline;">
                                            <input type="hidden" name="action" value="cambiarEstado">
                                            <input type="hidden" name="idUsuario" value="${u.idUsuario}">
                                            <input type="hidden" name="estado" value="false">
                                            <button type="submit" class="btn-action" title="Desactivar" style="color: var(--danger); border-color: var(--danger);"><i data-lucide="ban" style="width: 16px;"></i></button>
                                        </form>
                                    </c:if>
                                    <c:if test="${not u.activo}">
                                        <form action="${pageContext.request.contextPath}/admin/usuarios" method="POST" style="display:inline;">
                                            <input type="hidden" name="action" value="cambiarEstado">
                                            <input type="hidden" name="idUsuario" value="${u.idUsuario}">
                                            <input type="hidden" name="estado" value="true">
                                            <button type="submit" class="btn-action" title="Activar" style="color: #10b981; border-color: #10b981;"><i data-lucide="check-circle" style="width: 16px;"></i></button>
                                        </form>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty listaUsuarios}">
                            <tr>
                                <td colspan="6" style="text-align: center; padding: 32px; color: var(--text-muted);">
                                    <i data-lucide="inbox" style="width: 32px; height: 32px; margin-bottom: 8px; opacity: 0.5;"></i><br>
                                    No hay usuarios registrados
                                </td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </main>
    </div>

    <!-- MODAL NUEVO USUARIO -->
    <div class="modal-overlay" id="modalUsuario">
        <div class="modal-content">
            <div class="modal-header">
                <h3 class="modal-title">Registrar Nuevo Personal</h3>
                <button class="btn-close" onclick="cerrarModal()"><i data-lucide="x"></i></button>
            </div>
            <form action="${pageContext.request.contextPath}/admin/usuarios" method="POST">
                <div class="modal-body">
                    <div style="display: flex; gap: 16px;">
                        <div class="form-group" style="flex: 1;">
                            <label class="form-label">Nombres</label>
                            <input type="text" name="nombres" class="form-control" required>
                        </div>
                        <div class="form-group" style="flex: 1;">
                            <label class="form-label">Apellidos</label>
                            <input type="text" name="apellidos" class="form-control" required>
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Correo Electrónico (Corporativo)</label>
                        <input type="email" name="email" class="form-control" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Rol del Sistema</label>
                        <select name="rol" id="rolSelect" class="form-control" required onchange="toggleCamposDinamicos()">
                            <option value="" disabled selected>Seleccione un rol...</option>
                            <option value="MEDICO">Médico Especialista</option>
                            <option value="PERSONAL_ADMINISTRATIVO">Personal de Admisión</option>
                            <option value="ADMINISTRADOR">Administrador TI</option>
                        </select>
                    </div>

                    <!-- CAMPOS MÉDICO -->
                    <div class="dynamic-wrapper" id="wrapMedico">
                        <div class="dynamic-inner">
                            <div style="padding: 16px; background: var(--bg-body); border-radius: var(--radius-md); margin-bottom: 16px; border: 1px solid var(--border-light);">
                                <h4 style="font-size: 13px; color: var(--primary); margin-bottom: 12px; text-transform: uppercase; letter-spacing: 0.5px;">Detalles Médicos</h4>
                                <div style="display: flex; gap: 16px;">
                                    <div class="form-group" style="flex: 1; margin-bottom: 0;">
                                        <label class="form-label">Colegiatura (CMP)</label>
                                        <input type="text" name="cmp" id="inputCmp" class="form-control" placeholder="Ej. CMP-12345">
                                    </div>
                                    <div class="form-group" style="flex: 1; margin-bottom: 0;">
                                        <label class="form-label">Especialidad</label>
                                        <select name="id_especialidad" id="inputEspecialidad" class="form-control">
                                            <option value="" disabled selected>Seleccione...</option>
                                            <c:forEach var="esp" items="${listaEspecialidades}">
                                                <option value="${esp.id}">${esp.nombre}</option>
                                            </c:forEach>
                                        </select>
                                    </div>
                                </div>
                                <div class="form-group" style="margin-top: 16px; margin-bottom: 0;">
                                    <label class="form-label">Sede Asignada</label>
                                    <select name="id_sede_medico" id="inputSedeMedico" class="form-control">
                                        <option value="" disabled selected>Seleccione...</option>
                                        <c:forEach var="sede" items="${listaSedes}">
                                            <option value="${sede.id}">${sede.nombre}</option>
                                        </c:forEach>
                                    </select>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- CAMPOS PERSONAL ADMINISTRATIVO -->
                    <div class="dynamic-wrapper" id="wrapPersonal">
                        <div class="dynamic-inner">
                            <div style="padding: 16px; background: var(--bg-body); border-radius: var(--radius-md); margin-bottom: 16px; border: 1px solid var(--border-light);">
                                <h4 style="font-size: 13px; color: var(--primary); margin-bottom: 12px; text-transform: uppercase; letter-spacing: 0.5px;">Detalles Administrativos</h4>
                                
                                <div style="display: flex; gap: 16px;">
                                    <div class="form-group" style="flex: 1; margin-bottom: 0;">
                                        <label class="form-label">Código Empleado</label>
                                        <input type="text" class="form-control" value="Autogenerado" disabled style="background: #f1f5f9; color: #94a3b8; cursor: not-allowed; font-style: italic;">
                                    </div>
                                    <div class="form-group" style="flex: 1; margin-bottom: 0;">
                                        <label class="form-label">Cargo Administrativo</label>
                                        <input type="text" name="cargo" id="inputCargo" class="form-control" placeholder="Ej. Admisionista">
                                    </div>
                                </div>
                                <div class="form-group" style="margin-top: 16px; margin-bottom: 0;">
                                    <label class="form-label">Sede Asignada</label>
                                    <select name="id_sede_personal" id="inputSedePersonal" class="form-control">
                                        <option value="" disabled selected>Seleccione...</option>
                                        <c:forEach var="sede" items="${listaSedes}">
                                            <option value="${sede.id}">${sede.nombre}</option>
                                        </c:forEach>
                                    </select>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Contraseña Temporal</label>
                        <input type="password" name="password" class="form-control" required>
                        <small style="color: var(--text-muted); font-size: 11px; margin-top: 4px; display: block;">
                            El usuario deberá cambiar esta contraseña en su primer inicio de sesión.
                        </small>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn-secondary" onclick="cerrarModal()">Cancelar</button>
                    <button type="submit" class="btn-primary">Guardar Registro</button>
                </div>
            </form>
        </div>
    </div>

    <script>
        lucide.createIcons();

        const modal = document.getElementById('modalUsuario');

        function filtrarTabla() {
            const search = document.getElementById('filterSearch').value.toLowerCase();
            const rol = document.getElementById('filterRol').value;
            const estado = document.getElementById('filterEstado').value;
            
            const rows = document.querySelectorAll('#userTableBody .user-row');
            
            rows.forEach(row => {
                const text = row.innerText.toLowerCase();
                const rowRol = row.getAttribute('data-rol');
                const rowEstado = row.getAttribute('data-estado');
                
                const matchSearch = search === "" || text.includes(search);
                const matchRol = rol === "" || rowRol === rol;
                const matchEstado = estado === "" || rowEstado === estado;
                
                if (matchSearch && matchRol && matchEstado) {
                    row.style.display = "";
                } else {
                    row.style.display = "none";
                }
            });
        }

        function abrirModal() {
            modal.classList.add('active');
        }

        function cerrarModal() {
            modal.classList.remove('active');
        }

        function toggleCamposDinamicos() {
            const rol = document.getElementById('rolSelect').value;
            const wrapMedico = document.getElementById('wrapMedico');
            const wrapPersonal = document.getElementById('wrapPersonal');
            
            // Ocultar todos y limpiar required
            wrapMedico.classList.remove('show');
            document.getElementById('inputCmp').required = false;
            document.getElementById('inputEspecialidad').required = false;
            document.getElementById('inputSedeMedico').required = false;

            wrapPersonal.classList.remove('show');
            document.getElementById('inputCargo').required = false;
            document.getElementById('inputSedePersonal').required = false;

            if (rol === 'MEDICO') {
                wrapMedico.classList.add('show');
                document.getElementById('inputCmp').required = true;
                document.getElementById('inputEspecialidad').required = true;
                document.getElementById('inputSedeMedico').required = true;
            } else if (rol === 'PERSONAL_ADMINISTRATIVO') {
                wrapPersonal.classList.add('show');
                document.getElementById('inputCargo').required = true;
                document.getElementById('inputSedePersonal').required = true;
            }
        }

        // Cerrar al hacer clic fuera
        modal.addEventListener('click', (e) => {
            if (e.target === modal) cerrarModal();
        });
    </script>
</body>
</html>
