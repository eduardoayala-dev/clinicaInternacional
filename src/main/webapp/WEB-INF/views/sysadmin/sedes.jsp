<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreAdmin = (String) request.getAttribute("nombreAdmin");
    if (nombreAdmin == null && usuario != null) {
        nombreAdmin = usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreAdmin == null) nombreAdmin = "Roberto Vargas";
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gestión de Sedes - Clínica Internacional</title>
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

        /* METRIC CARDS */
        .metrics-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 24px; }
        .metric-card { background: var(--bg-surface); padding: 20px; border-radius: var(--radius-lg); border: 1px solid var(--border-light); box-shadow: var(--shadow-sm); display: flex; flex-direction: column; justify-content: space-between; }
        .metric-title { font-size: 12px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; display: flex; justify-content: space-between; align-items: center; }
        .metric-value { font-size: 28px; font-weight: 700; color: var(--text-main); margin: 10px 0 4px 0; }
        .metric-sub { font-size: 12px; color: var(--text-muted); }

        /* FILTER BAR */
        .filter-card {
            background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px 24px; margin-bottom: 24px; box-shadow: var(--shadow-sm);
            display: flex; justify-content: space-between; align-items: center; gap: 16px;
        }
        .search-box {
            display: flex; align-items: center; gap: 8px; border: 1px solid var(--border-light);
            padding: 10px 16px; border-radius: 20px; background: #ffffff; flex: 1; max-width: 400px;
        }
        .search-box input { border: none; outline: none; width: 100%; font-size: 14px; }
        .btn-add-sede {
            background: var(--primary); color: #ffffff; border: none; padding: 10px 22px; border-radius: 20px;
            font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 12px rgba(37,99,235,0.2);
            display: inline-flex; align-items: center; gap: 8px; text-decoration: none;
        }

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

        .badge-status { padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; display: inline-block; }
        .badge-activa { background: #dcfce7; color: #166534; }
        .badge-mantenimiento { background: #fef3c7; color: #92400e; }

        .btn-edit-pill {
            padding: 6px 14px; border-radius: 20px; font-size: 13px; font-weight: 600; text-decoration: none;
            transition: all 0.2s; display: inline-flex; align-items: center; gap: 6px; background: #eff6ff; color: #2563eb; border: none; cursor: pointer;
        }
        .btn-edit-pill:hover { background: #2563eb; color: #ffffff; }

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
                <span class="user-role">ADMINISTRADOR DEL SISTEMA</span>
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
            <a href="${pageContext.request.contextPath}/sysadmin/usuarios" class="nav-item">
                <i data-lucide="users" style="width: 18px;"></i> Usuarios y Roles
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/sedes" class="nav-item active">
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
                <h1 class="page-title">Gestión de Sedes Clínicas</h1>
                <p class="page-subtitle">Configuración de infraestructura física, asignación de consultorios y habilitación territorial.</p>
            </div>

            <!-- METRIC CARDS -->
            <div class="metrics-grid">
                <div class="metric-card">
                    <div class="metric-title">Consultorios <i data-lucide="layout-grid" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value">98</div>
                    <div class="metric-sub">4 Sedes Activas en Red</div>
                </div>

                <div class="metric-card">
                    <div class="metric-title">Cartera Médica <i data-lucide="briefcase" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value">22</div>
                    <div class="metric-sub">Especialidades Homologadas</div>
                </div>

                <div class="metric-card">
                    <div class="metric-title">Disponibilidad Boxes <i data-lucide="check-circle-2" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value">92.8%</div>
                    <div class="metric-sub">91 / 98 Habilitados en Línea</div>
                </div>

                <div class="metric-card">
                    <div class="metric-title">Mantenimiento <i data-lucide="alert-triangle" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value">1</div>
                    <div class="metric-sub">Sede Este (Preventivo)</div>
                </div>
            </div>

            <!-- FILTER CARD -->
            <div class="filter-card">
                <div class="search-box">
                    <i data-lucide="search" style="width: 18px; color: var(--text-muted);"></i>
                    <input type="text" placeholder="Buscar sede por nombre o dirección...">
                </div>
                <button class="btn-add-sede">
                    <i data-lucide="plus" style="width: 18px;"></i> + Nueva Sede
                </button>
            </div>

            <!-- TABLE PANEL -->
            <div class="panel">
                <table>
                    <thead>
                        <tr>
                            <th>Código Sede</th>
                            <th>Nombre de la Sede</th>
                            <th>Dirección / Zona Geográfica</th>
                            <th>Consultorios</th>
                            <th>Especialidades</th>
                            <th>Estado</th>
                            <th>Acciones</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><strong>SD-001</strong></td>
                            <td>Sede Central - Metropolitan</td>
                            <td>Av. Principal 1238 - Cercado de Lima</td>
                            <td>40 consultorios</td>
                            <td>22 especialidades</td>
                            <td><span class="badge-status badge-activa">ACTIVA</span></td>
                            <td><button class="btn-edit-pill"><i data-lucide="edit-2" style="width: 14px;"></i> Editar</button></td>
                        </tr>
                        <tr>
                            <td><strong>SD-002</strong></td>
                            <td>Sede Norte - Valle</td>
                            <td>Av. Alfredo Mendiola 1400 - Los Olivos</td>
                            <td>24 consultorios</td>
                            <td>14 especialidades</td>
                            <td><span class="badge-status badge-activa">ACTIVA</span></td>
                            <td><button class="btn-edit-pill"><i data-lucide="edit-2" style="width: 14px;"></i> Editar</button></td>
                        </tr>
                        <tr>
                            <td><strong>SD-003</strong></td>
                            <td>Sede Sur - Parque Industrial</td>
                            <td>Av. Pedro Miotta 820 - San Juan de Miraflores</td>
                            <td>18 consultorios</td>
                            <td>11 especialidades</td>
                            <td><span class="badge-status badge-activa">ACTIVA</span></td>
                            <td><button class="btn-edit-pill"><i data-lucide="edit-2" style="width: 14px;"></i> Editar</button></td>
                        </tr>
                        <tr>
                            <td><strong>SD-004</strong></td>
                            <td>Sede Este - Cordillera</td>
                            <td>Av. Javier Prado Este 3100 - La Molina</td>
                            <td>16 consultorios</td>
                            <td>09 especialidades</td>
                            <td><span class="badge-status badge-mantenimiento">MANTENIMIENTO</span></td>
                            <td><button class="btn-edit-pill"><i data-lucide="edit-2" style="width: 14px;"></i> Editar</button></td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </main>
    </div>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
