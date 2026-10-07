<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombrePaciente = (String) request.getAttribute("nombrePaciente");
    if (nombrePaciente == null) nombrePaciente = "Eduardo Ayala";

    String inicialesPaciente = "EA";
    if (nombrePaciente != null && !nombrePaciente.isEmpty()) {
        String[] partes = nombrePaciente.trim().split("\\s+");
        if (partes.length >= 2) {
            inicialesPaciente = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            inicialesPaciente = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reportes de Salud - Clínica Internacional</title>
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

        .btn-bell {
            width: 44px; height: 44px; border-radius: 12px; background: #ffffff; border: 1px solid var(--border-light);
            display: flex; align-items: center; justify-content: center; color: var(--text-gray); position: relative;
            text-decoration: none; transition: all 0.2s;
        }
        .btn-bell:hover { background: #f8fafc; color: var(--text-dark); }
        .btn-bell::after {
            content: ''; position: absolute; top: 10px; right: 12px; width: 8px; height: 8px; background: #ef4444; border-radius: 50%; border: 2px solid #ffffff;
        }

        .main-content { flex: 1; padding: 40px 50px; overflow-y: auto; }
        
        .header-row { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
        .header-title { font-size: 28px; font-weight: 700; color: var(--text-dark); margin-bottom: 4px; letter-spacing: -0.5px; }
        .header-subtitle { font-size: 14px; color: var(--text-gray); }

        .time-range-tabs { display: flex; gap: 4px; background: #ffffff; padding: 4px; border-radius: 30px; border: 1px solid var(--border-light); }
        .time-btn { padding: 6px 14px; border-radius: 20px; border: none; background: transparent; font-size: 12px; font-weight: 600; color: var(--text-gray); cursor: pointer; }
        .time-btn.active { background: var(--primary); color: #ffffff; }

        /* GRID INDICADORES */
        .metrics-grid {
            display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; margin-bottom: 32px;
        }
        .metric-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 24px; box-shadow: 0 4px 16px rgba(0,0,0,0.02); display: flex; flex-direction: column; justify-content: space-between;
            position: relative; overflow: hidden;
        }
        .metric-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
        .metric-title { font-size: 14px; font-weight: 600; color: var(--text-dark); }
        .metric-badge { padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; background: #fef3c7; color: #d97706; }
        .metric-val { font-size: 32px; font-weight: 800; color: var(--text-dark); margin-bottom: 4px; }
        .metric-unit { font-size: 14px; font-weight: 500; color: var(--text-gray); }
        .metric-ref { font-size: 12px; color: var(--text-light); }

        .chart-svg { width: 100%; height: 60px; margin-top: 16px; }

        /* LAB TABLE */
        .table-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            box-shadow: 0 4px 20px rgba(0,0,0,0.02); overflow: hidden;
        }
        .table-header-title { padding: 20px 24px 12px; display: flex; justify-content: space-between; align-items: center; }
        .table-header-title h3 { font-size: 16px; font-weight: 700; color: var(--text-dark); }
        .table-header-title span { font-size: 12px; color: var(--text-gray); }

        table { width: 100%; border-collapse: collapse; text-align: left; }
        th {
            background: #f8fafc; padding: 14px 24px; font-size: 11px; font-weight: 700;
            color: var(--text-gray); text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 1px solid var(--border-light);
        }
        td { padding: 16px 24px; font-size: 14px; color: var(--text-dark); border-bottom: 1px solid #f1f5f9; }
        tr:last-child td { border-bottom: none; }

        .badge-normal { background: #dcfce7; color: #166534; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; display: inline-block; }

        @media(max-width:768px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } .metrics-grid { grid-template-columns: 1fr; } }
    </style>
</head>
<body>

    <aside class="sidebar">
        <div class="brand-container">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>
        <nav class="nav-menu">
            <a href="${pageContext.request.contextPath}/paciente/dashboard" class="nav-item">
                <i data-lucide="home" style="width: 18px;"></i> Inicio
            </a>
            <a href="${pageContext.request.contextPath}/paciente/dashboard" class="nav-item">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> Panel
            </a>
            <a href="${pageContext.request.contextPath}/paciente/medicos" class="nav-item">
                <i data-lucide="users" style="width: 18px;"></i> Médicos
            </a>
            <a href="${pageContext.request.contextPath}/paciente/citas" class="nav-item">
                <i data-lucide="calendar" style="width: 18px;"></i> Citas
            </a>
            <a href="${pageContext.request.contextPath}/paciente/historial" class="nav-item">
                <i data-lucide="clipboard-list" style="width: 18px;"></i> Historial
            </a>
            <a href="${pageContext.request.contextPath}/paciente/reportes" class="nav-item active">
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
            <div class="avatar-initials"><%= inicialesPaciente %></div>
            <div style="flex: 1;">
                <div class="user-name-sm"><%= nombrePaciente %></div>
                <div class="user-role-sm">Paciente</div>
            </div>
            <i data-lucide="log-out" style="width: 16px; color: var(--text-gray);"></i>
        </a>
    </aside>

    <main class="main-content">
        <div class="header-row">
            <div>
                <h1 class="header-title">Reportes de Salud</h1>
                <p class="header-subtitle">Seguimiento de tus indicadores clínicos a lo largo del tiempo.</p>
            </div>
            <div style="display: flex; align-items: center; gap: 16px;">
                <div class="time-range-tabs">
                    <button class="time-btn">1M</button>
                    <button class="time-btn">3M</button>
                    <button class="time-btn active">6M</button>
                    <button class="time-btn">1A</button>
                </div>
                <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-bell" title="Alertas">
                    <i data-lucide="bell" style="width: 20px;"></i>
                </a>
            </div>
        </div>

        <div class="metrics-grid">
            <!-- 1. Presión Arterial -->
            <div class="metric-card">
                <div>
                    <div class="metric-header">
                        <div class="metric-title">Presión arterial sistólica</div>
                    </div>
                    <div class="metric-val">118 <span class="metric-unit">mmHg</span> <span class="metric-ref">ref: &lt; 120</span></div>
                </div>
                <svg class="chart-svg" viewBox="0 0 300 60" preserveAspectRatio="none">
                    <path d="M0 20 Q 75 50, 150 30 T 300 45" fill="none" stroke="#2563eb" stroke-width="3"/>
                    <path d="M0 20 Q 75 50, 150 30 T 300 45 L 300 60 L 0 60 Z" fill="rgba(37, 99, 235, 0.1)"/>
                    <circle cx="300" cy="45" r="4" fill="#2563eb"/>
                </svg>
            </div>

            <!-- 2. Frecuencia Cardiaca -->
            <div class="metric-card">
                <div>
                    <div class="metric-header">
                        <div class="metric-title">Frecuencia cardiaca</div>
                    </div>
                    <div class="metric-val" style="color: #8b5cf6;">72 <span class="metric-unit">bpm</span> <span class="metric-ref">ref: 60-100</span></div>
                </div>
                <svg class="chart-svg" viewBox="0 0 300 60" preserveAspectRatio="none">
                    <path d="M0 10 L 60 40 L 120 15 L 180 45 L 240 25 L 300 50" fill="none" stroke="#8b5cf6" stroke-width="3"/>
                    <path d="M0 10 L 60 40 L 120 15 L 180 45 L 240 25 L 300 50 L 300 60 L 0 60 Z" fill="rgba(139, 92, 246, 0.1)"/>
                    <circle cx="300" cy="50" r="4" fill="#8b5cf6"/>
                </svg>
            </div>

            <!-- 3. Glucosa -->
            <div class="metric-card">
                <div>
                    <div class="metric-header">
                        <div class="metric-title">Glucosa en sangre</div>
                        <span class="metric-badge">Atención</span>
                    </div>
                    <div class="metric-val" style="color: #f59e0b;">105 <span class="metric-unit">mg/dL</span> <span class="metric-ref">ref: 70-99</span></div>
                </div>
                <svg class="chart-svg" viewBox="0 0 300 60" preserveAspectRatio="none">
                    <path d="M0 45 Q 75 20, 150 10 T 300 30" fill="none" stroke="#f59e0b" stroke-width="3"/>
                    <path d="M0 45 Q 75 20, 150 10 T 300 30 L 300 60 L 0 60 Z" fill="rgba(245, 158, 11, 0.1)"/>
                    <circle cx="300" cy="30" r="4" fill="#f59e0b"/>
                </svg>
            </div>

            <!-- 4. Consultas Médicas -->
            <div class="metric-card">
                <div>
                    <div class="metric-header">
                        <div class="metric-title">Consultas médicas</div>
                    </div>
                    <div class="metric-val" style="color: #10b981;">2 <span class="metric-unit">total</span> <span class="metric-ref">ref: este mes</span></div>
                </div>
                <svg class="chart-svg" viewBox="0 0 300 60" preserveAspectRatio="none">
                    <path d="M0 50 L 75 20 L 150 40 L 225 15 L 300 35" fill="none" stroke="#10b981" stroke-width="3"/>
                    <path d="M0 50 L 75 20 L 150 40 L 225 15 L 300 35 L 300 60 L 0 60 Z" fill="rgba(16, 185, 129, 0.1)"/>
                    <circle cx="300" cy="35" r="4" fill="#10b981"/>
                </svg>
            </div>
        </div>

        <!-- TABLA RESULTADOS DE LABORATORIO -->
        <div class="table-card">
            <div class="table-header-title">
                <h3>Últimos resultados de laboratorio</h3>
                <span>14 Jun 2026 · CBC completo</span>
            </div>
            <table>
                <thead>
                    <tr>
                        <th>EXAMEN</th>
                        <th>RESULTADO</th>
                        <th>UNIDAD</th>
                        <th>REFERENCIA</th>
                        <th>ESTADO</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td style="font-weight: 600;">Hemoglobina</td>
                        <td style="font-weight: 700;">14.8</td>
                        <td style="color: var(--text-gray);">g/dL</td>
                        <td style="color: var(--text-gray);">13.5 - 17.5</td>
                        <td><span class="badge-normal">Normal</span></td>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Leucocitos</td>
                        <td style="font-weight: 700;">7,200</td>
                        <td style="color: var(--text-gray);">/µL</td>
                        <td style="color: var(--text-gray);">4,500 - 11,000</td>
                        <td><span class="badge-normal">Normal</span></td>
                    </tr>
                    <tr>
                        <td style="font-weight: 600;">Colesterol Total</td>
                        <td style="font-weight: 700;">192</td>
                        <td style="color: var(--text-gray);">mg/dL</td>
                        <td style="color: var(--text-gray);">&lt; 200</td>
                        <td><span class="badge-normal">Normal</span></td>
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
