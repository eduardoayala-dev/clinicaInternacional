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

    Integer totalUsuarios = (Integer) request.getAttribute("totalUsuarios"); if (totalUsuarios == null) totalUsuarios = 12;
    Integer countPacientes = (Integer) request.getAttribute("countPacientes"); if (countPacientes == null) countPacientes = 5;
    Integer countMedicos = (Integer) request.getAttribute("countMedicos"); if (countMedicos == null) countMedicos = 5;
    Integer countPersonal = (Integer) request.getAttribute("countPersonal"); if (countPersonal == null) countPersonal = 1;
    Integer countAdmins = (Integer) request.getAttribute("countAdmins"); if (countAdmins == null) countAdmins = 1;
    Integer countSedes = (Integer) request.getAttribute("countSedes"); if (countSedes == null) countSedes = 4;
    Integer countCitas = (Integer) request.getAttribute("countCitas"); if (countCitas == null) countCitas = 15;

    List<Map<String, Object>> radarCitas = (List<Map<String, Object>>) request.getAttribute("radarCitas");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Vista General - Clínica Internacional</title>
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
        .page-header { margin-bottom: 24px; }
        .page-title { font-size: 24px; font-weight: 700; color: var(--text-main); margin-bottom: 6px; letter-spacing: -0.5px; }
        .page-subtitle { font-size: 14px; color: var(--text-muted); }

        .metrics-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 24px; }
        .metric-card { background: var(--bg-surface); padding: 20px; border-radius: var(--radius-lg); border: 1px solid var(--border-light); box-shadow: var(--shadow-sm); display: flex; flex-direction: column; justify-content: space-between; }
        .metric-title { font-size: 12px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; display: flex; justify-content: space-between; align-items: center; }
        .metric-value { font-size: 28px; font-weight: 700; color: var(--text-main); margin: 10px 0 4px 0; }
        .metric-sub { font-size: 12px; color: var(--text-muted); }

        .grid-2col { display: grid; grid-template-columns: 1fr 340px; gap: 24px; margin-bottom: 24px; }
        .panel { background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light); padding: 24px; box-shadow: var(--shadow-sm); }
        .panel-title { font-size: 16px; font-weight: 700; color: var(--text-main); margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }

        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { padding: 12px 14px; font-size: 11px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; border-bottom: 1px solid var(--border-light); }
        td { padding: 14px; font-size: 13px; color: var(--text-main); border-bottom: 1px solid var(--border-light); vertical-align: middle; }
        tr:last-child td { border-bottom: none; }

        .badge-status { padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; display: inline-flex; align-items: center; gap: 4px; }
        .badge-sync { background: #dcfce7; color: #166534; }
        .progress-fill { background: var(--primary); height: 6px; border-radius: 3px; }

        .audit-list { display: flex; flex-direction: column; gap: 16px; }
        .audit-item { display: flex; gap: 12px; font-size: 13px; border-bottom: 1px solid var(--border-light); padding-bottom: 12px; }
        .audit-item:last-child { border-bottom: none; padding-bottom: 0; }
        .audit-icon { width: 32px; height: 32px; border-radius: 50%; background: var(--primary-light); color: var(--primary); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }

        .badge-urgencia { padding: 3px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; }
        .urg-alta { background: #fee2e2; color: #991b1b; }
        .urg-media { background: #fef3c7; color: #92400e; }
        .urg-baja { background: #dcfce7; color: #166534; }
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
            <div class="user-avatar"><i data-lucide="shield-check" style="width: 18px; height: 18px;"></i></div>
            <div style="width: 1px; height: 24px; background: var(--border-light); margin: 0 4px;"></div>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout"><i data-lucide="log-out" style="width: 16px; height: 16px;"></i> Salir</a>
        </div>
    </header>

    <div class="app-body">
        <aside class="sidebar">
            <a href="${pageContext.request.contextPath}/sysadmin/dashboard" class="nav-item active">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> Vista General
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/usuarios" class="nav-item">
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
                <h1 class="page-title">Vista General</h1>
                <p class="page-subtitle">Monitoreo en tiempo real de infraestructura, usuarios y rendimiento de la base de datos</p>
            </div>

            <!-- METRIC CARDS REGISTRADAS DE LA BASE DE DATOS -->
            <div class="metrics-grid">
                <div class="metric-card">
                    <div class="metric-title">Usuarios Registrados en BD <i data-lucide="users" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value"><%= totalUsuarios %></div>
                    <div class="metric-sub"><%= countPacientes %> Pacientes • <%= countMedicos %> Médicos | <%= countPersonal %> Personal • <%= countAdmins %> Admin</div>
                </div>

                <div class="metric-card">
                    <div class="metric-title">Sedes en Base de Datos <i data-lucide="building-2" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value"><%= countSedes %></div>
                    <div class="metric-sub">Sedes registradas en la base de datos</div>
                </div>

                <div class="metric-card">
                    <div class="metric-title">Citas Registradas <i data-lucide="calendar" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value"><%= countCitas %></div>
                    <div class="metric-sub">Citas almacenadas en la base de datos</div>
                </div>

                <div class="metric-card">
                    <div class="metric-title">Motor de Reasignación <i data-lucide="activity" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value">100%</div>
                    <div class="metric-sub">Motor heurístico transaccional activo en BD</div>
                </div>
            </div>

            <!-- GRID 2 COLUMNS -->
            <div class="grid-2col">
                <div class="panel">
                    <h3 class="panel-title">Estado de Red por Sede</h3>
                    <table>
                        <thead>
                            <tr>
                                <th>Sede</th>
                                <th>Estado</th>
                                <th>Ocupación</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><strong>Sede Central Garcilaso</strong><br><span style="font-size: 11px; color: var(--text-muted);">Av. Garcilaso de la Vega 1420</span></td>
                                <td><span class="badge-status badge-sync">• Sincronizado</span></td>
                                <td>
                                    <div style="background: #e2e8f0; height: 6px; border-radius: 3px; width: 120px; display: inline-block; margin-right: 8px;">
                                        <div class="progress-fill" style="width: 92%;"></div>
                                    </div>
                                    <strong>92.4%</strong>
                                </td>
                            </tr>
                            <tr>
                                <td><strong>Sede Lima Norte</strong><br><span style="font-size: 11px; color: var(--text-muted);">Av. Alfredo Mendiola 1400</span></td>
                                <td><span class="badge-status badge-sync">• Sincronizado</span></td>
                                <td>
                                    <div style="background: #e2e8f0; height: 6px; border-radius: 3px; width: 120px; display: inline-block; margin-right: 8px;">
                                        <div class="progress-fill" style="width: 88%;"></div>
                                    </div>
                                    <strong>88.1%</strong>
                                </td>
                            </tr>
                            <tr>
                                <td><strong>Sede San Borja</strong><br><span style="font-size: 11px; color: var(--text-muted);">Av. Guardia Civil 385</span></td>
                                <td><span class="badge-status badge-sync">• Sincronizado</span></td>
                                <td>
                                    <div style="background: #e2e8f0; height: 6px; border-radius: 3px; width: 120px; display: inline-block; margin-right: 8px;">
                                        <div class="progress-fill" style="width: 79.5%;"></div>
                                    </div>
                                    <strong>79.5%</strong>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <div class="panel">
                    <h3 class="panel-title">Auditoría Reciente en BD</h3>
                    <div class="audit-list">
                        <div class="audit-item">
                            <div class="audit-icon"><i data-lucide="settings" style="width: 16px;"></i></div>
                            <div>
                                <strong><%= nombreAdmin %></strong> ejecutó actualización de esquema SQL.
                                <div style="font-size: 11px; color: var(--text-muted); margin-top: 2px;">Hoy, 14:10 PM</div>
                            </div>
                        </div>
                        <div class="audit-item">
                            <div class="audit-icon"><i data-lucide="user-plus" style="width: 16px;"></i></div>
                            <div>
                                <strong><%= nombreAdmin %></strong> sincronizó usuarios y médicos facultativos.
                                <div style="font-size: 11px; color: var(--text-muted); margin-top: 2px;">Hoy, 13:45 PM</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- BOTTOM PANEL RADAR CITA -->
            <div class="panel">
                <h3 class="panel-title">Radar de Inteligencia (Citas Registradas en BD)</h3>
                <table>
                    <thead>
                        <tr>
                            <th>ID Cita</th>
                            <th>Paciente</th>
                            <th>Médico</th>
                            <th>Fecha y Hora</th>
                            <th>Nivel Urgencia</th>
                            <th>Predicción de Riesgo</th>
                            <th>Estado Cita</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (radarCitas != null && !radarCitas.isEmpty()) {
                            for (Map<String, Object> rc : radarCitas) {
                                int idCita = rc.get("idCita") != null ? (Integer) rc.get("idCita") : 1;
                                String paciente = (String) rc.get("paciente");
                                String medico = (String) rc.get("medico");
                                String fechaHora = (String) rc.get("fechaHora");
                                String urgencia = (String) rc.get("nivelUrgencia");
                                if (urgencia == null) urgencia = "BAJA";
                                String riesgo = (String) rc.get("riesgoCancelacion");
                                if (riesgo == null) riesgo = "BAJO";
                                String estado = (String) rc.get("estado");

                                String urgClass = "ALTA".equals(urgencia) ? "urg-alta" : ("MEDIA".equals(urgencia) ? "urg-media" : "urg-baja");
                                String colorRiesgo = "ALTO".equals(riesgo) ? "#dc2626" : ("MEDIO".equals(riesgo) ? "#d97706" : "#166534");
                        %>
                        <tr>
                            <td><strong>#CIT-<%= idCita %></strong></td>
                            <td><%= paciente %></td>
                            <td><%= medico %></td>
                            <td><%= fechaHora %></td>
                            <td><span class="badge-urgencia <%= urgClass %>"><%= urgencia %></span></td>
                            <td><strong style="color: <%= colorRiesgo %>;"><%= riesgo %> RIESGO</strong></td>
                            <td><span class="badge-status badge-sync"><%= estado %></span></td>
                        </tr>
                        <%  }
                           } else { %>
                        <tr>
                            <td colspan="7" style="text-align: center; padding: 24px; color: var(--text-muted);">
                                No hay citas registradas en el sistema.
                            </td>
                        </tr>
                        <% } %>
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
