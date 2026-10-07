<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreMedico = (String) request.getAttribute("nombreMedico");
    if (nombreMedico == null && usuario != null) {
        nombreMedico = "Dr. " + usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreMedico == null) nombreMedico = "Dr. Carlos Mendoza";

    String inicialesMedico = "CM";
    if (nombreMedico != null && !nombreMedico.trim().isEmpty()) {
        String[] partes = nombreMedico.replace("Dr.", "").replace("Dra.", "").trim().split("\\s+");
        if (partes.length >= 2) {
            inicialesMedico = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            inicialesMedico = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Portal del Médico - Clínica Internacional</title>
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

        /* ALERT BANNER */
        .alert-active-card {
            background: #eff6ff; border: 1px solid #bfdbfe; border-left: 4px solid var(--primary);
            border-radius: var(--radius-lg); padding: 18px 24px; margin-bottom: 24px;
            display: flex; justify-content: space-between; align-items: center; box-shadow: 0 4px 12px rgba(37,99,235,0.06);
        }
        .alert-info-title { font-size: 14px; font-weight: 700; color: #1e40af; margin-bottom: 2px; display: flex; align-items: center; gap: 8px; }
        .alert-info-sub { font-size: 13px; color: #3b82f6; font-weight: 500; }
        .btn-call-patient {
            background: var(--primary); color: #ffffff; border: none; padding: 10px 20px; border-radius: 20px;
            font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; display: inline-flex; align-items: center; gap: 8px;
            box-shadow: 0 4px 12px rgba(37,99,235,0.2); text-decoration: none;
        }
        .btn-call-patient:hover { background: var(--primary-hover); transform: translateY(-1px); }

        /* STATS GRID */
        .stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-bottom: 24px; }
        .stat-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 22px; box-shadow: 0 2px 10px rgba(0,0,0,0.02); display: flex; flex-direction: column; justify-content: space-between;
        }
        .stat-card-head { display: flex; justify-content: space-between; align-items: center; color: var(--text-gray); font-size: 13px; font-weight: 600; }
        .stat-card-icon { width: 40px; height: 40px; border-radius: 10px; background: var(--primary-light); color: var(--primary); display: flex; align-items: center; justify-content: center; }
        .stat-card-num { font-size: 26px; font-weight: 700; color: var(--text-dark); margin: 12px 0 6px 0; letter-spacing: -0.5px; }
        .stat-card-foot { font-size: 12px; color: var(--text-gray); font-weight: 500; }

        /* ACTION BAR */
        .action-bar {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 18px 24px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.02);
        }
        .action-bar-title { font-size: 14px; font-weight: 700; color: var(--text-dark); }
        .action-btns { display: flex; gap: 12px; }
        .btn-action-pill {
            padding: 10px 20px; border-radius: 20px; font-size: 13px; font-weight: 600; text-decoration: none;
            transition: all 0.2s; display: inline-flex; align-items: center; gap: 8px; border: 1px solid var(--border-light);
        }
        .btn-action-primary { background: var(--primary); color: #ffffff; border-color: var(--primary); box-shadow: 0 4px 12px rgba(37,99,235,0.2); }
        .btn-action-primary:hover { background: var(--primary-hover); }
        .btn-action-sec { background: #ffffff; color: var(--text-dark); }
        .btn-action-sec:hover { background: #f8fafc; }

        /* TABLE CARD */
        .table-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.02);
        }
        .table-header {
            padding: 20px 24px; border-bottom: 1px solid var(--border-light); display: flex; justify-content: space-between; align-items: center;
        }
        .table-title { font-size: 16px; font-weight: 700; color: var(--text-dark); }
        .table-tag { font-size: 12px; background: #e0f2fe; color: #0284c7; padding: 4px 10px; border-radius: 20px; font-weight: 600; }

        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { background: #f8fafc; padding: 14px 20px; font-size: 12px; font-weight: 600; color: var(--text-gray); border-bottom: 1px solid var(--border-light); }
        td { padding: 16px 20px; font-size: 14px; color: var(--text-dark); border-bottom: 1px solid var(--border-light); vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover { background: #f8fafc; }

        .badge-status { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; display: inline-block; }
        .badge-atendido { background: #f1f5f9; color: #475569; }
        .badge-espera { background: #dcfce7; color: #166534; }
        .badge-pendiente { background: #fef3c7; color: #92400e; }

        .btn-tbl {
            padding: 8px 16px; border-radius: 20px; font-size: 12px; font-weight: 600; text-decoration: none;
            transition: all 0.2s; display: inline-flex; align-items: center; gap: 6px;
        }
        .btn-tbl-primary { background: var(--primary-light); color: var(--primary); }
        .btn-tbl-primary:hover { background: var(--primary); color: #ffffff; }
        .btn-tbl-sec { background: #f1f5f9; color: var(--text-dark); }
        .btn-tbl-sec:hover { background: #e2e8f0; }

        @media(max-width:900px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } .stats-grid { grid-template-columns: 1fr; } }
    </style>
</head>
<body>

    <aside class="sidebar">
        <div class="brand-container">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>
        <nav class="nav-menu">
            <a href="${pageContext.request.contextPath}/medico/inicio" class="nav-item active">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> Inicio
            </a>
            <a href="${pageContext.request.contextPath}/medico/agenda" class="nav-item">
                <i data-lucide="calendar" style="width: 18px;"></i> Mi Agenda
            </a>
            <a href="${pageContext.request.contextPath}/medico/atencion" class="nav-item">
                <i data-lucide="stethoscope" style="width: 18px;"></i> Atención Paciente
            </a>
            <a href="${pageContext.request.contextPath}/medico/perfil" class="nav-item">
                <i data-lucide="user" style="width: 18px;"></i> Mi Perfil
            </a>
        </nav>
        <a href="${pageContext.request.contextPath}/logout" class="user-profile-bottom">
            <div class="avatar-initials"><%= inicialesMedico %></div>
            <div style="flex: 1;">
                <div class="user-name-sm"><%= nombreMedico %></div>
                <div class="user-role-sm">Médico Especialista</div>
            </div>
            <i data-lucide="log-out" style="width: 16px; color: var(--text-gray);"></i>
        </a>
    </aside>

    <main class="main-content">
        <div class="header-row">
            <div>
                <h1 class="header-title">Dashboard del Médico</h1>
                <p class="header-subtitle">Supervisión en tiempo real de consultas, cola de espera y gestión asistencial.</p>
            </div>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-bell" title="Alertas">
                <i data-lucide="bell" style="width: 20px;"></i>
            </a>
        </div>

        <!-- ACTIVE ALERT CARD -->
        <div class="alert-active-card">
            <div>
                <div class="alert-info-title">
                    <i data-lucide="bell-ring" style="width: 18px;"></i> 1 Paciente en Sala de Espera
                </div>
                <div class="alert-info-sub">
                    Turno Activo: 09:30 AM &nbsp;•&nbsp; Tiempo de espera estimado: 08 min &nbsp;•&nbsp; Sede Central — Consultorio 302
                </div>
            </div>
            <a href="${pageContext.request.contextPath}/medico/atencion?idCita=2" class="btn-call-patient">
                <i data-lucide="volume-2" style="width: 16px;"></i> Llamar a Consultorio
            </a>
        </div>

        <!-- STATS GRID -->
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-card-head">
                    <span>Citas Programadas Hoy</span>
                    <div class="stat-card-icon"><i data-lucide="calendar" style="width: 20px;"></i></div>
                </div>
                <div class="stat-card-num">8 Citas</div>
                <div class="stat-card-foot">2 Atendidos &nbsp;•&nbsp; 1 En Espera &nbsp;•&nbsp; 5 Pendientes</div>
            </div>

            <div class="stat-card">
                <div class="stat-card-head">
                    <span>Pacientes Pendientes</span>
                    <div class="stat-card-icon"><i data-lucide="users" style="width: 20px;"></i></div>
                </div>
                <div class="stat-card-num">6 Restantes</div>
                <div class="stat-card-foot">Turno Mañana: 4 &nbsp;•&nbsp; Turno Tarde: 2</div>
            </div>

            <div class="stat-card">
                <div class="stat-card-head">
                    <span>Disponibilidad Próx. Semana</span>
                    <div class="stat-card-icon"><i data-lucide="clock" style="width: 20px;"></i></div>
                </div>
                <div class="stat-card-num" style="font-size: 20px; color: #0284c7;">En Revisión Adm.</div>
                <div class="stat-card-foot">Enviada: 22 Oct 14:30 &nbsp;•&nbsp; Respuesta Pendiente</div>
            </div>
        </div>

        <!-- ACTION BAR -->
        <div class="action-bar">
            <div class="action-bar-title">Acciones Operativas Directas</div>
            <div class="action-btns">
                <a href="${pageContext.request.contextPath}/medico/agenda" class="btn-action-pill btn-action-sec">
                    <i data-lucide="calendar" style="width: 16px;"></i> Ver Agenda del Día
                </a>
                <a href="${pageContext.request.contextPath}/medico/atencion" class="btn-action-pill btn-action-primary">
                    <i data-lucide="stethoscope" style="width: 16px;"></i> Gestionar Atención Actual
                </a>
            </div>
        </div>

        <!-- TABLE CARD -->
        <div class="table-card">
            <div class="table-header">
                <div class="table-title">Pacientes del Turno Mañana (Sede Central)</div>
                <span class="table-tag">4 Registrados</span>
            </div>

            <table>
                <thead>
                    <tr>
                        <th>HORA</th>
                        <th>PACIENTE</th>
                        <th>DOCUMENTO</th>
                        <th>MOTIVO DE CONSULTA</th>
                        <th>ESTADO DE ASISTENCIA</th>
                        <th style="text-align: right;">ACCIÓN</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td style="font-weight: 700;">09:00 AM</td>
                        <td style="font-weight: 600;">Juan Alberto Pérez Gómez</td>
                        <td style="color: var(--text-gray);">DNI 72345678</td>
                        <td>Chequeo preventivo / Dolor torácico</td>
                        <td><span class="badge-status badge-atendido">Atendido</span></td>
                        <td style="text-align: right;">
                            <a href="${pageContext.request.contextPath}/medico/atencion?idCita=1" class="btn-tbl btn-tbl-sec">Ver Ficha</a>
                        </td>
                    </tr>
                    <tr style="background: #eff6ff;">
                        <td style="font-weight: 700; color: var(--primary);">09:30 AM</td>
                        <td style="font-weight: 700; color: var(--primary);">María Elena Ramos</td>
                        <td style="color: var(--text-gray);">DNI 44102931</td>
                        <td>Evaluación postoperatoria</td>
                        <td><span class="badge-status badge-espera">• En Espera / Presente</span></td>
                        <td style="text-align: right;">
                            <a href="${pageContext.request.contextPath}/medico/atencion?idCita=2" class="btn-tbl btn-tbl-primary">Atender Ahora</a>
                        </td>
                    </tr>
                    <tr>
                        <td style="font-weight: 700;">10:00 AM</td>
                        <td style="font-weight: 600;">Carlos Fernando Soto</td>
                        <td style="color: var(--text-gray);">DNI 10928374</td>
                        <td>Control de arritmia</td>
                        <td><span class="badge-status badge-pendiente">Pendiente de Llegada</span></td>
                        <td style="text-align: right;">
                            <a href="${pageContext.request.contextPath}/medico/atencion?idCita=3" class="btn-tbl btn-tbl-sec">Abrir</a>
                        </td>
                    </tr>
                    <tr>
                        <td style="font-weight: 700;">10:30 AM</td>
                        <td style="font-weight: 600;">Lucía Alejandra Vega</td>
                        <td style="color: var(--text-gray);">DNI 55829104</td>
                        <td>Lectura de holter</td>
                        <td><span class="badge-status badge-pendiente">Pendiente de Llegada</span></td>
                        <td style="text-align: right;">
                            <a href="${pageContext.request.contextPath}/medico/atencion?idCita=4" class="btn-tbl btn-tbl-sec">Abrir</a>
                        </td>
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
