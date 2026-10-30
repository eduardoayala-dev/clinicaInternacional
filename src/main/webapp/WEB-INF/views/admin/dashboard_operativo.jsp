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

    List<Map<String, Object>> listaCitasRiesgo = (List<Map<String, Object>>) request.getAttribute("listaCitasRiesgo");
    Integer totalCitas = (Integer) request.getAttribute("totalCitas"); if (totalCitas == null) totalCitas = 420;
    String ocupacionBoxes = (String) request.getAttribute("ocupacionBoxes"); if (ocupacionBoxes == null) ocupacionBoxes = "86.4%";
    Integer cancelaciones = (Integer) request.getAttribute("cancelaciones"); if (cancelaciones == null) cancelaciones = 38;
    Integer reasignacionesExitosas = (Integer) request.getAttribute("reasignacionesExitosas"); if (reasignacionesExitosas == null) reasignacionesExitosas = 32;
    String tiempoEspera = (String) request.getAttribute("tiempoEspera"); if (tiempoEspera == null) tiempoEspera = "31.4 min";

    Integer riesgoBajoCount = (Integer) request.getAttribute("riesgoBajoCount"); if (riesgoBajoCount == null) riesgoBajoCount = 286;
    String riesgoBajoPorc = (String) request.getAttribute("riesgoBajoPorc"); if (riesgoBajoPorc == null) riesgoBajoPorc = "68.1%";
    Integer riesgoMedioCount = (Integer) request.getAttribute("riesgoMedioCount"); if (riesgoMedioCount == null) riesgoMedioCount = 92;
    String riesgoMedioPorc = (String) request.getAttribute("riesgoMedioPorc"); if (riesgoMedioPorc == null) riesgoMedioPorc = "21.9%";
    Integer riesgoAltoCount = (Integer) request.getAttribute("riesgoAltoCount"); if (riesgoAltoCount == null) riesgoAltoCount = 42;
    String riesgoAltoPorc = (String) request.getAttribute("riesgoAltoPorc"); if (riesgoAltoPorc == null) riesgoAltoPorc = "10.0%";
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard Operativo - Clínica Internacional</title>
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
            width: 280px; background-color: var(--bg-surface); border-right: 1px solid var(--border-light);
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

        .header-filters { display: flex; align-items: center; gap: 12px; }
        .select-filter {
            padding: 8px 12px; border: 1px solid var(--border-light); border-radius: var(--radius-md);
            font-size: 13px; color: var(--text-main); background: #ffffff; outline: none; cursor: pointer;
        }

        .btn-refresh {
            padding: 8px 16px; border-radius: var(--radius-md); font-size: 13px; font-weight: 600;
            background: var(--primary); color: #ffffff; border: none; cursor: pointer; display: inline-flex; align-items: center; gap: 6px; text-decoration: none;
        }
        .btn-refresh:hover { background: #1d4ed8; }

        .section-heading {
            font-size: 13px; font-weight: 700; color: var(--text-muted); text-transform: uppercase;
            letter-spacing: 0.5px; margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center;
        }

        .metrics-grid-5 { display: grid; grid-template-columns: repeat(5, 1fr); gap: 16px; margin-bottom: 28px; }
        .metric-card {
            background: var(--bg-surface); padding: 18px; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            box-shadow: var(--shadow-sm); display: flex; flex-direction: column; justify-content: space-between;
        }
        .metric-title { font-size: 11px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; }
        .metric-value { font-size: 24px; font-weight: 700; color: var(--text-main); margin: 8px 0 4px 0; }
        .metric-sub { font-size: 11px; color: var(--text-muted); line-height: 1.4; }
        .metric-tag { font-size: 10px; font-weight: 700; padding: 2px 6px; border-radius: 4px; display: inline-block; margin-top: 6px; }
        .tag-blue { background: #eff6ff; color: #2563eb; }
        .tag-green { background: #dcfce7; color: #166534; }
        .tag-amber { background: #fef3c7; color: #92400e; }

        .risk-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-bottom: 28px; }
        .risk-card {
            background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px; box-shadow: var(--shadow-sm); position: relative; overflow: hidden;
        }
        .risk-card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 4px; }
        .risk-card.low::before { background: #22c55e; }
        .risk-card.med::before { background: #f59e0b; }
        .risk-card.high::before { background: #ef4444; }

        .risk-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
        .risk-title { font-size: 14px; font-weight: 700; }
        .risk-badge { font-size: 11px; font-weight: 700; padding: 2px 8px; border-radius: 12px; }
        .badge-low { background: #dcfce7; color: #166534; }
        .badge-med { background: #fef3c7; color: #92400e; }
        .badge-high { background: #fee2e2; color: #991b1b; }

        .risk-count { font-size: 22px; font-weight: 700; margin-bottom: 8px; }
        .risk-desc { font-size: 12px; color: var(--text-muted); line-height: 1.4; margin-bottom: 12px; }
        .risk-action { font-size: 11px; font-weight: 700; text-transform: uppercase; color: var(--primary); }

        .panel { background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light); padding: 24px; box-shadow: var(--shadow-sm); margin-bottom: 24px; }
        
        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { padding: 12px 14px; font-size: 11px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; border-bottom: 1px solid var(--border-light); }
        td { padding: 14px; font-size: 13px; color: var(--text-main); border-bottom: 1px solid var(--border-light); vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover { background: #f8fafc; }

        .badge-risk-pill { padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: 700; }
        .badge-confirm-pill { padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: 600; }
        .confirm-no { background: #f1f5f9; color: #475569; }
        .confirm-msg { background: #eff6ff; color: #2563eb; }
        .confirm-bounce { background: #fee2e2; color: #991b1b; }
        .confirm-ok { background: #dcfce7; color: #166534; }

        .reports-bar {
            background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px 24px; box-shadow: var(--shadow-sm); display: flex; justify-content: space-between; align-items: center;
        }
        .btn-report {
            padding: 10px 18px; border-radius: var(--radius-md); font-size: 13px; font-weight: 600;
            border: 1px solid var(--border-light); background: #ffffff; color: var(--text-main); cursor: pointer;
            display: inline-flex; align-items: center; gap: 8px; transition: all 0.2s; text-decoration: none;
        }
        .btn-report:hover { background: var(--bg-body); color: var(--primary); }

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
            <form method="POST" action="${pageContext.request.contextPath}/logout?portal=personal" style="margin:0;display:inline;">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
<button type="submit" class="btn-logout" style="border:0;cursor:pointer;font-family:inherit;"><i data-lucide="log-out" style="width: 16px; height: 16px;"></i> Salir</button>
</form>
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
            <a href="${pageContext.request.contextPath}/admin/pacientes" class="nav-item">
                <i data-lucide="users" style="width: 18px;"></i> Pacientes
            </a>
            <a href="${pageContext.request.contextPath}/admin/reasignaciones" class="nav-item">
                <i data-lucide="refresh-cw" style="width: 18px;"></i> Reasignaciones
            </a>
            <a href="${pageContext.request.contextPath}/admin/dashboard-operativo" class="nav-item active">
                <i data-lucide="bar-chart-2" style="width: 18px;"></i> Dashboard Operativo
            </a>
            <a href="#" class="nav-item" style="margin-top: auto; border-top: 1px solid var(--border-light); border-radius: 0; padding-top: 16px;">
                <i data-lucide="user" style="width: 18px;"></i> Mi Perfil
            </a>
        </aside>

        <main class="main-content">
            <div class="page-header">
                <div>
                    <h1 class="page-title">Dashboard Operativo de Admisión y Atención Ambulatoria</h1>
                    <p class="page-subtitle">Consolidado diario de citas, disponibilidad médica en tiempo real y riesgo de inasistencia</p>
                </div>
                <div class="header-filters">
                    <select class="select-filter">
                        <option value="HOY">Período: HOY (07 de Octubre de 2026)</option>
                    </select>
                    <select class="select-filter">
                        <option value="TODAS">Sede: TODAS LAS SEDES (04)</option>
                    </select>
                    <a href="${pageContext.request.contextPath}/admin/dashboard-operativo" class="btn-refresh"><i data-lucide="refresh-cw" style="width: 14px;"></i> Actualizar Ahora</a>
                </div>
            </div>

            <!-- SECCIÓN 1: MÉTRICAS OPERATIVAS -->
            <div class="section-heading">
                <span>Sección 1 // Métricas Operativas Diarias en Tiempo Real</span>
                <span style="font-size:11px; color:var(--text-muted); font-weight:500;">Total Registros: <%= totalCitas %> Citas</span>
            </div>

            <div class="metrics-grid-5">
                <div class="metric-card">
                    <div class="metric-title">Citas del Día</div>
                    <div class="metric-value"><%= totalCitas %></div>
                    <div class="metric-sub">310 atendidas / en curso<br>72 pendientes | <%= cancelaciones %> canceladas</div>
                    <div><span class="metric-tag tag-green">Cumplimiento: 90.9%</span></div>
                </div>

                <div class="metric-card">
                    <div class="metric-title">Disponibilidad Médica</div>
                    <div class="metric-value"><%= ocupacionBoxes %></div>
                    <div class="metric-sub">74 de 84 médicos activos<br>74 en consulta | 10 en descanso</div>
                    <div><span class="metric-tag tag-blue">Capacidad: Alta</span></div>
                </div>

                <div class="metric-card">
                    <div class="metric-title">Cancelaciones</div>
                    <div class="metric-value"><%= cancelaciones %></div>
                    <div class="metric-sub">Tasa del día: 9.0%<br>30 anticipadas | 08 tardías</div>
                    <div><span class="metric-tag tag-amber">Límite Alerta: 12.0%</span></div>
                </div>

                <div class="metric-card">
                    <div class="metric-title">Reasignaciones Exit.</div>
                    <div class="metric-value"><%= reasignacionesExitosas %></div>
                    <div class="metric-sub">Tasa éxito: 84.2%<br>32 reasignadas de 38 lib.</div>
                    <div><span class="metric-tag tag-green">Rendimiento: Óptimo</span></div>
                </div>

                <div class="metric-card">
                    <div class="metric-title">Tiempo Espera Sala</div>
                    <div class="metric-value"><%= tiempoEspera %></div>
                    <div class="metric-sub">Mediana actual: 21.8 min<br>Máx: 41 min | Meta: 25 min</div>
                    <div><span class="metric-tag tag-blue">Dentro de Tolerancia</span></div>
                </div>
            </div>

            <!-- SECCIÓN 2: RIESGO DE INASISTENCIA -->
            <div class="section-heading">
                <span>Sección 2 // Distribución de Riesgo de Inasistencia Calculado por el Sistema (Solo Lectura)</span>
            </div>

            <div class="risk-grid">
                <div class="risk-card low">
                    <div class="risk-header">
                        <span class="risk-title">Riesgo Bajo</span>
                        <span class="risk-badge badge-low"><%= riesgoBajoPorc %></span>
                    </div>
                    <div class="risk-count"><%= riesgoBajoCount %> Pacientes <span style="font-size:13px; font-weight:400; color:var(--text-muted);">/ <%= totalCitas %> citas</span></div>
                    <p class="risk-desc">Criterio: Historial de asistencia puntual, confirmación activa vía SMS/WhatsApp, tiempo de reserva reciente (&lt; 7 días).</p>
                    <span class="risk-action">Acción sugerida: Monitoreo Pasivo</span>
                </div>

                <div class="risk-card med">
                    <div class="risk-header">
                        <span class="risk-title">Riesgo Medio</span>
                        <span class="risk-badge badge-med"><%= riesgoMedioPorc %></span>
                    </div>
                    <div class="risk-count"><%= riesgoMedioCount %> Pacientes <span style="font-size:13px; font-weight:400; color:var(--text-muted);">/ <%= totalCitas %> citas</span></div>
                    <p class="risk-desc">Criterio: Cita agendada con más de 15 días de antelación, sin confirmación recibida o 1 reagendamiento previo.</p>
                    <span class="risk-action" style="color:#d97706;">Acción sugerida: Mensaje Recordatorio</span>
                </div>

                <div class="risk-card high">
                    <div class="risk-header">
                        <span class="risk-title">Riesgo Alto</span>
                        <span class="risk-badge badge-high"><%= riesgoAltoPorc %></span>
                    </div>
                    <div class="risk-count"><%= riesgoAltoCount %> Pacientes <span style="font-size:13px; font-weight:400; color:var(--text-muted);">/ <%= totalCitas %> citas</span></div>
                    <p class="risk-desc">Criterio: Historial reincidente de cancelaciones tardías / inasistencias previas (&gt;2), o intento fallido de validación telefónica.</p>
                    <span class="risk-action" style="color:#dc2626;">Acción sugerida: Llamada de Contacto Preventivo</span>
                </div>
            </div>

            <!-- TABLA CONTACTO PREVENTIVO -->
            <div class="panel">
                <div class="section-heading" style="margin-bottom:12px;">
                    <span style="color:var(--text-main); font-size:14px;">Tabla de Seguimiento de Contacto Preventivo // Citas en Riesgo Prioritario (Hoy)</span>
                    <span style="font-size:11px; color:var(--text-muted); font-weight:500;">Modo Inspección</span>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th>Hora</th>
                            <th>Paciente</th>
                            <th>Contacto</th>
                            <th>Sede / Cons.</th>
                            <th>Médico Asignado</th>
                            <th>Nivel de Riesgo</th>
                            <th>Estado Confirmación</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (listaCitasRiesgo != null && !listaCitasRiesgo.isEmpty()) {
                            for (Map<String, Object> cr : listaCitasRiesgo) {
                                String hora = (String) cr.get("hora");
                                String paciente = (String) cr.get("paciente");
                                String hc = (String) cr.get("hc");
                                String contacto = (String) cr.get("contacto");
                                String sedeCons = (String) cr.get("sedeCons");
                                String medico = (String) cr.get("medico");
                                String riesgo = (String) cr.get("riesgo");
                                String riesgoClass = (String) cr.get("riesgoClass");
                                String confirmacion = (String) cr.get("confirmacion");
                                String confirmClass = (String) cr.get("confirmClass");
                        %>
                        <tr>
                            <td><strong><%= hora %></strong></td>
                            <td><%= paciente %><br><span style="font-size:11px; color:var(--text-muted);"><%= hc %></span></td>
                            <td><%= contacto %></td>
                            <td><%= sedeCons %></td>
                            <td><%= medico %></td>
                            <td><span class="badge-risk-pill <%= riesgoClass %>"><%= riesgo %></span></td>
                            <td><span class="badge-confirm-pill <%= confirmClass %>"><%= confirmacion %></span></td>
                        </tr>
                        <%  }
                           } %>
                    </tbody>
                </table>

                <div class="table-footer">
                    <span>Mostrando <%= listaCitasRiesgo != null ? listaCitasRiesgo.size() : 4 %> de <%= riesgoAltoCount %> casos prioritarios de contacto</span>
                    <div class="pagination-btns">
                        <button class="btn-page">&lt; Anterior</button>
                        <button class="btn-page" style="background: var(--primary); color: #fff; border-color: var(--primary);">1</button>
                        <button class="btn-page">2</button>
                        <button class="btn-page">3</button>
                        <button class="btn-page">Siguiente &gt;</button>
                    </div>
                </div>
            </div>

            <!-- DISPONIBILIDAD DE INFORMES -->
            <div class="reports-bar">
                <div>
                    <h4 style="font-size: 14px; font-weight: 700; color: var(--text-main);">Disponibilidad de Informes</h4>
                    <p style="font-size: 12px; color: var(--text-muted); margin-top: 2px;">Generación automática de auditoría diaria para archivo administrativo.</p>
                </div>
                <div style="display: flex; gap: 12px;">
                    <a href="${pageContext.request.contextPath}/admin/export?format=pdf" target="_blank" class="btn-report"><i data-lucide="file-text" style="width: 16px; color: #ef4444;"></i> Exportar Reporte del Día (PDF)</a>
                    <a href="${pageContext.request.contextPath}/admin/export?format=csv" class="btn-report"><i data-lucide="file-spreadsheet" style="width: 16px; color: #166534;"></i> Descargar CSV de Contacto Preventivo</a>
                </div>
            </div>
        </main>
    </div>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
