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
    <title>Indicadores y Analítica Global - Clínica Internacional</title>
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

        /* FILTER CARD */
        .filter-card {
            background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px 24px; margin-bottom: 24px; box-shadow: var(--shadow-sm);
            display: flex; gap: 16px; align-items: flex-end; flex-wrap: wrap;
        }
        .form-group { display: flex; flex-direction: column; gap: 6px; flex: 1; }
        .form-group label { font-size: 12px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; }
        .form-group select {
            padding: 10px 14px; border-radius: var(--radius-md); border: 1px solid var(--border-light);
            font-size: 14px; color: var(--text-main); outline: none; background: #ffffff;
        }
        .btn-filter {
            background: var(--primary); color: #ffffff; border: none; padding: 11px 24px; border-radius: 20px;
            font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; display: inline-flex; align-items: center; gap: 8px;
        }

        .panel {
            background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 24px; margin-bottom: 24px; box-shadow: var(--shadow-sm);
        }
        .panel-title { font-size: 15px; font-weight: 700; color: var(--text-main); margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; }

        /* STATS GRID 5 COLUMNS */
        .stats-grid-5 { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; margin-bottom: 24px; }
        .stat-card-sm {
            background: #f8fafc; border-radius: var(--radius-md); border: 1px solid var(--border-light); padding: 16px;
        }
        .stat-card-title { font-size: 11px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; margin-bottom: 6px; }
        .stat-card-num { font-size: 24px; font-weight: 700; color: var(--text-main); }
        .stat-card-sub { font-size: 11px; color: var(--text-muted); margin-top: 4px; }

        /* GRID 2 COLUMNS FOR TABLES & CHARTS */
        .grid-2col { display: grid; grid-template-columns: 1fr 300px; gap: 24px; }

        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { background: #f8fafc; padding: 14px 16px; font-size: 12px; font-weight: 600; color: var(--text-muted); border-bottom: 1px solid var(--border-light); }
        td { padding: 14px 16px; font-size: 13px; color: var(--text-main); border-bottom: 1px solid var(--border-light); vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover { background: #f8fafc; }

        /* RISK CARDS */
        .risk-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 20px; }
        .risk-card { border-radius: var(--radius-md); padding: 18px; border: 1px solid var(--border-light); background: #ffffff; }
        .risk-card.low { border-left: 4px solid #166534; }
        .risk-card.medium { border-left: 4px solid #d97706; }
        .risk-card.high { border-left: 4px solid #dc2626; }
        .risk-title { font-size: 12px; font-weight: 700; text-transform: uppercase; margin-bottom: 4px; }
        .risk-pct { font-size: 26px; font-weight: 700; color: var(--text-main); }
        .risk-desc { font-size: 12px; color: var(--text-muted); margin-top: 6px; line-height: 1.4; }

        .footer-action { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; }
        .btn-export {
            background: var(--text-main); color: #ffffff; border: none; padding: 12px 24px; border-radius: 20px;
            font-size: 13px; font-weight: 600; cursor: pointer; display: inline-flex; align-items: center; gap: 8px; transition: 0.2s;
        }
        .btn-export:hover { background: #1e293b; }

        @media(max-width:1024px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } .stats-grid-5, .risk-grid, .grid-2col { grid-template-columns: 1fr; } }
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
            <a href="${pageContext.request.contextPath}/sysadmin/dashboard" class="nav-link">
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
            <a href="${pageContext.request.contextPath}/sysadmin/analitica" class="nav-link active">
                <i data-lucide="trending-up"></i>
                <span>Indicadores y analítica</span>
            </a>
        </nav>

        <div class="sidebar-footer">
            <div class="user-capsule">
                <div class="user-avatar-circle"><%= nombreAdmin != null && !nombreAdmin.isEmpty() ? ("" + nombreAdmin.charAt(0)).toUpperCase() : "JV" %></div>
                <div class="user-info-text">
                    <span class="user-full-name"><%= nombreAdmin %></span>
                    <span class="user-role-desc">Administrador</span>
                </div>
                <a href="${pageContext.request.contextPath}/logout" class="btn-sidebar-logout" title="Cerrar sesión">
                    <i data-lucide="log-out"></i>
                </a>
            </div>
        </div>
    </aside>

    <main class="main-content">
        <div class="breadcrumb-trail">Administrador / <span>Indicadores y analítica</span></div>
        <div class="page-header">
            <h1 class="page-title">Indicadores y Analítica Global</h1>
            <p class="page-subtitle">Supervisión agregada del rendimiento asistencial y distribución de riesgo de toda la clínica.</p>
        </div>

            <!-- FILTER CARD -->
            <div class="filter-card">
                <div class="form-group">
                    <label>Rango de Evaluación</label>
                    <select>
                        <option>Últimos 30 días</option>
                        <option>Últimos 7 días</option>
                        <option>Este mes</option>
                    </select>
                </div>

                <div class="form-group">
                    <label>Sede Asistencial</label>
                    <select>
                        <option>Todas las sedes</option>
                        <option>Sede Central</option>
                        <option>Sede Norte</option>
                        <option>Sede Sur</option>
                        <option>Sede Este</option>
                    </select>
                </div>

                <div class="form-group">
                    <label>Especialidad</label>
                    <select>
                        <option>Todas las especialidades</option>
                        <option>Cardiología</option>
                        <option>Medicina Interna</option>
                        <option>Pediatría</option>
                    </select>
                </div>

                <button class="btn-filter"><i data-lucide="filter" style="width: 16px;"></i> Filtrar</button>
            </div>

            <!-- SECCIÓN A -->
            <div class="panel">
                <div class="panel-title">
                    <span>Rendimiento Asistencial Global</span>
                </div>

                <div class="stats-grid-5">
                    <div class="stat-card-sm">
                        <div class="stat-card-title">Citas Programadas</div>
                        <div class="stat-card-num">12,450</div>
                        <div class="stat-card-sub">Volumen total en red</div>
                    </div>

                    <div class="stat-card-sm">
                        <div class="stat-card-title">Citas Atendidas</div>
                        <div class="stat-card-num" style="color: #166534;">10,210</div>
                        <div class="stat-card-sub">82.0% Cumplimiento global</div>
                    </div>

                    <div class="stat-card-sm">
                        <div class="stat-card-title">Cancelaciones</div>
                        <div class="stat-card-num" style="color: #dc2626;">1,480</div>
                        <div class="stat-card-sub">11.9% Cancelación neta</div>
                    </div>

                    <div class="stat-card-sm">
                        <div class="stat-card-title">Cupos Reasignados</div>
                        <div class="stat-card-num" style="color: var(--primary);">1,095</div>
                        <div class="stat-card-sub">74.0% Recuperación efectiva</div>
                    </div>

                    <div class="stat-card-sm">
                        <div class="stat-card-title">Inasistencias</div>
                        <div class="stat-card-num" style="color: #d97706;">760</div>
                        <div class="stat-card-sub">6.1% Ausentismo en red</div>
                    </div>
                </div>

                <div class="grid-2col">
                    <!-- TABLA COMPARATIVA SEDES -->
                    <div>
                        <h4 style="font-size: 13px; font-weight: 700; color: var(--text-main); margin-bottom: 12px;">Desempeño Comparativo por Sede</h4>
                        <table>
                            <thead>
                                <tr>
                                    <th>Sede</th>
                                    <th>Programadas</th>
                                    <th>Atendidas</th>
                                    <th>Cancelaciones</th>
                                    <th>Reasignadas</th>
                                    <th>% Éxito</th>
                                    <th>Inasistencias</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td><strong>Sede Norte</strong></td>
                                    <td>3,858</td>
                                    <td>3,180</td>
                                    <td>420</td>
                                    <td>325</td>
                                    <td><strong style="color: #166534;">77.3%</strong></td>
                                    <td>259</td>
                                </tr>
                                <tr>
                                    <td><strong>Sede Central</strong></td>
                                    <td>4,900</td>
                                    <td>4,010</td>
                                    <td>590</td>
                                    <td>445</td>
                                    <td><strong style="color: #166534;">75.4%</strong></td>
                                    <td>300</td>
                                </tr>
                                <tr>
                                    <td><strong>Sede Sur</strong></td>
                                    <td>2,100</td>
                                    <td>1,740</td>
                                    <td>260</td>
                                    <td>185</td>
                                    <td><strong style="color: #166534;">71.1%</strong></td>
                                    <td>180</td>
                                </tr>
                                <tr>
                                    <td><strong>Sede Este</strong></td>
                                    <td>1,600</td>
                                    <td>1,280</td>
                                    <td>210</td>
                                    <td>140</td>
                                    <td><strong style="color: #d97706;">66.6%</strong></td>
                                    <td>110</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>

                    <!-- TENDENCIA GRÁFICA -->
                    <div style="background: #f8fafc; border-radius: var(--radius-md); border: 1px solid var(--border-light); padding: 18px;">
                        <h4 style="font-size: 13px; font-weight: 700; color: var(--text-main); margin-bottom: 16px;">Tendencia Semanal</h4>
                        <div style="display: flex; justify-content: space-between; align-items: flex-end; height: 140px; padding: 0 10px;">
                            <div style="text-align: center;"><div style="background: var(--primary); height: 90px; width: 32px; border-radius: 6px; margin: 0 auto 6px;"></div><span style="font-size: 11px; font-weight: 600;">Sem 1</span></div>
                            <div style="text-align: center;"><div style="background: var(--primary); height: 110px; width: 32px; border-radius: 6px; margin: 0 auto 6px;"></div><span style="font-size: 11px; font-weight: 600;">Sem 2</span></div>
                            <div style="text-align: center;"><div style="background: var(--primary); height: 125px; width: 32px; border-radius: 6px; margin: 0 auto 6px;"></div><span style="font-size: 11px; font-weight: 600;">Sem 3</span></div>
                            <div style="text-align: center;"><div style="background: var(--primary); height: 105px; width: 32px; border-radius: 6px; margin: 0 auto 6px;"></div><span style="font-size: 11px; font-weight: 600;">Sem 4</span></div>
                        </div>
                        <div style="text-align: center; margin-top: 14px; font-size: 11px; color: var(--text-muted); font-weight: 600;">
                            Tasa media de recuperación: >75%
                        </div>
                    </div>
                </div>
            </div>

            <!-- SECCIÓN B -->
            <div class="panel">
                <div class="panel-title">
                    <span>Estimación y Categorización de Riesgo de Inasistencia</span>
                </div>

                <div class="risk-grid">
                    <div class="risk-card low">
                        <div class="risk-title" style="color: #166534;">RIESGO BAJO</div>
                        <div class="risk-pct">68%</div>
                        <div class="risk-desc">848 citas. Confirmación activa realizada por WhatsApp/SMS.</div>
                    </div>

                    <div class="risk-card medium">
                        <div class="risk-title" style="color: #d97706;">RIESGO MEDIO</div>
                        <div class="risk-pct">22%</div>
                        <div class="risk-desc">272 citas. Reserva anticipada sin confirmación en los últimos 3 días.</div>
                    </div>

                    <div class="risk-card high">
                        <div class="risk-title" style="color: #dc2626;">RIESGO ALTO</div>
                        <div class="risk-pct">10%</div>
                        <div class="risk-desc">123 citas. Historial de cancelaciones tardías o inasistencias previas.</div>
                    </div>
                </div>

                <h4 style="font-size: 13px; font-weight: 700; color: var(--text-main); margin-bottom: 12px;">Factores Observados en la Red</h4>
                <table>
                    <thead>
                        <tr>
                            <th>Factor Evaluado</th>
                            <th>Condición de Citación</th>
                            <th>Muestra Total</th>
                            <th>Asistencia Efectiva</th>
                            <th>Observación Clínica</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><strong>Horario de atención</strong></td>
                            <td>Turno Matutino (07:00 - 13:00)</td>
                            <td>7,100</td>
                            <td><strong style="color: #166534;">87.4%</strong></td>
                            <td style="color: var(--text-muted);">Puntualidad elevada; menor nivel de inasistencia.</td>
                        </tr>
                        <tr>
                            <td><strong>Anticipación de reserva</strong></td>
                            <td>Reciente (< 5 días previa)</td>
                            <td>4,800</td>
                            <td><strong style="color: #166534;">92.1%</strong></td>
                            <td style="color: var(--text-muted);">Alta retención por urgencia sentida del paciente.</td>
                        </tr>
                        <tr>
                            <td><strong>Canal de confirmación</strong></td>
                            <td>Validado por WhatsApp/SMS</td>
                            <td>8,920</td>
                            <td><strong style="color: #166534;">93.5%</strong></td>
                            <td style="color: var(--text-muted);">La confirmación digital activa reduce inasistencia al 6.5%.</td>
                        </tr>
                    </tbody>
                </table>

                <div class="footer-action">
                    <span style="font-size: 12px; color: var(--text-muted);"><i data-lucide="shield" style="width: 14px; vertical-align: middle; margin-right: 4px;"></i> Consolidado multi-sede regulado para la gerencia médica.</span>
                    <button class="btn-export"><i data-lucide="download" style="width: 16px;"></i> Exportar Reporte Estratégico (PDF / CSV)</button>
                </div>
            </div>
        </main>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
