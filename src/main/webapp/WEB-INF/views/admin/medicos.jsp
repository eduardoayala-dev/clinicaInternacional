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
    <title>Padrón de Médicos - Clínica Internacional</title>
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
        .filter-grid { display: grid; grid-template-columns: 2fr 1.5fr 1.5fr 1fr auto; gap: 16px; align-items: flex-end; }
        .form-group label { display: block; font-size: 13px; font-weight: 600; color: var(--text-dark); margin-bottom: 6px; }
        .form-group select, .form-group input {
            width: 100%; padding: 12px 16px; border-radius: var(--radius-md); border: 1px solid var(--border-light);
            font-size: 14px; color: var(--text-dark); outline: none; background: #ffffff; transition: all 0.2s;
        }
        .form-group select:focus, .form-group input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(37,99,235,0.1); }

        .btn-add-medico {
            background: var(--primary); color: #ffffff; border: none; padding: 12px 24px; border-radius: 30px;
            font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 12px rgba(37,99,235,0.2);
            display: inline-flex; align-items: center; gap: 8px; text-decoration: none;
        }
        .btn-add-medico:hover { background: var(--primary-hover); transform: translateY(-1px); }

        /* TABLE CARD */
        .table-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.02); margin-bottom: 24px;
        }
        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { background: #f8fafc; padding: 16px 20px; font-size: 12px; font-weight: 600; color: var(--text-gray); border-bottom: 1px solid var(--border-light); }
        td { padding: 16px 20px; font-size: 14px; color: var(--text-dark); border-bottom: 1px solid var(--border-light); vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover { background: #f8fafc; }

        .badge-status { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; display: inline-block; }
        .badge-activo { background: #dcfce7; color: #166534; }
        .badge-licencia { background: #fef3c7; color: #92400e; }

        .btn-edit-tbl {
            padding: 8px 16px; border-radius: 20px; font-size: 13px; font-weight: 600; text-decoration: none;
            transition: all 0.2s; display: inline-flex; align-items: center; gap: 6px; background: var(--primary-light); color: var(--primary);
        }
        .btn-edit-tbl:hover { background: var(--primary); color: #ffffff; }

        /* STATS GRID FOOTER */
        .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
        .stat-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.02);
        }
        .stat-card-title { font-size: 12px; font-weight: 600; color: var(--text-gray); text-transform: uppercase; letter-spacing: 0.5px; }
        .stat-card-num { font-size: 28px; font-weight: 700; color: var(--text-dark); margin: 8px 0 4px 0; letter-spacing: -0.5px; }
        .stat-card-sub { font-size: 12px; color: var(--text-gray); font-weight: 500; }

        @media(max-width:1024px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } .filter-grid, .stats-grid { grid-template-columns: 1fr; } }
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
            <a href="${pageContext.request.contextPath}/admin/medicos" class="nav-item active">
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
                <h1 class="header-title">Padrón de Médicos y Especialidades</h1>
                <p class="header-subtitle">Mantenimiento de datos operativos de profesionales clínicos y asignación de consultorios.</p>
            </div>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-bell" title="Alertas">
                <i data-lucide="bell" style="width: 20px;"></i>
            </a>
        </div>

        <!-- FILTER CARD -->
        <div class="filter-card">
            <div class="filter-grid">
                <div class="form-group">
                    <label>Buscar Médico o CMP</label>
                    <input type="text" placeholder="Ej: Dr. Mendoza o CMP 45892">
                </div>

                <div class="form-group">
                    <label>Especialidad</label>
                    <select>
                        <option value="">Todas las Especialidades</option>
                        <option>Cardiología</option>
                        <option>Neurología</option>
                        <option>Medicina General</option>
                    </select>
                </div>

                <div class="form-group">
                    <label>Sede Asignada</label>
                    <select>
                        <option value="">Todas las Sedes</option>
                        <option>Sede Central</option>
                        <option>Sede San Borja</option>
                    </select>
                </div>

                <div class="form-group">
                    <label>Estado</label>
                    <select>
                        <option value="ACTIVO">Activo</option>
                        <option value="LICENCIA">Licencia</option>
                    </select>
                </div>

                <a href="${pageContext.request.contextPath}/admin/usuarios" class="btn-add-medico">
                    <i data-lucide="user-plus" style="width: 18px;"></i> + Nuevo Médico
                </a>
            </div>
        </div>

        <!-- TABLE CARD -->
        <div class="table-card">
            <table>
                <thead>
                    <tr>
                        <th>CMP / DOC</th>
                        <th>NOMBRE Y APELLIDOS</th>
                        <th>ESPECIALIDAD(ES)</th>
                        <th>SEDE(S) ASIGNADAS</th>
                        <th>HORAS / SEMANA</th>
                        <th>ESTADO</th>
                        <th style="text-align: right;">ACCIÓN</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td style="font-weight: 700;">CMP 45211</td>
                        <td style="font-weight: 600;">Dr. Carlos Mendoza Valdivia <br><span style="font-size: 11px; color: var(--text-gray);">DNI 09482711</span></td>
                        <td>Cardiología Clínica</td>
                        <td>Sede Central</td>
                        <td>36 hrs</td>
                        <td><span class="badge-status badge-activo">Activo</span></td>
                        <td style="text-align: right;">
                            <a href="${pageContext.request.contextPath}/admin/usuarios" class="btn-edit-tbl">Editar</a>
                        </td>
                    </tr>
                    <tr>
                        <td style="font-weight: 700;">CMP 45892</td>
                        <td style="font-weight: 600;">Dra. Elena Rostova K. <br><span style="font-size: 11px; color: var(--text-gray);">DNI 41908233</span></td>
                        <td>Medicina Interna</td>
                        <td>Sede Central</td>
                        <td>48 hrs</td>
                        <td><span class="badge-status badge-activo">Activo</span></td>
                        <td style="text-align: right;">
                            <a href="${pageContext.request.contextPath}/admin/usuarios" class="btn-edit-tbl">Editar</a>
                        </td>
                    </tr>
                    <tr>
                        <td style="font-weight: 700;">CMP 38902</td>
                        <td style="font-weight: 600;">Dr. Fernando Solís Bravo <br><span style="font-size: 11px; color: var(--text-gray);">DNI 88761234</span></td>
                        <td>Cardiología / Arritmias</td>
                        <td>Sede Norte</td>
                        <td>30 hrs</td>
                        <td><span class="badge-status badge-activo">Activo</span></td>
                        <td style="text-align: right;">
                            <a href="${pageContext.request.contextPath}/admin/usuarios" class="btn-edit-tbl">Editar</a>
                        </td>
                    </tr>
                    <tr>
                        <td style="font-weight: 700;">CMP 39871</td>
                        <td style="font-weight: 600;">Dr. Roberto Salas Gómez <br><span style="font-size: 11px; color: var(--text-gray);">DNI 18456789</span></td>
                        <td>Dermatología Clínica</td>
                        <td>Sede Sur, Sede Central</td>
                        <td>32 hrs</td>
                        <td><span class="badge-status badge-activo">Activo</span></td>
                        <td style="text-align: right;">
                            <a href="${pageContext.request.contextPath}/admin/usuarios" class="btn-edit-tbl">Editar</a>
                        </td>
                    </tr>
                    <tr>
                        <td style="font-weight: 700;">CMP 31094</td>
                        <td style="font-weight: 600;">Dr. Hernán Cáceres Peña <br><span style="font-size: 11px; color: var(--text-gray);">DNI 07894561</span></td>
                        <td>Cardiología Intervencionista</td>
                        <td>Sede Central</td>
                        <td>20 hrs</td>
                        <td><span class="badge-status badge-licencia">Licencia</span></td>
                        <td style="text-align: right;">
                            <a href="${pageContext.request.contextPath}/admin/usuarios" class="btn-edit-tbl">Editar</a>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <!-- STATS FOOTER -->
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-card-title">Total en Padrón</div>
                <div class="stat-card-num">84</div>
                <div class="stat-card-sub">+2 Altas en revisión</div>
            </div>

            <div class="stat-card">
                <div class="stat-card-title">Total Activos</div>
                <div class="stat-card-num">79</div>
                <div class="stat-card-sub">94.04% Disponibilidad</div>
            </div>

            <div class="stat-card">
                <div class="stat-card-title">En Licencia / Permiso</div>
                <div class="stat-card-num">05</div>
                <div class="stat-card-sub">Reemplazos activados</div>
            </div>

            <div class="stat-card">
                <div class="stat-card-title">Especialidades</div>
                <div class="stat-card-num" style="color: var(--primary);">18</div>
                <div class="stat-card-sub">4 Sedes en Red</div>
            </div>
        </div>
    </main>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
