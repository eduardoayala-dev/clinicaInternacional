<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="pe.edu.utp.clinica.model.Paciente" %>
<%
    Usuario usuario = (Usuario) request.getAttribute("usuario");
    if (usuario == null) usuario = (Usuario) session.getAttribute("usuarioLogueado");
    
    Paciente paciente = (Paciente) request.getAttribute("paciente");

    String nombrePaciente = (String) request.getAttribute("nombrePaciente");
    if (nombrePaciente == null && usuario != null) {
        nombrePaciente = usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombrePaciente == null) nombrePaciente = "Paciente Registrado";

    String nombresVal = usuario != null && usuario.getNombres() != null ? usuario.getNombres() : "";
    String apellidosVal = usuario != null && usuario.getApellidos() != null ? usuario.getApellidos() : "";
    String emailVal = usuario != null && usuario.getEmail() != null ? usuario.getEmail() : "";
    String dniVal = paciente != null && paciente.getDni() != null ? paciente.getDni() : "No registrado";
    String fechaNacVal = paciente != null && paciente.getFechaNacimiento() != null ? paciente.getFechaNacimiento().toString() : "";
    String telefonoVal = paciente != null && paciente.getTelefono() != null ? paciente.getTelefono() : "";
    String direccionVal = paciente != null && paciente.getDireccion() != null ? paciente.getDireccion() : "";

    String inicialesPaciente = "P";
    if (nombrePaciente != null && !nombrePaciente.trim().isEmpty()) {
        String[] partes = nombrePaciente.trim().split("\\s+");
        if (partes.length >= 2) {
            inicialesPaciente = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            inicialesPaciente = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }

    String mensajeExito = (String) request.getAttribute("mensajeExito");
    String mensajeError = (String) request.getAttribute("mensajeError");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Administrar Perfil - Clínica Internacional</title>
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
        
        .header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
        .header-title { font-size: 26px; font-weight: 700; color: var(--text-dark); margin-bottom: 4px; letter-spacing: -0.5px; }
        .header-subtitle { font-size: 14px; color: var(--text-gray); }

        .btn-bell {
            width: 44px; height: 44px; border-radius: 12px; background: #ffffff; border: 1px solid var(--border-light);
            display: flex; align-items: center; justify-content: center; color: var(--text-gray); position: relative;
            text-decoration: none; transition: all 0.2s;
        }
        .btn-bell:hover { background: #f8fafc; color: var(--text-dark); }

        /* ALERT BANNERS */
        .alert-banner {
            padding: 12px 16px; border-radius: var(--radius-md); font-size: 13px; font-weight: 500;
            margin-bottom: 20px; display: flex; align-items: center; gap: 10px;
        }
        .alert-success { background: #dcfce7; color: #15803d; border: 1px solid #bbf7d0; }
        .alert-danger { background: #fee2e2; color: #b91c1c; border: 1px solid #fca5a5; }

        /* COMPACT 2-COLUMN PROFILE LAYOUT */
        .profile-layout-grid {
            display: grid; grid-template-columns: 320px 1fr; gap: 24px; max-width: 1100px; margin: 0 auto;
        }

        .profile-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 24px; box-shadow: 0 2px 10px rgba(0,0,0,0.02);
        }

        /* LEFT SIDEBAR CARDS */
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

        .readonly-title { font-size: 14px; font-weight: 700; color: var(--text-dark); margin-bottom: 12px; display: flex; align-items: center; gap: 6px; }
        .readonly-field { margin-bottom: 12px; }
        .readonly-label { font-size: 11px; font-weight: 600; color: var(--text-gray); text-transform: uppercase; letter-spacing: 0.5px; }
        .readonly-value {
            font-size: 13px; font-weight: 600; color: #334155; padding: 8px 12px; background: #f8fafc;
            border-radius: var(--radius-md); border: 1px solid #e2e8f0; margin-top: 4px; display: flex; align-items: center; justify-content: space-between;
        }
        .lock-icon { color: var(--text-light); width: 14px; }

        .security-note {
            font-size: 11px; color: var(--text-gray); background: #f1f5f9; padding: 10px 12px; border-radius: var(--radius-md);
            margin-top: 14px; line-height: 1.4; border-left: 3px solid var(--primary);
        }

        /* RIGHT FORM */
        .form-section-title { font-size: 16px; font-weight: 700; color: var(--text-dark); margin-bottom: 6px; }
        .form-section-desc { font-size: 13px; color: var(--text-gray); margin-bottom: 20px; }

        .form-group { margin-bottom: 16px; }
        .form-group label { display: block; font-size: 13px; font-weight: 600; color: var(--text-dark); margin-bottom: 6px; }
        .form-group input {
            width: 100%; padding: 12px 16px; border-radius: var(--radius-md); border: 1px solid var(--border-light);
            background: #ffffff; font-size: 14px; color: var(--text-dark); outline: none; transition: all 0.2s;
        }
        .form-group input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1); }

        .btn-save {
            background: var(--primary); color: #ffffff; border: none; padding: 12px 28px; border-radius: 30px;
            font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
            display: inline-flex; align-items: center; gap: 8px; margin-top: 8px;
        }
        .btn-save:hover { background: var(--primary-hover); transform: translateY(-1px); }

        @media(max-width:900px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } .profile-layout-grid { grid-template-columns: 1fr; } }
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
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="nav-item">
                <i data-lucide="bell" style="width: 18px;"></i> Alertas
            </a>
            <a href="${pageContext.request.contextPath}/paciente/perfil" class="nav-item active">
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
                <h1 class="header-title">Administrar perfil</h1>
                <p class="header-subtitle">Consulta tu información de identidad y actualiza tus datos de contacto.</p>
            </div>
            <div style="display: flex; align-items: center; gap: 12px;">
                <a href="${pageContext.request.contextPath}/paciente/dashboard" style="font-size: 13px; color: var(--text-gray); font-weight: 600; text-decoration: none; display: flex; align-items: center; gap: 4px;">
                    <i data-lucide="chevron-left" style="width: 16px;"></i> Volver
                </a>
                <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-bell" title="Alertas">
                    <i data-lucide="bell" style="width: 20px;"></i>
                </a>
            </div>
        </div>

        <% if (mensajeExito != null) { %>
            <div class="alert-banner alert-success">
                <i data-lucide="check-circle" style="width: 18px;"></i> <%= mensajeExito %>
            </div>
        <% } %>

        <% if (mensajeError != null) { %>
            <div class="alert-banner alert-danger">
                <i data-lucide="alert-circle" style="width: 18px;"></i> <%= mensajeError %>
            </div>
        <% } %>

        <div class="profile-layout-grid">
            <!-- IZQUIERDA: DATOS DE IDENTIDAD (READ-ONLY) -->
            <div class="profile-card">
                <div class="avatar-section">
                    <div class="avatar-circle"><%= inicialesPaciente %></div>
                    <div class="profile-fullname"><%= nombrePaciente %></div>
                    <span class="profile-badge">Paciente Registrado</span>
                </div>

                <div class="readonly-title">
                    <i data-lucide="shield-check" style="width: 16px; color: var(--primary);"></i> Datos de Identidad
                </div>

                <div class="readonly-field">
                    <div class="readonly-label">Nombres</div>
                    <div class="readonly-value">
                        <%= nombresVal %>
                        <i data-lucide="lock" class="lock-icon"></i>
                    </div>
                </div>

                <div class="readonly-field">
                    <div class="readonly-label">Apellidos</div>
                    <div class="readonly-value">
                        <%= apellidosVal %>
                        <i data-lucide="lock" class="lock-icon"></i>
                    </div>
                </div>

                <div class="readonly-field">
                    <div class="readonly-label">Documento Nacional de Identidad (DNI)</div>
                    <div class="readonly-value">
                        <%= dniVal %>
                        <i data-lucide="lock" class="lock-icon"></i>
                    </div>
                </div>

                <div class="readonly-field">
                    <div class="readonly-label">Fecha de Nacimiento</div>
                    <div class="readonly-value">
                        <%= fechaNacVal.isEmpty() ? "No registrada" : fechaNacVal %>
                        <i data-lucide="lock" class="lock-icon"></i>
                    </div>
                </div>

                <div class="security-note">
                    <i data-lucide="info" style="width: 14px; display: inline; vertical-align: middle;"></i>
                    Los datos de identidad provienen de tu registro oficial. Por seguridad no son editables en línea.
                </div>
            </div>

            <!-- DERECHA: DATOS DE CONTACTO EDITABLES -->
            <div class="profile-card">
                <h2 class="form-section-title">Actualizar Datos de Contacto</h2>
                <p class="form-section-desc">Modifica tu correo, teléfono o dirección para recibir notificaciones y confirmaciones de citas.</p>

                <form action="${pageContext.request.contextPath}/paciente/perfil" method="POST">
                    <div class="form-group">
                        <label for="emailInput">Correo Electrónico</label>
                        <input type="email" id="emailInput" name="email" value="<%= emailVal %>" required placeholder="correo@ejemplo.com">
                    </div>

                    <div class="form-group">
                        <label for="telefonoInput">Teléfono Celular</label>
                        <input type="tel" id="telefonoInput" name="telefono" value="<%= telefonoVal %>" required placeholder="Ej: 987654321">
                    </div>

                    <div class="form-group">
                        <label for="direccionInput">Dirección de Domicilio</label>
                        <input type="text" id="direccionInput" name="direccion" value="<%= direccionVal %>" required placeholder="Ej: Av. Javier Prado Este 1420, San Isidro">
                    </div>

                    <div style="margin-top: 24px; text-align: right;">
                        <button type="submit" class="btn-save">
                            <i data-lucide="save" style="width: 18px;"></i> Guardar Cambios
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </main>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
