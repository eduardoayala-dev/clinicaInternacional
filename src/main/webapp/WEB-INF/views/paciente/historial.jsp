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
    <title>Historial Médico - Clínica Internacional</title>
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

        .btn-bell {
            width: 44px; height: 44px; border-radius: 12px; background: #ffffff; border: 1px solid var(--border-light);
            display: flex; align-items: center; justify-content: center; color: var(--text-gray); position: relative;
            text-decoration: none; transition: all 0.2s;
        }
        .btn-bell:hover { background: #f8fafc; color: var(--text-dark); }
        .btn-bell::after {
            content: ''; position: absolute; top: 10px; right: 12px; width: 8px; height: 8px; background: #ef4444; border-radius: 50%; border: 2px solid #ffffff;
        }

        .main-content { flex: 1; padding: 40px 50px; overflow-y: auto; }
        .header-title { font-size: 28px; font-weight: 700; color: var(--text-dark); margin-bottom: 4px; letter-spacing: -0.5px; }
        .header-subtitle { font-size: 14px; color: var(--text-gray); margin-bottom: 24px; }

        /* CONTROLS HEADER */
        .controls-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; flex-wrap: wrap; gap: 16px; }
        .filter-tabs { display: flex; gap: 8px; background: #ffffff; padding: 4px; border-radius: 30px; border: 1px solid var(--border-light); }
        .tab-btn {
            padding: 8px 20px; border-radius: 20px; border: none; background: transparent;
            font-size: 13px; font-weight: 600; color: var(--text-gray); cursor: pointer; transition: all 0.2s;
        }
        .tab-btn.active { background: var(--primary-light); color: var(--primary); }

        .search-box {
            position: relative; width: 260px;
        }
        .search-box input {
            width: 100%; padding: 10px 16px 10px 38px; border-radius: 30px; border: 1px solid var(--border-light);
            background: #ffffff; font-size: 13px; outline: none; transition: all 0.2s;
        }
        .search-box input:focus { border-color: var(--primary); }
        .search-box i, .search-box svg { position: absolute; left: 14px; top: 50%; transform: translateY(-50%); color: var(--text-light); pointer-events: none; }

        /* TABLE CARD */
        .table-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            box-shadow: 0 4px 20px rgba(0,0,0,0.02); overflow: hidden;
        }
        table { width: 100%; border-collapse: collapse; text-align: left; }
        th {
            background: #f8fafc; padding: 16px 24px; font-size: 11px; font-weight: 700;
            color: var(--text-gray); text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 1px solid var(--border-light);
        }
        td { padding: 18px 24px; font-size: 14px; color: var(--text-dark); border-bottom: 1px solid #f1f5f9; vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover td { background: #fafafa; }

        .doc-name { font-weight: 600; color: var(--text-dark); }
        .doc-spec { font-size: 12px; color: var(--primary); font-weight: 500; }

        .type-chip {
            display: inline-block; padding: 6px 14px; border-radius: 20px; font-size: 12px; font-weight: 500;
            background: #f1f5f9; color: #475569;
        }

        .badge {
            display: inline-block; padding: 6px 14px; border-radius: 20px; font-size: 12px; font-weight: 600;
        }
        .badge-normal { background: #dcfce7; color: #166534; }
        .badge-seguimiento { background: #fef3c7; color: #92400e; }
        .badge-revision { background: #ffe4e6; color: #9f1239; }

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
            <a href="${pageContext.request.contextPath}/paciente/dashboard" class="nav-item">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> Panel
            </a>
            <a href="${pageContext.request.contextPath}/paciente/medicos" class="nav-item">
                <i data-lucide="users" style="width: 18px;"></i> Médicos
            </a>
            <a href="${pageContext.request.contextPath}/paciente/citas" class="nav-item">
                <i data-lucide="calendar" style="width: 18px;"></i> Citas
            </a>
            <a href="${pageContext.request.contextPath}/paciente/historial" class="nav-item active">
                <i data-lucide="clipboard-list" style="width: 18px;"></i> Historial
            </a>
            <a href="${pageContext.request.contextPath}/paciente/reportes" class="nav-item">
                <i data-lucide="bar-chart-2" style="width: 18px;"></i> Reportes
            </a>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="nav-item">
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
        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px;">
            <div>
                <h1 class="header-title">Historial Médico</h1>
                <p class="header-subtitle">Registro completo de tus consultas y estudios clínicos.</p>
            </div>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-bell" title="Alertas">
                <i data-lucide="bell" style="width: 20px;"></i>
            </a>
        </div>

        <div class="controls-row">
            <div class="filter-tabs">
                <button class="tab-btn active" onclick="filterType(this, 'ALL')">Todas</button>
                <button class="tab-btn" onclick="filterType(this, 'Consulta')">Consulta</button>
                <button class="tab-btn" onclick="filterType(this, 'Análisis')">Análisis</button>
                <button class="tab-btn" onclick="filterType(this, 'Diagnóstico')">Diagnóstico</button>
            </div>

            <div class="search-box">
                <i data-lucide="search" style="width: 16px;"></i>
                <input type="text" id="historialSearch" placeholder="Buscar...">
            </div>
        </div>

        <div class="table-card">
            <table>
                <thead>
                    <tr>
                        <th>FECHA</th>
                        <th>MÉDICO</th>
                        <th>DIAGNÓSTICO</th>
                        <th>TIPO</th>
                        <th>ESTADO</th>
                    </tr>
                </thead>
                <tbody id="historialTbody">
                    <tr data-tipo="Consulta">
                        <td style="color: var(--text-gray); font-size: 13px;">4 Ago 2026</td>
                        <td>
                            <div class="doc-name">Dra. María Rodríguez</div>
                            <div class="doc-spec">Medicina General</div>
                        </td>
                        <td style="font-weight: 500;">Chequeo general preventivo</td>
                        <td><span class="type-chip">Consulta</span></td>
                        <td><span class="badge badge-normal">Normal</span></td>
                    </tr>
                    <tr data-tipo="Consulta">
                        <td style="color: var(--text-gray); font-size: 13px;">23 Jul 2026</td>
                        <td>
                            <div class="doc-name">Dr. Jorge Herrera</div>
                            <div class="doc-spec">Traumatología</div>
                        </td>
                        <td style="font-weight: 500;">Lumbalgia leve por sobreesfuerzo</td>
                        <td><span class="type-chip">Consulta</span></td>
                        <td><span class="badge badge-seguimiento">Seguimiento</span></td>
                    </tr>
                    <tr data-tipo="Análisis">
                        <td style="color: var(--text-gray); font-size: 13px;">14 Jun 2026</td>
                        <td>
                            <div class="doc-name">Dr. Carlos Mendoza</div>
                            <div class="doc-spec">Cardiología</div>
                        </td>
                        <td style="font-weight: 500;">Panel CBC completo y Electrocardiograma</td>
                        <td><span class="type-chip">Análisis</span></td>
                        <td><span class="badge badge-normal">Normal</span></td>
                    </tr>
                    <tr data-tipo="Diagnóstico">
                        <td style="color: var(--text-gray); font-size: 13px;">3 Abr 2026</td>
                        <td>
                            <div class="doc-name">Dr. Alejandro Vargas</div>
                            <div class="doc-spec">Neurología</div>
                        </td>
                        <td style="font-weight: 500;">Cefalea tensional por estrés laboral</td>
                        <td><span class="type-chip">Diagnóstico</span></td>
                        <td><span class="badge badge-normal">Normal</span></td>
                    </tr>
                    <tr data-tipo="Análisis">
                        <td style="color: var(--text-gray); font-size: 13px;">28 Feb 2026</td>
                        <td>
                            <div class="doc-name">Dra. Lucía Morales</div>
                            <div class="doc-spec">Medicina General</div>
                        </td>
                        <td style="font-weight: 500;">Radiografía de tórax de control</td>
                        <td><span class="type-chip">Análisis</span></td>
                        <td><span class="badge badge-normal">Normal</span></td>
                    </tr>
                    <tr data-tipo="Análisis">
                        <td style="color: var(--text-gray); font-size: 13px;">8 Ene 2026</td>
                        <td>
                            <div class="doc-name">Dr. Carlos Mendoza</div>
                            <div class="doc-spec">Cardiología</div>
                        </td>
                        <td style="font-weight: 500;">Perfil lipídico y triglicéridos</td>
                        <td><span class="type-chip">Análisis</span></td>
                        <td><span class="badge badge-revision">Revisión</span></td>
                    </tr>
                </tbody>
            </table>
        </div>
    </main>

    <script>
        lucide.createIcons();

        function filterType(btn, tipo) {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            const rows = document.querySelectorAll('#historialTbody tr');
            rows.forEach(row => {
                if (tipo === 'ALL' || row.getAttribute('data-tipo') === tipo) {
                    row.style.display = '';
                } else {
                    row.style.display = 'none';
                }
            });
        }

        document.getElementById('historialSearch').addEventListener('input', function(e) {
            const term = e.target.value.toLowerCase();
            const rows = document.querySelectorAll('#historialTbody tr');
            rows.forEach(row => {
                const text = row.textContent.toLowerCase();
                row.style.display = text.includes(term) ? '' : 'none';
            });
        });
    </script>
</body>
</html>
