<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreMedico = (String) request.getAttribute("nombreMedico");
    if (nombreMedico == null && usuario != null) {
        nombreMedico = "Dr. " + usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreMedico == null) nombreMedico = "Dr. Carlos Mendoza";

    String emailVal = usuario != null && usuario.getEmail() != null ? usuario.getEmail() : "carlos.mendoza@clinica.pe";

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
    <title>Perfil del Médico - Clínica Internacional</title>
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

        /* PROFILE LAYOUT GRID */
        .profile-grid { display: grid; grid-template-columns: 320px 1fr; gap: 24px; max-width: 1100px; margin: 0 auto; }
        
        .card-box {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 24px; box-shadow: 0 2px 10px rgba(0,0,0,0.02);
        }

        .avatar-section { text-align: center; margin-bottom: 20px; padding-bottom: 20px; border-bottom: 1px solid var(--border-light); }
        .avatar-circle {
            width: 80px; height: 80px; border-radius: 50%; background: #eff6ff; color: var(--primary);
            display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 28px;
            margin: 0 auto 12px auto; border: 3px solid #ffffff; box-shadow: 0 4px 12px rgba(37,99,235,0.15);
        }
        .profile-fullname { font-size: 18px; font-weight: 700; color: var(--text-dark); }
        .profile-badge {
            display: inline-block; padding: 4px 12px; border-radius: 20px; background: #e0f2fe; color: #0284c7;
            font-size: 12px; font-weight: 600; margin-top: 6px;
        }

        .field-group { margin-bottom: 16px; }
        .field-label { font-size: 12px; font-weight: 600; color: var(--text-gray); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; }
        .field-val {
            font-size: 14px; font-weight: 600; color: var(--text-dark); padding: 12px 16px; background: #f8fafc;
            border-radius: var(--radius-md); border: 1px solid var(--border-light);
        }

        @media(max-width:900px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } .profile-grid { grid-template-columns: 1fr; } }
    </style>
</head>
<body>

    <aside class="sidebar">
        <div class="brand-container">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>
        <nav class="nav-menu">
            <a href="${pageContext.request.contextPath}/medico/inicio" class="nav-item">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> Inicio
            </a>
            <a href="${pageContext.request.contextPath}/medico/agenda" class="nav-item">
                <i data-lucide="calendar" style="width: 18px;"></i> Mi Agenda
            </a>
            <a href="${pageContext.request.contextPath}/medico/atencion" class="nav-item">
                <i data-lucide="stethoscope" style="width: 18px;"></i> Atención Paciente
            </a>
            <a href="${pageContext.request.contextPath}/medico/perfil" class="nav-item active">
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
                <h1 class="header-title">Perfil del Médico</h1>
                <p class="header-subtitle">Información profesional, especialidad y credenciales registradas.</p>
            </div>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-bell" title="Alertas">
                <i data-lucide="bell" style="width: 20px;"></i>
            </a>
        </div>

        <div class="profile-grid">
            <!-- IZQUIERDA: AVATAR CARD -->
            <div class="card-box">
                <div class="avatar-section">
                    <div class="avatar-circle"><%= inicialesMedico %></div>
                    <div class="profile-fullname"><%= nombreMedico %></div>
                    <span class="profile-badge">Médico Especialista Habilitado</span>
                </div>

                <div class="field-group">
                    <div class="field-label">Colegiatura / CMP</div>
                    <div class="field-val">CMP 45892 (Hábil)</div>
                </div>

                <div class="field-group">
                    <div class="field-label">Especialidad</div>
                    <div class="field-val" style="color: var(--primary);">Cardiología</div>
                </div>
            </div>

            <!-- DERECHA: CREDANCIALES DETALLADAS -->
            <div class="card-box">
                <h2 style="font-size: 16px; font-weight: 700; color: var(--text-dark); margin-bottom: 20px;">
                    Detalles Institucionales
                </h2>

                <div class="field-group">
                    <div class="field-label">Correo Institucional</div>
                    <div class="field-val"><%= emailVal %></div>
                </div>

                <div class="field-group">
                    <div class="field-label">Sede Asignada Principal</div>
                    <div class="field-val">Sede Central — San Isidro (Consultorio 302)</div>
                </div>

                <div class="field-group">
                    <div class="field-label">Propuesta de Horario Asistencial</div>
                    <div class="field-val">Turno Mañana: 08:00 AM a 13:00 PM (Lunes a Viernes)</div>
                </div>
            </div>
        </div>
    </main>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
