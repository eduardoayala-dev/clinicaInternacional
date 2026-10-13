<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreAdmin = (String) request.getAttribute("nombreAdmin");
    if (nombreAdmin == null && usuario != null) {
        nombreAdmin = usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreAdmin == null || nombreAdmin.trim().isEmpty()) {
        nombreAdmin = "Jorge Vargas";
    }

    String inicialesAdmin = "JV";
    if (nombreAdmin != null && !nombreAdmin.trim().isEmpty()) {
        String[] partes = nombreAdmin.trim().split("\\s+");
        if (partes.length >= 2) {
            inicialesAdmin = ("" + partes[0].charAt(0) + partes[1].charAt(0)).toUpperCase();
        } else if (partes.length == 1 && partes[0].length() > 0) {
            inicialesAdmin = ("" + partes[0].charAt(0)).toUpperCase();
        }
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Catálogo de sedes - Portal del Administrador | Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:wght@600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/sysadmin-layout.css">
    <style>
        /* ======================================================== */
        /* ESTILOS ESPECÍFICOS DE CATÁLOGO DE SEDES                */
        /* ======================================================== */
        .page-top-bar {
            display: flex;
            align-items: flex-start;
            justify-content: space-between;
            gap: 20px;
            margin-bottom: 24px;
            flex-wrap: wrap;
        }

        .page-titles {
            display: flex;
            flex-direction: column;
        }

        .page-main-title {
            font-family: 'Playfair Display', Georgia, serif;
            font-size: 30px;
            font-weight: 700;
            color: var(--text-dark);
            line-height: 1.15;
            letter-spacing: -0.5px;
        }

        .page-main-subtitle {
            font-size: 14px;
            color: var(--text-gray);
            margin-top: 5px;
            font-weight: 400;
        }

        .top-action-buttons {
            display: flex;
            align-items: center;
            gap: 10px;
            flex-wrap: wrap;
        }

        .btn-action-light {
            background-color: #ffffff;
            border: 1px solid #d1d5db;
            border-radius: 10px;
            padding: 9px 16px;
            font-size: 13px;
            font-weight: 500;
            color: var(--text-dark);
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            transition: all 0.15s ease;
            box-shadow: 0 1px 2px rgba(0,0,0,0.03);
            text-decoration: none;
        }

        .btn-action-light i {
            width: 16px;
            height: 16px;
            color: var(--primary);
            stroke-width: 2.2;
        }

        .btn-action-light:hover {
            border-color: #94a3b8;
            background-color: #f8fafc;
        }

        /* ALERTAS FLASH */
        .flash-notification {
            padding: 12px 18px;
            border-radius: 10px;
            font-size: 13.5px;
            font-weight: 500;
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 20px;
        }
        .flash-success { background-color: #ecfdf5; border: 1px solid #a7f3d0; color: #065f46; }
        .flash-error { background-color: #fef2f2; border: 1px solid #fecaca; color: #991b1b; }

        /* GRID DE 4 MÉTRICAS */
        .metrics-grid-row {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 18px;
            margin-bottom: 24px;
        }

        .stat-card {
            background-color: var(--bg-card);
            border-radius: var(--radius-lg);
            border: 1px solid var(--border-light);
            padding: 22px 24px;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
        }

        .stat-card-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 12px;
        }

        .stat-title {
            font-size: 13.5px;
            font-weight: 500;
            color: var(--text-gray);
        }

        .stat-badge-icon {
            width: 32px;
            height: 32px;
            border-radius: 8px;
            background-color: #f0f7ff;
            color: var(--primary);
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .stat-badge-icon i {
            width: 17px;
            height: 17px;
            stroke-width: 2.2;
        }

        .stat-badge-chevron {
            border-radius: 50%;
        }

        .stat-number {
            font-size: 28px;
            font-weight: 700;
            color: var(--text-dark);
            line-height: 1.1;
            margin-bottom: 6px;
        }

        .stat-subtext {
            font-size: 13px;
            color: var(--text-gray);
            font-weight: 400;
        }

        /* TARJETA DE FILTROS */
        .filters-container-card {
            background-color: var(--bg-card);
            border-radius: var(--radius-lg);
            border: 1px solid var(--border-light);
            padding: 18px 24px;
            display: flex;
            align-items: flex-end;
            gap: 20px;
            margin-bottom: 24px;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
        }

        .filter-field-group {
            display: flex;
            flex-direction: column;
            gap: 8px;
        }

        .filter-field-label {
            font-size: 13px;
            font-weight: 600;
            color: var(--text-dark);
        }

        .search-pill-box {
            position: relative;
            width: 440px;
        }

        .search-pill-input {
            width: 100%;
            padding: 10px 42px 10px 18px;
            border-radius: 24px;
            border: 1px solid #cbd5e1;
            font-size: 13.5px;
            color: var(--text-dark);
            background-color: #ffffff;
            outline: none;
            transition: all 0.2s;
        }

        .search-pill-input::placeholder {
            color: #94a3b8;
        }

        .search-pill-input:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.12);
        }

        .search-right-icon {
            position: absolute;
            right: 14px;
            top: 50%;
            transform: translateY(-50%);
            width: 17px;
            height: 17px;
            color: #94a3b8;
            pointer-events: none;
        }

        .select-pill-box {
            position: relative;
            min-width: 280px;
        }

        .select-pill-control {
            width: 100%;
            padding: 10px 38px 10px 18px;
            border-radius: 24px;
            border: 1px solid #cbd5e1;
            font-size: 13.5px;
            color: var(--text-dark);
            background-color: #ffffff;
            appearance: none;
            -webkit-appearance: none;
            cursor: pointer;
            outline: none;
            transition: all 0.2s;
        }

        .select-pill-control:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.12);
        }

        .select-right-arrow {
            position: absolute;
            right: 14px;
            top: 50%;
            transform: translateY(-50%);
            width: 16px;
            height: 16px;
            color: #94a3b8;
            pointer-events: none;
        }

        .btn-pill-primary {
            margin-left: auto;
            background-color: var(--primary);
            color: #ffffff;
            border: none;
            border-radius: 24px;
            padding: 11px 24px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            transition: all 0.15s ease;
            box-shadow: 0 2px 8px rgba(2, 132, 199, 0.25);
            text-decoration: none;
            white-space: nowrap;
        }

        .btn-pill-primary i {
            width: 18px;
            height: 18px;
            stroke-width: 2.4;
        }

        .btn-pill-primary:hover {
            background-color: var(--primary-hover);
        }

        /* TABLA DE SEDES */
        .table-data-card {
            background-color: var(--bg-card);
            border-radius: var(--radius-lg);
            border: 1px solid var(--border-light);
            padding: 12px 24px 20px 24px;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
            overflow: hidden;
        }

        .catalogo-table {
            width: 100%;
            border-collapse: collapse;
            text-align: left;
        }

        .catalogo-table thead th {
            padding: 18px 16px 14px 16px;
            font-size: 11px;
            font-weight: 700;
            color: var(--text-gray);
            text-transform: uppercase;
            letter-spacing: 0.5px;
            border-bottom: 1px solid #f1f5f9;
        }

        .catalogo-table tbody td {
            padding: 18px 16px;
            vertical-align: middle;
            border-bottom: 1px solid #f8fafc;
            font-size: 13.5px;
        }

        .catalogo-table tbody tr:last-child td {
            border-bottom: none;
        }

        .catalogo-table tbody tr:hover {
            background-color: #fafcff;
        }

        .cell-main-title {
            font-weight: 600;
            color: var(--text-dark);
            font-size: 14px;
            line-height: 1.3;
        }

        .cell-sub-detail {
            font-size: 12.5px;
            color: var(--text-gray);
            margin-top: 2px;
            font-weight: 400;
        }

        .cell-number {
            font-size: 14px;
            font-weight: 500;
            color: var(--text-dark);
        }

        .badge-especialidades-pill {
            background-color: var(--primary-pill);
            color: var(--primary);
            font-size: 12.5px;
            font-weight: 600;
            padding: 4px 14px;
            border-radius: 20px;
            display: inline-block;
            white-space: nowrap;
        }

        .badge-status-pill {
            padding: 4px 14px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 600;
            display: inline-block;
            white-space: nowrap;
        }

        .badge-status-activa {
            background-color: #dcfce7;
            color: #16a34a;
        }

        .badge-status-mantenimiento {
            background-color: #fef3c7;
            color: #d97706;
        }

        .badge-status-inactiva {
            background-color: #f1f5f9;
            color: #64748b;
        }

        .btn-link-editar {
            background: none;
            border: none;
            color: var(--primary);
            font-weight: 600;
            font-size: 13.5px;
            cursor: pointer;
            text-decoration: none;
            padding: 4px 6px;
            border-radius: 6px;
            transition: all 0.15s;
        }

        .btn-link-editar:hover {
            text-decoration: underline;
            color: var(--primary-hover);
        }

        .table-records-summary {
            margin-top: 18px;
            font-size: 13px;
            color: var(--text-gray);
            font-weight: 400;
            padding-left: 2px;
        }

        /* MODALES */
        .modal-backdrop {
            display: none;
            position: fixed;
            top: 0; left: 0; right: 0; bottom: 0;
            background: rgba(15, 23, 42, 0.45);
            backdrop-filter: blur(4px);
            z-index: 100;
            align-items: center;
            justify-content: center;
        }

        .modal-backdrop.active {
            display: flex;
        }

        .modal-box {
            background: #ffffff;
            width: 100%;
            max-width: 520px;
            border-radius: 16px;
            box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
            overflow: hidden;
            animation: modalFadeIn 0.2s ease-out;
        }

        @keyframes modalFadeIn {
            from { opacity: 0; transform: scale(0.97); }
            to { opacity: 1; transform: scale(1); }
        }

        .modal-box-header {
            padding: 20px 24px;
            border-bottom: 1px solid var(--border-light);
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .modal-box-title {
            font-size: 18px;
            font-weight: 700;
            color: var(--text-dark);
        }

        .btn-modal-close {
            background: none;
            border: none;
            cursor: pointer;
            color: var(--text-gray);
            padding: 4px;
            border-radius: 6px;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .btn-modal-close:hover {
            color: var(--text-dark);
            background: #f1f5f9;
        }

        .modal-box-body {
            padding: 24px;
            display: flex;
            flex-direction: column;
            gap: 16px;
        }

        .modal-input-group {
            display: flex;
            flex-direction: column;
            gap: 6px;
        }

        .modal-input-label {
            font-size: 13px;
            font-weight: 600;
            color: var(--text-dark);
        }

        .modal-control {
            width: 100%;
            padding: 10px 14px;
            border: 1px solid #cbd5e1;
            border-radius: 10px;
            font-size: 14px;
            color: var(--text-dark);
            outline: none;
            transition: border-color 0.2s;
        }

        .modal-control:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.12);
        }

        .modal-box-footer {
            padding: 16px 24px;
            border-top: 1px solid var(--border-light);
            background: #f8fafc;
            display: flex;
            justify-content: flex-end;
            gap: 12px;
        }

        .btn-modal-cancel {
            padding: 10px 18px;
            border-radius: 10px;
            border: 1px solid #cbd5e1;
            background: #ffffff;
            color: var(--text-gray);
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
        }

        .btn-modal-cancel:hover {
            background: #f1f5f9;
        }

        .btn-modal-submit {
            padding: 10px 20px;
            border-radius: 10px;
            border: none;
            background: var(--primary);
            color: #ffffff;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            box-shadow: 0 2px 8px rgba(2, 132, 199, 0.25);
        }

        .btn-modal-submit:hover {
            background: var(--primary-hover);
        }

        @media (max-width: 1200px) {
            .metrics-grid-row { grid-template-columns: repeat(2, 1fr); }
            .search-pill-box { width: 320px; }
        }

        @media (max-width: 900px) {
            .metrics-grid-row { grid-template-columns: 1fr; }
            .filters-container-card { flex-direction: column; align-items: stretch; }
            .search-pill-box { width: 100%; }
            .btn-pill-primary { margin-left: 0; }
        }
    </style>
</head>
<body>

    <!-- ======================================================== -->
    <!-- PANEL DE LA IZQUIERDA (SIDEBAR INSTITUCIONAL - 260px)   -->
    <!-- ======================================================== -->
    <aside class="sidebar">
        <a href="${pageContext.request.contextPath}/sysadmin/dashboard" class="brand-container">
            <img src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png" 
                 alt="Logo" class="brand-logo-img"
                 onerror="this.src='${pageContext.request.contextPath}/assets/images/logoClinical.png'">
            <div class="brand-text">
                <span class="brand-title">Clínica Internacional</span>
                <span class="brand-subtitle">Portal del administrador</span>
            </div>
        </a>

        <nav class="nav-menu">
            <a href="${pageContext.request.contextPath}/sysadmin/dashboard" class="nav-item">
                <i data-lucide="home"></i>
                <span>Inicio</span>
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/usuarios" class="nav-item">
                <i data-lucide="shield"></i>
                <span>Usuarios y roles</span>
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/sedes" class="nav-item active">
                <i data-lucide="building"></i>
                <span>Sedes</span>
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/priorizacion" class="nav-item">
                <i data-lucide="sliders"></i>
                <span>Configuración de priorización</span>
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/analitica" class="nav-item">
                <i data-lucide="trending-up"></i>
                <span>Indicadores y analítica</span>
            </a>
        </nav>

        <div class="user-profile">
            <div class="avatar"><%= inicialesAdmin %></div>
            <div class="user-info">
                <span class="user-name"><%= nombreAdmin %></span>
                <span class="user-role">Administrador</span>
            </div>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout" title="Cerrar sesión">
                <i data-lucide="log-out"></i>
            </a>
        </div>
    </aside>

    <!-- ======================================================== -->
    <!-- ÁREA PRINCIPAL CON HEADER SUPERIOR DISTINGUIDO          -->
    <!-- ======================================================== -->
    <main class="main-content">
        <!-- BARRA SUPERIOR (HEADER BLANCO DISTINGUIDO DEL CONTENIDO) -->
        <header class="dash-top-header">
            <nav class="breadcrumb-trail">
                <span>Administrador</span> <span class="bc-sep">/</span> <strong>Sedes</strong>
            </nav>
        </header>

        <!-- CONTENEDOR CON SCROLL DE CONTENIDO -->
        <div class="dash-scroll-container">
            <!-- TÍTULO Y BOTONES DE EXPORTACIÓN -->
            <div class="page-top-bar">
                <div class="page-titles">
                    <h1 class="page-main-title">Catálogo de sedes</h1>
                    <p class="page-main-subtitle">Sedes de la red, su capacidad y especialidades</p>
                </div>
                <div class="top-action-buttons">
                    <button type="button" class="btn-action-light" onclick="simularImportarExcel()">
                        <i data-lucide="arrow-up-to-line"></i>
                        <span>Importar desde Excel</span>
                    </button>
                    <button type="button" class="btn-action-light" onclick="descargarPlantilla()">
                        <i data-lucide="download"></i>
                        <span>Descargar plantilla</span>
                    </button>
                    <button type="button" class="btn-action-light" onclick="exportarExcel()">
                        <i data-lucide="download"></i>
                        <span>Exportar Excel</span>
                    </button>
                    <button type="button" class="btn-action-light" onclick="exportarCSV()">
                        <i data-lucide="download"></i>
                        <span>Exportar CSV</span>
                    </button>
                </div>
            </div>

            <!-- MENSAJES FLASH -->
            <c:if test="${not empty sessionScope.flashMessage}">
                <div class="flash-notification flash-success">
                    <i data-lucide="check-circle" style="width: 18px; height: 18px;"></i>
                    <span><c:out value="${sessionScope.flashMessage}" /></span>
                </div>
                <c:remove var="flashMessage" scope="session" />
            </c:if>

            <c:if test="${not empty sessionScope.flashError}">
                <div class="flash-notification flash-error">
                    <i data-lucide="alert-triangle" style="width: 18px; height: 18px;"></i>
                    <span><c:out value="${sessionScope.flashError}" /></span>
                </div>
                <c:remove var="flashError" scope="session" />
            </c:if>

            <!-- 4 TARJETAS DE MÉTRICAS -->
            <div class="metrics-grid-row">
                <div class="stat-card">
                    <div class="stat-card-header">
                        <span class="stat-title">Red de sedes</span>
                        <div class="stat-badge-icon">
                            <i data-lucide="building"></i>
                        </div>
                    </div>
                    <div class="stat-number"><c:out value="${metricasSedes.totalSedes != null ? metricasSedes.totalSedes : '4'}" /> sedes</div>
                    <div class="stat-subtext"><c:out value="${metricasSedes.activas != null ? metricasSedes.activas : '3'}" /> activas</div>
                </div>

                <div class="stat-card">
                    <div class="stat-card-header">
                        <span class="stat-title">Cartera médica global</span>
                        <div class="stat-badge-icon">
                            <i data-lucide="stethoscope"></i>
                        </div>
                    </div>
                    <div class="stat-number"><c:out value="${metricasSedes.totalEspecialidades != null ? metricasSedes.totalEspecialidades : '18'}" /></div>
                    <div class="stat-subtext">Especialidades en la red</div>
                </div>

                <div class="stat-card">
                    <div class="stat-card-header">
                        <span class="stat-title">Ocupación de agenda</span>
                        <div class="stat-badge-icon">
                            <i data-lucide="calendar"></i>
                        </div>
                    </div>
                    <div class="stat-number"><c:out value="${metricasSedes.ocupacionAgenda != null ? metricasSedes.ocupacionAgenda : '80 %'}" /></div>
                    <div class="stat-subtext">Promedio de las sedes activas</div>
                </div>

                <div class="stat-card">
                    <div class="stat-card-header">
                        <span class="stat-title">Sedes en mantenimiento</span>
                        <div class="stat-badge-icon stat-badge-chevron">
                            <i data-lucide="chevron-right"></i>
                        </div>
                    </div>
                    <div class="stat-number"><c:out value="${metricasSedes.mantenimiento != null ? metricasSedes.mantenimiento : '1'}" /> sede</div>
                    <div class="stat-subtext"><c:out value="${metricasSedes.sedeMantenimientoNombre != null ? metricasSedes.sedeMantenimientoNombre : 'Sede Surco'}" /></div>
                </div>
            </div>

            <!-- TARJETA DE FILTROS -->
            <div class="filters-container-card">
                <div class="filter-field-group">
                    <label class="filter-field-label" for="inputBuscarSede">Buscar</label>
                    <div class="search-pill-box">
                        <input type="text" id="inputBuscarSede" 
                               class="search-pill-input" 
                               placeholder="Nombre, código o distrito" 
                               onkeyup="filtrarTablaSedes()">
                        <i data-lucide="search" class="search-right-icon"></i>
                    </div>
                </div>

                <div class="filter-field-group">
                    <label class="filter-field-label" for="selectEstadoSede">Estado</label>
                    <div class="select-pill-box">
                        <select id="selectEstadoSede" class="select-pill-control" onchange="filtrarTablaSedes()">
                            <option value="TODOS">Todos (activas, en mantenimiento)</option>
                            <option value="ACTIVO">Activa</option>
                            <option value="MANTENIMIENTO">En mantenimiento</option>
                        </select>
                        <i data-lucide="chevron-down" class="select-right-arrow"></i>
                    </div>
                </div>

                <button type="button" class="btn-pill-primary" onclick="abrirModalNuevaSede()">
                    <i data-lucide="plus"></i>
                    <span>Nueva sede</span>
                </button>
            </div>

            <!-- TABLA PRINCIPAL DE SEDES -->
            <div class="table-data-card">
                <table class="catalogo-table" id="tablaSedes">
                    <thead>
                        <tr>
                            <th style="width: 20%;">NOMBRE DE LA SEDE</th>
                            <th style="width: 30%;">DIRECCIÓN / ZONA</th>
                            <th style="width: 14%;">CONSULTORIOS</th>
                            <th style="width: 16%;">ESPECIALIDADES</th>
                            <th style="width: 12%;">ESTADO</th>
                            <th style="width: 8%; text-align: right;">ACCIONES</th>
                        </tr>
                    </thead>
                    <tbody id="tbodySedes">
                        <c:choose>
                            <c:when test="${not empty listaSedes}">
                                <c:forEach var="s" items="${listaSedes}">
                                    <tr class="fila-sede" 
                                        data-search="${s.codigo} ${s.nombre} ${s.direccion} ${s.direccionPrincipal} ${s.zonaDistrito}"
                                        data-estado="${s.estado}">
                                        <td>
                                            <div class="cell-main-title"><c:out value="${s.nombre}" /></div>
                                            <div class="cell-sub-detail"><c:out value="${s.codigo}" /></div>
                                        </td>
                                        <td>
                                            <div class="cell-main-title"><c:out value="${s.direccionPrincipal}" /></div>
                                            <div class="cell-sub-detail"><c:out value="${s.zonaDistrito}" /></div>
                                        </td>
                                        <td>
                                            <span class="cell-number"><c:out value="${s.consultorios}" /></span>
                                        </td>
                                        <td>
                                            <span class="badge-especialidades-pill"><c:out value="${s.especialidadesCount}" /> especialidades</span>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${s.estado == 'ACTIVO'}">
                                                    <span class="badge-status-pill badge-status-activa">Activa</span>
                                                </c:when>
                                                <c:when test="${s.estado == 'MANTENIMIENTO'}">
                                                    <span class="badge-status-pill badge-status-mantenimiento">En mantenimiento</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge-status-pill badge-status-inactiva"><c:out value="${s.estadoEtiqueta}" /></span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td style="text-align: right;">
                                            <button type="button" class="btn-link-editar" 
                                                    onclick="abrirModalEditarSede('${s.idSede}', '${s.codigo}', '${s.nombre}', '${s.direccion}', '${s.telefono}', '${s.estado}')">
                                                Editar
                                            </button>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <tr class="fila-sede" data-search="SB-01 Sede San Borja Av. Guardia Civil 337 San Borja" data-estado="ACTIVO">
                                    <td>
                                        <div class="cell-main-title">Sede San Borja</div>
                                        <div class="cell-sub-detail">SB-01</div>
                                    </td>
                                    <td>
                                        <div class="cell-main-title">Av. Guardia Civil 337</div>
                                        <div class="cell-sub-detail">San Borja</div>
                                    </td>
                                    <td><span class="cell-number">24</span></td>
                                    <td><span class="badge-especialidades-pill">16 especialidades</span></td>
                                    <td><span class="badge-status-pill badge-status-activa">Activa</span></td>
                                    <td style="text-align: right;"><button type="button" class="btn-link-editar" onclick="abrirModalEditarSede('1', 'SB-01', 'Sede San Borja', 'Av. Guardia Civil 337, San Borja', '01-610-2222', 'ACTIVO')">Editar</button></td>
                                </tr>
                                <tr class="fila-sede" data-search="LI-02 Sede Lima Av. Garcilaso de la Vega 1420 Cercado de Lima" data-estado="ACTIVO">
                                    <td>
                                        <div class="cell-main-title">Sede Lima</div>
                                        <div class="cell-sub-detail">LI-02</div>
                                    </td>
                                    <td>
                                        <div class="cell-main-title">Av. Garcilaso de la Vega 1420</div>
                                        <div class="cell-sub-detail">Cercado de Lima</div>
                                    </td>
                                    <td><span class="cell-number">18</span></td>
                                    <td><span class="badge-especialidades-pill">14 especialidades</span></td>
                                    <td><span class="badge-status-pill badge-status-activa">Activa</span></td>
                                    <td style="text-align: right;"><button type="button" class="btn-link-editar" onclick="abrirModalEditarSede('2', 'LI-02', 'Sede Lima', 'Av. Garcilaso de la Vega 1420, Cercado de Lima', '01-610-1111', 'ACTIVO')">Editar</button></td>
                                </tr>
                                <tr class="fila-sede" data-search="LM-03 Sede La Molina Av. La Molina 1100 La Molina" data-estado="ACTIVO">
                                    <td>
                                        <div class="cell-main-title">Sede La Molina</div>
                                        <div class="cell-sub-detail">LM-03</div>
                                    </td>
                                    <td>
                                        <div class="cell-main-title">Av. La Molina 1100</div>
                                        <div class="cell-sub-detail">La Molina</div>
                                    </td>
                                    <td><span class="cell-number">12</span></td>
                                    <td><span class="badge-especialidades-pill">9 especialidades</span></td>
                                    <td><span class="badge-status-pill badge-status-activa">Activa</span></td>
                                    <td style="text-align: right;"><button type="button" class="btn-link-editar" onclick="abrirModalEditarSede('3', 'LM-03', 'Sede La Molina', 'Av. La Molina 1100, La Molina', '01-610-5555', 'ACTIVO')">Editar</button></td>
                                </tr>
                                <tr class="fila-sede" data-search="SU-04 Sede Surco Av. Primavera 1750 Santiago de Surco" data-estado="MANTENIMIENTO">
                                    <td>
                                        <div class="cell-main-title">Sede Surco</div>
                                        <div class="cell-sub-detail">SU-04</div>
                                    </td>
                                    <td>
                                        <div class="cell-main-title">Av. Primavera 1750</div>
                                        <div class="cell-sub-detail">Santiago de Surco</div>
                                    </td>
                                    <td><span class="cell-number">10</span></td>
                                    <td><span class="badge-especialidades-pill">8 especialidades</span></td>
                                    <td><span class="badge-status-pill badge-status-mantenimiento">En mantenimiento</span></td>
                                    <td style="text-align: right;"><button type="button" class="btn-link-editar" onclick="abrirModalEditarSede('4', 'SU-04', 'Sede Surco', 'Av. Primavera 1750, Santiago de Surco', '01-610-4444', 'MANTENIMIENTO')">Editar</button></td>
                                </tr>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>

                <div class="table-records-summary">
                    Total de registros: <span id="lblTotalRegistros"><c:out value="${listaSedes != null ? listaSedes.size() : '4'}" /></span>
                </div>
            </div>
        </div>
    </main>

    <!-- MODAL 1: REGISTRAR NUEVA SEDE -->
    <div class="modal-backdrop" id="modalNuevaSede">
        <div class="modal-box">
            <div class="modal-box-header">
                <h3 class="modal-box-title">Registrar Nueva Sede Clínica</h3>
                <button type="button" class="btn-modal-close" onclick="cerrarModalNuevaSede()">
                    <i data-lucide="x" style="width: 20px; height: 20px;"></i>
                </button>
            </div>
            <form action="${pageContext.request.contextPath}/sysadmin/sedes" method="POST">
                <input type="hidden" name="action" value="crearSede">
                <div class="modal-box-body">
                    <div class="modal-input-group">
                        <label class="modal-input-label">Código de Sede *</label>
                        <input type="text" name="codigo" required placeholder="Ej: SB-01 / SED-SURCO" class="modal-control" style="text-transform: uppercase;">
                    </div>
                    <div class="modal-input-group">
                        <label class="modal-input-label">Nombre de la Sede *</label>
                        <input type="text" name="nombre" required placeholder="Ej: Sede San Borja" class="modal-control">
                    </div>
                    <div class="modal-input-group">
                        <label class="modal-input-label">Dirección Completa (Dirección, Distrito) *</label>
                        <input type="text" name="direccion" required placeholder="Ej: Av. Guardia Civil 337, San Borja" class="modal-control">
                    </div>
                    <div class="modal-input-group">
                        <label class="modal-input-label">Teléfono / Central *</label>
                        <input type="text" name="telefono" required placeholder="Ej: 01-610-2222" class="modal-control">
                    </div>
                    <div class="modal-input-group">
                        <label class="modal-input-label">Estado Inicial *</label>
                        <select name="estado" class="modal-control">
                            <option value="ACTIVO" selected>Activa (Habilitada en red)</option>
                            <option value="MANTENIMIENTO">En mantenimiento (Temporal)</option>
                            <option value="INACTIVO">Inactiva (Deshabilitada)</option>
                        </select>
                    </div>
                </div>
                <div class="modal-box-footer">
                    <button type="button" class="btn-modal-cancel" onclick="cerrarModalNuevaSede()">Cancelar</button>
                    <button type="submit" class="btn-modal-submit">Guardar Sede</button>
                </div>
            </form>
        </div>
    </div>

    <!-- MODAL 2: EDITAR SEDE -->
    <div class="modal-backdrop" id="modalEditarSede">
        <div class="modal-box">
            <div class="modal-box-header">
                <h3 class="modal-box-title">Editar Sede Clínica</h3>
                <button type="button" class="btn-modal-close" onclick="cerrarModalEditarSede()">
                    <i data-lucide="x" style="width: 20px; height: 20px;"></i>
                </button>
            </div>
            <form action="${pageContext.request.contextPath}/sysadmin/sedes" method="POST">
                <input type="hidden" name="action" value="editarSede">
                <input type="hidden" name="idSede" id="editIdSede">
                <div class="modal-box-body">
                    <div class="modal-input-group">
                        <label class="modal-input-label">Código de Sede *</label>
                        <input type="text" name="codigo" id="editCodigo" required class="modal-control" style="text-transform: uppercase;">
                    </div>
                    <div class="modal-input-group">
                        <label class="modal-input-label">Nombre de la Sede *</label>
                        <input type="text" name="nombre" id="editNombre" required class="modal-control">
                    </div>
                    <div class="modal-input-group">
                        <label class="modal-input-label">Dirección Completa *</label>
                        <input type="text" name="direccion" id="editDireccion" required class="modal-control">
                    </div>
                    <div class="modal-input-group">
                        <label class="modal-input-label">Teléfono / Central *</label>
                        <input type="text" name="telefono" id="editTelefono" required class="modal-control">
                    </div>
                    <div class="modal-input-group">
                        <label class="modal-input-label">Estado Operativo *</label>
                        <select name="estado" id="editEstado" class="modal-control">
                            <option value="ACTIVO">Activa (Habilitada en red)</option>
                            <option value="MANTENIMIENTO">En mantenimiento (Temporal)</option>
                            <option value="INACTIVO">Inactiva (Deshabilitada)</option>
                        </select>
                    </div>
                </div>
                <div class="modal-box-footer">
                    <button type="button" class="btn-modal-cancel" onclick="cerrarModalEditarSede()">Cancelar</button>
                    <button type="submit" class="btn-modal-submit">Actualizar Sede</button>
                </div>
            </form>
        </div>
    </div>

    <!-- SCRIPTS -->
    <script>
        lucide.createIcons();

        function abrirModalNuevaSede() {
            document.getElementById('modalNuevaSede').classList.add('active');
        }
        function cerrarModalNuevaSede() {
            document.getElementById('modalNuevaSede').classList.remove('active');
        }

        function abrirModalEditarSede(id, codigo, nombre, direccion, telefono, estado) {
            document.getElementById('editIdSede').value = id;
            document.getElementById('editCodigo').value = codigo;
            document.getElementById('editNombre').value = nombre;
            document.getElementById('editDireccion').value = direccion;
            document.getElementById('editTelefono').value = telefono;
            document.getElementById('editEstado').value = estado || 'ACTIVO';
            document.getElementById('modalEditarSede').classList.add('active');
        }
        function cerrarModalEditarSede() {
            document.getElementById('modalEditarSede').classList.remove('active');
        }

        function filtrarTablaSedes() {
            var filtroTexto = (document.getElementById('inputBuscarSede').value || '').toLowerCase().trim();
            var filtroEstado = (document.getElementById('selectEstadoSede').value || 'TODOS').toUpperCase();
            var filas = document.querySelectorAll('.fila-sede');
            var visibles = 0;

            filas.forEach(function(fila) {
                var searchData = (fila.getAttribute('data-search') || '').toLowerCase();
                var estadoData = (fila.getAttribute('data-estado') || '').toUpperCase();

                var coincideTexto = filtroTexto === '' || searchData.indexOf(filtroTexto) > -1;
                var coincideEstado = filtroEstado === 'TODOS' || estadoData === filtroEstado;

                if (coincideTexto && coincideEstado) {
                    fila.style.display = '';
                    visibles++;
                } else {
                    fila.style.display = 'none';
                }
            });

            var lblTotal = document.getElementById('lblTotalRegistros');
            if (lblTotal) {
                lblTotal.innerText = visibles;
            }
        }

        function simularImportarExcel() {
            var fileInput = document.createElement("input");
            fileInput.type = "file";
            fileInput.accept = ".xlsx, .xls, .csv";
            fileInput.onchange = function(e) {
                var file = e.target.files[0];
                if (file) {
                    alert("Archivo '" + file.name + "' cargado correctamente para validación e importación al catálogo de sedes.");
                }
            };
            fileInput.click();
        }

        function descargarPlantilla() {
            var contenido = "\uFEFFCODIGO,NOMBRE,DIRECCION,DISTRITO,TELEFONO,ESTADO\n" +
                            "SB-01,Sede San Borja,Av. Guardia Civil 337,San Borja,01-610-2222,ACTIVO\n" +
                            "LI-02,Sede Lima,Av. Garcilaso de la Vega 1420,Cercado de Lima,01-610-1111,ACTIVO\n" +
                            "LM-03,Sede La Molina,Av. La Molina 1100,La Molina,01-610-5555,ACTIVO\n" +
                            "SU-04,Sede Surco,Av. Primavera 1750,Santiago de Surco,01-610-4444,MANTENIMIENTO\n";
            var blob = new Blob([contenido], { type: "text/csv;charset=utf-8;" });
            var link = document.createElement("a");
            var url = URL.createObjectURL(blob);
            link.setAttribute("href", url);
            link.setAttribute("download", "plantilla_catalogo_sedes.csv");
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            setTimeout(function() { URL.revokeObjectURL(url); }, 500);
        }

        function exportarExcel() {
            exportarCSV("catalogo_sedes_excel.csv");
        }

        function exportarCSV(nombreArchivo) {
            nombreArchivo = nombreArchivo || "catalogo_sedes.csv";
            var filas = document.querySelectorAll('#tablaSedes tr');
            var csv = [];
            filas.forEach(function(row) {
                if (row.style.display !== 'none') {
                    var cols = row.querySelectorAll('th, td');
                    var filaDatos = [];
                    for (var i = 0; i < cols.length - 1; i++) {
                        var texto = cols[i].innerText.replace(/\n/g, ' ').replace(/"/g, '""').trim();
                        filaDatos.push('"' + texto + '"');
                    }
                    csv.push(filaDatos.join(","));
                }
            });

            var blob = new Blob(["\uFEFF" + csv.join("\n")], { type: "text/csv;charset=utf-8;" });
            var link = document.createElement("a");
            var url = URL.createObjectURL(blob);
            link.setAttribute("href", url);
            link.setAttribute("download", nombreArchivo);
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            setTimeout(function() { URL.revokeObjectURL(url); }, 500);
        }
    </script>
</body>
</html>
