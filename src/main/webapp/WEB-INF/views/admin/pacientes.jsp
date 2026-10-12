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

    List<Map<String, Object>> listaPacientes = (List<Map<String, Object>>) request.getAttribute("listaPacientes");
    Integer totalPacientes = (Integer) request.getAttribute("totalPacientes");
    if (totalPacientes == null) totalPacientes = 1250;
    String calidadCanales = (String) request.getAttribute("calidadCanales");
    if (calidadCanales == null) calidadCanales = "96.4%";
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Padrón Maestro de Pacientes - Clínica Internacional</title>
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

        .header-actions { display: flex; gap: 12px; }

        .filters-panel {
            background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 18px 24px; box-shadow: var(--shadow-sm); margin-bottom: 24px;
            display: flex; gap: 14px; align-items: center; flex-wrap: wrap;
        }
        .search-box { flex: 1; min-width: 280px; position: relative; }
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
            display: inline-flex; align-items: center; gap: 6px; transition: all 0.2s; text-decoration: none;
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

        .tag-zone {
            display: inline-block; padding: 2px 6px; border-radius: 4px; font-size: 11px; font-weight: 500;
            background: #f1f5f9; color: #475569; margin-top: 2px;
        }

        .metrics-grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
        .metric-card {
            background: var(--bg-surface); padding: 20px; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            box-shadow: var(--shadow-sm); display: flex; flex-direction: column; justify-content: space-between;
        }
        .metric-title { font-size: 11px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; }
        .metric-value { font-size: 26px; font-weight: 700; color: var(--text-main); margin: 8px 0 2px 0; }
        .metric-sub { font-size: 12px; color: var(--text-muted); }

        .table-footer {
            display: flex; justify-content: space-between; align-items: center; margin-top: 16px; font-size: 13px; color: var(--text-muted);
        }
        .pagination-btns { display: flex; gap: 8px; }
        .btn-page { padding: 6px 12px; border-radius: var(--radius-md); border: 1px solid var(--border-light); background: #fff; font-size: 12px; cursor: pointer; }
        .btn-page:hover { background: var(--bg-body); }
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
            <a href="${pageContext.request.contextPath}/admin/medicos" class="nav-item">
                <i data-lucide="stethoscope" style="width: 18px;"></i> Médicos y Especialidades
            </a>
            <a href="${pageContext.request.contextPath}/admin/pacientes" class="nav-item active">
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
                    <h1 class="page-title">Padrón Maestro de Pacientes</h1>
                    <p class="page-subtitle">Registro demográfico, dirección/zona de residencia y canales de contacto para citación</p>
                </div>
                <div class="header-actions">
                    <button class="btn-filter"><i data-lucide="grid" style="width: 14px;"></i> Vista: Matriz Nominal</button>
                    <a href="${pageContext.request.contextPath}/admin/export?format=csv" class="btn-filter"><i data-lucide="download" style="width: 14px;"></i> Exportar CSV</a>
                </div>
            </div>

            <!-- FILTROS -->
            <form method="GET" action="${pageContext.request.contextPath}/admin/pacientes" class="filters-panel">
                <div class="search-box">
                    <i data-lucide="search"></i>
                    <input type="text" name="query" placeholder="Buscar por DNI, N° de Historia Clínica o Apellidos...">
                </div>
                <select name="zona" class="select-filter">
                    <option value="">Zona/Distrito: TODOS</option>
                    <option value="Norte">Distrito Norte</option>
                    <option value="Central">Distrito Central</option>
                    <option value="Sur">Distrito Sur</option>
                    <option value="Este">Distrito Este</option>
                </select>
                <select name="cobertura" class="select-filter">
                    <option value="">Filtro Cobertura: TODOS (EPS / PARTICULAR / SEGURO)</option>
                    <option value="EPS">EPS Rimac / Pacífico</option>
                    <option value="PARTICULAR">Particular</option>
                    <option value="SEGURO">Seguro Privado</option>
                </select>
                <a href="${pageContext.request.contextPath}/registro-paciente" class="btn-filter btn-primary-action"><i data-lucide="user-plus" style="width: 16px;"></i> + Nuevo Paciente</a>
            </form>

            <!-- TABLA PACIENTES -->
            <div class="panel">
                <table>
                    <thead>
                        <tr>
                            <th>N° HC / DNI</th>
                            <th>Nombre Completo</th>
                            <th>Edad / Fecha Nac.</th>
                            <th>Zona / Dirección Residencia</th>
                            <th>Teléfono / Correo</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (listaPacientes != null && !listaPacientes.isEmpty()) {
                            for (Map<String, Object> p : listaPacientes) {
                                String hc = (String) p.get("hc");
                                String dni = (String) p.get("dni");
                                String nombre = (String) p.get("nombre");
                                String edad = (String) p.get("edad");
                                String fnac = (String) p.get("fnac");
                                String direccion = (String) p.get("direccion");
                                String zona = (String) p.get("zona");
                                String telefono = (String) p.get("telefono");
                                String correo = (String) p.get("correo");
                        %>
                        <tr>
                            <td><strong><%= hc %></strong><br><span style="font-size:11px; color:var(--text-muted);">DNI <%= dni %></span></td>
                            <td><strong><%= nombre %></strong></td>
                            <td><%= edad %><br><span style="font-size:11px; color:var(--text-muted);"><%= fnac %></span></td>
                            <td><%= direccion %><br><span class="tag-zone"><%= zona %></span></td>
                            <td><%= telefono %><br><span style="font-size:11px; color:var(--text-muted);"><%= correo %></span></td>
                        </tr>
                        <%  } 
                           } %>
                    </tbody>
                </table>

                <div class="table-footer">
                    <span>Mostrando 1-<%= listaPacientes != null ? listaPacientes.size() : 6 %> de <%= totalPacientes %> pacientes registrados</span>
                    <div class="pagination-btns">
                        <button class="btn-page">&lt; Anterior</button>
                        <button class="btn-page" style="background: var(--primary); color: #fff; border-color: var(--primary);">Página 1 de 209</button>
                        <button class="btn-page">Siguiente &gt;</button>
                    </div>
                </div>
            </div>

            <!-- METRIC CARDS 3 COLUMNS AT BOTTOM -->
            <div class="metrics-grid-3">
                <div class="metric-card">
                    <div class="metric-title">Total Pacientes Registrados</div>
                    <div class="metric-value"><%= String.format("%,d", totalPacientes) %></div>
                    <div class="metric-sub">Padrón base actualizado al corte de hoy 08:00</div>
                </div>
                <div class="metric-card">
                    <div class="metric-title">Calidad Canales de Contacto</div>
                    <div class="metric-value"><%= calidadCanales %></div>
                    <div class="metric-sub">1,205 pacientes con teléfono y/o correo confirmados</div>
                </div>
                <div class="metric-card">
                    <div class="metric-title">Distribución Territorial</div>
                    <div class="metric-value" style="font-size: 16px; margin: 12px 0 6px 0; font-weight: 600; line-height: 1.4;">Residencia Norte: 42% | Central: 31% | Sur: 18% | Este: 9%</div>
                    <div class="metric-sub">Asignación primaria para priorización de turnos</div>
                </div>
            </div>
        </main>
    </div>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
