<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombrePaciente = (String) request.getAttribute("nombrePaciente");
    if (nombrePaciente == null && usuario != null) {
        nombrePaciente = usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombrePaciente == null) nombrePaciente = "Paciente Registrado";

    List<Map<String, Object>> listaMedicos = (List<Map<String, Object>>) request.getAttribute("listaMedicos");

    String inicialesPaciente = "P";
    if (nombrePaciente != null && !nombrePaciente.trim().isEmpty()) {
        String[] partes = nombrePaciente.trim().split("\\s+");
        if (partes.length >= 2) {
            inicialesPaciente = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            inicialesPaciente = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }

    pe.edu.utp.clinica.dao.CitaDAO citaDAO_bell = new pe.edu.utp.clinica.dao.CitaDAO();
    boolean tieneNotifSinLeer = usuario != null && citaDAO_bell.contarNotificacionesSinLeer(usuario.getIdUsuario()) > 0;
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Directorio de Especialistas - Clínica Internacional</title>
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
        
        .header-row { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
        .header-title { font-size: 26px; font-weight: 700; color: var(--text-dark); margin-bottom: 4px; letter-spacing: -0.5px; }
        .header-subtitle { font-size: 14px; color: var(--text-gray); }

        .btn-bell {
            width: 44px; height: 44px; border-radius: 12px; background: #ffffff; border: 1px solid var(--border-light);
            display: flex; align-items: center; justify-content: center; color: var(--text-gray); position: relative;
            text-decoration: none; transition: all 0.2s;
        }
        .btn-bell:hover { background: #f8fafc; color: var(--text-dark); }
        .btn-bell.has-unread::after {
            content: ''; position: absolute; top: 10px; right: 12px; width: 8px; height: 8px; background: #ef4444; border-radius: 50%; border: 2px solid #ffffff;
        }

        /* SEARCH & TOOLBAR */
        .pricing-banner {
            background: #ffffff; border: 1px solid #bfdbfe; border-left: 4px solid var(--primary);
            padding: 12px 18px; border-radius: var(--radius-md); margin-bottom: 20px;
            display: flex; align-items: center; justify-content: space-between; gap: 12px; font-size: 13px; color: #1e40af;
        }
        .pricing-banner-text { font-weight: 500; }
        .pricing-badge { background: #eff6ff; padding: 4px 10px; border-radius: 20px; font-weight: 700; color: var(--primary); font-size: 12px; }

        .toolbar-row {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 16px;
            margin-bottom: 20px;
            flex-wrap: wrap;
        }

        .search-container { position: relative; flex: 1; max-width: 480px; margin-bottom: 0; }
        .search-container input {
            width: 100%; padding: 12px 16px 12px 42px; border-radius: 30px; border: 1px solid var(--border-light);
            background: #ffffff; font-size: 14px; outline: none; transition: all 0.2s;
        }
        .search-container input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1); }
        .search-container i, .search-container svg { position: absolute; left: 16px; top: 50%; transform: translateY(-50%); color: var(--text-light); pointer-events: none; }

        .view-switcher-toggle {
            display: flex;
            background: #ffffff;
            border: 1px solid var(--border-light);
            border-radius: var(--radius-md);
            padding: 3px;
            gap: 4px;
            box-shadow: 0 2px 6px rgba(0,0,0,0.02);
        }

        .view-btn {
            background: transparent;
            border: none;
            padding: 8px 12px;
            border-radius: 8px;
            color: var(--text-gray);
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.2s;
        }

        .view-btn.active {
            background: var(--primary-light);
            color: var(--primary);
            font-weight: 600;
        }

        .view-btn:hover:not(.active) {
            background: #f8fafc;
            color: var(--text-dark);
        }

        .cat-pills { display: flex; gap: 8px; overflow-x: auto; padding-bottom: 8px; margin-bottom: 24px; }
        .cat-btn {
            padding: 8px 16px; border-radius: 20px; border: 1px solid var(--border-light); background: #ffffff;
            font-size: 13px; font-weight: 500; color: var(--text-gray); cursor: pointer; white-space: nowrap; transition: all 0.2s;
        }
        .cat-btn.active, .cat-btn:hover { background: var(--primary-light); color: var(--primary); border-color: var(--primary-light); font-weight: 600; }

        /* SPECIALTY COLOR ACCENTS */
        .spec-cardiologia .doc-avatar-circle { background: linear-gradient(135deg, #ffe4e6 0%, #fecdd3 100%); color: #be123c; border-color: #fda4af; }
        .spec-cardiologia .doc-spec-pill { background: #fff1f2; color: #e11d48; border-color: #fecdd3; }

        .spec-neurologia .doc-avatar-circle { background: linear-gradient(135deg, #ede9fe 0%, #ddd6fe 100%); color: #6d28d9; border-color: #c4b5fd; }
        .spec-neurologia .doc-spec-pill { background: #f5f3ff; color: #7c3aed; border-color: #ddd6fe; }

        .spec-medicina-general .doc-avatar-circle { background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%); color: #047857; border-color: #6ee7b7; }
        .spec-medicina-general .doc-spec-pill { background: #ecfdf5; color: #059669; border-color: #a7f3d0; }

        .spec-traumatologia .doc-avatar-circle { background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%); color: #b45309; border-color: #fcd34d; }
        .spec-traumatologia .doc-spec-pill { background: #fffbeb; color: #d97706; border-color: #fde68a; }

        .spec-dermatologia .doc-avatar-circle { background: linear-gradient(135deg, #cffaff 0%, #a5f3fc 100%); color: #0e7490; border-color: #67e8f9; }
        .spec-dermatologia .doc-spec-pill { background: #ecfeff; color: #0891b2; border-color: #a5f3fc; }

        .spec-ginecologia .doc-avatar-circle { background: linear-gradient(135deg, #fce7f3 0%, #fbcfe8 100%); color: #be185d; border-color: #f472b6; }
        .spec-ginecologia .doc-spec-pill { background: #fdf2f8; color: #db2777; border-color: #fbcfe8; }

        /* DOCTORS GRID & CARDS (GRID MODE) */
        .doctors-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
            gap: 20px;
        }

        .doctor-card {
            background: #ffffff;
            border-radius: var(--radius-lg);
            border: 1px solid var(--border-light);
            padding: 22px;
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
            transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
            display: flex;
            flex-direction: column;
            justify-content: space-between;
        }

        .doctor-card:hover {
            transform: translateY(-4px);
            box-shadow: 0 12px 28px -6px rgba(37, 99, 235, 0.12);
            border-color: #cbd5e1;
        }

        .doc-card-header {
            display: flex;
            align-items: center;
            gap: 14px;
            margin-bottom: 16px;
        }

        .doc-avatar-circle {
            width: 48px;
            height: 48px;
            border-radius: 50%;
            background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
            border: 1.5px solid #bfdbfe;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
            color: var(--primary);
            font-size: 15px;
            flex-shrink: 0;
            box-shadow: 0 2px 6px rgba(37, 99, 235, 0.08);
        }

        .doc-header-info {
            display: flex;
            flex-direction: column;
            gap: 4px;
            min-width: 0;
        }

        .doc-card-name {
            font-size: 16px;
            font-weight: 700;
            color: var(--text-dark);
            line-height: 1.25;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .doc-spec-pill {
            display: inline-block;
            align-self: flex-start;
            background: #eff6ff;
            color: var(--primary);
            font-size: 12px;
            font-weight: 600;
            padding: 2px 9px;
            border-radius: 12px;
            border: 1px solid #dbeafe;
        }

        .doc-card-body {
            display: flex;
            flex-direction: column;
            gap: 14px;
            flex: 1;
            justify-content: space-between;
        }

        .doc-rating-row {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 13px;
        }

        .rating-num {
            font-weight: 700;
            color: var(--text-dark);
        }

        .rating-count {
            color: var(--text-gray);
            font-size: 12px;
        }

        .doc-info-list {
            display: flex;
            flex-direction: column;
            gap: 8px;
            padding-top: 12px;
            border-top: 1px dashed var(--border-light);
            margin-bottom: 4px;
        }

        .doc-info-item {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 12px;
            color: var(--text-gray);
        }

        .btn-reserve {
            width: 100%;
            padding: 11px 16px;
            border-radius: 24px;
            background: var(--primary);
            color: #ffffff;
            font-size: 14px;
            font-weight: 600;
            border: none;
            cursor: pointer;
            text-decoration: none;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
            box-shadow: 0 4px 12px rgba(37, 99, 235, 0.18);
        }

        .btn-reserve:hover {
            background: var(--primary-hover);
            transform: translateY(-1px);
            box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3);
            color: #ffffff;
        }

        /* LIST MODE (MODO LISTA HORIZONTAL ELEGANTE) */
        .doctors-grid.list-view-mode {
            display: flex;
            flex-direction: column;
            gap: 12px;
        }

        .doctors-grid.list-view-mode .doctor-card {
            flex-direction: row;
            align-items: center;
            justify-content: space-between;
            padding: 16px 24px;
            gap: 20px;
        }

        .doctors-grid.list-view-mode .doctor-card:hover {
            transform: translateX(4px);
        }

        .doctors-grid.list-view-mode .doc-card-header {
            margin-bottom: 0;
            width: 300px;
            flex-shrink: 0;
        }

        .doctors-grid.list-view-mode .doc-card-body {
            flex-direction: row;
            align-items: center;
            justify-content: space-between;
            gap: 24px;
            margin-bottom: 0;
            flex: 1;
        }

        .doctors-grid.list-view-mode .doc-info-list {
            flex-direction: row;
            align-items: center;
            gap: 24px;
            padding-top: 0;
            border-top: none;
            margin-bottom: 0;
        }

        .doctors-grid.list-view-mode .btn-reserve {
            width: auto;
            min-width: 150px;
            padding: 10px 20px;
        }

        @media(max-width:768px) {
            body { flex-direction: column; overflow-y: auto; }
            .sidebar { width:100%; }
            .main-content { padding:24px 16px; }
            .doctors-grid.list-view-mode .doctor-card { flex-direction: column; align-items: flex-start; }
            .doctors-grid.list-view-mode .doc-card-body { flex-direction: column; width: 100%; }
        }
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
            <a href="${pageContext.request.contextPath}/paciente/medicos" class="nav-item active">
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
                <h1 class="header-title">Directorio de Especialistas</h1>
                <p class="header-subtitle">Encuentra el médico de tu elección con tarifas estandarizadas por especialidad.</p>
            </div>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-bell <%= tieneNotifSinLeer ? "has-unread" : "" %>" title="Alertas">
                <i data-lucide="bell" style="width: 20px;"></i>
            </a>
        </div>

        <div class="pricing-banner">
            <div class="pricing-banner-text">
                <i data-lucide="tag" style="width: 16px; display: inline; vertical-align: middle; margin-right: 6px;"></i>
                <strong>Tarifas Transparentes por Especialidad:</strong> El costo de tu consulta se define por la especialidad seleccionada, garantizando igualdad de tarifas entre médicos.
            </div>
            <span class="pricing-badge">Sin recargos por médico</span>
        </div>

        <div class="toolbar-row">
            <div class="search-container" role="search">
                <i data-lucide="search" style="width: 18px;"></i>
                <input type="text" id="medicoFilterInput" placeholder="Buscar médico por nombre o especialidad..." aria-label="Buscar médico por nombre o especialidad">
            </div>

            <div class="view-switcher-toggle" role="toolbar" aria-label="Modo de Vista">
                <button type="button" class="view-btn active" id="btnViewGrid" onclick="setViewMode('grid')" title="Vista en Mosaico">
                    <i data-lucide="layout-grid" style="width: 18px; height: 18px;"></i>
                </button>
                <button type="button" class="view-btn" id="btnViewList" onclick="setViewMode('list')" title="Vista en Lista">
                    <i data-lucide="list" style="width: 18px; height: 18px;"></i>
                </button>
            </div>
        </div>

        <div class="cat-pills" role="tablist">
            <button class="cat-btn active" onclick="filterCat(this, 'ALL')">Todas las Especialidades</button>
            <button class="cat-btn" onclick="filterCat(this, 'Cardiología')">Cardiología (S/ 80.00)</button>
            <button class="cat-btn" onclick="filterCat(this, 'Neurología')">Neurología (S/ 85.00)</button>
            <button class="cat-btn" onclick="filterCat(this, 'Medicina General')">Medicina General (S/ 60.00)</button>
            <button class="cat-btn" onclick="filterCat(this, 'Traumatología')">Traumatología (S/ 80.00)</button>
            <button class="cat-btn" onclick="filterCat(this, 'Dermatología')">Dermatología (S/ 75.00)</button>
            <button class="cat-btn" onclick="filterCat(this, 'Ginecología')">Ginecología (S/ 80.00)</button>
        </div>

        <div class="doctors-grid" id="medicosCardsContainer">
            <% if (listaMedicos != null && !listaMedicos.isEmpty()) {
                for (Map<String, Object> m : listaMedicos) {
                    int idMed = (Integer) m.get("idMedico");
                    int idEsp = m.get("idEspecialidad") != null ? (Integer) m.get("idEspecialidad") : 1;
                    String rawNombre = (String) m.get("nombre");
                    String cleanNombre = rawNombre != null ? rawNombre.replaceAll("(?i)\\b(Dr\\.|Dra\\.|Dr|Dra)\\b|[^a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]", "").replaceAll("\\s+", " ").trim() : "";
                    String displayNombre = "Dr. " + cleanNombre;
                    String especialidad = (String) m.get("especialidad");
                    String sede = (String) m.get("sede");
                    String rating = (String) m.get("rating");
                    String exp = (String) m.get("experiencia");
                    String inits = "DR";
                    if (!cleanNombre.isEmpty()) {
                        String[] p = cleanNombre.split("\\s+");
                        inits = p.length >= 2 ? p[0].substring(0,1) + p[1].substring(0,1) : cleanNombre.substring(0, Math.min(2, cleanNombre.length()));
                    }

                    String specClass = "spec-medicina-general";
                    if (especialidad != null) {
                        String lower = especialidad.toLowerCase();
                        if (lower.contains("cardio")) specClass = "spec-cardiologia";
                        else if (lower.contains("neuro")) specClass = "spec-neurologia";
                        else if (lower.contains("general")) specClass = "spec-medicina-general";
                        else if (lower.contains("trauma")) specClass = "spec-traumatologia";
                        else if (lower.contains("derma")) specClass = "spec-dermatologia";
                        else if (lower.contains("gineco")) specClass = "spec-ginecologia";
                    }
            %>
            <div class="doctor-card <%= specClass %>" data-spec="<%= especialidad %>">
                <div class="doc-card-header">
                    <div class="doc-avatar-circle"><%= inits.toUpperCase() %></div>
                    <div class="doc-header-info">
                        <div class="doc-card-name"><%= displayNombre %></div>
                        <span class="doc-spec-pill"><%= especialidad %></span>
                    </div>
                </div>
                <div class="doc-card-body">
                    <div class="doc-rating-row">
                        <i data-lucide="star" style="width: 14px; height: 14px; fill: #f59e0b; color: #f59e0b;"></i>
                        <span class="rating-num"><%= rating %></span>
                        <span class="rating-count">(150 opiniones)</span>
                    </div>

                    <div class="doc-info-list">
                        <div class="doc-info-item">
                            <i data-lucide="map-pin" style="width: 14px; height: 14px;"></i>
                            <span>Clínica Internacional — <%= sede %></span>
                        </div>
                        <div class="doc-info-item">
                            <i data-lucide="award" style="width: 14px; height: 14px;"></i>
                            <span><%= exp %></span>
                        </div>
                    </div>

                    <a href="${pageContext.request.contextPath}/paciente/citas?action=nueva&medicoId=<%= idMed %>&especialidadId=<%= idEsp %>" class="btn-reserve">
                        <i data-lucide="calendar-plus" style="width: 16px; height: 16px;"></i>
                        Reservar cita
                    </a>
                </div>
            </div>
            <%  }
            } else { %>
            <div style="grid-column: 1 / -1; text-align: center; padding: 48px; color: var(--text-gray);">
                <i data-lucide="user-x" style="width: 40px; height: 40px; stroke-width: 1.5; margin-bottom: 8px;"></i>
                <div style="font-size: 16px; font-weight: 600; color: var(--text-dark);">No hay médicos disponibles en este momento</div>
            </div>
            <% } %>
        </div>
    </main>

    <script>
        lucide.createIcons();

        function setViewMode(mode) {
            const container = document.getElementById('medicosCardsContainer');
            const btnGrid = document.getElementById('btnViewGrid');
            const btnList = document.getElementById('btnViewList');

            if (mode === 'list') {
                container.classList.add('list-view-mode');
                btnList.classList.add('active');
                btnGrid.classList.remove('active');
                localStorage.setItem('medicosViewMode', 'list');
            } else {
                container.classList.remove('list-view-mode');
                btnGrid.classList.add('active');
                btnList.classList.remove('active');
                localStorage.setItem('medicosViewMode', 'grid');
            }
        }

        document.addEventListener('DOMContentLoaded', function() {
            const savedMode = localStorage.getItem('medicosViewMode');
            if (savedMode === 'list') {
                setViewMode('list');
            }
        });

        function filterCat(btn, cat) {
            document.querySelectorAll('.cat-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            const cards = document.querySelectorAll('#medicosCardsContainer .doctor-card');
            cards.forEach(card => {
                if (cat === 'ALL' || card.getAttribute('data-spec') === cat) {
                    card.style.display = containerIsList() ? 'flex' : 'flex';
                } else {
                    card.style.display = 'none';
                }
            });
        }

        function containerIsList() {
            return document.getElementById('medicosCardsContainer').classList.contains('list-view-mode');
        }

        document.getElementById('medicoFilterInput').addEventListener('input', function(e) {
            const term = e.target.value.toLowerCase();
            const cards = document.querySelectorAll('#medicosCardsContainer .doctor-card');
            cards.forEach(card => {
                const text = card.textContent.toLowerCase();
                card.style.display = text.includes(term) ? 'flex' : 'none';
            });
        });
    </script>
</body>
</html>

