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

    List<Map<String, Object>> agenda = (List<Map<String, Object>>) request.getAttribute("agenda");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Portal del Médico - Inicio | Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        :root {
            --primary: #0f172a;
            --primary-accent: #2563eb;
            --bg-page: #f8fafc;
            --bg-card: #ffffff;
            --border-dark: #0f172a;
            --border-light: #cbd5e1;
            --text-dark: #0f172a;
            --text-muted: #64748b;
            --radius-md: 8px;
        }

        * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Inter', sans-serif; }
        body { background-color: var(--bg-page); color: var(--text-dark); display: flex; flex-direction: column; min-height: 100vh; }

        /* HEADER SUPERIOR WIREFRAME STYLE */
        .top-header {
            background: #ffffff; border-bottom: 2px solid var(--border-dark); padding: 12px 24px;
            display: flex; justify-content: space-between; align-items: center; font-size: 13px; font-weight: 700;
        }
        .top-header-left { display: flex; align-items: center; gap: 10px; text-transform: uppercase; letter-spacing: 0.5px; }
        .top-header-right { display: flex; align-items: center; gap: 16px; }
        .user-tag { background: #f1f5f9; padding: 6px 14px; border: 1px solid var(--border-dark); border-radius: 4px; font-size: 12px; }
        .btn-logout {
            background: #ffffff; border: 1px solid var(--border-dark); color: var(--text-dark); padding: 6px 14px;
            border-radius: 4px; text-decoration: none; font-weight: 700; display: flex; align-items: center; gap: 6px;
            transition: all 0.2s; font-size: 12px;
        }
        .btn-logout:hover { background: #fee2e2; border-color: #ef4444; color: #b91c1c; }

        /* BODY LAYOUT */
        .app-container { display: flex; flex: 1; }

        /* LEFT SIDEBAR NAVIGATION WIREFRAME */
        .sidebar {
            width: 240px; background: #ffffff; border-right: 2px solid var(--border-dark); padding: 16px;
            display: flex; flex-direction: column; gap: 8px; flex-shrink: 0;
        }
        .sidebar-item {
            padding: 12px 14px; border: 1px solid var(--border-dark); border-radius: 4px; color: var(--text-dark);
            text-decoration: none; font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;
            transition: all 0.2s; display: flex; align-items: center; justify-content: space-between;
        }
        .sidebar-item:hover { background: #f1f5f9; }
        .sidebar-item.active { background: var(--border-dark); color: #ffffff; }

        /* MAIN CONTENT CONTAINER */
        .main-content { flex: 1; padding: 24px 32px; overflow-y: auto; }

        /* ACTIVE ALERT BANNER */
        .alert-active-banner {
            background: #f1f5f9; border: 2px solid var(--border-dark); border-radius: 6px; padding: 12px 18px;
            display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; font-size: 13px;
        }
        .alert-tag { background: #0f172a; color: #ffffff; padding: 4px 10px; font-weight: 800; font-size: 11px; border-radius: 3px; }
        .alert-text { font-weight: 700; letter-spacing: 0.3px; }
        .btn-call {
            background: #0f172a; color: #ffffff; border: 1px solid var(--border-dark); padding: 8px 16px;
            font-size: 12px; font-weight: 800; border-radius: 4px; cursor: pointer; text-transform: uppercase; transition: all 0.2s;
        }
        .btn-call:hover { background: #2563eb; border-color: #2563eb; }

        /* DASHBOARD SECTION HEADER */
        .dash-header-box {
            background: #ffffff; border: 2px solid var(--border-dark); padding: 20px 24px; border-radius: 6px;
            margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center;
        }
        .dash-title { font-size: 22px; font-weight: 800; text-transform: uppercase; letter-spacing: -0.5px; }
        .dash-sub { font-size: 13px; color: var(--text-muted); margin-top: 4px; }
        .dash-meta { text-align: right; font-size: 12px; font-weight: 700; color: var(--text-dark); }
        .dash-meta-badge { background: #f1f5f9; padding: 4px 8px; border: 1px solid var(--border-light); border-radius: 4px; margin-top: 4px; display: inline-block; }

        /* STAT CARDS GRID */
        .stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 20px; }
        .stat-card {
            background: #ffffff; border: 2px solid var(--border-dark); border-radius: 6px; padding: 18px;
            display: flex; flex-direction: column; justify-content: space-between;
        }
        .stat-card-head { font-size: 12px; font-weight: 800; text-transform: uppercase; color: var(--text-muted); display: flex; justify-content: space-between; }
        .stat-card-num { font-size: 26px; font-weight: 800; margin: 10px 0 6px 0; color: var(--text-dark); letter-spacing: -0.5px; }
        .stat-card-foot { font-size: 12px; font-weight: 600; color: var(--text-muted); background: #f8fafc; padding: 8px; border-radius: 4px; border: 1px solid #e2e8f0; }

        /* ACTION BAR */
        .action-bar {
            background: #ffffff; border: 2px solid var(--border-dark); border-radius: 6px; padding: 14px 20px;
            display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
        }
        .action-bar-label { font-size: 12px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.5px; color: var(--text-muted); }
        .action-btns { display: flex; gap: 12px; }
        .btn-act {
            padding: 10px 18px; border-radius: 4px; font-size: 13px; font-weight: 800; text-decoration: none;
            text-transform: uppercase; transition: all 0.2s; border: 2px solid var(--border-dark); display: inline-flex; align-items: center; gap: 8px;
        }
        .btn-act-primary { background: #0f172a; color: #ffffff; }
        .btn-act-primary:hover { background: #1e293b; }
        .btn-act-sec { background: #ffffff; color: #0f172a; }
        .btn-act-sec:hover { background: #f1f5f9; }

        /* TABLE SECTION WIREFRAME */
        .table-box { background: #ffffff; border: 2px solid var(--border-dark); border-radius: 6px; overflow: hidden; }
        .table-head-bar {
            background: #f1f5f9; border-bottom: 2px solid var(--border-dark); padding: 14px 20px;
            display: flex; justify-content: space-between; align-items: center; font-size: 13px; font-weight: 800; text-transform: uppercase;
        }
        .table-badge { background: #0f172a; color: #ffffff; font-size: 11px; padding: 3px 8px; border-radius: 3px; margin-left: 8px; }

        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { background: #f8fafc; border-bottom: 2px solid var(--border-dark); padding: 12px 16px; font-size: 12px; font-weight: 800; text-transform: uppercase; color: var(--text-dark); }
        td { border-bottom: 1px solid #e2e8f0; padding: 14px 16px; font-size: 13px; font-weight: 500; vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover { background: #f8fafc; }

        .time-badge { font-weight: 800; font-size: 13px; }
        .patient-name { font-weight: 700; color: var(--text-dark); }
        .doc-num { font-size: 12px; color: var(--text-muted); font-weight: 600; }

        .status-badge { display: inline-block; padding: 4px 10px; border-radius: 4px; font-size: 11px; font-weight: 800; text-transform: uppercase; border: 1px solid var(--border-dark); }
        .status-atendido { background: #e2e8f0; color: #475569; }
        .status-espera { background: #0f172a; color: #ffffff; }
        .status-pendiente { background: #ffffff; color: #64748b; }

        .btn-table-action {
            padding: 6px 14px; border-radius: 4px; font-size: 12px; font-weight: 800; border: 1px solid var(--border-dark);
            background: #ffffff; color: var(--text-dark); text-decoration: none; display: inline-block; transition: all 0.2s; text-transform: uppercase;
        }
        .btn-table-action.primary { background: #0f172a; color: #ffffff; }
        .btn-table-action.primary:hover { background: #2563eb; border-color: #2563eb; }
        .btn-table-action:hover { background: #f1f5f9; }

        .table-foot-bar {
            background: #f8fafc; border-top: 1px solid var(--border-light); padding: 10px 20px;
            display: flex; justify-content: space-between; font-size: 11px; color: var(--text-muted); font-weight: 700; text-transform: uppercase;
        }
    </style>
</head>
<body>

    <!-- TOP HEADER -->
    <header class="top-header">
        <div class="top-header-left">
            <i data-lucide="activity" style="width: 18px;"></i>
            PORTAL DEL MÉDICO — CLÍNICA INTERNACIONAL
        </div>
        <div class="top-header-right">
            <span class="user-tag"><%= nombreMedico %> | ROL: MÉDICO</span>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">
                <i data-lucide="log-out" style="width: 14px;"></i> CERRAR SESIÓN
            </a>
        </div>
    </header>

    <div class="app-container">
        <!-- SIDEBAR -->
        <aside class="sidebar">
            <a href="${pageContext.request.contextPath}/medico/inicio" class="sidebar-item active">
                0. INICIO <i data-lucide="chevron-right" style="width: 16px;"></i>
            </a>
            <a href="${pageContext.request.contextPath}/medico/agenda" class="sidebar-item">
                2. MI AGENDA <i data-lucide="chevron-right" style="width: 16px;"></i>
            </a>
            <a href="${pageContext.request.contextPath}/medico/atencion" class="sidebar-item">
                3. ATENCIÓN PACIENTE <i data-lucide="chevron-right" style="width: 16px;"></i>
            </a>
            <a href="${pageContext.request.contextPath}/medico/perfil" class="sidebar-item">
                4. PERFIL <i data-lucide="chevron-right" style="width: 16px;"></i>
            </a>
        </aside>

        <!-- MAIN CONTENT -->
        <main class="main-content">

            <!-- ACTIVE ALERT BANNER -->
            <div class="alert-active-banner">
                <div style="display: flex; align-items: center; gap: 12px;">
                    <span class="alert-tag">[ ! ] ALERTA ACTIVA</span>
                    <span class="alert-text">1 PACIENTE EN SALA DE ESPERA — TURNO ACTIVO: 09:30 AM (TIEMPO DE ESPERA: 08 MIN)</span>
                </div>
                <button class="btn-call" onclick="window.location.href='${pageContext.request.contextPath}/medico/atencion'">
                    LLAMAR A CONSULTORIO <i data-lucide="volume-2" style="width: 14px; display: inline; vertical-align: middle;"></i>
                </button>
            </div>

            <!-- DASHBOARD HEADER BOX -->
            <div class="dash-header-box">
                <div>
                    <h1 class="dash-title">0. INICIO — DASHBOARD DEL MÉDICO</h1>
                    <p class="dash-sub">Panel de supervisión en tiempo real de consultas, cola de espera y gestión asistencial.</p>
                </div>
                <div class="dash-meta">
                    <div>FECHA: 14 OCTUBRE 2026</div>
                    <span class="dash-meta-badge">SEDE: SEDE CENTRAL — CONS. 302</span>
                </div>
            </div>

            <!-- STATS CARDS GRID -->
            <div class="stats-grid">
                <div class="stat-card">
                    <div class="stat-card-head">
                        <span>CITAS PROGRAMADAS HOY</span>
                        <i data-lucide="calendar" style="width: 16px;"></i>
                    </div>
                    <div class="stat-card-num">TOTAL: 8 CITAS</div>
                    <div class="stat-card-foot">2 ATENDIDOS &nbsp;|&nbsp; 1 EN ESPERA &nbsp;|&nbsp; 5 PENDIENTES</div>
                </div>

                <div class="stat-card">
                    <div class="stat-card-head">
                        <span>PACIENTES PENDIENTES</span>
                        <i data-lucide="users" style="width: 16px;"></i>
                    </div>
                    <div class="stat-card-num">6 RESTANTES</div>
                    <div class="stat-card-foot">TURNO MAÑANA: 4 &nbsp;|&nbsp; TURNO TARDE: 2</div>
                </div>

                <div class="stat-card">
                    <div class="stat-card-head">
                        <span>DISPONIBILIDAD PRÓX. SEMANA</span>
                        <i data-lucide="clock" style="width: 16px;"></i>
                    </div>
                    <div class="stat-card-num" style="font-size: 18px; color: #2563eb;">PROPUESTA EN REVISIÓN</div>
                    <div class="stat-card-foot">ENVIADA: 22 OCT 14:30 &nbsp;|&nbsp; ESTADO: PENDIENTE ADM.</div>
                </div>
            </div>

            <!-- ACTION BAR -->
            <div class="action-bar">
                <span class="action-bar-label">BARRA DE ACCIONES OPERATIVAS DIRECTAS</span>
                <div class="action-btns">
                    <a href="${pageContext.request.contextPath}/medico/agenda" class="btn-act btn-act-sec">
                        IR A MI AGENDA DEL DÍA <i data-lucide="arrow-right" style="width: 16px;"></i>
                    </a>
                    <a href="${pageContext.request.contextPath}/medico/atencion" class="btn-act btn-act-primary">
                        GESTIONAR ATENCIÓN ACTUAL <i data-lucide="stethoscope" style="width: 16px;"></i>
                    </a>
                </div>
            </div>

            <!-- TABLE SECTION -->
            <div class="table-box">
                <div class="table-head-bar">
                    <div>PACIENTES DEL TURNO DE LA MAÑANA (SEDE CENTRAL) <span class="table-badge">4 REGISTRADOS</span></div>
                    <span style="font-size: 11px; color: var(--text-muted);">INTERVALO POR CITA: 30 MIN</span>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th>HORA</th>
                            <th>PACIENTE</th>
                            <th>N° DOCUMENTO</th>
                            <th>MOTIVO DE CONSULTA</th>
                            <th>ESTADO DE ASISTENCIA</th>
                            <th style="text-align: right;">ACCIÓN</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td class="time-badge">09:00 AM</td>
                            <td class="patient-name">Juan Alberto Pérez Gómez</td>
                            <td class="doc-num">DNI 72345678</td>
                            <td>Chequeo preventivo / Dolor torácico</td>
                            <td><span class="status-badge status-atendido">[ ATENDIDO ]</span></td>
                            <td style="text-align: right;">
                                <a href="${pageContext.request.contextPath}/medico/atencion?idCita=1" class="btn-table-action">[ VER FICHA ]</a>
                            </td>
                        </tr>
                        <tr>
                            <td class="time-badge" style="color: #2563eb;">09:30 AM</td>
                            <td class="patient-name" style="color: #2563eb;">María Elena Ramos</td>
                            <td class="doc-num">DNI 44102931</td>
                            <td>Evaluación postoperatoria</td>
                            <td><span class="status-badge status-espera">[ EN ESPERA / PRESENTE ]</span></td>
                            <td style="text-align: right;">
                                <a href="${pageContext.request.contextPath}/medico/atencion?idCita=2" class="btn-table-action primary">[ ATENDER AHORA ]</a>
                            </td>
                        </tr>
                        <tr>
                            <td class="time-badge">10:00 AM</td>
                            <td class="patient-name">Carlos Fernando Soto</td>
                            <td class="doc-num">DNI 10928374</td>
                            <td>Control de arritmia</td>
                            <td><span class="status-badge status-pendiente">[ PENDIENTE LLEGADA ]</span></td>
                            <td style="text-align: right;">
                                <a href="${pageContext.request.contextPath}/medico/atencion?idCita=3" class="btn-table-action">[ ABRIR ]</a>
                            </td>
                        </tr>
                        <tr>
                            <td class="time-badge">10:30 AM</td>
                            <td class="patient-name">Lucía Alejandra Vega</td>
                            <td class="doc-num">DNI 55829104</td>
                            <td>Lectura de holter</td>
                            <td><span class="status-badge status-pendiente">[ PENDIENTE LLEGADA ]</span></td>
                            <td style="text-align: right;">
                                <a href="${pageContext.request.contextPath}/medico/atencion?idCita=4" class="btn-table-action">[ ABRIR ]</a>
                            </td>
                        </tr>
                    </tbody>
                </table>

                <div class="table-foot-bar">
                    <span>REGISTROS 1 - 4 DE UN TOTAL DE 8 PROGRAMADOS PARA LA JORNADA</span>
                    <span>TOLERANCIA SALA: 15 MIN</span>
                </div>
            </div>

        </main>
    </div>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
