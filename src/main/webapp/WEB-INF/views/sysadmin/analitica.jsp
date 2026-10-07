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
    <title>Indicadores y Analítica Strategic - Clínica Internacional</title>
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

        /* FILTER CARD */
        .filter-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px 24px; margin-bottom: 24px; box-shadow: 0 2px 10px rgba(0,0,0,0.02);
            display: flex; gap: 16px; align-items: flex-end; flex-wrap: wrap;
        }
        .form-group { display: flex; flex-direction: column; gap: 6px; flex: 1; }
        .form-group label { font-size: 12px; font-weight: 600; color: var(--text-gray); text-transform: uppercase; }
        .form-group select {
            padding: 10px 14px; border-radius: var(--radius-md); border: 1px solid var(--border-light);
            font-size: 14px; color: var(--text-dark); outline: none; background: #ffffff;
        }
        .btn-filter {
            background: var(--primary); color: #ffffff; border: none; padding: 11px 24px; border-radius: 30px;
            font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; display: inline-flex; align-items: center; gap: 8px;
        }

        .section-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 24px; margin-bottom: 24px; box-shadow: 0 2px 10px rgba(0,0,0,0.02);
        }
        .section-title { font-size: 15px; font-weight: 700; color: var(--text-dark); margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; }

        /* STATS GRID 5 COLUMNS */
        .stats-grid-5 { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; margin-bottom: 24px; }
        .stat-card-sm {
            background: #f8fafc; border-radius: var(--radius-md); border: 1px solid var(--border-light); padding: 16px;
        }
        .stat-card-title { font-size: 11px; font-weight: 700; color: var(--text-gray); text-transform: uppercase; margin-bottom: 6px; }
        .stat-card-num { font-size: 24px; font-weight: 700; color: var(--text-dark); }
        .stat-card-sub { font-size: 11px; color: var(--text-gray); margin-top: 4px; }

        /* GRID 2 COLUMNS FOR TABLES & CHARTS */
        .grid-2col { display: grid; grid-template-columns: 1fr 300px; gap: 24px; }

        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { background: #f8fafc; padding: 14px 16px; font-size: 12px; font-weight: 600; color: var(--text-gray); border-bottom: 1px solid var(--border-light); }
        td { padding: 14px 16px; font-size: 13px; color: var(--text-dark); border-bottom: 1px solid var(--border-light); vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover { background: #f8fafc; }

        /* RISK CARDS */
        .risk-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 20px; }
        .risk-card { border-radius: var(--radius-md); padding: 18px; border: 1px solid var(--border-light); background: #ffffff; }
        .risk-card.low { border-left: 4px solid #166534; }
        .risk-card.medium { border-left: 4px solid #d97706; }
        .risk-card.high { border-left: 4px solid #dc2626; }
        .risk-title { font-size: 12px; font-weight: 700; text-transform: uppercase; margin-bottom: 4px; }
        .risk-pct { font-size: 26px; font-weight: 700; color: var(--text-dark); }
        .risk-desc { font-size: 12px; color: var(--text-gray); margin-top: 6px; line-height: 1.4; }

        .footer-action { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; }
        .btn-export {
            background: var(--text-dark); color: #ffffff; border: none; padding: 12px 24px; border-radius: 30px;
            font-size: 13px; font-weight: 600; cursor: pointer; display: inline-flex; align-items: center; gap: 8px; transition: 0.2s;
        }
        .btn-export:hover { background: #1e293b; }

        @media(max-width:1024px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } .stats-grid-5, .risk-grid, .grid-2col { grid-template-columns: 1fr; } }
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
            <a href="${pageContext.request.contextPath}/sysadmin/sedes" class="nav-item">
                <i data-lucide="building-2" style="width: 18px;"></i> 3. Sedes
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/priorizacion" class="nav-item">
                <i data-lucide="sliders" style="width: 18px;"></i> 4. Config. Priorización
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/analitica" class="nav-item active">
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
                <h1 class="header-title">Indicadores y Analítica Estratégica</h1>
                <p class="header-subtitle">Supervisión agregada del rendimiento asistencial y distribución de riesgo de toda la clínica.</p>
            </div>
            <a href="#" class="btn-bell" title="Alertas">
                <i data-lucide="bell" style="width: 20px;"></i>
            </a>
        </div>

        <!-- FILTER CARD -->
        <div class="filter-card">
            <div class="form-group">
                <label>Rango de Evaluación</label>
                <select>
                    <option>Período: Últimos 30 días</option>
                    <option>Período: Últimos 7 días</option>
                    <option>Período: Este mes</option>
                </select>
            </div>

            <div class="form-group">
                <label>Nodo / Sede Asistencial</label>
                <select>
                    <option>Sede: Todas las sedes (04)</option>
                    <option>Sede Central</option>
                    <option>Sede Norte</option>
                    <option>Sede Sur</option>
                    <option>Sede Este</option>
                </select>
            </div>

            <div class="form-group">
                <label>Agrupación Clínica</label>
                <select>
                    <option>Especialidad: Todas</option>
                    <option>Cardiología</option>
                    <option>Medicina Interna</option>
                    <option>Pediatría</option>
                </select>
            </div>

            <button class="btn-filter"><i data-lucide="filter" style="width: 16px;"></i> Filtrar</button>
        </div>

        <!-- SECCIÓN A -->
        <div class="section-card">
            <div class="section-title">
                <span>SECCIÓN A // ANALÍTICA DESCRIPTIVA (NIVEL AGREGADO DE TODA LA CLÍNICA)</span>
                <span style="font-size: 11px; background: #e2e8f0; color: #475569; padding: 4px 10px; border-radius: 20px; font-weight: 600;">REGISTRO HISTÓRICO AUDITADO</span>
            </div>

            <div class="stats-grid-5">
                <div class="stat-card-sm">
                    <div class="stat-card-title">TOTAL CITAS PROGRAMADAS</div>
                    <div class="stat-card-num">12,450</div>
                    <div class="stat-card-sub">100% Volumen base en red</div>
                </div>

                <div class="stat-card-sm">
                    <div class="stat-card-title">CITAS ATENDIDAS EFECTIVAS</div>
                    <div class="stat-card-num" style="color: #166534;">10,210</div>
                    <div class="stat-card-sub">82.0% Cumplimiento global</div>
                </div>

                <div class="stat-card-sm">
                    <div class="stat-card-title">CANCELACIONES REGISTRADAS</div>
                    <div class="stat-card-num" style="color: #dc2626;">1,480</div>
                    <div class="stat-card-sub">11.9% Tasa de cancelación</div>
                </div>

                <div class="stat-card-sm">
                    <div class="stat-card-title">CUPOS REASIGNADOS CON ÉXITO</div>
                    <div class="stat-card-num" style="color: var(--primary);">1,095</div>
                    <div class="stat-card-sub">74.0% Recuperación efectiva</div>
                </div>

                <div class="stat-card-sm">
                    <div class="stat-card-title">INASISTENCIAS (NO ASISTIÓ)</div>
                    <div class="stat-card-num" style="color: #d97706;">760</div>
                    <div class="stat-card-sub">6.1% Ausentismo en red</div>
                </div>
            </div>

            <div class="grid-2col">
                <!-- TABLA COMPARATIVA SEDES -->
                <div>
                    <h4 style="font-size: 13px; font-weight: 700; color: var(--text-dark); margin-bottom: 12px;">TABLA COMPARATIVA AGREGADA POR SEDE CLÍNICA</h4>
                    <table>
                        <thead>
                            <tr>
                                <th>Sede</th>
                                <th>Programadas</th>
                                <th>Atendidas</th>
                                <th>Cancelaciones</th>
                                <th>Reasignadas</th>
                                <th>% Éxito Reasig.</th>
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
                    <h4 style="font-size: 13px; font-weight: 700; color: var(--text-dark); margin-bottom: 16px;">TENDENCIA SEMANAL RED</h4>
                    <div style="display: flex; justify-content: space-between; align-items: flex-end; height: 140px; padding: 0 10px;">
                        <div style="text-align: center;"><div style="background: var(--primary); height: 90px; width: 32px; border-radius: 6px; margin: 0 auto 6px;"></div><span style="font-size: 11px; font-weight: 600;">S-1</span></div>
                        <div style="text-align: center;"><div style="background: var(--primary); height: 110px; width: 32px; border-radius: 6px; margin: 0 auto 6px;"></div><span style="font-size: 11px; font-weight: 600;">S-2</span></div>
                        <div style="text-align: center;"><div style="background: var(--primary); height: 125px; width: 32px; border-radius: 6px; margin: 0 auto 6px;"></div><span style="font-size: 11px; font-weight: 600;">S-3</span></div>
                        <div style="text-align: center;"><div style="background: var(--primary); height: 105px; width: 32px; border-radius: 6px; margin: 0 auto 6px;"></div><span style="font-size: 11px; font-weight: 600;">S-4</span></div>
                    </div>
                    <div style="text-align: center; margin-top: 14px; font-size: 11px; color: var(--text-gray); font-weight: 600;">
                        Efectividad promedio reasignación: >75%
                    </div>
                </div>
            </div>
        </div>

        <!-- SECCIÓN B -->
        <div class="section-card">
            <div class="section-title">
                <span>SECCIÓN B // ESTIMACIÓN DE RIESGO (DISTRIBUCIÓN AGREGADA DE TODOS LOS PACIENTES)</span>
                <span style="font-size: 11px; background: #e0f2fe; color: #0369a1; padding: 4px 10px; border-radius: 20px; font-weight: 600;">MODELO PREDICTIVO ACTIVO</span>
            </div>

            <div class="risk-grid">
                <div class="risk-card low">
                    <div class="risk-title" style="color: #166534;">RIESGO BAJO</div>
                    <div class="risk-pct">68%</div>
                    <div class="risk-desc">848 citas. Pacientes con historial de puntualidad y confirmación activa por SMS/WhatsApp.</div>
                </div>

                <div class="risk-card medium">
                    <div class="risk-title" style="color: #d97706;">RIESGO MEDIO</div>
                    <div class="risk-pct">22%</div>
                    <div class="risk-desc">272 citas. Citas agendadas con más de 15 días de anticipación sin reconfirmar.</div>
                </div>

                <div class="risk-card high">
                    <div class="risk-title" style="color: #dc2626;">RIESGO ALTO</div>
                    <div class="risk-pct">10%</div>
                    <div class="risk-desc">123 citas. Historial de ausencias previas o reincidencia en cancelaciones tardías.</div>
                </div>
            </div>

            <h4 style="font-size: 13px; font-weight: 700; color: var(--text-dark); margin-bottom: 12px;">TABLA DE SEGUIMIENTO DESCRIPTIVO DE FACTORES OBSERVADOS DE ASISTENCIA</h4>
            <table>
                <thead>
                    <tr>
                        <th>Factor Evaluado</th>
                        <th>Segmento / Condición</th>
                        <th>Muestra Asociada</th>
                        <th>Asistencia Efectiva</th>
                        <th>Impacto Observado en Registro</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><strong>Horario de citación</strong></td>
                        <td>Turno Matutino (07:00 - 13:00)</td>
                        <td>7,100</td>
                        <td><strong style="color: #166534;">87.4%</strong></td>
                        <td style="color: var(--text-gray);">Puntualidad alta; menor tasa de ausencia espontánea.</td>
                    </tr>
                    <tr>
                        <td><strong>Anticipación de reserva</strong></td>
                        <td>Inmediata / Reciente (< 5 días previos)</td>
                        <td>4,800</td>
                        <td><strong style="color: #166534;">92.1%</strong></td>
                        <td style="color: var(--text-gray);">Tasa crítica de retención; paciente con urgencia sentida.</td>
                    </tr>
                    <tr>
                        <td><strong>Canal digital de confirmación</strong></td>
                        <td>Validado con confirmación activa (SMS/WA/App)</td>
                        <td>8,920</td>
                        <td><strong style="color: #166534;">93.5%</strong></td>
                        <td style="color: var(--text-gray);">Confirmación activa reduce inasistencia neta al 6.5%.</td>
                    </tr>
                </tbody>
            </table>

            <div class="footer-action">
                <span style="font-size: 12px; color: var(--text-gray);"><i data-lucide="shield" style="width: 14px; vertical-align: middle; margin-right: 4px;"></i> Estado agregado multi-sede regulado y consolidado para Gerencia Médica.</span>
                <button class="btn-export"><i data-lucide="download" style="width: 16px;"></i> Exportar Informe Estratégico (PDF / CSV)</button>
            </div>
        </div>
    </main>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
