<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreAdmin = (String) request.getAttribute("nombreAdmin");
    if (nombreAdmin == null && usuario != null) {
        nombreAdmin = usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreAdmin == null) nombreAdmin = "Ing. Roberto Vargas";

    String iniciales = "RV";
    if (nombreAdmin != null && !nombreAdmin.trim().isEmpty()) {
        String[] partes = nombreAdmin.trim().split("\\s+");
        if (partes.length >= 2) {
            iniciales = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            iniciales = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Catálogo Maestro de Sedes - Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        :root {
            --primary: #2563eb;
            --primary-hover: #1d4ed8;
            --primary-light: #eff6ff;
            --bg-page: #f0f8ff;
            --bg-card: #ffffff;
            --text-dark: #0f172a;
            --text-gray: #64748b;
            --border-light: #e2e8f0;
            --radius-lg: 16px;
            --radius-md: 12px;
            --sidebar-width: 260px;
        }

        * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Inter', sans-serif; }
        body { background-color: var(--bg-page); color: var(--text-dark); display: flex; height: 100vh; overflow: hidden; }

        .sidebar {
            width: var(--sidebar-width); background-color: var(--bg-card); border-right: 1px solid var(--border-light);
            display: flex; flex-direction: column; padding: 24px 20px; flex-shrink: 0; z-index: 10;
        }
        .brand-container { display: flex; align-items: center; gap: 12px; margin-bottom: 40px; padding-left: 8px; }
        .brand-container img { height: 36px; }
        .nav-menu { display: flex; flex-direction: column; gap: 8px; flex: 1; }
        .nav-item {
            display: flex; align-items: center; gap: 14px; padding: 12px 16px; border-radius: var(--radius-md);
            color: var(--text-gray); text-decoration: none; font-size: 14px; font-weight: 500; transition: all 0.2s;
        }
        .nav-item:hover { background-color: #f8fafc; color: var(--text-dark); }
        .nav-item.active { background-color: #eff6ff; color: var(--primary); font-weight: 600; }
        .user-profile-bottom {
            margin-top: auto; display: flex; align-items: center; gap: 12px; padding: 16px 8px 8px 8px;
            border-top: 1px solid var(--border-light); cursor: pointer; text-decoration: none;
        }
        .avatar-initials {
            display: flex; align-items: center; justify-content: center; border-radius: 50%; font-weight: 600;
            color: var(--primary); background-color: #eff6ff; width: 36px; height: 36px; font-size: 13px; flex-shrink: 0;
        }
        .user-name-sm { font-size: 13px; font-weight: 600; color: var(--text-dark); line-height: 1.3; }
        .user-role-sm { font-size: 11px; color: var(--text-gray); }

        .main-content { flex: 1; padding: 32px 40px; overflow-y: auto; }
        
        .header-row { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
        .header-title { font-size: 26px; font-weight: 700; color: var(--text-dark); margin-bottom: 4px; letter-spacing: -0.5px; }
        .header-subtitle { font-size: 14px; color: var(--text-gray); }

        .btn-bell {
            width: 44px; height: 44px; border-radius: 12px; background: #ffffff; border: 1px solid var(--border-light);
            display: flex; align-items: center; justify-content: center; color: var(--text-gray); position: relative;
            text-decoration: none; transition: all 0.2s;
        }
        .btn-bell:hover { background: #f8fafc; color: var(--text-dark); }

        /* STATS GRID */
        .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
        .stat-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.02);
        }
        .stat-card-title { font-size: 11px; font-weight: 700; color: var(--text-gray); text-transform: uppercase; letter-spacing: 0.5px; display: flex; align-items: center; justify-content: space-between; }
        .stat-card-num { font-size: 26px; font-weight: 700; color: var(--text-dark); margin: 8px 0 4px 0; letter-spacing: -0.5px; }
        .stat-card-sub { font-size: 12px; color: var(--text-gray); font-weight: 500; }

        /* FILTER BAR */
        .filter-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px 24px; margin-bottom: 24px; box-shadow: 0 2px 10px rgba(0,0,0,0.02);
            display: flex; justify-content: space-between; align-items: center; gap: 16px;
        }
        .search-box {
            display: flex; align-items: center; gap: 8px; border: 1px solid var(--border-light);
            padding: 10px 16px; border-radius: var(--radius-md); background: #ffffff; flex: 1; max-width: 400px;
        }
        .search-box input { border: none; outline: none; width: 100%; font-size: 14px; }
        .btn-add-sede {
            background: var(--primary); color: #ffffff; border: none; padding: 10px 22px; border-radius: 30px;
            font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 12px rgba(37,99,235,0.2);
            display: inline-flex; align-items: center; gap: 8px; text-decoration: none;
        }

        /* TABLE CARD */
        .table-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.02);
        }
        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { background: #f8fafc; padding: 16px 20px; font-size: 12px; font-weight: 600; color: var(--text-gray); border-bottom: 1px solid var(--border-light); }
        td { padding: 16px 20px; font-size: 14px; color: var(--text-dark); border-bottom: 1px solid var(--border-light); vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover { background: #f8fafc; }

        .badge-status { padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; display: inline-block; }
        .badge-activa { background: #dcfce7; color: #166534; }
        .badge-mantenimiento { background: #fef3c7; color: #92400e; }

        .btn-edit-tbl {
            padding: 6px 14px; border-radius: 20px; font-size: 12px; font-weight: 600; text-decoration: none;
            transition: all 0.2s; display: inline-flex; align-items: center; gap: 6px; background: var(--primary-light); color: var(--primary);
        }
        .btn-edit-tbl:hover { background: var(--primary); color: #ffffff; }

        @media(max-width:1024px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } .stats-grid { grid-template-columns: repeat(2, 1fr); } }
    </style>
</head>
<body>

    <aside class="sidebar">
        <div class="brand-container">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>
        <nav class="nav-menu">
            <a href="${pageContext.request.contextPath}/sysadmin/dashboard" class="nav-item">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> 1. Inicio
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/usuarios" class="nav-item">
                <i data-lucide="users" style="width: 18px;"></i> 2. Usuarios y Roles
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/sedes" class="nav-item active">
                <i data-lucide="building-2" style="width: 18px;"></i> 3. Sedes
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/priorizacion" class="nav-item">
                <i data-lucide="sliders" style="width: 18px;"></i> 4. Config. Priorización
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/analitica" class="nav-item">
                <i data-lucide="bar-chart-3" style="width: 18px;"></i> 5. Indicadores y Analítica
            </a>
        </nav>
        <a href="${pageContext.request.contextPath}/logout" class="user-profile-bottom">
            <div class="avatar-initials"><%= iniciales %></div>
            <div style="flex: 1;">
                <div class="user-name-sm"><%= nombreAdmin %></div>
                <div class="user-role-sm">Administrador del Sistema</div>
            </div>
            <i data-lucide="log-out" style="width: 16px; color: var(--text-gray);"></i>
        </a>
    </aside>

    <main class="main-content">
        <div class="header-row">
            <div>
                <h1 class="header-title">Catálogo Maestro de Sedes Clínicas</h1>
                <p class="header-subtitle">Configuración de infraestructura física, asignación de consultorios y habilitación territorial de sedes.</p>
            </div>
            <a href="#" class="btn-bell" title="Alertas de Infraestructura">
                <i data-lucide="bell" style="width: 20px;"></i>
            </a>
        </div>

        <!-- STATS GRID -->
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-card-title">TOTAL CONSULTORIOS <i data-lucide="layout-grid" style="width: 16px;"></i></div>
                <div class="stat-card-num">98</div>
                <div class="stat-card-sub">4 Sedes Activas en Red</div>
            </div>

            <div class="stat-card">
                <div class="stat-card-title">CARTERA MÉDICA GLOBAL <i data-lucide="briefcase" style="width: 16px;"></i></div>
                <div class="stat-card-num">22</div>
                <div class="stat-card-sub">Especialidades Homologadas</div>
            </div>

            <div class="stat-card">
                <div class="stat-card-title">DISPONIBILIDAD DE BOXES <i data-lucide="check-circle-2" style="width: 16px;"></i></div>
                <div class="stat-card-num">92.8%</div>
                <div class="stat-card-sub">91 / 98 Habilitados en Línea</div>
            </div>

            <div class="stat-card">
                <div class="stat-card-title">INCIDENTES INFRAESTRUCTURA <i data-lucide="alert-triangle" style="width: 16px;"></i></div>
                <div class="stat-card-num" style="color: #d97706;">01</div>
                <div class="stat-card-sub">Sede Este (Mantenimiento preventivo)</div>
            </div>
        </div>

        <!-- FILTER BAR -->
        <div class="filter-card">
            <div class="search-box">
                <i data-lucide="search" style="width: 18px; color: var(--text-gray);"></i>
                <input type="text" placeholder="Buscar sede por nombre o zona geográfica...">
            </div>
            <button class="btn-add-sede">
                <i data-lucide="plus" style="width: 18px;"></i> + Nueva Sede
            </button>
        </div>

        <!-- TABLE CARD -->
        <div class="table-card">
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
                        <td><strong>#SD-001</strong></td>
                        <td>Sede Central - Metropolitan</td>
                        <td>Av. Principal 1238 - Cercado de Lima</td>
                        <td>40 consultorios</td>
                        <td>22 especialidades</td>
                        <td><span class="badge-status badge-activa">ACTIVA</span></td>
                        <td><a href="#" class="btn-edit-tbl"><i data-lucide="edit" style="width: 14px;"></i> Editar</a></td>
                    </tr>
                    <tr>
                        <td><strong>#SD-002</strong></td>
                        <td>Sede Norte - Valle</td>
                        <td>Av. Alfredo Mendiola 1400 - Los Olivos</td>
                        <td>24 consultorios</td>
                        <td>14 especialidades</td>
                        <td><span class="badge-status badge-activa">ACTIVA</span></td>
                        <td><a href="#" class="btn-edit-tbl"><i data-lucide="edit" style="width: 14px;"></i> Editar</a></td>
                    </tr>
                    <tr>
                        <td><strong>#SD-003</strong></td>
                        <td>Sede Sur - Parque Industrial</td>
                        <td>Av. Pedro Miotta 820 - San Juan de Miraflores</td>
                        <td>18 consultorios</td>
                        <td>11 especialidades</td>
                        <td><span class="badge-status badge-activa">ACTIVA</span></td>
                        <td><a href="#" class="btn-edit-tbl"><i data-lucide="edit" style="width: 14px;"></i> Editar</a></td>
                    </tr>
                    <tr>
                        <td><strong>#SD-004</strong></td>
                        <td>Sede Este - Cordillera</td>
                        <td>Av. Javier Prado Este 3100 - La Molina</td>
                        <td>16 consultorios</td>
                        <td>09 especialidades</td>
                        <td><span class="badge-status badge-mantenimiento">MANTENIMIENTO</span></td>
                        <td><a href="#" class="btn-edit-tbl"><i data-lucide="edit" style="width: 14px;"></i> Editar</a></td>
                    </tr>
                </tbody>
            </table>
        </div>
    </main>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
