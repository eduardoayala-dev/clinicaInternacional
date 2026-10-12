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
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        :root {
            --primary: #2563eb;
            --primary-light: #eff6ff;
            --bg-body: #f8fafc;
            --bg-surface: #ffffff;
            --text-main: #0f172a;
            --text-muted: #64748b;
            --border-light: #e2e8f0;
            --shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05);
            --radius-md: 8px;
            --radius-lg: 12px;
        }

        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Inter', sans-serif; }
        body { display: flex; flex-direction: column; height: 100vh; background-color: var(--bg-body); overflow: hidden; color: var(--text-main); }

        .topbar {
            height: 64px; flex-shrink: 0; background-color: var(--bg-surface);
            border-bottom: 1px solid var(--border-light); display: flex;
            align-items: center; justify-content: space-between; padding: 0 32px; z-index: 10;
        }
        .topbar-brand { display: flex; align-items: center; gap: 12px; }
        .topbar-brand img { height: 36px; }
        .topbar-right { display: flex; align-items: center; gap: 16px; }
        .user-info { display: flex; flex-direction: column; align-items: flex-end; }
        .user-name { font-weight: 700; font-size: 13px; color: var(--text-main); }
        .user-role { font-size: 11px; color: var(--primary); font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
        .user-avatar { width: 36px; height: 36px; border-radius: 50%; background-color: var(--primary-light); color: var(--primary); display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px; }
        .btn-logout { display: flex; align-items: center; gap: 6px; padding: 8px 12px; border-radius: var(--radius-md); font-size: 13px; font-weight: 500; color: var(--text-muted); text-decoration: none; transition: all 0.2s; }
        .btn-logout:hover { color: #ef4444; background: #fef2f2; }

        .app-body { display: flex; height: calc(100vh - 64px); overflow: hidden; }

        .sidebar {
            width: 260px; background-color: var(--bg-surface); border-right: 1px solid var(--border-light);
            padding: 24px 16px; display: flex; flex-direction: column; gap: 6px; flex-shrink: 0; overflow-y: auto;
        }
        .nav-item {
            padding: 12px 16px; display: flex; align-items: center; gap: 12px;
            color: var(--text-muted); text-decoration: none; font-size: 14px; font-weight: 500;
            border-radius: var(--radius-md); transition: all 0.2s ease;
        }
        .nav-item:hover { background-color: var(--bg-body); color: var(--text-main); }
        .nav-item.active { background-color: var(--primary-light); color: var(--primary); font-weight: 600; }

        .main-content { flex: 1; height: 100%; overflow-y: auto; padding: 32px 40px; }
        .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
        .page-title { font-size: 24px; font-weight: 700; color: var(--text-main); margin-bottom: 6px; letter-spacing: -0.5px; }
        .page-subtitle { font-size: 14px; color: var(--text-muted); }

        .header-actions { display: flex; gap: 12px; }
        .btn-header {
            padding: 9px 16px; border-radius: var(--radius-md); font-size: 13px; font-weight: 600;
            border: 1px solid var(--border-light); background: #ffffff; color: var(--text-main); cursor: pointer;
            display: inline-flex; align-items: center; gap: 8px; text-decoration: none; transition: all 0.2s;
        }
        .btn-header:hover { background: var(--bg-body); color: var(--primary); }

        .flash-alert {
            background: #dcfce7; color: #166534; border: 1px solid #bbf7d0; padding: 14px 18px;
            border-radius: var(--radius-md); font-weight: 600; margin-bottom: 24px; font-size: 14px; display: flex; align-items: center; gap: 10px;
        }

        .metrics-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 24px; }
        .metric-card {
            background: var(--bg-surface); padding: 20px; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            box-shadow: var(--shadow-sm); display: flex; flex-direction: column; justify-content: space-between;
        }
        .metric-title { font-size: 11px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; display: flex; justify-content: space-between; align-items: center; }
        .metric-value { font-size: 28px; font-weight: 700; color: var(--text-main); margin: 10px 0 4px 0; }
        .metric-sub { font-size: 12px; color: var(--text-muted); }

        .grid-2col { display: grid; grid-template-columns: 1fr 340px; gap: 24px; margin-bottom: 24px; }
        .panel { background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light); padding: 24px; box-shadow: var(--shadow-sm); }
        .panel-title { font-size: 16px; font-weight: 700; color: var(--text-main); margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }

        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { padding: 12px 14px; font-size: 11px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; border-bottom: 1px solid var(--border-light); }
        td { padding: 14px; font-size: 13px; color: var(--text-main); border-bottom: 1px solid var(--border-light); vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover { background: #f8fafc; }

        .badge-status { padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; display: inline-block; }
        .badge-espera { background: #dcfce7; color: #166534; }
        .badge-admision { background: #eff6ff; color: #2563eb; }
        .badge-pendiente { background: #fef3c7; color: #92400e; }

        .btn-action-sm {
            padding: 6px 14px; border-radius: 20px; font-size: 12px; font-weight: 600; text-decoration: none;
            transition: all 0.2s; display: inline-flex; align-items: center; gap: 6px; border: none; cursor: pointer;
            background: var(--primary); color: #ffffff;
        }
        .btn-action-sm:hover { background: #1d4ed8; }

        .feed-list { display: flex; flex-direction: column; gap: 14px; }
        .feed-item { font-size: 13px; border-bottom: 1px solid var(--border-light); padding-bottom: 10px; }
        .feed-item:last-child { border-bottom: none; padding-bottom: 0; }
        .feed-time { font-weight: 700; color: var(--primary); font-size: 12px; margin-bottom: 2px; }

        .quick-actions-bar {
            background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px 24px; box-shadow: var(--shadow-sm); display: flex; justify-content: space-between; align-items: center; gap: 16px;
        }
        .btn-quick {
            padding: 12px 24px; border-radius: 20px; font-size: 13px; font-weight: 600; text-decoration: none;
            transition: all 0.2s; display: inline-flex; align-items: center; gap: 8px; border: 1px solid var(--border-light); cursor: pointer;
        }
        .btn-quick-primary { background: var(--primary); color: #ffffff; border-color: var(--primary); }
        .btn-quick-primary:hover { background: #1d4ed8; }

        /* MODAL STYLING */
        .modal-overlay {
            position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
            background: rgba(15, 23, 42, 0.5); backdrop-filter: blur(4px);
            display: none; align-items: center; justify-content: center; z-index: 100;
        }
        .modal-box {
            background: #ffffff; border-radius: var(--radius-lg); width: 100%; max-width: 500px;
            padding: 28px; box-shadow: 0 20px 25px -5px rgb(0 0 0 / 0.1);
        }
        .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
        .modal-title { font-size: 18px; font-weight: 700; }
        .form-group { margin-bottom: 16px; }
        .form-group label { display: block; font-size: 12px; font-weight: 600; color: var(--text-muted); margin-bottom: 6px; }
        .form-group input, .form-group select {
            width: 100%; padding: 10px 12px; border: 1px solid var(--border-light); border-radius: var(--radius-md); font-size: 13px; outline: none;
        }
        .form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 24px; }
    </style>
</head>
<body>

    <header class="topbar">
        <div class="topbar-brand">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>
        <div class="topbar-right">
            <div class="user-info">
                <span class="user-name"><%= nombrePersonal %></span>
                <span class="user-role">PERSONAL ADMINISTRATIVO</span>
            </div>
            <div class="user-avatar"><i data-lucide="user" style="width: 18px; height: 18px;"></i></div>
            <div style="width: 1px; height: 24px; background: var(--border-light); margin: 0 4px;"></div>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout"><i data-lucide="log-out" style="width: 16px; height: 16px;"></i> Salir</a>
        </div>
    </header>

    <div class="app-body">
        <aside class="sidebar">
            <a href="${pageContext.request.contextPath}/admin/dashboard" class="nav-item active">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> Inicio
            </a>
            <a href="${pageContext.request.contextPath}/admin/medicos" class="nav-item">
                <i data-lucide="stethoscope" style="width: 18px;"></i> Médicos y Especialidades
            </a>
            <a href="${pageContext.request.contextPath}/admin/pacientes" class="nav-item">
                <i data-lucide="users" style="width: 18px;"></i> Pacientes
            </a>
            <a href="${pageContext.request.contextPath}/admin/reasignaciones" class="nav-item">
                <i data-lucide="refresh-cw" style="width: 18px;"></i> Reasignaciones
            </a>
            <a href="${pageContext.request.contextPath}/admin/dashboard-operativo" class="nav-item">
                <i data-lucide="bar-chart-2" style="width: 18px;"></i> Dashboard Operativo
            </a>
            <a href="#" class="nav-item" style="margin-top: auto; border-top: 1px solid var(--border-light); border-radius: 0; padding-top: 16px;">
                <i data-lucide="user" style="width: 18px;"></i> Mi Perfil
            </a>
        </aside>

        <main class="main-content">
            <div class="page-header">
                <div>
                    <h1 class="page-title">Tablero de Control Operativo Diario</h1>
                    <p class="page-subtitle">Supervisión inmediata de citas en ventanilla, disponibilidad y reasignaciones en curso</p>
                </div>
                <div class="header-actions">
                    <a href="${pageContext.request.contextPath}/admin/export?format=csv" class="btn-header"><i data-lucide="file-spreadsheet" style="width: 15px;"></i> Exportar Reporte (CSV)</a>
                    <a href="${pageContext.request.contextPath}/admin/export?format=pdf" target="_blank" class="btn-header" style="background: var(--primary); color: #fff; border-color: var(--primary);"><i data-lucide="printer" style="width: 15px;"></i> Imprimir / PDF</a>
                </div>
            </div>

            <% if (flashMessage != null) { %>
                <div class="flash-alert">
                    <i data-lucide="check-circle" style="width: 18px;"></i>
                    <%= flashMessage %>
                </div>
            <% } %>

            <!-- METRIC CARDS 4 COLUMNS -->
            <div class="metrics-grid">
                <div class="metric-card">
                    <div class="metric-title">Citas del Día <i data-lucide="calendar" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value">420</div>
                    <div class="metric-sub">310 Confirmadas | 65 En espera | 45 Completadas</div>
                </div>

                <div class="metric-card">
                    <div class="metric-title">Cancelaciones <i data-lucide="user-x" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value">38</div>
                    <div class="metric-sub">28 Notificadas | 10 Inmediatas ventanilla</div>
                </div>

                <div class="metric-card">
                    <div class="metric-title">Cupos Libres en Red <i data-lucide="check-circle" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value">74</div>
                    <div class="metric-sub">52 Regulares | 22 En proceso oferta</div>
                </div>

                <div class="metric-card">
                    <div class="metric-title">Reasignaciones Pendientes <i data-lucide="clock" style="width: 16px; color: var(--primary);"></i></div>
                    <div class="metric-value">12</div>
                    <div class="metric-sub">8 En ventana | 4 Requieren manual</div>
                </div>
            </div>

            <!-- GRID 2 COLUMNS -->
            <div class="grid-2col">
                <div class="panel">
                    <h3 class="panel-title">Citas de Atención Prioritaria en Ventanilla / Sala de Espera</h3>
                    <table>
                        <thead>
                            <tr>
                                <th>Hora</th>
                                <th>Paciente</th>
                                <th>Sede / Cons.</th>
                                <th>Médico</th>
                                <th>Estado</th>
                                <th>Acción</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (listaCitas != null && !listaCitas.isEmpty()) { 
                                for (Map<String, Object> c : listaCitas) {
                                    String hora = (String) c.get("hora");
                                    if (hora == null) hora = "08:30";
                                    String paciente = (String) c.get("paciente");
                                    String dni = (String) c.get("dni");
                                    String sede = (String) c.get("sede");
                                    String cons = (String) c.get("consultorio");
                                    String medico = (String) c.get("medico");
                                    String estado = (String) c.get("estado");
                                    String badgeClass = (String) c.get("badgeClass");
                                    if (badgeClass == null) badgeClass = "badge-espera";
                                    String btnTexto = (String) c.get("btnTexto");
                                    if (btnTexto == null) btnTexto = "Atender";
                                    int idCita = c.get("idCita") != null ? (Integer) c.get("idCita") : 1;
                            %>
                                <tr>
                                    <td><strong><%= hora %></strong></td>
                                    <td><%= paciente %><br><span style="font-size: 11px; color: var(--text-muted);">DNI: <%= dni != null ? dni : "10492837" %></span></td>
                                    <td><%= sede != null ? sede : "Sede Central" %><br><span style="font-size: 11px; color: var(--text-muted);"><%= cons != null ? cons : "Cons. 101" %></span></td>
                                    <td><%= medico %></td>
                                    <td><span class="badge-status <%= badgeClass %>"><%= estado %></span></td>
                                    <td>
                                        <form method="POST" action="${pageContext.request.contextPath}/admin/dashboard" style="display:inline;">
                                            <input type="hidden" name="action" value="cambiar_estado">
                                            <input type="hidden" name="idCita" value="<%= idCita %>">
                                            <input type="hidden" name="nuevoEstado" value="ATENDIDA">
                                            <button type="submit" class="btn-action-sm"><%= btnTexto %></button>
                                        </form>
                                    </td>
                                </tr>
                            <%  }
                               } %>
                        </tbody>
                    </table>
                </div>

                <div class="panel">
                    <h3 class="panel-title">Operaciones Recientes en Ventanilla</h3>
                    <div class="feed-list">
                        <div class="feed-item">
                            <div class="feed-time">08:24 AM</div>
                            <div>Admisión manual: Paciente P-8830 registrado en Sede Central.</div>
                        </div>
                        <div class="feed-item">
                            <div class="feed-time">08:19 AM</div>
                            <div>Notificación de liberación de cupo enviada (Dr. Silva).</div>
                        </div>
                        <div class="feed-item">
                            <div class="feed-time">08:12 AM</div>
                            <div>Paciente llamado a consultorio 102 (P-1092).</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- QUICK ACTIONS BAR -->
            <div class="quick-actions-bar">
                <span style="font-size: 15px; font-weight: 700;">Acciones Rápidas de Admisión</span>
                <div style="display: flex; gap: 12px;">
                    <button onclick="openModal()" class="btn-quick btn-quick-primary"><i data-lucide="plus-circle" style="width: 16px;"></i> Registrar Cita Manual en Ventanilla</button>
                    <a href="${pageContext.request.contextPath}/admin/pacientes" class="btn-quick"><i data-lucide="user-plus" style="width: 16px;"></i> Registrar Nuevo Paciente en Padrón</a>
                </div>
            </div>
        </main>
    </div>

    <!-- MODAL REGISTRO CITA MANUAL -->
    <div id="modalCita" class="modal-overlay">
        <div class="modal-box">
            <div class="modal-header">
                <h3 class="modal-title">Registrar Cita Manual en Ventanilla</h3>
                <button onclick="closeModal()" style="background:none; border:none; cursor:pointer;"><i data-lucide="x"></i></button>
            </div>
            <form method="POST" action="${pageContext.request.contextPath}/admin/dashboard">
                <input type="hidden" name="action" value="registrar_cita_ventanilla">
                <div class="form-group">
                    <label>DNI del Paciente</label>
                    <input type="text" name="dni" placeholder="Ingrese DNI (e.g. 72345678)" required>
                </div>
                <div class="form-group">
                    <label>Médico Facultativo</label>
                    <select name="medico" required>
                        <option value="Dr. Carlos Mendoza (Cardiología)">Dr. Carlos Mendoza (Cardiología - Sede Norte)</option>
                        <option value="Dra. Elena Rostova (Medicina Interna)">Dra. Elena Rostova (Medicina Interna - Sede Central)</option>
                        <option value="Dr. Fernando Solís (Cardiología)">Dr. Fernando Solís (Cardiología - Sede Norte)</option>
                        <option value="Dra. Valeria Rivas (Cardiología)">Dra. Valeria Rivas (Cardiología - Sede Sur)</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Fecha de Cita</label>
                    <input type="date" name="fecha" value="2026-10-07" required>
                </div>
                <div class="form-group">
                    <label>Hora de Atención</label>
                    <input type="time" name="hora" value="10:30" required>
                </div>
                <div class="form-actions">
                    <button type="button" onclick="closeModal()" class="btn-header">Cancelar</button>
                    <button type="submit" class="btn-header" style="background:var(--primary); color:#fff; border-color:var(--primary);">Registrar Cita</button>
                </div>
            </form>
        </div>
    </div>

    <script>
        lucide.createIcons();
        function openModal() { document.getElementById('modalCita').style.display = 'flex'; }
        function closeModal() { document.getElementById('modalCita').style.display = 'none'; }
    </script>
</body>
</html>