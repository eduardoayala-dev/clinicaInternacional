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

    String iniciales = "MS";
    if (nombrePersonal != null && !nombrePersonal.trim().isEmpty()) {
        String[] partes = nombrePersonal.trim().split("\\s+");
        if (partes.length >= 2) {
            iniciales = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            iniciales = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }

    List<Map<String, Object>> listaCitas = (List<Map<String, Object>>) request.getAttribute("listaCitas");
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
    <title>Tablero de Control Operativo - Clínica Internacional</title>
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
        .flash-alert {
            background: #dcfce7; color: #15803d; border: 1px solid #bbf7d0; padding: 14px 18px;
            border-radius: var(--radius-md); font-weight: 600; margin-bottom: 24px; font-size: 14px; display: flex; align-items: center; gap: 10px;
        }

        /* STATS GRID */
        .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
        .stat-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.02); display: flex; flex-direction: column; justify-content: space-between;
        }
        .stat-card-title { font-size: 12px; font-weight: 600; color: var(--text-gray); text-transform: uppercase; letter-spacing: 0.5px; }
        .stat-card-num { font-size: 28px; font-weight: 700; color: var(--text-dark); margin: 10px 0 4px 0; letter-spacing: -0.5px; }
        .stat-card-sub { font-size: 12px; color: var(--text-gray); font-weight: 500; }

        /* MAIN GRID (TABLE + LIVE FEED) */
        .grid-2col { display: grid; grid-template-columns: 1fr 300px; gap: 24px; margin-bottom: 24px; }

        /* TABLE CARD */
        .card-box {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.02);
        }
        .card-box-header {
            padding: 20px 24px; border-bottom: 1px solid var(--border-light); display: flex; justify-content: space-between; align-items: center;
        }
        .card-box-title { font-size: 16px; font-weight: 700; color: var(--text-dark); display: flex; align-items: center; gap: 8px; }

        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { background: #f8fafc; padding: 14px 18px; font-size: 12px; font-weight: 600; color: var(--text-gray); border-bottom: 1px solid var(--border-light); }
        td { padding: 14px 18px; font-size: 13px; color: var(--text-dark); border-bottom: 1px solid var(--border-light); vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover { background: #f8fafc; }

        .badge-status { padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; display: inline-block; }
        .badge-espera { background: #dcfce7; color: #166534; }
        .badge-admision { background: #eff6ff; color: #2563eb; }
        .badge-pendiente { background: #fef3c7; color: #92400e; }
        .badge-confirmada { background: #f1f5f9; color: #475569; }

        .btn-tbl-action {
            padding: 6px 14px; border-radius: 20px; font-size: 12px; font-weight: 600; text-decoration: none;
            transition: all 0.2s; display: inline-flex; align-items: center; gap: 6px; border: none; cursor: pointer;
        }
        .btn-tbl-primary { background: var(--primary); color: #ffffff; box-shadow: 0 2px 6px rgba(37,99,235,0.2); }
        .btn-tbl-primary:hover { background: var(--primary-hover); }
        .btn-tbl-sec { background: var(--primary-light); color: var(--primary); }
        .btn-tbl-sec:hover { background: var(--primary); color: #ffffff; }

        /* LIVE LOG FEED CARD */
        .live-feed-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.02); display: flex; flex-direction: column;
        }
        .live-feed-head { font-size: 14px; font-weight: 700; color: var(--text-dark); margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
        .live-tag { font-size: 10px; background: #ef4444; color: #ffffff; padding: 2px 8px; border-radius: 10px; font-weight: 700; text-transform: uppercase; }

        .feed-item { padding: 12px 0; border-bottom: 1px solid var(--border-light); font-size: 12px; }
        .feed-item:last-child { border-bottom: none; }
        .feed-time { font-weight: 700; color: var(--primary); margin-bottom: 2px; }
        .feed-desc { color: var(--text-gray); line-height: 1.4; }

        /* QUICK ACTIONS BAR */
        .quick-actions-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px 24px; box-shadow: 0 2px 10px rgba(0,0,0,0.02); display: flex; justify-content: space-between; align-items: center;
        }
        .quick-actions-title { font-size: 14px; font-weight: 700; color: var(--text-dark); display: flex; align-items: center; gap: 8px; }
        .btn-quick {
            padding: 12px 24px; border-radius: 30px; font-size: 13px; font-weight: 600; text-decoration: none;
            transition: all 0.2s; display: inline-flex; align-items: center; gap: 8px; border: 1px solid var(--border-light); cursor: pointer;
        }
        .btn-quick-primary { background: var(--primary); color: #ffffff; border-color: var(--primary); box-shadow: 0 4px 12px rgba(37,99,235,0.2); }
        .btn-quick-primary:hover { background: var(--primary-hover); }
        .btn-quick-sec { background: #ffffff; color: var(--text-dark); }
        .btn-quick-sec:hover { background: #f8fafc; }

        @media(max-width:1024px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } .stats-grid { grid-template-columns: repeat(2, 1fr); } .grid-2col { grid-template-columns: 1fr; } }
    </style>
</head>
<body>

    <aside class="sidebar">
        <div class="brand-container">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>
        <nav class="nav-menu">
            <a href="${pageContext.request.contextPath}/admin/dashboard" class="nav-item active">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> Inicio / Tablero
            </a>
            <a href="${pageContext.request.contextPath}/admin/medicos" class="nav-item">
                <i data-lucide="stethoscope" style="width: 18px;"></i> Padrón Médicos
            </a>
            <a href="${pageContext.request.contextPath}/admin/usuarios" class="nav-item">
                <i data-lucide="users" style="width: 18px;"></i> Gestión Usuarios
            </a>
            <a href="${pageContext.request.contextPath}/admin/reasignaciones" class="nav-item">
                <i data-lucide="refresh-cw" style="width: 18px;"></i> Reasignaciones
            </a>
        </nav>
        <a href="${pageContext.request.contextPath}/logout" class="user-profile-bottom">
            <div class="avatar-initials"><%= iniciales %></div>
            <div style="flex: 1;">
                <div class="user-name-sm"><%= nombrePersonal %></div>
                <div class="user-role-sm">Personal Administrativo</div>
            </div>
            <i data-lucide="log-out" style="width: 16px; color: var(--text-gray);"></i>
        </a>
    </aside>

    <main class="main-content">
        <div class="header-row">
            <div>
                <h1 class="header-title">Tablero de Control Operativo Diario</h1>
                <p class="header-subtitle">Supervisión inmediata de citas, disponibilidad y reasignaciones del motor inteligente.</p>
            </div>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-bell" title="Alertas">
                <i data-lucide="bell" style="width: 20px;"></i>
            </a>
        </div>

        <% if (flashMessage != null) { %>
            <div class="flash-alert">
                <i data-lucide="check-circle" style="width: 20px;"></i> <%= flashMessage %>
            </div>
        <% } %>

        <!-- STATS GRID -->
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-card-title">Citas del Día</div>
                <div class="stat-card-num">420</div>
                <div class="stat-card-sub">310 Confirmadas &nbsp;•&nbsp; 65 En Espera</div>
            </div>

            <div class="stat-card">
                <div class="stat-card-title">Cancelaciones</div>
                <div class="stat-card-num">38</div>
                <div class="stat-card-sub">28 Notificadas &nbsp;•&nbsp; 10 Inmediatas</div>
            </div>

            <div class="stat-card">
                <div class="stat-card-title">Cupos Disponibles Red</div>
                <div class="stat-card-num">74</div>
                <div class="stat-card-sub">52 Regulares &nbsp;•&nbsp; 22 En Oferta</div>
            </div>

            <div class="stat-card">
                <div class="stat-card-title">Reasignaciones Pendientes</div>
                <div class="stat-card-num" style="color: var(--primary);">12</div>
                <div class="stat-card-sub">8 En Ventana 15m &nbsp;•&nbsp; 4 Manuales</div>
            </div>
        </div>

        <!-- MAIN 2COL GRID -->
        <div class="grid-2col">
            <!-- LEFT TABLE CARD -->
            <div class="card-box">
                <div class="card-box-header">
                    <div class="card-box-title">
                        <i data-lucide="clock" style="width: 18px; color: var(--primary);"></i> Citas de Atención Prioritaria en Ventanilla
                    </div>
                    <div style="display: flex; gap: 8px;">
                        <input type="text" placeholder="Buscar DNI o Paciente..." style="padding: 8px 14px; border-radius: 20px; border: 1px solid var(--border-light); font-size: 12px; outline: none;">
                    </div>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th>HORA</th>
                            <th>PACIENTE</th>
                            <th>SEDE / CONSULTORIO</th>
                            <th>MÉDICO</th>
                            <th>ESTADO</th>
                            <th style="text-align: right;">ACCIÓN</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td style="font-weight: 700;">08:30 AM</td>
                            <td style="font-weight: 600;">Jorge Valdivia N. <br><span style="font-size: 11px; color: var(--text-gray);">DNI 18492837</span></td>
                            <td>Sede Central (Cons. 302)</td>
                            <td>Dr. Carlos Mendoza</td>
                            <td><span class="badge-status badge-espera">En Espera</span></td>
                            <td style="text-align: right;">
                                <button class="btn-tbl-action btn-tbl-primary" onclick="alert('Paciente llamado a consultorio')">Llamar</button>
                            </td>
                        </tr>
                        <tr>
                            <td style="font-weight: 700;">08:45 AM</td>
                            <td style="font-weight: 600;">Sofía Rivas C. <br><span style="font-size: 11px; color: var(--text-gray);">DNI 72910482</span></td>
                            <td>Sede Central (Cons. 104)</td>
                            <td>Dra. Elena Rostova</td>
                            <td><span class="badge-status badge-admision">Admisión</span></td>
                            <td style="text-align: right;">
                                <button class="btn-tbl-action btn-tbl-sec" onclick="alert('Asistencia confirmada')">Confirmar</button>
                            </td>
                        </tr>
                        <tr>
                            <td style="font-weight: 700;">09:00 AM</td>
                            <td style="font-weight: 600;">Juan Carlos Peña <br><span style="font-size: 11px; color: var(--text-gray);">DNI 09482711</span></td>
                            <td>Sede Norte (Cons. 210)</td>
                            <td>Dr. Fernando Solís</td>
                            <td><span class="badge-status badge-pendiente">Pendiente</span></td>
                            <td style="text-align: right;">
                                <button class="btn-tbl-action btn-tbl-sec" onclick="alert('Registrando admisión')">Admitir</button>
                            </td>
                        </tr>
                        <tr>
                            <td style="font-weight: 700;">09:15 AM</td>
                            <td style="font-weight: 600;">Rosa Martínez P. <br><span style="font-size: 11px; color: var(--text-gray);">DNI 41928301</span></td>
                            <td>Sede Sur (Cons. 012)</td>
                            <td>Dr. Roberto Salas</td>
                            <td><span class="badge-status badge-confirmada">Confirmada</span></td>
                            <td style="text-align: right;">
                                <button class="btn-tbl-action btn-tbl-sec" onclick="alert('Abriendo ficha del paciente')">Ver Ficha</button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- RIGHT LIVE FEED -->
            <div class="live-feed-card">
                <div class="live-feed-head">
                    <span>Operaciones Registradas</span>
                    <span class="live-tag">• Live</span>
                </div>

                <div class="feed-item">
                    <div class="feed-time">08:24 AM</div>
                    <div class="feed-desc">Admisión manual registrada para paciente P-8030 en Sede Central.</div>
                </div>

                <div class="feed-item">
                    <div class="feed-time">08:19 AM</div>
                    <div class="feed-desc">Notificación enviada por liberación de cupo en Medicina General (Dr. Silva).</div>
                </div>

                <div class="feed-item">
                    <div class="feed-time">08:12 AM</div>
                    <div class="feed-desc">Paciente llamado a consultorio 302 (Paciente P-1092).</div>
                </div>

                <div class="feed-item">
                    <div class="feed-time">08:05 AM</div>
                    <div class="feed-desc">Apertura de turno de mañana en Sede Central completada sin observaciones.</div>
                </div>
            </div>
        </div>

        <!-- QUICK ACTIONS BAR -->
        <div class="quick-actions-card">
            <div class="quick-actions-title">
                <i data-lucide="zap" style="width: 18px; color: var(--primary);"></i> Acciones Rápida Operativas
            </div>
            <div style="display: flex; gap: 12px;">
                <a href="${pageContext.request.contextPath}/admin/reasignaciones" class="btn-quick btn-quick-primary">
                    <i data-lucide="plus-circle" style="width: 16px;"></i> Registrar Cita Manual en Ventanilla
                </a>
                <a href="${pageContext.request.contextPath}/admin/usuarios" class="btn-quick btn-quick-sec">
                    <i data-lucide="user-plus" style="width: 16px;"></i> Registrar Nuevo Paciente en Padrón
                </a>
            </div>
        </div>
    </main>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
