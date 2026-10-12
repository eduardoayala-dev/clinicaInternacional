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
    if (nombrePersonal == null) nombrePersonal = "Lic. Marina Salvatierra";

    List<Map<String, Object>> listaMedicos = (List<Map<String, Object>>) request.getAttribute("listaMedicos");
    Integer totalMedicos = (Integer) request.getAttribute("totalMedicos");
    if (totalMedicos == null) totalMedicos = 84;
    Integer totalActivos = (Integer) request.getAttribute("totalActivos");
    if (totalActivos == null) totalActivos = 79;
    Integer totalLicencia = (Integer) request.getAttribute("totalLicencia");
    if (totalLicencia == null) totalLicencia = 5;

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
    <title>Padrón de Médicos - Clínica Internacional</title>
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

        .app-body { display: flex; height: calc(100vh - 64px); overflow: hidden; }

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

        .main-content { flex: 1; height: 100%; overflow-y: auto; padding: 32px 40px; }
        .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
        .page-title { font-size: 24px; font-weight: 700; color: var(--text-main); margin-bottom: 6px; letter-spacing: -0.5px; }
        .page-subtitle { font-size: 14px; color: var(--text-muted); }

        .flash-alert {
            background: #dcfce7; color: #166534; border: 1px solid #bbf7d0; padding: 14px 18px;
            border-radius: var(--radius-md); font-weight: 600; margin-bottom: 24px; font-size: 14px; display: flex; align-items: center; gap: 10px;
        }

        .filters-panel {
            background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 18px 24px; box-shadow: var(--shadow-sm); margin-bottom: 24px;
            display: flex; gap: 14px; align-items: center; flex-wrap: wrap;
        }
        .search-box { flex: 1; min-width: 260px; position: relative; }
        .search-box input {
            width: 100%; padding: 10px 14px 10px 38px; border: 1px solid var(--border-light);
            border-radius: var(--radius-md); font-size: 13px; outline: none; transition: border-color 0.2s;
        }
        .search-box input:focus { border-color: var(--primary); }
        .search-box i { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: var(--text-muted); width: 16px; }

        .select-filter {
            padding: 10px 14px; border: 1px solid var(--border-light); border-radius: var(--radius-md);
            font-size: 13px; color: var(--text-main); background: #ffffff; outline: none; cursor: pointer;
        }

        .btn-filter {
            padding: 10px 18px; border-radius: var(--radius-md); font-size: 13px; font-weight: 600;
            cursor: pointer; border: 1px solid var(--border-light); background: #ffffff; color: var(--text-main);
            display: inline-flex; align-items: center; gap: 6px; transition: all 0.2s;
        }
        .btn-filter:hover { background: var(--bg-body); }
        .btn-primary-action { background: var(--primary); color: #ffffff; border-color: var(--primary); }
        .btn-primary-action:hover { background: #1d4ed8; }

        .panel { background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light); padding: 24px; box-shadow: var(--shadow-sm); margin-bottom: 24px; }
        
        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { padding: 12px 14px; font-size: 11px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; border-bottom: 1px solid var(--border-light); }
        td { padding: 14px; font-size: 13px; color: var(--text-main); border-bottom: 1px solid var(--border-light); vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover { background: #f8fafc; }

        .tag-specialty {
            display: inline-block; padding: 4px 8px; border-radius: 6px; font-size: 11px; font-weight: 500;
            background: #f1f5f9; color: #334155; margin-right: 4px; margin-bottom: 2px; border: 1px solid #e2e8f0;
        }

        .badge-status { padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; display: inline-block; }
        .badge-activo { background: #dcfce7; color: #166534; }
        .badge-licencia { background: #fef3c7; color: #92400e; }

        .btn-icon-edit {
            padding: 6px 12px; border-radius: var(--radius-md); font-size: 12px; font-weight: 600;
            border: 1px solid var(--border-light); background: #ffffff; color: var(--text-main); cursor: pointer;
            display: inline-flex; align-items: center; gap: 4px; text-decoration: none;
        }
        .btn-icon-edit:hover { background: var(--bg-body); color: var(--primary); }

        .metrics-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 20px; }
        .metric-card {
            background: var(--bg-surface); padding: 18px 20px; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            box-shadow: var(--shadow-sm); display: flex; flex-direction: column; justify-content: space-between;
        }
        .metric-title { font-size: 11px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; }
        .metric-value { font-size: 26px; font-weight: 700; color: var(--text-main); margin: 8px 0 2px 0; }
        .metric-sub { font-size: 12px; color: var(--text-muted); }

        .notice-banner {
            background: #eff6ff; border: 1px solid #bfdbfe; color: #1e40af; padding: 14px 18px;
            border-radius: var(--radius-md); font-size: 12px; line-height: 1.5; display: flex; align-items: flex-start; gap: 10px;
        }

        .table-footer {
            display: flex; justify-content: space-between; align-items: center; margin-top: 16px; font-size: 13px; color: var(--text-muted);
        }
        .pagination-btns { display: flex; gap: 8px; }
        .btn-page { padding: 6px 12px; border-radius: var(--radius-md); border: 1px solid var(--border-light); background: #fff; font-size: 12px; cursor: pointer; }
        .btn-page:hover { background: var(--bg-body); }

        /* MODAL */
        .modal-overlay {
            position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
            background: rgba(15, 23, 42, 0.5); backdrop-filter: blur(4px);
            display: none; align-items: center; justify-content: center; z-index: 100;
        }
        .modal-box {
            background: #ffffff; border-radius: var(--radius-lg); width: 100%; max-width: 520px;
            padding: 28px; box-shadow: 0 20px 25px -5px rgb(0 0 0 / 0.1);
        }
        .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
        .modal-title { font-size: 18px; font-weight: 700; }
        .form-group { margin-bottom: 16px; }
        .form-group label { display: block; font-size: 12px; font-weight: 600; color: var(--text-muted); margin-bottom: 6px; }
        .form-group input, .form-group select {
            width: 100%; padding: 10px 12px; border: 1px solid var(--border-light); border-radius: var(--radius-md); font-size: 13px; outline: none;
        }
        .form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 24px; }
    </style>
</head>
<body>

    <header class="topbar">
        <div class="topbar-brand">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>
        <div class="topbar-right">
            <div class="user-info">
                <span class="user-name"><%= nombrePersonal %></span>
                <span class="user-role">PERSONAL ADMINISTRATIVO</span>
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
            <a href="${pageContext.request.contextPath}/admin/medicos" class="nav-item active">
                <i data-lucide="stethoscope" style="width: 18px;"></i> Médicos y Especialidades
            </a>
            <a href="${pageContext.request.contextPath}/admin/pacientes" class="nav-item">
                <i data-lucide="users" style="width: 18px;"></i> Pacientes
            </a>
            <a href="${pageContext.request.contextPath}/admin/reasignaciones" class="nav-item">
                <i data-lucide="refresh-cw" style="width: 18px;"></i> Reasignaciones
            </a>
            <a href="${pageContext.request.contextPath}/admin/dashboard-operativo" class="nav-item">
                <i data-lucide="bar-chart-2" style="width: 18px;"></i> Dashboard Operativo
            </a>
            <a href="#" class="nav-item" style="margin-top: auto; border-top: 1px solid var(--border-light); border-radius: 0; padding-top: 16px;">
                <i data-lucide="user" style="width: 18px;"></i> Mi Perfil
            </a>
        </aside>

        <main class="main-content">
            <div class="page-header">
                <div>
                    <h1 class="page-title">Padrón de Médicos</h1>
                    <p class="page-subtitle">Mantenimiento de datos operativos de profesionales clínicos y asignación de consultorios</p>
                </div>
                <button onclick="openModal()" class="btn-filter btn-primary-action"><i data-lucide="award" style="width: 16px;"></i> Registro Maestro: Facultativos</button>
            </div>

            <% if (flashMessage != null) { %>
                <div class="flash-alert">
                    <i data-lucide="check-circle" style="width: 18px;"></i>
                    <%= flashMessage %>
                </div>
            <% } %>

            <!-- FILTROS -->
            <form method="GET" action="${pageContext.request.contextPath}/admin/medicos" class="filters-panel">
                <div class="search-box">
                    <i data-lucide="search"></i>
                    <input type="text" name="query" placeholder="Buscar por nombre o colegiatura (CMP)...">
                </div>
                <select name="especialidad" class="select-filter">
                    <option value="">Especialidad: TODAS</option>
                    <option value="Cardiología">Cardiología</option>
                    <option value="Medicina Interna">Medicina Interna</option>
                    <option value="Dermatología">Dermatología</option>
                    <option value="Oftalmología">Oftalmología</option>
                </select>
                <select name="sede" class="select-filter">
                    <option value="">Sede: TODAS LAS SEDES</option>
                    <option value="Lima">Sede Lima</option>
                    <option value="Central">Sede Central</option>
                    <option value="Norte">Sede Norte</option>
                    <option value="Sur">Sede Sur</option>
                </select>
                <select name="estado" class="select-filter">
                    <option value="">Estado: ACTIVO</option>
                    <option value="ACTIVO">Activo</option>
                    <option value="LICENCIA">En Licencia</option>
                </select>
                <a href="${pageContext.request.contextPath}/admin/medicos" class="btn-filter" style="text-decoration:none;"><i data-lucide="rotate-ccw" style="width: 14px;"></i> Reiniciar</a>
                <button type="button" onclick="openModal()" class="btn-filter btn-primary-action"><i data-lucide="plus" style="width: 16px;"></i> Nuevo Médico</button>
            </form>

            <!-- TABLA MÉDICOS -->
            <div class="panel">
                <table>
                    <thead>
                        <tr>
                            <th>CMP / DOC</th>
                            <th>Nombre y Apellidos</th>
                            <th>Especialidad(es)</th>
                            <th>Sede(s) Asignada(s)</th>
                            <th>Horas/Semana</th>
                            <th>Estado</th>
                            <th>Acciones</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (listaMedicos != null && !listaMedicos.isEmpty()) {
                            for (Map<String, Object> m : listaMedicos) {
                                String cmp = (String) m.get("cmp");
                                String dni = (String) m.get("dni");
                                String rne = (String) m.get("rne");
                                String nombre = (String) m.get("nombre");
                                List<String> especialidades = (List<String>) m.get("especialidades");
                                String sede = (String) m.get("sede");
                                String horas = (String) m.get("horas");
                                String est = (String) m.get("estado");
                                String badgeClass = "ACTIVO".equals(est) ? "badge-activo" : "badge-licencia";
                        %>
                        <tr>
                            <td><strong><%= cmp %></strong><br><span style="font-size:11px; color:var(--text-muted);">DNI: <%= dni %> | RNE: <%= rne %></span></td>
                            <td><strong><%= nombre %></strong></td>
                            <td>
                                <% if (especialidades != null) { 
                                    for (String esp : especialidades) { %>
                                        <span class="tag-specialty"><%= esp %></span>
                                <%  } } %>
                            </td>
                            <td><%= sede %></td>
                            <td><%= horas %></td>
                            <td><span class="badge-status <%= badgeClass %>"><%= est %></span></td>
                            <td><button onclick="openModal()" class="btn-icon-edit"><i data-lucide="edit-3" style="width: 14px;"></i> Editar</button></td>
                        </tr>
                        <%  } 
                           } %>
                    </tbody>
                </table>

                <div class="table-footer">
                    <span>Mostrando 1-<%= listaMedicos != null ? listaMedicos.size() : 7 %> de <%= totalMedicos %> médicos registrados</span>
                    <div class="pagination-btns">
                        <button class="btn-page">&lt; Anterior</button>
                        <button class="btn-page" style="background: var(--primary); color: #fff; border-color: var(--primary);">Página 1 de 12</button>
                        <button class="btn-page">Siguiente &gt;</button>
                    </div>
                </div>
            </div>

            <!-- METRIC CARDS 4 COLUMNS AT BOTTOM -->
            <div class="metrics-grid">
                <div class="metric-card">
                    <div class="metric-title">Total en Padrón</div>
                    <div class="metric-value"><%= totalMedicos %></div>
                    <div class="metric-sub">+2 altas en revisión</div>
                </div>
                <div class="metric-card">
                    <div class="metric-title">Total Activos</div>
                    <div class="metric-value"><%= totalActivos %></div>
                    <div class="metric-sub">94.04% disponibilidad</div>
                </div>
                <div class="metric-card">
                    <div class="metric-title">En Licencia / Permiso</div>
                    <div class="metric-value">0<%= totalLicencia %></div>
                    <div class="metric-sub">Reemplazos activados</div>
                </div>
                <div class="metric-card">
                    <div class="metric-title">Especialidades Homologadas</div>
                    <div class="metric-value">18</div>
                    <div class="metric-sub">4 sedes en red</div>
                </div>
            </div>

            <!-- FOOTER NOTICE -->
            <div class="notice-banner">
                <i data-lucide="info" style="width: 20px; color: var(--primary); flex-shrink:0;"></i>
                <div>
                    <strong>Nota institucional:</strong> El alta de credenciales de acceso al sistema la gestiona el Administrador del Sistema (TI). Este formulario registra exclusivamente los datos asistenciales, especialidades homologadas y sede física asignada del facultativo.
                </div>
            </div>
        </main>
    </div>

    <!-- MODAL NUEVO MÉDICO -->
    <div id="modalMedico" class="modal-overlay">
        <div class="modal-box">
            <div class="modal-header">
                <h3 class="modal-title">Registrar Facultativo en Padrón</h3>
                <button onclick="closeModal()" style="background:none; border:none; cursor:pointer;"><i data-lucide="x"></i></button>
            </div>
            <form method="POST" action="${pageContext.request.contextPath}/admin/medicos">
                <input type="hidden" name="action" value="nuevo_medico">
                <div class="form-group">
                    <label>Colegiatura CMP</label>
                    <input type="text" name="cmp" placeholder="e.g. CMP 48920" required>
                </div>
                <div class="form-group">
                    <label>Nombres y Apellidos del Médico</label>
                    <input type="text" name="nombre" placeholder="e.g. Dr. Alejandro Ugarte" required>
                </div>
                <div class="form-group">
                    <label>Especialidad Principal</label>
                    <select name="especialidad" required>
                        <option value="Cardiología">Cardiología</option>
                        <option value="Medicina Interna">Medicina Interna</option>
                        <option value="Dermatología">Dermatología</option>
                        <option value="Oftalmología">Oftalmología</option>
                        <option value="Pediatría">Pediatría</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Sede Física Asignada</label>
                    <select name="sede" required>
                        <option value="Sede Central">Sede Central Garcilaso</option>
                        <option value="Sede Norte">Sede Lima Norte</option>
                        <option value="Sede Sur">Sede San Borja</option>
                    </select>
                </div>
                <div class="form-actions">
                    <button type="button" onclick="closeModal()" class="btn-filter">Cancelar</button>
                    <button type="submit" class="btn-filter btn-primary-action">Guardar Registro</button>
                </div>
            </form>
        </div>
    </div>

    <script>
        lucide.createIcons();
        function openModal() { document.getElementById('modalMedico').style.display = 'flex'; }
        function closeModal() { document.getElementById('modalMedico').style.display = 'none'; }
    </script>
</body>
</html>
