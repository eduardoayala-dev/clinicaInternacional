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

    List<Map<String, Object>> listaReasignaciones = (List<Map<String, Object>>) request.getAttribute("listaReasignaciones");

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
    <title>Seguimiento de Reasignaciones - Clínica Internacional</title>
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
        .page-header { margin-bottom: 24px; }
        .page-title { font-size: 24px; font-weight: 700; color: var(--text-main); margin-bottom: 6px; letter-spacing: -0.5px; }
        .page-subtitle { font-size: 14px; color: var(--text-muted); }

        .flash-alert {
            background: #dcfce7; color: #166534; border: 1px solid #bbf7d0; padding: 14px 18px;
            border-radius: var(--radius-md); font-weight: 600; margin-bottom: 24px; font-size: 14px; display: flex; align-items: center; gap: 10px;
        }

        .section-tag {
            display: inline-flex; align-items: center; gap: 8px; font-size: 12px; font-weight: 700;
            color: var(--primary); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 12px;
            background: var(--primary-light); padding: 6px 12px; border-radius: var(--radius-md);
        }

        .filters-panel {
            background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 18px 24px; box-shadow: var(--shadow-sm); margin-bottom: 24px;
            display: flex; gap: 14px; align-items: center; flex-wrap: wrap;
        }

        .select-filter, .date-filter {
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
        td { padding: 14px; font-size: 13px; color: var(--text-main); border-bottom: 1px solid var(--border-light); vertical-align: top; }
        tr:last-child td { border-bottom: none; }
        tr:hover { background: #f8fafc; }

        .badge-code {
            font-family: monospace; font-weight: 700; color: var(--primary); background: var(--primary-light);
            padding: 4px 8px; border-radius: 4px; font-size: 12px; display: inline-block;
        }

        .candidate-flow { font-size: 12px; color: var(--text-main); line-height: 1.5; }

        .time-badge { font-weight: 600; font-size: 12px; display: inline-block; padding: 4px 8px; border-radius: 12px; }
        .time-ok { background: #dcfce7; color: #166534; }
        .time-exhausted { background: #fee2e2; color: #991b1b; }

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
            <a href="${pageContext.request.contextPath}/admin/pacientes" class="nav-item">
                <i data-lucide="users" style="width: 18px;"></i> Pacientes
            </a>
            <a href="${pageContext.request.contextPath}/admin/reasignaciones" class="nav-item active">
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
                <h1 class="page-title">Seguimiento de Reasignaciones</h1>
                <p class="page-subtitle">Monitoreo en tiempo real del flujo de reasignación automática de citas canceladas</p>
            </div>

            <% if (flashMessage != null) { %>
                <div class="flash-alert">
                    <i data-lucide="check-circle" style="width: 18px;"></i>
                    <%= flashMessage %>
                </div>
            <% } %>

            <div class="section-tag">
                <i data-lucide="history" style="width: 14px;"></i> Sección A — Historial de Reasignaciones Automáticas (Solo Lectura)
            </div>

            <!-- FILTROS -->
            <form method="GET" action="${pageContext.request.contextPath}/admin/reasignaciones" class="filters-panel">
                <select name="sede" class="select-filter">
                    <option value="">Sede: TODAS</option>
                    <option value="Lima">Sede Lima</option>
                    <option value="Central">Sede Central</option>
                    <option value="Norte">Sede Norte</option>
                    <option value="Sur">Sede Sur</option>
                </select>
                <select name="especialidad" class="select-filter">
                    <option value="">Especialidad: TODAS</option>
                    <option value="Cardiología">Cardiología</option>
                    <option value="Medicina Interna">Medicina Interna</option>
                    <option value="Dermatología">Dermatología</option>
                    <option value="Oftalmología">Oftalmología</option>
                </select>
                <div style="display: flex; align-items: center; gap: 8px;">
                    <span style="font-size: 13px; color: var(--text-muted);">Fecha:</span>
                    <input type="date" name="fecha" class="date-filter" value="2026-10-07">
                </div>
                <a href="${pageContext.request.contextPath}/admin/reasignaciones" class="btn-filter"><i data-lucide="x" style="width: 14px;"></i> Limpiar Filtros</a>
                <button type="submit" class="btn-filter btn-primary-action"><i data-lucide="refresh-cw" style="width: 14px;"></i> Actualizar</button>
            </form>

            <!-- TABLA REASIGNACIONES -->
            <div class="panel">
                <table>
                    <thead>
                        <tr>
                            <th>Código Cita</th>
                            <th>Cupo Liberado (Fecha / Hora / Médico)</th>
                            <th>Motivo Anulación</th>
                            <th>Candidatos Ofrecidos</th>
                            <th>Tiempo Respuesta</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (listaReasignaciones != null && !listaReasignaciones.isEmpty()) {
                            for (Map<String, Object> r : listaReasignaciones) {
                                String codigo = (String) r.get("codigo");
                                String cupo = (String) r.get("cupo");
                                String medicoInfo = (String) r.get("medicoInfo");
                                String motivo = (String) r.get("motivo");
                                String candidatosHtml = (String) r.get("candidatosHtml");
                                String tiempo = (String) r.get("tiempo");
                                String tiempoClass = (String) r.get("tiempoClass");
                        %>
                        <tr>
                            <td><span class="badge-code"><%= codigo %></span></td>
                            <td><strong><%= cupo %></strong><br><span style="font-size:12px; color:var(--text-muted);"><%= medicoInfo %></span></td>
                            <td><%= motivo %></td>
                            <td><div class="candidate-flow"><%= candidatosHtml %></div></td>
                            <td><span class="time-badge <%= tiempoClass %>"><%= tiempo %></span></td>
                        </tr>
                        <%  }
                           } %>
                    </tbody>
                </table>

                <div class="table-footer">
                    <span>Mostrando <%= listaReasignaciones != null ? listaReasignaciones.size() : 4 %> de <%= listaReasignaciones != null ? listaReasignaciones.size() : 4 %> cancelaciones recientes // Log de ejecución del motor #REASIG-402</span>
                    <div class="pagination-btns">
                        <button class="btn-page">&lt; Prev</button>
                        <button class="btn-page" style="background: var(--primary); color: #fff; border-color: var(--primary);">Pág 1 / 1</button>
                        <button class="btn-page">Sig &gt;</button>
                    </div>
                </div>
            </div>
        </main>
    </div>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
