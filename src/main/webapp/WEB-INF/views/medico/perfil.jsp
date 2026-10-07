<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreMedico = (String) request.getAttribute("nombreMedico");
    if (nombreMedico == null && usuario != null) {
        nombreMedico = "Dr. " + usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreMedico == null) nombreMedico = "Dr. Carlos Mendoza";

    String emailVal = usuario != null ? usuario.getEmail() : "carlos.mendoza@clinica.pe";
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Perfil del Médico - Portal del Médico | Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        :root {
            --primary: #0f172a;
            --bg-page: #f8fafc;
            --bg-card: #ffffff;
            --border-dark: #0f172a;
            --border-light: #cbd5e1;
            --text-dark: #0f172a;
            --text-muted: #64748b;
        }

        * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Inter', sans-serif; }
        body { background-color: var(--bg-page); color: var(--text-dark); display: flex; flex-direction: column; min-height: 100vh; }

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
            font-size: 12px; transition: all 0.2s;
        }
        .btn-logout:hover { background: #fee2e2; border-color: #ef4444; color: #b91c1c; }

        .app-container { display: flex; flex: 1; }

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

        .main-content { flex: 1; padding: 24px 32px; overflow-y: auto; }

        .profile-card {
            background: #ffffff; border: 2px solid var(--border-dark); border-radius: 6px; padding: 24px; max-width: 800px;
        }
        .profile-head { display: flex; align-items: center; gap: 20px; border-bottom: 2px solid var(--border-dark); padding-bottom: 20px; margin-bottom: 20px; }
        .avatar-box {
            width: 70px; height: 70px; border-radius: 50%; background: #0f172a; color: #ffffff; font-size: 24px; font-weight: 800;
            display: flex; align-items: center; justify-content: center; border: 2px solid var(--border-dark);
        }

        .field-group { margin-bottom: 16px; }
        .field-label { font-size: 11px; font-weight: 800; text-transform: uppercase; color: var(--text-muted); margin-bottom: 4px; }
        .field-val { padding: 10px 14px; border: 1px solid var(--border-dark); border-radius: 4px; font-size: 13px; font-weight: 700; background: #f8fafc; }
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
            <a href="${pageContext.request.contextPath}/medico/inicio" class="sidebar-item">
                0. INICIO <i data-lucide="chevron-right" style="width: 16px;"></i>
            </a>
            <a href="${pageContext.request.contextPath}/medico/agenda" class="sidebar-item">
                2. MI AGENDA <i data-lucide="chevron-right" style="width: 16px;"></i>
            </a>
            <a href="${pageContext.request.contextPath}/medico/atencion" class="sidebar-item">
                3. ATENCIÓN PACIENTE <i data-lucide="chevron-right" style="width: 16px;"></i>
            </a>
            <a href="${pageContext.request.contextPath}/medico/perfil" class="sidebar-item active">
                4. PERFIL <i data-lucide="chevron-right" style="width: 16px;"></i>
            </a>
        </aside>

        <!-- MAIN CONTENT -->
        <main class="main-content">
            <div class="profile-card">
                <div class="profile-head">
                    <div class="avatar-box">CM</div>
                    <div>
                        <h2 style="font-size: 20px; font-weight: 800; text-transform: uppercase;"><%= nombreMedico %></h2>
                        <span style="font-size: 12px; font-weight: 700; color: #2563eb;">ESPECIALISTA EN CARDIOLOGÍA — CMP 45892</span>
                    </div>
                </div>

                <div class="field-group">
                    <div class="field-label">CORREO INSTITUCIONAL</div>
                    <div class="field-val"><%= emailVal %></div>
                </div>

                <div class="field-group">
                    <div class="field-label">SEDE ASIGNADA PRINCIPAL</div>
                    <div class="field-val">SEDE CENTRAL — SAN ISIDRO (CONSULTORIO 302)</div>
                </div>

                <div class="field-group">
                    <div class="field-label">CÓDIGO DE COLEGIATURA</div>
                    <div class="field-val">CMP 45892 — COLEGIO MÉDICO DEL PERÚ (ESTADO: HÁBIL)</div>
                </div>

                <div class="field-group">
                    <div class="field-label">PROPUESTA DE DISPONIBILIDAD HORARIA</div>
                    <div class="field-val">TURNO MAÑANA: 08:00 AM A 13:00 PM (LUNES A VIERNES)</div>
                </div>
            </div>
        </main>
    </div>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
