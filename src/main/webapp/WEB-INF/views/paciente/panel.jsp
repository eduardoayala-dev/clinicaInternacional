<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombrePaciente = (String) request.getAttribute("nombrePaciente");
    if (nombrePaciente == null && usuario != null) nombrePaciente = usuario.getNombres() + " " + usuario.getApellidos();
    if (nombrePaciente == null) nombrePaciente = "Paciente";

    List<Map<String, Object>> listaActividades = (List<Map<String, Object>>) request.getAttribute("listaActividades");

    String inicialesPaciente = "P";
    if (nombrePaciente != null && !nombrePaciente.trim().isEmpty()) {
        String[] partes = nombrePaciente.trim().split("\\s+");
        if (partes.length >= 2) {
            inicialesPaciente = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            inicialesPaciente = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }

    pe.edu.utp.clinica.dao.CitaDAO citaDAO_bell = new pe.edu.utp.clinica.dao.CitaDAO();
    boolean tieneNotifSinLeer = usuario != null && citaDAO_bell.contarNotificacionesSinLeer(usuario.getIdUsuario()) > 0;
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Panel Principal - Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
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

        .main-content { flex: 1; padding: 40px 50px; overflow-y: auto; }
        
        .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 28px; }
        .header-title { font-size: 28px; font-weight: 700; color: var(--text-dark); margin-bottom: 4px; letter-spacing: -0.5px; }
        .header-subtitle { font-size: 14px; color: var(--text-gray); }

        .btn-bell {
            width: 44px; height: 44px; border-radius: 12px; background: #ffffff; border: 1px solid var(--border-light);
            display: flex; align-items: center; justify-content: center; color: var(--text-gray); position: relative;
            text-decoration: none; transition: all 0.2s;
        }
        .btn-bell:hover { background: #f8fafc; color: var(--text-dark); }
        .btn-bell.has-unread::after {
            content: ''; position: absolute; top: 10px; right: 12px; width: 8px; height: 8px; background: #ef4444; border-radius: 50%; border: 2px solid #ffffff;
        }

        /* GRID LAYOUT PANEL */
        .panel-grid {
            display: grid; grid-template-columns: 300px 1fr 300px; gap: 20px; align-items: start;
        }

        /* CALENDARIO WIDGET */
        .card-box {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px; box-shadow: 0 4px 16px rgba(0,0,0,0.02); margin-bottom: 20px;
        }
        .calendar-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; font-weight: 700; font-size: 15px; }
        .cal-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; text-align: center; font-size: 12px; }
        .cal-day-head { font-weight: 700; color: var(--text-gray); padding-bottom: 8px; font-size: 11px; }
        .cal-day {
            padding: 8px 0; border-radius: 8px; color: var(--text-dark); font-weight: 500; cursor: pointer; transition: all 0.2s; position: relative;
        }
        .cal-day:hover { background: #f1f5f9; }
        .cal-day.active { background: var(--primary); color: #ffffff; font-weight: 700; }
        .cal-day.has-event::after {
            content: ''; position: absolute; bottom: 3px; left: 50%; transform: translateX(-50%); width: 4px; height: 4px; background: var(--primary); border-radius: 50%;
        }

        /* ACTIVIDADES */
        .box-title { font-size: 15px; font-weight: 700; color: var(--text-dark); margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
        .activity-card {
            padding: 14px 16px; border-radius: var(--radius-md); margin-bottom: 12px; border-left: 4px solid var(--primary);
        }
        .act-blue { background: #eff6ff; border-left-color: #2563eb; }
        .act-purple { background: #f3e8ff; border-left-color: #9333ea; }
        .act-green { background: #dcfce7; border-left-color: #16a34a; }
        
        .act-title { font-size: 14px; font-weight: 700; color: var(--text-dark); }
        .act-sub { font-size: 12px; color: var(--text-gray); margin-top: 2px; }

        /* BIENESTAR */
        .score-val { font-size: 32px; font-weight: 800; color: var(--primary); line-height: 1; }
        .progress-bar { height: 8px; background: #e2e8f0; border-radius: 10px; overflow: hidden; margin-top: 12px; }
        .progress-fill { height: 100%; width: 78%; background: var(--primary); border-radius: 10px; }

        /* MEDICAMENTOS Y TAREAS */
        .med-item { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
        .med-icon { width: 32px; height: 32px; border-radius: 50%; background: #fee2e2; color: #ef4444; display: flex; align-items: center; justify-content: center; font-size: 12px; flex-shrink: 0; }
        .med-name { font-size: 13px; font-weight: 700; color: var(--text-dark); }
        .med-dose { font-size: 11px; color: var(--text-gray); }

        .task-item {
            display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; border-radius: var(--radius-md);
            margin-bottom: 8px; font-size: 13px; font-weight: 600;
        }
        .task-high { background: #ffe4e6; color: #9f1239; }
        .task-mid { background: #fef3c7; color: #92400e; }
        .task-low { background: #e0f2fe; color: #0369a1; }
        .badge-pill { padding: 2px 8px; border-radius: 10px; font-size: 10px; font-weight: 700; background: #ffffff; }

        @media(max-width:1024px) { .panel-grid { grid-template-columns: 1fr; } body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } }
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
            <a href="${pageContext.request.contextPath}/paciente/panel" class="nav-item active">
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
            <a href="${pageContext.request.contextPath}/paciente/reportes" class="nav-item">
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
        <div class="page-header">
            <div>
                <h1 class="header-title">Panel Principal</h1>
                <p class="header-subtitle">Vista general de tu actividad médica y estado de salud.</p>
            </div>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-bell <%= tieneNotifSinLeer ? "has-unread" : "" %>" title="Alertas">
                <i data-lucide="bell" style="width: 20px;"></i>
            </a>
        </div>

        <div class="panel-grid">
            
            <!-- COLUMNA IZQUIERDA: CALENDARIO -->
            <div>
                <div class="card-box">
                    <div class="calendar-header">
                        <span>Octubre 2026</span>
                        <div style="display: flex; gap: 4px;">
                            <i data-lucide="chevron-left" style="width: 16px; cursor: pointer;"></i>
                            <i data-lucide="chevron-right" style="width: 16px; cursor: pointer;"></i>
                        </div>
                    </div>
                    <div class="cal-grid">
                        <div class="cal-day-head">L</div><div class="cal-day-head">M</div><div class="cal-day-head">X</div><div class="cal-day-head">J</div><div class="cal-day-head">V</div><div class="cal-day-head">S</div><div class="cal-day-head">D</div>
                        
                        <div class="cal-day" style="color: #cbd5e1;">28</div>
                        <div class="cal-day" style="color: #cbd5e1;">29</div>
                        <div class="cal-day" style="color: #cbd5e1;">30</div>
                        <div class="cal-day">1</div><div class="cal-day">2</div><div class="cal-day">3</div><div class="cal-day">4</div>
                        
                        <div class="cal-day">5</div><div class="cal-day">6</div>
                        <div class="cal-day">7</div>
                        <div class="cal-day">8</div><div class="cal-day">9</div><div class="cal-day">10</div><div class="cal-day">11</div>
                        
                        <div class="cal-day">12</div><div class="cal-day">13</div><div class="cal-day">14</div><div class="cal-day">15</div><div class="cal-day">16</div><div class="cal-day">17</div><div class="cal-day">18</div>
                        
                        <div class="cal-day">19</div><div class="cal-day">20</div><div class="cal-day">21</div><div class="cal-day">22</div><div class="cal-day">23</div><div class="cal-day">24</div><div class="cal-day">25</div>
                        
                        <div class="cal-day">26</div><div class="cal-day">27</div><div class="cal-day">28</div><div class="cal-day">29</div><div class="cal-day">30</div><div class="cal-day">31</div>
                    </div>
                </div>
            </div>

            <!-- COLUMNA CENTRO: ACTIVIDADES Y BIENESTAR -->
            <div>
                <div class="card-box">
                    <div class="box-title">
                        <span>Próximas actividades</span>
                        <a href="${pageContext.request.contextPath}/paciente/citas?action=nueva" style="font-size: 12px; color: var(--primary); text-decoration: none;">+ Agregar</a>
                    </div>

                    <% if (listaActividades != null && !listaActividades.isEmpty()) {
                        for (Map<String, Object> act : listaActividades) {
                            String actMed = (String) act.get("medico");
                            String actEsp = (String) act.get("especialidad");
                            String actFh = (String) act.get("fechaHora");
                    %>
                    <div class="activity-card act-blue">
                        <div class="act-title">Consulta — <%= actMed %></div>
                        <div class="act-sub"><%= actEsp %> · <%= actFh %></div>
                    </div>
                    <%  }
                    } else { %>
                    <div style="text-align: center; padding: 24px 12px; color: var(--text-gray); font-size: 13px;">
                        No tienes actividades próximas programadas.
                    </div>
                    <% } %>
                </div>

                <div class="card-box">
                    <div class="box-title">
                        <span>Índice de bienestar</span>
                        <span style="font-size: 11px; padding: 2px 8px; border-radius: 10px; background: #f1f5f9; color: #475569;">A la espera</span>
                    </div>
                    <div class="score-val">-- <span style="font-size: 16px; color: var(--text-gray); font-weight: 500;">/100</span></div>
                    <div class="progress-bar">
                        <div class="progress-fill" style="width: 0%;"></div>
                    </div>
                </div>
            </div>

            <!-- COLUMNA DERECHA: MEDICAMENTOS Y TAREAS -->
            <div>
                <div class="card-box">
                    <div class="box-title">Medicamentos activos</div>
                    <div style="text-align: center; padding: 20px 12px; color: var(--text-gray); font-size: 13px;">
                        No tienes medicamentos activos registrados.
                    </div>
                </div>

                <div class="card-box">
                    <div class="box-title">Tareas pendientes</div>
                    <div style="text-align: center; padding: 20px 12px; color: var(--text-gray); font-size: 13px;">
                        No tienes tareas pendientes por realizar.
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
