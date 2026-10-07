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
    <title>Alertas y Notificaciones - Clínica Internacional</title>
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

        .main-content { flex: 1; padding: 40px 50px; overflow-y: auto; }
        
        .header-row { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
        .header-title { font-size: 28px; font-weight: 700; color: var(--text-dark); margin-bottom: 4px; letter-spacing: -0.5px; }
        .header-subtitle { font-size: 14px; color: var(--text-gray); }

        .btn-bell {
            width: 44px; height: 44px; border-radius: 12px; background: #ffffff; border: 1px solid var(--border-light);
            display: flex; align-items: center; justify-content: center; color: var(--text-gray); position: relative;
            text-decoration: none; transition: all 0.2s;
        }
        .btn-bell:hover { background: #f8fafc; color: var(--text-dark); }
        .btn-bell::after {
            content: ''; position: absolute; top: 10px; right: 12px; width: 8px; height: 8px; background: #ef4444; border-radius: 50%; border: 2px solid #ffffff;
        }

        .cat-pills { display: flex; gap: 8px; margin-bottom: 24px; }
        .cat-btn {
            padding: 8px 18px; border-radius: 20px; border: 1px solid var(--border-light); background: #ffffff;
            font-size: 13px; font-weight: 500; color: var(--text-gray); cursor: pointer; transition: all 0.2s;
        }
        .cat-btn.active { background: var(--primary-light); color: var(--primary); font-weight: 600; border-color: var(--primary-light); }

        /* NOTIFICATION CARDS */
        .notif-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px 24px; margin-bottom: 16px; box-shadow: 0 4px 16px rgba(0,0,0,0.02); display: flex; gap: 16px;
        }
        .notif-icon {
            width: 40px; height: 40px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0;
        }
        .icon-blue { background: #eff6ff; color: #2563eb; }
        .icon-purple { background: #f3e8ff; color: #9333ea; }
        .icon-red { background: #ffe4e6; color: #ef4444; }
        .icon-green { background: #dcfce7; color: #16a34a; }
        .icon-gray { background: #f1f5f9; color: #64748b; }

        .notif-body { flex: 1; }
        .notif-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
        .notif-title { font-size: 15px; font-weight: 700; color: var(--text-dark); display: flex; align-items: center; gap: 8px; }
        .notif-dot { width: 6px; height: 6px; background: #2563eb; border-radius: 50%; }
        .notif-time { font-size: 12px; color: var(--text-gray); }

        .notif-desc { font-size: 13px; color: var(--text-gray); margin-bottom: 12px; line-height: 1.4; }

        .notif-actions { display: flex; gap: 8px; }
        .btn-act-primary {
            padding: 6px 16px; border-radius: 20px; background: var(--primary); color: #ffffff; border: none;
            font-size: 12px; font-weight: 600; cursor: pointer; text-decoration: none;
        }
        .btn-act-sec {
            padding: 6px 16px; border-radius: 20px; background: #ffffff; color: var(--primary); border: 1px solid var(--primary);
            font-size: 12px; font-weight: 600; cursor: pointer; text-decoration: none;
        }

        @media(max-width:768px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } }
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
            <a href="${pageContext.request.contextPath}/paciente/panel" class="nav-item">
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
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="nav-item active">
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
                <h1 class="header-title">Notificaciones</h1>
                <p class="header-subtitle">3 notificaciones sin leer.</p>
            </div>
            <div style="display: flex; align-items: center; gap: 16px;">
                <a href="#" style="font-size: 13px; color: var(--primary); font-weight: 600; text-decoration: none;">Marcar todo como leído</a>
                <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-bell" title="Alertas">
                    <i data-lucide="bell" style="width: 20px;"></i>
                </a>
            </div>
        </div>

        <div class="cat-pills">
            <button class="cat-btn active">Todas <span style="background: var(--primary); color: #fff; padding: 2px 6px; border-radius: 10px; font-size: 11px;">3</span></button>
            <button class="cat-btn">Citas</button>
            <button class="cat-btn">Resultados</button>
            <button class="cat-btn">Medicamentos</button>
            <button class="cat-btn">Sistema</button>
        </div>

        <!-- 1. Cita -->
        <div class="notif-card">
            <div class="notif-icon icon-blue"><i data-lucide="calendar"></i></div>
            <div class="notif-body">
                <div class="notif-head">
                    <div class="notif-title">Recordatorio de cita <span class="notif-dot"></span></div>
                    <div class="notif-time">Hace 30 min</div>
                </div>
                <div class="notif-desc">Tienes una cita con Dra. María Rodríguez mañana, Lun 11 Ago a las 15:00 en Sede Central.</div>
                <div class="notif-actions">
                    <button class="btn-act-primary">Confirmar</button>
                    <button class="btn-act-sec">Reagendar</button>
                </div>
            </div>
        </div>

        <!-- 2. Resultado -->
        <div class="notif-card">
            <div class="notif-icon icon-purple"><i data-lucide="file-text"></i></div>
            <div class="notif-body">
                <div class="notif-head">
                    <div class="notif-title">Resultado disponible <span class="notif-dot"></span></div>
                    <div class="notif-time">Hace 2h</div>
                </div>
                <div class="notif-desc">Tu análisis de sangre del 4 de agosto ya está disponible en tu historial. El especialista recomienda revisarlo.</div>
                <div class="notif-actions">
                    <button class="btn-act-primary" style="background: #9333ea;">Ver resultado</button>
                </div>
            </div>
        </div>

        <!-- 3. Medicamento -->
        <div class="notif-card">
            <div class="notif-icon icon-red"><i data-lucide="pill"></i></div>
            <div class="notif-body">
                <div class="notif-head">
                    <div class="notif-title">Stock bajo de medicamento <span class="notif-dot"></span></div>
                    <div class="notif-time">Hace 5h</div>
                </div>
                <div class="notif-desc">Te quedan 8 días de Atenolol 50mg. Solicita la renovación de tu receta antes de que se agote.</div>
                <div class="notif-actions">
                    <button class="btn-act-primary" style="background: #ef4444;">Solicitar receta</button>
                </div>
            </div>
        </div>

        <!-- 4. Perfil -->
        <div class="notif-card">
            <div class="notif-icon icon-green"><i data-lucide="check-circle"></i></div>
            <div class="notif-body">
                <div class="notif-head">
                    <div class="notif-title">Perfil actualizado</div>
                    <div class="notif-time">Ayer</div>
                </div>
                <div class="notif-desc">Tu información de contacto y datos de seguro médico han sido actualizados exitosamente.</div>
            </div>
        </div>

        <!-- 5. Cita completada -->
        <div class="notif-card">
            <div class="notif-icon icon-gray"><i data-lucide="check"></i></div>
            <div class="notif-body">
                <div class="notif-head">
                    <div class="notif-title">Cita completada</div>
                    <div class="notif-time">Hace 2 días</div>
                </div>
                <div class="notif-desc">Tu consulta con Dr. Jorge Herrera fue marcada como completada. ¿Cómo fue tu experiencia?</div>
            </div>
        </div>
    </main>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
