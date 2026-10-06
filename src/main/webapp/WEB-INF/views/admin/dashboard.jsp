<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard Administrador - Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        :root {
            /* Paleta Cohesiva "Clean Medical" */
            --primary: #2563eb;         /* Azul médico corporativo (botones, acentos) */
            --primary-light: #eff6ff;   /* Fondo muy suave para elementos activos */
            
            --bg-body: #f8fafc;         /* Fondo general ligerísimamente gris para contraste */
            --bg-surface: #ffffff;      /* Tarjetas, sidebar y topbar blancos */
            
            --text-main: #0f172a;       /* Texto principal oscuro */
            --text-muted: #64748b;      /* Texto secundario gris */
            --border-light: #e2e8f0;    /* Bordes sutiles */
            
            --shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05);
            --shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.05), 0 2px 4px -2px rgb(0 0 0 / 0.05);
            
            --radius-md: 8px;
            --radius-lg: 12px;
        }

        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Inter', sans-serif; }
        
        body { 
            display: flex; 
            flex-direction: column;
            height: 100vh; 
            background-color: var(--bg-body); 
            overflow: hidden; 
            color: var(--text-main); 
        }

        /* HEADER SUPERIOR (TOPBAR) */
        .topbar {
            height: 64px;
            flex-shrink: 0;
            background-color: var(--bg-surface);
            border-bottom: 1px solid var(--border-light);
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 32px;
            z-index: 10;
        }

        .topbar-brand {
            display: flex;
            align-items: center;
            gap: 12px;
            font-weight: 700;
            color: var(--text-main);
            letter-spacing: -0.5px;
            font-size: 16px;
        }
        .topbar-brand img { height: 44px; }

        .topbar-right {
            display: flex;
            align-items: center;
            gap: 20px;
        }
        .user-info { display: flex; flex-direction: column; align-items: flex-end; }
        .user-name { font-weight: 600; font-size: 13px; color: var(--text-main); }
        .user-role { font-size: 11px; color: var(--text-muted); font-weight: 500; text-transform: uppercase; letter-spacing: 0.5px; }
        
        .user-avatar {
            width: 36px; height: 36px;
            border-radius: 50%;
            background-color: var(--primary-light);
            color: var(--primary);
            display: flex; align-items: center; justify-content: center;
        }

        .btn-logout {
            display: flex;
            align-items: center;
            gap: 6px;
            padding: 8px 12px;
            border-radius: var(--radius-md);
            font-size: 13px;
            font-weight: 500;
            color: var(--text-muted);
            text-decoration: none;
            transition: all 0.2s ease;
        }
        .btn-logout:hover { color: #ef4444; background: #fef2f2; }

        /* CONTENEDOR INFERIOR */
        .app-body {
            display: flex;
            height: calc(100vh - 64px);
            overflow: hidden;
        }

        /* SIDEBAR BLANCO */
        .sidebar {
            width: 260px;
            background-color: var(--bg-surface);
            border-right: 1px solid var(--border-light);
            padding: 24px 16px;
            display: flex;
            flex-direction: column;
            gap: 6px;
            flex-shrink: 0;
            overflow-y: auto;
        }

        .nav-item {
            padding: 12px 16px;
            display: flex;
            align-items: center;
            gap: 12px;
            color: var(--text-muted);
            text-decoration: none;
            font-size: 14px;
            font-weight: 500;
            border-radius: var(--radius-md);
            transition: all 0.2s ease;
        }

        .nav-item:hover {
            background-color: var(--bg-body);
            color: var(--text-main);
        }

        .nav-item.active {
            background-color: var(--primary-light);
            color: var(--primary);
            font-weight: 600;
        }
        .nav-item.active svg { color: var(--primary); }

        /* ÁREA PRINCIPAL */
        .main-content {
            flex: 1;
            height: 100%;
            overflow-y: auto;
            padding: 32px 40px;
        }

        /* HEADER DE SECCIÓN */
        .page-header { margin-bottom: 24px; }
        .page-title {
            font-size: 24px;
            font-weight: 700;
            color: var(--text-main);
            margin-bottom: 6px;
            letter-spacing: -0.5px;
        }
        .page-subtitle { font-size: 14px; color: var(--text-muted); }

        /* KPIS GRID */
        .kpi-grid {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 20px;
            margin-bottom: 24px;
        }
        .kpi-card {
            background: var(--bg-surface);
            border-radius: var(--radius-lg);
            padding: 20px;
            box-shadow: var(--shadow-sm);
            border: 1px solid var(--border-light);
        }
        .kpi-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
        .kpi-title { font-size: 13px; font-weight: 500; color: var(--text-muted); }
        .kpi-icon { color: var(--primary); }
        .kpi-value { font-size: 28px; font-weight: 700; color: var(--text-main); line-height: 1; margin-bottom: 8px; letter-spacing: -0.5px; }
        .kpi-desc { font-size: 12px; color: var(--text-muted); line-height: 1.4; }

        /* SECCIONES SECUNDARIAS (GRID 2 COLUMNAS) */
        .content-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 20px;
            margin-bottom: 24px;
        }

        .panel {
            background: var(--bg-surface);
            border-radius: var(--radius-lg);
            padding: 24px;
            box-shadow: var(--shadow-sm);
            border: 1px solid var(--border-light);
        }

        .panel-title {
            font-size: 16px;
            font-weight: 600;
            color: var(--text-main);
            margin-bottom: 20px;
        }

        /* TABLA */
        .data-table { width: 100%; border-collapse: separate; border-spacing: 0; }
        .data-table th, .data-table td { padding: 12px 0; text-align: left; border-bottom: 1px solid var(--border-light); font-size: 13px; }
        .data-table th { font-size: 11px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; padding-bottom: 8px; letter-spacing: 0.5px; }
        .data-table tr:last-child td { border-bottom: none; }
        
        .badge { padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; display: inline-flex; align-items: center; gap: 4px; }
        .badge-online { background: #dcfce7; color: #166534; }
        .badge-indicator { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
        
        /* BARRA DE PROGRESO */
        .progress-container { width: 100px; }
        .progress-header { display: flex; justify-content: space-between; font-size: 11px; font-weight: 500; margin-bottom: 4px; color: var(--text-main); }
        .progress-bar { width: 100%; height: 4px; background: var(--bg-body); border-radius: 2px; overflow: hidden; }
        .progress-fill { height: 100%; background: var(--primary); border-radius: 2px; }

        /* AUDITORIA Y ACTIVIDAD */
        .activity-item {
            display: flex;
            gap: 12px;
            padding-bottom: 12px;
            margin-bottom: 12px;
            border-bottom: 1px solid var(--border-light);
        }
        .activity-item:last-child { border-bottom: none; margin-bottom: 0; padding-bottom: 0; }
        .activity-icon {
            width: 32px; height: 32px; border-radius: 50%;
            background: var(--bg-body); color: var(--text-muted);
            display: flex; align-items: center; justify-content: center; flex-shrink: 0;
        }
        .activity-content p { font-size: 13px; color: var(--text-main); line-height: 1.4; margin-bottom: 2px; }
        .activity-content p strong { font-weight: 600; }
        .activity-time { font-size: 11px; color: var(--text-muted); }
        
        .text-success { color: #16a34a; font-weight: 500; }

    </style>
</head>
<body>

    <header class="topbar">
        <div class="topbar-brand">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>
        <div class="topbar-right">
            <div class="user-info">
                <span class="user-name"><c:out value="${sessionScope.nombreUsuario}" default="Roberto Vargas" /></span>
                <span class="user-role">Administrador del Sistema</span>
            </div>
            <div class="user-avatar">
                <i data-lucide="user" style="width: 18px; height: 18px;"></i>
            </div>
            <div style="width: 1px; height: 24px; background: var(--border-light); margin: 0 4px;"></div>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">
                <i data-lucide="log-out" style="width: 16px; height: 16px;"></i>
                Salir
            </a>
        </div>
    </header>

    <div class="app-body">
        <aside class="sidebar">
            <a href="${pageContext.request.contextPath}/admin/dashboard" class="nav-item active">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i>
                Inicio
            </a>
            <a href="${pageContext.request.contextPath}/admin/usuarios" class="nav-item">
                <i data-lucide="users" style="width: 18px;"></i>
                Usuarios y Roles
            </a>
            <a href="#" class="nav-item">
                <i data-lucide="building-2" style="width: 18px;"></i>
                Gestión de Sedes
            </a>
            <a href="#" class="nav-item">
                <i data-lucide="sliders-horizontal" style="width: 18px;"></i>
                Config. Priorización
            </a>
            <a href="#" class="nav-item">
                <i data-lucide="pie-chart" style="width: 18px;"></i>
                Métricas
            </a>
            
            <a href="${pageContext.request.contextPath}/admin/perfil" class="nav-item" style="margin-top: auto; border-top: 1px solid var(--border-light); border-radius: 0; padding-top: 16px;">
                <i data-lucide="user-cog" style="width: 18px;"></i>
                Mi Perfil
            </a>
        </aside>

        <main class="main-content">
            <div class="page-header">
                <h1 class="page-title">Vista General</h1>
                <p class="page-subtitle">Monitoreo en tiempo real de infraestructura, usuarios y rendimiento.</p>
            </div>

            <div class="kpi-grid">
                <div class="kpi-card">
                    <div class="kpi-header">
                        <span class="kpi-title">Usuarios Activos</span>
                        <div class="kpi-icon"><i data-lucide="users" style="width:20px; height:20px;"></i></div>
                    </div>
                    <div class="kpi-value">1,420</div>
                    <div class="kpi-desc">1,250 Pacientes • 120 Médicos<br>48 Personal • 2 Admins</div>
                </div>
                <div class="kpi-card">
                    <div class="kpi-header">
                        <span class="kpi-title">Sedes Operativas</span>
                        <div class="kpi-icon"><i data-lucide="building" style="width:20px; height:20px;"></i></div>
                    </div>
                    <div class="kpi-value">4</div>
                    <div class="kpi-desc">Norte, Central, Sur, Este<br>98 consultorios habilitados</div>
                </div>
                <div class="kpi-card">
                    <div class="kpi-header">
                        <span class="kpi-title">Citas Mensuales</span>
                        <div class="kpi-icon"><i data-lucide="calendar" style="width:20px; height:20px;"></i></div>
                    </div>
                    <div class="kpi-value">8,640</div>
                    <div class="kpi-desc">89.2% Cumplimiento asistencial<br><span class="text-success">+4.8% vs. mes anterior</span></div>
                </div>
                <div class="kpi-card">
                    <div class="kpi-header">
                        <span class="kpi-title">Motor de Reasignación</span>
                        <div class="kpi-icon"><i data-lucide="activity" style="width:20px; height:20px;"></i></div>
                    </div>
                    <div class="kpi-value">74.5%</div>
                    <div class="kpi-desc">Recuperados este mes<br>1,095 cupos salvados</div>
                </div>
            </div>

            <div class="content-grid">
                <!-- PANEL IZQUIERDO -->
                <div class="panel">
                    <div class="panel-title">Estado de Red por Sede</div>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Sede</th>
                                <th>Estado</th>
                                <th>Ocupación</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>
                                    <div style="font-weight:500; color:var(--text-main);">Sede Central</div>
                                    <div style="font-size:11px; color:var(--text-muted);">Metropolitano</div>
                                </td>
                                <td><span class="badge badge-online"><div class="badge-indicator"></div> Sincronizado</span></td>
                                <td>
                                    <div class="progress-container">
                                        <div class="progress-header"><span></span><span>92.4%</span></div>
                                        <div class="progress-bar"><div class="progress-fill" style="width:92.4%;"></div></div>
                                    </div>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    <div style="font-weight:500; color:var(--text-main);">Sede Norte</div>
                                    <div style="font-size:11px; color:var(--text-muted);">Valle</div>
                                </td>
                                <td><span class="badge badge-online"><div class="badge-indicator"></div> Sincronizado</span></td>
                                <td>
                                    <div class="progress-container">
                                        <div class="progress-header"><span></span><span>88.1%</span></div>
                                        <div class="progress-bar"><div class="progress-fill" style="width:88.1%;"></div></div>
                                    </div>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    <div style="font-weight:500; color:var(--text-main);">Sede Sur</div>
                                    <div style="font-size:11px; color:var(--text-muted);">Parque Industrial</div>
                                </td>
                                <td><span class="badge badge-online"><div class="badge-indicator"></div> Sincronizado</span></td>
                                <td>
                                    <div class="progress-container">
                                        <div class="progress-header"><span></span><span>79.5%</span></div>
                                        <div class="progress-bar"><div class="progress-fill" style="width:79.5%;"></div></div>
                                    </div>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <!-- PANEL DERECHO: ACTIVIDAD RECIENTE -->
                <div class="panel">
                    <div class="panel-title">Auditoría Reciente</div>
                    
                    <div class="activity-item">
                        <div class="activity-icon" style="background:#dcfce7; color:#166534;"><i data-lucide="settings" style="width:16px;"></i></div>
                        <div class="activity-content">
                            <p><strong>Roberto Vargas</strong> ajustó tiempo de espera en Sede Central.</p>
                            <span class="activity-time">Hoy, 10:14 AM</span>
                        </div>
                    </div>

                    <div class="activity-item">
                        <div class="activity-icon" style="background:#eff6ff; color:#2563eb;"><i data-lucide="user-plus" style="width:16px;"></i></div>
                        <div class="activity-content">
                            <p><strong>Roberto Vargas</strong> creó el perfil médico para Dr. Carlos Mendoza.</p>
                            <span class="activity-time">Ayer, 17:40 PM</span>
                        </div>
                    </div>

                    <div class="activity-item">
                        <div class="activity-icon" style="background:#fef2f2; color:#dc2626;"><i data-lucide="user-minus" style="width:16px;"></i></div>
                        <div class="activity-content">
                            <p><strong>Sistema</strong> revocó acceso a OP-4491 por expiración de contrato.</p>
                            <span class="activity-time">Ayer, 14:02 PM</span>
                        </div>
                    </div>

                    <a href="#" style="display:inline-block; margin-top:12px; font-size:13px; font-weight:500; color:var(--primary); text-decoration:none;">Ver todo el historial &rarr;</a>
                </div>
            </div>

        </main>
    </div>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
