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
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Vista General - Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        :root {
            --primary: #0284c7;
            --primary-hover: #0369a1;
            --primary-light: #e8f1fc;
            --bg-body: #f3f6fb;
            --bg-surface: #ffffff;
            --text-main: #0f172a;
            --text-muted: #64748b;
            --border-light: #edf2f7;
            --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.02);
            --radius-md: 10px;
            --radius-lg: 14px;
        }

        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Inter', sans-serif; }
        body { display: flex; height: 100vh; width: 100vw; background-color: var(--bg-body); overflow: hidden; color: var(--text-main); }

        /* PANEL IZQUIERDO ESTÁNDAR */
        .sidebar {
            width: 250px; min-width: 250px; height: 100vh; background-color: #ffffff;
            border-right: 1px solid var(--border-light); display: flex; flex-direction: column;
            padding: 24px 16px; flex-shrink: 0; z-index: 20;
        }
        .sidebar-brand { display: flex; align-items: center; gap: 12px; padding: 4px 8px 24px 8px; text-decoration: none; }
        .brand-icon-wrapper { width: 38px; height: 38px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
        .brand-icon-img { width: 36px; height: 36px; object-fit: contain; }
        .brand-text-block { display: flex; flex-direction: column; }
        .brand-name { font-size: 14.5px; font-weight: 700; color: var(--text-main); line-height: 1.2; }
        .brand-subtext { font-size: 11.5px; font-weight: 500; color: var(--text-muted); margin-top: 2px; }

        .sidebar-nav { display: flex; flex-direction: column; gap: 4px; margin-top: 8px; flex: 1; }
        .nav-link {
            display: flex; align-items: center; gap: 12px; padding: 11px 16px; border-radius: 12px;
            color: #475569; text-decoration: none; font-size: 13.5px; font-weight: 500; transition: all 0.15s ease;
        }
        .nav-link i { width: 19px; height: 19px; color: #94a3b8; stroke-width: 2; transition: color 0.15s ease; }
        .nav-link:hover { background-color: #f8fafc; color: var(--text-main); }
        .nav-link:hover i { color: #64748b; }
        .nav-link.active { background-color: var(--primary-light); color: var(--primary); font-weight: 600; }
        .nav-link.active i { color: var(--primary); }

        .sidebar-footer { margin-top: auto; padding-top: 16px; }
        .user-capsule { background-color: #edf2f7; border-radius: 14px; padding: 10px 14px; display: flex; align-items: center; gap: 12px; }
        .user-avatar-circle { width: 36px; height: 36px; border-radius: 50%; background-color: var(--primary); color: #ffffff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px; flex-shrink: 0; }
        .user-info-text { display: flex; flex-direction: column; flex: 1; min-width: 0; }
        .user-full-name { font-size: 13px; font-weight: 700; color: var(--text-main); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; line-height: 1.2; }
        .user-role-desc { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
        .btn-sidebar-logout { background: none; border: none; color: var(--text-muted); cursor: pointer; padding: 4px; border-radius: 6px; display: flex; align-items: center; justify-content: center; text-decoration: none; }
        .btn-sidebar-logout:hover { color: #dc2626; background-color: #fee2e2; }
        .btn-sidebar-logout i { width: 18px; height: 18px; }

        .main-content { flex: 1; height: 100vh; overflow-y: auto; padding: 28px 48px 40px 48px; }
        .breadcrumb-trail { font-size: 12.5px; color: var(--text-muted); margin-bottom: 20px; font-weight: 500; }
        .page-header { margin-bottom: 24px; }
        .page-title { font-size: 28px; font-weight: 700; color: var(--text-main); margin-bottom: 6px; letter-spacing: -0.5px; }
        .page-subtitle { font-size: 14px; color: var(--text-muted); }

        .metrics-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; margin-bottom: 24px; }
        .metric-card { background: var(--bg-surface); padding: 22px 24px; border-radius: var(--radius-lg); border: 1px solid var(--border-light); box-shadow: var(--shadow-sm); display: flex; flex-direction: column; justify-content: space-between; }
        .metric-title { font-size: 13px; font-weight: 500; color: var(--text-muted); display: flex; justify-content: space-between; align-items: center; }
        .metric-value { font-size: 28px; font-weight: 700; color: var(--text-main); margin: 10px 0 4px 0; }
        .metric-sub { font-size: 12.5px; color: var(--text-muted); }

        .grid-2col { display: grid; grid-template-columns: 1fr 340px; gap: 24px; margin-bottom: 24px; }
        .panel { background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light); padding: 24px; box-shadow: var(--shadow-sm); }
        .panel-title { font-size: 16px; font-weight: 700; color: var(--text-main); margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }

        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { padding: 14px; font-size: 11px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; border-bottom: 1px solid #f1f5f9; letter-spacing: 0.5px; }
        td { padding: 16px 14px; font-size: 13.5px; color: var(--text-main); border-bottom: 1px solid #f8fafc; vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover { background: #fafcff; }

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

    <!-- PANEL DE LA IZQUIERDA ESTÁNDAR -->
    <aside class="sidebar">
        <a href="${pageContext.request.contextPath}/sysadmin/dashboard" class="sidebar-brand">
            <div class="brand-icon-wrapper">
                <img src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png" 
                     alt="Logo" class="brand-icon-img"
                     onerror="this.src='${pageContext.request.contextPath}/assets/images/logoClinical.png'">
            </div>
            <div class="brand-text-block">
                <span class="brand-name">Clínica Internacional</span>
                <span class="brand-subtext">Portal del administrador</span>
            </div>
        </a>

        <nav class="sidebar-nav">
            <a href="${pageContext.request.contextPath}/sysadmin/dashboard" class="nav-link active">
                <i data-lucide="home"></i>
                <span>Inicio</span>
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/usuarios" class="nav-link">
                <i data-lucide="shield"></i>
                <span>Usuarios y roles</span>
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/sedes" class="nav-link">
                <i data-lucide="building"></i>
                <span>Sedes</span>
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/priorizacion" class="nav-link">
                <i data-lucide="sliders"></i>
                <span>Configuración de priorización</span>
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/analitica" class="nav-link">
                <i data-lucide="trending-up"></i>
                <span>Indicadores y analítica</span>
            </a>
            <a href="${pageContext.request.contextPath}/cuenta/seguridad" class="nav-link"><i data-lucide="shield-check"></i><span>Seguridad de cuenta</span></a>
        </nav>

        <div class="sidebar-footer">
            <div class="user-capsule">
                <div class="user-avatar-circle"><%= nombreAdmin != null && !nombreAdmin.isEmpty() ? ("" + nombreAdmin.charAt(0)).toUpperCase() : "JV" %></div>
                <div class="user-info-text">
                    <span class="user-full-name"><%= nombreAdmin %></span>
                    <span class="user-role-desc">Administrador</span>
                </div>
                <form method="POST" action="${pageContext.request.contextPath}/logout" style="margin:0;display:inline;">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
<button type="submit" class="btn-sidebar-logout" title="Cerrar sesión" style="border:0;cursor:pointer;font-family:inherit;">
                    <i data-lucide="log-out"></i>
                </button>
</form>
            </div>
        </div>
    </aside>

    <main class="main-content">
        <div class="breadcrumb-trail">Administrador / <span>Inicio</span></div>
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

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
