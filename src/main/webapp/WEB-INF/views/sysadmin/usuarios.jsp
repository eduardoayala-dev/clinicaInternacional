<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreAdmin = (String) request.getAttribute("nombreAdmin");
    if (nombreAdmin == null && usuario != null) {
        nombreAdmin = usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreAdmin == null) nombreAdmin = "Ing. Roberto Vargas";

    String iniciales = "RV";
    if (nombreAdmin != null && !nombreAdmin.trim().isEmpty()) {
        String[] partes = nombreAdmin.trim().split("\\s+");
        if (partes.length >= 2) {
            iniciales = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            iniciales = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }

    List<Usuario> listaUsuarios = (List<Usuario>) request.getAttribute("listaUsuarios");
    List<Map<String, Object>> sedes = (List<Map<String, Object>>) request.getAttribute("sedes");
    List<Map<String, Object>> especialidades = (List<Map<String, Object>>) request.getAttribute("especialidades");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Directorio de Usuarios y Roles - Clínica Internacional</title>
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

        /* STATS GRID */
        .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
        .stat-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.02);
        }
        .stat-card-title { font-size: 11px; font-weight: 700; color: var(--text-gray); text-transform: uppercase; letter-spacing: 0.5px; display: flex; align-items: center; justify-content: space-between; }
        .stat-card-num { font-size: 26px; font-weight: 700; color: var(--text-dark); margin: 8px 0 4px 0; letter-spacing: -0.5px; }
        .stat-card-sub { font-size: 12px; color: var(--text-gray); font-weight: 500; }

        /* FILTER BAR */
        .filter-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px 24px; margin-bottom: 24px; box-shadow: 0 2px 10px rgba(0,0,0,0.02);
            display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap;
        }
        .filter-inputs { display: flex; gap: 12px; align-items: center; flex: 1; }
        .search-box {
            display: flex; align-items: center; gap: 8px; border: 1px solid var(--border-light);
            padding: 10px 16px; border-radius: var(--radius-md); background: #ffffff; flex: 1; max-width: 360px;
        }
        .search-box input { border: none; outline: none; width: 100%; font-size: 14px; }
        .select-filter {
            padding: 10px 16px; border-radius: var(--radius-md); border: 1px solid var(--border-light);
            font-size: 14px; color: var(--text-dark); outline: none; background: #ffffff;
        }
        .btn-add-user {
            background: var(--primary); color: #ffffff; border: none; padding: 10px 22px; border-radius: 30px;
            font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 12px rgba(37,99,235,0.2);
            display: inline-flex; align-items: center; gap: 8px; text-decoration: none;
        }
        .btn-add-user:hover { background: var(--primary-hover); transform: translateY(-1px); }

        /* TABLE CARD */
        .table-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.02);
        }
        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { background: #f8fafc; padding: 16px 20px; font-size: 12px; font-weight: 600; color: var(--text-gray); border-bottom: 1px solid var(--border-light); }
        td { padding: 16px 20px; font-size: 14px; color: var(--text-dark); border-bottom: 1px solid var(--border-light); vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover { background: #f8fafc; }

        .badge-role { padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; display: inline-block; }
        .badge-admin { background: #fee2e2; color: #991b1b; }
        .badge-medico { background: #e0f2fe; color: #0369a1; }
        .badge-personal { background: #fef3c7; color: #92400e; }
        .badge-paciente { background: #f1f5f9; color: #475569; }

        .badge-activo { background: #dcfce7; color: #166534; }
        .badge-inactivo { background: #fee2e2; color: #991b1b; }

        .btn-edit-tbl {
            padding: 6px 14px; border-radius: 20px; font-size: 12px; font-weight: 600; text-decoration: none;
            transition: all 0.2s; display: inline-flex; align-items: center; gap: 6px; background: var(--primary-light); color: var(--primary);
        }
        .btn-edit-tbl:hover { background: var(--primary); color: #ffffff; }

        /* MODAL STYLING */
        .modal-overlay {
            position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(15,23,42,0.4); backdrop-filter: blur(4px);
            display: none; align-items: center; justify-content: center; z-index: 1000;
        }
        .modal-card {
            background: #ffffff; border-radius: var(--radius-lg); width: 100%; max-width: 560px; padding: 28px;
            box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1); border: 1px solid var(--border-light);
        }
        .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
        .modal-title { font-size: 18px; font-weight: 700; color: var(--text-dark); }
        .modal-close { background: none; border: none; font-size: 20px; color: var(--text-gray); cursor: pointer; }
        .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
        .form-group { display: flex; flex-direction: column; gap: 6px; }
        .form-group.full { grid-column: 1 / -1; }
        .form-group label { font-size: 13px; font-weight: 600; color: var(--text-dark); }
        .form-group input, .form-group select {
            padding: 10px 14px; border-radius: var(--radius-md); border: 1px solid var(--border-light); font-size: 14px; outline: none;
        }
        .form-group input:focus, .form-group select:focus { border-color: var(--primary); }
        .modal-footer { display: flex; justify-content: flex-end; gap: 12px; }
        .btn-cancel { padding: 10px 20px; border-radius: 20px; border: 1px solid var(--border-light); background: #ffffff; cursor: pointer; font-weight: 600; }
        .btn-save { padding: 10px 22px; border-radius: 20px; border: none; background: var(--primary); color: #ffffff; cursor: pointer; font-weight: 600; }

        @media(max-width:1024px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } .stats-grid { grid-template-columns: repeat(2, 1fr); } }
    </style>
</head>
<body>

    <aside class="sidebar">
        <div class="brand-container">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>
        <nav class="nav-menu">
            <a href="${pageContext.request.contextPath}/sysadmin/dashboard" class="nav-item">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> 1. Inicio
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/usuarios" class="nav-item active">
                <i data-lucide="users" style="width: 18px;"></i> 2. Usuarios y Roles
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/sedes" class="nav-item">
                <i data-lucide="building-2" style="width: 18px;"></i> 3. Sedes
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/priorizacion" class="nav-item">
                <i data-lucide="sliders" style="width: 18px;"></i> 4. Config. Priorización
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/analitica" class="nav-item">
                <i data-lucide="bar-chart-3" style="width: 18px;"></i> 5. Indicadores y Analítica
            </a>
        </nav>
        <a href="${pageContext.request.contextPath}/logout" class="user-profile-bottom">
            <div class="avatar-initials"><%= iniciales %></div>
            <div style="flex: 1;">
                <div class="user-name-sm"><%= nombreAdmin %></div>
                <div class="user-role-sm">Administrador del Sistema</div>
            </div>
            <i data-lucide="log-out" style="width: 16px; color: var(--text-gray);"></i>
        </a>
    </aside>

    <main class="main-content">
        <div class="header-row">
            <div>
                <h1 class="header-title">Directorio de Usuarios y Roles del Sistema</h1>
                <p class="header-subtitle">Control de credenciales de acceso y asignación de perfiles estáticos de la plataforma clínica.</p>
            </div>
            <a href="#" class="btn-bell" title="Alertas de Seguridad">
                <i data-lucide="bell" style="width: 20px;"></i>
            </a>
        </div>

        <!-- STATS GRID -->
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-card-title">ROL: MÉDICO <i data-lucide="stethoscope" style="width: 16px;"></i></div>
                <div class="stat-card-num">142</div>
                <div class="stat-card-sub">Activos: 139 | Inactivos: 3</div>
            </div>

            <div class="stat-card">
                <div class="stat-card-title">ROL: ADMIN SISTEMA <i data-lucide="shield" style="width: 16px;"></i></div>
                <div class="stat-card-num">008</div>
                <div class="stat-card-sub">Activos: 8 | Multi-sede: 8</div>
            </div>

            <div class="stat-card">
                <div class="stat-card-title">ROL: PERSONAL ADM. <i data-lucide="briefcase" style="width: 16px;"></i></div>
                <div class="stat-card-num">064</div>
                <div class="stat-card-sub">Activos: 62 | Suspendidos: 2</div>
            </div>

            <div class="stat-card">
                <div class="stat-card-title">ROL: PACIENTE <i data-lucide="user-check" style="width: 16px;"></i></div>
                <div class="stat-card-num">1,248</div>
                <div class="stat-card-sub">Verificados Portal: 98.4%</div>
            </div>
        </div>

        <!-- FILTER BAR -->
        <div class="filter-card">
            <div class="filter-inputs">
                <div class="search-box">
                    <i data-lucide="search" style="width: 18px; color: var(--text-gray);"></i>
                    <input type="text" id="searchInput" placeholder="Buscar por nombre, DNI o correo..." onkeyup="filtrarTabla()">
                </div>
                <select class="select-filter" id="rolFilter" onchange="filtrarTabla()">
                    <option value="">Todos los Roles</option>
                    <option value="MEDICO">Médico</option>
                    <option value="ADMINISTRADOR">Admin Sistema</option>
                    <option value="PERSONAL_ADMINISTRATIVO">Personal Adm.</option>
                    <option value="PACIENTE">Paciente</option>
                </select>
                <select class="select-filter" id="estadoFilter" onchange="filtrarTabla()">
                    <option value="">Todos los Estados</option>
                    <option value="ACTIVO">Activo</option>
                    <option value="INACTIVO">Inactivo</option>
                </select>
            </div>
            <button class="btn-add-user" onclick="abrirModalNuevo()">
                <i data-lucide="user-plus" style="width: 18px;"></i> + Nuevo Usuario
            </button>
        </div>

        <!-- TABLE CARD -->
        <div class="table-card">
            <table id="usuariosTable">
                <thead>
                    <tr>
                        <th>Nombre y Apellidos</th>
                        <th>Correo / Identificador</th>
                        <th>Rol Asignado</th>
                        <th>Sede Principal</th>
                        <th>Estado</th>
                        <th>Fecha Registro</th>
                        <th>Acciones</th>
                    </tr>
                </thead>
                <tbody>
                    <% if (listaUsuarios != null && !listaUsuarios.isEmpty()) { 
                        for (Usuario u : listaUsuarios) { 
                            String badgeClass = "badge-paciente";
                            if ("ADMINISTRADOR".equals(u.getRol())) badgeClass = "badge-admin";
                            else if ("MEDICO".equals(u.getRol())) badgeClass = "badge-medico";
                            else if ("PERSONAL_ADMINISTRATIVO".equals(u.getRol())) badgeClass = "badge-personal";
                    %>
                    <tr>
                        <td><strong><%= u.getNombreCompleto() %></strong></td>
                        <td style="color: var(--text-gray);"><%= u.getEmail() %></td>
                        <td><span class="badge-role <%= badgeClass %>"><%= u.getRol() %></span></td>
                        <td>Sede Central</td>
                        <td>
                            <% if (u.isActivo()) { %>
                                <span class="badge-role badge-activo">ACTIVO</span>
                            <% } else { %>
                                <span class="badge-role badge-inactivo">INACTIVO</span>
                            <% } %>
                        </td>
                        <td style="color: var(--text-gray);">2026-10-01</td>
                        <td>
                            <button class="btn-edit-tbl" onclick="editarUsuario('<%= u.getIdUsuario() %>', '<%= u.getNombres() %>', '<%= u.getApellidos() %>', '<%= u.getEmail() %>', '<%= u.getRol() %>')">
                                <i data-lucide="edit-2" style="width: 14px;"></i> Editar
                            </button>
                        </td>
                    </tr>
                    <% } } else { %>
                    <tr>
                        <td colspan="7" style="text-align: center; color: var(--text-gray); padding: 30px;">
                            No hay usuarios registrados en el sistema.
                        </td>
                    </tr>
                    <% } %>
                </tbody>
            </table>
        </div>
    </main>

    <!-- MODAL REGISTRO/EDICION -->
    <div class="modal-overlay" id="userModal">
        <div class="modal-card">
            <div class="modal-header">
                <h3 class="modal-title" id="modalTitle">Nuevo Usuario del Sistema</h3>
                <button class="modal-close" onclick="cerrarModal()">&times;</button>
            </div>
            <form action="${pageContext.request.contextPath}/admin/usuarios" method="POST">
                <input type="hidden" name="idUsuario" id="idUsuario">
                <div class="form-grid">
                    <div class="form-group">
                        <label>Nombres</label>
                        <input type="text" name="nombres" id="modalNombres" required>
                    </div>
                    <div class="form-group">
                        <label>Apellidos</label>
                        <input type="text" name="apellidos" id="modalApellidos" required>
                    </div>
                    <div class="form-group full">
                        <label>Correo Electrónico</label>
                        <input type="email" name="email" id="modalEmail" required>
                    </div>
                    <div class="form-group">
                        <label>Rol del Sistema</label>
                        <select name="rol" id="modalRol" required>
                            <option value="MEDICO">Médico</option>
                            <option value="PERSONAL_ADMINISTRATIVO">Personal Administrativo</option>
                            <option value="ADMINISTRADOR">Administrador del Sistema</option>
                            <option value="PACIENTE">Paciente</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Contraseña</label>
                        <input type="password" name="password" id="modalPassword" placeholder="Solo para nuevo usuario">
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn-cancel" onclick="cerrarModal()">Cancelar</button>
                    <button type="submit" class="btn-save">Guardar Usuario</button>
                </div>
            </form>
        </div>
    </div>

    <script>
        lucide.createIcons();

        function abrirModalNuevo() {
            document.getElementById('modalTitle').innerText = "Nuevo Usuario del Sistema";
            document.getElementById('idUsuario').value = "";
            document.getElementById('modalNombres').value = "";
            document.getElementById('modalApellidos').value = "";
            document.getElementById('modalEmail').value = "";
            document.getElementById('modalPassword').required = true;
            document.getElementById('userModal').style.display = "flex";
        }

        function editarUsuario(id, nombres, apellidos, email, rol) {
            document.getElementById('modalTitle').innerText = "Editar Usuario #" + id;
            document.getElementById('idUsuario').value = id;
            document.getElementById('modalNombres').value = nombres;
            document.getElementById('modalApellidos').value = apellidos;
            document.getElementById('modalEmail').value = email;
            document.getElementById('modalRol').value = rol;
            document.getElementById('modalPassword').required = false;
            document.getElementById('userModal').style.display = "flex";
        }

        function cerrarModal() {
            document.getElementById('userModal').style.display = "none";
        }

        function filtrarTabla() {
            let q = document.getElementById('searchInput').value.toLowerCase();
            let rol = document.getElementById('rolFilter').value.toLowerCase();
            let estado = document.getElementById('estadoFilter').value.toLowerCase();
            let rows = document.querySelectorAll('#usuariosTable tbody tr');

            rows.forEach(tr => {
                let text = tr.innerText.toLowerCase();
                let match = text.includes(q) && tr.innerText.toLowerCase().includes(rol) && tr.innerText.toLowerCase().includes(estado);
                tr.style.display = match ? '' : 'none';
            });
        }
    </script>
</body>
</html>
