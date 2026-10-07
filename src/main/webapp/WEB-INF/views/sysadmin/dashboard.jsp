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
    <title>Configuración y Supervisión Global - Clínica Internacional</title>
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
            --text-light: #94a3b8;
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

        .status-banner {
            background: #dcfce7; color: #166534; border: 1px solid #bbf7d0; padding: 12px 20px;
            border-radius: var(--radius-md); font-size: 13px; font-weight: 600; margin-bottom: 24px;
            display: flex; justify-content: space-between; align-items: center;
        }

        /* STATS GRID */
        .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
        .stat-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.02); display: flex; flex-direction: column; justify-content: space-between;
        }
        .stat-card-title { font-size: 11px; font-weight: 700; color: var(--text-gray); text-transform: uppercase; letter-spacing: 0.5px; display: flex; align-items: center; justify-content: space-between; }
        .stat-card-num { font-size: 26px; font-weight: 700; color: var(--text-dark); margin: 10px 0 4px 0; letter-spacing: -0.5px; }
        .stat-card-sub { font-size: 12px; color: var(--text-gray); font-weight: 500; }

        /* GRID 2 COLUMNS */
        .grid-2col { display: grid; grid-template-columns: 1fr 340px; gap: 24px; margin-bottom: 24px; }

        .card-box {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.02);
        }
        .card-box-header {
            padding: 20px 24px; border-bottom: 1px solid var(--border-light); display: flex; justify-content: space-between; align-items: center;
        }
        .card-box-title { font-size: 15px; font-weight: 700; color: var(--text-dark); display: flex; align-items: center; gap: 8px; }

        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { background: #f8fafc; padding: 14px 18px; font-size: 12px; font-weight: 600; color: var(--text-gray); border-bottom: 1px solid var(--border-light); }
        td { padding: 14px 18px; font-size: 13px; color: var(--text-dark); border-bottom: 1px solid var(--border-light); vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover { background: #f8fafc; }

        .badge-status { padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; display: inline-block; }
        .badge-online { background: #dcfce7; color: #166534; }
        .badge-alta { background: #fee2e2; color: #991b1b; }
        .badge-aplicado { background: #eff6ff; color: #2563eb; }

        .progress-bar-bg { background: #e2e8f0; height: 8px; border-radius: 4px; overflow: hidden; width: 100px; display: inline-block; vertical-align: middle; margin-right: 8px; }
        .progress-bar-fill { background: var(--primary); height: 100%; }

        .quick-links-list { display: flex; flex-direction: column; gap: 12px; padding: 20px; }
        .quick-link-btn {
            display: flex; align-items: center; justify-content: space-between; padding: 14px 18px;
            border-radius: var(--radius-md); border: 1px solid var(--border-light); background: #ffffff;
            color: var(--text-dark); text-decoration: none; font-size: 13px; font-weight: 600; transition: all 0.2s;
        }
        .quick-link-btn:hover { border-color: var(--primary); color: var(--primary); background: #eff6ff; }

        @media(max-width:1024px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } .stats-grid { grid-template-columns: repeat(2, 1fr); } .grid-2col { grid-template-columns: 1fr; } }
    </style>
</head>
<body>

    <aside class="sidebar">
        <div class="brand-container">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>
        <nav class="nav-menu">
            <a href="${pageContext.request.contextPath}/sysadmin/dashboard" class="nav-item active">
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
                <h1 class="header-title">Configuración y Supervisión Global</h1>
                <p class="header-subtitle">Consolidado de infraestructura, usuarios y rendimiento asistencial de toda la red clínica multi-sede.</p>
            </div>
            <a href="#" class="btn-bell" title="Alertas de Infraestructura">
                <i data-lucide="bell" style="width: 20px;"></i>
            </a>
        </div>

        <div class="status-banner">
            <span><i data-lucide="shield-check" style="width: 16px; vertical-align: middle; margin-right: 6px;"></i> ESTADO DEL SISTEMA: OPERATIVO EN RED NACIONAL | SIN INCIDENCIAS CRÍTICAS</span>
            <span style="font-weight: 500; font-size: 12px; color: #15803d;">Última sincronización global: 11:45:00</span>
        </div>

        <!-- STATS GRID -->
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-card-title">K-01 IDENTIDAD Y ACCESO <i data-lucide="users" style="width: 16px;"></i></div>
                <div class="stat-card-num">1,420</div>
                <div class="stat-card-sub">1,250 Pacientes | 120 Médicos | 48 Personal Adm. | 2 Admins</div>
            </div>

            <div class="stat-card">
                <div class="stat-card-title">K-02 INFRAESTRUCTURA <i data-lucide="building-2" style="width: 16px;"></i></div>
                <div class="stat-card-num">4 ACTIVAS</div>
                <div class="stat-card-sub">Norte, Central, Sur, Este | 98 consultorios habilitados</div>
            </div>

            <div class="stat-card">
                <div class="stat-card-title">K-03 DEMANDA CONSOLIDADA <i data-lucide="activity" style="width: 16px;"></i></div>
                <div class="stat-card-num">8,640</div>
                <div class="stat-card-sub">89.2% Tasa cumplimiento | +4.8% vs. mes anterior</div>
            </div>

            <div class="stat-card">
                <div class="stat-card-title">K-04 ALGORITMO PRIORIDAD <i data-lucide="cpu" style="width: 16px;"></i></div>
                <div class="stat-card-num">74.5%</div>
                <div class="stat-card-sub">1,895 cupos salvados de deserción</div>
            </div>
        </div>

        <!-- GRID 2 COLUMNS -->
        <div class="grid-2col">
            <!-- MONITOREO NODOS DE RED POR SEDE -->
            <div class="card-box">
                <div class="card-box-header">
                    <span class="card-box-title"><i data-lucide="server" style="width: 18px; color: var(--primary);"></i> Monitoreo de Nodos de Red por Sede</span>
                    <span style="font-size: 12px; color: var(--text-gray);">Supervisión en tiempo real de capacidad</span>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>ID Sede</th>
                            <th>Nombre Sede</th>
                            <th>Estado Red</th>
                            <th>Consultorios</th>
                            <th>Ocupación Global</th>
                            <th>Última Auditoría</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><strong>NOD-01</strong></td>
                            <td>Sede Central - Metropolitano</td>
                            <td><span class="badge-status badge-online">ONLINE / SYNC</span></td>
                            <td>36 / 36</td>
                            <td>
                                <div class="progress-bar-bg"><div class="progress-bar-fill" style="width: 92%;"></div></div>
                                <strong>92.4%</strong>
                            </td>
                            <td style="color: var(--text-gray);">11:30:12</td>
                        </tr>
                        <tr>
                            <td><strong>NOD-02</strong></td>
                            <td>Sede Norte - Valle</td>
                            <td><span class="badge-status badge-online">ONLINE / SYNC</span></td>
                            <td>24 / 24</td>
                            <td>
                                <div class="progress-bar-bg"><div class="progress-bar-fill" style="width: 88%;"></div></div>
                                <strong>88.1%</strong>
                            </td>
                            <td style="color: var(--text-gray);">11:28:44</td>
                        </tr>
                        <tr>
                            <td><strong>NOD-03</strong></td>
                            <td>Sede Sur - Parque Industrial</td>
                            <td><span class="badge-status badge-online">ONLINE / SYNC</span></td>
                            <td>18 / 20</td>
                            <td>
                                <div class="progress-bar-bg"><div class="progress-bar-fill" style="width: 79%;"></div></div>
                                <strong>79.5%</strong>
                            </td>
                            <td style="color: var(--text-gray);">11:25:01</td>
                        </tr>
                        <tr>
                            <td><strong>NOD-04</strong></td>
                            <td>Sede Este - Cordillera</td>
                            <td><span class="badge-status badge-alta">ONLINE / ALTA CARGA</span></td>
                            <td>18 / 18</td>
                            <td>
                                <div class="progress-bar-bg"><div class="progress-bar-fill" style="width: 96%; background: #dc2626;"></div></div>
                                <strong>96.8%</strong>
                            </td>
                            <td style="color: var(--text-gray);">11:42:19</td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- ACCESOS DIRECTOS -->
            <div class="card-box">
                <div class="card-box-header">
                    <span class="card-box-title"><i data-lucide="zap" style="width: 18px; color: var(--primary);"></i> Accesos Directos TI</span>
                </div>
                <div class="quick-links-list">
                    <a href="${pageContext.request.contextPath}/sysadmin/usuarios" class="quick-link-btn">
                        <span><i data-lucide="user-plus" style="width: 16px; margin-right: 8px; vertical-align: middle;"></i> 1. Gestionar Usuarios y Roles</span>
                        <i data-lucide="arrow-right" style="width: 16px;"></i>
                    </a>
                    <a href="${pageContext.request.contextPath}/sysadmin/sedes" class="quick-link-btn">
                        <span><i data-lucide="building-2" style="width: 16px; margin-right: 8px; vertical-align: middle;"></i> 2. Configurar Catálogo de Sedes</span>
                        <i data-lucide="arrow-right" style="width: 16px;"></i>
                    </a>
                    <a href="${pageContext.request.contextPath}/sysadmin/priorizacion" class="quick-link-btn">
                        <span><i data-lucide="sliders" style="width: 16px; margin-right: 8px; vertical-align: middle;"></i> 3. Ajustar Criterios Priorización</span>
                        <i data-lucide="arrow-right" style="width: 16px;"></i>
                    </a>
                    <a href="${pageContext.request.contextPath}/sysadmin/analitica" class="quick-link-btn">
                        <span><i data-lucide="line-chart" style="width: 16px; margin-right: 8px; vertical-align: middle;"></i> 4. Consultar Indicadores / Analítica</span>
                        <i data-lucide="arrow-right" style="width: 16px;"></i>
                    </a>
                </div>
            </div>
        </div>

        <!-- AUDIT LOG CARD -->
        <div class="card-box">
            <div class="card-box-header">
                <span class="card-box-title"><i data-lucide="file-text" style="width: 18px; color: var(--primary);"></i> Registro Reciente de Auditoría y Cambios de Configuración (Global)</span>
                <span class="badge-status badge-online">SOLO LECTURA // AUDIT LOG ACTIVO</span>
            </div>
            <table>
                <thead>
                    <tr>
                        <th>Fecha / Hora</th>
                        <th>Usuario Administrador</th>
                        <th>Módulo Objeto</th>
                        <th>Evento de Configuración</th>
                        <th>Resultado</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>2026-10-07 10:14:22</td>
                        <td>Ing. Roberto Vargas (ID: ADM-01)</td>
                        <td>MOTOR PRIORIZACIÓN</td>
                        <td>Modificación de parámetro TIEMPO_OFERTA_DESERCIÓN: ajustado de 15 min a 10 min en cola rápida de Sede Central.</td>
                        <td><span class="badge-status badge-aplicado">APLICADO</span></td>
                    </tr>
                    <tr>
                        <td>2026-10-06 17:40:05</td>
                        <td>Ing. Roberto Vargas (ID: ADM-01)</td>
                        <td>CATÁLOGO DE SEDES</td>
                        <td>Habilitación de consultorios 17 y 18 en Sede Este (Especialidad: Traumatología y Ortopedia).</td>
                        <td><span class="badge-status badge-aplicado">APLICADO</span></td>
                    </tr>
                    <tr>
                        <td>2026-10-05 14:02:49</td>
                        <td>Lic. Elena Morales (ID: ADM-02)</td>
                        <td>USUARIOS Y ROLES</td>
                        <td>Revocación de credencial administrativa por expiración de contrato temporal (Usuario: OP-4491).</td>
                        <td><span class="badge-status badge-aplicado">APLICADO</span></td>
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
