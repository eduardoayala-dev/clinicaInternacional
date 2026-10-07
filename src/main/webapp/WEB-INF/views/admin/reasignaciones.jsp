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
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Seguimiento de Reasignaciones - Clínica Internacional</title>
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

        /* FILTER CARD */
        .filter-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 24px; margin-bottom: 24px; box-shadow: 0 2px 10px rgba(0,0,0,0.02);
        }
        .filter-grid { display: grid; grid-template-columns: 1.5fr 1.5fr 1.5fr auto; gap: 16px; align-items: flex-end; }
        .form-group label { display: block; font-size: 13px; font-weight: 600; color: var(--text-dark); margin-bottom: 6px; }
        .form-group select, .form-group input {
            width: 100%; padding: 12px 16px; border-radius: var(--radius-md); border: 1px solid var(--border-light);
            font-size: 14px; color: var(--text-dark); outline: none; background: #ffffff; transition: all 0.2s;
        }
        .form-group select:focus, .form-group input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(37,99,235,0.1); }

        .btn-update {
            background: var(--primary); color: #ffffff; border: none; padding: 12px 24px; border-radius: 30px;
            font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 12px rgba(37,99,235,0.2);
            display: inline-flex; align-items: center; gap: 8px;
        }
        .btn-update:hover { background: var(--primary-hover); transform: translateY(-1px); }

        /* TABLE CARD */
        .table-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.02); margin-bottom: 24px;
        }
        .table-header {
            padding: 20px 24px; border-bottom: 1px solid var(--border-light); display: flex; justify-content: space-between; align-items: center;
        }
        .table-title { font-size: 16px; font-weight: 700; color: var(--text-dark); display: flex; align-items: center; gap: 8px; }
        .table-badge { font-size: 12px; background: #eff6ff; color: var(--primary); padding: 4px 12px; border-radius: 20px; font-weight: 600; }

        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { background: #f8fafc; padding: 16px 20px; font-size: 12px; font-weight: 600; color: var(--text-gray); border-bottom: 1px solid var(--border-light); }
        td { padding: 16px 20px; font-size: 14px; color: var(--text-dark); border-bottom: 1px solid var(--border-light); vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover { background: #f8fafc; }

        .badge-status { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; display: inline-block; }
        .badge-exito { background: #dcfce7; color: #166534; }
        .badge-agotado { background: #fef3c7; color: #92400e; }

        @media(max-width:900px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } .filter-grid { grid-template-columns: 1fr; } }
    </style>
</head>
<body>

    <aside class="sidebar">
        <div class="brand-container">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>
        <nav class="nav-menu">
            <a href="${pageContext.request.contextPath}/admin/dashboard" class="nav-item">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> Inicio / Tablero
            </a>
            <a href="${pageContext.request.contextPath}/admin/medicos" class="nav-item">
                <i data-lucide="stethoscope" style="width: 18px;"></i> Padrón Médicos
            </a>
            <a href="${pageContext.request.contextPath}/admin/usuarios" class="nav-item">
                <i data-lucide="users" style="width: 18px;"></i> Gestión Usuarios
            </a>
            <a href="${pageContext.request.contextPath}/admin/reasignaciones" class="nav-item active">
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
                <h1 class="header-title">Seguimiento de Reasignaciones Automáticas</h1>
                <p class="header-subtitle">Monitoreo del flujo de rescate de cupos por cancelaciones anticipadas.</p>
            </div>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-bell" title="Alertas">
                <i data-lucide="bell" style="width: 20px;"></i>
            </a>
        </div>

        <!-- FILTER CARD -->
        <div class="filter-card">
            <div class="filter-grid">
                <div class="form-group">
                    <label>Sede Clínica</label>
                    <select>
                        <option value="">Todas las Sedes</option>
                        <option>Sede Central</option>
                        <option>Sede San Borja</option>
                    </select>
                </div>

                <div class="form-group">
                    <label>Especialidad</label>
                    <select>
                        <option value="">Todas las Especialidades</option>
                        <option>Cardiología</option>
                        <option>Medicina Interna</option>
                    </select>
                </div>

                <div class="form-group">
                    <label>Fecha de Cancelación</label>
                    <input type="date" value="2026-10-14">
                </div>

                <button class="btn-update" onclick="location.reload();">
                    <i data-lucide="refresh-cw" style="width: 16px;"></i> Actualizar Log
                </button>
            </div>
        </div>

        <!-- TABLE CARD -->
        <div class="table-card">
            <div class="table-header">
                <div class="table-title">
                    <i data-lucide="history" style="width: 18px; color: var(--primary);"></i> Historial de Ejecución del Motor (Solo Lectura)
                </div>
                <span class="table-badge">Ejecución Activa</span>
            </div>

            <table>
                <thead>
                    <tr>
                        <th>CÓDIGO CITA</th>
                        <th>CUPO LIBERADO (FECHA/HORA/MÉDICO)</th>
                        <th>MOTIVO ANULACIÓN</th>
                        <th>CANDIDATOS OFRECIDOS</th>
                        <th>TIEMPO RESPUESTA / ESTADO</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td style="font-weight: 700; color: var(--primary);">#CAN-1092</td>
                        <td>25/10 10:15 AM — Dr. Mendoza (Cardio - Sede Norte)</td>
                        <td>Paciente canceló web</td>
                        <td>1. Juan Pérez (Rechazó) &nbsp;➔&nbsp; <strong>2. María Ramos (Aceptó)</strong></td>
                        <td><span class="badge-status badge-exito">14m 12s (Cupo Recuperado)</span></td>
                    </tr>
                    <tr>
                        <td style="font-weight: 700; color: var(--primary);">#CAN-1093</td>
                        <td>25/10 11:30 AM — Dra. Rostova (Med. Int - Central)</td>
                        <td>Incompatibilidad laboral</td>
                        <td><strong>1. Lucía Vega (Aceptó)</strong></td>
                        <td><span class="badge-status badge-exito">06m 40s (Cupo Recuperado)</span></td>
                    </tr>
                    <tr>
                        <td style="font-weight: 700; color: var(--primary);">#CAN-1094</td>
                        <td>24/10 04:15 PM — Dr. Salas (Dermato - Sur)</td>
                        <td>Cancelación por ventanilla</td>
                        <td>1. Beatriz S. (Expiró) &nbsp;➔&nbsp; 2. Carlos P. (Rechazó) &nbsp;➔&nbsp; 3. Raúl G. (Expiró)</td>
                        <td><span class="badge-status badge-agotado">45m 00s (Agotado)</span></td>
                    </tr>
                    <tr>
                        <td style="font-weight: 700; color: var(--primary);">#CAN-1095</td>
                        <td>24/10 05:00 PM — Dra. Ruiz (Oftalmo - Central)</td>
                        <td>Reprogramación externa</td>
                        <td>1. Rosa M. (Expiró) &nbsp;➔&nbsp; 2. Hugo F. (Rechazó)</td>
                        <td><span class="badge-status badge-agotado">30m 00s (Agotado)</span></td>
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
