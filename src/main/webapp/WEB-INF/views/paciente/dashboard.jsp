<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombrePaciente = (String) request.getAttribute("nombrePaciente");
    if (nombrePaciente == null) nombrePaciente = "Eduardo Ayala";
    
    // Obtener iniciales del paciente
    String inicialesPaciente = "EA";
    if (nombrePaciente != null && !nombrePaciente.isEmpty()) {
        String[] partes = nombrePaciente.trim().split("\\s+");
        if (partes.length >= 2) {
            inicialesPaciente = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            inicialesPaciente = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }
    
    Map<String, String> proximaCita = (Map<String, String>) request.getAttribute("proximaCita");
    List<Map<String, String>> listaProximasCitas = (List<Map<String, String>>) request.getAttribute("listaProximasCitas");
    Map<String, Integer> stats = (Map<String, Integer>) request.getAttribute("stats");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Portal del Paciente - Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        :root {
            --primary: #2563eb;
            --primary-hover: #1d4ed8;
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

        body {
            background-color: var(--bg-page);
            color: var(--text-dark);
            display: flex;
            height: 100vh;
            overflow: hidden;
            -webkit-font-smoothing: antialiased;
            -moz-osx-font-smoothing: grayscale;
        }

        /* SIDEBAR */
        .sidebar {
            width: var(--sidebar-width);
            background-color: var(--bg-card);
            border-right: 1px solid var(--border-light);
            display: flex;
            flex-direction: column;
            padding: 24px 20px;
            flex-shrink: 0;
            z-index: 10;
        }

        .brand-container {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 40px;
            padding-left: 8px;
        }
        .brand-container img { height: 36px; }

        .nav-menu {
            display: flex;
            flex-direction: column;
            gap: 8px;
            flex: 1;
        }

        .nav-item {
            display: flex;
            align-items: center;
            gap: 14px;
            padding: 12px 16px;
            border-radius: var(--radius-md);
            color: var(--text-gray);
            text-decoration: none;
            font-size: 14px;
            font-weight: 500;
            transition: all 0.2s;
        }

        .nav-item:hover {
            background-color: #f8fafc;
            color: var(--text-dark);
        }

        .nav-item.active {
            background-color: #eff6ff;
            color: var(--primary);
            font-weight: 600;
        }

        .user-profile-bottom {
            margin-top: auto;
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 16px 8px 8px 8px;
            border-top: 1px solid var(--border-light);
            cursor: pointer;
            text-decoration: none;
        }
        
        .avatar-initials {
            display: flex; align-items: center; justify-content: center;
            border-radius: 50%; font-weight: 600; color: var(--primary);
            background-color: #eff6ff;
        }
        .avatar-lg { width: 48px; height: 48px; font-size: 16px; border: 2px solid #fff; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
        .avatar-sm { width: 36px; height: 36px; font-size: 13px; flex-shrink: 0; }

        .user-name-sm { font-size: 13px; font-weight: 600; color: var(--text-dark); line-height: 1.3; }
        .user-role-sm { font-size: 11px; color: var(--text-gray); }

        /* MAIN CONTENT */
        .main-content {
            flex: 1;
            padding: 40px 50px;
            overflow-y: auto;
        }

        /* HEADER */
        .page-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 32px;
        }

        .header-date { font-size: 13px; color: var(--text-gray); font-weight: 500; margin-bottom: 8px; text-transform: capitalize; }
        .header-title { font-size: 28px; font-weight: 700; color: var(--text-dark); margin-bottom: 4px; letter-spacing: -0.5px; }
        .header-subtitle { font-size: 14px; color: var(--text-gray); }

        .btn-bell {
            width: 44px; height: 44px;
            border-radius: 12px;
            background: #ffffff;
            border: 1px solid var(--border-light);
            display: flex; align-items: center; justify-content: center;
            color: var(--text-gray);
            position: relative;
            text-decoration: none;
            transition: all 0.2s;
        }
        .btn-bell:hover { background: #f8fafc; color: var(--text-dark); }
        .btn-bell::after {
            content: '';
            position: absolute; top: 10px; right: 12px;
            width: 8px; height: 8px; background: #ef4444; border-radius: 50%;
            border: 2px solid #ffffff;
        }

        .btn-primary {
            background-color: var(--primary);
            color: white;
            border: none;
            padding: 12px 24px;
            border-radius: 30px;
            font-size: 14px;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 8px;
            text-decoration: none;
            box-shadow: 0 4px 6px -1px rgba(37, 99, 235, 0.2);
            transition: all 0.2s;
        }
        .btn-primary:hover { background-color: var(--primary-hover); transform: translateY(-1px); box-shadow: 0 6px 8px -1px rgba(37, 99, 235, 0.3); }

        /* KPI ROW */
        .kpi-row {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 20px;
            margin-bottom: 32px;
        }

        .kpi-card {
            background: var(--bg-card);
            border-radius: var(--radius-lg);
            padding: 24px;
            box-shadow: 0 4px 10px rgba(0,0,0,0.02);
            display: flex;
            flex-direction: column;
            gap: 16px;
            position: relative;
        }

        .kpi-badge {
            position: absolute; top: 24px; right: 24px;
            padding: 4px 10px; border-radius: 20px;
            font-size: 9px; font-weight: 700;
            text-transform: uppercase; letter-spacing: 0.5px;
        }
        .badge-neutral { background: #f1f5f9; color: #475569; }
        .badge-warning { background: #fef2f2; color: #dc2626; }

        .kpi-icon-wrapper {
            width: 32px; height: 32px;
            border-radius: 8px;
            display: flex; align-items: center; justify-content: center;
        }

        .kpi-number {
            font-size: 32px;
            font-weight: 700;
            color: var(--primary);
            line-height: 1;
            letter-spacing: -1px;
        }

        .kpi-label {
            font-size: 13px;
            color: var(--text-gray);
            font-weight: 500;
        }

        /* GRID MAIN */
        .main-grid {
            display: grid;
            grid-template-columns: 2fr 1fr;
            gap: 24px;
        }

        .section-card {
            background: var(--bg-card);
            border-radius: var(--radius-lg);
            padding: 24px;
            box-shadow: 0 4px 10px rgba(0,0,0,0.02);
            margin-bottom: 24px;
        }

        .section-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 24px;
        }

        .section-title {
            font-size: 16px;
            font-weight: 700;
            color: var(--text-dark);
        }

        .link-all {
            font-size: 12px;
            color: var(--primary);
            text-decoration: none;
            font-weight: 600;
        }

        /* LISTA CITAS */
        .appt-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 16px;
            background: #ffffff;
            border-radius: var(--radius-md);
            margin-bottom: 12px;
            border: 1px solid var(--border-light);
        }
        .appt-item:last-child { margin-bottom: 0; }
        
        .appt-doc-info {
            display: flex;
            align-items: center;
            gap: 16px;
        }
        
        .doc-name { font-weight: 600; font-size: 15px; color: var(--text-dark); margin-bottom: 4px; }
        .doc-spec { font-size: 13px; color: var(--text-gray); }

        .appt-meta {
            display: flex;
            align-items: center;
            gap: 16px;
            font-size: 12px;
            color: var(--text-gray);
            margin-top: 8px;
        }
        .appt-meta span { display: flex; align-items: center; gap: 4px; }
        .badge-video { color: #10b981; background: #d1fae5; padding: 2px 8px; border-radius: 12px; font-weight: 600; }
        .badge-pres { color: #3b82f6; background: #dbeafe; padding: 2px 8px; border-radius: 12px; font-weight: 600; }

        .btn-confirm {
            background: var(--primary);
            color: white;
            border: none;
            padding: 8px 16px;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            transition: background 0.2s;
        }
        .btn-confirm:hover { background: var(--primary-hover); }

        /* SIGNOS VITALES GRID */
        .vitals-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 16px;
        }
        .vital-box {
            border: none;
            border-radius: var(--radius-md);
            padding: 16px;
        }
        .vital-label { font-size: 13px; color: var(--text-gray); margin-bottom: 12px; font-weight: 500; }
        .vital-val { font-size: 28px; font-weight: 700; color: var(--primary); margin-bottom: 4px; line-height: 1; letter-spacing: -0.5px; }
        .vital-unit { font-size: 14px; font-weight: 500; color: var(--text-gray); }
        .vital-trend { font-size: 12px; color: var(--text-gray); }

        /* ACCIONES RAPIDAS */
        .action-list {
            display: flex;
            flex-direction: column;
            gap: 12px;
        }
        .action-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 14px 16px;
            background: #ffffff;
            border: 1px solid var(--border-light);
            border-radius: var(--radius-md);
            text-decoration: none;
            color: var(--text-dark);
            font-size: 14px;
            font-weight: 600;
            transition: all 0.2s;
        }
        .action-item:hover { background: #f8fafc; border-color: #cbd5e1; }
        .action-left { display: flex; align-items: center; gap: 12px; }
        .action-icon { color: var(--text-gray); }

        /* EQUIPO MEDICO */
        .team-list {
            display: flex;
            flex-direction: column;
            gap: 16px;
        }
        .team-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .team-info { display: flex; align-items: center; gap: 12px; }
        .team-doc { font-size: 14px; font-weight: 600; color: var(--text-dark); }
        .team-spec { font-size: 12px; color: var(--text-gray); }
        .team-link { font-size: 12px; color: var(--primary); text-decoration: none; font-weight: 600; padding: 6px 14px; background: #eff6ff; border-radius: 12px; transition: background 0.2s; }
        .team-link:hover { background: #dbeafe; }
        
    </style>
</head>
<body>

    <!-- SIDEBAR -->
    <aside class="sidebar">
        <div class="brand-container">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>

        <nav class="nav-menu">
            <a href="${pageContext.request.contextPath}/paciente/dashboard" class="nav-item active">
                <i data-lucide="home" style="width: 18px;"></i> Inicio
            </a>
            <a href="#" class="nav-item">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> Panel
            </a>
            <a href="#" class="nav-item">
                <i data-lucide="users" style="width: 18px;"></i> Médicos
            </a>
            <a href="${pageContext.request.contextPath}/paciente/citas" class="nav-item">
                <i data-lucide="calendar" style="width: 18px;"></i> Citas
            </a>
            <a href="#" class="nav-item">
                <i data-lucide="clipboard-list" style="width: 18px;"></i> Historial
            </a>
            <a href="#" class="nav-item">
                <i data-lucide="bar-chart-2" style="width: 18px;"></i> Reportes
            </a>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="nav-item">
                <i data-lucide="bell" style="width: 18px;"></i> Alertas
            </a>
            <a href="${pageContext.request.contextPath}/paciente/perfil" class="nav-item">
                <i data-lucide="user" style="width: 18px;"></i> Mi perfil
            </a>
        </nav>

        <a href="${pageContext.request.contextPath}/logout" class="user-profile-bottom">
            <div class="avatar-initials avatar-sm"><%= inicialesPaciente %></div>
            <div style="flex: 1;">
                <div class="user-name-sm"><%= nombrePaciente %></div>
                <div class="user-role-sm">Paciente</div>
            </div>
            <i data-lucide="log-out" style="width: 16px; color: var(--text-gray);"></i>
        </a>
    </aside>

    <!-- MAIN CONTENT -->
    <main class="main-content">
        
        <div class="page-header">
            <div>
                <div class="header-date">Lunes, 7 de septiembre 2026</div>
                <h1 class="header-title">Buenos días, <%= nombrePaciente.split(" ")[0] %></h1>
                <p class="header-subtitle">Tienes <%= stats != null ? stats.get("proximasCitas") : 2 %> citas programadas esta semana.</p>
            </div>
            <div style="display: flex; align-items: center; gap: 16px;">
                <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-bell">
                    <i data-lucide="bell" style="width: 20px;"></i>
                </a>
                <a href="${pageContext.request.contextPath}/paciente/citas?action=nueva" class="btn-primary">
                    <i data-lucide="plus" style="width: 18px;"></i>
                    Programar cita
                </a>
            </div>
        </div>

        <!-- KPI CARDS -->
        <div class="kpi-row">
            <div class="kpi-card">
                <span class="kpi-badge badge-neutral">este mes</span>
                <div class="kpi-icon-wrapper" style="background: #eff6ff; color: var(--primary);">
                    <i data-lucide="calendar" style="width: 18px;"></i>
                </div>
                <div>
                    <div class="kpi-number"><%= stats != null ? stats.get("proximasCitas") : 2 %></div>
                    <div class="kpi-label">Próximas citas</div>
                </div>
            </div>
            
            <div class="kpi-card">
                <span class="kpi-badge badge-neutral">activos</span>
                <div class="kpi-icon-wrapper" style="background: #eff6ff; color: var(--primary);">
                    <i data-lucide="stethoscope" style="width: 18px;"></i>
                </div>
                <div>
                    <div class="kpi-number"><%= stats != null ? stats.get("medicosAsignados") : 4 %></div>
                    <div class="kpi-label">Médicos asignados</div>
                </div>
            </div>
            
            <div class="kpi-card">
                <span class="kpi-badge badge-warning">revisión requerida</span>
                <div class="kpi-icon-wrapper" style="background: #eff6ff; color: var(--primary);">
                    <i data-lucide="microscope" style="width: 18px;"></i>
                </div>
                <div>
                    <div class="kpi-number"><%= stats != null ? stats.get("estudiosPendientes") : 1 %></div>
                    <div class="kpi-label">Estudios pendientes</div>
                </div>
            </div>
            
            <div class="kpi-card">
                <span class="kpi-badge badge-neutral">historial</span>
                <div class="kpi-icon-wrapper" style="background: #eff6ff; color: var(--primary);">
                    <i data-lucide="clipboard-list" style="width: 18px;"></i>
                </div>
                <div>
                    <div class="kpi-number"><%= stats != null ? stats.get("consultasTotales") : 18 %></div>
                    <div class="kpi-label">Consultas totales</div>
                </div>
            </div>
        </div>

        <!-- MAIN CONTENT GRID -->
        <div class="main-grid">
            
            <!-- LEFT COLUMN -->
            <div>
                <!-- PROXIMAS CITAS LIST -->
                <div class="section-card">
                    <div class="section-header">
                        <div class="section-title">Próximas citas</div>
                        <a href="${pageContext.request.contextPath}/paciente/citas" class="link-all">Ver todas &rarr;</a>
                    </div>
                    
                    <div class="appt-item">
                        <div class="appt-doc-info">
                            <div class="avatar-initials avatar-lg">EV</div>
                            <div>
                                <div class="doc-name">Dr. Elena Vargas</div>
                                <div class="doc-spec">Cardiología</div>
                                <div class="appt-meta">
                                    <span><i data-lucide="calendar" style="width: 14px;"></i> Lun 07 Sept</span>
                                    <span><i data-lucide="clock" style="width: 14px;"></i> 15:00</span>
                                    <span class="badge-pres"><i data-lucide="map-pin" style="width: 12px;"></i> Presencial</span>
                                </div>
                            </div>
                        </div>
                        <button class="btn-confirm">Confirmar</button>
                    </div>

                    <div class="appt-item">
                        <div class="appt-doc-info">
                            <div class="avatar-initials avatar-lg" style="color:#8b5cf6; background:#f5f3ff;">MO</div>
                            <div>
                                <div class="doc-name">Dr. Marcus Okonkwo</div>
                                <div class="doc-spec">Neurología</div>
                                <div class="appt-meta">
                                    <span><i data-lucide="calendar" style="width: 14px;"></i> Lun 14 Sept</span>
                                    <span><i data-lucide="clock" style="width: 14px;"></i> 10:00</span>
                                    <span class="badge-video"><i data-lucide="video" style="width: 12px;"></i> Video</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- SIGNOS VITALES -->
                <div class="section-card">
                    <div class="section-header">
                        <div class="section-title">Signos vitales recientes</div>
                        <div style="font-size: 12px; color: var(--text-gray);">Actualizado: 7 Ago</div>
                    </div>
                    
                    <div class="vitals-grid">
                        <div class="vital-box" style="background: #eff6ff;">
                            <div class="vital-label">Presión arterial</div>
                            <div class="vital-val" style="color: #1d4ed8;">118/76 <span class="vital-unit">mmHg</span></div>
                            <div class="vital-trend" style="color: var(--primary);">+0 vs última</div>
                        </div>
                        <div class="vital-box" style="background: #f0f9ff;">
                            <div class="vital-label">Frecuencia cardíaca</div>
                            <div class="vital-val" style="color: #0369a1;">72 <span class="vital-unit">bpm</span></div>
                            <div class="vital-trend" style="color: var(--text-gray);">-3 vs última</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- RIGHT COLUMN -->
            <div>
                <!-- ACCIONES RAPIDAS -->
                <div class="section-card">
                    <div class="section-header">
                        <div class="section-title">Acciones rápidas</div>
                    </div>
                    
                    <div class="action-list">
                        <a href="${pageContext.request.contextPath}/paciente/citas?action=nueva" class="action-item">
                            <div class="action-left">
                                <i data-lucide="search" class="action-icon" style="width: 18px;"></i>
                                Buscar médico
                            </div>
                            <i data-lucide="chevron-right" style="width: 18px; color: var(--text-gray);"></i>
                        </a>
                        <a href="#" class="action-item">
                            <div class="action-left">
                                <i data-lucide="clipboard-list" class="action-icon" style="width: 18px;"></i>
                                Ver historial
                            </div>
                            <i data-lucide="chevron-right" style="width: 18px; color: var(--text-gray);"></i>
                        </a>
                        <a href="#" class="action-item">
                            <div class="action-left">
                                <i data-lucide="bar-chart-2" class="action-icon" style="width: 18px;"></i>
                                Mis reportes
                            </div>
                            <i data-lucide="chevron-right" style="width: 18px; color: var(--text-gray);"></i>
                        </a>
                    </div>
                </div>

                <!-- EQUIPO MEDICO -->
                <div class="section-card">
                    <div class="section-header">
                        <div class="section-title">Mi equipo médico</div>
                        <a href="#" class="link-all">Ver más</a>
                    </div>
                    
                    <div class="team-list">
                        <div class="team-item">
                            <div class="team-info">
                                <div class="avatar-initials avatar-sm">EV</div>
                                <div>
                                    <div class="team-doc">Dr. Elena Vargas</div>
                                    <div class="team-spec">Cardiología</div>
                                </div>
                            </div>
                            <a href="#" class="team-link">Cita</a>
                        </div>
                        <div class="team-item">
                            <div class="team-info">
                                <div class="avatar-initials avatar-sm" style="color:#8b5cf6; background:#f5f3ff;">MO</div>
                                <div>
                                    <div class="team-doc">Dr. Marcus Okonkwo</div>
                                    <div class="team-spec">Neurología</div>
                                </div>
                            </div>
                            <a href="#" class="team-link">Cita</a>
                        </div>
                        <div class="team-item">
                            <div class="team-info">
                                <div class="avatar-initials avatar-sm" style="color:#10b981; background:#ecfdf5;">PS</div>
                                <div>
                                    <div class="team-doc">Dr. Priya Sharma</div>
                                    <div class="team-spec">Medicina Interna</div>
                                </div>
                            </div>
                            <a href="#" class="team-link">Cita</a>
                        </div>
                    </div>
                </div>

            </div>

        </div>
    </main>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
