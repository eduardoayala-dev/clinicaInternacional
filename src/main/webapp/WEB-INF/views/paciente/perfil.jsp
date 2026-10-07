<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombrePaciente = (String) request.getAttribute("nombrePaciente");
    if (nombrePaciente == null && usuario != null) {
        nombrePaciente = usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombrePaciente == null) nombrePaciente = "Eduardo Ayala";

    String nombresVal = usuario != null ? usuario.getNombres() : "Eduardo";
    String apellidosVal = usuario != null ? usuario.getApellidos() : "Ayala";
    String emailVal = usuario != null ? usuario.getEmail() : "eayala@clinica.pe";

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

        .profile-container { max-width: 800px; margin: 0 auto; }

        /* AVATAR CARD */
        .avatar-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 32px; text-align: center; margin-bottom: 24px; box-shadow: 0 4px 16px rgba(0,0,0,0.02);
        }
        .profile-avatar-wrapper { position: relative; width: 96px; height: 96px; margin: 0 auto 16px; }
        .profile-avatar {
            width: 100%; height: 100%; border-radius: 50%; background: #eff6ff; color: var(--primary);
            display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 32px; border: 4px solid #ffffff;
            box-shadow: 0 6px 16px rgba(0,0,0,0.06);
        }
        .cam-btn {
            position: absolute; bottom: 0; right: 0; width: 32px; height: 32px; border-radius: 50%; background: var(--primary);
            color: #ffffff; display: flex; align-items: center; justify-content: center; border: 2px solid #ffffff; cursor: pointer;
        }

        .profile-name { font-size: 20px; font-weight: 700; color: var(--text-dark); margin-bottom: 2px; }
        .profile-role { font-size: 13px; color: var(--text-gray); margin-bottom: 12px; }
        .btn-del-photo { font-size: 12px; color: #ef4444; font-weight: 600; text-decoration: none; cursor: pointer; }

        /* FORM CARDS */
        .form-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 28px; margin-bottom: 24px; box-shadow: 0 4px 16px rgba(0,0,0,0.02);
        }
        .section-title { font-size: 16px; font-weight: 700; color: var(--text-dark); margin-bottom: 20px; }

        .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
        .form-group { display: flex; flex-direction: column; gap: 6px; }
        .form-group.full { grid-column: 1 / -1; }
        
        label { font-size: 13px; font-weight: 600; color: var(--text-dark); }
        input, select {
            width: 100%; padding: 12px 16px; border-radius: var(--radius-md); border: 1px solid var(--border-light);
            background: #f8fafc; font-size: 14px; color: var(--text-dark); outline: none; transition: all 0.2s;
        }
        input:focus, select:focus { border-color: var(--primary); background: #ffffff; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1); }

        .btn-save {
            background: var(--primary); color: #ffffff; border: none; padding: 14px 32px; border-radius: 30px;
            font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
            display: inline-flex; align-items: center; gap: 8px;
        }
        .btn-save:hover { background: var(--primary-hover); transform: translateY(-1px); }

        @media(max-width:768px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } .form-grid { grid-template-columns: 1fr; } }
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
                <p class="header-subtitle">Actualiza tus datos personales y de contacto.</p>
            </div>
            <div style="display: flex; align-items: center; gap: 16px;">
                <a href="${pageContext.request.contextPath}/paciente/dashboard" style="font-size: 13px; color: var(--text-gray); font-weight: 600; text-decoration: none; display: flex; align-items: center; gap: 4px;">
                    <i data-lucide="chevron-left" style="width: 16px;"></i> Volver
                </a>
                <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-bell" title="Alertas">
                    <i data-lucide="bell" style="width: 20px;"></i>
                </a>
            </div>
        </div>

        <div class="profile-container">
            <!-- TARJETA FOTO -->
            <div class="avatar-card">
                <div class="profile-avatar-wrapper">
                    <div class="profile-avatar"><%= inicialesPaciente %></div>
                    <div class="cam-btn" title="Cambiar foto"><i data-lucide="camera" style="width: 16px;"></i></div>
                </div>
                <div class="profile-name"><%= nombrePaciente %></div>
                <div class="profile-role">Paciente Registrado</div>
                <a href="#" class="btn-del-photo">Eliminar foto</a>
            </div>

            <form onsubmit="alert('¡Perfil actualizado con éxito!'); return false;">
                <!-- DATOS PERSONALES -->
                <div class="form-card">
                    <h3 class="section-title">Datos personales</h3>
                    <div class="form-grid">
                        <div class="form-group">
                            <label>Nombres</label>
                            <input type="text" value="<%= nombresVal %>" required>
                        </div>
                        <div class="form-group">
                            <label>Apellidos</label>
                            <input type="text" value="<%= apellidosVal %>" required>
                        </div>
                        <div class="form-group">
                            <label>Tipo de documento</label>
                            <select>
                                <option value="DNI" selected>DNI - Documento Nacional de Identidad</option>
                                <option value="CE">Carnet de Extranjería</option>
                                <option value="PASAPORTE">Pasaporte</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>Número de documento</label>
                            <input type="text" value="73910284" required>
                        </div>
                        <div class="form-group">
                            <label>Fecha de nacimiento</label>
                            <input type="date" value="1995-06-15" required>
                        </div>
                        <div class="form-group">
                            <label>Género</label>
                            <select>
                                <option value="MASCULINO" selected>Masculino</option>
                                <option value="FEMENINO">Femenino</option>
                                <option value="OTRO">Prefiero no decir</option>
                            </select>
                        </div>
                    </div>
                </div>

                <!-- CONTACTO -->
                <div class="form-card">
                    <h3 class="section-title">Contacto</h3>
                    <div class="form-grid">
                        <div class="form-group">
                            <label>Correo electrónico</label>
                            <input type="email" value="<%= emailVal %>" required>
                        </div>
                        <div class="form-group">
                            <label>Celular</label>
                            <input type="tel" value="987654321" required>
                        </div>
                        <div class="form-group full">
                            <label>Dirección de domicilio</label>
                            <input type="text" value="Av. Javier Prado Este 1420, San Isidro, Lima">
                        </div>
                    </div>
                </div>

                <div style="text-align: right; margin-bottom: 40px;">
                    <button type="submit" class="btn-save">
                        <i data-lucide="check" style="width: 18px;"></i> Guardar Cambios
                    </button>
                </div>
            </form>
        </div>
    </main>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
